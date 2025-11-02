package com.cashbee.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Command DTO for syncing user data from Keycloak to local database.
 *
 * This is used when a user logs in via Keycloak and we need to ensure
 * their data is synchronized in our local database.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncCommand {

    /**
     * Keycloak user ID (UUID).
     * This is the primary identifier from Keycloak.
     */
    @NotBlank(message = "Keycloak ID is required")
    private String keycloakId;

    /**
     * Username from Keycloak.
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * Email from Keycloak.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Full name from Keycloak.
     */
    private String fullName;

    /**
     * Phone number (optional).
     */
    private String phone;

    /**
     * Referral code used during registration (if any).
     * This is used to link the new user to their referrer.
     */
    private String referredBy;
}
