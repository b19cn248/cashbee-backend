package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.VerifyOtpRequest;
import com.cashbee.application.dto.auth.VerifyOtpResponse;
import com.cashbee.application.util.ReferralCodeGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.UserRole;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.port.EmailPort;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Use Case for verifying OTP and completing user registration.
 *
 * Flow:
 * 1. Find OTP record by email and purpose
 * 2. Validate OTP (expiry, attempts, code match)
 * 3. If invalid → increment attempts, throw exception
 * 4. If valid:
 *    a. Parse registrationData JSON
 *    b. Enable Keycloak user
 *    c. Assign USER role
 *    d. Generate referral code
 *    e. Create local DB user
 *    f. Create wallet
 *    g. Mark OTP as verified
 *    h. Send welcome email (non-blocking)
 *    i. Return success response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyOtpUseCase {

    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final UserWalletRepository walletRepository;
    private final IdentityProviderPort identityProvider;
    private final EmailPort emailPort;
    private final ReferralCodeGenerator referralCodeGenerator;
    private final ObjectMapper objectMapper;

    /**
     * Execute OTP verification and complete registration.
     *
     * @param request OTP verification request
     * @return VerifyOtpResponse with user information
     * @throws BusinessException if OTP is invalid
     */
    @Transactional
    public VerifyOtpResponse execute(VerifyOtpRequest request) {
        log.info("UseCase: Verifying OTP for email: {}", request.email());

        // Step 1: Find latest OTP record
        OtpVerification otpRecord = findOtpRecord(request.email());

        // Step 2: Validate OTP
        validateOtp(otpRecord, request.otpCode());

        // Step 3: Parse registration data
        Map<String, String> registrationData = parseRegistrationData(otpRecord);

        // Step 4: Enable Keycloak user
        enableKeycloakUser(otpRecord.getKeycloakId());

        // Step 5: Assign USER role
        assignUserRole(otpRecord.getKeycloakId());

        // Step 6: Generate referral code
        String referralCode = referralCodeGenerator.generate();
        log.info("UseCase: Generated referral code: {}", referralCode);

        // Step 7: Create local DB user
        User user = createLocalUser(request.email(), otpRecord.getKeycloakId(), registrationData, referralCode);
        log.info("UseCase: User created in DB: userId={}, username={}", user.getId(), user.getUsername());

        // Step 8: Create wallet
        UserWallet wallet = createWallet(user);
        log.info("UseCase: Wallet created: walletId={}", wallet.getId());

        // Step 9: Mark OTP as verified
        otpRecord.markAsVerified();
        otpRepository.save(otpRecord);
        log.info("UseCase: OTP marked as verified");

        // Step 10: Send welcome email (non-blocking - don't fail if this fails)
        sendWelcomeEmail(user);

        // Step 11: Build and return response
        VerifyOtpResponse response = buildResponse(user, wallet);
        log.info("UseCase: OTP verification completed successfully for userId={}", user.getId());

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
                return new BusinessException("No OTP verification request found for this email. Please register again.");
            });
    }

    /**
     * Validate OTP code.
     *
     * @param otpRecord OTP verification record
     * @param otpCode OTP code from user
     * @throws BusinessException if OTP is invalid
     */
    private void validateOtp(OtpVerification otpRecord, String otpCode) {
        // Check if already verified
        if (otpRecord.isVerified()) {
            log.error("UseCase: OTP already verified for email: {}", otpRecord.getEmail());
            throw new BusinessException("This OTP has already been used. Please login.");
        }

        // Check if expired
        if (otpRecord.isExpired()) {
            log.error("UseCase: OTP expired for email: {}", otpRecord.getEmail());
            throw new BusinessException("OTP has expired. Please request a new one.");
        }

        // Check if max attempts reached
        if (!otpRecord.canAttempt()) {
            log.error("UseCase: Max OTP attempts reached for email: {}", otpRecord.getEmail());
            throw new BusinessException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        // Check if code matches
        if (!otpRecord.getOtpCode().equals(otpCode)) {
            // Increment attempt count
            otpRecord.incrementAttempt();
            otpRepository.save(otpRecord);

            int attemptsLeft = otpRecord.getMaxAttempts() - otpRecord.getAttemptCount();
            log.error("UseCase: Invalid OTP code for email: {}. Attempts left: {}",
                otpRecord.getEmail(), attemptsLeft);

            throw new BusinessException(String.format(
                "Invalid OTP code. You have %d attempt(s) remaining.", attemptsLeft));
        }

        log.info("UseCase: OTP validated successfully for email: {}", otpRecord.getEmail());
    }

    /**
     * Parse registration data from JSON.
     *
     * @param otpRecord OTP verification record
     * @return Map of registration data
     * @throws BusinessException if parsing fails
     */
    private Map<String, String> parseRegistrationData(OtpVerification otpRecord) {
        try {
            String json = otpRecord.getRegistrationData();
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("UseCase: Failed to parse registration data", e);
            throw new BusinessException("Invalid registration data. Please register again.");
        }
    }

    /**
     * Enable Keycloak user (was created in DISABLED state).
     *
     * @param keycloakId Keycloak user ID
     */
    private void enableKeycloakUser(String keycloakId) {
        try {
            identityProvider.enableUser(keycloakId);
            log.info("UseCase: Keycloak user enabled: {}", keycloakId);
        } catch (Exception e) {
            log.error("UseCase: Failed to enable Keycloak user: {}", keycloakId, e);
            throw new RuntimeException("Failed to activate user account. Please contact support.", e);
        }
    }

    /**
     * Assign USER role to Keycloak user.
     *
     * @param keycloakId Keycloak user ID
     */
    private void assignUserRole(String keycloakId) {
        try {
            identityProvider.assignRole(keycloakId, UserRole.USER);
            log.info("UseCase: USER role assigned to keycloakId: {}", keycloakId);
        } catch (Exception e) {
            log.warn("UseCase: Failed to assign USER role (non-critical): {}", e.getMessage());
            // Don't throw - user is already enabled and can login
        }
    }

    /**
     * Create user in local database.
     *
     * @param email User email
     * @param keycloakId Keycloak user ID
     * @param registrationData Registration data from JSON
     * @param referralCode Generated referral code
     * @return Created user
     */
    private User createLocalUser(String email, String keycloakId,
                                  Map<String, String> registrationData, String referralCode) {
        log.debug("UseCase: Creating user in local database");

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
            .keycloakId(keycloakId)
            .username(registrationData.get("username"))
            .email(email)
            .fullName(registrationData.get("fullName"))
            .phone(registrationData.get("phone"))
            .referralCode(referralCode)
            .referredBy(registrationData.get("referredBy"))
            .status(UserStatus.ACTIVE)
            .lastSyncAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();

        return userRepository.save(user);
    }

    /**
     * Create wallet for user with zero balance.
     *
     * @param user User entity
     * @return Created wallet
     */
    private UserWallet createWallet(User user) {
        log.debug("UseCase: Creating wallet for userId={}", user.getId());

        LocalDateTime now = LocalDateTime.now();

        UserWallet wallet = UserWallet.builder()
            .userId(user.getId())
            .balance(BigDecimal.ZERO)
            .pendingBalance(BigDecimal.ZERO)
            .lockedBalance(BigDecimal.ZERO)
            .totalEarned(BigDecimal.ZERO)
            .totalWithdrawn(BigDecimal.ZERO)
            .createdAt(now)
            .updatedAt(now)
            .build();

        return walletRepository.save(wallet);
    }

    /**
     * Send welcome email to user (non-blocking).
     *
     * @param user User entity
     */
    private void sendWelcomeEmail(User user) {
        try {
            emailPort.sendWelcomeEmail(
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getReferralCode()
            );
            log.info("UseCase: Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("UseCase: Failed to send welcome email (non-critical): {}", e.getMessage());
            // Don't throw - registration is already complete
        }
    }

    /**
     * Build verification response.
     *
     * @param user Created user
     * @param wallet Created wallet
     * @return VerifyOtpResponse
     */
    private VerifyOtpResponse buildResponse(User user, UserWallet wallet) {
        return VerifyOtpResponse.builder()
            .userId(user.getId())
            .keycloakId(user.getKeycloakId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .referralCode(user.getReferralCode())
            .referredBy(user.getReferredBy())
            .walletId(wallet.getId())
            .status(user.getStatus().name())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
