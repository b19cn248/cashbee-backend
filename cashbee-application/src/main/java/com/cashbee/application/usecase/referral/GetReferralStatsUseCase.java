package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ReferralStatsResponse;
import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.MilestoneConfigRepository;
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
    private final MilestoneConfigRepository milestoneConfigRepository;

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

        // Determine milestone type based on user's referrer status
        MilestoneType milestoneType = user.hasReferrer()
                ? MilestoneType.WITH_REFERRER
                : MilestoneType.WITHOUT_REFERRER;

        // Get milestone configs from database
        List<MilestoneConfig> milestoneConfigs = milestoneConfigRepository.findActiveByMilestoneType(milestoneType);

        // Next milestone calculation
        int completedOrders = user.getTotalCompletedOrders() != null ? user.getTotalCompletedOrders() : 0;
        MilestoneConfig nextMilestoneConfig = calculateNextMilestone(completedOrders, milestoneConfigs);
        int nextMilestone = nextMilestoneConfig != null ? nextMilestoneConfig.getOrdersRequired() : 0;
        builder.nextMilestone(nextMilestone)
                .ordersToNextMilestone(nextMilestone > 0 ? nextMilestone - completedOrders : 0)
                .nextMilestoneReward(getNextMilestoneReward(nextMilestoneConfig));

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
                completedOrders, achievedMilestones, rewards, milestoneConfigs);
        builder.milestones(milestoneProgress);

        // Recent rewards
        List<ReferralStatsResponse.RewardInfo> recentRewards = buildRecentRewards(rewards);
        builder.recentRewards(recentRewards);

        log.debug("Referral stats compiled for user: {}", user.getId());
        return builder.build();
    }

    /**
     * Calculate the next milestone based on completed orders.
     *
     * @param completedOrders Number of orders the user has completed
     * @param milestoneConfigs List of milestone configs from database
     * @return MilestoneConfig for next milestone, or null if all achieved
     */
    private MilestoneConfig calculateNextMilestone(int completedOrders, List<MilestoneConfig> milestoneConfigs) {
        return milestoneConfigs.stream()
                .filter(config -> completedOrders < config.getOrdersRequired())
                .min((a, b) -> Integer.compare(a.getOrdersRequired(), b.getOrdersRequired()))
                .orElse(null);
    }

    /**
     * Get description of next milestone reward from config.
     *
     * @param milestoneConfig The milestone config, or null if all achieved
     * @return Description of the reward
     */
    private String getNextMilestoneReward(MilestoneConfig milestoneConfig) {
        if (milestoneConfig == null) {
            return "All milestones achieved!";
        }

        // Build description from config
        StringBuilder description = new StringBuilder();

        if (milestoneConfig.hasRefereeBonus()) {
            description.append(formatAmount(milestoneConfig.getRefereeBonus())).append(" bonus");
        }

        if (milestoneConfig.activatesReferralCommission()) {
            if (!description.isEmpty()) {
                description.append(" + ");
            }
            description.append("Referral activation (")
                    .append(milestoneConfig.getCommissionMonths())
                    .append(" months)");
        }

        if (milestoneConfig.hasTierUpgrade()) {
            if (!description.isEmpty()) {
                description.append(" + ");
            }
            description.append(milestoneConfig.getNewTier().name())
                    .append(" tier upgrade");
        }

        return description.isEmpty() ? milestoneConfig.getDescription() : description.toString();
    }

    /**
     * Format amount for display.
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
    }

    /**
     * Build milestone progress list from database configs.
     *
     * @param completedOrders User's completed orders
     * @param achievedMilestones Set of milestone numbers already achieved
     * @param rewards List of user's referral rewards
     * @param milestoneConfigs List of milestone configs from database
     * @return List of milestone progress for display
     */
    private List<ReferralStatsResponse.MilestoneProgress> buildMilestoneProgress(
            int completedOrders,
            Set<Integer> achievedMilestones,
            List<ReferralReward> rewards,
            List<MilestoneConfig> milestoneConfigs) {

        // Sort configs by ordersRequired ascending
        List<MilestoneConfig> sortedConfigs = milestoneConfigs.stream()
                .sorted((a, b) -> Integer.compare(a.getOrdersRequired(), b.getOrdersRequired()))
                .toList();

        List<ReferralStatsResponse.MilestoneProgress> progress = new ArrayList<>();

        for (MilestoneConfig config : sortedConfigs) {
            int milestone = config.getOrdersRequired();
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
                    .rewardDescription(buildMilestoneDescription(config))
                    .achieved(achieved)
                    .achievedAt(achievedAt)
                    .build());
        }

        return progress;
    }

    /**
     * Build description for a milestone config.
     */
    private String buildMilestoneDescription(MilestoneConfig config) {
        StringBuilder description = new StringBuilder();

        if (config.hasRefereeBonus()) {
            description.append(formatAmount(config.getRefereeBonus())).append(" bonus");
        }

        if (config.activatesReferralCommission()) {
            if (!description.isEmpty()) {
                description.append(" + ");
            }
            description.append("Referral activation");
        }

        if (config.hasTierUpgrade()) {
            if (!description.isEmpty()) {
                description.append(" + ");
            }
            description.append(config.getNewTier().name()).append(" tier");
        }

        // Fallback to database description if no specific rewards
        if (description.isEmpty() && config.getDescription() != null) {
            return config.getDescription();
        }

        return description.isEmpty() ? "Milestone reward" : description.toString();
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
