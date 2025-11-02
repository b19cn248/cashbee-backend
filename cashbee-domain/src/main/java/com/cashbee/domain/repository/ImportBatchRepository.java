package com.cashbee.domain.repository;

import com.cashbee.domain.enums.ImportStatus;
import com.cashbee.domain.model.ImportBatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ImportBatch entity.
 * This is a PORT in hexagonal architecture.
 *
 * @author CashBee Team
 */
public interface ImportBatchRepository {

    /**
     * Save an import batch (create or update).
     */
    ImportBatch save(ImportBatch batch);

    /**
     * Find batch by ID.
     */
    Optional<ImportBatch> findById(Long id);

    /**
     * Find all batches.
     */
    List<ImportBatch> findAll();

    /**
     * Find batches by platform.
     */
    List<ImportBatch> findByPlatformId(Long platformId);

    /**
     * Find batches by status.
     */
    List<ImportBatch> findByStatus(ImportStatus status);

    /**
     * Find batches by date range.
     */
    List<ImportBatch> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find latest batch for platform.
     */
    Optional<ImportBatch> findLatestByPlatformId(Long platformId);

    /**
     * Find batches imported by specific user.
     */
    List<ImportBatch> findByImportedBy(Long userId);

    /**
     * Count total batches.
     */
    long count();

    /**
     * Count batches by status.
     */
    long countByStatus(ImportStatus status);

    /**
     * Delete batch by ID.
     */
    void deleteById(Long id);
}
