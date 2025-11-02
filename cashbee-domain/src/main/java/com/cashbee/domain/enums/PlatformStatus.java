package com.cashbee.domain.enums;

/**
 * Enum representing the status of an affiliate platform.
 *
 * Used to indicate whether a platform is currently active and accepting
 * affiliate orders or temporarily inactive.
 *
 * @author CashBee Team
 */
public enum PlatformStatus {
    /**
     * Platform is active and accepting orders.
     */
    ACTIVE,

    /**
     * Platform is temporarily inactive (not accepting new orders).
     */
    INACTIVE
}
