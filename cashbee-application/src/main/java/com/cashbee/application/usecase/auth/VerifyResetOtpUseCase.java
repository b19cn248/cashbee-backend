package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.VerifyResetOtpRequest;
import com.cashbee.application.dto.auth.VerifyResetOtpResponse;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.exception.OtpValidationException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for verifying the password reset OTP and issuing a reset token.
 *
 * Flow:
 * 1. Find the latest PASSWORD_RESET OTP for the email
 * 2. Check expiry → throw OTP_EXPIRED
 * 3. Check max attempts → throw MAX_ATTEMPTS_EXCEEDED
 * 4. Check code match → if wrong, increment attempts and throw INVALID_OTP with attemptsRemaining
 * 5. Generate reset token via otp.generateResetToken(), save, return token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyResetOtpUseCase {

    private final OtpVerificationRepository otpRepository;

    @Transactional
    public VerifyResetOtpResponse execute(VerifyResetOtpRequest request) {
        String email = request.email();
        String otpCode = request.otpCode();
        log.info("UseCase: Verifying reset OTP for email: {}", email);

        // Step 1: Find OTP record
        OtpVerification otp = otpRepository
            .findLatestByEmailAndPurpose(email, OtpPurpose.PASSWORD_RESET)
            .orElseThrow(() -> {
                log.warn("UseCase: No PASSWORD_RESET OTP found for email: {}", email);
                return new BadRequestException(ErrorCode.INVALID_OTP,
                    "No password reset request found for this email.");
            });

        // Step 2: Check expiry
        if (otp.isExpired()) {
            log.warn("UseCase: OTP expired for email: {}", email);
            throw new BadRequestException(ErrorCode.OTP_EXPIRED, "OTP has expired. Please request a new one.");
        }

        // Step 3: Check max attempts
        if (!otp.canAttempt()) {
            log.warn("UseCase: Max attempts exceeded for email: {}", email);
            throw new BadRequestException(ErrorCode.MAX_ATTEMPTS_EXCEEDED,
                "Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        // Step 4: Check code match
        if (!otp.getOtpCode().equals(otpCode)) {
            otp.incrementAttempt();
            otpRepository.save(otp);
            int attemptsRemaining = otp.getMaxAttempts() - otp.getAttemptCount();
            log.warn("UseCase: Invalid OTP code for email: {}. Remaining attempts: {}",
                email, attemptsRemaining);
            throw new OtpValidationException(ErrorCode.INVALID_OTP,
                String.format("Invalid OTP code. You have %d attempt(s) remaining.", attemptsRemaining),
                attemptsRemaining);
        }

        // Step 5: Generate reset token
        otp.generateResetToken();
        otpRepository.save(otp);
        log.info("UseCase: OTP verified and reset token generated for email: {}", email);

        return new VerifyResetOtpResponse(otp.getResetToken());
    }
}
