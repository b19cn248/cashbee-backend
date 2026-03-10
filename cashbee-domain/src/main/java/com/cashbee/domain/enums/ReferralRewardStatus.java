package com.cashbee.domain.enums;

/**
 * Status of a referral reward.
 *
 * @author CashBee Team
 */
public enum ReferralRewardStatus {

    /**
     * Reward is pending processing.
     */
    PENDING,

    /**
     * Reward has been granted to the user.
     */
    GRANTED,

    /**
     * Reward has been paid out in a batch transfer.
     */
    PAID
}
