package com.cashbee.domain.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AffiliateOrder entity.
 * This is a PORT in hexagonal architecture.
 *
 * @author CashBee Team
 */
public interface AffiliateOrderRepository {

    /**
     * Save an order (create or update).
     */
    AffiliateOrder save(AffiliateOrder order);

    /**
     * Find order by ID.
     */
    Optional<AffiliateOrder> findById(Long id);

    /**
     * Find order by platform order ID.
     */
    Optional<AffiliateOrder> findByOrderId(String orderId);

    /**
     * Find all orders.
     */
    List<AffiliateOrder> findAll();

    /**
     * Find all orders for a specific user (WITHOUT pagination).
     * WARNING: This can cause memory issues for users with many orders.
     * Use findByUserId(userId, pageable) instead.
     *
     * @deprecated Use findByUserId(userId, pageable) for better performance
     */
    @Deprecated
    List<AffiliateOrder> findByUserId(Long userId);

    /**
     * Find orders for a specific user with PAGINATION.
     * This is the RECOMMENDED method for user viewing their orders!
     * Prevents memory issues when users have many orders.
     *
     * @param userId User ID
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated orders
     */
    Page<AffiliateOrder> findByUserId(Long userId, Pageable pageable);

    /**
     * Find orders by user and status (WITHOUT pagination).
     * @deprecated Use findByUserIdAndStatus(userId, status, pageable) for better performance
     */
    @Deprecated
    List<AffiliateOrder> findByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * Find orders by user and status with PAGINATION.
     * This is the RECOMMENDED method for filtering user orders by status.
     *
     * @param userId User ID
     * @param status Order status to filter
     * @param pageable Pagination parameters
     * @return Paginated orders
     */
    Page<AffiliateOrder> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Find orders by platform.
     */
    List<AffiliateOrder> findByPlatformId(Long platformId);

    /**
     * Find orders by import batch.
     */
    List<AffiliateOrder> findByImportBatchId(Long importBatchId);

    /**
     * Find orders by status.
     */
    List<AffiliateOrder> findByStatus(OrderStatus status);

    /**
     * Check if order exists by platform order ID.
     */
    boolean existsByOrderId(String orderId);

    /**
     * Delete order by ID.
     */
    void deleteById(Long id);

    /**
     * Count total orders for user.
     */
    long countByUserId(Long userId);

    /**
     * Find orders by user and cashback status with PAGINATION.
     * This method joins with cashback table to filter by cashback status
     * instead of order status.
     *
     * Use cases:
     * - PAID: Orders where cashback has been transferred to user
     * - CONFIRMED: Orders where cashback is confirmed but not yet paid
     * - PENDING: Orders where cashback is pending confirmation
     *
     * @param userId User ID
     * @param cashbackStatus Cashback status to filter
     * @param pageable Pagination parameters
     * @return Paginated orders matching the cashback status
     */
    Page<AffiliateOrder> findByUserIdAndCashbackStatus(Long userId, CashbackStatus cashbackStatus, Pageable pageable);
}
