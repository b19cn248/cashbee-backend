package com.cashbee.application.util;

import com.cashbee.common.exception.ForbiddenException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.common.exception.UnauthorizedException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Security Utility class for extracting user information from JWT tokens.
 *
 * This class provides helper methods to:
 * - Extract user ID from JWT token (Keycloak integration)
 * - Validate user authentication and authorization
 * - Get user details from JWT claims
 *
 * Usage in Controllers:
 * <pre>
 * {@code
 * @PostMapping("/some-endpoint")
 * public ResponseEntity<?> someMethod(
 *     @AuthenticationPrincipal Jwt jwt) {
 *
 *     Long userId = securityUtils.getCurrentUserId(jwt);
 *     // Use userId safely - it's from authenticated token
 * }
 * }
 * </pre>
 *
 * Security Flow:
 * 1. Spring Security validates JWT token (signature, expiration, issuer)
 * 2. If valid, JWT is injected into controller via @AuthenticationPrincipal
 * 3. This utility extracts keycloakId from JWT.subject
 * 4. Finds corresponding User in local database
 * 5. Returns internal userId (Long) for business logic
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Get current authenticated user's internal ID from JWT token.
     *
     * Flow:
     * 1. Extract Keycloak User UUID from JWT subject
     * 2. Find User in local database by keycloakId
     * 3. Validate user is active
     * 4. Return internal user ID
     *
     * @param jwt JWT token from Spring Security (already validated)
     * @return Internal user ID (Long)
     * @throws UnauthorizedException if JWT is invalid or missing subject
     * @throws NotFoundException if user not found in local database
     * @throws ForbiddenException if user account is not active
     */
    public Long getCurrentUserId(Jwt jwt) {
        log.debug("Extracting user ID from JWT token");

        // Step 1: Extract Keycloak User UUID from JWT
        String keycloakId = jwt.getSubject();

        if (keycloakId == null || keycloakId.isBlank()) {
            log.error("JWT token missing subject (Keycloak User UUID)");
            throw new UnauthorizedException("Invalid JWT token: missing subject");
        }

        log.debug("JWT subject (Keycloak ID): {}", keycloakId);

        // Step 2: Find User in local database
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> {
                log.error("User not found for Keycloak ID: {}", keycloakId);
                return new NotFoundException(
                    "User not found in system. Please complete registration first. " +
                    "Keycloak ID: " + keycloakId
                );
            });

        log.debug("Found user: ID={}, username={}, email={}",
            user.getId(), user.getUsername(), user.getEmail());

        // Step 3: Validate user is active
        if (!user.isActive()) {
            log.warn("User {} (ID={}) is not active. Status: {}",
                user.getUsername(), user.getId(), user.getStatus());
            throw new ForbiddenException(
                "User account is not active. Status: " + user.getStatus()
            );
        }

        log.debug("User {} (ID={}) is active and authenticated",
            user.getUsername(), user.getId());

        return user.getId();
    }

    /**
     * Get current authenticated user's email from JWT token.
     *
     * @param jwt JWT token
     * @return User email address
     * @throws UnauthorizedException if email claim is missing
     */
    public String getCurrentUserEmail(Jwt jwt) {
        String email = jwt.getClaim("email");

        if (email == null || email.isBlank()) {
            log.error("JWT token missing email claim");
            throw new UnauthorizedException("Invalid JWT token: missing email");
        }

        return email;
    }

    /**
     * Get current authenticated user's username from JWT token.
     *
     * @param jwt JWT token
     * @return Username (preferred_username claim from Keycloak)
     */
    public String getCurrentUsername(Jwt jwt) {
        // Keycloak stores username in "preferred_username" claim
        String username = jwt.getClaim("preferred_username");

        if (username == null || username.isBlank()) {
            log.warn("JWT token missing preferred_username claim, falling back to subject");
            return jwt.getSubject();
        }

        return username;
    }

    /**
     * Get Keycloak User UUID from JWT token.
     *
     * @param jwt JWT token
     * @return Keycloak User UUID
     * @throws UnauthorizedException if subject is missing
     */
    public String getKeycloakUserId(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        if (keycloakId == null || keycloakId.isBlank()) {
            throw new UnauthorizedException("Invalid JWT token: missing subject");
        }

        return keycloakId;
    }

    /**
     * Check if JWT token is valid and user is authenticated.
     *
     * Note: If this method is called, it means Spring Security already validated
     * the JWT signature, expiration, and issuer. This is an additional check
     * for user existence in local database.
     *
     * @param jwt JWT token
     * @return true if user exists and is active
     */
    public boolean isAuthenticated(Jwt jwt) {
        try {
            getCurrentUserId(jwt);
            return true;
        } catch (Exception e) {
            log.debug("User authentication check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get full User object from JWT token.
     *
     * @param jwt JWT token
     * @return User domain object
     * @throws NotFoundException if user not found
     */
    public User getCurrentUser(Jwt jwt) {
        String keycloakId = getKeycloakUserId(jwt);

        return userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(
                "User not found for Keycloak ID: " + keycloakId
            ));
    }
}
