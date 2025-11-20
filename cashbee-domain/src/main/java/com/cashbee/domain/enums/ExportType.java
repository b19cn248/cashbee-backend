package com.cashbee.domain.enums;

/**
 * Batch Transfer Export Type.
 *
 * Loại export: thủ công hoặc tự động.
 *
 * @author CashBee Team
 */
public enum ExportType {
    /**
     * Admin export thủ công qua API.
     */
    MANUAL,

    /**
     * Scheduled job tự động export định kỳ.
     */
    SCHEDULED
}
