package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.ExportType;
import com.cashbee.infrastructure.persistence.entity.BatchTransferExportJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find batch export by batch code with pessimistic write lock.
     * Use this when updating batch status to prevent race conditions.
     *
     * @param batchCode Batch code
     * @return Batch export if found (locked for update)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchTransferExportJpaEntity b WHERE b.batchCode = :batchCode")
    Optional<BatchTransferExportJpaEntity> findByBatchCodeForUpdate(@Param("batchCode") String batchCode);

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
