package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.ExportType;
import com.cashbee.infrastructure.persistence.entity.BatchTransferExportJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA Repository for BatchTransferExportJpaEntity.
 *
 * This repository provides database access for batch transfer exports.
 *
 * @author CashBee Team
 */
@Repository
public interface BatchTransferExportJpaRepository extends JpaRepository<BatchTransferExportJpaEntity, Long> {

    /**
     * Find batch export by batch code.
     *
     * @param batchCode Batch code
     * @return Batch export if found
     */
    Optional<BatchTransferExportJpaEntity> findByBatchCode(String batchCode);

    /**
     * Count batches created between two timestamps.
     *
     * @param startTime Start time
     * @param endTime End time
     * @return Count of batches
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find batch exports by export type.
     *
     * @param exportType Export type (MANUAL or SCHEDULED)
     * @param pageable Pagination info
     * @return Page of batch exports
     */
    Page<BatchTransferExportJpaEntity> findByExportType(ExportType exportType, Pageable pageable);
}
