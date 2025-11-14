package com.cashbee.application.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user registration.
 *
 * Contains all information about the newly registered user:
 * - User identification: userId, keycloakId, username, email
 * - Profile info: fullName, phone
 * - Referral info: referralCode (user's own code), referredBy (referrer's code)
 * - Wallet info: walletId
 * - Timestamps: createdAt
 *
 * This response is sent back to mobile app after successful registration.
 * Mobile app should:
 * 1. Store userId and referralCode locally
 * 2. Display success message with referral code
 * 3. Navigate user to login screen or auto-login
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    /**
     * Internal database user ID.
     * Used for all subsequent API calls.
     */
    private Long userId;

    /**
     * Keycloak user UUID.
     * Links local user to Keycloak authentication.
     */
    private String keycloakId;

    /**
     * Username (login identifier).
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * User's full name.
     */
    private String fullName;

    /**
     * Phone number (Vietnamese format).
     */
    private String phone;

    /**
     * User's unique referral code.
     * Format: CB + 6 alphanumeric characters (e.g., CB4F7A9K)
     *
     * User can share this code with friends to earn referral bonuses.
     * Mobile app should display this prominently in user profile.
     */
    private String referralCode;

    /**
     * Referral code of the person who referred this user.
     * Null if user was not referred by anyone.
     */
    private String referredBy;

    /**
     * User's wallet ID.
     * Automatically created during registration with balance = 0.
     */
    private Long walletId;

    /**
     * User account status.
     * Should be "ACTIVE" after successful registration.
     */
    private String status;

    /**
     * Registration timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Success message for mobile app.
     * Example: "Registration successful! Your referral code is CB4F7A9K"
     */
    private String message;

    /**
     * Check if user was referred by someone.
     *
     * @return true if user has a referrer
     */
    public boolean hasReferrer() {
        return referredBy != null && !referredBy.isBlank();
    }
}
