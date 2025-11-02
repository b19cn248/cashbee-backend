package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.PlatformStatus;
import com.cashbee.infrastructure.persistence.entity.AffiliatePlatformJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AffiliatePlatform.
 *
 * Provides database access methods for affiliate_platform table.
 * Spring Data automatically implements basic CRUD operations.
 *
 * Custom query methods are defined using Spring Data method naming convention.
 *
 * @author CashBee Team
 */
@Repository
public interface AffiliatePlatformJpaRepository extends JpaRepository<AffiliatePlatformJpaEntity, Long> {

    /**
     * Find platform by code.
     *
     * @param code platform code (e.g., "shopee", "lazada")
     * @return Optional containing platform if found
     */
    Optional<AffiliatePlatformJpaEntity> findByCode(String code);

    /**
     * Find all platforms with specific status.
     *
     * @param status platform status
     * @return list of platforms with the given status
     */
    List<AffiliatePlatformJpaEntity> findByStatus(PlatformStatus status);

    /**
     * Check if platform exists by code.
     *
     * @param code platform code
     * @return true if platform exists
     */
    boolean existsByCode(String code);
}
