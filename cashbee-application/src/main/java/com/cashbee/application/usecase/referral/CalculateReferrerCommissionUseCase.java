package com.cashbee.application.usecase.referral;

import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use Case: Calculate and save referrer commission for a completed order.
 *
 * This use case is called when an order is confirmed/paid.
 * It calculates 5% of the original Shopee commission and saves it
 * as pending commission for the referrer.
 *
 * Business rules:
 * - Referee must have a referrer (referredBy is set)
 * - Referral must be activated (3+ orders completed)
 * - Must be within 3-month commission period
 * - Original commission must be greater than zero
 * - Commission is 5% of original Shopee commission
 * - App pays this commission (not deducted from referee)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateReferrerCommissionUseCase {

    private final UserRepository userRepository;
    private final ReferrerCommissionRepository referrerCommissionRepository;

    /**
     * Execute use case to calculate and save referrer commission.
     *
     * @param refereeUserId ID of the referee whose order was completed
     * @param orderId ID of the completed order
     * @param originalCommission Original commission amount from platform (Shopee)
     */
    @Transactional
    public void execute(Long refereeUserId, Long orderId, BigDecimal originalCommission) {
        log.debug("Calculating referrer commission: refereeId={}, orderId={}, originalCommission={}",
                refereeUserId, orderId, originalCommission);

        // Validate original commission
        if (originalCommission == null || originalCommission.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Original commission is zero or null, skipping: orderId={}", orderId);
            return;
        }

        // 1. Find the referee
        User referee = userRepository.findById(refereeUserId).orElse(null);
        if (referee == null) {
            log.warn("Referee not found for commission calculation: {}", refereeUserId);
            return;
        }

        // 2. Check if referee has a referrer
        if (!referee.hasReferrer()) {
            log.debug("Referee has no referrer, skipping commission: refereeId={}", refereeUserId);
            return;
        }

        // 3. Check if referral is activated
        if (!referee.isReferralActivated()) {
            log.debug("Referral not yet activated, skipping commission: refereeId={}", refereeUserId);
            return;
        }

        // 4. Check if within commission period (3 months)
        if (!referee.isWithinReferralPeriod()) {
            log.debug("Referral period expired, skipping commission: refereeId={}, expiresAt={}",
                    refereeUserId, referee.getReferralExpiresAt());
            return;
        }

        // 5. Find the referrer
        User referrer = userRepository.findByReferralCode(referee.getReferredBy()).orElse(null);
        if (referrer == null) {
            log.warn("Referrer not found: code={}", referee.getReferredBy());
            return;
        }

        // 6. Check if referrer is active (optional - we might still create commission)
        if (!referrer.isActive()) {
            log.warn("Referrer is inactive, skipping commission: referrerId={}, status={}",
                    referrer.getId(), referrer.getStatus());
            return;
        }

        // 7. Check for duplicate commission
        if (referrerCommissionRepository.existsBySourceOrderId(orderId)) {
            log.debug("Commission already exists for order: {}", orderId);
            return;
        }

        // 8. Get referral expiration date
        LocalDateTime expiresAt = referee.getReferralExpiresAt();

        // 9. Create commission
        ReferrerCommission commission = ReferrerCommission.create(
                referrer.getId(),
                referee.getId(),
                orderId,
                originalCommission,
                expiresAt
        );

        // 10. Save commission
        referrerCommissionRepository.save(commission);

        log.info("Referrer commission created: referrerId={}, refereeId={}, orderId={}, " +
                        "originalCommission={}, commissionAmount={}, expiresAt={}",
                referrer.getId(), referee.getId(), orderId,
                originalCommission, commission.getCommissionAmount(), expiresAt);
    }
}
