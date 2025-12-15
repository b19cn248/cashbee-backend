package com.cashbee.domain.enums;

/**
 * Types of referral rewards that can be granted to users.
 *
 * @author CashBee Team
 */
public enum ReferralRewardType {

    /**
     * Cash bonus for reaching order milestones.
     * Examples:
     * - 3 orders: 10,000 VND
     * - 10 orders: 20,000 VND
     */
    MILESTONE_BONUS,

    /**
     * Tier upgrade for reaching order milestones.
     * Examples:
     * - 40 orders: VIP tier (83% cashback)
     * - 150 orders: SUPER tier (85% cashback)
     */
    TIER_UPGRADE
}
