package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.dto.user.UserSyncCommand;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.common.util.StringUtils;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for synchronizing user data from Keycloak to local database.
 *
 * This is invoked when:
 * 1. A user logs in for the first time (creates new user + wallet)
 * 2. A user logs in and their Keycloak data has changed (updates user)
 *
 * Business Rules:
 * - If user doesn't exist locally, create new user + wallet
 * - If user exists, update their information from Keycloak
 * - Always create a wallet when creating a new user
 * - Generate unique referral code for new users
 * - Link user to referrer if referral code is provided
 *
 * This is a critical use case for the Keycloak integration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncUserFromKeycloakUseCase {

    private final UserRepository userRepository;
    private final UserWalletRepository walletRepository;
    private final UserDtoMapper userMapper;

    /**
     * Sync user from Keycloak to local database.
     *
     * @param command User data from Keycloak
     * @return User response DTO
     */
    @Transactional
    public UserResponse execute(UserSyncCommand command) {
        log.info("Syncing user from Keycloak: keycloakId={}, username={}, email={}",
            command.getKeycloakId(), command.getUsername(), command.getEmail());

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByKeycloakId(command.getKeycloakId());

        User user;
        if (existingUser.isPresent()) {
            // User exists - update information
            user = updateExistingUser(existingUser.get(), command);
            log.info("Updated existing user: id={}, keycloakId={}", user.getId(), user.getKeycloakId());
        } else {
            // New user - create user + wallet
            user = createNewUser(command);
            log.info("Created new user: id={}, keycloakId={}", user.getId(), user.getKeycloakId());
        }

        return userMapper.toResponse(user);
    }

    /**
     * Update existing user with data from Keycloak.
     */
    private User updateExistingUser(User user, UserSyncCommand command) {
        // Update fields from Keycloak
        user.setUsername(command.getUsername());
        user.setEmail(command.getEmail());
        user.setFullName(command.getFullName());
        user.setPhone(command.getPhone());

        // Save updated user
        return userRepository.save(user);
    }

    /**
     * Create new user from Keycloak data.
     */
    private User createNewUser(UserSyncCommand command) {
        // Generate unique referral code
        String referralCode = generateUniqueReferralCode();

        // Validate referrer if referral code is provided
        String referredBy = null;
        if (command.getReferredBy() != null && !command.getReferredBy().isBlank()) {
            referredBy = validateAndGetReferralCode(command.getReferredBy());
        }

        // Create user
        User user = User.builder()
            .keycloakId(command.getKeycloakId())
            .username(command.getUsername())
            .email(command.getEmail())
            .fullName(command.getFullName())
            .phone(command.getPhone())
            .referralCode(referralCode)
            .referredBy(referredBy)
            .status(UserStatus.ACTIVE)
            .build();

        // Save user
        user = userRepository.save(user);

        // Create wallet for new user
        createWalletForUser(user.getId());

        return user;
    }

    /**
     * Generate a unique referral code.
     * Retries if collision occurs.
     */
    private String generateUniqueReferralCode() {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            String code = StringUtils.generateReferralCode();
            if (!userRepository.findByReferralCode(code).isPresent()) {
                return code;
            }
            log.warn("Referral code collision: {} - retrying ({}/{})", code, i + 1, maxRetries);
        }
        throw new RuntimeException("Failed to generate unique referral code after " + maxRetries + " attempts");
    }

    /**
     * Validate referral code and return it if valid.
     * Returns null if invalid (don't fail registration for invalid referral code).
     */
    private String validateAndGetReferralCode(String referralCode) {
        Optional<User> referrer = userRepository.findByReferralCode(referralCode);
        if (referrer.isPresent()) {
            log.info("Valid referral code: {} from user: {}", referralCode, referrer.get().getId());
            return referralCode;
        } else {
            log.warn("Invalid referral code provided: {} - ignoring", referralCode);
            return null;
        }
    }

    /**
     * Create a new wallet for the user.
     * Wallet is initialized with all balances set to zero (via @Builder.Default).
     */
    private void createWalletForUser(Long userId) {
        UserWallet wallet = UserWallet.builder()
            .userId(userId)
            .build();
        walletRepository.save(wallet);
        log.info("Created wallet for user: userId={}", userId);
    }
}
