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
 * Flow:
 * 1. Find batch by batchCode
 * 2. Validate batch status is PENDING
 * 3. For each item in batch:
 *    - Find wallet by walletId
 *    - Deduct balance (balance → 0, total_withdrawn += amount)
 *    - Create transaction record
 *    - Mark item as COMPLETED
 * 4. Mark batch as COMPLETED
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompleteBatchTransferUseCase {

    private final BatchTransferExportRepository batchExportRepository;
    private final BatchTransferItemRepository batchItemRepository;
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CashbackRepository cashbackRepository;

    /**
     * Complete batch transfer.
     *
     * @param batchCode Batch code
     * @return Completed batch export
     */
    @Transactional
    public BatchTransferExport execute(String batchCode) {
        log.info("CompleteBatchTransferUseCase: Completing batch {}", batchCode);

        // 1. Find batch
        BatchTransferExport batch = batchExportRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> NotFoundException.of("BATCH_NOT_FOUND",
                        "Batch not found: " + batchCode));

        // 2. Validate status
        if (batch.getStatus() != ExportStatus.PENDING) {
            throw new BusinessException("BATCH_ALREADY_COMPLETED",
                    "Batch " + batchCode + " is already " + batch.getStatus());
        }

        // 3. Get all pending items
        List<BatchTransferItem> items = batchItemRepository.findByBatchIdAndStatus(
                batch.getId(), BatchItemStatus.PENDING);

        if (items.isEmpty()) {
            throw new BusinessException("NO_PENDING_ITEMS",
                    "No pending items found in batch " + batchCode);
        }

        log.info("CompleteBatchTransferUseCase: Processing {} items", items.size());

        int successCount = 0;
        int failCount = 0;

        // 4. Process each item (pass batch.getId() for cashback tracking)
        for (BatchTransferItem item : items) {
            try {
                processItem(item, batch.getId(), batchCode);
                successCount++;
            } catch (Exception e) {
                log.error("CompleteBatchTransferUseCase: Failed to process item {} for user {}: {}",
                        item.getId(), item.getUserId(), e.getMessage());
                failCount++;
            }
        }

        // 5. Mark batch as completed
        batch.markAsCompleted();
        BatchTransferExport savedBatch = batchExportRepository.save(batch);

        log.info("CompleteBatchTransferUseCase: Batch {} completed. Success: {}, Failed: {}",
                batchCode, successCount, failCount);

        return savedBatch;
    }

    /**
     * Process single batch item.
     *
     * @param item Batch transfer item
     * @param batchId Batch ID for cashback tracking
     * @param batchCode Batch code for transaction description
     */
    private void processItem(BatchTransferItem item, Long batchId, String batchCode) {
        // 0. Validate: batch_transfer_item.amount should match sum of unpaid CONFIRMED cashbacks
        BigDecimal unpaidCashbackSum = cashbackRepository.sumUnpaidConfirmedCashbackByUserId(item.getUserId());
        if (unpaidCashbackSum.compareTo(item.getAmount()) != 0) {
            log.warn("CompleteBatchTransferUseCase: User {} amount mismatch! " +
                            "Batch item amount: {}, Unpaid CONFIRMED cashback sum: {}. " +
                            "This may indicate new cashbacks were confirmed after batch creation.",
                    item.getUserId(), item.getAmount(), unpaidCashbackSum);
            // Note: We continue processing but log the warning for audit
            // The actual deduction will be based on wallet balance, not cashback sum
        }

        // 1. Find wallet
        UserWallet wallet = walletRepository.findById(item.getWalletId())
                .orElseThrow(() -> NotFoundException.of("WALLET_NOT_FOUND",
                        "Wallet not found: " + item.getWalletId()));

        // 2. Validate balance >= amount
        BigDecimal amountToDeduct = item.getAmount();
        if (wallet.getBalance().compareTo(amountToDeduct) < 0) {
            // Balance đã thay đổi sau khi tạo batch, chỉ trừ số dư hiện tại
            log.warn("CompleteBatchTransferUseCase: User {} balance {} < snapshot amount {}. Deducting actual balance.",
                    item.getUserId(), wallet.getBalance(), amountToDeduct);
            amountToDeduct = wallet.getBalance();
        }

        if (amountToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("CompleteBatchTransferUseCase: User {} has no balance to deduct, skipping",
                    item.getUserId());
            item.markAsCompleted();
            batchItemRepository.save(item);
            return;
        }

        // 3. Deduct balance
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(amountToDeduct));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amountToDeduct));
        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

        // 4. Create transaction record
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

        // 5. Update cashback status: CONFIRMED → PAID (with batch tracking)
        // Only updates cashbacks where paid_batch_id IS NULL (unpaid cashbacks)
        // This ensures newly CONFIRMED cashbacks (after batch creation) are NOT updated
        int updatedCashbacks = cashbackRepository.updateStatusByUserIdAndStatusWithBatchId(
                item.getUserId(),
                CashbackStatus.CONFIRMED,
                CashbackStatus.PAID,
                batchId  // Record which batch paid these cashbacks
        );
        log.debug("CompleteBatchTransferUseCase: Updated {} unpaid CONFIRMED cashbacks to PAID for user {} (batchId={})",
                updatedCashbacks, item.getUserId(), batchId);

        // 6. Mark item as completed
        item.markAsCompleted();
        batchItemRepository.save(item);

        log.debug("CompleteBatchTransferUseCase: Processed user {}: {} VND deducted, {} cashbacks updated to PAID",
                item.getUserId(), amountToDeduct, updatedCashbacks);
    }
}
