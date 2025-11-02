package com.cashbee.domain.enums;

/**
 * Affiliate order status.
 *
 * @author CashBee Team
 */
public enum OrderStatus {
    /** Order is pending confirmation */
    PENDING,

    /** Order is approved by platform */
    APPROVED,

    /** Order is cancelled */
    CANCELLED,

    /** Order is rejected */
    REJECTED,

    /** Order is paid/completed */
    PAID
}
