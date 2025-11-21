package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.Cashback;
import com.cashbee.infrastructure.persistence.entity.CashbackJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Cashback domain model and CashbackJpaEntity.
 *
 * @author CashBee Team
 */
@Component
public class CashbackMapper {

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Cashback domain model
     * @return CashbackJpaEntity
     */
    public CashbackJpaEntity toEntity(Cashback domain) {
        if (domain == null) {
            return null;
        }

        return CashbackJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .orderId(domain.getOrderId())
            .orderItemId(domain.getOrderItemId())
            .platformId(domain.getPlatformId())
            .commissionAmount(domain.getCommissionAmount())
            .cashbackAmount(domain.getCashbackAmount())
            .cashbackRate(domain.getCashbackRate())
            .policyId(domain.getPolicyId())
            .status(domain.getStatus())
            .note(domain.getNote())
            .createdAt(domain.getCreatedAt())
            .confirmedAt(domain.getConfirmedAt())
            .paidAt(domain.getPaidAt())
            .cancelledAt(domain.getCancelledAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity CashbackJpaEntity
     * @return Cashback domain model
     */
    public Cashback toDomain(CashbackJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Cashback.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .orderId(entity.getOrderId())
            .orderItemId(entity.getOrderItemId())
            .platformId(entity.getPlatformId())
            .commissionAmount(entity.getCommissionAmount())
            .cashbackAmount(entity.getCashbackAmount())
            .cashbackRate(entity.getCashbackRate())
            .policyId(entity.getPolicyId())
            .status(entity.getStatus())
            .note(entity.getNote())
            .createdAt(entity.getCreatedAt())
            .confirmedAt(entity.getConfirmedAt())
            .paidAt(entity.getPaidAt())
            .cancelledAt(entity.getCancelledAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
