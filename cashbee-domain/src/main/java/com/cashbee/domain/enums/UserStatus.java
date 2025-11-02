package com.cashbee.domain.enums;

/**
 * User account status.
 *
 * @author CashBee Team
 */
public enum UserStatus {
    /**
     * User account is active and can access the system.
     */
    ACTIVE,

    /**
     * User account is banned and cannot access the system.
     */
    BANNED,

    /**
     * User account is temporarily suspended.
     */
    SUSPENDED
}
