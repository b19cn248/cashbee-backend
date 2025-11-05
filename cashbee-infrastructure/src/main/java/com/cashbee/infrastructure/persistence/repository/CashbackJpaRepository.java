package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.infrastructure.persistence.entity.CashbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
