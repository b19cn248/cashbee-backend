package com.cashbee.domain.enums;

/**
 * Status of CSV import batch.
 *
 * @author CashBee Team
 */
public enum ImportStatus {
    /**
     * Import batch is being processed.
     */
    PROCESSING,

    /**
     * Import completed successfully.
     */
    COMPLETED,

    /**
     * Import failed with errors.
     */
    FAILED,

    /**
     * Import partially succeeded (some rows failed).
     */
    PARTIAL
}
