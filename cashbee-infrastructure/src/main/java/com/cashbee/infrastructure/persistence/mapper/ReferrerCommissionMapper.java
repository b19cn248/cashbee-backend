package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.enums.ReferrerCommissionStatus;
import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.infrastructure.persistence.entity.ReferrerCommissionJpaEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between ReferrerCommission domain model and JPA entity.
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReferrerCommissionMapper {

    /**
     * Convert domain model to JPA entity.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    ReferrerCommissionJpaEntity toEntity(ReferrerCommission domain);

    /**
     * Convert JPA entity to domain model.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    ReferrerCommission toDomain(ReferrerCommissionJpaEntity entity);

    /**
     * Convert list of JPA entities to domain models.
     */
    List<ReferrerCommission> toDomainList(List<ReferrerCommissionJpaEntity> entities);

    /**
     * Update existing JPA entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    void updateEntityFromDomain(ReferrerCommission domain, @MappingTarget ReferrerCommissionJpaEntity entity);

    // ===== Enum Converters =====

    @Named("statusToString")
    default String statusToString(ReferrerCommissionStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default ReferrerCommissionStatus stringToStatus(String status) {
        return status != null ? ReferrerCommissionStatus.valueOf(status) : null;
    }
}
