package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.invoice.GenerateInvoiceCommand;
import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.application.usecase.invoice.GeneratePaymentInvoiceUseCase;
import com.cashbee.application.usecase.invoice.SendInvoiceEmailUseCase;
import com.cashbee.application.usecase.wallet.RecalculateWalletUseCase;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.model.BatchTransferItem;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.BatchCashbackSnapshotRepository;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import com.cashbee.domain.repository.BatchTransferItemRepository;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use case for completing batch transfer after admin has transferred money.
 *
 * OPTIMIZED VERSION - Performance improvements:
 * 1. Batch load all wallets upfront (1 query instead of N)
 * 2. Batch load all snapshot cashback IDs (1 query instead of N)
 * 3. Bulk save transactions at the end
 * 4. Bulk save batch items at the end
 * 5. Batch query cashbacks for invoice generation
 * 6. Email sending temporarily disabled for performance
 *
 * Flow:
 * 1. Find batch by batchCode with pessimistic lock (prevent race condition)
 * 2. Validate batch status is PENDING (not already processed)
 * 3. Mark batch as PROCESSING (prevent double-processing)
 * 4. Load all required data upfront (wallets, snapshots)
 * 5. Process items in memory, collect changes
 * 6. Bulk save all changes
 * 7. Generate invoices in batch
 * 8. Mark batch as COMPLETED/PARTIAL_FAILED/FAILED
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
    private final ReferralRewardRepository referralRewardRepository;
    private final ReferrerCommissionRepository referrerCommissionRepository;
    private final UserRepository userRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final GeneratePaymentInvoiceUseCase generatePaymentInvoiceUseCase;
    private final SendInvoiceEmailUseCase sendInvoiceEmailUseCase;
    private final RecalculateWalletUseCase recalculateWalletUseCase;

    // Feature flag for email sending - disabled for performance optimization
    private static final boolean EMAIL_SENDING_ENABLED = false;

    /**
     * Complete batch transfer.
     *
     * @param batchCode Batch code
     * @param adminId ID of admin who completed the batch (for audit trail)
     * @return Completed batch export
     */
    @Transactional
    public BatchTransferExport execute(String batchCode, Long adminId) {
        log.info("CompleteBatchTransferUseCase: Starting batch completion for {} by admin {}", batchCode, adminId);
        long startTime = System.currentTimeMillis();

        // 1. Find batch with pessimistic lock (prevent race condition)
        BatchTransferExport batch = batchExportRepository.findByBatchCodeForUpdate(batchCode)
                .orElseThrow(() -> NotFoundException.of("BATCH_NOT_FOUND",
                        "Batch not found: " + batchCode));

        // 2. Validate status - only PENDING batches can be processed
        validateBatchStatus(batch, batchCode);

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

        // 5. OPTIMIZATION: Load all required data upfront
        BatchProcessingContext context = prepareProcessingContext(items, batch.getId());

        // 6. Process all items and collect results
        BatchProcessingResult result = processAllItems(items, batch.getId(), batchCode, context);

        // 7. Bulk save all changes
        bulkSaveChanges(result, context);

        // 7.5 FIX: Recalculate wallets after marking all sources as PAID
        // This ensures balance reflects actual unpaid amount (should be 0 after payment)
        // Why this is needed:
        // - Batch item amount is a SNAPSHOT taken when batch was created
        // - Commission/bonus may have been added AFTER batch creation
        // - After bulkSaveChanges(): all cashback/rewards/commissions are marked as PAID
        // - RecalculateWallet queries source of truth and sets balance correctly
        Set<Long> userIdsToRecalculate = result.getSuccessfulItems().stream()
                .map(BatchTransferItem::getUserId)
                .collect(Collectors.toSet());

        if (!userIdsToRecalculate.isEmpty()) {
            log.info("CompleteBatchTransferUseCase: Recalculating wallets for {} users after payment",
                    userIdsToRecalculate.size());
            recalculateWalletUseCase.executeForUsers(userIdsToRecalculate);
        }

        // 8. Generate invoices for successful items (optimized batch query)
        generateInvoicesOptimized(result.getSuccessfulItems(), batch.getId(), context);

        // 9. Mark batch with final status
        batch.markAsCompleted(adminId, result.getSuccessCount(), result.getFailCount());
        BatchTransferExport savedBatch = batchExportRepository.save(batch);

        long duration = System.currentTimeMillis() - startTime;
        log.info("CompleteBatchTransferUseCase: Batch {} completed in {}ms. Status: {}, Success: {}, Failed: {}",
                batchCode, duration, savedBatch.getStatus(), result.getSuccessCount(), result.getFailCount());

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
     * Validate batch status before processing.
     */
    private void validateBatchStatus(BatchTransferExport batch, String batchCode) {
        if (!batch.canBeProcessed()) {
            if (batch.isProcessing()) {
                throw new BusinessException("BATCH_PROCESSING",
                        "Batch " + batchCode + " is currently being processed by another request");
            }
            throw new BusinessException("BATCH_ALREADY_COMPLETED",
                    "Batch " + batchCode + " is already " + batch.getStatus());
        }
    }

    /**
     * OPTIMIZATION: Prepare all required data upfront to avoid N+1 queries.
     * Loads wallets, snapshot cashback IDs, and platform mapping in bulk.
     */
    private BatchProcessingContext prepareProcessingContext(List<BatchTransferItem> items, Long batchId) {
        log.debug("CompleteBatchTransferUseCase: Preparing processing context for {} items", items.size());

        // Extract all wallet IDs and user IDs
        Set<Long> walletIds = items.stream()
                .map(BatchTransferItem::getWalletId)
                .collect(Collectors.toSet());

        Set<Long> userIds = items.stream()
                .map(BatchTransferItem::getUserId)
                .collect(Collectors.toSet());

        // Bulk load wallets
        Map<Long, UserWallet> walletsById = walletRepository.findAllById(new ArrayList<>(walletIds))
                .stream()
                .collect(Collectors.toMap(UserWallet::getId, Function.identity()));

        // Bulk load snapshot cashback IDs for all users in this batch
        List<Long> allSnapshotCashbackIds = batchCashbackSnapshotRepository.findCashbackIdsByBatchId(batchId);

        // Group by user - we need to query snapshot per user for proper tracking
        Map<Long, List<Long>> snapshotCashbackIdsByUser = new HashMap<>();
        for (Long userId : userIds) {
            List<Long> userSnapshotIds = batchCashbackSnapshotRepository
                    .findCashbackIdsByBatchIdAndUserId(batchId, userId);
            snapshotCashbackIdsByUser.put(userId, userSnapshotIds);
        }

        // Load all platforms for invoice generation
        List<AffiliatePlatform> platforms = platformRepository.findAll();
        Map<Long, String> platformCodeById = platforms.stream()
                .collect(Collectors.toMap(AffiliatePlatform::getId, AffiliatePlatform::getCode));

        // Load unpaid referral rewards for all users in this batch
        Map<Long, List<ReferralReward>> unpaidRewardsByUser = new HashMap<>();
        for (Long userId : userIds) {
            List<ReferralReward> unpaidRewards = referralRewardRepository.findUnpaidByUserId(userId);
            if (!unpaidRewards.isEmpty()) {
                unpaidRewardsByUser.put(userId, unpaidRewards);
            }
        }

        // Load unpaid referrer commissions for all users in this batch
        // (referrer = user who receives commission from their referees' orders)
        Map<Long, List<ReferrerCommission>> unpaidCommissionsByReferrer = new HashMap<>();
        for (Long userId : userIds) {
            List<ReferrerCommission> unpaidCommissions = referrerCommissionRepository.findUnpaidByReferrerId(userId);
            if (!unpaidCommissions.isEmpty()) {
                unpaidCommissionsByReferrer.put(userId, unpaidCommissions);
            }
        }

        log.debug("CompleteBatchTransferUseCase: Context prepared - {} wallets, {} users with snapshots, {} users with unpaid rewards, {} users with unpaid commissions",
                walletsById.size(), snapshotCashbackIdsByUser.size(), unpaidRewardsByUser.size(), unpaidCommissionsByReferrer.size());

        return new BatchProcessingContext(walletsById, snapshotCashbackIdsByUser, platformCodeById, unpaidRewardsByUser, unpaidCommissionsByReferrer);
    }

    /**
     * Process all items and collect results without intermediate saves.
     */
    private BatchProcessingResult processAllItems(
            List<BatchTransferItem> items,
            Long batchId,
            String batchCode,
            BatchProcessingContext context) {

        List<BatchTransferItem> successfulItems = new ArrayList<>();
        List<BatchTransferItem> failedItems = new ArrayList<>();
        List<Transaction> transactionsToSave = new ArrayList<>();
        List<UserWallet> walletsToSave = new ArrayList<>();
        Map<Long, List<Long>> cashbackIdsToUpdate = new HashMap<>();
        List<Long> rewardIdsToMarkAsPaid = new ArrayList<>();
        List<Long> commissionIdsToMarkAsPaid = new ArrayList<>();

        for (BatchTransferItem item : items) {
            try {
                ProcessItemResult itemResult = processItemOptimized(item, batchId, batchCode, context);

                if (itemResult.getTransaction() != null) {
                    transactionsToSave.add(itemResult.getTransaction());
                }
                if (itemResult.getUpdatedWallet() != null) {
                    walletsToSave.add(itemResult.getUpdatedWallet());
                }
                if (itemResult.getCashbackIdsToUpdate() != null && !itemResult.getCashbackIdsToUpdate().isEmpty()) {
                    cashbackIdsToUpdate.put(item.getUserId(), itemResult.getCashbackIdsToUpdate());
                }

                // Collect reward IDs to mark as paid for this user
                List<ReferralReward> unpaidRewards = context.getUnpaidRewardsByUser()
                        .getOrDefault(item.getUserId(), List.of());
                for (ReferralReward reward : unpaidRewards) {
                    rewardIdsToMarkAsPaid.add(reward.getId());
                }

                // Collect commission IDs to mark as paid for this user (as referrer)
                List<ReferrerCommission> unpaidCommissions = context.getUnpaidCommissionsByReferrer()
                        .getOrDefault(item.getUserId(), List.of());
                for (ReferrerCommission commission : unpaidCommissions) {
                    commissionIdsToMarkAsPaid.add(commission.getId());
                }

                successfulItems.add(item);

            } catch (Exception e) {
                log.error("CompleteBatchTransferUseCase: Failed to process item {} for user {}: {}",
                        item.getId(), item.getUserId(), e.getMessage());

                item.markAsFailed(truncateErrorMessage(e.getMessage()));
                failedItems.add(item);
            }
        }

        return new BatchProcessingResult(
                successfulItems,
                failedItems,
                transactionsToSave,
                walletsToSave,
                cashbackIdsToUpdate,
                rewardIdsToMarkAsPaid,
                commissionIdsToMarkAsPaid
        );
    }

    /**
     * Process single item without DB saves (optimized for batch processing).
     */
    private ProcessItemResult processItemOptimized(
            BatchTransferItem item,
            Long batchId,
            String batchCode,
            BatchProcessingContext context) {

        // Mark item as PROCESSING
        item.markAsProcessing();

        // Get wallet from pre-loaded context
        UserWallet wallet = context.getWalletsById().get(item.getWalletId());
        if (wallet == null) {
            throw NotFoundException.of("WALLET_NOT_FOUND", "Wallet not found: " + item.getWalletId());
        }

        // Calculate amount to deduct
        BigDecimal amountToDeduct = item.getAmount();
        if (wallet.getBalance().compareTo(amountToDeduct) < 0) {
            log.warn("CompleteBatchTransferUseCase: User {} balance {} < snapshot amount {}. Deducting actual balance.",
                    item.getUserId(), wallet.getBalance(), amountToDeduct);
            amountToDeduct = wallet.getBalance();
        }

        Transaction transaction = null;
        UserWallet updatedWallet = null;

        if (amountToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            // Deduct balance
            BigDecimal balanceBefore = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().subtract(amountToDeduct));
            wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amountToDeduct));
            BigDecimal balanceAfter = wallet.getBalance();

            updatedWallet = wallet;

            // Create transaction record (will be saved in bulk)
            transaction = Transaction.builder()
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
        }

        // Get snapshot cashback IDs for this user
        List<Long> snapshotCashbackIds = context.getSnapshotCashbackIdsByUser()
                .getOrDefault(item.getUserId(), List.of());

        // Mark item as completed
        item.markAsCompleted(amountToDeduct);

        return new ProcessItemResult(transaction, updatedWallet, snapshotCashbackIds);
    }

    /**
     * Bulk save all changes to database.
     */
    private void bulkSaveChanges(BatchProcessingResult result, BatchProcessingContext context) {
        log.debug("CompleteBatchTransferUseCase: Bulk saving changes - {} transactions, {} wallets, {} cashback updates, {} rewards to mark paid, {} commissions to mark paid",
                result.getTransactionsToSave().size(),
                result.getWalletsToSave().size(),
                result.getCashbackIdsToUpdate().size(),
                result.getRewardIdsToMarkAsPaid().size(),
                result.getCommissionIdsToMarkAsPaid().size());

        // Bulk save wallets
        if (!result.getWalletsToSave().isEmpty()) {
            walletRepository.saveAll(result.getWalletsToSave());
        }

        // Bulk save transactions
        if (!result.getTransactionsToSave().isEmpty()) {
            transactionRepository.saveAll(result.getTransactionsToSave());
        }

        // Bulk save batch items (both successful and failed)
        List<BatchTransferItem> allItems = new ArrayList<>();
        allItems.addAll(result.getSuccessfulItems());
        allItems.addAll(result.getFailedItems());
        if (!allItems.isEmpty()) {
            batchItemRepository.saveAll(allItems);
        }

        // Update cashback statuses (one query per user, but using efficient bulk update)
        for (Map.Entry<Long, List<Long>> entry : result.getCashbackIdsToUpdate().entrySet()) {
            Long userId = entry.getKey();
            List<Long> cashbackIds = entry.getValue();

            if (!cashbackIds.isEmpty()) {
                // Find batchId from one of the successful items
                Long batchId = result.getSuccessfulItems().stream()
                        .filter(item -> item.getUserId().equals(userId))
                        .findFirst()
                        .map(item -> {
                            // Get batchId from context - we need to pass it through
                            return result.getSuccessfulItems().get(0).getBatchId();
                        })
                        .orElse(null);

                if (batchId != null) {
                    int updated = cashbackRepository.updateStatusByCashbackIdsWithBatchId(
                            cashbackIds,
                            CashbackStatus.CONFIRMED,
                            CashbackStatus.PAID,
                            batchId
                    );
                    log.debug("CompleteBatchTransferUseCase: Updated {} cashbacks to PAID for user {}", updated, userId);
                }
            }
        }

        // Catch-all: update any remaining CONFIRMED cashbacks with NULL paid_batch_id
        // These are cashbacks that were confirmed AFTER the batch snapshot was taken
        // but their balance was included in the wallet balance (and thus the transfer amount).
        // The method has "AND paidBatchId IS NULL" so already-updated cashbacks are skipped.
        for (BatchTransferItem item : result.getSuccessfulItems()) {
            Long batchId = item.getBatchId();
            if (batchId != null) {
                int extraUpdated = cashbackRepository.updateStatusByUserIdAndStatusWithBatchId(
                        item.getUserId(),
                        CashbackStatus.CONFIRMED,
                        CashbackStatus.PAID,
                        batchId
                );
                if (extraUpdated > 0) {
                    log.info("CompleteBatchTransferUseCase: Updated {} additional non-snapshotted cashbacks to PAID for user {}",
                            extraUpdated, item.getUserId());
                }
            }
        }

        // Mark referral rewards as paid by this batch
        if (!result.getRewardIdsToMarkAsPaid().isEmpty()) {
            Long batchId = result.getSuccessfulItems().isEmpty() ? null
                    : result.getSuccessfulItems().get(0).getBatchId();

            if (batchId != null) {
                int updatedRewards = referralRewardRepository.markAsPaidByBatch(
                        result.getRewardIdsToMarkAsPaid(),
                        batchId
                );
                log.info("CompleteBatchTransferUseCase: Marked {} referral rewards as PAID by batch {}",
                        updatedRewards, batchId);
            }
        }

        // Mark referrer commissions as paid by this batch
        if (!result.getCommissionIdsToMarkAsPaid().isEmpty()) {
            Long batchId = result.getSuccessfulItems().isEmpty() ? null
                    : result.getSuccessfulItems().get(0).getBatchId();

            if (batchId != null) {
                int updatedCommissions = referrerCommissionRepository.markAsPaidByBatch(
                        result.getCommissionIdsToMarkAsPaid(),
                        batchId
                );
                log.info("CompleteBatchTransferUseCase: Marked {} referrer commissions as PAID by batch {}",
                        updatedCommissions, batchId);
            }
        }
    }

    /**
     * OPTIMIZATION: Generate invoices with batch-loaded cashback data.
     */
    private void generateInvoicesOptimized(List<BatchTransferItem> items, Long batchId, BatchProcessingContext context) {
        if (items.isEmpty()) {
            log.info("CompleteBatchTransferUseCase: No successful items to generate invoices for");
            return;
        }

        log.info("CompleteBatchTransferUseCase: Generating invoices for {} successful items", items.size());

        // Batch load all paid cashbacks for this batch
        List<Cashback> allPaidCashbacks = cashbackRepository.findByPaidBatchId(batchId);

        // Group cashbacks by user
        Map<Long, List<Cashback>> cashbacksByUser = allPaidCashbacks.stream()
                .collect(Collectors.groupingBy(Cashback::getUserId));

        for (BatchTransferItem item : items) {
            try {
                List<Cashback> userCashbacks = cashbacksByUser.getOrDefault(item.getUserId(), List.of());
                generateInvoiceForItemOptimized(item, batchId, userCashbacks, context);
            } catch (Exception e) {
                log.error("CompleteBatchTransferUseCase: Failed to generate invoice for user {}: {}",
                        item.getUserId(), e.getMessage());
                // Don't fail the batch - invoice generation is secondary
            }
        }
    }

    /**
     * Generate invoice for a single item using pre-loaded cashback data.
     *
     * IMPORTANT: Invoice được tạo cho user có BẤT KỲ loại tiền nào:
     * - Cashback từ đơn hàng của chính mình
     * - Bonus từ milestone (thưởng mốc)
     * - Commission từ việc giới thiệu (hoa hồng 5% từ đơn hàng của referee)
     *
     * User có thể chỉ có bonus + commission mà không có cashback (ví dụ: chỉ giới thiệu người khác).
     */
    private void generateInvoiceForItemOptimized(
            BatchTransferItem item,
            Long batchId,
            List<Cashback> paidCashbacks,
            BatchProcessingContext context) {

        // === STEP 1: Tính bonus từ referral_reward table ===
        List<ReferralReward> userUnpaidRewards = context.getUnpaidRewardsByUser()
                .getOrDefault(item.getUserId(), List.of());

        int bonusOrders = 0;
        BigDecimal bonusAmount = BigDecimal.ZERO;
        int referrerCommissionOrders = 0;
        BigDecimal referrerCommissionAmount = BigDecimal.ZERO;

        for (ReferralReward reward : userUnpaidRewards) {
            if (reward.getAmount() != null && reward.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                String rewardType = reward.getRewardType() != null ? reward.getRewardType().name() : "";
                if ("MILESTONE_BONUS".equals(rewardType)) {
                    bonusOrders++;
                    bonusAmount = bonusAmount.add(reward.getAmount());
                } else if ("REFERRER_BONUS".equals(rewardType) || "REFERRER_COMMISSION".equals(rewardType)) {
                    referrerCommissionOrders++;
                    referrerCommissionAmount = referrerCommissionAmount.add(reward.getAmount());
                }
            }
        }

        // === STEP 2: Tính commission từ referrer_commission table (5% từ đơn hàng của referee) ===
        List<ReferrerCommission> userUnpaidCommissions = context.getUnpaidCommissionsByReferrer()
                .getOrDefault(item.getUserId(), List.of());

        for (ReferrerCommission commission : userUnpaidCommissions) {
            if (commission.getCommissionAmount() != null && commission.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
                referrerCommissionOrders++;
                referrerCommissionAmount = referrerCommissionAmount.add(commission.getCommissionAmount());
            }
        }

        // === STEP 3: Tính cashback từ đơn hàng ===
        BigDecimal totalCashbackAmount = paidCashbacks.stream()
                .map(Cashback::getCashbackAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // === STEP 4: Kiểm tra - chỉ skip nếu KHÔNG có bất kỳ loại tiền nào ===
        boolean hasCashback = totalCashbackAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasBonus = bonusAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasCommission = referrerCommissionAmount.compareTo(BigDecimal.ZERO) > 0;

        if (!hasCashback && !hasBonus && !hasCommission) {
            log.warn("CompleteBatchTransferUseCase: No payment (cashback/bonus/commission) found for user {} in batch {}, skipping invoice",
                    item.getUserId(), batchId);
            return;
        }

        log.info("CompleteBatchTransferUseCase: User {} has cashback={}, bonus={}, commission={} in batch {}",
                item.getUserId(), totalCashbackAmount, bonusAmount, referrerCommissionAmount, batchId);

        // === STEP 5: Aggregate cashbacks by platform (nếu có) ===
        Map<Long, List<Cashback>> cashbacksByPlatform = new HashMap<>();
        for (Cashback cashback : paidCashbacks) {
            Long platformId = cashback.getPlatformId() != null ? cashback.getPlatformId() : 0L;
            cashbacksByPlatform.computeIfAbsent(platformId, k -> new ArrayList<>()).add(cashback);
        }

        // Build platform order details
        List<GenerateInvoiceCommand.PlatformOrderDetail> platformDetails = new ArrayList<>();
        for (Map.Entry<Long, List<Cashback>> entry : cashbacksByPlatform.entrySet()) {
            Long platformId = entry.getKey();
            List<Cashback> cashbacks = entry.getValue();

            String platformCode = context.getPlatformCodeById().getOrDefault(platformId, "other");
            if (platformId == 0L) {
                platformCode = "other";
            }

            int orderCount = (int) cashbacks.stream().map(Cashback::getOrderId).distinct().count();
            BigDecimal totalAmount = cashbacks.stream()
                    .map(Cashback::getCashbackAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            platformDetails.add(GenerateInvoiceCommand.PlatformOrderDetail.builder()
                    .platformCode(platformCode)
                    .orderCount(orderCount)
                    .totalAmount(totalAmount)
                    .build());
        }

        // === STEP 6: Build and execute generate invoice command ===
        GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                .userId(item.getUserId())
                .batchId(batchId)
                .batchItemId(item.getId())
                .amount(item.getActualAmountDeducted() != null ? item.getActualAmountDeducted() : item.getAmount())
                .bankName(item.getBankName())
                .bankAccountNumber(item.getAccountNumber())
                .totalOrders((int) paidCashbacks.stream().map(Cashback::getOrderId).distinct().count())
                .transferTime(item.getCompletedAt() != null ? item.getCompletedAt() : LocalDateTime.now())
                .platformOrders(platformDetails)
                // Bonus breakdown
                .bonusOrders(bonusOrders)
                .bonusAmount(bonusAmount)
                .referrerCommissionOrders(referrerCommissionOrders)
                .referrerCommissionAmount(referrerCommissionAmount)
                .build();

        PaymentInvoiceResponse invoiceResponse = generatePaymentInvoiceUseCase.execute(command);

        log.info("CompleteBatchTransferUseCase: Generated invoice {} for user {}",
                invoiceResponse.getInvoiceNumber(), item.getUserId());

        // EMAIL SENDING TEMPORARILY DISABLED FOR PERFORMANCE
        // TODO: Re-enable when email infrastructure is optimized (use async queue)
        if (EMAIL_SENDING_ENABLED) {
            sendInvoiceEmailAsync(invoiceResponse.getId(), item.getUserId());
        }
    }

    /**
     * Send invoice email asynchronously.
     * Currently DISABLED for performance optimization.
     *
     * @param invoiceId Invoice ID
     * @param userId User ID
     */
    @SuppressWarnings("unused")
    private void sendInvoiceEmailAsync(Long invoiceId, Long userId) {
        // EMAIL SENDING TEMPORARILY DISABLED
        // This method is kept for future re-enablement
        // To re-enable: set EMAIL_SENDING_ENABLED = true
        /*
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("CompleteBatchTransferUseCase: User {} not found, skipping invoice email", userId);
                return;
            }

            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                log.info("CompleteBatchTransferUseCase: User {} has no email, skipping invoice email", userId);
                return;
            }

            SendInvoiceEmailCommand emailCommand = SendInvoiceEmailCommand.forInvoice(
                    invoiceId,
                    user.getEmail(),
                    user.getFullName() != null ? user.getFullName() : user.getUsername()
            );

            sendInvoiceEmailUseCase.sendAsync(emailCommand);

            log.debug("CompleteBatchTransferUseCase: Queued invoice email for user {}", userId);
        } catch (Exception e) {
            log.error("CompleteBatchTransferUseCase: Failed to send invoice email for user {}: {}",
                    userId, e.getMessage(), e);
            // Don't throw - email is secondary
        }
        */
        log.debug("CompleteBatchTransferUseCase: Email sending disabled, skipping for user {}", userId);
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

    // ========== Inner Classes for Batch Processing ==========

    /**
     * Context holding pre-loaded data for batch processing.
     * Avoids N+1 queries by loading all required data upfront.
     */
    private static class BatchProcessingContext {
        private final Map<Long, UserWallet> walletsById;
        private final Map<Long, List<Long>> snapshotCashbackIdsByUser;
        private final Map<Long, String> platformCodeById;
        private final Map<Long, List<ReferralReward>> unpaidRewardsByUser;
        private final Map<Long, List<ReferrerCommission>> unpaidCommissionsByReferrer;

        public BatchProcessingContext(
                Map<Long, UserWallet> walletsById,
                Map<Long, List<Long>> snapshotCashbackIdsByUser,
                Map<Long, String> platformCodeById,
                Map<Long, List<ReferralReward>> unpaidRewardsByUser,
                Map<Long, List<ReferrerCommission>> unpaidCommissionsByReferrer) {
            this.walletsById = walletsById;
            this.snapshotCashbackIdsByUser = snapshotCashbackIdsByUser;
            this.platformCodeById = platformCodeById;
            this.unpaidRewardsByUser = unpaidRewardsByUser;
            this.unpaidCommissionsByReferrer = unpaidCommissionsByReferrer;
        }

        public Map<Long, UserWallet> getWalletsById() {
            return walletsById;
        }

        public Map<Long, List<Long>> getSnapshotCashbackIdsByUser() {
            return snapshotCashbackIdsByUser;
        }

        public Map<Long, String> getPlatformCodeById() {
            return platformCodeById;
        }

        public Map<Long, List<ReferralReward>> getUnpaidRewardsByUser() {
            return unpaidRewardsByUser;
        }

        public Map<Long, List<ReferrerCommission>> getUnpaidCommissionsByReferrer() {
            return unpaidCommissionsByReferrer;
        }
    }

    /**
     * Result of processing all batch items.
     */
    private static class BatchProcessingResult {
        private final List<BatchTransferItem> successfulItems;
        private final List<BatchTransferItem> failedItems;
        private final List<Transaction> transactionsToSave;
        private final List<UserWallet> walletsToSave;
        private final Map<Long, List<Long>> cashbackIdsToUpdate;
        private final List<Long> rewardIdsToMarkAsPaid;
        private final List<Long> commissionIdsToMarkAsPaid;

        public BatchProcessingResult(
                List<BatchTransferItem> successfulItems,
                List<BatchTransferItem> failedItems,
                List<Transaction> transactionsToSave,
                List<UserWallet> walletsToSave,
                Map<Long, List<Long>> cashbackIdsToUpdate,
                List<Long> rewardIdsToMarkAsPaid,
                List<Long> commissionIdsToMarkAsPaid) {
            this.successfulItems = successfulItems;
            this.failedItems = failedItems;
            this.transactionsToSave = transactionsToSave;
            this.walletsToSave = walletsToSave;
            this.cashbackIdsToUpdate = cashbackIdsToUpdate;
            this.rewardIdsToMarkAsPaid = rewardIdsToMarkAsPaid;
            this.commissionIdsToMarkAsPaid = commissionIdsToMarkAsPaid;
        }

        public List<BatchTransferItem> getSuccessfulItems() {
            return successfulItems;
        }

        public List<BatchTransferItem> getFailedItems() {
            return failedItems;
        }

        public List<Transaction> getTransactionsToSave() {
            return transactionsToSave;
        }

        public List<UserWallet> getWalletsToSave() {
            return walletsToSave;
        }

        public Map<Long, List<Long>> getCashbackIdsToUpdate() {
            return cashbackIdsToUpdate;
        }

        public List<Long> getRewardIdsToMarkAsPaid() {
            return rewardIdsToMarkAsPaid;
        }

        public List<Long> getCommissionIdsToMarkAsPaid() {
            return commissionIdsToMarkAsPaid;
        }

        public int getSuccessCount() {
            return successfulItems.size();
        }

        public int getFailCount() {
            return failedItems.size();
        }
    }

    /**
     * Result of processing a single item.
     */
    private static class ProcessItemResult {
        private final Transaction transaction;
        private final UserWallet updatedWallet;
        private final List<Long> cashbackIdsToUpdate;

        public ProcessItemResult(Transaction transaction, UserWallet updatedWallet, List<Long> cashbackIdsToUpdate) {
            this.transaction = transaction;
            this.updatedWallet = updatedWallet;
            this.cashbackIdsToUpdate = cashbackIdsToUpdate;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public UserWallet getUpdatedWallet() {
            return updatedWallet;
        }

        public List<Long> getCashbackIdsToUpdate() {
            return cashbackIdsToUpdate;
        }
    }
}
