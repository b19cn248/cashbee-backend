package com.cashbee.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BatchCashbackSnapshot Domain Model.
 *
 * Snapshot of which cashbacks are included in each batch at creation time.
 * When completing batch, only these specific cashbacks will be marked as PAID.
 *
 * This solves the race condition problem:
 * 1. Admin creates batch (based on wallet balance)
 * 2. New orders get CONFIRMED (adding to wallet balance)
 * 3. Admin completes batch
 * 4. Without snapshot: ALL CONFIRMED cashbacks get PAID (wrong!)
 * 5. With snapshot: Only original cashbacks get PAID (correct!)
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
public class BatchCashbackSnapshot {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Reference to batch_transfer_export.
     */
    private Long batchId;

    /**
     * Reference to cashback.
     */
    private Long cashbackId;

    /**
     * User ID for easier querying (denormalized from cashback).
     */
    private Long userId;

    /**
     * Amount at snapshot time (for audit trail).
     * This is the cashback amount at the time batch was created.
     */
    private BigDecimal amount;

    /**
     * Timestamp when snapshot was created.
     */
    private LocalDateTime createdAt;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     */
    public void validate() {
        if (batchId == null) {
            throw new IllegalStateException("Batch ID is required");
        }

        if (cashbackId == null) {
            throw new IllegalStateException("Cashback ID is required");
        }

        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Amount must be non-negative");
        }
    }

    /**
     * Create snapshot from cashback.
     *
     * @param batchId Batch ID
     * @param cashback Cashback to snapshot
     * @return New BatchCashbackSnapshot
     */
    public static BatchCashbackSnapshot fromCashback(Long batchId, Cashback cashback) {
        return BatchCashbackSnapshot.builder()
                .batchId(batchId)
                .cashbackId(cashback.getId())
                .userId(cashback.getUserId())
                .amount(cashback.getCashbackAmount())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
