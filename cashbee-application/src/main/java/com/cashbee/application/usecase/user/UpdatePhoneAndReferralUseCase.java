package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.referral.ReferralValidationResult;
import com.cashbee.application.dto.user.UpdatePhoneAndReferralCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.application.service.ReferralCodeValidator;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserBankAccountRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Update số điện thoại và mã giới thiệu.
 *
 * API đơn giản hơn UpdateUserUseCase - chỉ update 2 trường:
 * - phone: Số điện thoại
 * - referredBy: Mã giới thiệu của người đã giới thiệu user này
 *
 * Không update:
 * - Bank account (dùng UpdateUserUseCase)
 * - Keycloak attributes
 *
 * Lưu ý: referredBy chỉ có thể set 1 lần duy nhất.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdatePhoneAndReferralUseCase {

    private final UserRepository userRepository;
    private final UserBankAccountRepository userBankAccountRepository;
    private final ReferralCodeValidator referralCodeValidator;
    private final UserDtoMapper userDtoMapper;

    /**
     * Update số điện thoại và/hoặc mã giới thiệu cho user.
     *
     * @param keycloakId Keycloak user ID (từ JWT token)
     * @param command Command chứa phone và/hoặc referredBy
     * @return UserResponse sau khi update
     */
    @Transactional
    public UserResponse execute(String keycloakId, UpdatePhoneAndReferralCommand command) {
        log.info("Updating phone and referral for user: keycloakId={}", keycloakId);

        // 1. Tìm user theo keycloakId
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new NotFoundException("User not found: " + keycloakId));

        log.debug("Found user: id={}, username={}", user.getId(), user.getUsername());

        // 2. Update phone nếu được cung cấp
        if (command.getPhone() != null && !command.getPhone().isBlank()) {
            log.debug("Updating phone: {} -> {}", user.getPhone(), command.getPhone());
            user.setPhone(command.getPhone());
        }

        // 3. Update referredBy nếu được cung cấp
        if (command.getReferredBy() != null && !command.getReferredBy().isBlank()) {
            updateReferredBy(user, command.getReferredBy());
        }

        // 4. Đánh dấu user đã login (không còn là first login)
        // Khi user cập nhật phone/referral = họ đã tương tác với hệ thống
        user.markAsLoggedIn();

        // 5. Save user
        user = userRepository.save(user);
        log.info("User updated successfully: userId={}", user.getId());

        // 6. Build và trả về response
        return buildUserResponse(user);
    }

    /**
     * Update referredBy cho user.
     * Chỉ cho phép set nếu user chưa có referredBy.
     *
     * @param user User entity
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
}
