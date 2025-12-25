package com.cashbee.domain.model;

import com.cashbee.domain.enums.MilestoneType;
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
 * MilestoneConfig Domain Model.
 *
 * Configures milestone rewards based on completed orders.
 * Different configurations for users WITH_REFERRER and WITHOUT_REFERRER.
 *
 * Example configurations:
 * - WITH_REFERRER, 5 orders: referee +10k, referrer +20k, activate 5-month commission
 * - WITH_REFERRER, 10 orders: referee +20k
 * - WITH_REFERRER, 80 orders: upgrade to VIP
 * - WITHOUT_REFERRER, 80 orders: upgrade to VIP
 *
 * This model is a pure POJO with NO JPA annotations.
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
public class MilestoneConfig {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Type of milestone: WITH_REFERRER or WITHOUT_REFERRER.
     */
    private MilestoneType milestoneType;

    /**
     * Number of completed orders required to reach this milestone.
     */
    private Integer ordersRequired;

    /**
     * Bonus amount for referee (user who completes orders).
     */
    @Builder.Default
    private BigDecimal refereeBonus = BigDecimal.ZERO;

    /**
     * Bonus amount for referrer (user who shared the code).
     */
    @Builder.Default
    private BigDecimal referrerBonus = BigDecimal.ZERO;

    /**
     * New tier to upgrade to (VIP, SUPER) or null if no tier change.
     */
    private UserLevel newTier;

    /**
     * Duration in months for referrer to receive commission.
     * Only applicable for activation milestone (e.g., 5 months).
     */
    @Builder.Default
    private Integer commissionMonths = 0;

    /**
     * Whether this milestone activates referral commission.
     */
    @Builder.Default
    private Boolean activatesReferral = false;

    /**
     * Description for display/logging purposes.
     */
    private String description;

    /**
     * Whether this configuration is active.
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Check if this milestone grants a bonus to the referee.
     *
     * @return true if referee_bonus > 0
     */
    public boolean hasRefereeBonus() {
        return refereeBonus != null && refereeBonus.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this milestone grants a bonus to the referrer.
     *
     * @return true if referrer_bonus > 0
     */
    public boolean hasReferrerBonus() {
        return referrerBonus != null && referrerBonus.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this milestone upgrades user tier.
     *
     * @return true if new_tier is set
     */
    public boolean hasTierUpgrade() {
        return newTier != null;
    }

    /**
     * Check if this milestone activates referral commission.
     *
     * @return true if activates_referral is true
     */
    public boolean activatesReferralCommission() {
        return Boolean.TRUE.equals(activatesReferral);
    }

    /**
     * Check if this milestone is for users with referrer.
     *
     * @return true if milestone_type is WITH_REFERRER
     */
    public boolean isForUsersWithReferrer() {
        return milestoneType == MilestoneType.WITH_REFERRER;
    }

    /**
     * Check if this milestone is for users without referrer.
     *
     * @return true if milestone_type is WITHOUT_REFERRER
     */
    public boolean isForUsersWithoutReferrer() {
        return milestoneType == MilestoneType.WITHOUT_REFERRER;
    }

    /**
     * Validate configuration.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (milestoneType == null) {
            throw new IllegalStateException("Milestone type is required");
        }
        if (ordersRequired == null || ordersRequired <= 0) {
            throw new IllegalStateException("Orders required must be positive");
        }
        if (refereeBonus == null) {
            throw new IllegalStateException("Referee bonus cannot be null");
        }
        if (refereeBonus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Referee bonus cannot be negative");
        }
        if (referrerBonus == null) {
            throw new IllegalStateException("Referrer bonus cannot be null");
        }
        if (referrerBonus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Referrer bonus cannot be negative");
        }
        if (commissionMonths != null && commissionMonths < 0) {
            throw new IllegalStateException("Commission months cannot be negative");
        }
    }
}
