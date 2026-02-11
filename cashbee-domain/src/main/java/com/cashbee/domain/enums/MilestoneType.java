package com.cashbee.domain.enums;

/**
 * Enum representing milestone types based on referral status.
 *
 * Milestones are different for users who:
 * - WITH_REFERRER: Used someone's referral code (has referrer)
 * - WITHOUT_REFERRER: Did not use any referral code
 *
 * @author CashBee Team
 */
public enum MilestoneType {

    /**
     * User who entered a referral code from another user.
     * Milestones: 5, 10, 80, 300 orders
     */
    WITH_REFERRER,

    /**
     * User who did not enter any referral code.
     * Milestones: 80, 300 orders only
     */
    WITHOUT_REFERRER
}
