package com.cashbee.application.dto.referral;

import lombok.*;

/**
 * Response DTO after successfully setting referral code.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetReferralCodeResponse {

    /**
     * The referral code that was set.
     */
    private String referralCode;

    /**
     * ID of the referrer user.
     */
    private Long referrerId;

    /**
     * Username of the referrer (masked for privacy).
     */
    private String referrerName;

    /**
     * Message to display to user.
     */
    private String message;

    /**
     * Indicates if referral code was set successfully.
     */
    private boolean success;

    /**
     * Factory method for successful response.
     */
    public static SetReferralCodeResponse success(String referralCode, Long referrerId, String referrerName) {
        return SetReferralCodeResponse.builder()
                .referralCode(referralCode)
                .referrerId(referrerId)
                .referrerName(referrerName)
                .message("Referral code applied successfully! Complete 3 orders to activate referral benefits.")
                .success(true)
                .build();
    }
}
