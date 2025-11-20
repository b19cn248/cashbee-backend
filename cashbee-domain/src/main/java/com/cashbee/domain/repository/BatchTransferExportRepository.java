package com.cashbee.domain.repository;

import com.cashbee.domain.model.BatchTransferExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for BatchTransferExport domain model.
 *
 * This is a domain-level interface (port in hexagonal architecture).
 * Implementation will be in infrastructure layer.
 *
 * @author CashBee Team
 */
public interface BatchTransferExportRepository {

    /**
     * Find batch export by batch code.
     *
     * @param batchCode Batch code
     * @return Batch export if found
     */
    Optional<BatchTransferExport> findByBatchCode(String batchCode);

    /**
     * Save batch export.
     *
     * @param batchExport Batch export to save
     * @return Saved batch export
     */
    BatchTransferExport save(BatchTransferExport batchExport);

    /**
     * Count batches created between two timestamps.
     * Used to generate batch code (BATCH_YYYYMMDD_XXX).
     *
     * @param startTime Start time
     * @param endTime End time
     * @return Count of batches
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find all batch exports (paginated).
     *
     * @param pageable Pagination info
     * @return Page of batch exports
     */
    Page<BatchTransferExport> findAll(Pageable pageable);

    /**
     * Find batch exports by export type.
     *
     * @param exportType Export type (MANUAL or SCHEDULED)
     * @param pageable Pagination info
     * @return Page of batch exports
     */
    Page<BatchTransferExport> findByExportType(String exportType, Pageable pageable);
}
