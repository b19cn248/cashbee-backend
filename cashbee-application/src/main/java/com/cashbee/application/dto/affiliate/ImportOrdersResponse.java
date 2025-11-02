package com.cashbee.application.dto.affiliate;

import com.cashbee.domain.enums.ImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for import orders operation.
 *
 * Contains statistics and details about the CSV import process.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportOrdersResponse {

    /**
     * Import batch ID.
     */
    private Long batchId;

    /**
     * Platform name (e.g., "Shopee").
     */
    private String platformName;

    /**
     * Platform code (e.g., "shopee").
     */
    private String platformCode;

    /**
     * Original file name uploaded.
     */
    private String fileName;

    /**
     * Import status.
     */
    private ImportStatus status;

    /**
     * Total rows in CSV file (excluding header).
     */
    private Integer totalRows;

    /**
     * Number of orders successfully imported.
     */
    private Integer successCount;

    /**
     * Number of orders that failed to import.
     */
    private Integer failedCount;

    /**
     * Number of orders skipped (e.g., duplicates).
     */
    private Integer skippedCount;

    /**
     * Number of orders matched with clicks.
     */
    private Integer matchedCount;

    /**
     * Success rate as percentage (0-100).
     */
    private Double successRate;

    /**
     * Error message if import failed.
     */
    private String errorMessage;

    /**
     * List of error details for failed rows.
     */
    @Builder.Default
    private List<ImportErrorDetail> errors = new ArrayList<>();

    /**
     * Timestamp when import started.
     */
    private LocalDateTime startedAt;

    /**
     * Timestamp when import completed.
     */
    private LocalDateTime completedAt;

    /**
     * Duration in seconds.
     */
    private Long durationSeconds;

    /**
     * Admin user ID who imported the file.
     */
    private Long importedBy;

    /**
     * Message to display to user.
     */
    private String message;

    /**
     * DTO for individual import error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportErrorDetail {
        /**
         * Row number in CSV (1-based, excluding header).
         */
        private Integer rowNumber;

        /**
         * Order ID from CSV (if available).
         */
        private String orderId;

        /**
         * Error message.
         */
        private String error;

        /**
         * Raw CSV row data (for debugging).
         */
        private String rawData;
    }
}
