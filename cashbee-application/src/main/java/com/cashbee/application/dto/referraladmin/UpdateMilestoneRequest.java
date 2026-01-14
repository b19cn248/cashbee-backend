package com.cashbee.application.dto.referraladmin;

import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.enums.UserLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for updating a milestone configuration.
 * All fields are optional - only provided fields will be updated.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMilestoneRequest {

    /**
     * Type of milestone: WITH_REFERRER or WITHOUT_REFERRER.
     */
    private MilestoneType milestoneType;

    /**
     * Number of completed orders required for this milestone.
     */
    @Min(value = 1, message = "Orders required must be at least 1")
    private Integer ordersRequired;

    /**
     * Bonus for referee (user who completes orders).
     */
    @DecimalMin(value = "0", message = "Referee bonus cannot be negative")
    private BigDecimal refereeBonus;

    /**
     * Bonus for referrer (user who shared the code).
     */
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

    /**
     * Check if at least one field is provided for update.
     */
    public boolean hasAnyFieldToUpdate() {
        return milestoneType != null
            || ordersRequired != null
            || refereeBonus != null
            || referrerBonus != null
            || newTier != null
            || commissionMonths != null
            || activatesReferral != null
            || description != null
            || isActive != null;
    }
}
