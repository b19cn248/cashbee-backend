package com.cashbee.domain.repository;

import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OtpVerification domain entity.
 * Follows the Port pattern in Hexagonal Architecture.
 */
public interface OtpVerificationRepository {

    /**
     * Save or update an OTP verification record
     *
     * @param otpVerification The OTP verification to save
     * @return The saved OTP verification
     */
    OtpVerification save(OtpVerification otpVerification);

    /**
     * Find an OTP verification by ID
     *
     * @param id The OTP verification ID
     * @return Optional containing the OTP verification if found
     */
    Optional<OtpVerification> findById(Long id);

    /**
     * Find the latest OTP verification for an email and purpose
     * Useful for resend functionality and verification
     *
     * @param email The email address
     * @param purpose The OTP purpose
     * @return Optional containing the latest OTP verification if found
     */
    Optional<OtpVerification> findLatestByEmailAndPurpose(String email, OtpPurpose purpose);

    /**
     * Find an OTP verification by email, purpose, and code
     * Used during OTP verification
     *
     * @param email The email address
     * @param purpose The OTP purpose
     * @param otpCode The OTP code
     * @return Optional containing the OTP verification if found
     */
    Optional<OtpVerification> findByEmailAndPurposeAndOtpCode(String email, OtpPurpose purpose, String otpCode);

    /**
     * Find all unverified OTP records that have expired
     * Used by cleanup job to remove stale OTP records
     *
     * @param expiredBefore Find OTPs expired before this time
     * @return List of expired and unverified OTP verifications
     */
    List<OtpVerification> findExpiredAndUnverified(LocalDateTime expiredBefore);

    /**
     * Find OTP verification by Keycloak user ID
     * Used during cleanup to find associated OTP when deleting Keycloak users
     *
     * @param keycloakId The Keycloak user ID
     * @return Optional containing the OTP verification if found
     */
    Optional<OtpVerification> findByKeycloakId(String keycloakId);

    /**
     * Count how many OTPs were sent to an email today for a specific purpose
     * Used for rate limiting (max 5 OTPs per day)
     *
     * @param email The email address
     * @param purpose The OTP purpose
     * @param since Count OTPs created since this time
     * @return Number of OTPs created
     */
    long countByEmailAndPurposeSince(String email, OtpPurpose purpose, LocalDateTime since);

    /**
     * Delete an OTP verification record
     *
     * @param otpVerification The OTP verification to delete
     */
    void delete(OtpVerification otpVerification);

    /**
     * Delete multiple OTP verification records
     * Used by cleanup job
     *
     * @param otpVerifications List of OTP verifications to delete
     */
    void deleteAll(List<OtpVerification> otpVerifications);

    /**
     * Check if an OTP exists for the given email and purpose
     *
     * @param email The email address
     * @param purpose The OTP purpose
     * @return true if OTP exists
     */
    boolean existsByEmailAndPurpose(String email, OtpPurpose purpose);

    /**
     * Find a verified OTP verification by email, reset token, and purpose.
     * Used during password reset to validate the reset token.
     *
     * @param email      The email address
     * @param resetToken The reset token (UUID)
     * @param purpose    The OTP purpose
     * @return Optional containing the OTP verification if found
     */
    Optional<OtpVerification> findByEmailAndResetTokenAndPurposeAndVerified(
            String email, String resetToken, OtpPurpose purpose, boolean verified);
}
