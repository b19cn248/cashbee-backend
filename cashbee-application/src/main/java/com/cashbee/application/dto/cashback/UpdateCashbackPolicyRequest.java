package com.cashbee.application.dto.cashback;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating a cashback policy.
 *
 * All fields are optional - only provided fields will be updated.
 * Validation annotations ensure data integrity.
 *
 * Example usage:
 * - Update only cashback rate: {"cashbackRate": 75.00}
 * - Update multiple fields: {"cashbackRate": 75.00, "isActive": true}
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCashbackPolicyRequest {

    /**
     * New cashback rate (percentage of commission).
     * Range: 0.00 - 100.00
     */
    @DecimalMin(value = "0", message = "Cashback rate cannot be negative")
    @DecimalMax(value = "100", message = "Cashback rate cannot exceed 100%")
    private BigDecimal cashbackRate;

    /**
     * Minimum order value for policy to apply.
     * Orders below this amount don't qualify for cashback.
     */
    @DecimalMin(value = "0", message = "Min order value cannot be negative")
    private BigDecimal minOrderValue;

    /**
     * Maximum cashback amount per order (cap).
     * Null means no cap.
     */
    @DecimalMin(value = "0", message = "Max cashback per order cannot be negative")
    private BigDecimal maxCashbackPerOrder;

    /**
     * Whether policy is active or not.
     */
    private Boolean isActive;

    /**
     * Policy priority for conflict resolution.
     * Higher priority wins if multiple policies match.
     */
    @Min(value = 0, message = "Priority cannot be negative")
    private Integer priority;

    /**
     * Policy effective start date/time.
     */
    private LocalDateTime effectiveFrom;

    /**
     * Policy effective end date/time.
     * Null means no end date (permanent).
     */
    private LocalDateTime effectiveTo;

    /**
     * Policy name for display.
     */
    @Size(max = 255, message = "Policy name cannot exceed 255 characters")
    private String policyName;

    /**
     * Check if at least one field is provided for update.
     *
     * @return true if at least one field is non-null
     */
    public boolean hasAnyFieldToUpdate() {
        return cashbackRate != null
            || minOrderValue != null
            || maxCashbackPerOrder != null
            || isActive != null
            || priority != null
            || effectiveFrom != null
            || effectiveTo != null
            || policyName != null;
    }
}
