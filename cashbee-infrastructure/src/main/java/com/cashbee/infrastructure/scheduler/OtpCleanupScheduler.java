package com.cashbee.infrastructure.scheduler;

import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for cleaning up expired and unverified OTP records.
 *
 * Runs daily at 2 AM to:
 * 1. Find OTP records expired more than 24 hours ago and not verified
 * 2. Delete associated disabled Keycloak users
 * 3. Delete OTP records from database
 *
 * This prevents database bloat and removes abandoned registrations.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupScheduler {

    private final OtpVerificationRepository otpRepository;
    private final IdentityProviderPort identityProvider;

    /**
     * Cleanup expired OTP records and associated Keycloak users.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2:00 AM
    public void cleanupExpiredOtps() {
        log.info("Scheduler: Starting OTP cleanup job");

        try {
            // Find OTPs expired more than 24 hours ago
            LocalDateTime expiredBefore = LocalDateTime.now().minusHours(24);
            List<OtpVerification> expiredOtps = otpRepository.findExpiredAndUnverified(expiredBefore);

            if (expiredOtps.isEmpty()) {
                log.info("Scheduler: No expired OTPs to cleanup");
                return;
            }

            log.info("Scheduler: Found {} expired OTP(s) to cleanup", expiredOtps.size());

            int keycloakDeletedCount = 0;
            int keycloakDeleteFailedCount = 0;

            // Delete associated Keycloak users
            for (OtpVerification otp : expiredOtps) {
                if (otp.hasKeycloakUser()) {
                    try {
                        identityProvider.deleteUser(otp.getKeycloakId());
                        keycloakDeletedCount++;
                        log.debug("Scheduler: Deleted Keycloak user: {}", otp.getKeycloakId());
                    } catch (Exception e) {
                        keycloakDeleteFailedCount++;
                        log.warn("Scheduler: Failed to delete Keycloak user: {} - {}",
                            otp.getKeycloakId(), e.getMessage());
                        // Continue with cleanup even if Keycloak delete fails
                    }
                }
            }

            // Delete OTP records
            otpRepository.deleteAll(expiredOtps);

            log.info("Scheduler: OTP cleanup completed. " +
                    "OTP records deleted: {}, " +
                    "Keycloak users deleted: {}, " +
                    "Keycloak delete failures: {}",
                expiredOtps.size(), keycloakDeletedCount, keycloakDeleteFailedCount);

        } catch (Exception e) {
            log.error("Scheduler: OTP cleanup job failed", e);
            // Don't throw - let scheduler continue on next run
        }
    }
}
