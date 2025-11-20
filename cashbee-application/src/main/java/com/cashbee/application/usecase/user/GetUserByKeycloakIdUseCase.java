package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserBankAccountRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving user by Keycloak ID.
 *
 * This is used when:
 * - User logs in via Keycloak and we need their local user data
 * - We need to verify if a Keycloak user exists in our database
 * - We need to get user profile linked to Keycloak account
 *
 * Business Rules:
 * - Keycloak ID is the primary identifier from the auth system
 * - Every user in our system must have a Keycloak ID
 * - Only active users should be returned (not banned/deleted)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserByKeycloakIdUseCase {

    private final UserRepository userRepository;
    private final UserBankAccountRepository userBankAccountRepository;
    private final UserDtoMapper userMapper;

    /**
     * Get user by Keycloak ID.
     *
     * @param keycloakId Keycloak user ID
     * @return User response DTO with bank account info (if exists)
     * @throws NotFoundException if user doesn't exist
     */
    @Transactional(readOnly = true)
    public UserResponse execute(String keycloakId) {
        log.debug("Getting user by keycloakId: {}", keycloakId);

        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND",
                "User not found with Keycloak ID: " + keycloakId));

        log.debug("Found user: id={}, username={}, status={}",
            user.getId(), user.getUsername(), user.getStatus());

        // Map to response
        UserResponse response = userMapper.toResponse(user);

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
