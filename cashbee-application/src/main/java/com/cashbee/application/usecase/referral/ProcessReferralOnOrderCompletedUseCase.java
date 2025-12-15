package com.cashbee.application.usecase.referral;

import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Use Case: Process referral rewards when an order is completed.
 *
 * This is the integration point that should be called when an order
 * status changes to PAID or APPROVED (completed). It handles:
 *
 * 1. Processing milestone achievements for the user
 * 2. Calculating and saving referrer commission (if applicable)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessReferralOnOrderCompletedUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final ProcessReferralMilestoneUseCase processMilestoneUseCase;
    private final CalculateReferrerCommissionUseCase calculateCommissionUseCase;

    /**
     * Process referral when order status changes to completed (APPROVED/PAID).
     *
     * @param orderId Order ID
     * @param oldStatus Previous order status
     * @param newStatus New order status
     */
    @Transactional
    public void execute(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        log.debug("Processing referral for order {} (status: {} → {})",
                orderId, oldStatus, newStatus);

        // Only process when transitioning TO PAID (final completion)
        // We use PAID as the trigger because that's when the order is truly complete
        if (newStatus != OrderStatus.PAID) {
            log.debug("Order {} not PAID yet, skipping referral processing", orderId);
            return;
        }

        // Don't reprocess if already PAID
        if (oldStatus == OrderStatus.PAID) {
            log.debug("Order {} already processed (was PAID), skipping", orderId);
            return;
        }

        // Get order details
        AffiliateOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for referral processing", orderId);
            return;
        }

        Long userId = order.getUserId();
        if (userId == null) {
            log.debug("Order {} has no user assigned, skipping referral", orderId);
            return;
        }

        log.info("Processing referral for completed order: orderId={}, userId={}",
                orderId, userId);

        // 1. Process milestone achievements (always)
        try {
            processMilestoneUseCase.execute(userId);
            log.debug("Milestone processing completed for user {}", userId);
        } catch (Exception e) {
            log.error("Error processing milestone for user {}: {}", userId, e.getMessage(), e);
            // Don't fail the whole transaction, continue with commission
        }

        // 2. Calculate referrer commission (if order has commission)
        BigDecimal commissionAmount = order.getCommissionAmount();
        if (commissionAmount != null && commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                calculateCommissionUseCase.execute(userId, orderId, commissionAmount);
                log.debug("Referrer commission processed for order {}", orderId);
            } catch (Exception e) {
                log.error("Error calculating referrer commission for order {}: {}",
                        orderId, e.getMessage(), e);
                // Don't fail the whole transaction
            }
        } else {
            log.debug("Order {} has no commission, skipping referrer commission", orderId);
        }

        log.info("Referral processing completed for order: {}", orderId);
    }

    /**
     * Process referral for a specific user and order.
     * Simpler method when order details are already known.
     *
     * @param userId User ID
     * @param orderId Order ID
     * @param originalCommission Original commission from platform
     */
    @Transactional
    public void execute(Long userId, Long orderId, BigDecimal originalCommission) {
        log.info("Processing referral: userId={}, orderId={}, commission={}",
                userId, orderId, originalCommission);

        // 1. Process milestone achievements
        try {
            processMilestoneUseCase.execute(userId);
        } catch (Exception e) {
            log.error("Error processing milestone for user {}: {}", userId, e.getMessage(), e);
        }

        // 2. Calculate referrer commission
        if (originalCommission != null && originalCommission.compareTo(BigDecimal.ZERO) > 0) {
            try {
                calculateCommissionUseCase.execute(userId, orderId, originalCommission);
            } catch (Exception e) {
                log.error("Error calculating referrer commission: {}", e.getMessage(), e);
            }
        }

        log.info("Referral processing completed: userId={}, orderId={}", userId, orderId);
    }
}
