package com.cashbee.application.service.usecase;

import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.model.BatchTransferItem;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.BatchCashbackSnapshotRepository;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import com.cashbee.domain.repository.BatchTransferItemRepository;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Use case for completing batch transfer after admin has transferred money.
 *
 * Improved Flow:
 * 1. Find batch by batchCode with pessimistic lock (prevent race condition)
 * 2. Validate batch status is PENDING (not already processed)
 * 3. Mark batch as PROCESSING (prevent double-processing)
 * 4. For each item in batch:
 *    - Mark item as PROCESSING
 *    - Find wallet by walletId
 *    - Deduct balance (balance → 0, total_withdrawn += amount)
 *    - Create transaction record
 *    - Update cashback status (CONFIRMED → PAID)
 *    - Mark item as COMPLETED or FAILED
 * 5. Mark batch as COMPLETED/PARTIAL_FAILED/FAILED based on results
 *
 * Improvements over previous version:
 * - Pessimistic locking to prevent race conditions
 * - PROCESSING state to prevent double-processing
 * - Proper error handling with FAILED status per item
 * - Audit trail with completedBy, completedAt
 * - Statistics with successCount, failedCount
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompleteBatchTransferUseCase {

    private final BatchTransferExportRepository batchExportRepository;
    private final BatchTransferItemRepository batchItemRepository;
    private final BatchCashbackSnapshotRepository batchCashbackSnapshotRepository;
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CashbackRepository cashbackRepository;

    /**
     * Complete batch transfer.
     *
     * @param batchCode Batch code
     * @param adminId ID of admin who completed the batch (for audit trail)
     * @return Completed batch export
     */
    @Transactional
    public BatchTransferExport execute(String batchCode, Long adminId) {
        log.info("CompleteBatchTransferUseCase: Completing batch {} by admin {}", batchCode, adminId);

        // 1. Find batch with pessimistic lock (prevent race condition)
        BatchTransferExport batch = batchExportRepository.findByBatchCodeForUpdate(batchCode)
                .orElseThrow(() -> NotFoundException.of("BATCH_NOT_FOUND",
                        "Batch not found: " + batchCode));

        // 2. Validate status - only PENDING batches can be processed
        if (!batch.canBeProcessed()) {
            if (batch.isProcessing()) {
                throw new BusinessException("BATCH_PROCESSING",
                        "Batch " + batchCode + " is currently being processed by another request");
            }
            throw new BusinessException("BATCH_ALREADY_COMPLETED",
                    "Batch " + batchCode + " is already " + batch.getStatus());
        }

        // 3. Mark batch as PROCESSING (prevent double-processing)
        batch.markAsProcessing();
        batchExportRepository.save(batch);

        // 4. Get all pending items
        List<BatchTransferItem> items = batchItemRepository.findByBatchIdAndStatus(
                batch.getId(), BatchItemStatus.PENDING);

        if (items.isEmpty()) {
            throw new BusinessException("NO_PENDING_ITEMS",
                    "No pending items found in batch " + batchCode);
        }

        log.info("CompleteBatchTransferUseCase: Processing {} items", items.size());

        int successCount = 0;
        int failCount = 0;

        // 5. Process each item
        for (BatchTransferItem item : items) {
            try {
                processItem(item, batch.getId(), batchCode);
                successCount++;
            } catch (Exception e) {
                log.error("CompleteBatchTransferUseCase: Failed to process item {} for user {}: {}",
                        item.getId(), item.getUserId(), e.getMessage(), e);

                // Mark item as failed with error message
                item.markAsFailed(truncateErrorMessage(e.getMessage()));
                batchItemRepository.save(item);
                failCount++;
            }
        }

        // 6. Mark batch with final status based on results
        batch.markAsCompleted(adminId, successCount, failCount);
        BatchTransferExport savedBatch = batchExportRepository.save(batch);

        log.info("CompleteBatchTransferUseCase: Batch {} completed with status {}. Success: {}, Failed: {}",
                batchCode, savedBatch.getStatus(), successCount, failCount);

        return savedBatch;
    }

    /**
     * Complete batch transfer (backward compatible - without admin ID).
     *
     * @param batchCode Batch code
     * @return Completed batch export
     * @deprecated Use {@link #execute(String, Long)} instead for proper audit trail
     */
    @Deprecated
    @Transactional
    public BatchTransferExport execute(String batchCode) {
        return execute(batchCode, null);
    }

    /**
     * Process single batch item.
     *
     * @param item Batch transfer item
     * @param batchId Batch ID for cashback tracking
     * @param batchCode Batch code for transaction description
     */
    private void processItem(BatchTransferItem item, Long batchId, String batchCode) {
        // 0. Mark item as PROCESSING
        item.markAsProcessing();
        batchItemRepository.save(item);

        // 1. Validate: batch_transfer_item.amount should match sum of unpaid CONFIRMED cashbacks
        BigDecimal unpaidCashbackSum = cashbackRepository.sumUnpaidConfirmedCashbackByUserId(item.getUserId());
        if (unpaidCashbackSum.compareTo(item.getAmount()) != 0) {
            log.warn("CompleteBatchTransferUseCase: User {} amount mismatch! " +
                            "Batch item amount: {}, Unpaid CONFIRMED cashback sum: {}. " +
                            "This may indicate new cashbacks were confirmed after batch creation.",
                    item.getUserId(), item.getAmount(), unpaidCashbackSum);
            // Note: We continue processing but log the warning for audit
        }

        // 2. Find wallet
        UserWallet wallet = walletRepository.findById(item.getWalletId())
                .orElseThrow(() -> NotFoundException.of("WALLET_NOT_FOUND",
                        "Wallet not found: " + item.getWalletId()));

        // 3. Validate balance >= amount
        BigDecimal amountToDeduct = item.getAmount();
        if (wallet.getBalance().compareTo(amountToDeduct) < 0) {
            // Balance đã thay đổi sau khi tạo batch, chỉ trừ số dư hiện tại
            log.warn("CompleteBatchTransferUseCase: User {} balance {} < snapshot amount {}. Deducting actual balance.",
                    item.getUserId(), wallet.getBalance(), amountToDeduct);
            amountToDeduct = wallet.getBalance();
        }

        if (amountToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("CompleteBatchTransferUseCase: User {} has no balance to deduct, marking as completed with 0",
                    item.getUserId());
            item.markAsCompleted(BigDecimal.ZERO);
            batchItemRepository.save(item);
            return;
        }

        // 4. Deduct balance
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(amountToDeduct));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amountToDeduct));
        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

        // 5. Create transaction record
        Transaction transaction = Transaction.builder()
                .userId(item.getUserId())
                .walletId(item.getWalletId())
                .type(TransactionType.WITHDRAW)
                .amount(amountToDeduct)
                .description("Batch transfer " + batchCode)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transaction.validate();
        transactionRepository.save(transaction);

        // 6. Update cashback status: CONFIRMED → PAID (Approach B - using snapshot)
        // Only updates cashbacks that were snapshot at batch creation time.
        // This prevents newly CONFIRMED cashbacks (after batch creation) from being marked as PAID.
        List<Long> snapshotCashbackIds = batchCashbackSnapshotRepository
                .findCashbackIdsByBatchIdAndUserId(batchId, item.getUserId());

        int updatedCashbacks = 0;
        if (!snapshotCashbackIds.isEmpty()) {
            updatedCashbacks = cashbackRepository.updateStatusByCashbackIdsWithBatchId(
                    snapshotCashbackIds,
                    CashbackStatus.CONFIRMED,
                    CashbackStatus.PAID,
                    batchId
            );
            log.info("CompleteBatchTransferUseCase: Updated {} of {} snapshot cashbacks to PAID for user {} (batchId={})",
                    updatedCashbacks, snapshotCashbackIds.size(), item.getUserId(), batchId);
        } else {
            // NO FALLBACK: If no snapshot exists, don't update any cashbacks
            // This is safer - old batches created before snapshot feature will not update cashbacks
            log.warn("CompleteBatchTransferUseCase: No snapshot found for user {} in batch {}. " +
                    "Skipping cashback status update. This batch was likely created before snapshot feature.",
                    item.getUserId(), batchId);
        }

        // 7. Mark item as completed with actual amount deducted
        item.markAsCompleted(amountToDeduct);
        batchItemRepository.save(item);

        log.debug("CompleteBatchTransferUseCase: Processed user {}: {} VND deducted, {} cashbacks updated to PAID",
                item.getUserId(), amountToDeduct, updatedCashbacks);
    }

    /**
     * Truncate error message to fit in database column (max 1000 chars).
     */
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }
}
