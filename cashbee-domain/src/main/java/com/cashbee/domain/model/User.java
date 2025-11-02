package com.cashbee.domain.model;

import com.cashbee.domain.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User Domain Model.
 * Pure business object without any infrastructure concerns.
 *
 * This represents a user in the CashBee system.
 *
 * IMPORTANT: Authentication & Authorization are managed by Keycloak:
 * - Username, email, password stored in Keycloak
 * - Roles (USER, ADMIN) managed in Keycloak
 * - This model only stores business-related data (referral, wallet link, etc.)
 * - keycloakId field links local user to Keycloak user
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"deletedAt"})
@EqualsAndHashCode(of = {"id"})
public class User {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Keycloak User UUID (required).
     * Links this local user record to Keycloak user.
     * All authentication data (password, roles) stored in Keycloak.
     * Must be unique and NOT NULL.
     */
    private String keycloakId;

    /**
     * Username (synced from Keycloak).
     * Must be unique.
     */
    private String username;

    /**
     * Email address (synced from Keycloak).
     * Must be unique and valid email format.
     */
    private String email;

    /**
     * User's full name.
     */
    private String fullName;

    /**
     * Phone number (Vietnamese format: 0XXXXXXXXX).
     */
    private String phone;

    /**
     * Unique referral code for this user.
     * Format: CB + 6 random alphanumeric characters (e.g., CB4F7A9K).
     * Used for referral program.
     */
    private String referralCode;

    /**
     * Referral code of the user who referred this user.
     * Null if user was not referred by anyone.
     */
    private String referredBy;

    /**
     * User account status.
     */
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Last login timestamp.
     */
    private LocalDateTime lastLoginAt;

    /**
     * Last sync timestamp from Keycloak.
     */
    private LocalDateTime lastSyncAt;

    /**
     * Timestamp when entity was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when entity was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Soft delete timestamp.
     * Null if not deleted.
     */
    private LocalDateTime deletedAt;

    // ===== Business Logic Methods =====

    /**
     * Check if user account is active and not deleted.
     *
     * @return true if user is active and not deleted
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isDeleted();
    }

    /**
     * Check if user account is banned.
     *
     * @return true if user is banned
     */
    public boolean isBanned() {
        return this.status == UserStatus.BANNED;
    }

    /**
     * Check if user is suspended.
     *
     * @return true if user is suspended
     */
    public boolean isSuspended() {
        return this.status == UserStatus.SUSPENDED;
    }

    /**
     * Check if user is deleted (soft delete).
     *
     * @return true if user is deleted
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Ban user account.
     * Changes status to BANNED.
     */
    public void ban() {
        this.status = UserStatus.BANNED;
    }

    /**
     * Suspend user account.
     * Changes status to SUSPENDED.
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    /**
     * Activate user account.
     * Changes status to ACTIVE.
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Soft delete user.
     * Sets deletedAt timestamp.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restore deleted user.
     * Clears deletedAt timestamp.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Update last login timestamp to now.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Update last sync timestamp to now.
     */
    public void updateLastSync() {
        this.lastSyncAt = LocalDateTime.now();
    }

    /**
     * Check if user was referred by another user.
     *
     * @return true if user has a referrer
     */
    public boolean hasReferrer() {
        return this.referredBy != null && !this.referredBy.isBlank();
    }

    /**
     * Check if this is a new user (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Check if user has Keycloak integration.
     *
     * @return true if keycloak ID is set
     */
    public boolean hasKeycloakId() {
        return this.keycloakId != null && !this.keycloakId.isBlank();
    }

    /**
     * Validate user data.
     * Throws exception if validation fails.
     *
     * Note: Authentication data (password, roles) are managed by Keycloak,
     * not stored in this domain model.
     */
    public void validate() {
        // Keycloak ID is required (link to Keycloak user)
        if (this.keycloakId == null || this.keycloakId.isBlank()) {
            throw new IllegalStateException("Keycloak ID is required");
        }

        if (this.username == null || this.username.isBlank()) {
            throw new IllegalStateException("Username is required");
        }
        if (this.email == null || this.email.isBlank()) {
            throw new IllegalStateException("Email is required");
        }
        if (this.status == null) {
            throw new IllegalStateException("Status is required");
        }
    }
}
