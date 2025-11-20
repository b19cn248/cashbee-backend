package com.cashbee.application.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for exporting batch transfer file.
 *
 * Used when admin manually exports file or scheduled job runs.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportBatchTransferRequest {

    /**
     * Minimum balance required to include user in export.
     * Default: 50,000 VND
     */
    @Builder.Default
    private BigDecimal minBalance = new BigDecimal("50000");

    /**
     * Template for remark/content field in Excel.
     * Optional. If not provided, will auto-generate: "Hoan tien CashBee MM/YYYY"
     *
     * Example: "Hoan tien CashBee 11/2025"
     */
    private String remarkTemplate;

    /**
     * Export type (for tracking).
     * Optional. Default: MANUAL
     */
    private String exportType;
}
