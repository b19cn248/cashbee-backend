package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ReferralStatsResponse;
import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use Case: Get referral statistics for a user.
 *
 * Returns comprehensive referral information including:
 * - User's own referral progress (as referee)
 * - User's referral earnings (as referrer)
 * - Milestone progress
 * - Recent rewards
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetReferralStatsUseCase {

    private final UserRepository userRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final ReferrerCommissionRepository referrerCommissionRepository;

    // Milestones for referee rewards
    private static final int[] MILESTONES = {3, 10, 40, 150};
    private static final String[] MILESTONE_REWARDS = {
            "10,000 VND bonus + Referral activation",
            "20,000 VND bonus",
            "VIP tier upgrade (83% cashback)",
            "SUPER tier upgrade (85% cashback)"
    };

    /**
     * Execute use case to get referral statistics.
     *
     * @param keycloakId Keycloak user ID (from JWT)
     * @return ReferralStatsResponse with all referral data
     */
    @Transactional(readOnly = true)
    public ReferralStatsResponse execute(String keycloakId) {
        log.debug("Getting referral stats for user: keycloakId={}", keycloakId);

        // 1. Find the user
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    log.error("User not found: keycloakId={}", keycloakId);
                    return new RuntimeException("User not found: " + keycloakId);
                });

        log.debug("Found user: id={}, username={}", user.getId(), user.getUsername());

        // 2. Build response
        ReferralStatsResponse.ReferralStatsResponseBuilder builder = ReferralStatsResponse.builder();

        // User's own info
        builder.myReferralCode(user.getReferralCode())
                .currentTier(user.getUserLevel().name())
                .totalCompletedOrders(user.getTotalCompletedOrders());

        // Referrer info (if user was referred)
        if (user.hasReferrer()) {
            builder.referredByCode(user.getReferredBy());
            userRepository.findByReferralCode(user.getReferredBy())
                    .ifPresent(referrer -> {
                        builder.referredByName(maskName(referrer.getFullName(), referrer.getUsername()));
                    });
        }

        // Next milestone calculation
        int completedOrders = user.getTotalCompletedOrders() != null ? user.getTotalCompletedOrders() : 0;
        int nextMilestone = calculateNextMilestone(completedOrders);
        builder.nextMilestone(nextMilestone)
                .ordersToNextMilestone(nextMilestone > 0 ? nextMilestone - completedOrders : 0)
                .nextMilestoneReward(getNextMilestoneReward(nextMilestone));

        // Referral earnings (as referrer)
        List<User> referrals = userRepository.findByReferredBy(user.getReferralCode());
        builder.totalReferrals(referrals.size());

        // Count active referrals (within 3 month period)
        long activeReferrals = referrals.stream()
                .filter(User::isWithinReferralPeriod)
                .count();
        builder.activeReferrals((int) activeReferrals);

        // Commission totals
        builder.totalCommissionEarned(referrerCommissionRepository.sumCommissionByReferrerId(user.getId()))
                .pendingCommission(referrerCommissionRepository.sumPendingCommissionByReferrerId(user.getId()))
                .confirmedCommission(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(user.getId()))
                .paidCommission(referrerCommissionRepository.sumPaidCommissionByReferrerId(user.getId()));

        // Milestone progress
        List<ReferralReward> rewards = referralRewardRepository.findByUserId(user.getId());
        Set<Integer> achievedMilestones = rewards.stream()
                .filter(r -> r.getStatus() == ReferralRewardStatus.GRANTED)
                .map(ReferralReward::getMilestone)
                .collect(Collectors.toSet());

        List<ReferralStatsResponse.MilestoneProgress> milestoneProgress = buildMilestoneProgress(
                completedOrders, achievedMilestones, rewards);
        builder.milestones(milestoneProgress);

        // Recent rewards
        List<ReferralStatsResponse.RewardInfo> recentRewards = buildRecentRewards(rewards);
        builder.recentRewards(recentRewards);

        log.debug("Referral stats compiled for user: {}", user.getId());
        return builder.build();
    }

    /**
     * Calculate the next milestone based on completed orders.
     */
    private int calculateNextMilestone(int completedOrders) {
        for (int milestone : MILESTONES) {
            if (completedOrders < milestone) {
                return milestone;
            }
        }
        return 0; // All milestones achieved
    }

    /**
     * Get description of next milestone reward.
     */
    private String getNextMilestoneReward(int nextMilestone) {
        for (int i = 0; i < MILESTONES.length; i++) {
            if (MILESTONES[i] == nextMilestone) {
                return MILESTONE_REWARDS[i];
            }
        }
        return "All milestones achieved!";
    }

    /**
     * Build milestone progress list.
     */
    private List<ReferralStatsResponse.MilestoneProgress> buildMilestoneProgress(
            int completedOrders,
            Set<Integer> achievedMilestones,
            List<ReferralReward> rewards) {

        List<ReferralStatsResponse.MilestoneProgress> progress = new ArrayList<>();

        for (int i = 0; i < MILESTONES.length; i++) {
            int milestone = MILESTONES[i];
            boolean achieved = achievedMilestones.contains(milestone);

            LocalDateTime achievedAt = null;
            if (achieved) {
                achievedAt = rewards.stream()
                        .filter(r -> r.getMilestone().equals(milestone))
                        .map(ReferralReward::getGrantedAt)
                        .findFirst()
                        .orElse(null);
            }

            progress.add(ReferralStatsResponse.MilestoneProgress.builder()
                    .milestone(milestone)
                    .rewardDescription(MILESTONE_REWARDS[i])
                    .achieved(achieved)
                    .achievedAt(achievedAt)
                    .build());
        }

        return progress;
    }

    /**
     * Build recent rewards list.
     */
    private List<ReferralStatsResponse.RewardInfo> buildRecentRewards(List<ReferralReward> rewards) {
        return rewards.stream()
                .filter(r -> r.getStatus() == ReferralRewardStatus.GRANTED)
                .sorted((a, b) -> {
                    if (a.getGrantedAt() == null) return 1;
                    if (b.getGrantedAt() == null) return -1;
                    return b.getGrantedAt().compareTo(a.getGrantedAt());
                })
                .limit(5)
                .map(r -> ReferralStatsResponse.RewardInfo.builder()
                        .type(r.getRewardType().name())
                        .description(buildRewardDescription(r))
                        .amount(r.getAmount())
                        .newTier(r.getNewTier() != null ? r.getNewTier().name() : null)
                        .grantedAt(r.getGrantedAt())
                        .build())
                .toList();
    }

    /**
     * Build description for a reward.
     */
    private String buildRewardDescription(ReferralReward reward) {
        if (reward.getRewardType() == ReferralRewardType.MILESTONE_BONUS) {
            return String.format("Milestone %d bonus: %,d VND",
                    reward.getMilestone(),
                    reward.getAmount() != null ? reward.getAmount().intValue() : 0);
        } else {
            return String.format("Milestone %d: Upgraded to %s tier",
                    reward.getMilestone(),
                    reward.getNewTier() != null ? reward.getNewTier().name() : "");
        }
    }

    /**
     * Mask a name for privacy.
     */
    private String maskName(String fullName, String username) {
        String nameToMask = (fullName != null && !fullName.isBlank()) ? fullName : username;

        if (nameToMask == null || nameToMask.length() <= 2) {
            return "***";
        }

        String visible = nameToMask.substring(0, 2);
        int maskLength = Math.min(nameToMask.length() - 2, 8);
        return visible + "*".repeat(maskLength);
    }
}
