package com.cashbee.infrastructure.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AffiliateOrderJpaEntity.
 * Infrastructure layer - JPA implementation.
 *
 * @author CashBee Team
 */
@Repository
public interface AffiliateOrderJpaRepository extends JpaRepository<AffiliateOrderJpaEntity, Long> {

    /**
     * Find order by platform order ID.
     */
    Optional<AffiliateOrderJpaEntity> findByOrderId(String orderId);

    /**
     * Find all orders for a specific user (WITHOUT pagination).
     * @deprecated Use findByUserId(userId, pageable) for better performance
     */
    @Deprecated
    List<AffiliateOrderJpaEntity> findByUserId(Long userId);

    /**
     * Find orders for a specific user with PAGINATION.
     * This is the RECOMMENDED method for user viewing their orders!
     */
    Page<AffiliateOrderJpaEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * Find orders by user and status (WITHOUT pagination).
     * @deprecated Use findByUserIdAndOrderStatus(userId, status, pageable) for better performance
     */
    @Deprecated
    List<AffiliateOrderJpaEntity> findByUserIdAndOrderStatus(Long userId, OrderStatus status);

    /**
     * Find orders by user and status with PAGINATION.
     */
    Page<AffiliateOrderJpaEntity> findByUserIdAndOrderStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Find orders by platform.
     */
    List<AffiliateOrderJpaEntity> findByPlatformId(Long platformId);

    /**
     * Find orders by import batch.
     */
    List<AffiliateOrderJpaEntity> findByImportBatchId(Long importBatchId);

    /**
     * Find orders by status.
     */
    List<AffiliateOrderJpaEntity> findByOrderStatus(OrderStatus status);

    /**
     * Check if order exists by platform order ID.
     */
    boolean existsByOrderId(String orderId);

    /**
     * Count total orders for user.
     */
    long countByUserId(Long userId);

    /**
     * Find orders by user and cashback status with PAGINATION.
     * Joins with cashback table to filter by cashback.status instead of order.order_status.
     *
     * This query:
     * - Uses INNER JOIN to only return orders that have cashback records
     * - Filters by cashback.status (PAID, CONFIRMED, PENDING, etc.)
     * - Supports pagination and sorting
     *
     * @param userId User ID
     * @param cashbackStatus Cashback status to filter
     * @param pageable Pagination parameters
     * @return Paginated orders matching the cashback status
     */
    @Query("""
        SELECT DISTINCT o FROM AffiliateOrderJpaEntity o
        INNER JOIN com.cashbee.infrastructure.persistence.entity.CashbackJpaEntity c ON c.orderId = o.id
        WHERE o.userId = :userId AND c.status = :cashbackStatus
        """)
    Page<AffiliateOrderJpaEntity> findByUserIdAndCashbackStatus(
            @Param("userId") Long userId,
            @Param("cashbackStatus") CashbackStatus cashbackStatus,
            Pageable pageable);
}
