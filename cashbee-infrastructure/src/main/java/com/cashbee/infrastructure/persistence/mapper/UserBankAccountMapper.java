package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.UserBankAccount;
import com.cashbee.infrastructure.persistence.entity.UserBankAccountJpaEntity;

/**
 * Mapper between UserBankAccount domain model and UserBankAccountJpaEntity.
 *
 * This mapper converts between domain objects (business logic)
 * and JPA entities (database persistence).
 *
 * @author CashBee Team
 */
public class UserBankAccountMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    public static UserBankAccount toDomain(UserBankAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserBankAccount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                .bankName(entity.getBankName())
                .bankCode(entity.getBankCode())
                .isDefault(entity.getIsDefault())
                .verified(entity.getVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    public static UserBankAccountJpaEntity toEntity(UserBankAccount domain) {
        if (domain == null) {
            return null;
        }

        return UserBankAccountJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .accountNumber(domain.getAccountNumber())
                .accountName(domain.getAccountName())
                .bankName(domain.getBankName())
                .bankCode(domain.getBankCode())
                .isDefault(domain.getIsDefault())
                .verified(domain.getVerified())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    // Private constructor to prevent instantiation
    private UserBankAccountMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
}
