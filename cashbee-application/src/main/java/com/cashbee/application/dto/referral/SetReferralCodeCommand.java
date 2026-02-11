package com.cashbee.application.dto.referral;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Command DTO for setting referral code (becoming a referee).
 *
 * Used when user enters a referral code from their referrer.
 * Can be set during registration or when adding bank account.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetReferralCodeCommand {

    /**
     * The referral code to set (from the referrer).
     * Format: 8 characters alphanumeric.
     */
    @NotBlank(message = "Referral code is required")
    @Size(min = 6, max = 12, message = "Referral code must be between 6 and 12 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Referral code must be alphanumeric")
    private String referralCode;
}
