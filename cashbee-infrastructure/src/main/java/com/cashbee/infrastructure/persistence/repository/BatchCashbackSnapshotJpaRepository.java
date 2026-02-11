package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.BatchCashbackSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for BatchCashbackSnapshot.
 *
 * @author CashBee Team
 */
@Repository
public interface BatchCashbackSnapshotJpaRepository extends JpaRepository<BatchCashbackSnapshotJpaEntity, Long> {

    /**
     * Find all snapshots by batch ID.
     */
    List<BatchCashbackSnapshotJpaEntity> findByBatchId(Long batchId);

    /**
     * Find all snapshots by batch ID and user ID.
     */
    List<BatchCashbackSnapshotJpaEntity> findByBatchIdAndUserId(Long batchId, Long userId);

    /**
     * Get all cashback IDs for a batch.
     */
    @Query("SELECT s.cashbackId FROM BatchCashbackSnapshotJpaEntity s WHERE s.batchId = :batchId")
    List<Long> findCashbackIdsByBatchId(@Param("batchId") Long batchId);

    /**
     * Get cashback IDs for a specific user in a batch.
     */
    @Query("SELECT s.cashbackId FROM BatchCashbackSnapshotJpaEntity s WHERE s.batchId = :batchId AND s.userId = :userId")
    List<Long> findCashbackIdsByBatchIdAndUserId(@Param("batchId") Long batchId, @Param("userId") Long userId);

    /**
     * Count snapshots by batch ID.
     */
    long countByBatchId(Long batchId);

    /**
     * Delete all snapshots by batch ID.
     */
    @Modifying
    @Query("DELETE FROM BatchCashbackSnapshotJpaEntity s WHERE s.batchId = :batchId")
    void deleteByBatchId(@Param("batchId") Long batchId);

    /**
     * Check if snapshot exists for a batch and cashback.
     */
    boolean existsByBatchIdAndCashbackId(Long batchId, Long cashbackId);
}
