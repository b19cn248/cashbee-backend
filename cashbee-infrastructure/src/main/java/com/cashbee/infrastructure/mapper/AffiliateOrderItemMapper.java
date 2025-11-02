package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.infrastructure.entity.AffiliateOrderItemJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between AffiliateOrderItem (domain) and AffiliateOrderItemJpaEntity (infrastructure).
 * Auto-generates implementation at compile time.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AffiliateOrderItemMapper {

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain domain model
     * @return JPA entity
     */
    AffiliateOrderItemJpaEntity toEntity(AffiliateOrderItem domain);

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return domain model
     */
    AffiliateOrderItem toDomain(AffiliateOrderItemJpaEntity entity);
}
