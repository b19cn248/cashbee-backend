package com.cashbee.domain.repository;

import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateOrder;

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
     * Find all orders for a specific user.
     * This is the KEY method for user viewing their orders!
     */
    List<AffiliateOrder> findByUserId(Long userId);

    /**
     * Find orders by user and status.
     */
    List<AffiliateOrder> findByUserIdAndStatus(Long userId, OrderStatus status);

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
}
