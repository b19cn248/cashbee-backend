package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.OtpVerificationEntity;
import com.cashbee.infrastructure.persistence.entity.OtpVerificationEntity.OtpPurposeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for OtpVerificationEntity.
 */
@Repository
public interface OtpVerificationJpaRepository extends JpaRepository<OtpVerificationEntity, Long> {

    /**
     * Find the latest OTP verification for an email and purpose
     * Ordered by creation time descending (latest first)
     */
    @Query("SELECT o FROM OtpVerificationEntity o " +
           "WHERE o.email = :email AND o.purpose = :purpose " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerificationEntity> findLatestByEmailAndPurpose(
        @Param("email") String email,
        @Param("purpose") OtpPurposeEntity purpose
    );

    /**
     * Find OTP verification by email, purpose, and code
     */
    Optional<OtpVerificationEntity> findByEmailAndPurposeAndOtpCode(
        String email,
        OtpPurposeEntity purpose,
        String otpCode
    );

    /**
     * Find all unverified OTP records that have expired
     */
    @Query("SELECT o FROM OtpVerificationEntity o " +
           "WHERE o.verified = false AND o.expiresAt < :expiredBefore")
    List<OtpVerificationEntity> findExpiredAndUnverified(
        @Param("expiredBefore") LocalDateTime expiredBefore
    );

    /**
     * Find OTP verification by Keycloak user ID
     */
    Optional<OtpVerificationEntity> findByKeycloakId(String keycloakId);

    /**
     * Count how many OTPs were sent to an email since a specific time
     */
    @Query("SELECT COUNT(o) FROM OtpVerificationEntity o " +
           "WHERE o.email = :email AND o.purpose = :purpose AND o.createdAt >= :since")
    long countByEmailAndPurposeSince(
        @Param("email") String email,
        @Param("purpose") OtpPurposeEntity purpose,
        @Param("since") LocalDateTime since
    );

    /**
     * Check if OTP exists for email and purpose
     */
    boolean existsByEmailAndPurpose(String email, OtpPurposeEntity purpose);

    /**
     * Find OTP by email, reset token, and purpose (for password reset flow)
     */
    Optional<OtpVerificationEntity> findByEmailAndResetTokenAndPurpose(
        String email,
        String resetToken,
        OtpPurposeEntity purpose
    );
}
