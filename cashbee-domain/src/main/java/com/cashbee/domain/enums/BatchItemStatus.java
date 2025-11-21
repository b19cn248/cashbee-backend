package com.cashbee.domain.enums;

/**
 * Trạng thái của từng item trong batch transfer.
 *
 * PENDING: Chờ thanh toán (mới tạo batch)
 * COMPLETED: Đã thanh toán (admin đã confirm)
 *
 * @author CashBee Team
 */
public enum BatchItemStatus {
    /**
     * Chờ thanh toán.
     */
    PENDING,

    /**
     * Đã thanh toán.
     */
    COMPLETED
}
