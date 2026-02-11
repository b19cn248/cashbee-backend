package com.cashbee.application.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for batch transfer export.
 *
 * Contains metadata about the export (không chứa file).
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportBatchTransferResponse {

    /**
     * Batch code (unique identifier).
     * Format: BATCH_YYYYMMDD_XXX
     * Example: BATCH_20251119_001
     */
    private String batchCode;

    /**
     * File name.
     * Example: BATCH_20251119_001.xls
     */
    private String fileName;

    /**
     * Total users eligible for transfer.
     */
    private Integer totalUsers;

    /**
     * Total amount to transfer (VND).
     */
    private BigDecimal totalAmount;

    /**
     * When export was created.
     */
    private LocalDateTime exportedAt;

    /**
     * User-friendly message.
     * Example: "Export completed. 25 users eligible for transfer (total: 15,500,000 VND)"
     */
    private String message;

    /**
     * Export status.
     * Values: PENDING, COMPLETED, FAILED
     */
    private String status;
}
