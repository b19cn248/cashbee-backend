package com.cashbee.infrastructure.repository;

import com.cashbee.infrastructure.entity.AffiliateOrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AffiliateOrderItemJpaEntity.
 * Infrastructure layer - JPA implementation.
 *
 * @author CashBee Team
 */
@Repository
public interface AffiliateOrderItemJpaRepository extends JpaRepository<AffiliateOrderItemJpaEntity, Long> {

    /**
     * Find all items for a specific order.
     * This is the KEY method!
     */
    List<AffiliateOrderItemJpaEntity> findByOrderId(Long orderId);

    /**
     * Delete all items for an order.
     * Will be used when deleting an order.
     */
    void deleteByOrderId(Long orderId);

    /**
     * Count items for an order.
     */
    long countByOrderId(Long orderId);

    /**
     * Find item by order ID and item ID.
     */
    Optional<AffiliateOrderItemJpaEntity> findByOrderIdAndItemId(Long orderId, String itemId);
}
