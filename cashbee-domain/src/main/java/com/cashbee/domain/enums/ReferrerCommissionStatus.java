package com.cashbee.domain.enums;

/**
 * Status of a referrer commission from referee's order.
 *
 * @author CashBee Team
 */
public enum ReferrerCommissionStatus {

    /**
     * Commission is pending (order not yet confirmed/paid).
     */
    PENDING,

    /**
     * Commission is confirmed (order is PAID).
     * Waiting to be added to referrer's wallet.
     */
    CONFIRMED,

    /**
     * Commission has been paid to referrer's wallet.
     */
    PAID
}
