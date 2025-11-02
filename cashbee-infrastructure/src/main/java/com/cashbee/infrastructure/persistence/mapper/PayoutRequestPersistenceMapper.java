package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.infrastructure.persistence.entity.PayoutRequestJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for PayoutRequest domain model ↔ PayoutRequestJpaEntity.
 *
 * Converts between:
 * - Domain model (PayoutRequest) - pure domain logic, no JPA
 * - JPA entity (PayoutRequestJpaEntity) - database representation
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PayoutRequestPersistenceMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    PayoutRequest toDomain(PayoutRequestJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    PayoutRequestJpaEntity toEntity(PayoutRequest domain);

    /**
     * Convert list of JPA entities to list of domain models.
     *
     * @param entities List of JPA entities
     * @return List of domain models
     */
    List<PayoutRequest> toDomainList(List<PayoutRequestJpaEntity> entities);

    /**
     * Convert list of domain models to list of JPA entities.
     *
     * @param domains List of domain models
     * @return List of JPA entities
     */
    List<PayoutRequestJpaEntity> toEntityList(List<PayoutRequest> domains);
}
