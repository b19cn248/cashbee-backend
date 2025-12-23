package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.BatchTransferItem;
import com.cashbee.infrastructure.persistence.entity.BatchTransferItemJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between BatchTransferItem domain model and JPA entity.
 *
 * @author CashBee Team
 */
@Component
public class BatchTransferItemMapper {

    /**
     * Convert JPA entity to domain model.
     */
    public BatchTransferItem toDomain(BatchTransferItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BatchTransferItem.builder()
                .id(entity.getId())
                .batchId(entity.getBatchId())
                .userId(entity.getUserId())
                .walletId(entity.getWalletId())
                .amount(entity.getAmount())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                .bankName(entity.getBankName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .errorMessage(entity.getErrorMessage())
                .actualAmountDeducted(entity.getActualAmountDeducted())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     */
    public BatchTransferItemJpaEntity toEntity(BatchTransferItem domain) {
        if (domain == null) {
            return null;
        }

        return BatchTransferItemJpaEntity.builder()
                .id(domain.getId())
                .batchId(domain.getBatchId())
                .userId(domain.getUserId())
                .walletId(domain.getWalletId())
                .amount(domain.getAmount())
                .accountNumber(domain.getAccountNumber())
                .accountName(domain.getAccountName())
                .bankName(domain.getBankName())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .completedAt(domain.getCompletedAt())
                .errorMessage(domain.getErrorMessage())
                .actualAmountDeducted(domain.getActualAmountDeducted())
                .build();
    }
}
