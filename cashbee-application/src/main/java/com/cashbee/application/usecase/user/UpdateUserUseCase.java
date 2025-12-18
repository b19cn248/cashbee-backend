package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.UpdateUserCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.common.exception.InvalidReferralCodeException;
import com.cashbee.common.exception.ReferralCodeAlreadySetException;
import com.cashbee.common.exception.SelfReferralException;
import com.cashbee.domain.model.Bank;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserBankAccount;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.BankRepository;
import com.cashbee.domain.repository.UserBankAccountRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Use Case: Update user information.
 *
 * This use case handles updating user profile including:
 * - Basic information (fullName, phone)
 * - Bank account information (accountNumber, bankCode)
 *
 * Changes are persisted to both:
 * - Local database (user, user_bank_account tables)
 * - Keycloak (for attributes that should be in JWT)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserBankAccountRepository userBankAccountRepository;
    private final BankRepository bankRepository;
    private final IdentityProviderPort identityProviderPort;
    private final UserDtoMapper userDtoMapper;

    /**
     * Execute use case to update user.
     *
     * @param keycloakId Keycloak user ID (from JWT)
     * @param command Update command with new values
     * @return Updated user response
     */
    @Transactional
    public UserResponse execute(String keycloakId, UpdateUserCommand command) {
        log.info("Updating user: keycloakId={}", keycloakId);

        // Validate command
        command.validate();

        // 1. Find user by keycloak ID
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found: " + keycloakId));

        log.debug("Found user: id={}, username={}", user.getId(), user.getUsername());

        // 2. Update basic user info (if provided)
        boolean userUpdated = false;

        if (command.getFullName() != null && !command.getFullName().isBlank()) {
            log.debug("Updating fullName: {} -> {}", user.getFullName(), command.getFullName());
            user.setFullName(command.getFullName());
            userUpdated = true;
        }

        if (command.getPhone() != null && !command.getPhone().isBlank()) {
            log.debug("Updating phone: {} -> {}", user.getPhone(), command.getPhone());
            user.setPhone(command.getPhone());
            userUpdated = true;
        }

        // 3. Update referredBy (mã giới thiệu của người khác)
        if (command.getReferredBy() != null && !command.getReferredBy().isBlank()) {
            updateReferredBy(user, command.getReferredBy());
            userUpdated = true;
        }

        if (userUpdated) {
            user = userRepository.save(user);
            log.info("User basic info updated: userId={}", user.getId());
        }

        // 4. Update bank account info (if provided)
        if (command.hasBankAccountInfo()) {
            updateBankAccount(user, command);
        }

        // 5. Update Keycloak attributes
        updateKeycloakAttributes(user, command);

        // 6. Return updated user with bank info
        return buildUserResponse(user);
    }

    /**
     * Update or create user bank account.
     *
     * @param user User entity
     * @param command Update command
     */
    private void updateBankAccount(User user, UpdateUserCommand command) {
        log.info("Updating bank account for user: userId={}", user.getId());

        // Validate bank code exists
        Bank bank = bankRepository.findByBankCode(command.getBankCode())
                .orElseThrow(() -> new RuntimeException(
                        "Invalid bank code: " + command.getBankCode()));

        log.debug("Found bank: code={}, name={}", bank.getBankCode(), bank.getBankName());

        // Check if bank is active
        if (!bank.isActive()) {
            throw new RuntimeException("Bank is not active: " + bank.getBankCode());
        }

        // Find or create bank account
        UserBankAccount bankAccount = userBankAccountRepository
                .findByUserId(user.getId())
                .orElse(UserBankAccount.builder()
                        .userId(user.getId())
                        .isDefault(true)
                        .verified(false)
                        .build());

        // Update bank account fields
        bankAccount.setAccountNumber(command.getAccountNumber());
        bankAccount.setBankCode(bank.getBankCode());
        bankAccount.setBankName(bank.getBankName());

        // Set account name (use provided or default to user's full name)
        if (command.getAccountName() != null && !command.getAccountName().isBlank()) {
            bankAccount.setAccountName(command.getAccountName());
        } else if (user.getFullName() != null) {
            bankAccount.setAccountName(user.getFullName());
        } else {
            bankAccount.setAccountName(user.getUsername());
        }

        // Save bank account
        userBankAccountRepository.save(bankAccount);

        log.info("Bank account updated: userId={}, bankCode={}, accountNumber={}",
                user.getId(), bank.getBankCode(), command.getAccountNumber());
    }

    /**
     * Update referredBy (mã giới thiệu của người khác).
     * Chỉ cho phép set nếu user chưa có referredBy.
     *
     * @param user User entity
     * @param referredByCode Mã giới thiệu cần set
     * @throws ReferralCodeAlreadySetException nếu user đã có referredBy
     * @throws InvalidReferralCodeException nếu mã không tồn tại hoặc không hợp lệ
     * @throws SelfReferralException nếu user tự giới thiệu chính mình
     */
    private void updateReferredBy(User user, String referredByCode) {
        log.info("Updating referredBy for user: userId={}, referredByCode={}",
                user.getId(), referredByCode);

        // Bước 1: Kiểm tra user đã có referredBy chưa
        if (user.getReferredBy() != null && !user.getReferredBy().isBlank()) {
            log.warn("User {} already has referredBy set: {}", user.getId(), user.getReferredBy());
            throw new ReferralCodeAlreadySetException(
                    "Mã giới thiệu chỉ có thể nhập một lần duy nhất");
        }

        // Bước 2: Chuẩn hóa mã (uppercase, trim)
        String normalizedCode = referredByCode.toUpperCase().trim();

        // Bước 3: Tìm người giới thiệu (referrer) theo mã
        User referrer = userRepository.findByReferralCode(normalizedCode)
                .orElseThrow(() -> {
                    log.warn("Referral code not found: {}", normalizedCode);
                    return InvalidReferralCodeException.notFound(normalizedCode);
                });

        log.debug("Found referrer: userId={}, username={}", referrer.getId(), referrer.getUsername());

        // Bước 4: Kiểm tra self-referral (không được tự giới thiệu chính mình)
        if (user.getId().equals(referrer.getId())) {
            log.warn("Self-referral attempt by user: {}", user.getId());
            throw SelfReferralException.forUser(user.getId());
        }

        // Bước 5: Kiểm tra referrer có active không
        if (!referrer.isActive()) {
            log.warn("Referrer {} is inactive (status={})", referrer.getId(), referrer.getStatus());
            throw new InvalidReferralCodeException(
                    "REFERRER_INACTIVE",
                    "Mã giới thiệu không hợp lệ");
        }

        // Bước 6: Set referredBy cho user
        user.setReferredBy(normalizedCode);

        log.info("ReferredBy set successfully: userId={}, referredBy={}",
                user.getId(), normalizedCode);
    }

    /**
     * Update Keycloak user attributes.
     * Stores bank account info in Keycloak so it's available in JWT.
     *
     * @param user User entity
     * @param command Update command
     */
    private void updateKeycloakAttributes(User user, UpdateUserCommand command) {
        log.debug("Updating Keycloak attributes for user: {}", user.getKeycloakId());

        try {
            Map<String, String> attributes = new HashMap<>();

            // Add bank account attributes if updated
            if (command.hasBankAccountInfo()) {
                attributes.put("account_number", command.getAccountNumber());
                attributes.put("bank_code", command.getBankCode());
            }

            // Only update identity provider if there are attributes to update
            if (!attributes.isEmpty()) {
                identityProviderPort.updateUserAttributes(user.getKeycloakId(), attributes);
                log.info("Identity provider attributes updated for user: {}", user.getKeycloakId());
            }

            // Update basic identity provider user info if fullName changed
            if (command.getFullName() != null && !command.getFullName().isBlank()) {
                // Split full name into firstName and lastName for identity provider
                String[] nameParts = command.getFullName().split("\\s+", 2);
                String firstName = nameParts.length > 0 ? nameParts[0] : command.getFullName();
                String lastName = nameParts.length > 1 ? nameParts[1] : "";

                identityProviderPort.updateUser(
                        user.getKeycloakId(),
                        null, // don't change email
                        firstName,
                        lastName
                );
                log.debug("Identity provider user name updated: {} {}", firstName, lastName);
            }

        } catch (Exception e) {
            log.error("Failed to update Keycloak for user: {}", user.getKeycloakId(), e);
            // Don't fail the whole operation if Keycloak update fails
            // User data is already saved in local database
        }
    }

    /**
     * Build UserResponse with bank account info.
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    private UserResponse buildUserResponse(User user) {
        // Map basic user info
        UserResponse response = userDtoMapper.toResponse(user);

        // Add bank account info if exists
        userBankAccountRepository.findByUserId(user.getId())
                .ifPresent(bankAccount -> {
                    response.setAccountNumber(bankAccount.getAccountNumber());
                    response.setAccountName(bankAccount.getAccountName());
                    response.setBankCode(bankAccount.getBankCode());
                    response.setBankName(bankAccount.getBankName());
                });

        return response;
    }
}
