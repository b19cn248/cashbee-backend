package com.cashbee.application.dto.batch;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for batch cashback traceability.
 * Returns all cashbacks paid by a specific batch.
 *
 * @author CashBee Team
 */
@Getter
@Builder
public class BatchCashbackResponse {

    /**
     * Batch code (e.g., BATCH_20251119_001)
     */
    private final String batchCode;

    /**
     * Batch status (PENDING, COMPLETED)
     */
    private final String status;

    /**
     * When batch was completed
     */
    private final LocalDateTime completedAt;

    /**
     * Total number of cashbacks paid by this batch
     */
    private final int totalCashbacks;

    /**
     * Total amount of all cashbacks
     */
    private final BigDecimal totalCashbackAmount;

    /**
     * List of cashback details
     */
    private final List<CashbackDetail> cashbacks;

    /**
     * Individual cashback detail
     */
    @Getter
    @Builder
    public static class CashbackDetail {
        private final Long cashbackId;
        private final Long userId;
        private final Long orderId;
        private final Long orderItemId;
        private final BigDecimal cashbackAmount;
        private final BigDecimal commissionAmount;
        private final LocalDateTime paidAt;
        private final LocalDateTime createdAt;
    }
}
