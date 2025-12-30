package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.infrastructure.persistence.entity.MilestoneConfigJpaEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between MilestoneConfig domain model and JPA entity.
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MilestoneConfigMapper {

    /**
     * Convert domain model to JPA entity.
     */
    @Mapping(target = "milestoneType", source = "milestoneType", qualifiedByName = "milestoneTypeToString")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "userLevelToString")
    MilestoneConfigJpaEntity toEntity(MilestoneConfig domain);

    /**
     * Convert JPA entity to domain model.
     */
    @Mapping(target = "milestoneType", source = "milestoneType", qualifiedByName = "stringToMilestoneType")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "stringToUserLevel")
    MilestoneConfig toDomain(MilestoneConfigJpaEntity entity);

    /**
     * Convert list of JPA entities to domain models.
     */
    List<MilestoneConfig> toDomainList(List<MilestoneConfigJpaEntity> entities);

    /**
     * Update existing JPA entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "milestoneType", source = "milestoneType", qualifiedByName = "milestoneTypeToString")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "userLevelToString")
    void updateEntityFromDomain(MilestoneConfig domain, @MappingTarget MilestoneConfigJpaEntity entity);

    // ===== Enum Converters =====

    @Named("milestoneTypeToString")
    default String milestoneTypeToString(MilestoneType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToMilestoneType")
    default MilestoneType stringToMilestoneType(String type) {
        return (type != null && !type.isBlank()) ? MilestoneType.valueOf(type) : null;
    }

    @Named("userLevelToString")
    default String userLevelToString(UserLevel level) {
        return level != null ? level.name() : null;
    }

    @Named("stringToUserLevel")
    default UserLevel stringToUserLevel(String level) {
        return (level != null && !level.isBlank()) ? UserLevel.valueOf(level) : null;
    }
}
