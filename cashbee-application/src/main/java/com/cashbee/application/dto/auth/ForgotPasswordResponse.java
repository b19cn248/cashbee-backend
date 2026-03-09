package com.cashbee.application.dto.auth;

/**
 * Response DTO for forgot password initiation.
 */
public record ForgotPasswordResponse(
    String maskedEmail,
    int expiresInSeconds
) {}
