package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ChangePasswordRequest;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.common.util.PasswordValidator;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.port.IdentityProviderPort.IdentityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for changing password by an authenticated user.
 *
 * Flow:
 * 1. Retrieve user from Keycloak by keycloakId to get username
 * 2. Verify old password via Keycloak token endpoint
 * 3. Validate new password strength
 * 4. Ensure new password differs from old password
 * 5. Update password in Keycloak
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasswordUseCase {

    private final IdentityProviderPort identityProvider;

    @Transactional
    public void execute(String keycloakId, ChangePasswordRequest request) {
        log.info("UseCase: Changing password for keycloakId: {}", keycloakId);

        // Step 1: Get user info from Keycloak
        IdentityUser user = identityProvider.getUserById(keycloakId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND,
                "User not found for keycloakId: " + keycloakId));

        // Step 2: Verify old password
        boolean valid = identityProvider.verifyUserCredentials(user.getUsername(), request.oldPassword());
        if (!valid) {
            log.warn("UseCase: Invalid old password for keycloakId: {}", keycloakId);
            throw new BadRequestException(ErrorCode.INVALID_CREDENTIALS,
                "Current password is incorrect.");
        }

        // Step 3: Validate new password strength
        PasswordValidator.validatePasswordStrength(request.newPassword());

        // Step 4: Ensure new password differs from old
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "New password must be different from the current password.");
        }

        // Step 5: Update password in Keycloak
        identityProvider.resetUserPassword(keycloakId, request.newPassword());
        log.info("UseCase: Password changed successfully for keycloakId: {}", keycloakId);
    }

}
