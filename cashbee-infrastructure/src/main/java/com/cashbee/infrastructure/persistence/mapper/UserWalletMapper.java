package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.UserWallet;
import com.cashbee.infrastructure.persistence.entity.UserWalletJpaEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between UserWallet domain model and UserWalletJpaEntity.
 *
 * Uses MapStruct for automatic mapping generation.
 * All fields map directly (no complex conversions needed).
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
public interface UserWalletMapper {

    /**
     * Convert UserWallet domain model to JPA entity.
     *
     * @param wallet Domain model
     * @return JPA entity
     */
    UserWalletJpaEntity toEntity(UserWallet wallet);

    /**
     * Convert JPA entity to UserWallet domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    UserWallet toDomain(UserWalletJpaEntity entity);

    /**
     * Convert list of JPA entities to list of domain models.
     *
     * @param entities List of JPA entities
     * @return List of domain models
     */
    List<UserWallet> toDomainList(List<UserWalletJpaEntity> entities);

    /**
     * Convert list of domain models to list of JPA entities.
     *
     * @param wallets List of domain models
     * @return List of JPA entities
     */
    List<UserWalletJpaEntity> toEntityList(List<UserWallet> wallets);

    /**
     * Update existing JPA entity from domain model.
     * Used for updating entities while preserving JPA state.
     *
     * Ignores id and createdAt to preserve database-managed fields.
     *
     * @param wallet Domain model (source)
     * @param entity JPA entity (target)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDomain(UserWallet wallet, @MappingTarget UserWalletJpaEntity entity);
}
