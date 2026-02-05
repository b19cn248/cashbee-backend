package com.cashbee.domain.model;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.enums.UserLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ReferralReward Domain Model.
 * Tracks milestone bonuses and tier upgrades granted to users.
 *
 * <p>Reward types:
 * <ul>
 *   <li>MILESTONE_BONUS: Cash bonus for reaching order milestones (3 orders = 10k, 10 orders = 20k)</li>
 *   <li>TIER_UPGRADE: Tier upgrade for reaching milestones (40 orders = VIP, 150 orders = SUPER)</li>
 * </ul>
 *
 * <p>This model is a pure POJO with NO JPA annotations.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class ReferralReward {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * User who receives the reward (referee).
     * Foreign key to user table.
     */
    private Long userId;

    /**
     * User who referred (referrer).
     * Foreign key to user table.
     * Can be null if user has no referrer.
     */
    private Long referrerId;

    /**
     * Type of reward: MILESTONE_BONUS or TIER_UPGRADE.
     */
    private ReferralRewardType rewardType;

    /**
     * Milestone reached: 3, 10, 40, or 150.
     */
    private Integer milestone;

    /**
     * Bonus amount for MILESTONE_BONUS type.
     * 10,000 VND for 3 orders, 20,000 VND for 10 orders.
     */
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * New tier for TIER_UPGRADE type.
     * VIP for 40 orders, SUPER for 150 orders.
     */
    private UserLevel newTier;

    /**
     * Reward status: PENDING or GRANTED.
     */
    @Builder.Default
    private ReferralRewardStatus status = ReferralRewardStatus.GRANTED;

    /**
     * Timestamp when reward was granted.
     */
    private LocalDateTime grantedAt;

    /**
     * Timestamp when record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Reference to batch_transfer_export that paid this reward.
     * NULL = unpaid (bonus in wallet but not transferred yet).
     */
    private Long paidBatchId;

    /**
     * Timestamp when reward was paid via batch transfer.
     * NULL = unpaid.
     */
    private LocalDateTime paidAt;

    // ===== Business Logic Methods =====

    /**
     * Check if this is a milestone bonus reward.
     *
     * @return true if reward type is MILESTONE_BONUS
     */
    public boolean isMilestoneBonus() {
        return this.rewardType == ReferralRewardType.MILESTONE_BONUS;
    }

    /**
     * Check if this is a tier upgrade reward.
     *
     * @return true if reward type is TIER_UPGRADE
     */
    public boolean isTierUpgrade() {
        return this.rewardType == ReferralRewardType.TIER_UPGRADE;
    }

    /**
     * Check if reward has been granted.
     *
     * @return true if status is GRANTED
     */
    public boolean isGranted() {
        return this.status == ReferralRewardStatus.GRANTED;
    }

    /**
     * Grant the reward.
     * Sets status to GRANTED and records the timestamp.
     */
    public void grant() {
        this.status = ReferralRewardStatus.GRANTED;
        this.grantedAt = LocalDateTime.now();
    }

    /**
     * Check if this reward has been paid via batch transfer.
     *
     * @return true if paidBatchId is set (reward has been paid)
     */
    public boolean isPaid() {
        return this.paidBatchId != null;
    }

    /**
     * Mark this reward as paid via batch transfer.
     *
     * @param batchId ID of the batch that paid this reward
     */
    public void markAsPaid(Long batchId) {
        this.paidBatchId = batchId;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Check if this is a new reward (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Validate reward data.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (this.userId == null) {
            throw new IllegalStateException("User ID is required");
        }
        if (this.rewardType == null) {
            throw new IllegalStateException("Reward type is required");
        }
        if (this.milestone == null) {
            throw new IllegalStateException("Milestone is required");
        }
        if (this.status == null) {
            throw new IllegalStateException("Status is required");
        }

        // Validate milestone values
        if (!isValidMilestone(this.milestone)) {
            throw new IllegalStateException("Invalid milestone value: " + this.milestone);
        }

        // Validate amount for MILESTONE_BONUS
        if (isMilestoneBonus() && (this.amount == null || this.amount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalStateException("Amount is required for MILESTONE_BONUS");
        }

        // Validate new tier for TIER_UPGRADE
        if (isTierUpgrade() && this.newTier == null) {
            throw new IllegalStateException("New tier is required for TIER_UPGRADE");
        }
    }

    /**
     * Check if milestone value is valid.
     * Valid milestones: 3 (10k bonus), 10 (20k bonus), 40 (VIP), 150 (SUPER).
     *
     * @param milestone milestone value to check
     * @return true if valid
     */
    private boolean isValidMilestone(Integer milestone) {
        return milestone == 3 || milestone == 10 || milestone == 40 || milestone == 150;
    }

    // ===== Factory Methods =====

    /**
     * Create a milestone bonus reward.
     *
     * @param userId     user receiving the bonus
     * @param referrerId user who referred (can be null)
     * @param milestone  milestone reached (3 or 10)
     * @param amount     bonus amount
     * @return new ReferralReward instance
     */
    public static ReferralReward createMilestoneBonus(Long userId, Long referrerId,
                                                       Integer milestone, BigDecimal amount) {
        return ReferralReward.builder()
                .userId(userId)
                .referrerId(referrerId)
                .rewardType(ReferralRewardType.MILESTONE_BONUS)
                .milestone(milestone)
                .amount(amount)
                .status(ReferralRewardStatus.GRANTED)
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a tier upgrade reward.
     *
     * @param userId     user receiving the upgrade
     * @param referrerId user who referred (can be null)
     * @param milestone  milestone reached (40 or 150)
     * @param newTier    new tier level
     * @return new ReferralReward instance
     */
    public static ReferralReward createTierUpgrade(Long userId, Long referrerId,
                                                    Integer milestone, UserLevel newTier) {
        return ReferralReward.builder()
                .userId(userId)
                .referrerId(referrerId)
                .rewardType(ReferralRewardType.TIER_UPGRADE)
                .milestone(milestone)
                .newTier(newTier)
                .amount(BigDecimal.ZERO)
                .status(ReferralRewardStatus.GRANTED)
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
