package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.Transaction;
import com.cashbee.infrastructure.persistence.entity.TransactionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Transaction domain model ↔ TransactionJpaEntity.
 *
 * Converts between:
 * - Domain model (Transaction) - pure domain logic, no JPA
 * - JPA entity (TransactionJpaEntity) - database representation
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransactionPersistenceMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    Transaction toDomain(TransactionJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    TransactionJpaEntity toEntity(Transaction domain);

    /**
     * Convert list of JPA entities to list of domain models.
     *
     * @param entities List of JPA entities
     * @return List of domain models
     */
    List<Transaction> toDomainList(List<TransactionJpaEntity> entities);

    /**
     * Convert list of domain models to list of JPA entities.
     *
     * @param domains List of domain models
     * @return List of JPA entities
     */
    List<TransactionJpaEntity> toEntityList(List<Transaction> domains);
}
