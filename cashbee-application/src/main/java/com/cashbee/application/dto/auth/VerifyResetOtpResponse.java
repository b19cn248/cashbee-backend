package com.cashbee.application.dto.auth;

/**
 * Response DTO for password reset OTP verification.
 */
public record VerifyResetOtpResponse(
    String resetToken
) {}
