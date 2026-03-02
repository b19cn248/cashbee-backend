package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ForgotPasswordRequest;
import com.cashbee.application.dto.auth.ForgotPasswordResponse;
import com.cashbee.application.util.EmailMaskingUtil;
import com.cashbee.application.util.OtpGenerator;
import com.cashbee.common.exception.BadRequestException;
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
 * Use Case for initiating the password reset flow (step 1).
 *
 * Flow:
 * 1. Validate that the email exists in Keycloak
 * 2. Generate a 6-digit OTP code
 * 3. Create or update OTP record with purpose=PASSWORD_RESET
 * 4. Send password reset OTP email
 * 5. Return response with masked email and expiry info
 *
 * @author CashBee Team
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

    /**
     * Execute the forgot password use case.
     *
     * @param request Forgot password request with user email
     * @return ForgotPasswordResponse with masked email and expiry
     * @throws BadRequestException if email is not found
     */
    @Transactional
    public ForgotPasswordResponse execute(ForgotPasswordRequest request) {
        log.info("UseCase: Forgot password request for email: {}", request.email());

        // Step 1: Find user by email in Keycloak
        Optional<IdentityUser> identityUser = identityProvider.getUserByEmail(request.email());
        if (identityUser.isEmpty()) {
            log.error("UseCase: Email not found in identity provider: {}", request.email());
            throw new BadRequestException("Email khong ton tai trong he thong");
        }

        IdentityUser user = identityUser.get();
        String keycloakId = user.getId();
        log.info("UseCase: Found user in Keycloak: keycloakId={}", keycloakId);

        // Step 2: Generate 6-digit OTP code
        String otpCode = OtpGenerator.generate();
        log.info("UseCase: Generated OTP code for password reset: email={}", request.email());

        // Step 3: Create OTP record with purpose=PASSWORD_RESET
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(otpExpiryMinutes);

        OtpVerification otpVerification = OtpVerification.builder()
                .email(request.email())
                .otpCode(otpCode)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .keycloakId(keycloakId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .verified(false)
                .attemptCount(0)
                .maxAttempts(3)
                .resendCount(0)
                .build();

        OtpVerification saved = otpRepository.save(otpVerification);
        log.info("UseCase: OTP record saved: otpId={}", saved.getId());

        // Step 4: Send password reset OTP email
        try {
            emailPort.sendPasswordResetOtpEmail(request.email(), otpCode, otpExpiryMinutes);
            log.info("UseCase: Password reset OTP email sent to: {}", request.email());
        } catch (Exception e) {
            log.error("UseCase: Failed to send password reset OTP email to: {}. Exception: {} - {}",
                    request.email(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BadRequestException("Khong the gui email xac thuc. Vui long thu lai sau.");
        }

        // Step 5: Build and return response
        String maskedEmail = EmailMaskingUtil.maskEmail(request.email());
        ForgotPasswordResponse response = ForgotPasswordResponse.of(
                maskedEmail,
                otpExpiryMinutes * 60
        );

        log.info("UseCase: Forgot password initiated successfully for: {}", request.email());
        return response;
    }
}
