package com.cashbee.application.dto.referraladmin;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a referrer tier configuration.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTierRequest {

    /**
     * Tier name: BRONZE, SILVER, GOLD, etc.
     */
    @NotBlank(message = "Tier name is required")
    @Size(max = 50, message = "Tier name cannot exceed 50 characters")
    private String tierName;

    /**
     * Minimum number of activated referrals required for this tier.
     */
    @NotNull(message = "Min referrals is required")
    @Min(value = 0, message = "Min referrals cannot be negative")
    private Integer minReferrals;

    /**
     * Commission rate for this tier (percentage, e.g., 5.00 for 5%).
     */
    @NotNull(message = "Commission rate is required")
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
}
