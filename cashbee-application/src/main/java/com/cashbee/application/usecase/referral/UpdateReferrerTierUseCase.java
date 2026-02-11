package com.cashbee.application.usecase.referral;

import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use Case: Update referrer tier based on their activated referral count.
 *
 * This use case should be called:
 * - When a referee reaches activation milestone (3 orders)
 * - During periodic batch jobs to sync tier levels
 *
 * Tier Levels:
 * - BRONZE (default): 0+ referrals, 5% commission
 * - SILVER: 5+ referrals, 7% commission
 * - GOLD: 20+ referrals, 10% commission
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateReferrerTierUseCase {

    private static final String DEFAULT_TIER = "BRONZE";

    private final UserRepository userRepository;
    private final ReferrerTierConfigRepository tierConfigRepository;

    /**
     * Execute use case to update referrer tier.
     *
     * @param referrerId ID of the referrer to update
     * @return true if tier was updated, false otherwise
     */
    @Transactional
    public boolean execute(Long referrerId) {
        log.debug("Checking tier update for referrer: {}", referrerId);

        // 1. Find the referrer
        User referrer = userRepository.findById(referrerId).orElse(null);
        if (referrer == null) {
            log.warn("Referrer not found: {}", referrerId);
            return false;
        }

        // 2. Get current activated referrals count
        int activatedReferrals = referrer.getActivatedReferralsCount();
        String currentTier = referrer.getReferrerTier();
        if (currentTier == null) {
            currentTier = DEFAULT_TIER;
        }

        // 3. Find appropriate tier based on referral count
        Optional<ReferrerTierConfig> tierConfigOpt = tierConfigRepository.findTierByReferralCount(activatedReferrals);
        if (tierConfigOpt.isEmpty()) {
            log.debug("No tier config found for referral count: {}", activatedReferrals);
            return false;
        }

        ReferrerTierConfig tierConfig = tierConfigOpt.get();
        String newTier = tierConfig.getTierName();

        // 4. Check if tier changed
        if (currentTier.equals(newTier)) {
            log.debug("Referrer tier unchanged: referrerId={}, tier={}", referrerId, currentTier);
            return false;
        }

        // 5. Update tier
        String previousTier = currentTier;
        referrer.updateReferrerTier(newTier);
        userRepository.save(referrer);

        log.info("Referrer tier updated: referrerId={}, previousTier={}, newTier={}, " +
                        "activatedReferrals={}, newCommissionRate={}%",
                referrerId, previousTier, newTier,
                activatedReferrals, tierConfig.getCommissionRate());

        return true;
    }

    /**
     * Execute use case to increment activated referrals and update tier.
     * Called when a referee reaches activation milestone.
     *
     * @param referrerId ID of the referrer
     * @return true if tier was updated
     */
    @Transactional
    public boolean incrementAndUpdateTier(Long referrerId) {
        log.debug("Incrementing activated referrals for referrer: {}", referrerId);

        // 1. Find the referrer
        User referrer = userRepository.findById(referrerId).orElse(null);
        if (referrer == null) {
            log.warn("Referrer not found: {}", referrerId);
            return false;
        }

        // 2. Increment activated referrals
        referrer.incrementActivatedReferrals();
        int newCount = referrer.getActivatedReferralsCount();

        // 3. Check if tier should be updated
        String currentTier = referrer.getReferrerTier();
        if (currentTier == null) {
            currentTier = DEFAULT_TIER;
        }

        Optional<ReferrerTierConfig> tierConfigOpt = tierConfigRepository.findTierByReferralCount(newCount);
        if (tierConfigOpt.isEmpty()) {
            // Just save the incremented count
            userRepository.save(referrer);
            log.debug("Referrer activated referrals incremented: referrerId={}, count={}",
                    referrerId, newCount);
            return false;
        }

        ReferrerTierConfig tierConfig = tierConfigOpt.get();
        String newTier = tierConfig.getTierName();

        // 4. Update tier if changed
        boolean tierChanged = !currentTier.equals(newTier);
        if (tierChanged) {
            referrer.updateReferrerTier(newTier);
            log.info("Referrer tier upgraded: referrerId={}, previousTier={}, newTier={}, " +
                            "activatedReferrals={}, newCommissionRate={}%",
                    referrerId, currentTier, newTier,
                    newCount, tierConfig.getCommissionRate());
        }

        // 5. Save changes
        userRepository.save(referrer);

        if (!tierChanged) {
            log.debug("Referrer activated referrals incremented: referrerId={}, count={}, tier={}",
                    referrerId, newCount, currentTier);
        }

        return tierChanged;
    }
}
