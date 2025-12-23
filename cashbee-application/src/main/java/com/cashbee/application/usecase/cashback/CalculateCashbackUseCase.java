package com.cashbee.application.usecase.cashback;

import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.CashbackPolicy;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for calculating cashback from affiliate commission.
 *
 * Business Logic:
 * 1. Get active cashback policy for platform
 * 2. Calculate cashback = commission * (rate / 100)
 * 3. Apply min/max limits from policy
 * 4. Create Cashback record with appropriate status
 *
 * Status Logic:
 * - If order completed → CONFIRMED (ready to add to wallet)
 * - If order pending → PENDING (wait for completion)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateCashbackUseCase {

    private final CashbackRepository cashbackRepository;
    private final CashbackPolicyRepository policyRepository;
    private final UserWalletRepository walletRepository;
    private final EntityManager entityManager;

    /**
     * Calculate and create cashback for an order.
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param platformId Platform ID
     * @param commissionAmount Commission amount from platform (VND)
     * @param isOrderCompleted Whether order is already completed
     * @return Created Cashback
     */
    @Transactional
    public Cashback execute(Long userId, Long orderId, Long platformId,
                           BigDecimal commissionAmount, boolean isOrderCompleted) {

        log.info("UseCase: Calculating cashback for order {} (user: {}, commission: {}, completed: {})",
            orderId, userId, commissionAmount, isOrderCompleted);

        // Validate inputs
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID: " + orderId);
        }

        if (platformId == null || platformId <= 0) {
            throw new IllegalArgumentException("Invalid platform ID: " + platformId);
        }

        if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Commission amount must be positive: " + commissionAmount);
        }

        // Check if cashback already exists for this order
        if (cashbackRepository.existsByOrderId(orderId)) {
            log.warn("UseCase: Cashback already exists for order {}, skipping calculation", orderId);
            return cashbackRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));
        }

        // Get active cashback policy for platform
        // Using UserLevel.NORMAL as default user level
        CashbackPolicy policy = policyRepository
            .findActivePolicyFor(platformId, UserLevel.NORMAL, LocalDateTime.now())
            .orElse(null);

        BigDecimal cashbackRate;
        Long policyId = null;

        if (policy != null) {
            cashbackRate = policy.getCashbackRate();
            policyId = policy.getId();
            log.info("UseCase: Using policy {} with rate {}%", policy.getPolicyName(), cashbackRate);
        } else {
            // Default rate if no policy found (70%)
            cashbackRate = new BigDecimal("70.00");
            log.warn("UseCase: No active policy found for platform {}, using default rate {}%",
                platformId, cashbackRate);
        }

        // Calculate cashback amount
        BigDecimal cashbackAmount = commissionAmount
            .multiply(cashbackRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        log.info("UseCase: Calculated cashback: {} VND (rate: {}%)", cashbackAmount, cashbackRate);

        // Apply policy limits if policy exists
        if (policy != null) {
            // Check minimum order value
            if (policy.getMinOrderValue() != null &&
                commissionAmount.compareTo(policy.getMinOrderValue()) < 0) {
                log.warn("UseCase: Commission {} below minimum {}, setting cashback to 0",
                    commissionAmount, policy.getMinOrderValue());
                cashbackAmount = BigDecimal.ZERO;
            }

            // Apply maximum cashback per order
            if (policy.getMaxCashbackPerOrder() != null &&
                cashbackAmount.compareTo(policy.getMaxCashbackPerOrder()) > 0) {
                log.warn("UseCase: Cashback {} exceeds maximum {}, capping to maximum",
                    cashbackAmount, policy.getMaxCashbackPerOrder());
                cashbackAmount = policy.getMaxCashbackPerOrder();
            }
        }

        // Determine cashback status based on order status
        CashbackStatus status = isOrderCompleted
            ? CashbackStatus.CONFIRMED  // Order completed, ready to add to wallet
            : CashbackStatus.PENDING;   // Order pending, wait for completion

        // Create Cashback record
        Cashback cashback = Cashback.builder()
            .userId(userId)
            .orderId(orderId)
            .platformId(platformId)
            .commissionAmount(commissionAmount)
            .cashbackAmount(cashbackAmount)
            .cashbackRate(cashbackRate)
            .policyId(policyId)
            .status(status)
            .note(isOrderCompleted ? "Order completed, cashback confirmed" : "Waiting for order completion")
            .createdAt(LocalDateTime.now())
            .confirmedAt(isOrderCompleted ? LocalDateTime.now() : null)
            .updatedAt(LocalDateTime.now())
            .build();

        // Validate business rules
        cashback.validate();

        // Save cashback
        Cashback savedCashback = cashbackRepository.save(cashback);

        log.info("UseCase: Created cashback {} with amount {} VND (status: {})",
            savedCashback.getId(), savedCashback.getCashbackAmount(), savedCashback.getStatus());

        return savedCashback;
    }

    /**
     * Calculate and create cashback for an order item.
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param orderItemId Order Item ID
     * @param platformId Platform ID
     * @param commissionAmount Commission amount from platform (VND)
     * @param isItemCompleted Whether item is already completed
     * @return Created Cashback
     */
    /**
     * NOTE: Removed @Transactional to prevent nested transaction issues.
     * This method is called from ImportShopeeOrdersUseCase which already has a transaction.
     * Having @Transactional here causes "rollback-only" marking when exceptions occur,
     * leading to UnexpectedRollbackException even when errors are caught.
     */
    public Cashback executeForItem(Long userId, Long orderId, Long orderItemId, Long platformId,
                                   BigDecimal commissionAmount, boolean isItemCompleted) {

        log.info("[DEBUG-CASHBACK] executeForItem START: userId={}, orderId={}, orderItemId={}, platformId={}, commission={}, completed={}",
            userId, orderId, orderItemId, platformId, commissionAmount, isItemCompleted);

        // Validate inputs
        if (orderItemId == null || orderItemId <= 0) {
            log.error("[DEBUG-CASHBACK] VALIDATION FAILED: orderItemId is null or <= 0: {}", orderItemId);
            throw new IllegalArgumentException("Invalid order item ID: " + orderItemId);
        }
        log.info("[DEBUG-CASHBACK] orderItemId validation passed: {}", orderItemId);

        // Check if cashback already exists for this item
        if (cashbackRepository.existsByOrderItemId(orderItemId)) {
            log.warn("UseCase: Cashback already exists for item {}, skipping calculation", orderItemId);
            return cashbackRepository.findByOrderItemId(orderItemId)
                .orElseThrow(() -> new NotFoundException("Cashback not found for item: " + orderItemId));
        }

        // Calculate using same logic
        BigDecimal cashbackRate = getDefaultCashbackRate(platformId);
        BigDecimal cashbackAmount = commissionAmount
            .multiply(cashbackRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        CashbackStatus status = isItemCompleted ? CashbackStatus.CONFIRMED : CashbackStatus.PENDING;

        Cashback cashback = Cashback.builder()
            .userId(userId)
            .orderId(orderId)
            .orderItemId(orderItemId)
            .platformId(platformId)
            .commissionAmount(commissionAmount)
            .cashbackAmount(cashbackAmount)
            .cashbackRate(cashbackRate)
            .status(status)
            .note(isItemCompleted ? "Item completed, cashback confirmed" : "Waiting for item completion")
            .createdAt(LocalDateTime.now())
            .confirmedAt(isItemCompleted ? LocalDateTime.now() : null)
            .updatedAt(LocalDateTime.now())
            .build();

        cashback.validate();
        Cashback savedCashback = cashbackRepository.save(cashback);

        // Update wallet directly (same transaction, no query needed)
        updateWalletForNewCashback(userId, cashbackAmount, status);

        log.info("UseCase: Created cashback {} for item {} with amount {} VND (status: {})",
            savedCashback.getId(), orderItemId, savedCashback.getCashbackAmount(), savedCashback.getStatus());

        return savedCashback;
    }

    /**
     * Upsert cashback for an order item (create if not exists, update if exists).
     * This is a convenience method that delegates to the full version with isCancelled=false.
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param orderItemId Order Item ID
     * @param platformId Platform ID
     * @param commissionAmount Commission amount from platform (VND)
     * @param isItemCompleted Whether item is already completed
     * @return Created or updated Cashback
     */
    /**
     * NOTE: Removed @Transactional - called from ImportShopeeOrdersUseCase which has transaction.
     */
    public Cashback upsertForItem(Long userId, Long orderId, Long orderItemId, Long platformId,
                                  BigDecimal commissionAmount, boolean isItemCompleted) {
        return upsertForItemWithCancellation(userId, orderId, orderItemId, platformId,
            commissionAmount, isItemCompleted, false);
    }

    /**
     * Upsert cashback for an order item with cancellation support.
     *
     * Handles all status transitions:
     * - PENDING → CONFIRMED: Order completed, move pending_balance to balance
     * - PENDING → CANCELLED: Order cancelled before completion, subtract pending_balance
     * - CONFIRMED → CANCELLED: Order cancelled after completion (refund), subtract balance
     * - PAID → CANCELLED: Order refunded after payment, subtract balance
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param orderItemId Order Item ID
     * @param platformId Platform ID
     * @param commissionAmount Commission amount from platform (VND)
     * @param isItemCompleted Whether item is already completed
     * @param isCancelled Whether item is cancelled
     * @return Created or updated Cashback, or null if cancelled order has no existing cashback
     */
    /**
     * NOTE: Removed @Transactional to prevent nested transaction issues.
     * This method is called from ImportShopeeOrdersUseCase.updateExistingOrderWithItems()
     * which already runs within a transaction. Having @Transactional here causes
     * "rollback-only" marking when any exception occurs (even if caught),
     * leading to UnexpectedRollbackException at commit time.
     */
    public Cashback upsertForItemWithCancellation(Long userId, Long orderId, Long orderItemId, Long platformId,
                                                   BigDecimal commissionAmount, boolean isItemCompleted,
                                                   boolean isCancelled) {

        log.info("[DEBUG-CASHBACK] START upsertForItemWithCancellation: userId={}, orderId={}, orderItemId={}, platformId={}, commission={}, completed={}, cancelled={}",
            userId, orderId, orderItemId, platformId, commissionAmount, isItemCompleted, isCancelled);

        // CRITICAL FIX: First try to find by orderItemId (if not null), then fallback to orderId
        // This handles the case where orderItem gets recreated with a new ID or doesn't exist
        Optional<Cashback> existingCashback = Optional.empty();
        if (orderItemId != null) {
            existingCashback = cashbackRepository.findByOrderItemId(orderItemId);
            log.info("UseCase: findByOrderItemId({}) returned: {}",
                orderItemId, existingCashback.isPresent() ? existingCashback.get().getId() : "empty");
        } else {
            log.info("UseCase: orderItemId is null, will search by orderId directly");
        }

        // Track the OLD status before any modifications
        CashbackStatus oldStatusBeforeUpdate = null;
        BigDecimal cashbackAmountForWallet = null;

        // If not found by orderItemId (or orderItemId is null), try to find ANY cashback for this order
        // This is needed when orderItem is recreated in subsequent imports or when item doesn't exist
        if (existingCashback.isEmpty() && orderId != null) {
            log.info("UseCase: Cashback not found by orderItemId {}, searching by orderId {}",
                orderItemId, orderId);
            // Find all cashbacks for this order and check if any needs updating
            var orderCashbacks = cashbackRepository.findAllByOrderId(orderId);
            log.info("UseCase: findAllByOrderId({}) returned {} cashbacks", orderId, orderCashbacks.size());

            if (!orderCashbacks.isEmpty()) {
                // Use the first cashback found for this order (usually there's only one per order anyway)
                Cashback oldCashback = orderCashbacks.get(0);
                log.info("UseCase: Found existing cashback {} by orderId (status: {}), will update it",
                    oldCashback.getId(), oldCashback.getStatus());

                // CRITICAL: Save old status BEFORE any modifications
                oldStatusBeforeUpdate = oldCashback.getStatus();
                cashbackAmountForWallet = oldCashback.getCashbackAmount();

                // Create new cashback - only update orderItemId if provided (not null)
                // Keep existing orderItemId if new one is null (for cancellation by orderId)
                Long effectiveOrderItemId = orderItemId != null ? orderItemId : oldCashback.getOrderItemId();
                Cashback updatedCashback = Cashback.builder()
                    .id(oldCashback.getId())
                    .userId(oldCashback.getUserId())
                    .orderId(oldCashback.getOrderId())
                    .orderItemId(effectiveOrderItemId)  // Keep existing if new is null
                    .platformId(oldCashback.getPlatformId())
                    .commissionAmount(oldCashback.getCommissionAmount())
                    .cashbackAmount(oldCashback.getCashbackAmount())
                    .cashbackRate(oldCashback.getCashbackRate())
                    .policyId(oldCashback.getPolicyId())
                    .status(oldCashback.getStatus())
                    .note(oldCashback.getNote())
                    .createdAt(oldCashback.getCreatedAt())
                    .confirmedAt(oldCashback.getConfirmedAt())
                    .paidAt(oldCashback.getPaidAt())
                    .cancelledAt(oldCashback.getCancelledAt())
                    .updatedAt(LocalDateTime.now())
                    .build();

                updatedCashback = cashbackRepository.save(updatedCashback);
                existingCashback = Optional.of(updatedCashback);
            }
        }

        if (existingCashback.isPresent()) {
            Cashback cashback = existingCashback.get();
            CashbackStatus currentStatus = cashback.getStatus();

            // FIX: Don't downgrade PAID status - it's already finalized
            // PAID cashbacks can only be changed to CANCELLED (refund scenario)
            if (currentStatus == CashbackStatus.PAID && !isCancelled) {
                log.info("UseCase: Cashback {} already PAID, skipping status update (import cannot downgrade PAID status)",
                    cashback.getId());
                return cashback;
            }

            // Determine new status based on flags
            CashbackStatus newStatus;
            if (isCancelled) {
                newStatus = CashbackStatus.CANCELLED;
            } else if (isItemCompleted) {
                newStatus = CashbackStatus.CONFIRMED;
            } else {
                newStatus = CashbackStatus.PENDING;
            }

            log.info("UseCase: Cashback {} - currentStatus: {}, newStatus: {}, completed: {}, cancelled: {}",
                cashback.getId(), currentStatus, newStatus, isItemCompleted, isCancelled);

            // Only update if status changed
            if (currentStatus != newStatus) {
                log.info("UseCase: Status CHANGED! Updating cashback {} from {} to {}",
                    cashback.getId(), currentStatus, newStatus);

                // Use the amount from cashback (not from input, as it may differ)
                BigDecimal amountToUse = cashbackAmountForWallet != null
                    ? cashbackAmountForWallet
                    : cashback.getCashbackAmount();

                // Handle wallet update based on status change type
                if (newStatus == CashbackStatus.CANCELLED) {
                    // Handle cancellation - subtract from appropriate balance
                    log.info("UseCase: Calling updateWalletForCancellation(userId={}, amount={}, oldStatus={})",
                        cashback.getUserId(), amountToUse, currentStatus);
                    updateWalletForCancellation(cashback.getUserId(), amountToUse, currentStatus);
                } else {
                    // Handle normal status change (PENDING ↔ CONFIRMED)
                    log.info("UseCase: Calling updateWalletForStatusChange(userId={}, amount={}, {} -> {})",
                        cashback.getUserId(), amountToUse, currentStatus, newStatus);
                    updateWalletForStatusChange(cashback.getUserId(), amountToUse, currentStatus, newStatus);
                }

                // Build note based on status change
                String note;
                if (isCancelled) {
                    note = "Order cancelled, cashback reversed";
                } else if (isItemCompleted) {
                    note = "Item completed, cashback confirmed";
                } else {
                    note = "Item status changed to pending";
                }

                Cashback updatedCashback = cashback.withStatus(newStatus, note);
                Cashback savedCashback = cashbackRepository.save(updatedCashback);

                log.info("UseCase: Cashback {} saved with new status {}", savedCashback.getId(), savedCashback.getStatus());
                return savedCashback;
            }

            log.info("UseCase: Cashback {} already has status {}, no update needed",
                cashback.getId(), currentStatus);
            return cashback;
        }

        // For cancelled orders without existing cashback, nothing to do
        if (isCancelled) {
            log.info("[DEBUG-CASHBACK] Cancelled order {} has no existing cashback, nothing to cancel - returning null", orderId);
            return null;
        }

        // Create new cashback
        log.info("[DEBUG-CASHBACK] No existing cashback found, calling executeForItem for item {}", orderItemId);
        log.info("[DEBUG-CASHBACK] executeForItem params: userId={}, orderId={}, orderItemId={}, platformId={}, commission={}, completed={}",
            userId, orderId, orderItemId, platformId, commissionAmount, isItemCompleted);
        Cashback result = executeForItem(userId, orderId, orderItemId, platformId, commissionAmount, isItemCompleted);
        log.info("[DEBUG-CASHBACK] executeForItem returned: {}", result != null ? result.getId() : "NULL");
        return result;
    }

    private BigDecimal getDefaultCashbackRate(Long platformId) {
        CashbackPolicy policy = policyRepository
            .findActivePolicyFor(platformId, UserLevel.NORMAL, LocalDateTime.now())
            .orElse(null);

        if (policy != null) {
            return policy.getCashbackRate();
        }
        return new BigDecimal("70.00"); // Default 70%
    }

    /**
     * Update cashback status when order status changes.
     *
     * @param orderId Order ID
     * @param newStatus New cashback status
     * @param note Note for status change
     * @return Updated Cashback
     */
    @Transactional
    public Cashback updateCashbackStatus(Long orderId, CashbackStatus newStatus, String note) {
        log.info("UseCase: Updating cashback status for order {} to {}", orderId, newStatus);

        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));

        Cashback updatedCashback = cashback.withStatus(newStatus, note);
        updatedCashback = cashbackRepository.save(updatedCashback);

        log.info("UseCase: Updated cashback {} status to {}", updatedCashback.getId(), newStatus);

        return updatedCashback;
    }

    /**
     * Update wallet when a NEW cashback is created.
     *
     * Logic:
     * - PENDING cashback → add to pending_balance
     * - CONFIRMED cashback → add to balance and total_earned
     *
     * CRITICAL FIX: Uses direct JPQL update to bypass JPA persistence context
     * and avoid stale data issues caused by entityManager.clear() in import flow.
     *
     * @param userId User ID
     * @param amount Cashback amount
     * @param status Cashback status (PENDING or CONFIRMED)
     */
    private void updateWalletForNewCashback(Long userId, BigDecimal amount, CashbackStatus status) {
        log.info("[DEBUG-WALLET] updateWalletForNewCashback START: userId={}, amount={}, status={}", userId, amount, status);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[DEBUG-WALLET] updateWalletForNewCashback: Skipping, amount is null or <= 0: {}", amount);
            return;
        }

        // Ensure wallet exists first
        log.info("[DEBUG-WALLET] Calling ensureWalletExists for userId={}", userId);
        ensureWalletExists(userId);
        log.info("[DEBUG-WALLET] ensureWalletExists completed for userId={}", userId);

        log.info("[DEBUG-WALLET] updateWalletForNewCashback: User {}, amount {}, status {}", userId, amount, status);

        boolean success;
        if (status == CashbackStatus.PENDING) {
            // New PENDING cashback → add to pending_balance using direct JPQL update
            success = walletRepository.addPendingBalanceDirectly(userId, amount);
            log.info("updateWalletForNewCashback: Added {} to pending_balance for user {}, success={}",
                amount, userId, success);
        } else if (status == CashbackStatus.CONFIRMED) {
            // New CONFIRMED cashback → add to balance and total_earned using direct JPQL update
            success = walletRepository.addConfirmedBalanceDirectly(userId, amount);
            log.info("updateWalletForNewCashback: Added {} to balance for user {}, success={}",
                amount, userId, success);
        } else {
            log.warn("updateWalletForNewCashback: Unknown status {}, skipping wallet update", status);
            return;
        }

        if (!success) {
            log.error("updateWalletForNewCashback: Failed to update wallet for user {}", userId);
        }
    }

    /**
     * Update wallet when cashback status CHANGES.
     *
     * Logic:
     * - PENDING → CONFIRMED: subtract from pending_balance, add to balance and total_earned
     * - CONFIRMED → PENDING: subtract from balance and total_earned, add to pending_balance (rare case)
     *
     * CRITICAL FIX: Uses direct JPQL update to bypass JPA persistence context
     * and avoid stale data issues caused by entityManager.clear() in import flow.
     *
     * @param userId User ID
     * @param amount Cashback amount
     * @param oldStatus Old status
     * @param newStatus New status
     */
    private void updateWalletForStatusChange(Long userId, BigDecimal amount,
                                              CashbackStatus oldStatus, CashbackStatus newStatus) {
        log.info("[DEBUG-WALLET] updateWalletForStatusChange START: userId={}, amount={}, {} → {}",
            userId, amount, oldStatus, newStatus);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[DEBUG-WALLET] updateWalletForStatusChange: Skipping, amount is null or <= 0: {}", amount);
            return;
        }

        if (oldStatus == newStatus) {
            log.warn("[DEBUG-WALLET] updateWalletForStatusChange: Skipping, oldStatus == newStatus: {}", oldStatus);
            return; // No change
        }

        log.info("[DEBUG-WALLET] updateWalletForStatusChange: User {}, amount {}, {} → {}",
            userId, amount, oldStatus, newStatus);

        if (oldStatus == CashbackStatus.PENDING && newStatus == CashbackStatus.CONFIRMED) {
            // PENDING → CONFIRMED: move from pending_balance to balance using direct JPQL update
            // This bypasses persistence context completely, ensuring atomic DB operation
            boolean success = walletRepository.confirmPendingBalanceDirectly(userId, amount);
            log.info("updateWalletForStatusChange: PENDING→CONFIRMED for user {}, success={}", userId, success);

            if (!success) {
                log.error("updateWalletForStatusChange: Failed to confirm pending balance for user {}. " +
                    "Possible cause: insufficient pending balance or wallet not found.", userId);
            }
        } else if (oldStatus == CashbackStatus.CONFIRMED && newStatus == CashbackStatus.PENDING) {
            // CONFIRMED → PENDING: This is a rare reversal case
            // For now, log warning - full implementation would need reverseConfirmedBalanceDirectly
            log.warn("updateWalletForStatusChange: CONFIRMED→PENDING reversal for user {} - not yet implemented in direct query mode",
                userId);
        } else {
            log.warn("updateWalletForStatusChange: Unhandled status change {} → {} for user {}",
                oldStatus, newStatus, userId);
        }
    }

    /**
     * Update wallet when cashback is CANCELLED.
     *
     * Logic based on old status:
     * - PENDING → CANCELLED: subtract from pending_balance
     * - CONFIRMED → CANCELLED: subtract from balance and total_earned
     * - PAID → CANCELLED: subtract from balance and total_earned (refund)
     *
     * @param userId User ID
     * @param amount Cashback amount to reverse
     * @param oldStatus Previous cashback status before cancellation
     */
    private void updateWalletForCancellation(Long userId, BigDecimal amount, CashbackStatus oldStatus) {
        log.info("[DEBUG-WALLET] updateWalletForCancellation START: userId={}, amount={}, oldStatus={}",
            userId, amount, oldStatus);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[DEBUG-WALLET] updateWalletForCancellation: Skipping, amount is null or <= 0: {}", amount);
            return;
        }

        log.info("[DEBUG-WALLET] updateWalletForCancellation: User {}, amount {}, oldStatus {}",
            userId, amount, oldStatus);

        boolean success;
        if (oldStatus == CashbackStatus.PENDING) {
            // PENDING → CANCELLED: subtract from pending_balance
            success = walletRepository.subtractPendingBalanceDirectly(userId, amount);
            log.info("updateWalletForCancellation: PENDING→CANCELLED for user {}, success={}", userId, success);

            if (!success) {
                log.error("updateWalletForCancellation: Failed to subtract pending balance for user {}. " +
                    "Possible cause: insufficient pending balance or wallet not found.", userId);
            }
        } else if (oldStatus == CashbackStatus.CONFIRMED || oldStatus == CashbackStatus.PAID) {
            // CONFIRMED/PAID → CANCELLED: subtract from balance and total_earned (refund scenario)
            success = walletRepository.reverseConfirmedBalanceDirectly(userId, amount);
            log.info("updateWalletForCancellation: {}→CANCELLED for user {}, success={}", oldStatus, userId, success);

            if (!success) {
                log.error("updateWalletForCancellation: Failed to reverse confirmed balance for user {}. " +
                    "Possible cause: insufficient balance or wallet not found.", userId);
            }
        } else if (oldStatus == CashbackStatus.CANCELLED) {
            // Already cancelled, nothing to do
            log.warn("updateWalletForCancellation: Cashback already cancelled for user {}, skipping", userId);
        } else {
            log.warn("updateWalletForCancellation: Unhandled old status {} for user {}", oldStatus, userId);
        }
    }

    /**
     * Get existing wallet or create new one if not exists.
     */
    private UserWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
            .orElseGet(() -> {
                log.info("Wallet not found for user {}, creating new wallet", userId);
                UserWallet newWallet = UserWallet.builder()
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .totalEarned(BigDecimal.ZERO)
                    .totalWithdrawn(BigDecimal.ZERO)
                    .build();
                return walletRepository.save(newWallet);
            });
    }

    /**
     * Ensure wallet exists for user. Creates if not exists.
     * Used before direct JPQL updates that require existing wallet.
     */
    private void ensureWalletExists(Long userId) {
        if (!walletRepository.existsByUserId(userId)) {
            log.info("Wallet not found for user {}, creating new wallet", userId);
            UserWallet newWallet = UserWallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
            walletRepository.save(newWallet);
            // Flush to ensure wallet is persisted before direct JPQL update
            entityManager.flush();
        }
    }
}
