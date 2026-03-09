package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ForgotPasswordRequest;
import com.cashbee.application.dto.auth.ForgotPasswordResponse;
import com.cashbee.application.util.EmailMaskingUtil;
import com.cashbee.application.util.OtpGenerator;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.common.exception.TooManyRequestsException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.EmailPort;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.port.IdentityProviderPort.IdentityUser;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for initiating the forgot password flow.
 *
 * Flow:
 * 1. Look up user by email in Keycloak (must exist and be enabled)
 * 2. Check for an existing PASSWORD_RESET OTP and handle resend cooldown
 * 3. Generate 6-digit OTP, create or update OtpVerification record
 * 4. Send password reset OTP email
 * 5. Return masked email and expiry seconds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordUseCase {

    private final OtpVerificationRepository otpRepository;
    private final IdentityProviderPort identityProvider;
    private final EmailPort emailPort;

    @Value("${cashbee.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${cashbee.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds;

    @Transactional
    public ForgotPasswordResponse execute(ForgotPasswordRequest request) {
        String email = request.email();
        log.info("UseCase: Initiating forgot password for email: {}", email);

        // Step 1: Look up user in Keycloak
        IdentityUser user = identityProvider.getUserByEmail(email)
            .filter(IdentityUser::isEnabled)
            .orElseThrow(() -> {
                log.warn("UseCase: User not found or disabled for email: {}", email);
                return new NotFoundException(ErrorCode.USER_NOT_FOUND,
                    "No active account found for this email address.");
            });

        // Step 2: Check for existing OTP and handle resend cooldown
        Optional<OtpVerification> existingOtp = otpRepository
            .findLatestByEmailAndPurpose(email, OtpPurpose.PASSWORD_RESET);

        if (existingOtp.isPresent()) {
            OtpVerification otp = existingOtp.get();
            if (!otp.isVerified() && !otp.canResend(resendCooldownSeconds)) {
                log.warn("UseCase: OTP resend cooldown active for email: {}", email);
                throw new TooManyRequestsException(ErrorCode.OTP_RESEND_COOLDOWN,
                    "Please wait before requesting a new OTP.");
            }
        }

        // Step 3: Generate OTP code
        String otpCode = OtpGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(otpExpiryMinutes);

        OtpVerification otpRecord;
        if (existingOtp.isPresent() && !existingOtp.get().isVerified()) {
            // Reuse existing unverified record — update for resend
            otpRecord = existingOtp.get();
            otpRecord.updateForResend(otpCode, expiresAt);
            log.info("UseCase: Updating existing OTP record for resend, email: {}", email);
        } else {
            // Create a new OTP record
            otpRecord = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .keycloakId(user.getId())
                .createdAt(now)
                .expiresAt(expiresAt)
                .verified(false)
                .attemptCount(0)
                .maxAttempts(3)
                .resendCount(0)
                .build();
            log.info("UseCase: Creating new OTP record for email: {}", email);
        }

        otpRepository.save(otpRecord);

        // Step 4: Send email
        emailPort.sendPasswordResetOtpEmail(email, otpCode, otpExpiryMinutes);
        log.debug("UseCase: Password reset OTP sent to: {}", email);

        // Step 5: Return response
        String maskedEmail = EmailMaskingUtil.maskEmail(email);
        return new ForgotPasswordResponse(maskedEmail, otpExpiryMinutes * 60);
    }
}
