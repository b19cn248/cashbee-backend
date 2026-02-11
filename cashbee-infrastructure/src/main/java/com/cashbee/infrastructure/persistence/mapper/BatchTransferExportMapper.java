package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.infrastructure.persistence.entity.BatchTransferExportJpaEntity;

/**
 * Mapper between BatchTransferExport domain model and BatchTransferExportJpaEntity.
 *
 * This mapper converts between domain objects (business logic)
 * and JPA entities (database persistence).
 *
 * @author CashBee Team
 */
public class BatchTransferExportMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    public static BatchTransferExport toDomain(BatchTransferExportJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BatchTransferExport.builder()
                .id(entity.getId())
                .batchCode(entity.getBatchCode())
                .fileName(entity.getFileName())
                .totalUsers(entity.getTotalUsers())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .exportType(entity.getExportType())
                .exportedBy(entity.getExportedBy())
                .remarkTemplate(entity.getRemarkTemplate())
                .minBalance(entity.getMinBalance())
                .createdAt(entity.getCreatedAt())
                .completedBy(entity.getCompletedBy())
                .completedAt(entity.getCompletedAt())
                .successCount(entity.getSuccessCount())
                .failedCount(entity.getFailedCount())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    public static BatchTransferExportJpaEntity toEntity(BatchTransferExport domain) {
        if (domain == null) {
            return null;
        }

        return BatchTransferExportJpaEntity.builder()
                .id(domain.getId())
                .batchCode(domain.getBatchCode())
                .fileName(domain.getFileName())
                .totalUsers(domain.getTotalUsers())
                .totalAmount(domain.getTotalAmount())
                .status(domain.getStatus())
                .exportType(domain.getExportType())
                .exportedBy(domain.getExportedBy())
                .remarkTemplate(domain.getRemarkTemplate())
                .minBalance(domain.getMinBalance())
                .createdAt(domain.getCreatedAt())
                .completedBy(domain.getCompletedBy())
                .completedAt(domain.getCompletedAt())
                .successCount(domain.getSuccessCount())
                .failedCount(domain.getFailedCount())
                .build();
    }

    // Private constructor to prevent instantiation
    private BatchTransferExportMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
}
