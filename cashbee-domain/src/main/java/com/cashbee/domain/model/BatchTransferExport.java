package com.cashbee.domain.model;

import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.enums.ExportType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BatchTransferExport Domain Model.
 *
 * Lưu metadata của mỗi lần export file chuyển khoản lô.
 *
 * Business Purpose:
 * - Tracking lịch sử export
 * - Lưu thông tin: bao nhiêu users, tổng tiền, ai export, khi nào
 * - KHÔNG lưu file thực tế (file generate realtime khi download)
 *
 * Design:
 * - Immutable batch code (unique identifier)
 * - Chỉ lưu metadata, không lưu file content
 * - Có thể xem lại lịch sử export
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
public class BatchTransferExport {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Batch code (unique identifier).
     * Format: BATCH_YYYYMMDD_XXX
     * Example: BATCH_20251119_001
     */
    private String batchCode;

    /**
     * File name (for reference).
     * Example: BATCH_20251119_001.xls
     */
    private String fileName;

    /**
     * Tổng số users trong batch.
     */
    @Builder.Default
    private Integer totalUsers = 0;

    /**
     * Tổng số tiền cần chuyển (VND).
     */
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Trạng thái export.
     */
    @Builder.Default
    private ExportStatus status = ExportStatus.PENDING;

    /**
     * Loại export (MANUAL hoặc SCHEDULED).
     */
    @Builder.Default
    private ExportType exportType = ExportType.MANUAL;

    /**
     * Admin user ID (người export).
     * Null nếu là SCHEDULED job.
     */
    private Long exportedBy;

    /**
     * Template nội dung chuyển khoản.
     * Example: "Hoan tien CashBee 11/2025"
     */
    private String remarkTemplate;

    /**
     * Minimum balance criteria used when creating this batch.
     * Default: 50,000 VND
     */
    @Builder.Default
    private BigDecimal minBalance = new BigDecimal("50000");

    /**
     * Timestamp when batch was created.
     */
    private LocalDateTime createdAt;

    /**
     * Admin user ID who completed the batch.
     * Null if batch is not yet completed.
     */
    private Long completedBy;

    /**
     * Timestamp when batch was completed.
     */
    private LocalDateTime completedAt;

    /**
     * Number of items processed successfully.
     */
    @Builder.Default
    private Integer successCount = 0;

    /**
     * Number of items that failed processing.
     */
    @Builder.Default
    private Integer failedCount = 0;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (batchCode == null || batchCode.isBlank()) {
            throw new IllegalStateException("Batch code is required");
        }

        if (batchCode.length() > 50) {
            throw new IllegalStateException("Batch code too long (max 50 characters)");
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("File name is required");
        }

        if (fileName.length() > 255) {
            throw new IllegalStateException("File name too long (max 255 characters)");
        }

        if (totalUsers == null || totalUsers < 0) {
            throw new IllegalStateException("Total users must be >= 0");
        }

        if (totalAmount == null) {
            throw new IllegalStateException("Total amount is required");
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Total amount must be >= 0");
        }

        if (status == null) {
            throw new IllegalStateException("Status is required");
        }

        if (exportType == null) {
            throw new IllegalStateException("Export type is required");
        }
    }

    /**
     * Mark export as processing (prevent double-processing).
     */
    public void markAsProcessing() {
        this.status = ExportStatus.PROCESSING;
    }

    /**
     * Mark export as completed with statistics.
     *
     * @param adminId ID of admin who completed the batch
     * @param successCount Number of items processed successfully
     * @param failedCount Number of items that failed
     */
    public void markAsCompleted(Long adminId, int successCount, int failedCount) {
        this.completedBy = adminId;
        this.completedAt = LocalDateTime.now();
        this.successCount = successCount;
        this.failedCount = failedCount;

        if (failedCount == 0) {
            this.status = ExportStatus.COMPLETED;
        } else if (successCount == 0) {
            this.status = ExportStatus.FAILED;
        } else {
            this.status = ExportStatus.PARTIAL_FAILED;
        }
    }

    /**
     * Mark export as completed (all success).
     * @deprecated Use {@link #markAsCompleted(Long, int, int)} instead
     */
    @Deprecated
    public void markAsCompleted() {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark export as failed (all failed).
     */
    public void markAsFailed() {
        this.status = ExportStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Check if batch is currently processing.
     */
    public boolean isProcessing() {
        return ExportStatus.PROCESSING.equals(this.status);
    }

    /**
     * Check if batch can be processed (only PENDING batches).
     */
    public boolean canBeProcessed() {
        return ExportStatus.PENDING.equals(this.status);
    }

    /**
     * Check if batch has any failed items.
     */
    public boolean hasFailedItems() {
        return this.failedCount != null && this.failedCount > 0;
    }

    /**
     * Check if this is a new batch (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Check if export is manual.
     *
     * @return true if export type is MANUAL
     */
    public boolean isManual() {
        return ExportType.MANUAL.equals(this.exportType);
    }

    /**
     * Check if export is scheduled.
     *
     * @return true if export type is SCHEDULED
     */
    public boolean isScheduled() {
        return ExportType.SCHEDULED.equals(this.exportType);
    }
}
