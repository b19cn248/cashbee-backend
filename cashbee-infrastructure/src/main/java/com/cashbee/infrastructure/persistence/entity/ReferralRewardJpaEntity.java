package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ReferralReward JPA Entity for database persistence.
 *
 * <p>Maps to 'referral_reward' table in database.
 * Tracks milestone bonuses and tier upgrades granted to users.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "referral_reward", indexes = {
        @Index(name = "idx_referral_reward_user_id", columnList = "user_id"),
        @Index(name = "idx_referral_reward_referrer_id", columnList = "referrer_id"),
        @Index(name = "idx_referral_reward_user_milestone", columnList = "user_id, milestone"),
        @Index(name = "idx_referral_reward_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRewardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User who receives the reward (referee).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * User who referred (referrer). Can be null.
     */
    @Column(name = "referrer_id")
    private Long referrerId;

    /**
     * Reward type: MILESTONE_BONUS or TIER_UPGRADE.
     */
    @Column(name = "reward_type", nullable = false, length = 30)
    private String rewardType;

    /**
     * Milestone reached: 3, 10, 40, 150.
     */
    @Column(name = "milestone")
    private Integer milestone;

    /**
     * Bonus amount for MILESTONE_BONUS type.
     */
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * New tier for TIER_UPGRADE type: VIP, SUPER.
     */
    @Column(name = "new_tier", length = 20)
    private String newTier;

    /**
     * Reward status: PENDING or GRANTED.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Timestamp when reward was granted.
     */
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    /**
     * Timestamp when record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = "GRANTED";
        }
    }
}
