package com.cashbee.application.dto.auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 *
 * This DTO contains all information needed to register a new user:
 * - Authentication data: username, email, password (sent to Keycloak)
 * - Profile data: fullName, phone (stored in local database)
 * - Referral data: referredBy (optional, for referral program)
 *
 * Validation Rules:
 * - Username: 3-50 characters, alphanumeric, no spaces
 * - Email: Valid email format
 * - Password: Minimum 8 characters
 * - Phone: Vietnamese format (10-11 digits, starts with 0)
 * - ReferredBy: Optional referral code
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Username for login.
     * Must be unique across all users.
     * Will be stored in both Keycloak and local database.
     *
     * Validation:
     * - 3-50 characters
     * - Only letters, numbers, underscore, dot, hyphen
     * - No spaces allowed
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "Username can only contain letters, numbers, dots, underscores, and hyphens"
    )
    private String username;

    /**
     * Email address.
     * Must be unique and valid email format.
     * Will be stored in both Keycloak and local database.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * Password for authentication.
     * Will be hashed and stored in Keycloak only (NOT in local database).
     *
     * Validation:
     * - Minimum 8 characters
     * - Should contain uppercase, lowercase, and numbers (enforced by Keycloak)
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * User's full name (optional).
     * Stored in local database.
     */
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    /**
     * Phone number (optional).
     * Vietnamese format: 10-11 digits, starts with 0.
     *
     * Examples: 0912345678, 0312345678, 0123456789
     */
    @Pattern(
        regexp = "^0\\d{9,10}$",
        message = "Phone number must be Vietnamese format (10-11 digits, starting with 0)"
    )
    private String phone;

    /**
     * Referral code of the person who referred this user (optional).
     * If provided, must exist in database.
     *
     * Format: CB + 6 alphanumeric characters (e.g., CB4F7A9K)
     *
     * Benefits:
     * - User gets referral bonus (if policy exists)
     * - Referrer gets commission from user's activities
     */
    @Pattern(
        regexp = "^CB[A-Z0-9]{6}$",
        message = "Referral code must be in format: CB followed by 6 alphanumeric characters"
    )
    private String referredBy;

    /**
     * Check if user has a referrer.
     *
     * @return true if referredBy is provided
     */
    public boolean hasReferrer() {
        return referredBy != null && !referredBy.isBlank();
    }
}
