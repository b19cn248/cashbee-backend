package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckFirstLoginResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for checking and handling first-time login.
 *
 * Logic:
 * 1. Find user by keycloakId
 * 2. Check if hasEverLoggedIn is false (first login)
 * 3. If first login, mark user as logged in and save
 * 4. Return whether this was the first login
 *
 * This is an atomic operation - the check and update happen in one transaction.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
public class CheckFirstLoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckFirstLoginUseCase.class);

    private final UserRepository userRepository;

    /**
     * Check if this is the user's first login and mark as logged in if so.
     *
     * @param keycloakId Keycloak user ID
     * @return Response indicating whether this is the first login
     */
    @Transactional
    public CheckFirstLoginResponse execute(String keycloakId) {
        log.info("Checking first login for user: keycloakId={}", keycloakId);

        // Find user by keycloakId
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> NotFoundException.ofField("User", "keycloakId", keycloakId));

        // Check if this is first login
        boolean isFirstLogin = user.isFirstLogin();

        if (isFirstLogin) {
            // Mark user as having logged in
            user.markAsLoggedIn();
            user.updateLastLogin();
            userRepository.save(user);
            log.info("First login detected and marked for user: userId={}", user.getId());
        } else {
            log.info("User has logged in before: userId={}", user.getId());
        }

        return CheckFirstLoginResponse.builder()
                .firstLogin(isFirstLogin)
                .build();
    }
}
