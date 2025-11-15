package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.RegisterRequest;
import com.cashbee.application.dto.auth.RegisterResponse;
import com.cashbee.application.util.EmailMaskingUtil;
import com.cashbee.application.util.OtpGenerator;
import com.cashbee.application.util.ReferralCodeGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.UserRole;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.model.User;
import com.cashbee.domain.port.EmailPort;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import com.cashbee.domain.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Use Case for registering a new user with OTP email verification (Approach 3).
 *
 * NEW FLOW (with OTP verification):
 * 1. Validates input (username, email, password, referral code)
 * 2. Checks for duplicates in Keycloak (username, email)
 * 3. Creates user in Keycloak with ENABLED=FALSE (user cannot login yet)
 * 4. Generates 6-digit OTP code
 * 5. Saves OTP record with keycloakId and registration data (JSON without password)
 * 6. Sends OTP email to user
 * 7. Returns response indicating OTP verification is required
 *
 * User must then call VerifyOtpUseCase to:
 * - Enable Keycloak user
 * - Create local DB user
 * - Create wallet
 * - Complete registration
 *
 * Transaction Management:
 * - Uses @Transactional for OTP save
 * - If OTP save fails, cleanup Keycloak user
 *
 * Error Handling:
 * - Duplicate username/email → BusinessException
 * - Invalid referral code → BusinessException
 * - Keycloak creation failure → RuntimeException
 * - OTP save failure → RuntimeException (with cleanup)
 * - Email send failure → RuntimeException (with cleanup)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final IdentityProviderPort identityProvider;
    private final EmailPort emailPort;
    private final ReferralCodeGenerator referralCodeGenerator;
    private final ObjectMapper objectMapper;

    @Value("${cashbee.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    /**
     * Execute user registration (NEW: with OTP verification).
     *
     * @param request Registration request with user data
     * @return RegisterResponse indicating OTP verification is required
     * @throws BusinessException if validation fails or duplicate found
     */
    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        log.info("UseCase: Starting registration with OTP verification: username={}, email={}",
            request.getUsername(), request.getEmail());

        // Step 1: Validate referral code if provided
        validateReferralCode(request);

        // Step 2: Check for duplicates in Keycloak
        checkDuplicates(request);

        // Step 3: Create DISABLED user in Keycloak (user cannot login yet)
        String keycloakId = createDisabledKeycloakUser(request);

        // Step 4: Generate OTP code
        String otpCode = OtpGenerator.generate();
        log.info("UseCase: Generated OTP code for email: {}", request.getEmail());

        // Step 5: Save OTP record with registration data
        OtpVerification otpVerification;
        try {
            otpVerification = saveOtpRecord(request, keycloakId, otpCode);
            log.info("UseCase: OTP record saved: otpId={}", otpVerification.getId());
        } catch (Exception e) {
            log.error("UseCase: Failed to save OTP record, cleaning up Keycloak user", e);
            cleanupKeycloakUser(keycloakId);
            throw new RuntimeException("Failed to initiate registration", e);
        }

        // Step 6: Send OTP email
        try {
            emailPort.sendOtpEmail(request.getEmail(), otpCode, otpExpiryMinutes);
            log.info("UseCase: OTP email sent to: {}", request.getEmail());
        } catch (Exception e) {
            log.error("UseCase: Failed to send OTP email, cleaning up", e);
            cleanupKeycloakUser(keycloakId);
            otpRepository.delete(otpVerification);
            throw new RuntimeException("Failed to send verification email. Please try again.", e);
        }

        // Step 7: Build response indicating OTP verification is required
        String maskedEmail = EmailMaskingUtil.maskEmail(request.getEmail());
        RegisterResponse response = RegisterResponse.requiresOtpVerification(
            request.getEmail(),
            maskedEmail,
            otpExpiryMinutes * 60 // Convert to seconds
        );

        log.info("UseCase: Registration initiated, OTP verification required for: {}", request.getEmail());
        return response;
    }

    /**
     * Validate referral code if provided.
     *
     * @param request Registration request
     * @throws BusinessException if referral code is invalid
     */
    private void validateReferralCode(RegisterRequest request) {
        if (!request.hasReferrer()) {
            log.debug("UseCase: No referral code provided");
            return;
        }

        String referralCode = request.getReferredBy();
        log.info("UseCase: Validating referral code: {}", referralCode);

        // Check format
        if (!referralCodeGenerator.isValidFormat(referralCode)) {
            log.error("UseCase: Invalid referral code format: {}", referralCode);
            throw new BusinessException("Invalid referral code format. Must be CB followed by 6 alphanumeric characters.");
        }

        // Check existence
        Optional<User> referrer = userRepository.findByReferralCode(referralCode);
        if (referrer.isEmpty()) {
            log.error("UseCase: Referral code not found: {}", referralCode);
            throw new BusinessException("Referral code does not exist: " + referralCode);
        }

        // Check if referrer is active
        User referrerUser = referrer.get();
        if (!referrerUser.isActive()) {
            log.error("UseCase: Referrer account is not active: {}", referralCode);
            throw new BusinessException("Referral code is no longer valid (account inactive)");
        }

        log.info("UseCase: Valid referral code from user: userId={}, username={}",
            referrerUser.getId(), referrerUser.getUsername());
    }

    /**
     * Check for duplicate username or email in identity provider.
     *
     * @param request Registration request
     * @throws BusinessException if duplicate found
     */
    private void checkDuplicates(RegisterRequest request) {
        log.debug("UseCase: Checking for duplicates in identity provider");

        // Check username
        if (identityProvider.existsByUsername(request.getUsername())) {
            log.error("UseCase: Username already exists: {}", request.getUsername());
            throw new BusinessException("Username already exists: " + request.getUsername());
        }

        // Check email
        if (identityProvider.existsByEmail(request.getEmail())) {
            log.error("UseCase: Email already exists: {}", request.getEmail());
            throw new BusinessException("Email already exists: " + request.getEmail());
        }

        log.debug("UseCase: No duplicates found");
    }

    /**
     * Create DISABLED user in Keycloak with password.
     * User will be enabled after OTP verification.
     * <p>
     * Note: Role assignment is done after OTP verification in VerifyOtpUseCase.
     *
     * @param request Registration request
     * @return Keycloak user ID
     * @throws RuntimeException if user creation fails
     */
    private String createDisabledKeycloakUser(RegisterRequest request) {
        log.info("UseCase: Creating DISABLED user in Keycloak: username={}", request.getUsername());

        try {
            // Parse full name into first name and last name
            String firstName = null;
            String lastName = null;
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                String[] nameParts = request.getFullName().trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : null;
            }

            // Create DISABLED user in Keycloak (enabled=false)
            String keycloakId = identityProvider.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                firstName,
                lastName,
                false  // DISABLED - will be enabled after OTP verification
            );

            log.info("UseCase: DISABLED user created in Keycloak: keycloakId={}", keycloakId);
            return keycloakId;

        } catch (Exception e) {
            log.error("UseCase: Failed to create user in Keycloak", e);
            throw new RuntimeException("Failed to create user in authentication system", e);
        }
    }

    /**
     * Save OTP verification record with registration data.
     * Registration data is stored as JSON (without password).
     *
     * @param request Registration request
     * @param keycloakId Keycloak user ID
     * @param otpCode Generated OTP code
     * @return Saved OTP verification
     * @throws JsonProcessingException if JSON serialization fails
     */
    private OtpVerification saveOtpRecord(RegisterRequest request, String keycloakId, String otpCode)
            throws JsonProcessingException {
        log.debug("UseCase: Saving OTP record for email: {}", request.getEmail());

        // Build registration data JSON (WITHOUT password)
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", request.getUsername());
        registrationData.put("fullName", request.getFullName());
        registrationData.put("phone", request.getPhone());
        registrationData.put("referredBy", request.getReferredBy());

        String registrationDataJson = objectMapper.writeValueAsString(registrationData);

        // Calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        // Build and save OTP verification
        OtpVerification otpVerification = OtpVerification.builder()
            .email(request.getEmail())
            .otpCode(otpCode)
            .purpose(OtpPurpose.REGISTRATION)
            .keycloakId(keycloakId)
            .registrationData(registrationDataJson)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .verified(false)
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .build();

        // Validate before saving
        otpVerification.validate();

        return otpRepository.save(otpVerification);
    }


    /**
     * Cleanup identity provider user if database operation fails.
     *
     * This is a best-effort cleanup. If it fails, admin must manually delete.
     *
     * @param userId Identity provider user ID to delete
     */
    private void cleanupKeycloakUser(String userId) {
        try {
            log.warn("UseCase: Attempting to cleanup identity provider user: {}", userId);
            identityProvider.deleteUser(userId);
            log.info("UseCase: Identity provider user deleted successfully: {}", userId);
        } catch (Exception e) {
            log.error("UseCase: Failed to cleanup identity provider user (manual cleanup required): {}",
                userId, e);
            // Don't throw - this is cleanup, original error is more important
        }
    }
}
