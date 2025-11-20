package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.Bank;
import com.cashbee.infrastructure.persistence.entity.BankJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between Bank domain model and BankJpaEntity.
 *
 * This mapper handles the translation between:
 * - Domain layer (Bank) - business logic
 * - Infrastructure layer (BankJpaEntity) - database persistence
 *
 * MapStruct generates implementation at compile time.
 *
 * @author CashBee Team
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BankMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity BankJpaEntity from database
     * @return Bank domain model
     */
    Bank toDomain(BankJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Bank domain model
     * @return BankJpaEntity for database
     */
    BankJpaEntity toEntity(Bank domain);
}
