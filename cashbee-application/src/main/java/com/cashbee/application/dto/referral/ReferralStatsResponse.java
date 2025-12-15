package com.cashbee.application.dto.referral;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for user's referral statistics.
 *
 * Contains information about:
 * - User's own referral progress (as referee)
 * - User's referral earnings (as referrer)
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralStatsResponse {

    // ===== User's Own Referral Info (As Referee) =====

    /**
     * User's own referral code (to share with others).
     */
    private String myReferralCode;

    /**
     * The referral code user was referred by (if any).
     */
    private String referredByCode;

    /**
     * Name of user who referred this user (masked).
     */
    private String referredByName;

    /**
     * User's current tier level (NORMAL, VIP, SUPER).
     */
    private String currentTier;

    /**
     * User's total completed orders.
     */
    private Integer totalCompletedOrders;

    /**
     * Next milestone for reward.
     */
    private Integer nextMilestone;

    /**
     * Orders needed to reach next milestone.
     */
    private Integer ordersToNextMilestone;

    /**
     * Description of next milestone reward.
     */
    private String nextMilestoneReward;

    // ===== Referral Earnings (As Referrer) =====

    /**
     * Total number of users referred by this user.
     */
    private Integer totalReferrals;

    /**
     * Number of active referrals (within 3 month period).
     */
    private Integer activeReferrals;

    /**
     * Total commission earned from referrals (all time).
     */
    private BigDecimal totalCommissionEarned;

    /**
     * Pending commission (not yet paid).
     */
    private BigDecimal pendingCommission;

    /**
     * Confirmed commission (ready for payout).
     */
    private BigDecimal confirmedCommission;

    /**
     * Paid commission (already withdrawn).
     */
    private BigDecimal paidCommission;

    // ===== Milestone Progress =====

    /**
     * List of milestones and their status.
     */
    private List<MilestoneProgress> milestones;

    /**
     * List of recent referral rewards received.
     */
    private List<RewardInfo> recentRewards;

    /**
     * Milestone progress info.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MilestoneProgress {
        private Integer milestone;
        private String rewardDescription;
        private boolean achieved;
        private LocalDateTime achievedAt;
    }

    /**
     * Reward info.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RewardInfo {
        private String type;
        private String description;
        private BigDecimal amount;
        private String newTier;
        private LocalDateTime grantedAt;
    }
}
