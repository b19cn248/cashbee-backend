package com.cashbee.application.dto.auth;

/**
 * Response DTO for OTP resend operation.
 */
public record ResendOtpResponse(
    String email,
    String maskedEmail,
    int expiresIn,
    String message
) {
    public static ResendOtpResponse of(String email, String maskedEmail, int expiresIn) {
        return new ResendOtpResponse(
            email,
            maskedEmail,
            expiresIn,
            "Mã OTP mới đã được gửi đến email của bạn"
        );
    }
}
