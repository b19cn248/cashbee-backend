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
     * Batch mới tạo, chờ admin xác nhận đã chuyển tiền.
     */
    PENDING,

    /**
     * Batch đang được xử lý (đang trừ tiền các users).
     * Trạng thái này ngăn chặn double-processing.
     */
    PROCESSING,

    /**
     * Tất cả users trong batch đã được xử lý thành công.
     */
    COMPLETED,

    /**
     * Một số users xử lý thất bại, một số thành công.
     * Cần review và retry các items failed.
     */
    PARTIAL_FAILED,

    /**
     * Tất cả users trong batch đều thất bại.
     */
    FAILED
}
