package com.cashbee.infrastructure.keycloak;

import com.cashbee.domain.enums.UserRole;
import com.cashbee.infrastructure.config.KeycloakProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Keycloak Admin Service - Wrapper for Keycloak Admin Client API.
 *
 * This service provides methods to interact with Keycloak for:
 * - User management (create, update, delete users)
 * - Role management (assign, remove roles)
 * - User queries (get user info, search users)
 *
 * Authentication:
 * - Uses client credentials or admin username/password from KeycloakProperties
 * - Automatically handles token refresh
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final KeycloakProperties keycloakProperties;

    /**
     * Keycloak Admin Client instance.
     * Initialized in @PostConstruct, closed in @PreDestroy.
     */
    private Keycloak keycloak;

    /**
     * Initialize Keycloak Admin Client after bean construction.
     *
     * This method creates a Keycloak instance using either:
     * 1. Client credentials (clientId + clientSecret) - RECOMMENDED
     * 2. Admin credentials (username + password) - Alternative
     *
     * The client is configured to authenticate with the realm specified
     * in KeycloakProperties and will auto-refresh tokens as needed.
     */
    @PostConstruct
    public void initKeycloak() {
        log.info("Initializing Keycloak Admin Client...");
        log.debug("Keycloak Server URL: {}", keycloakProperties.getServerUrl());
        log.debug("Keycloak Realm: {}", keycloakProperties.getRealm());

        try {
            // Build Keycloak Admin Client
            // Note: Connection pool configuration is handled by the underlying HTTP client
            // Keycloak Admin Client reuses connections automatically
            KeycloakBuilder builder = KeycloakBuilder.builder()
                    .serverUrl(keycloakProperties.getServerUrl())
                    .realm(keycloakProperties.getRealm())
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS);

            // Prefer client credentials if available
            if (keycloakProperties.getClientSecret() != null &&
                !keycloakProperties.getClientSecret().isBlank()) {

                log.info("Using client credentials authentication");
                builder.clientId(keycloakProperties.getClientId())
                       .clientSecret(keycloakProperties.getClientSecret());
            } else {
                // Fallback to admin credentials
                log.warn("Using admin username/password authentication (not recommended for production)");
                builder.clientId("admin-cli")
                       .username(keycloakProperties.getUsername())
                       .password(keycloakProperties.getPassword());
            }

            this.keycloak = builder.build();

            // Test connection
            keycloak.serverInfo().getInfo();
            log.info("Keycloak Admin Client initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Keycloak Admin Client", e);
            throw new RuntimeException("Keycloak initialization failed", e);
        }
    }

    /**
     * Close Keycloak Admin Client on bean destruction.
     * Releases HTTP client resources.
     */
    @PreDestroy
    public void closeKeycloak() {
        if (keycloak != null) {
            log.info("Closing Keycloak Admin Client...");
            keycloak.close();
        }
    }

    /**
     * Get RealmResource for the configured realm.
     * Used internally by other methods.
     *
     * @return RealmResource instance
     */
    private RealmResource getRealmResource() {
        return keycloak.realm(keycloakProperties.getRealm());
    }

    /**
     * Get UsersResource for user operations.
     * Used internally by other methods.
     *
     * @return UsersResource instance
     */
    private UsersResource getUsersResource() {
        return getRealmResource().users();
    }

    // ============================================================
    // USER QUERY METHODS
    // ============================================================

    /**
     * Get user information by Keycloak user ID.
     *
     * @param keycloakId Keycloak user UUID
     * @return Optional containing UserRepresentation if found
     */
    public Optional<UserRepresentation> getUserById(String keycloakId) {
        log.debug("Getting user by Keycloak ID: {}", keycloakId);

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();
            log.debug("Found user: {} ({})", user.getUsername(), user.getEmail());
            return Optional.of(user);

        } catch (Exception e) {
            log.warn("User not found with Keycloak ID: {}", keycloakId);
            return Optional.empty();
        }
    }

    /**
     * Get user by username.
     *
     * @param username Username to search
     * @return Optional containing UserRepresentation if found
     */
    public Optional<UserRepresentation> getUserByUsername(String username) {
        log.debug("Searching user by username: {}", username);

        List<UserRepresentation> users = getUsersResource()
                .search(username, true); // exact match

        if (users.isEmpty()) {
            log.debug("User not found with username: {}", username);
            return Optional.empty();
        }

        UserRepresentation user = users.get(0);
        log.debug("Found user: {} (ID: {})", user.getUsername(), user.getId());
        return Optional.of(user);
    }

    /**
     * Get user by email.
     *
     * @param email Email to search
     * @return Optional containing UserRepresentation if found
     */
    public Optional<UserRepresentation> getUserByEmail(String email) {
        log.debug("Searching user by email: {}", email);

        List<UserRepresentation> users = getUsersResource()
                .searchByEmail(email, true); // exact match

        if (users.isEmpty()) {
            log.debug("User not found with email: {}", email);
            return Optional.empty();
        }

        UserRepresentation user = users.get(0);
        log.debug("Found user: {} (ID: {})", user.getEmail(), user.getId());
        return Optional.of(user);
    }

    // ============================================================
    // USER CREATION METHODS
    // ============================================================

    /**
     * Create a new user in Keycloak.
     *
     * @param username Username (must be unique)
     * @param email Email (must be unique)
     * @param password User password (will be hashed by Keycloak)
     * @param firstName First name (optional)
     * @param lastName Last name (optional)
     * @param enabled Whether user is enabled (default: true)
     * @return Keycloak user ID of created user
     * @throws RuntimeException if user creation fails
     */
    public String createUser(String username, String email, String password,
                            String firstName, String lastName, boolean enabled) {
        log.info("Creating user in Keycloak: username={}, email={}", username, email);

        // Build user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        user.setEmailVerified(false); // Default: email not verified

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false); // Not temporary password
        user.setCredentials(Collections.singletonList(credential));

        try {
            // Create user
            var response = getUsersResource().create(user);

            if (response.getStatus() != 201) {
                String error = response.readEntity(String.class);
                log.error("Failed to create user: status={}, error={}",
                         response.getStatus(), error);
                throw new RuntimeException("Failed to create user: " + error);
            }

            // Extract user ID from Location header
            String location = response.getHeaderString("Location");
            String userId = location.substring(location.lastIndexOf('/') + 1);

            log.info("User created successfully: keycloakId={}, username={}",
                    userId, username);

            return userId;

        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    // ============================================================
    // ROLE MANAGEMENT METHODS
    // ============================================================

    /**
     * Check if a realm role exists.
     *
     * @param roleName Role name to check
     * @return true if role exists, false otherwise
     */
    public boolean roleExists(String roleName) {
        log.debug("Checking if role exists: {}", roleName);

        try {
            getRealmResource()
                    .roles()
                    .get(roleName)
                    .toRepresentation();
            log.debug("Role {} exists", roleName);
            return true;

        } catch (Exception e) {
            log.debug("Role {} does not exist", roleName);
            return false;
        }
    }

    /**
     * Assign a realm role to user.
     * <p>
     * IMPORTANT: This method will NOT fail if the role doesn't exist.
     * Instead, it logs a warning and continues. This ensures that user
     * registration doesn't fail due to missing roles in Keycloak configuration.
     * <p>
     * Best Practice: Ensure all required roles (USER, ADMIN) exist in Keycloak
     * realm before using this application.
     *
     * @param keycloakId Keycloak user ID
     * @param role UserRole enum (USER or ADMIN)
     */
    public void assignRealmRole(String keycloakId, UserRole role) {
        log.info("Assigning role {} to user {}", role, keycloakId);

        // Check if role exists first
        if (!roleExists(role.name())) {
            log.warn("Role {} does not exist in Keycloak realm. Skipping role assignment for user {}. " +
                     "Please create the role in Keycloak: Realm Settings > Roles > Create Role '{}'",
                     role, keycloakId, role.name());
            return;
        }

        try {
            // Get role representation from realm
            RoleRepresentation roleRep = getRealmResource()
                    .roles()
                    .get(role.name())
                    .toRepresentation();

            // Assign role to user
            getUsersResource()
                    .get(keycloakId)
                    .roles()
                    .realmLevel()
                    .add(Collections.singletonList(roleRep));

            log.info("Role {} assigned successfully to user {}", role, keycloakId);

        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}. User created but without role.",
                     role, keycloakId, e.getMessage());
            // Don't throw exception - user creation should succeed even if role assignment fails
            // This prevents orphaned users in Keycloak
        }
    }

    /**
     * Remove a realm role from user.
     *
     * @param keycloakId Keycloak user ID
     * @param role UserRole enum (USER or ADMIN)
     */
    public void removeRealmRole(String keycloakId, UserRole role) {
        log.info("Removing role {} from user {}", role, keycloakId);

        try {
            // Get role representation
            RoleRepresentation roleRep = getRealmResource()
                    .roles()
                    .get(role.name())
                    .toRepresentation();

            // Remove role from user
            getUsersResource()
                    .get(keycloakId)
                    .roles()
                    .realmLevel()
                    .remove(Collections.singletonList(roleRep));

            log.info("Role {} removed successfully from user {}", role, keycloakId);

        } catch (Exception e) {
            log.error("Failed to remove role {} from user {}", role, keycloakId, e);
            throw new RuntimeException("Failed to remove role", e);
        }
    }

    /**
     * Get all realm roles assigned to user.
     *
     * @param keycloakId Keycloak user ID
     * @return List of role names
     */
    public List<String> getUserRoles(String keycloakId) {
        log.debug("Getting roles for user {}", keycloakId);

        try {
            List<RoleRepresentation> roles = getUsersResource()
                    .get(keycloakId)
                    .roles()
                    .realmLevel()
                    .listEffective();

            List<String> roleNames = roles.stream()
                    .map(RoleRepresentation::getName)
                    .toList();

            log.debug("User {} has roles: {}", keycloakId, roleNames);
            return roleNames;

        } catch (Exception e) {
            log.error("Failed to get roles for user {}", keycloakId, e);
            throw new RuntimeException("Failed to get user roles", e);
        }
    }

    /**
     * Check if user has a specific role.
     *
     * @param keycloakId Keycloak user ID
     * @param role UserRole enum
     * @return true if user has the role
     */
    public boolean hasRole(String keycloakId, UserRole role) {
        List<String> roles = getUserRoles(keycloakId);
        return roles.contains(role.name());
    }

    // ============================================================
    // USER UPDATE METHODS
    // ============================================================

    /**
     * Update user information in Keycloak.
     *
     * @param keycloakId Keycloak user ID
     * @param email New email (optional)
     * @param firstName New first name (optional)
     * @param lastName New last name (optional)
     */
    public void updateUser(String keycloakId, String email,
                          String firstName, String lastName) {
        log.info("Updating user {} in Keycloak", keycloakId);

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            // Update fields if provided
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            }
            if (firstName != null && !firstName.isBlank()) {
                user.setFirstName(firstName);
            }
            if (lastName != null && !lastName.isBlank()) {
                user.setLastName(lastName);
            }

            // Save changes
            userResource.update(user);
            log.info("User {} updated successfully", keycloakId);

        } catch (Exception e) {
            log.error("Failed to update user {}", keycloakId, e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    /**
     * Enable or disable user account.
     *
     * @param keycloakId Keycloak user ID
     * @param enabled true to enable, false to disable
     */
    public void setUserEnabled(String keycloakId, boolean enabled) {
        log.info("Setting user {} enabled status to {}", keycloakId, enabled);

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);

            log.info("User {} enabled status set to {}", keycloakId, enabled);

        } catch (Exception e) {
            log.error("Failed to set enabled status for user {}", keycloakId, e);
            throw new RuntimeException("Failed to set user enabled status", e);
        }
    }

    /**
     * Enable a disabled user account.
     * Used after OTP verification to activate newly registered users.
     *
     * @param keycloakId Keycloak user ID
     */
    public void enableUser(String keycloakId) {
        log.info("Enabling user account: {}", keycloakId);
        setUserEnabled(keycloakId, true);
    }

    /**
     * Disable an enabled user account.
     * Used for account suspension or security purposes.
     *
     * @param keycloakId Keycloak user ID
     */
    public void disableUser(String keycloakId) {
        log.info("Disabling user account: {}", keycloakId);
        setUserEnabled(keycloakId, false);
    }

    /**
     * Delete user from Keycloak.
     * WARNING: This is a permanent deletion.
     *
     * @param keycloakId Keycloak user ID
     */
    public void deleteUser(String keycloakId) {
        log.warn("Deleting user {} from Keycloak (PERMANENT)", keycloakId);

        try {
            getUsersResource().delete(keycloakId);
            log.info("User {} deleted successfully", keycloakId);

        } catch (Exception e) {
            log.error("Failed to delete user {}", keycloakId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    // ============================================================
    // USER ATTRIBUTES METHODS
    // ============================================================

    /**
     * Update user custom attributes in Keycloak.
     * Keycloak allows storing custom key-value pairs in user attributes.
     * These attributes can be included in JWT tokens.
     *
     * Use case: Store bank account info (account_number, bank_code)
     * so they're available in JWT without querying database.
     *
     * @param keycloakId Keycloak user ID
     * @param attributeName Attribute name (e.g., "account_number", "bank_code")
     * @param attributeValue Attribute value
     */
    public void updateUserAttribute(String keycloakId, String attributeName, String attributeValue) {
        log.info("Updating user {} attribute: {}={}", keycloakId, attributeName, attributeValue);

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            // Get existing attributes or create new map
            var attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new java.util.HashMap<>();
                user.setAttributes(attributes);
            }

            // Set attribute value (attributes are stored as List<String>)
            attributes.put(attributeName, java.util.Collections.singletonList(attributeValue));

            // Save changes
            userResource.update(user);
            log.info("User {} attribute {} updated successfully", keycloakId, attributeName);

        } catch (Exception e) {
            log.error("Failed to update user {} attribute {}", keycloakId, attributeName, e);
            throw new RuntimeException("Failed to update user attribute", e);
        }
    }

    /**
     * Update multiple user attributes at once.
     *
     * @param keycloakId Keycloak user ID
     * @param attributes Map of attribute name -> value
     */
    public void updateUserAttributes(String keycloakId, java.util.Map<String, String> attributes) {
        log.info("Updating user {} with {} attributes", keycloakId, attributes.size());

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            // Get existing attributes or create new map
            var userAttributes = user.getAttributes();
            if (userAttributes == null) {
                userAttributes = new java.util.HashMap<>();
                user.setAttributes(userAttributes);
            }

            // Update all attributes
            for (var entry : attributes.entrySet()) {
                userAttributes.put(entry.getKey(),
                        java.util.Collections.singletonList(entry.getValue()));
            }

            // Save changes
            userResource.update(user);
            log.info("User {} attributes updated successfully", keycloakId);

        } catch (Exception e) {
            log.error("Failed to update user {} attributes", keycloakId, e);
            throw new RuntimeException("Failed to update user attributes", e);
        }
    }

    // ============================================================
    // PASSWORD MANAGEMENT METHODS
    // ============================================================

    /**
     * Reset (set) a user's password in Keycloak.
     *
     * @param keycloakId Keycloak user ID
     * @param newPassword New password (plain text, hashed by Keycloak)
     */
    public void setUserPassword(String keycloakId, String newPassword) {
        log.info("Resetting password for user: {}", keycloakId);

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            getUsersResource().get(keycloakId).resetPassword(credential);
            log.info("Password reset successfully for user: {}", keycloakId);

        } catch (Exception e) {
            log.error("Failed to reset password for user: {}", keycloakId, e);
            throw new RuntimeException("Failed to reset user password", e);
        }
    }

    /**
     * Verify user credentials by requesting a token from Keycloak.
     * Uses Resource Owner Password Credentials grant (grant_type=password).
     *
     * @param username Username to verify
     * @param password Password to verify
     * @return true if credentials are valid, false otherwise
     */
    public boolean verifyUserCredentials(String username, String password) {
        log.debug("Verifying credentials for user: {}", username);

        Keycloak testClient = KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getServerUrl())
                .realm(keycloakProperties.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(keycloakProperties.getClientId())
                .clientSecret(keycloakProperties.getClientSecret())
                .username(username)
                .password(password)
                .build();
        try {
            // If token retrieval succeeds, credentials are valid
            testClient.tokenManager().getAccessToken();
            log.debug("Credentials verified successfully for user: {}", username);
            return true;
        } catch (Exception e) {
            log.debug("Credential verification failed for user: {} - {}", username, e.getMessage());
            return false;
        } finally {
            testClient.close();
        }
    }

    /**
     * Get user attribute value.
     *
     * @param keycloakId Keycloak user ID
     * @param attributeName Attribute name
     * @return Attribute value or null if not found
     */
    public String getUserAttribute(String keycloakId, String attributeName) {
        log.debug("Getting user {} attribute: {}", keycloakId, attributeName);

        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation user = userResource.toRepresentation();

            var attributes = user.getAttributes();
            if (attributes != null && attributes.containsKey(attributeName)) {
                var values = attributes.get(attributeName);
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get user {} attribute {}", keycloakId, attributeName, e);
            throw new RuntimeException("Failed to get user attribute", e);
        }
    }
}
