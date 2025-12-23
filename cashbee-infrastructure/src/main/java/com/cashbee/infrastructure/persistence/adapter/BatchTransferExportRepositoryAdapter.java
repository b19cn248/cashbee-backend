package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import com.cashbee.infrastructure.persistence.entity.BatchTransferExportJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.BatchTransferExportMapper;
import com.cashbee.infrastructure.persistence.repository.BatchTransferExportJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adapter implementing BatchTransferExportRepository domain interface.
 *
 * This is the implementation of the repository port in hexagonal architecture.
 * It bridges the domain layer with the infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchTransferExportRepositoryAdapter implements BatchTransferExportRepository {

    private final BatchTransferExportJpaRepository jpaRepository;

    @Override
    public Optional<BatchTransferExport> findByBatchCode(String batchCode) {
        log.debug("Repository: Finding batch export by code: {}", batchCode);
        return jpaRepository.findByBatchCode(batchCode)
                .map(BatchTransferExportMapper::toDomain);
    }

    @Override
    public Optional<BatchTransferExport> findByBatchCodeForUpdate(String batchCode) {
        log.debug("Repository: Finding batch export by code with lock: {}", batchCode);
        return jpaRepository.findByBatchCodeForUpdate(batchCode)
                .map(BatchTransferExportMapper::toDomain);
    }

    @Override
    @Transactional
    public BatchTransferExport save(BatchTransferExport batchExport) {
        log.debug("Repository: Saving batch export: {}", batchExport.getBatchCode());

        // Validate domain model before persisting
        batchExport.validate();

        // Convert to JPA entity
        BatchTransferExportJpaEntity entity = BatchTransferExportMapper.toEntity(batchExport);

        // Save to database
        BatchTransferExportJpaEntity savedEntity = jpaRepository.save(entity);

        // Convert back to domain model
        return BatchTransferExportMapper.toDomain(savedEntity);
    }

    @Override
    public long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Repository: Counting batches between {} and {}", startTime, endTime);
        return jpaRepository.countByCreatedAtBetween(startTime, endTime);
    }

    @Override
    public Page<BatchTransferExport> findAll(Pageable pageable) {
        log.debug("Repository: Finding all batch exports (page: {})", pageable.getPageNumber());
        return jpaRepository.findAll(pageable)
                .map(BatchTransferExportMapper::toDomain);
    }

    @Override
    public Page<BatchTransferExport> findByExportType(String exportType, Pageable pageable) {
        log.debug("Repository: Finding batch exports by type: {} (page: {})", exportType, pageable.getPageNumber());
        // Convert string to enum
        com.cashbee.domain.enums.ExportType type = com.cashbee.domain.enums.ExportType.valueOf(exportType);
        return jpaRepository.findByExportType(type, pageable)
                .map(BatchTransferExportMapper::toDomain);
    }
}
