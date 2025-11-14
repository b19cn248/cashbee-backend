package com.cashbee.infrastructure.keycloak;

import com.cashbee.domain.enums.UserRole;
import com.cashbee.domain.port.IdentityProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Keycloak adapter implementing IdentityProviderPort.
 * <p>
 * This adapter is part of the Hexagonal Architecture (Ports & Adapters):
 * - Port: IdentityProviderPort (application layer interface)
 * - Adapter: This class (infrastructure layer implementation)
 * <p>
 * Benefits:
 * - Application layer remains independent of Keycloak
 * - Easy to swap Keycloak for another identity provider (Auth0, Cognito, etc.)
 * - Testability: Can mock IdentityProviderPort in tests
 * <p>
 * This adapter delegates all operations to KeycloakAdminService which
 * contains the actual Keycloak Admin Client implementation.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    private final KeycloakAdminService keycloakAdminService;

    @Override
    public String createUser(String username, String email, String password,
                             String firstName, String lastName, boolean enabled) {
        log.debug("Adapter: Creating user in Keycloak: username={}, email={}", username, email);

        return keycloakAdminService.createUser(
            username, email, password,
            firstName, lastName, enabled
        );
    }

    @Override
    public void assignRole(String userId, UserRole role) {
        log.debug("Adapter: Assigning role {} to user {}", role, userId);
        keycloakAdminService.assignRealmRole(userId, role);
    }

    @Override
    public void deleteUser(String userId) {
        log.debug("Adapter: Deleting user from Keycloak: {}", userId);
        keycloakAdminService.deleteUser(userId);
    }

    @Override
    public Optional<IdentityUser> getUserByUsername(String username) {
        log.debug("Adapter: Getting user by username: {}", username);

        Optional<UserRepresentation> keycloakUser = keycloakAdminService.getUserByUsername(username);
        return keycloakUser.map(this::toIdentityUser);
    }

    @Override
    public Optional<IdentityUser> getUserByEmail(String email) {
        log.debug("Adapter: Getting user by email: {}", email);

        Optional<UserRepresentation> keycloakUser = keycloakAdminService.getUserByEmail(email);
        return keycloakUser.map(this::toIdentityUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        log.debug("Adapter: Checking if username exists: {}", username);
        return keycloakAdminService.getUserByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        log.debug("Adapter: Checking if email exists: {}", email);
        return keycloakAdminService.getUserByEmail(email).isPresent();
    }

    /**
     * Convert Keycloak UserRepresentation to IdentityUser.
     *
     * @param keycloakUser Keycloak user representation
     * @return IdentityUser implementation
     */
    private IdentityUser toIdentityUser(UserRepresentation keycloakUser) {
        return new KeycloakIdentityUser(keycloakUser);
    }

    /**
     * Keycloak implementation of IdentityUser.
     * <p>
     * This inner class wraps Keycloak's UserRepresentation and exposes
     * only the fields defined in the IdentityUser interface, providing
     * a clean abstraction over Keycloak-specific details.
     */
    private static class KeycloakIdentityUser implements IdentityUser {
        private final UserRepresentation keycloakUser;

        public KeycloakIdentityUser(UserRepresentation keycloakUser) {
            this.keycloakUser = keycloakUser;
        }

        @Override
        public String getId() {
            return keycloakUser.getId();
        }

        @Override
        public String getUsername() {
            return keycloakUser.getUsername();
        }

        @Override
        public String getEmail() {
            return keycloakUser.getEmail();
        }

        @Override
        public String getFirstName() {
            return keycloakUser.getFirstName();
        }

        @Override
        public String getLastName() {
            return keycloakUser.getLastName();
        }

        @Override
        public boolean isEnabled() {
            return keycloakUser.isEnabled();
        }
    }
}
