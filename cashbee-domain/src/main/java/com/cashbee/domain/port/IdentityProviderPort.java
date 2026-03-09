package com.cashbee.domain.port;

import com.cashbee.domain.enums.UserRole;

import java.util.Map;
import java.util.Optional;

/**
 * Port interface for Identity Provider operations.
 * <p>
 * This is an output port in Hexagonal Architecture that abstracts
 * identity provider operations (Keycloak, Auth0, Cognito, etc.).
 * <p>
 * The infrastructure layer provides the concrete implementation.
 * This allows the domain and application layers to remain independent of
 * specific identity provider technology.
 *
 * @author CashBee Team
 */
public interface IdentityProviderPort {

    /**
     * Represents a user in the identity provider system.
     */
    interface IdentityUser {
        String getId();
        String getUsername();
        String getEmail();
        String getFirstName();
        String getLastName();
        boolean isEnabled();
    }

    /**
     * Create a new user in the identity provider.
     *
     * @param username  Username
     * @param email     Email address
     * @param password  Password (will be hashed by identity provider)
     * @param firstName First name (nullable)
     * @param lastName  Last name (nullable)
     * @param enabled   Whether user is enabled
     * @return Identity provider user ID (e.g., Keycloak UUID)
     * @throws RuntimeException if user creation fails
     */
    String createUser(String username, String email, String password,
                      String firstName, String lastName, boolean enabled);

    /**
     * Assign a role to a user in the identity provider.
     *
     * @param userId User ID in identity provider
     * @param role   Role to assign
     * @throws RuntimeException if role assignment fails
     */
    void assignRole(String userId, UserRole role);

    /**
     * Delete a user from the identity provider.
     * <p>
     * This is typically used for cleanup when database operations fail
     * after user creation in the identity provider.
     *
     * @param userId User ID in identity provider
     * @throws RuntimeException if user deletion fails
     */
    void deleteUser(String userId);

    /**
     * Get user by username from identity provider.
     *
     * @param username Username to search for
     * @return Optional containing user if found
     */
    Optional<IdentityUser> getUserByUsername(String username);

    /**
     * Get user by email from identity provider.
     *
     * @param email Email to search for
     * @return Optional containing user if found
     */
    Optional<IdentityUser> getUserByEmail(String email);

    /**
     * Check if a username exists in identity provider.
     *
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email exists in identity provider.
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Enable a disabled user in the identity provider.
     * <p>
     * Used after OTP verification to enable a user that was created
     * in disabled state during registration.
     *
     * @param userId User ID in identity provider
     * @throws RuntimeException if user enabling fails
     */
    void enableUser(String userId);

    /**
     * Disable an enabled user in the identity provider.
     * <p>
     * Can be used for account suspension or security purposes.
     *
     * @param userId User ID in identity provider
     * @throws RuntimeException if user disabling fails
     */
    void disableUser(String userId);

    /**
     * Get a user by their identity provider ID.
     *
     * @param userId User ID in identity provider
     * @return Optional containing user if found
     */
    Optional<IdentityUser> getUserById(String userId);

    /**
     * Update user information in identity provider.
     *
     * @param userId User ID in identity provider
     * @param email New email (optional, null to skip)
     * @param firstName New first name (optional, null to skip)
     * @param lastName New last name (optional, null to skip)
     * @throws RuntimeException if update fails
     */
    void updateUser(String userId, String email, String firstName, String lastName);

    /**
     * Update user custom attributes in identity provider.
     * Attributes can be included in JWT tokens.
     *
     * @param userId User ID in identity provider
     * @param attributes Map of attribute name -> value
     * @throws RuntimeException if update fails
     */
    void updateUserAttributes(String userId, Map<String, String> attributes);

    /**
     * Reset a user's password in the identity provider.
     *
     * @param keycloakId User ID in identity provider
     * @param newPassword New password (will be hashed by identity provider)
     * @throws RuntimeException if password reset fails
     */
    void resetUserPassword(String keycloakId, String newPassword);

    /**
     * Verify user credentials by attempting authentication.
     *
     * @param username Username to authenticate
     * @param password Password to verify
     * @return true if credentials are valid
     */
    boolean verifyUserCredentials(String username, String password);
}
