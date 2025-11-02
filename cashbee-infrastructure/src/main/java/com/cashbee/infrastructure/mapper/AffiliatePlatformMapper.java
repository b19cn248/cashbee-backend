package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.infrastructure.persistence.entity.AffiliatePlatformJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between AffiliatePlatform domain model
 * and AffiliatePlatformJpaEntity.
 *
 * MapStruct generates implementation at compile time.
 * No manual mapping code needed.
 *
 * @author CashBee Team
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AffiliatePlatformMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return domain model
     */
    AffiliatePlatform toDomain(AffiliatePlatformJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain domain model
     * @return JPA entity
     */
    AffiliatePlatformJpaEntity toEntity(AffiliatePlatform domain);
}
