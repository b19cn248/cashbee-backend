package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity for OTP verification records.
 * Maps to the otp_verifications table.
 */
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_email_purpose", columnList = "email, purpose"),
    @Index(name = "idx_otp_keycloak_id", columnList = "keycloak_id"),
    @Index(name = "idx_otp_expires_at", columnList = "expires_at"),
    @Index(name = "idx_otp_verified", columnList = "verified"),
    @Index(name = "idx_otp_cleanup", columnList = "verified, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OtpPurposeEntity purpose;

    @Column(name = "keycloak_id", length = 255)
    private String keycloakId;

    @Column(name = "registration_data", columnDefinition = "JSON")
    private String registrationData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Builder.Default
    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "last_resend_at")
    private LocalDateTime lastResendAt;

    @Column(name = "reset_token", length = 36)
    private String resetToken;

    /**
     * Enum for OTP purpose - JPA entity level
     */
    public enum OtpPurposeEntity {
        REGISTRATION,
        PASSWORD_RESET,
        EMAIL_CHANGE,
        TWO_FACTOR_AUTH
    }
}
