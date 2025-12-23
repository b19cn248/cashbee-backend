package com.cashbee.domain.enums;

/**
 * Trạng thái của từng item trong batch transfer.
 *
 * @author CashBee Team
 */
public enum BatchItemStatus {
    /**
     * Chờ thanh toán (mới tạo batch, chưa process).
     */
    PENDING,

    /**
     * Đang xử lý (đang trừ tiền user này).
     * Trạng thái tạm thời trong quá trình processing.
     */
    PROCESSING,

    /**
     * Đã thanh toán thành công (đã trừ tiền, tạo transaction).
     */
    COMPLETED,

    /**
     * Xử lý thất bại (có lỗi khi trừ tiền hoặc tạo transaction).
     * Cần admin review và retry.
     */
    FAILED
}
