package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.infrastructure.persistence.entity.BatchTransferItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for BatchTransferItem.
 *
 * @author CashBee Team
 */
@Repository
public interface BatchTransferItemJpaRepository extends JpaRepository<BatchTransferItemJpaEntity, Long> {

    /**
     * Find all items by batch ID.
     */
    List<BatchTransferItemJpaEntity> findByBatchId(Long batchId);

    /**
     * Find all items by batch ID and status.
     */
    List<BatchTransferItemJpaEntity> findByBatchIdAndStatus(Long batchId, BatchItemStatus status);

    /**
     * Count items by batch ID.
     */
    long countByBatchId(Long batchId);

    /**
     * Count items by batch ID and status.
     */
    long countByBatchIdAndStatus(Long batchId, BatchItemStatus status);
}
