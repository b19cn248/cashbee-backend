package com.cashbee.domain.enums;

/**
 * Batch Transfer Export Status.
 *
 * Trạng thái của batch export file Excel.
 *
 * @author CashBee Team
 */
public enum ExportStatus {
    /**
     * Export đang xử lý (đang tạo file).
     */
    PENDING,

    /**
     * Export hoàn thành thành công.
     */
    COMPLETED,

    /**
     * Export thất bại.
     */
    FAILED
}
