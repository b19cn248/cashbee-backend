package com.cashbee.domain.model;

import com.cashbee.domain.enums.BatchItemStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BatchTransferItem Domain Model.
 *
 * Lưu snapshot chi tiết từng user trong một batch transfer.
 * Khi tạo batch, snapshot balance + bank info tại thời điểm đó.
 * Khi confirm batch, dùng amount này để trừ tiền (không phải balance hiện tại).
 *
 * Business Purpose:
 * - Snapshot amount tại thời điểm tạo batch
 * - Track trạng thái thanh toán từng user
 * - Audit trail: biết ai đã được thanh toán bao nhiêu
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class BatchTransferItem {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Reference to batch_transfer_export.
     */
    private Long batchId;

    /**
     * User ID.
     */
    private Long userId;

    /**
     * Wallet ID.
     */
    private Long walletId;

    /**
     * Snapshot amount tại thời điểm tạo batch.
     * Đây là số tiền sẽ được trừ khi confirm, không phải balance hiện tại.
     */
    private BigDecimal amount;

    /**
     * Snapshot account number.
     */
    private String accountNumber;

    /**
     * Snapshot account name.
     */
    private String accountName;

    /**
     * Snapshot bank name.
     */
    private String bankName;

    /**
     * Trạng thái: PENDING -> COMPLETED.
     */
    @Builder.Default
    private BatchItemStatus status = BatchItemStatus.PENDING;

    /**
     * Timestamp khi tạo item.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp khi hoàn thành thanh toán.
     */
    private LocalDateTime completedAt;

    /**
     * Error message if processing failed.
     * Null if successful or not yet processed.
     */
    private String errorMessage;

    /**
     * Actual amount deducted (may differ from snapshot if balance changed).
     */
    private BigDecimal actualAmountDeducted;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     */
    public void validate() {
        if (batchId == null) {
            throw new IllegalStateException("Batch ID is required");
        }

        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (walletId == null) {
            throw new IllegalStateException("Wallet ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Amount must be positive");
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("Account number is required");
        }

        if (accountName == null || accountName.isBlank()) {
            throw new IllegalStateException("Account name is required");
        }

        if (bankName == null || bankName.isBlank()) {
            throw new IllegalStateException("Bank name is required");
        }
    }

    /**
     * Mark item as processing (prevent double-processing).
     */
    public void markAsProcessing() {
        this.status = BatchItemStatus.PROCESSING;
    }

    /**
     * Mark item as completed (đã thanh toán).
     *
     * @param actualAmount Actual amount deducted from wallet
     */
    public void markAsCompleted(BigDecimal actualAmount) {
        this.status = BatchItemStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.actualAmountDeducted = actualAmount;
        this.errorMessage = null;
    }

    /**
     * Mark item as completed (đã thanh toán).
     * @deprecated Use {@link #markAsCompleted(BigDecimal)} instead
     */
    @Deprecated
    public void markAsCompleted() {
        this.status = BatchItemStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark item as failed with error message.
     *
     * @param error Error message describing why processing failed
     */
    public void markAsFailed(String error) {
        this.status = BatchItemStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = error;
    }

    /**
     * Check if item is pending.
     */
    public boolean isPending() {
        return BatchItemStatus.PENDING.equals(this.status);
    }

    /**
     * Check if item is processing.
     */
    public boolean isProcessing() {
        return BatchItemStatus.PROCESSING.equals(this.status);
    }

    /**
     * Check if item is completed.
     */
    public boolean isCompleted() {
        return BatchItemStatus.COMPLETED.equals(this.status);
    }

    /**
     * Check if item is failed.
     */
    public boolean isFailed() {
        return BatchItemStatus.FAILED.equals(this.status);
    }

    /**
     * Check if item can be retried (only FAILED items can be retried).
     */
    public boolean canRetry() {
        return BatchItemStatus.FAILED.equals(this.status);
    }
}
