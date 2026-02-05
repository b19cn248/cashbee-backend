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
     * Find all cashbacks paid by a specific batch for a specific user.
     * Used for generating payment invoice with platform breakdown.
     */
    List<CashbackJpaEntity> findByPaidBatchIdAndUserId(Long paidBatchId, Long userId);

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

    /**
     * Update status for specific cashbacks by their IDs (Approach B).
     * Only updates cashbacks that are in the provided list AND match the old status.
     * This ensures only cashbacks that were snapshot at batch creation are marked as PAID.
     */
    @Modifying
    @Query("UPDATE CashbackJpaEntity c SET c.status = :newStatus, c.paidAt = CURRENT_TIMESTAMP, c.updatedAt = CURRENT_TIMESTAMP, c.paidBatchId = :batchId WHERE c.id IN :cashbackIds AND c.status = :oldStatus")
    int updateStatusByCashbackIdsWithBatchId(@Param("cashbackIds") List<Long> cashbackIds, @Param("oldStatus") CashbackStatus oldStatus, @Param("newStatus") CashbackStatus newStatus, @Param("batchId") Long batchId);

    /**
     * Count distinct orders that have cashback with CONFIRMED or PAID status for a user.
     *
     * Why COUNT(DISTINCT c.orderId)?
     * - One order may have multiple items → multiple cashback records
     * - We want to count completed ORDERS, not cashback items
     *
     * Why include both CONFIRMED and PAID?
     * - CONFIRMED = order verified, user will receive cashback
     * - PAID = cashback already paid to user
     * - Both represent "completed" orders from user perspective
     */
    @Query("SELECT COUNT(DISTINCT c.orderId) FROM CashbackJpaEntity c WHERE c.userId = :userId AND c.status IN ('CONFIRMED', 'PAID')")
    int countConfirmedOrdersByUserId(@Param("userId") Long userId);

    /**
     * Find cashbacks with full order and item details for invoice display.
     *
     * This native query performs a JOIN across:
     * - cashback (c)
     * - affiliate_order (ao)
     * - affiliate_order_item (aoi)
     * - affiliate_platform (ap)
     *
     * Returns projection arrays with all necessary info for detailed invoice display.
     *
     * @param batchId Batch ID that paid these cashbacks
     * @param userId User ID
     * @return List of projection arrays containing cashback + order + item + platform info
     */
    @Query(value = """
        SELECT
            c.id AS cashback_id,
            c.cashback_amount,
            c.commission_amount AS cashback_commission,
            c.cashback_rate,
            ao.id AS order_internal_id,
            ao.order_id AS order_code,
            ao.order_time,
            ao.product_price AS order_product_price,
            ao.commission_amount AS order_commission,
            aoi.id AS item_id,
            aoi.item_name,
            aoi.shop_name,
            aoi.quantity,
            aoi.actual_amount AS item_price,
            aoi.item_commission,
            aoi.category_lv1,
            aoi.img_url,
            ap.id AS platform_id,
            ap.name AS platform_name
        FROM cashback c
        INNER JOIN affiliate_order ao ON ao.id = c.order_id
        LEFT JOIN affiliate_order_item aoi ON aoi.id = c.order_item_id
        INNER JOIN affiliate_platform ap ON ap.id = c.platform_id
        WHERE c.paid_batch_id = :batchId
          AND c.user_id = :userId
        ORDER BY ao.order_time DESC, aoi.id ASC
        """, nativeQuery = true)
    List<Object[]> findCashbacksWithOrderDetailsByBatchIdAndUserId(
            @Param("batchId") Long batchId,
            @Param("userId") Long userId);

    /**
     * Count confirmed orders with minimum amount filter (anti-abuse).
     * Uses > (greater than) not >= for the amount comparison.
     *
     * Joins with affiliate_order to check product_price.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT c.order_id)
        FROM cashback c
        INNER JOIN affiliate_order ao ON ao.id = c.order_id
        WHERE c.user_id = :userId
        AND c.status IN ('CONFIRMED', 'PAID')
        AND ao.product_price > :minAmount
        """, nativeQuery = true)
    int countConfirmedOrdersByUserIdWithMinAmount(
            @Param("userId") Long userId,
            @Param("minAmount") BigDecimal minAmount);

    /**
     * Find all user IDs that have at least one qualifying order.
     * Used by admin to batch re-process milestones.
     */
    @Query(value = """
        SELECT DISTINCT c.user_id
        FROM cashback c
        INNER JOIN affiliate_order ao ON ao.id = c.order_id
        WHERE c.status IN ('CONFIRMED', 'PAID')
        AND ao.product_price > :minAmount
        """, nativeQuery = true)
    List<Long> findUsersWithQualifyingOrders(@Param("minAmount") BigDecimal minAmount);
}
