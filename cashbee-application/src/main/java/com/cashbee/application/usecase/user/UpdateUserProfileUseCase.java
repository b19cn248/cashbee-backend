package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.referral.ReferralValidationResult;
import com.cashbee.application.dto.user.UpdateUserProfileCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.application.service.ReferralCodeValidator;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserBankAccount;
import com.cashbee.domain.repository.UserBankAccountRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use Case: Update thông tin profile của user.
 * <p>
 * Cho phép update các thông tin:
 * - phone: Số điện thoại
 * - referredBy: Mã giới thiệu của người đã giới thiệu user này
 * - Bank account: accountNumber, accountName, bankName, bankCode
 * <p>
 * Tất cả các trường đều optional - chỉ update nếu được cung cấp.
 * <p>
 * Lưu ý: referredBy chỉ có thể set 1 lần duy nhất.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserBankAccountRepository userBankAccountRepository;
    private final ReferralCodeValidator referralCodeValidator;
    private final UserDtoMapper userDtoMapper;

    /**
     * Update thông tin profile cho user.
     *
     * @param keycloakId Keycloak user ID (từ JWT token)
     * @param command    Command chứa các thông tin cần update
     * @return UserResponse sau khi update
     */
    @Transactional
    public UserResponse execute(String keycloakId, UpdateUserProfileCommand command) {
        log.info("Updating user profile: keycloakId={}", keycloakId);

        // 1. Tìm user theo keycloakId
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new NotFoundException("User not found: " + keycloakId));

        log.debug("Found user: id={}, username={}", user.getId(), user.getUsername());

        // 2. Update phone nếu được cung cấp
        if (isNotBlank(command.getPhone())) {
            log.debug("Updating phone: {} -> {}", user.getPhone(), command.getPhone());
            user.setPhone(command.getPhone());
        }

        // 3. Update referredBy nếu được cung cấp
        if (isNotBlank(command.getReferredBy())) {
            updateReferredBy(user, command.getReferredBy());
        }

        // 4. Update bank account nếu có thông tin bank được cung cấp
        if (command.hasBankAccountInfo()) {
            updateBankAccount(user.getId(), command);
        }

        // 5. Đánh dấu user đã login (không còn là first login)
        user.markAsLoggedIn();

        // 6. Save user
        user = userRepository.save(user);
        log.info("User profile updated successfully: userId={}", user.getId());

        // 7. Build và trả về response
        return buildUserResponse(user);
    }

    /**
     * Update referredBy cho user.
     * Chỉ cho phép set nếu user chưa có referredBy.
     *
     * @param user           User entity
     * @param referredByCode Mã giới thiệu cần set
     */
    private void updateReferredBy(User user, String referredByCode) {
        log.info("Updating referredBy: userId={}, code={}", user.getId(), referredByCode);

        // 1. Kiểm tra user đã có referredBy chưa
        referralCodeValidator.checkNotAlreadyReferred(user);

        // 2. Validate mã giới thiệu (throws exception nếu invalid)
        ReferralValidationResult result = referralCodeValidator.validateOrThrow(
                referredByCode,
                user.getId()
        );

        // 3. Set referredBy cho user
        user.setReferredBy(result.getNormalizedCode());

        log.info("ReferredBy set successfully: userId={}, referredBy={}, referrerId={}",
                user.getId(), result.getNormalizedCode(), result.getReferrerId());
    }

    /**
     * Update hoặc tạo mới bank account cho user.
     *
     * @param userId  User ID
     * @param command Command chứa thông tin bank account
     */
    private void updateBankAccount(Long userId, UpdateUserProfileCommand command) {
        log.info("Updating bank account for user: userId={}", userId);

        // Tìm bank account hiện có hoặc tạo mới
        Optional<UserBankAccount> existingAccount = userBankAccountRepository.findByUserId(userId);

        UserBankAccount bankAccount;
        if (existingAccount.isPresent()) {
            // Update bank account hiện có
            bankAccount = existingAccount.get();
            log.debug("Found existing bank account: id={}", bankAccount.getId());
        } else {
            // Tạo mới bank account
            bankAccount = UserBankAccount.builder()
                    .userId(userId)
                    .isDefault(true)
                    .verified(false)
                    .build();
            log.debug("Creating new bank account for user: userId={}", userId);
        }

        // Update các fields nếu được cung cấp
        if (isNotBlank(command.getAccountNumber())) {
            bankAccount.setAccountNumber(command.getAccountNumber());
        }
        if (isNotBlank(command.getAccountName())) {
            bankAccount.setAccountName(command.getAccountName());
        }
        if (isNotBlank(command.getBankName())) {
            bankAccount.setBankName(command.getBankName());
        }
        if (isNotBlank(command.getBankCode())) {
            bankAccount.setBankCode(command.getBankCode());
        }

        // Save bank account
        userBankAccountRepository.save(bankAccount);
        log.info("Bank account saved successfully for user: userId={}", userId);
    }

    /**
     * Build UserResponse với bank account info.
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    private UserResponse buildUserResponse(User user) {
        // Map basic user info
        UserResponse response = userDtoMapper.toResponse(user);

        // Thêm bank account info nếu có
        userBankAccountRepository.findByUserId(user.getId())
                .ifPresent(bankAccount -> {
                    response.setAccountNumber(bankAccount.getAccountNumber());
                    response.setAccountName(bankAccount.getAccountName());
                    response.setBankCode(bankAccount.getBankCode());
                    response.setBankName(bankAccount.getBankName());
                });

        return response;
    }

    /**
     * Check if string is not blank.
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
