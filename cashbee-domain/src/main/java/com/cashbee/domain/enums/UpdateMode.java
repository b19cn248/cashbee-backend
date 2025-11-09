package com.cashbee.domain.enums;

/**
 * Update mode for importing duplicate orders.
 *
 * Business Purpose:
 * When re-uploading CSV file, this determines how to handle duplicate orders (same order ID).
 *
 * Modes:
 * - SKIP: Skip duplicate orders (keep old data, don't update)
 * - UPDATE: Update existing orders with new data (sync status changes from platform)
 *
 * Use Case:
 * - Day 1: Upload CSV with 100 orders (all PENDING)
 * - Day 2: Upload same CSV but 50 orders now APPROVED
 * - With UPDATE mode: System will update those 50 orders to APPROVED and move cashback to confirmed
 * - With SKIP mode: System will keep old data, no updates
 *
 * @author CashBee Team
 */
public enum UpdateMode {

    /**
     * Skip duplicate orders.
     * Keep existing data unchanged.
     */
    SKIP,

    /**
     * Update existing orders with new data.
     * Sync status changes and other fields from CSV.
     */
    UPDATE
}
