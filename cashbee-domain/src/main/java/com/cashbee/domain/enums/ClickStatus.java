package com.cashbee.domain.enums;

/**
 * Status of an affiliate click/tracking link.
 *
 * @author CashBee Team
 */
public enum ClickStatus {
    /**
     * Click tracking created, link generated.
     */
    CREATED,

    /**
     * User clicked the link and redirected to Shopee.
     */
    CLICKED,

    /**
     * Order matched with this click (found in CSV import).
     */
    MATCHED,

    /**
     * Click expired (no order after X days).
     */
    EXPIRED
}
