package com.cashbee.domain.model;

import com.cashbee.domain.enums.ImportStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ImportBatch Domain Model.
 * Represents a batch of orders imported from CSV file.
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
public class ImportBatch {

    /**
     * Internal database ID.
     */
    private Long id;

    /**
     * Platform this import is for (Shopee, Lazada, etc.)
     */
    private Long platformId;

    /**
     * Original CSV filename.
     */
    private String fileName;

    /**
     * Total rows in CSV file.
     */
    private Integer totalRows;

    /**
     * Number of successfully imported rows.
     */
    @Builder.Default
    private Integer successCount = 0;

    /**
     * Number of failed rows.
     */
    @Builder.Default
    private Integer failedCount = 0;

    /**
     * Number of rows skipped (duplicates, etc.)
     */
    @Builder.Default
    private Integer skippedCount = 0;

    /**
     * Current status of import.
     */
    @Builder.Default
    private ImportStatus status = ImportStatus.PROCESSING;

    /**
     * Error messages if any.
     */
    private String errorMessage;

    /**
     * When import started.
     */
    private LocalDateTime createdAt;

    /**
     * When import completed.
     */
    private LocalDateTime completedAt;

    /**
     * User who initiated the import (admin).
     */
    private Long importedBy;

    // ===== Business Logic Methods =====

    /**
     * Mark import as completed successfully.
     */
    public void markAsCompleted() {
        this.status = ImportStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark import as failed.
     */
    public void markAsFailed(String errorMessage) {
        this.status = ImportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark import as partial (some succeeded, some failed).
     */
    public void markAsPartial() {
        if (this.failedCount > 0 && this.successCount > 0) {
            this.status = ImportStatus.PARTIAL;
        } else if (this.failedCount > 0) {
            this.status = ImportStatus.FAILED;
        } else {
            this.status = ImportStatus.COMPLETED;
        }
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Increment success count.
     */
    public void incrementSuccess() {
        this.successCount++;
    }

    /**
     * Increment failed count.
     */
    public void incrementFailed() {
        this.failedCount++;
    }

    /**
     * Increment skipped count.
     */
    public void incrementSkipped() {
        this.skippedCount++;
    }

    /**
     * Calculate success rate.
     */
    public double getSuccessRate() {
        if (this.totalRows == null || this.totalRows == 0) {
            return 0.0;
        }
        return (this.successCount * 100.0) / this.totalRows;
    }

    /**
     * Check if import is still processing.
     */
    public boolean isProcessing() {
        return this.status == ImportStatus.PROCESSING;
    }

    /**
     * Check if import is done (completed, failed, or partial).
     */
    public boolean isDone() {
        return this.status == ImportStatus.COMPLETED
            || this.status == ImportStatus.FAILED
            || this.status == ImportStatus.PARTIAL;
    }

    /**
     * Get processing duration in seconds.
     */
    public long getProcessingDurationSeconds() {
        if (this.completedAt == null) {
            return java.time.temporal.ChronoUnit.SECONDS.between(
                this.createdAt,
                LocalDateTime.now()
            );
        }
        return java.time.temporal.ChronoUnit.SECONDS.between(
            this.createdAt,
            this.completedAt
        );
    }

    /**
     * Validate import batch data.
     */
    public void validate() {
        if (this.platformId == null) {
            throw new IllegalStateException("Platform ID is required");
        }

        if (this.fileName == null || this.fileName.isBlank()) {
            throw new IllegalStateException("File name is required");
        }

        if (this.totalRows == null || this.totalRows < 0) {
            throw new IllegalStateException("Total rows must be >= 0");
        }
    }

    /**
     * Check if this is a new batch (not persisted yet).
     */
    public boolean isNew() {
        return this.id == null;
    }
}
