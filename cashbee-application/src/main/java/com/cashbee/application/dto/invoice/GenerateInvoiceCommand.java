package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Command DTO for generating payment invoice.
 *
 * Contains all information needed to create an invoice
 * when a batch payment is completed.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateInvoiceCommand {

    /**
     * Batch ID that this invoice belongs to.
     */
    private Long batchId;

    /**
     * Batch item ID for this specific user's transfer.
     */
    private Long batchItemId;

    /**
     * User ID who receives this invoice.
     */
    private Long userId;

    /**
     * Total amount transferred.
     */
    private BigDecimal amount;

    /**
     * Currency code (default: VND).
     */
    @Builder.Default
    private String currency = "VND";

    /**
     * User's bank account number.
     */
    private String bankAccountNumber;

    /**
     * User's bank name.
     */
    private String bankName;

    /**
     * Transfer reference/transaction ID from bank.
     */
    private String transferReference;

    /**
     * Time when transfer was completed.
     */
    private LocalDateTime transferTime;

    /**
     * Transfer status (e.g., SUCCESS, PENDING).
     */
    @Builder.Default
    private String transferStatus = "SUCCESS";

    /**
     * Total number of orders included.
     */
    private Integer totalOrders;

    /**
     * Order details by platform.
     */
    private List<PlatformOrderDetail> platformOrders;

    /**
     * Detail for orders from a specific platform.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformOrderDetail {
        private String platformCode;
        private Integer orderCount;
        private BigDecimal totalAmount;
    }
}
