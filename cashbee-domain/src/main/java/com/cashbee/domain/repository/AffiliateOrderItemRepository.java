package com.cashbee.domain.repository;

import com.cashbee.domain.model.AffiliateOrderItem;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AffiliateOrderItem entity.
 * This is a PORT in hexagonal architecture.
 *
 * @author CashBee Team
 */
public interface AffiliateOrderItemRepository {

    /**
     * Save an order item (create or update).
     */
    AffiliateOrderItem save(AffiliateOrderItem item);

    /**
     * Save multiple order items at once.
     */
    List<AffiliateOrderItem> saveAll(List<AffiliateOrderItem> items);

    /**
     * Find item by ID.
     */
    Optional<AffiliateOrderItem> findById(Long id);

    /**
     * Find all items for a specific order.
     * This is the KEY method!
     */
    List<AffiliateOrderItem> findByOrderId(Long orderId);

    /**
     * Find all items.
     */
    List<AffiliateOrderItem> findAll();

    /**
     * Delete item by ID.
     */
    void deleteById(Long id);

    /**
     * Delete all items for an order.
     */
    void deleteByOrderId(Long orderId);

    /**
     * Count items for an order.
     */
    long countByOrderId(Long orderId);

    /**
     * Find item by order ID, item ID, and model ID (unique combination).
     * Model ID distinguishes different variants (color/size) of the same item.
     */
    Optional<AffiliateOrderItem> findByOrderIdAndItemIdAndModelId(Long orderId, String itemId, String modelId);

    /**
     * Find item by order ID and item ID (may return multiple if same item has different models).
     * @deprecated Use findByOrderIdAndItemIdAndModelId for unique lookup
     */
    @Deprecated
    Optional<AffiliateOrderItem> findByOrderIdAndItemId(Long orderId, String itemId);
}
