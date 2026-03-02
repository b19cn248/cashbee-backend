package com.cashbee.application.dto.auth;

/**
 * Response DTO for forgot password (step 1).
 * Contains masked email and OTP expiry information.
 */
public record ForgotPasswordResponse(
    String maskedEmail,
    int expiresInSeconds,
    String message
) {
    public static ForgotPasswordResponse of(String maskedEmail, int expiresInSeconds) {
        return new ForgotPasswordResponse(
            maskedEmail,
            expiresInSeconds,
            "Ma OTP dat lai mat khau da duoc gui den email cua ban"
        );
    }
}
