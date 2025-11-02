package com.cashbee.domain.enums;

/**
 * Enum representing user levels/tiers in the system.
 *
 * Different user levels may receive different cashback rates.
 * This allows for loyalty programs and VIP benefits.
 *
 * @author CashBee Team
 */
public enum UserLevel {
    /**
     * Normal/regular user (default level).
     * Standard cashback rates apply.
     */
    NORMAL,

    /**
     * VIP user (premium tier).
     * Higher cashback rates than normal users.
     */
    VIP,

    /**
     * Super VIP user (highest tier).
     * Highest cashback rates available.
     */
    SUPER
}
