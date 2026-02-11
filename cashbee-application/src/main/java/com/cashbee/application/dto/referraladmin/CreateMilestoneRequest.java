package com.cashbee.application.dto.referraladmin;

import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.enums.UserLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a milestone configuration.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMilestoneRequest {

    /**
     * Type of milestone: WITH_REFERRER or WITHOUT_REFERRER.
     */
    @NotNull(message = "Milestone type is required")
    private MilestoneType milestoneType;

    /**
     * Number of completed orders required for this milestone.
     */
    @NotNull(message = "Orders required is required")
    @Min(value = 1, message = "Orders required must be at least 1")
    private Integer ordersRequired;

    /**
     * Bonus for referee (user who completes orders).
     */
    @NotNull(message = "Referee bonus is required")
    @DecimalMin(value = "0", message = "Referee bonus cannot be negative")
    private BigDecimal refereeBonus;

    /**
     * Bonus for referrer (user who shared the code).
     */
    @NotNull(message = "Referrer bonus is required")
    @DecimalMin(value = "0", message = "Referrer bonus cannot be negative")
    private BigDecimal referrerBonus;

    /**
     * New tier to upgrade to (VIP, SUPER, DIAMOND) or null if no tier change.
     */
    private UserLevel newTier;

    /**
     * Duration in months for referrer commission.
     */
    @Min(value = 0, message = "Commission months cannot be negative")
    private Integer commissionMonths;

    /**
     * Whether this milestone activates referral commission.
     */
    private Boolean activatesReferral;

    /**
     * Description for display purposes.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Whether this configuration is active.
     */
    private Boolean isActive;
}
