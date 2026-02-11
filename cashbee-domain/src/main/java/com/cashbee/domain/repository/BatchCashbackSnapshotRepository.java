package com.cashbee.domain.repository;

import com.cashbee.domain.model.BatchCashbackSnapshot;

import java.util.List;

/**
 * Repository interface for BatchCashbackSnapshot domain model.
 *
 * @author CashBee Team
 */
public interface BatchCashbackSnapshotRepository {

    /**
     * Save a batch cashback snapshot.
     */
    BatchCashbackSnapshot save(BatchCashbackSnapshot snapshot);

    /**
     * Save multiple batch cashback snapshots.
     */
    List<BatchCashbackSnapshot> saveAll(List<BatchCashbackSnapshot> snapshots);

    /**
     * Find all snapshots by batch ID.
     */
    List<BatchCashbackSnapshot> findByBatchId(Long batchId);

    /**
     * Find all snapshots by batch ID and user ID.
     * Used when completing batch to get specific user's cashbacks.
     */
    List<BatchCashbackSnapshot> findByBatchIdAndUserId(Long batchId, Long userId);

    /**
     * Get all cashback IDs for a batch.
     * Returns only the cashback IDs for efficient lookup.
     */
    List<Long> findCashbackIdsByBatchId(Long batchId);

    /**
     * Get cashback IDs for a specific user in a batch.
     */
    List<Long> findCashbackIdsByBatchIdAndUserId(Long batchId, Long userId);

    /**
     * Count snapshots by batch ID.
     */
    long countByBatchId(Long batchId);

    /**
     * Delete all snapshots by batch ID.
     * Used when batch is deleted or needs to be regenerated.
     */
    void deleteByBatchId(Long batchId);

    /**
     * Check if snapshot exists for a batch and cashback.
     */
    boolean existsByBatchIdAndCashbackId(Long batchId, Long cashbackId);
}
