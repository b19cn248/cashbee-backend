package com.cashbee.application.dto.referral;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for referral reward details.
 *
 * Represents milestone bonuses and tier upgrades earned by referee.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRewardResponse {

    /**
     * Reward ID.
     */
    private Long id;

    /**
     * User who received the reward.
     */
    private Long userId;

    /**
     * Referrer who enabled this reward (if applicable).
     */
    private Long referrerId;

    /**
     * Type of reward (MILESTONE_BONUS, TIER_UPGRADE).
     */
    private String rewardType;

    /**
     * Milestone that triggered this reward (3, 10, 40, 150).
     */
    private Integer milestone;

    /**
     * Bonus amount (for MILESTONE_BONUS type).
     */
    private BigDecimal amount;

    /**
     * New tier achieved (for TIER_UPGRADE type).
     */
    private String newTier;

    /**
     * Description of the reward.
     */
    private String description;

    /**
     * Status (PENDING, GRANTED).
     */
    private String status;

    /**
     * When the reward was granted.
     */
    private LocalDateTime grantedAt;

    /**
     * When the reward was created.
     */
    private LocalDateTime createdAt;
}
