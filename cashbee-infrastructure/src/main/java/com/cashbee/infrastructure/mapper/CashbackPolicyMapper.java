package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.CashbackPolicy;
import com.cashbee.infrastructure.persistence.entity.CashbackPolicyJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for CashbackPolicy.
 *
 * @author CashBee Team
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CashbackPolicyMapper {

    /**
     * Convert JPA entity to domain model.
     */
    CashbackPolicy toDomain(CashbackPolicyJpaEntity entity);

    /**
     * Convert domain model to JPA entity.
     */
    CashbackPolicyJpaEntity toEntity(CashbackPolicy domain);
}
