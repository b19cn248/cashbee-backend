package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.MilestoneConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for MilestoneConfig entity.
 *
 * @author CashBee Team
 */
@Repository
public interface MilestoneConfigJpaRepository extends JpaRepository<MilestoneConfigJpaEntity, Long> {

    /**
     * Find all active milestone configs.
     */
    List<MilestoneConfigJpaEntity> findByIsActiveTrue();

    /**
     * Find all milestone configs by type.
     */
    List<MilestoneConfigJpaEntity> findByMilestoneType(String milestoneType);

    /**
     * Find active milestone configs by type.
     */
    List<MilestoneConfigJpaEntity> findByMilestoneTypeAndIsActiveTrue(String milestoneType);

    /**
     * Find milestone config by type and orders required.
     */
    Optional<MilestoneConfigJpaEntity> findByMilestoneTypeAndOrdersRequired(
            String milestoneType,
            Integer ordersRequired
    );

    /**
     * Find active milestone config by type and orders required.
     */
    Optional<MilestoneConfigJpaEntity> findByMilestoneTypeAndOrdersRequiredAndIsActiveTrue(
            String milestoneType,
            Integer ordersRequired
    );

    /**
     * Find next milestones (where orders_required > currentOrders).
     */
    @Query("SELECT m FROM MilestoneConfigJpaEntity m " +
           "WHERE m.milestoneType = :milestoneType " +
           "AND m.ordersRequired > :currentOrders " +
           "AND m.isActive = true " +
           "ORDER BY m.ordersRequired ASC")
    List<MilestoneConfigJpaEntity> findNextMilestones(
            @Param("milestoneType") String milestoneType,
            @Param("currentOrders") Integer currentOrders
    );

    /**
     * Check if milestone config exists for type and orders.
     */
    boolean existsByMilestoneTypeAndOrdersRequired(String milestoneType, Integer ordersRequired);
}
