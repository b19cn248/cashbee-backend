package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between AffiliateOrder (domain) and AffiliateOrderJpaEntity (infrastructure).
 * Auto-generates implementation at compile time.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AffiliateOrderMapper {

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain domain model
     * @return JPA entity
     */
    AffiliateOrderJpaEntity toEntity(AffiliateOrder domain);

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return domain model
     */
    AffiliateOrder toDomain(AffiliateOrderJpaEntity entity);
}
