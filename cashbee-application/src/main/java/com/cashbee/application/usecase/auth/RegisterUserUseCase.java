package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.RegisterRequest;
import com.cashbee.application.dto.auth.RegisterResponse;
import com.cashbee.application.util.ReferralCodeGenerator;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.UserRole;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use Case for registering a new user.
 *
 * This is a public endpoint (no authentication required) that:
 * 1. Validates input (username, email, password, referral code)
 * 2. Checks for duplicates in Keycloak (username, email)
 * 3. Creates user in Keycloak with password and assigns USER role
 * 4. Generates unique referral code
 * 5. Saves user to local database
 * 6. Creates wallet with balance = 0
 * 7. Returns registration response
 *
 * Transaction Management:
 * - Uses @Transactional to ensure atomicity
 * - If database save fails after Keycloak creation, we attempt cleanup
 * - If Keycloak creation fails, transaction rolls back automatically
 *
 * Error Handling:
 * - Duplicate username/email → BusinessException
 * - Invalid referral code → BusinessException
 * - Keycloak creation failure → RuntimeException
 * - Database save failure → RuntimeException (with cleanup attempt)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final UserWalletRepository walletRepository;
    private final IdentityProviderPort identityProvider;
    private final ReferralCodeGenerator referralCodeGenerator;

    /**
     * Execute user registration.
     *
     * @param request Registration request with user data
     * @return RegisterResponse with user and wallet information
     * @throws BusinessException if validation fails or duplicate found
     */
    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        log.info("UseCase: Registering new user: username={}, email={}",
            request.getUsername(), request.getEmail());

        // Step 1: Validate referral code if provided
        validateReferralCode(request);

        // Step 2: Check for duplicates in Keycloak
        checkDuplicates(request);

        // Step 3: Create user in Keycloak
        String keycloakId = createKeycloakUser(request);

        // Step 4: Generate unique referral code
        String referralCode = referralCodeGenerator.generate();
        log.info("UseCase: Generated referral code: {}", referralCode);

        // Step 5: Save user to local database
        User user;
        try {
            user = createLocalUser(request, keycloakId, referralCode);
            log.info("UseCase: User saved to database: userId={}", user.getId());
        } catch (Exception e) {
            log.error("UseCase: Failed to save user to database, cleaning up Keycloak user", e);
            cleanupKeycloakUser(keycloakId);
            throw new RuntimeException("Failed to create user account", e);
        }

        // Step 6: Create wallet for user
        UserWallet wallet;
        try {
            wallet = createWallet(user);
            log.info("UseCase: Wallet created: walletId={}", wallet.getId());
        } catch (Exception e) {
            log.error("UseCase: Failed to create wallet", e);
            // Transaction will rollback, Keycloak user will be cleaned up
            cleanupKeycloakUser(keycloakId);
            throw new RuntimeException("Failed to create user wallet", e);
        }

        // Step 7: Build and return response
        RegisterResponse response = buildResponse(user, wallet);
        log.info("UseCase: User registration completed successfully: userId={}, username={}",
            user.getId(), user.getUsername());

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
     * Create user in identity provider with password and assign USER role.
     * <p>
     * Note: Role assignment is optional and will not fail the registration if the role
     * doesn't exist in Keycloak. The user will be created successfully and can login,
     * but won't have the USER role assigned until it's created in Keycloak.
     *
     * @param request Registration request
     * @return Identity provider user ID
     * @throws RuntimeException if user creation fails
     */
    private String createKeycloakUser(RegisterRequest request) {
        log.info("UseCase: Creating user in identity provider: username={}", request.getUsername());

        String userId = null;
        try {
            // Parse full name into first name and last name
            String firstName = null;
            String lastName = null;
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                String[] nameParts = request.getFullName().trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : null;
            }

            // Create user in identity provider
            userId = identityProvider.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                firstName,
                lastName,
                true  // enabled
            );

            log.info("UseCase: User created in identity provider: userId={}", userId);

            // Assign USER role (non-blocking - won't fail if role doesn't exist)
            try {
                identityProvider.assignRole(userId, UserRole.USER);
                log.info("UseCase: USER role assignment completed for userId={}", userId);
            } catch (Exception roleEx) {
                log.warn("UseCase: Role assignment failed but continuing registration: {}", roleEx.getMessage());
                // Don't throw - user is already created and can login
            }

            return userId;

        } catch (Exception e) {
            log.error("UseCase: Failed to create user in identity provider", e);
            throw new RuntimeException("Failed to create user in authentication system", e);
        }
    }

    /**
     * Create user in local database.
     *
     * @param request Registration request
     * @param keycloakId Keycloak user ID
     * @param referralCode Generated referral code
     * @return Saved user
     */
    private User createLocalUser(RegisterRequest request, String keycloakId, String referralCode) {
        log.debug("UseCase: Creating user in local database");

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
            .keycloakId(keycloakId)
            .username(request.getUsername())
            .email(request.getEmail())
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .referralCode(referralCode)
            .referredBy(request.getReferredBy())  // Nullable
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
     * Build registration response.
     *
     * @param user Created user
     * @param wallet Created wallet
     * @return RegisterResponse
     */
    private RegisterResponse buildResponse(User user, UserWallet wallet) {
        return RegisterResponse.builder()
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
            .message("Registration successful! Your referral code is: " + user.getReferralCode())
            .build();
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
