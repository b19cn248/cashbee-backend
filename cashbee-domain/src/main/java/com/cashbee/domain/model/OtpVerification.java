package com.cashbee.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing an OTP (One-Time Password) verification record.
 *
 * This entity is used for email verification during user registration (Approach 3):
 * 1. Keycloak user is created in DISABLED state
 * 2. OTP is generated and sent to user's email
 * 3. User verifies OTP to enable Keycloak account and create local DB user
 *
 * Security Note: Password is NEVER stored in this entity - it goes directly to Keycloak.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"otpCode", "registrationData"})
@EqualsAndHashCode(of = "id")
public class OtpVerification {

    /**
     * Unique identifier for this OTP record
     */
    private Long id;

    /**
     * Email address for this OTP verification
     */
    private String email;

    /**
     * The 6-digit OTP code
     * Security: Should be excluded from logs/toString
     */
    private String otpCode;

    /**
     * Purpose of this OTP (REGISTRATION, PASSWORD_RESET, etc.)
     */
    private OtpPurpose purpose;

    /**
     * Keycloak user ID (created in DISABLED state)
     * Null for non-registration OTP purposes
     */
    private String keycloakId;

    /**
     * JSON string containing registration data (username, fullName, phone, referredBy)
     * Does NOT contain password - password is only in Keycloak
     * Null for non-registration OTP purposes
     */
    private String registrationData;

    /**
     * When this OTP was created
     */
    private LocalDateTime createdAt;

    /**
     * When this OTP expires (typically 5 minutes after creation)
     */
    private LocalDateTime expiresAt;

    /**
     * Whether this OTP has been successfully verified
     */
    private boolean verified;

    /**
     * When this OTP was verified (null if not yet verified)
     */
    private LocalDateTime verifiedAt;

    /**
     * Number of failed verification attempts
     */
    private int attemptCount;

    /**
     * Maximum allowed verification attempts (default: 3)
     */
    private int maxAttempts;

    /**
     * Number of times OTP was resent
     */
    private int resendCount;

    /**
     * Last time OTP was resent (for cooldown check)
     */
    private LocalDateTime lastResendAt;

    /**
     * Reset token (UUID) generated after successful OTP verification for password reset.
     * Used to authorize the actual password reset request.
     * Null until OTP is verified for PASSWORD_RESET purpose.
     */
    private String resetToken;

    // ==================== Business Logic Methods ====================

    /**
     * Check if this OTP has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if more verification attempts are allowed
     */
    public boolean canAttempt() {
        return attemptCount < maxAttempts;
    }

    /**
     * Increment the attempt counter
     */
    public void incrementAttempt() {
        this.attemptCount++;
    }

    /**
     * Mark this OTP as verified
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Validate if the provided code matches this OTP
     *
     * @param code The code to validate
     * @return true if code matches, not expired, has attempts left, and not already verified
     */
    public boolean isValid(String code) {
        if (verified) {
            return false; // Already used
        }

        if (isExpired()) {
            return false; // Expired
        }

        if (!canAttempt()) {
            return false; // Too many attempts
        }

        return otpCode.equals(code);
    }

    /**
     * Check if OTP can be resent (cooldown period check)
     *
     * @param cooldownSeconds Minimum seconds between resends
     * @return true if resend is allowed
     */
    public boolean canResend(int cooldownSeconds) {
        if (lastResendAt == null) {
            return true; // Never resent before
        }

        LocalDateTime cooldownExpiry = lastResendAt.plusSeconds(cooldownSeconds);
        return LocalDateTime.now().isAfter(cooldownExpiry);
    }

    /**
     * Update resend tracking
     *
     * @param newOtpCode The new OTP code
     * @param newExpiresAt The new expiry time
     */
    public void updateForResend(String newOtpCode, LocalDateTime newExpiresAt) {
        this.otpCode = newOtpCode;
        this.expiresAt = newExpiresAt;
        this.attemptCount = 0; // Reset attempts
        this.resendCount++;
        this.lastResendAt = LocalDateTime.now();
    }

    /**
     * Generate a unique reset token (UUID) for password reset authorization.
     * Called after successful OTP verification for PASSWORD_RESET purpose.
     */
    public void generateResetToken() {
        this.resetToken = UUID.randomUUID().toString();
    }

    /**
     * Check if this OTP has a linked Keycloak user
     */
    public boolean hasKeycloakUser() {
        return keycloakId != null && !keycloakId.isBlank();
    }

    /**
     * Check if this OTP has registration data
     */
    public boolean hasRegistrationData() {
        return registrationData != null && !registrationData.isBlank();
    }

    /**
     * Validate domain invariants
     */
    public void validate() {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("OTP code cannot be null or blank");
        }

        if (otpCode.length() != 6) {
            throw new IllegalArgumentException("OTP code must be exactly 6 digits");
        }

        if (purpose == null) {
            throw new IllegalArgumentException("OTP purpose cannot be null");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiry time cannot be null");
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be positive");
        }

        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count cannot be negative");
        }

        if (resendCount < 0) {
            throw new IllegalArgumentException("Resend count cannot be negative");
        }

        // Registration-specific validation
        if (purpose == OtpPurpose.REGISTRATION) {
            if (!hasKeycloakUser()) {
                throw new IllegalArgumentException("Registration OTP must have a Keycloak user ID");
            }
            if (!hasRegistrationData()) {
                throw new IllegalArgumentException("Registration OTP must have registration data");
            }
        }
    }
}
