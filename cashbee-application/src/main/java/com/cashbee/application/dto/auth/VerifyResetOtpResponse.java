package com.cashbee.application.dto.auth;

/**
 * Response DTO for successful password reset OTP verification (step 2).
 * Contains the reset token used to authorize the actual password reset.
 */
public record VerifyResetOtpResponse(
    String resetToken,
    String message
) {
    public static VerifyResetOtpResponse of(String resetToken) {
        return new VerifyResetOtpResponse(
            resetToken,
            "Xac thuc OTP thanh cong. Vui long dat lai mat khau moi."
        );
    }
}
