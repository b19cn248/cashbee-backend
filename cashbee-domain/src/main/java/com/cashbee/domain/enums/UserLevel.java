package com.cashbee.domain.enums;

import java.math.BigDecimal;

/**
 * Enum representing user levels/tiers in the system.
 *
 * Different user levels receive different cashback rates:
 * - NORMAL: 80% of full commission
 * - VIP: 83% of full commission
 * - SUPER: 85% of full commission
 *
 * This allows for loyalty programs and VIP benefits.
 *
 * @author CashBee Team
 */
public enum UserLevel {
    /**
     * Normal/regular user (default level).
     * Receives 80% of full commission as cashback.
     */
    NORMAL(80),

    /**
     * VIP user (premium tier).
     * Receives 83% of full commission as cashback.
     * Unlocked at 40 completed orders.
     */
    VIP(83),

    /**
     * Super VIP user (highest tier).
     * Receives 85% of full commission as cashback.
     * Unlocked at 150 completed orders.
     */
    SUPER(85);

    /**
     * Cashback rate percentage for this user level.
     * Example: 80 means user gets 80% of full commission.
     */
    private final int cashbackRate;

    UserLevel(int cashbackRate) {
        this.cashbackRate = cashbackRate;
    }

    /**
     * Get cashback rate as integer percentage.
     *
     * @return cashback rate (e.g., 80 for 80%)
     */
    public int getCashbackRate() {
        return cashbackRate;
    }

    /**
     * Get cashback rate as BigDecimal for calculation.
     *
     * @return cashback rate as BigDecimal (e.g., 80 for 80%)
     */
    public BigDecimal getCashbackRateDecimal() {
        return BigDecimal.valueOf(cashbackRate);
    }
}
