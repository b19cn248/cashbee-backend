package com.cashbee.application.usecase.cashback;

import com.cashbee.application.usecase.referral.PayReferrerCommissionUseCase;
import com.cashbee.application.usecase.referral.ProcessReferralOnOrderCompletedUseCase;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.repository.CashbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating cashback when order status changes.
 *
 * Business Purpose:
 * When re-importing CSV file, order status may change (e.g., PENDING → APPROVED → PAID).
 * This use case handles cashback updates based on status transitions.
 *
 * Status Transitions & Actions:
 *
 * 1. PENDING → APPROVED/PAID:
 *    - Move cashback from pending_balance → balance
 *    - Update cashback status: PENDING → CONFIRMED → PAID
 *
 * 2. PENDING → CANCELLED:
 *    - Cancel cashback
 *    - Remove from pending_balance
 *
 * 3. APPROVED → PAID:
 *    - Already in balance, just update status
 *
 * 4. APPROVED → CANCELLED:
 *    - Rollback: Remove from balance
 *    - Update cashback status to CANCELLED
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateCashbackOnOrderStatusChangeUseCase {

    private final CashbackRepository cashbackRepository;
    private final AddCashbackToWalletUseCase addCashbackToWalletUseCase;
    private final ProcessReferralOnOrderCompletedUseCase processReferralUseCase;
    private final PayReferrerCommissionUseCase payReferrerCommissionUseCase;

    /**
     * Update cashback when order status changes.
     *
     * @param orderId Order ID
     * @param oldStatus Old order status
     * @param newStatus New order status
     */
    @Transactional
    public void execute(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        log.info("UseCase: Updating cashback for order {} (status: {} → {})",
            orderId, oldStatus, newStatus);

        // Skip if status unchanged
        if (oldStatus == newStatus) {
            log.debug("UseCase: Order status unchanged, skipping cashback update");
            return;
        }

        // Get cashback for this order
        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElse(null);

        // If no cashback exists, skip (order may have no tracking code)
        if (cashback == null) {
            log.debug("UseCase: No cashback found for order {}, skipping", orderId);
            return;
        }

        // Handle different status transitions
        handleStatusTransition(orderId, oldStatus, newStatus, cashback);

        // Process referral rewards when order is completed (APPROVED or PAID)
        processReferralRewards(orderId, oldStatus, newStatus);
    }

    /**
     * Process referral rewards when order status changes to completed.
     */
    private void processReferralRewards(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            processReferralUseCase.execute(orderId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to process referral for order {}: {}", orderId, e.getMessage(), e);
            // Don't fail the cashback update if referral processing fails
        }
    }

    /**
     * Pay referrer commission when referee's cashback is confirmed.
     * This adds the 5% commission to the referrer's wallet balance.
     */
    private void payReferrerCommission(Long orderId) {
        try {
            payReferrerCommissionUseCase.execute(orderId);
        } catch (Exception e) {
            log.error("Failed to pay referrer commission for order {}: {}", orderId, e.getMessage(), e);
            // Don't fail the cashback confirmation if commission payment fails
        }
    }

    /**
     * Handle different status transition scenarios.
     */
    private void handleStatusTransition(Long orderId, OrderStatus oldStatus,
                                       OrderStatus newStatus, Cashback cashback) {

        // PENDING → APPROVED or PAID (order completed)
        if (oldStatus == OrderStatus.PENDING &&
            (newStatus == OrderStatus.APPROVED || newStatus == OrderStatus.PAID)) {

            log.info("UseCase: Order {} completed (PENDING → {}), confirming cashback",
                orderId, newStatus);

            // Move from pending_balance → balance
            addCashbackToWalletUseCase.confirmCashbackForOrder(orderId);

            // Pay referrer commission when referee's cashback is confirmed
            payReferrerCommission(orderId);

            log.info("UseCase: Successfully confirmed cashback for order {}", orderId);
            return;
        }

        // PENDING → CANCELLED (order cancelled before completion)
        if (oldStatus == OrderStatus.PENDING && newStatus == OrderStatus.CANCELLED) {
            log.info("UseCase: Order {} cancelled (PENDING → CANCELLED), cancelling cashback",
                orderId);

            addCashbackToWalletUseCase.cancelCashbackForOrder(orderId);

            log.info("UseCase: Successfully cancelled cashback for order {}", orderId);
            return;
        }

        // APPROVED → PAID (already confirmed, just status update)
        if (oldStatus == OrderStatus.APPROVED && newStatus == OrderStatus.PAID) {
            log.debug("UseCase: Order {} status updated APPROVED → PAID, cashback already confirmed",
                orderId);
            // No action needed - cashback already in balance
            return;
        }

        // APPROVED → CANCELLED (rare case: order was approved then cancelled)
        if (oldStatus == OrderStatus.APPROVED && newStatus == OrderStatus.CANCELLED) {
            log.warn("UseCase: Order {} cancelled after approval (APPROVED → CANCELLED)", orderId);

            // Cancel cashback (will fail if already paid)
            addCashbackToWalletUseCase.cancelCashbackForOrder(orderId);

            log.info("UseCase: Successfully cancelled cashback for order {}", orderId);
            return;
        }

        // PAID → CANCELLED (refund scenario: order was paid then cancelled)
        if (oldStatus == OrderStatus.PAID && newStatus == OrderStatus.CANCELLED) {
            log.warn("UseCase: Order {} cancelled after payment (PAID → CANCELLED), reversing cashback",
                orderId);

            // Reverse paid cashback (will subtract from balance and totalEarned)
            addCashbackToWalletUseCase.reversePaidCashbackForOrder(orderId);

            log.info("UseCase: Successfully reversed paid cashback for order {}", orderId);
            return;
        }

        // Other transitions: No action needed
        log.debug("UseCase: No cashback action required for transition {} → {}",
            oldStatus, newStatus);
    }
}
