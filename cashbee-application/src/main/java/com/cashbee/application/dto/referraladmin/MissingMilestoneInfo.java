package com.cashbee.application.dto.referraladmin;

import com.cashbee.domain.enums.MilestoneType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO containing information about a missing milestone reward.
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingMilestoneInfo {

    /**
     * User ID who should receive the reward.
     */
    private Long userId;

    /**
     * Username for display.
     */
    private String username;

    /**
     * Referrer ID if user has referrer.
     */
    private Long referrerId;

    /**
     * Referrer username for display.
     */
    private String referrerUsername;

    /**
     * User's actual completed orders count.
     */
    private Integer actualOrders;

    /**
     * Milestone orders required.
     */
    private Integer milestone;

    /**
     * Milestone description.
     */
    private String description;

    /**
     * Milestone type (WITH_REFERRER or WITHOUT_REFERRER).
     */
    private MilestoneType milestoneType;

    /**
     * Referee bonus amount.
     */
    private BigDecimal refereeBonus;

    /**
     * Referrer bonus amount.
     */
    private BigDecimal referrerBonus;

    /**
     * Whether this milestone activates referral.
     */
    private Boolean activatesReferral;
}
