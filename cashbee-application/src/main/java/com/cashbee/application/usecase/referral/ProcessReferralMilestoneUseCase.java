package com.cashbee.application.usecase.referral;

import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Use Case: Process referral milestone when an order is completed.
 *
 * This use case is called when an order status changes to PAID.
 * It handles:
 * - Incrementing the user's completed order count
 * - Checking and granting milestone rewards
 * - Activating referral at 3 orders
 * - Upgrading user tier at milestones
 *
 * Milestones:
 * - 3 orders: 10,000 VND bonus + referral activation
 * - 10 orders: 20,000 VND bonus
 * - 40 orders: VIP tier upgrade (83% cashback)
 * - 150 orders: SUPER tier upgrade (85% cashback)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessReferralMilestoneUseCase {

    private final UserRepository userRepository;
    private final ReferralRewardRepository referralRewardRepository;

    // Milestone constants
    private static final int MILESTONE_ACTIVATION = 3;
    private static final int MILESTONE_SECOND_BONUS = 10;
    private static final int MILESTONE_VIP = 40;
    private static final int MILESTONE_SUPER = 150;

    // Bonus amounts
    private static final BigDecimal BONUS_MILESTONE_3 = new BigDecimal("10000");
    private static final BigDecimal BONUS_MILESTONE_10 = new BigDecimal("20000");

    /**
     * Execute use case when an order is completed.
     *
     * @param userId ID of the user whose order was completed
     */
    @Transactional
    public void execute(Long userId) {
        log.info("Processing referral milestone for user: {}", userId);

        // 1. Find the user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for milestone processing: {}", userId);
            return;
        }

        // 2. Increment completed orders
        user.incrementCompletedOrders();
        int completedOrders = user.getTotalCompletedOrders();

        log.debug("User {} now has {} completed orders", userId, completedOrders);

        // 3. Find referrer if user was referred
        User referrer = null;
        if (user.hasReferrer()) {
            referrer = userRepository.findByReferralCode(user.getReferredBy()).orElse(null);
            if (referrer != null) {
                log.debug("User {} was referred by: {} (id={})",
                        userId, user.getReferredBy(), referrer.getId());
            }
        }

        // 4. Process milestones
        processMilestone3(user, referrer, completedOrders);
        processMilestone10(user, referrer, completedOrders);
        processMilestone40(user, referrer, completedOrders);
        processMilestone150(user, referrer, completedOrders);

        // 5. Save user
        userRepository.save(user);

        log.info("Milestone processing completed for user: {}, orders: {}",
                userId, completedOrders);
    }

    /**
     * Process milestone 3: First activation + 10,000 VND bonus.
     */
    private void processMilestone3(User user, User referrer, int completedOrders) {
        if (completedOrders != MILESTONE_ACTIVATION) {
            return;
        }

        Long referrerId = referrer != null ? referrer.getId() : null;

        // Check if reward already granted
        if (referralRewardRepository.existsByUserIdAndMilestone(user.getId(), MILESTONE_ACTIVATION)) {
            log.debug("Milestone 3 reward already granted for user: {}", user.getId());
            return;
        }

        // Activate referral (only if user has referrer)
        if (referrer != null) {
            user.activateReferral();
            log.info("Referral activated for user: {}, referrer: {}",
                    user.getId(), referrer.getId());
        }

        // Grant bonus reward
        ReferralReward reward = ReferralReward.createMilestoneBonus(
                user.getId(),
                referrerId,
                MILESTONE_ACTIVATION,
                BONUS_MILESTONE_3
        );
        reward.grant();
        referralRewardRepository.save(reward);

        log.info("Milestone 3 bonus granted: userId={}, amount={}",
                user.getId(), BONUS_MILESTONE_3);
    }

    /**
     * Process milestone 10: 20,000 VND bonus.
     */
    private void processMilestone10(User user, User referrer, int completedOrders) {
        if (completedOrders != MILESTONE_SECOND_BONUS) {
            return;
        }

        Long referrerId = referrer != null ? referrer.getId() : null;

        // Check if reward already granted
        if (referralRewardRepository.existsByUserIdAndMilestone(user.getId(), MILESTONE_SECOND_BONUS)) {
            log.debug("Milestone 10 reward already granted for user: {}", user.getId());
            return;
        }

        // Grant bonus reward
        ReferralReward reward = ReferralReward.createMilestoneBonus(
                user.getId(),
                referrerId,
                MILESTONE_SECOND_BONUS,
                BONUS_MILESTONE_10
        );
        reward.grant();
        referralRewardRepository.save(reward);

        log.info("Milestone 10 bonus granted: userId={}, amount={}",
                user.getId(), BONUS_MILESTONE_10);
    }

    /**
     * Process milestone 40: VIP tier upgrade.
     */
    private void processMilestone40(User user, User referrer, int completedOrders) {
        if (completedOrders != MILESTONE_VIP) {
            return;
        }

        // Only upgrade if currently NORMAL
        if (user.getUserLevel() != UserLevel.NORMAL) {
            log.debug("User {} already VIP or higher, skipping milestone 40", user.getId());
            return;
        }

        Long referrerId = referrer != null ? referrer.getId() : null;

        // Check if reward already granted
        if (referralRewardRepository.existsByUserIdAndMilestone(user.getId(), MILESTONE_VIP)) {
            log.debug("Milestone 40 reward already granted for user: {}", user.getId());
            return;
        }

        // Upgrade to VIP
        user.upgradeTo(UserLevel.VIP);

        // Grant tier upgrade reward
        ReferralReward reward = ReferralReward.createTierUpgrade(
                user.getId(),
                referrerId,
                MILESTONE_VIP,
                UserLevel.VIP
        );
        reward.grant();
        referralRewardRepository.save(reward);

        log.info("Milestone 40 tier upgrade granted: userId={}, newTier=VIP",
                user.getId());
    }

    /**
     * Process milestone 150: SUPER tier upgrade.
     */
    private void processMilestone150(User user, User referrer, int completedOrders) {
        if (completedOrders != MILESTONE_SUPER) {
            return;
        }

        // Only upgrade if currently VIP
        if (user.getUserLevel() != UserLevel.VIP) {
            log.debug("User {} not VIP, cannot upgrade to SUPER at milestone 150", user.getId());
            return;
        }

        Long referrerId = referrer != null ? referrer.getId() : null;

        // Check if reward already granted
        if (referralRewardRepository.existsByUserIdAndMilestone(user.getId(), MILESTONE_SUPER)) {
            log.debug("Milestone 150 reward already granted for user: {}", user.getId());
            return;
        }

        // Upgrade to SUPER
        user.upgradeTo(UserLevel.SUPER);

        // Grant tier upgrade reward
        ReferralReward reward = ReferralReward.createTierUpgrade(
                user.getId(),
                referrerId,
                MILESTONE_SUPER,
                UserLevel.SUPER
        );
        reward.grant();
        referralRewardRepository.save(reward);

        log.info("Milestone 150 tier upgrade granted: userId={}, newTier=SUPER",
                user.getId());
    }
}
