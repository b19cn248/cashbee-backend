package com.cashbee.application.usecase.cashback;

import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.CashbackPolicy;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import com.cashbee.domain.repository.CashbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
    @Transactional
    public Cashback executeForItem(Long userId, Long orderId, Long orderItemId, Long platformId,
                                   BigDecimal commissionAmount, boolean isItemCompleted) {

        log.info("UseCase: Calculating cashback for item {} (order: {}, user: {}, commission: {}, completed: {})",
            orderItemId, orderId, userId, commissionAmount, isItemCompleted);

        // Validate inputs
        if (orderItemId == null || orderItemId <= 0) {
            throw new IllegalArgumentException("Invalid order item ID: " + orderItemId);
        }

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

        log.info("UseCase: Created cashback {} for item {} with amount {} VND (status: {})",
            savedCashback.getId(), orderItemId, savedCashback.getCashbackAmount(), savedCashback.getStatus());

        return savedCashback;
    }

    /**
     * Upsert cashback for an order item (create if not exists, update if exists).
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param orderItemId Order Item ID
     * @param platformId Platform ID
     * @param commissionAmount Commission amount from platform (VND)
     * @param isItemCompleted Whether item is already completed
     * @return Created or updated Cashback
     */
    @Transactional
    public Cashback upsertForItem(Long userId, Long orderId, Long orderItemId, Long platformId,
                                  BigDecimal commissionAmount, boolean isItemCompleted) {

        log.info("UseCase: Upserting cashback for item {} (order: {}, completed: {})",
            orderItemId, orderId, isItemCompleted);

        var existingCashback = cashbackRepository.findByOrderItemId(orderItemId);

        if (existingCashback.isPresent()) {
            Cashback cashback = existingCashback.get();
            CashbackStatus newStatus = isItemCompleted ? CashbackStatus.CONFIRMED : CashbackStatus.PENDING;

            // Only update if status changed
            if (cashback.getStatus() != newStatus) {
                log.info("UseCase: Updating cashback {} status from {} to {}",
                    cashback.getId(), cashback.getStatus(), newStatus);

                Cashback updatedCashback = cashback.withStatus(newStatus,
                    isItemCompleted ? "Item completed, cashback confirmed" : "Item status changed to pending");
                return cashbackRepository.save(updatedCashback);
            }

            log.info("UseCase: Cashback {} already has status {}, no update needed",
                cashback.getId(), cashback.getStatus());
            return cashback;
        }

        // Create new cashback
        return executeForItem(userId, orderId, orderItemId, platformId, commissionAmount, isItemCompleted);
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
}
