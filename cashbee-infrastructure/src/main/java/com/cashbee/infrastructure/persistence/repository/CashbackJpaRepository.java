package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.infrastructure.persistence.entity.CashbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Cashback.
 *
 * @author CashBee Team
 */
@Repository
public interface CashbackJpaRepository extends JpaRepository<CashbackJpaEntity, Long> {

    /**
     * Find cashback by order ID.
     *
     * @param orderId Order ID
     * @return Optional cashback entity
     */
    Optional<CashbackJpaEntity> findByOrderId(Long orderId);

    /**
     * Find all cashbacks by order ID.
     * Used when an order may have multiple items with separate cashbacks.
     *
     * @param orderId Order ID
     * @return List of cashback entities for the order
     */
    List<CashbackJpaEntity> findAllByOrderId(Long orderId);

    /**
     * Find all cashbacks for a user.
     *
     * @param userId User ID
     * @return List of cashback entities
     */
    List<CashbackJpaEntity> findByUserId(Long userId);

    /**
     * Find all cashbacks for a user with specific status.
     *
     * @param userId User ID
     * @param status Cashback status
     * @return List of cashback entities
     */
    List<CashbackJpaEntity> findByUserIdAndStatus(Long userId, CashbackStatus status);

    /**
     * Check if cashback exists for an order.
     *
     * @param orderId Order ID
     * @return true if cashback exists
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find cashback by order item ID.
     */
    Optional<CashbackJpaEntity> findByOrderItemId(Long orderItemId);

    /**
     * Check if cashback exists for an order item.
     */
    boolean existsByOrderItemId(Long orderItemId);

    /**
     * Sum cashback amount by user ID and status.
     */
    @Query("SELECT COALESCE(SUM(c.cashbackAmount), 0) FROM CashbackJpaEntity c WHERE c.userId = :userId AND c.status = :status")
    BigDecimal sumCashbackAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CashbackStatus status);

    /**
     * Sum cashback amount by user ID and statuses.
     */
    @Query("SELECT COALESCE(SUM(c.cashbackAmount), 0) FROM CashbackJpaEntity c WHERE c.userId = :userId AND c.status IN :statuses")
    BigDecimal sumCashbackAmountByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<CashbackStatus> statuses);

    /**
     * Update status for all cashbacks of a user with specific status.
     * Used when batch transfer completes: CONFIRMED → PAID
     * @deprecated Use {@link #updateStatusByUserIdAndStatusWithBatchId} instead
     */
    @Deprecated
    @Modifying
    @Query("UPDATE CashbackJpaEntity c SET c.status = :newStatus, c.paidAt = CURRENT_TIMESTAMP, c.updatedAt = CURRENT_TIMESTAMP WHERE c.userId = :userId AND c.status = :oldStatus")
    int updateStatusByUserIdAndStatus(@Param("userId") Long userId, @Param("oldStatus") CashbackStatus oldStatus, @Param("newStatus") CashbackStatus newStatus);

    /**
     * Update status for unpaid cashbacks of a user with specific status.
     * Only updates cashbacks where paid_batch_id IS NULL (not yet paid).
     * Also records which batch paid these cashbacks for traceability.
     *
     * Key differences from deprecated method:
     * 1. Only updates cashbacks where paidBatchId IS NULL
     * 2. Sets paidBatchId to track which batch paid
     */
    @Modifying
    @Query("UPDATE CashbackJpaEntity c SET c.status = :newStatus, c.paidAt = CURRENT_TIMESTAMP, c.updatedAt = CURRENT_TIMESTAMP, c.paidBatchId = :batchId WHERE c.userId = :userId AND c.status = :oldStatus AND c.paidBatchId IS NULL")
    int updateStatusByUserIdAndStatusWithBatchId(@Param("userId") Long userId, @Param("oldStatus") CashbackStatus oldStatus, @Param("newStatus") CashbackStatus newStatus, @Param("batchId") Long batchId);

    /**
     * Find all cashbacks paid by a specific batch.
     * Used for traceability: batchCode → cashbacks → orders
     */
    List<CashbackJpaEntity> findByPaidBatchId(Long paidBatchId);

    /**
     * Update status for unpaid cashbacks of a user that were CONFIRMED before batch creation.
     *
     * FIX: Only updates cashbacks where:
     * 1. paidBatchId IS NULL (not yet paid)
     * 2. confirmedAt <= batchCreatedAt (was CONFIRMED before batch was created)
     *
     * This prevents newly CONFIRMED cashbacks (after batch creation) from being marked as PAID.
     */
    @Modifying
    @Query("UPDATE CashbackJpaEntity c SET c.status = :newStatus, c.paidAt = CURRENT_TIMESTAMP, c.updatedAt = CURRENT_TIMESTAMP, c.paidBatchId = :batchId WHERE c.userId = :userId AND c.status = :oldStatus AND c.paidBatchId IS NULL AND c.confirmedAt <= :batchCreatedAt")
    int updateStatusByUserIdAndStatusWithBatchIdBeforeDate(
            @Param("userId") Long userId,
            @Param("oldStatus") CashbackStatus oldStatus,
            @Param("newStatus") CashbackStatus newStatus,
            @Param("batchId") Long batchId,
            @Param("batchCreatedAt") LocalDateTime batchCreatedAt);

    /**
     * Sum cashback amount for unpaid CONFIRMED cashbacks of a user.
     * Only counts cashbacks where paidBatchId IS NULL.
     */
    @Query("SELECT COALESCE(SUM(c.cashbackAmount), 0) FROM CashbackJpaEntity c WHERE c.userId = :userId AND c.status = 'CONFIRMED' AND c.paidBatchId IS NULL")
    BigDecimal sumUnpaidConfirmedCashbackByUserId(@Param("userId") Long userId);
}
