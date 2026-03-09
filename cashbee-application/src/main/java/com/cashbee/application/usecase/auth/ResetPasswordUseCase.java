package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ResetPasswordRequest;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.util.PasswordValidator;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for resetting a password using a validated reset token.
 *
 * Flow:
 * 1. Find OTP record by email + resetToken (must be PASSWORD_RESET and verified)
 * 2. Validate new password strength (min 8, uppercase, lowercase, digit)
 * 3. Update password in Keycloak
 * 4. Delete the OTP record
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordUseCase {

    private final OtpVerificationRepository otpRepository;
    private final IdentityProviderPort identityProvider;

    @Transactional
    public void execute(ResetPasswordRequest request) {
        log.info("UseCase: Resetting password for email: {}", request.email());

        // Step 1: Find OTP by email + resetToken (must be verified)
        OtpVerification otp = otpRepository
            .findByEmailAndResetToken(request.email(), request.resetToken(), OtpPurpose.PASSWORD_RESET)
            .orElseThrow(() -> {
                log.warn("UseCase: Invalid reset token for email: {}", request.email());
                return new BadRequestException(ErrorCode.INVALID_RESET_TOKEN,
                    "Invalid or expired password reset token.");
            });

        // Step 2: Verify the OTP was verified and the token is within the 10-minute window
        if (!otp.isVerified() || otp.getVerifiedAt() == null) {
            log.warn("UseCase: Reset token used on unverified OTP for email: {}", request.email());
            throw new BadRequestException(ErrorCode.INVALID_RESET_TOKEN,
                "Invalid or expired password reset token.");
        }
        if (otp.getVerifiedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            log.warn("UseCase: Reset token expired (>10 min) for email: {}", request.email());
            throw new BadRequestException(ErrorCode.INVALID_RESET_TOKEN,
                "Password reset token has expired. Please start the process again.");
        }

        // Step 3: Validate password strength
        PasswordValidator.validatePasswordStrength(request.newPassword());

        // Step 4: Update password in Keycloak
        identityProvider.resetUserPassword(otp.getKeycloakId(), request.newPassword());
        log.info("UseCase: Password updated in Keycloak for keycloakId: {}", otp.getKeycloakId());

        // Step 5: Delete OTP record (one-time use)
        otpRepository.delete(otp);
        log.info("UseCase: OTP record deleted after successful password reset for email: {}", request.email());
    }
}
