package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.infrastructure.entity.AffiliateClickJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for AffiliateClick.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AffiliateClickMapper {

    AffiliateClickJpaEntity toEntity(AffiliateClick domain);

    AffiliateClick toDomain(AffiliateClickJpaEntity entity);
}
