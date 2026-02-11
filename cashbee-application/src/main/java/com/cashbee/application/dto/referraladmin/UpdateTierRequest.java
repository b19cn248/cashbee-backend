package com.cashbee.application.dto.referraladmin;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for updating a referrer tier configuration.
 * All fields are optional - only provided fields will be updated.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTierRequest {

    /**
     * Tier name: BRONZE, SILVER, GOLD, etc.
     */
    @Size(max = 50, message = "Tier name cannot exceed 50 characters")
    private String tierName;

    /**
     * Minimum number of activated referrals required for this tier.
     */
    @Min(value = 0, message = "Min referrals cannot be negative")
    private Integer minReferrals;

    /**
     * Commission rate for this tier (percentage, e.g., 5.00 for 5%).
     */
    @DecimalMin(value = "0", message = "Commission rate cannot be negative")
    @DecimalMax(value = "100", message = "Commission rate cannot exceed 100%")
    private BigDecimal commissionRate;

    /**
     * Bonus amount paid to referrer for each new referral activation.
     */
    @DecimalMin(value = "0", message = "Bonus per activation cannot be negative")
    private BigDecimal bonusPerActivation;

    /**
     * Description of this tier.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Whether this tier is active.
     */
    private Boolean isActive;

    /**
     * Check if at least one field is provided for update.
     */
    public boolean hasAnyFieldToUpdate() {
        return tierName != null
            || minReferrals != null
            || commissionRate != null
            || bonusPerActivation != null
            || description != null
            || isActive != null;
    }
}
