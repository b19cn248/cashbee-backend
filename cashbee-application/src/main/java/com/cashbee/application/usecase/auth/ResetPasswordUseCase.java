package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ResetPasswordRequest;
import com.cashbee.application.dto.auth.ResetPasswordResponse;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Use Case for resetting the user password (step 3).
 *
 * Flow:
 * 1. Find OTP by email, resetToken, purpose=PASSWORD_RESET, verified=true
 * 2. Validate: not too old (within reset token validity window)
 * 3. Call identity provider to reset password
 * 4. Delete/invalidate the OTP record
 * 5. Return success response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordUseCase {

    private final OtpVerificationRepository otpRepository;
    private final IdentityProviderPort identityProvider;

    @Value("${cashbee.otp.reset-token-validity-minutes:10}")
    private int resetTokenValidityMinutes;

    /**
     * Execute the reset password use case.
     *
     * @param request Reset password request with email, resetToken, and newPassword
     * @return ResetPasswordResponse with success message
     * @throws BusinessException if reset token is invalid or expired
     */
    @Transactional
    public ResetPasswordResponse execute(ResetPasswordRequest request) {
        log.info("UseCase: Reset password request for email: {}", request.email());

        // Step 1: Find verified OTP by email, resetToken, and purpose
        OtpVerification otpRecord = otpRepository
                .findByEmailAndResetTokenAndPurposeAndVerified(
                        request.email(),
                        request.resetToken(),
                        OtpPurpose.PASSWORD_RESET,
                        true
                )
                .orElseThrow(() -> {
                    log.error("UseCase: Invalid reset token for email: {}", request.email());
                    return new BusinessException(
                            "Reset token khong hop le hoac da het han. Vui long thu lai.");
                });

        // Step 2: Validate reset token is not too old
        LocalDateTime verifiedAt = otpRecord.getVerifiedAt();
        if (verifiedAt == null) {
            log.error("UseCase: OTP verifiedAt is null for email: {}", request.email());
            throw new BusinessException("Reset token khong hop le. Vui long thu lai.");
        }

        long minutesSinceVerification = Duration.between(verifiedAt, LocalDateTime.now()).toMinutes();
        if (minutesSinceVerification > resetTokenValidityMinutes) {
            log.error("UseCase: Reset token expired for email: {}. Verified {} minutes ago.",
                    request.email(), minutesSinceVerification);
            throw new BusinessException(
                    "Reset token da het han. Vui long yeu cau dat lai mat khau moi.");
        }

        // Step 3: Reset password in identity provider (Keycloak)
        String keycloakId = otpRecord.getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            log.error("UseCase: No keycloak ID found in OTP record for email: {}", request.email());
            throw new BadRequestException("Khong tim thay thong tin tai khoan. Vui long thu lai.");
        }

        try {
            identityProvider.resetPassword(keycloakId, request.newPassword());
            log.info("UseCase: Password reset successfully in Keycloak for keycloakId: {}", keycloakId);
        } catch (Exception e) {
            log.error("UseCase: Failed to reset password in Keycloak for keycloakId: {}", keycloakId, e);
            throw new BadRequestException("Khong the dat lai mat khau. Vui long thu lai sau.", e);
        }

        // Step 4: Delete the OTP record (invalidate)
        otpRepository.delete(otpRecord);
        log.info("UseCase: OTP record deleted after successful password reset for email: {}",
                request.email());

        // Step 5: Return success response
        log.info("UseCase: Password reset completed successfully for email: {}", request.email());
        return ResetPasswordResponse.success();
    }
}
