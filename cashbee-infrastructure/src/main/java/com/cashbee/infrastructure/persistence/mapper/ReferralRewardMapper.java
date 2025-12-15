package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.infrastructure.persistence.entity.ReferralRewardJpaEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between ReferralReward domain model and JPA entity.
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReferralRewardMapper {

    /**
     * Convert domain model to JPA entity.
     */
    @Mapping(target = "rewardType", source = "rewardType", qualifiedByName = "rewardTypeToString")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "userLevelToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    ReferralRewardJpaEntity toEntity(ReferralReward domain);

    /**
     * Convert JPA entity to domain model.
     */
    @Mapping(target = "rewardType", source = "rewardType", qualifiedByName = "stringToRewardType")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "stringToUserLevel")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    ReferralReward toDomain(ReferralRewardJpaEntity entity);

    /**
     * Convert list of JPA entities to domain models.
     */
    List<ReferralReward> toDomainList(List<ReferralRewardJpaEntity> entities);

    /**
     * Update existing JPA entity from domain model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "rewardType", source = "rewardType", qualifiedByName = "rewardTypeToString")
    @Mapping(target = "newTier", source = "newTier", qualifiedByName = "userLevelToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    void updateEntityFromDomain(ReferralReward domain, @MappingTarget ReferralRewardJpaEntity entity);

    // ===== Enum Converters =====

    @Named("rewardTypeToString")
    default String rewardTypeToString(ReferralRewardType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToRewardType")
    default ReferralRewardType stringToRewardType(String type) {
        return type != null ? ReferralRewardType.valueOf(type) : null;
    }

    @Named("userLevelToString")
    default String userLevelToString(UserLevel level) {
        return level != null ? level.name() : null;
    }

    @Named("stringToUserLevel")
    default UserLevel stringToUserLevel(String level) {
        return level != null ? UserLevel.valueOf(level) : null;
    }

    @Named("statusToString")
    default String statusToString(ReferralRewardStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default ReferralRewardStatus stringToStatus(String status) {
        return status != null ? ReferralRewardStatus.valueOf(status) : null;
    }
}
