package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.BatchCashbackSnapshot;
import com.cashbee.infrastructure.persistence.entity.BatchCashbackSnapshotJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between BatchCashbackSnapshot domain model and JPA entity.
 *
 * @author CashBee Team
 */
@Component
public class BatchCashbackSnapshotMapper {

    /**
     * Convert JPA entity to domain model.
     */
    public BatchCashbackSnapshot toDomain(BatchCashbackSnapshotJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BatchCashbackSnapshot.builder()
                .id(entity.getId())
                .batchId(entity.getBatchId())
                .cashbackId(entity.getCashbackId())
                .userId(entity.getUserId())
                .amount(entity.getAmount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     */
    public BatchCashbackSnapshotJpaEntity toEntity(BatchCashbackSnapshot domain) {
        if (domain == null) {
            return null;
        }

        return BatchCashbackSnapshotJpaEntity.builder()
                .id(domain.getId())
                .batchId(domain.getBatchId())
                .cashbackId(domain.getCashbackId())
                .userId(domain.getUserId())
                .amount(domain.getAmount())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
