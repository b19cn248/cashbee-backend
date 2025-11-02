package com.cashbee.infrastructure.repository;

import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Find all orders for a specific user.
     * This is the KEY method for user viewing their orders!
     */
    List<AffiliateOrderJpaEntity> findByUserId(Long userId);

    /**
     * Find orders by user and status.
     */
    List<AffiliateOrderJpaEntity> findByUserIdAndOrderStatus(Long userId, OrderStatus status);

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
}
