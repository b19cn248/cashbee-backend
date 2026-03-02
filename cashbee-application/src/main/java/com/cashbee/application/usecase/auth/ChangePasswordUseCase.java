package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ChangePasswordRequest;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.domain.port.IdentityProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Use Case for changing the authenticated user's password.
 *
 * Flow:
 * 1. Validate new password strength (min 8 chars, uppercase, lowercase, digit)
 * 2. Check that new password differs from old password
 * 3. Verify old password against Keycloak via Resource Owner Password Grant
 * 4. Reset password in Keycloak to the new password
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChangePasswordUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    private final IdentityProviderPort identityProvider;

    /**
     * Execute the change password use case.
     *
     * @param keycloakId Keycloak user ID (from JWT sub claim)
     * @param username   Username (from JWT preferred_username claim)
     * @param request    Change password request with old and new passwords
     * @throws BadRequestException if validation fails or old password is incorrect
     */
    public void execute(String keycloakId, String username, ChangePasswordRequest request) {
        log.info("UseCase: Change password request for user: {}", username);

        // Step 1: Validate new password strength
        validatePasswordStrength(request.newPassword());

        // Step 2: Check new password differs from old password
        if (request.oldPassword().equals(request.newPassword())) {
            log.warn("UseCase: New password is the same as old password for user: {}", username);
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        // Step 3: Verify old password against Keycloak
        boolean oldPasswordCorrect = identityProvider.verifyPassword(username, request.oldPassword());
        if (!oldPasswordCorrect) {
            log.warn("UseCase: Old password incorrect for user: {}", username);
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        // Step 4: Reset password in Keycloak
        try {
            identityProvider.resetPassword(keycloakId, request.newPassword());
            log.info("UseCase: Password changed successfully for user: {}", username);
        } catch (Exception e) {
            log.error("UseCase: Failed to change password for user: {}", username, e);
            throw new BadRequestException("Không thể đổi mật khẩu. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Validate password strength requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH
                || !UPPERCASE_PATTERN.matcher(password).find()
                || !LOWERCASE_PATTERN.matcher(password).find()
                || !DIGIT_PATTERN.matcher(password).find()) {
            throw new BadRequestException(
                    "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
        }
    }
}
