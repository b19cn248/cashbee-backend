package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ResendOtpRequest;
import com.cashbee.application.dto.auth.ResendOtpResponse;
import com.cashbee.application.util.EmailMaskingUtil;
import com.cashbee.application.util.OtpGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.EmailPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use Case for resending OTP verification email.
 *
 * Features:
 * - Cooldown period (30 seconds between resends)
 * - Daily limit (max 5 resends per day)
 * - Resets attempt count
 * - Updates expiry time
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResendOtpUseCase {

    private final OtpVerificationRepository otpRepository;
    private final EmailPort emailPort;

    @Value("${cashbee.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${cashbee.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds;

    @Value("${cashbee.otp.max-resends-per-day:5}")
    private int maxResendsPerDay;

    /**
     * Execute OTP resend.
     *
     * @param request Resend OTP request
     * @return ResendOtpResponse
     * @throws BusinessException if validation fails
     */
    @Transactional
    public ResendOtpResponse execute(ResendOtpRequest request) {
        log.info("UseCase: Resending OTP for email: {}", request.email());

        // Step 1: Find latest OTP record
        OtpVerification otpRecord = findOtpRecord(request.email());

        // Step 2: Check if already verified
        if (otpRecord.isVerified()) {
            log.error("UseCase: OTP already verified for email: {}", request.email());
            throw new BusinessException("Email already verified. Please login.");
        }

        // Step 3: Check cooldown period
        checkCooldown(otpRecord);

        // Step 4: Check daily limit
        checkDailyLimit(request.email());

        // Step 5: Generate new OTP
        String newOtpCode = OtpGenerator.generate();
        log.info("UseCase: Generated new OTP code for email: {}", request.email());

        // Step 6: Update OTP record
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        otpRecord.updateForResend(newOtpCode, newExpiresAt);
        otpRepository.save(otpRecord);
        log.info("UseCase: OTP record updated for resend");

        // Step 7: Send new OTP email
        try {
            emailPort.sendOtpEmail(request.email(), newOtpCode, otpExpiryMinutes);
            log.info("UseCase: New OTP email sent to: {}", request.email());
        } catch (Exception e) {
            log.error("UseCase: Failed to send OTP email", e);
            throw new RuntimeException("Failed to send verification email. Please try again later.", e);
        }

        // Step 8: Build and return response
        String maskedEmail = EmailMaskingUtil.maskEmail(request.email());
        ResendOtpResponse response = ResendOtpResponse.of(
            request.email(),
            maskedEmail,
            otpExpiryMinutes * 60 // Convert to seconds
        );

        log.info("UseCase: OTP resent successfully for email: {}", request.email());
        return response;
    }

    /**
     * Find OTP record by email and purpose.
     *
     * @param email User email
     * @return OTP verification record
     * @throws BusinessException if OTP not found
     */
    private OtpVerification findOtpRecord(String email) {
        return otpRepository.findLatestByEmailAndPurpose(email, OtpPurpose.REGISTRATION)
            .orElseThrow(() -> {
                log.error("UseCase: No OTP record found for email: {}", email);
                return new BusinessException("No OTP verification request found. Please register first.");
            });
    }

    /**
     * Check cooldown period between resends.
     *
     * @param otpRecord OTP verification record
     * @throws BusinessException if cooldown not expired
     */
    private void checkCooldown(OtpVerification otpRecord) {
        if (!otpRecord.canResend(resendCooldownSeconds)) {
            LocalDateTime lastResend = otpRecord.getLastResendAt();
            if (lastResend == null) {
                lastResend = otpRecord.getCreatedAt();
            }

            long secondsSinceLastResend = java.time.Duration.between(
                lastResend, LocalDateTime.now()
            ).getSeconds();

            long remainingSeconds = resendCooldownSeconds - secondsSinceLastResend;

            log.error("UseCase: Resend cooldown not expired for email: {}. Remaining: {}s",
                otpRecord.getEmail(), remainingSeconds);

            throw new BusinessException(String.format(
                "Please wait %d seconds before requesting a new OTP.", remainingSeconds));
        }

        log.debug("UseCase: Cooldown check passed");
    }

    /**
     * Check daily resend limit.
     *
     * @param email User email
     * @throws BusinessException if daily limit exceeded
     */
    private void checkDailyLimit(String email) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayCount = otpRepository.countByEmailAndPurposeSince(
            email,
            OtpPurpose.REGISTRATION,
            startOfDay
        );

        if (todayCount >= maxResendsPerDay) {
            log.error("UseCase: Daily resend limit exceeded for email: {}. Count: {}",
                email, todayCount);

            throw new BusinessException(String.format(
                "Daily OTP request limit exceeded (%d requests). Please try again tomorrow.",
                maxResendsPerDay));
        }

        log.debug("UseCase: Daily limit check passed. Count: {}/{}", todayCount, maxResendsPerDay);
    }
}
