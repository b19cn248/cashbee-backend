package com.cashbee.application.dto.referral;

import lombok.*;

/**
 * Response DTO for validating a referral code.
 *
 * Used to check if a referral code is valid before setting it.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateReferralCodeResponse {

    /**
     * The referral code that was validated.
     */
    private String referralCode;

    /**
     * Whether the referral code is valid.
     */
    private boolean valid;

    /**
     * Referrer's name (masked for privacy).
     */
    private String referrerName;

    /**
     * Message explaining validation result.
     */
    private String message;

    /**
     * Factory method for valid code.
     */
    public static ValidateReferralCodeResponse valid(String code, String referrerName) {
        return ValidateReferralCodeResponse.builder()
                .referralCode(code)
                .valid(true)
                .referrerName(referrerName)
                .message("Referral code is valid!")
                .build();
    }

    /**
     * Factory method for invalid code.
     */
    public static ValidateReferralCodeResponse invalid(String code, String reason) {
        return ValidateReferralCodeResponse.builder()
                .referralCode(code)
                .valid(false)
                .referrerName(null)
                .message(reason)
                .build();
    }
}
