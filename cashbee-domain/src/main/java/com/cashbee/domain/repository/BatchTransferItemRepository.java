package com.cashbee.domain.repository;

import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.domain.model.BatchTransferItem;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BatchTransferItem domain model.
 *
 * @author CashBee Team
 */
public interface BatchTransferItemRepository {

    /**
     * Save a batch transfer item.
     */
    BatchTransferItem save(BatchTransferItem item);

    /**
     * Save multiple batch transfer items.
     */
    List<BatchTransferItem> saveAll(List<BatchTransferItem> items);

    /**
     * Find item by ID.
     */
    Optional<BatchTransferItem> findById(Long id);

    /**
     * Find all items by batch ID.
     */
    List<BatchTransferItem> findByBatchId(Long batchId);

    /**
     * Find all items by batch ID and status.
     */
    List<BatchTransferItem> findByBatchIdAndStatus(Long batchId, BatchItemStatus status);

    /**
     * Count items by batch ID.
     */
    long countByBatchId(Long batchId);

    /**
     * Count items by batch ID and status.
     */
    long countByBatchIdAndStatus(Long batchId, BatchItemStatus status);
}
