package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.ImportBatch;
import com.cashbee.infrastructure.entity.ImportBatchJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for ImportBatch.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ImportBatchMapper {

    ImportBatchJpaEntity toEntity(ImportBatch domain);

    ImportBatch toDomain(ImportBatchJpaEntity entity);
}
