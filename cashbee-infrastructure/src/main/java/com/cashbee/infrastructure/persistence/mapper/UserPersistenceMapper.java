package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between User domain model and UserJpaEntity.
 *
 * Uses MapStruct for automatic mapping generation.
 * Handles conversion between domain enums and database strings.
 *
 * This is the adapter between domain and infrastructure layers.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserPersistenceMapper {

    /**
     * Convert User domain model to JPA entity.
     *
     * @param user Domain model
     * @return JPA entity
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    UserJpaEntity toEntity(User user);

    /**
     * Convert JPA entity to User domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    User toDomain(UserJpaEntity entity);

    /**
     * Convert list of JPA entities to list of domain models.
     *
     * @param entities List of JPA entities
     * @return List of domain models
     */
    List<User> toDomainList(List<UserJpaEntity> entities);

    /**
     * Convert list of domain models to list of JPA entities.
     *
     * @param users List of domain models
     * @return List of JPA entities
     */
    List<UserJpaEntity> toEntityList(List<User> users);

    /**
     * Update existing JPA entity from domain model.
     * Used for updating entities while preserving JPA state.
     *
     * @param user Domain model (source)
     * @param entity JPA entity (target)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    void updateEntityFromDomain(User user, @MappingTarget UserJpaEntity entity);

    /**
     * Convert UserStatus enum to String for database storage.
     *
     * @param status UserStatus enum
     * @return String representation
     */
    @Named("statusToString")
    default String statusToString(UserStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * Convert String from database to UserStatus enum.
     *
     * @param status String from database
     * @return UserStatus enum
     */
    @Named("stringToStatus")
    default UserStatus stringToStatus(String status) {
        return status != null ? UserStatus.valueOf(status) : null;
    }
}
