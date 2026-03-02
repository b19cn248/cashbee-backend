package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.VerifyResetOtpRequest;
import com.cashbee.application.dto.auth.VerifyResetOtpResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case for verifying the password reset OTP (step 2).
 *
 * Flow:
 * 1. Find latest unverified OTP for email with purpose=PASSWORD_RESET
 * 2. Validate: not expired, attempts remaining, code matches
 * 3. If code matches: mark as verified, generate resetToken, save
 * 4. Return response with the resetToken
 * 5. If code doesn't match: increment attempts, save, throw error
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyResetOtpUseCase {

    private final OtpVerificationRepository otpRepository;

    /**
     * Execute the verify reset OTP use case.
     *
     * @param request Verify reset OTP request with email and OTP code
     * @return VerifyResetOtpResponse with reset token
     * @throws BusinessException if OTP is invalid
     */
    @Transactional
    public VerifyResetOtpResponse execute(VerifyResetOtpRequest request) {
        log.info("UseCase: Verifying password reset OTP for email: {}", request.email());

        // Step 1: Find latest OTP record for PASSWORD_RESET
        OtpVerification otpRecord = otpRepository
                .findLatestByEmailAndPurpose(request.email(), OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> {
                    log.error("UseCase: No password reset OTP found for email: {}", request.email());
                    return new BusinessException(
                            "Khong tim thay yeu cau dat lai mat khau. Vui long thu lai.");
                });

        // Step 2: Validate OTP
        if (otpRecord.isVerified()) {
            log.error("UseCase: OTP already verified for email: {}", request.email());
            throw new BusinessException("Ma OTP nay da duoc su dung. Vui long yeu cau ma moi.");
        }

        if (otpRecord.isExpired()) {
            log.error("UseCase: OTP expired for email: {}", request.email());
            throw new BusinessException("Ma OTP da het han. Vui long yeu cau ma moi.");
        }

        if (!otpRecord.canAttempt()) {
            log.error("UseCase: Max OTP attempts reached for email: {}", request.email());
            throw new BusinessException(
                    "Da vuot qua so lan thu toi da. Vui long yeu cau ma OTP moi.");
        }

        // Step 3: Check if code matches
        if (!otpRecord.getOtpCode().equals(request.otpCode())) {
            // Increment attempt count
            otpRecord.incrementAttempt();
            otpRepository.save(otpRecord);

            int attemptsLeft = otpRecord.getMaxAttempts() - otpRecord.getAttemptCount();
            log.error("UseCase: Invalid OTP code for email: {}. Attempts left: {}",
                    request.email(), attemptsLeft);

            throw new BusinessException(String.format(
                    "Mã OTP không chính xác. Bạn còn %d lần thử.", attemptsLeft));
        }

        // Step 4: Code matches - mark as verified and generate reset token
        otpRecord.markAsVerified();
        otpRecord.generateResetToken();
        otpRepository.save(otpRecord);

        log.info("UseCase: Password reset OTP verified successfully for email: {}. Reset token generated.",
                request.email());

        // Step 5: Return response with reset token
        return VerifyResetOtpResponse.of(otpRecord.getResetToken());
    }
}
