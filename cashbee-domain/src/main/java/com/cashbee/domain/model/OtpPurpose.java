package com.cashbee.domain.model;

/**
 * Enum representing the purpose of an OTP verification.
 * Allows the OTP system to be reused for different verification flows.
 */
public enum OtpPurpose {
    /**
     * OTP for user registration email verification
     */
    REGISTRATION,

    /**
     * OTP for password reset flow
     */
    PASSWORD_RESET,

    /**
     * OTP for email change verification
     */
    EMAIL_CHANGE,

    /**
     * OTP for two-factor authentication
     */
    TWO_FACTOR_AUTH
}
