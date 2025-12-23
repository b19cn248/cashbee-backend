package com.cashbee.domain.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.model.Cashback;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Cashback.
 * Infrastructure layer will implement this interface.
 *
 * @author CashBee Team
 */
public interface CashbackRepository {

    /**
     * Save cashback.
     *
     * @param cashback Cashback to save
     * @return Saved cashback with generated ID
     */
    Cashback save(Cashback cashback);

    /**
     * Find cashback by ID.
     *
     * @param id Cashback ID
     * @return Optional cashback
     */
    Optional<Cashback> findById(Long id);

    /**
     * Find cashback by order ID.
     *
     * @param orderId Order ID
     * @return Optional cashback
     */
    Optional<Cashback> findByOrderId(Long orderId);

    /**
     * Find all cashbacks by order ID.
     * Used when an order may have multiple items with separate cashbacks.
     *
     * @param orderId Order ID
     * @return List of cashbacks for the order
     */
    List<Cashback> findAllByOrderId(Long orderId);

    /**
     * Find all cashbacks for a user.
     *
     * @param userId User ID
     * @return List of cashbacks
     */
    List<Cashback> findByUserId(Long userId);

    /**
     * Find all cashbacks for a user with specific status.
     *
     * @param userId User ID
     * @param status Cashback status
     * @return List of cashbacks
     */
    List<Cashback> findByUserIdAndStatus(Long userId, CashbackStatus status);

    /**
     * Check if cashback exists for an order.
     *
     * @param orderId Order ID
     * @return true if cashback exists
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Delete cashback by ID.
     *
     * @param id Cashback ID
     */
    void deleteById(Long id);

    /**
     * Find cashback by order item ID.
     *
     * @param orderItemId Order item ID
     * @return Optional cashback
     */
    Optional<Cashback> findByOrderItemId(Long orderItemId);

    /**
     * Check if cashback exists for an order item.
     *
     * @param orderItemId Order item ID
     * @return true if cashback exists
     */
    boolean existsByOrderItemId(Long orderItemId);

    /**
     * Sum cashback amount by user ID and status.
     *
     * @param userId User ID
     * @param status Cashback status
     * @return Sum of cashback amounts
     */
    java.math.BigDecimal sumCashbackAmountByUserIdAndStatus(Long userId, CashbackStatus status);

    /**
     * Sum cashback amount by user ID and statuses.
     *
     * @param userId User ID
     * @param statuses List of statuses
     * @return Sum of cashback amounts
     */
    java.math.BigDecimal sumCashbackAmountByUserIdAndStatusIn(Long userId, List<CashbackStatus> statuses);

    /**
     * Update status for all cashbacks of a user with specific status.
     * Used when batch transfer completes: CONFIRMED → PAID
     *
     * @param userId User ID
     * @param oldStatus Current status to match
     * @param newStatus New status to set
     * @return Number of records updated
     * @deprecated Use {@link #updateStatusByUserIdAndStatusWithBatchId} instead for proper batch tracking
     */
    @Deprecated
    int updateStatusByUserIdAndStatus(Long userId, CashbackStatus oldStatus, CashbackStatus newStatus);

    /**
     * Update status for unpaid cashbacks of a user with specific status.
     * Only updates cashbacks where paid_batch_id IS NULL (not yet paid).
     * Also records which batch paid these cashbacks for traceability.
     *
     * Used when batch transfer completes: CONFIRMED → PAID
     *
     * @param userId User ID
     * @param oldStatus Current status to match (typically CONFIRMED)
     * @param newStatus New status to set (typically PAID)
     * @param batchId Batch ID that is paying these cashbacks
     * @return Number of records updated
     */
    int updateStatusByUserIdAndStatusWithBatchId(Long userId, CashbackStatus oldStatus, CashbackStatus newStatus, Long batchId);

    /**
     * Update status for unpaid cashbacks of a user that were CONFIRMED before batch creation.
     *
     * FIX: Only updates cashbacks where:
     * 1. paidBatchId IS NULL (not yet paid)
     * 2. confirmedAt <= batchCreatedAt (was CONFIRMED before batch was created)
     *
     * This prevents newly CONFIRMED cashbacks (after batch creation) from being marked as PAID.
     *
     * @param userId User ID
     * @param oldStatus Current status to match (typically CONFIRMED)
     * @param newStatus New status to set (typically PAID)
     * @param batchId Batch ID that is paying these cashbacks
     * @param batchCreatedAt Batch creation timestamp - only cashbacks confirmed before this are updated
     * @return Number of records updated
     */
    int updateStatusByUserIdAndStatusWithBatchIdBeforeDate(
            Long userId,
            CashbackStatus oldStatus,
            CashbackStatus newStatus,
            Long batchId,
            java.time.LocalDateTime batchCreatedAt);

    /**
     * Find all cashbacks paid by a specific batch.
     * Used for traceability: batchCode → cashbacks → orders
     *
     * @param batchId Batch ID
     * @return List of cashbacks paid by this batch
     */
    List<Cashback> findByPaidBatchId(Long batchId);

    /**
     * Sum cashback amount for unpaid CONFIRMED cashbacks of a user.
     * Used for validation: batch_transfer_item.amount should match this sum.
     *
     * @param userId User ID
     * @return Sum of unpaid CONFIRMED cashback amounts
     */
    java.math.BigDecimal sumUnpaidConfirmedCashbackByUserId(Long userId);

    /**
     * Update status for specific cashbacks by their IDs (Approach B).
     * Only updates cashbacks that are in the provided list AND match the old status.
     * This ensures only cashbacks that were snapshot at batch creation are marked as PAID.
     *
     * @param cashbackIds List of cashback IDs to update (from batch_cashback_snapshot)
     * @param oldStatus Current status to match (typically CONFIRMED)
     * @param newStatus New status to set (typically PAID)
     * @param batchId Batch ID that is paying these cashbacks
     * @return Number of records updated
     */
    int updateStatusByCashbackIdsWithBatchId(List<Long> cashbackIds, CashbackStatus oldStatus, CashbackStatus newStatus, Long batchId);
}
