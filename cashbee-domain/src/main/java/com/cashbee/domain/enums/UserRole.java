package com.cashbee.domain.enums;

/**
 * User role enumeration - FOR REFERENCE ONLY.
 *
 * IMPORTANT: Actual roles are managed in Keycloak, not in local database.
 * This enum is used for:
 * - Type-safe role references in code
 * - Keycloak Admin API operations
 * - Documentation purposes
 *
 * Roles MUST match exactly with Keycloak realm roles.
 *
 * @author CashBee Team
 */
public enum UserRole {
    /**
     * Regular user role (Keycloak realm role: "USER").
     * Can access standard user features:
     * - View own wallet
     * - Create payout requests
     * - View own transactions
     * - Manage own profile
     */
    USER,

    /**
     * Administrator role (Keycloak realm role: "ADMIN").
     * Has full system access:
     * - All USER permissions
     * - Approve/reject payout requests
     * - View system statistics
     * - Manage all users
     * - Access admin dashboard
     */
    ADMIN
}
