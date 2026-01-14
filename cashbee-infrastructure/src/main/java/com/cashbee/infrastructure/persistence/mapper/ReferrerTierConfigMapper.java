package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.infrastructure.persistence.entity.ReferrerTierConfigJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for ReferrerTierConfig.
 *
 * @author CashBee Team
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReferrerTierConfigMapper {

    /**
     * Convert JPA entity to domain model.
     */
    ReferrerTierConfig toDomain(ReferrerTierConfigJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     */
    ReferrerTierConfigJpaEntity toEntity(ReferrerTierConfig domain);

    /**
     * Convert list of JPA entities to domain models.
     */
    List<ReferrerTierConfig> toDomainList(List<ReferrerTierConfigJpaEntity> entities);
}
