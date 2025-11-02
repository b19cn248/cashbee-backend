package com.cashbee.domain.repository;

import com.cashbee.domain.model.AffiliatePlatform;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AffiliatePlatform entity.
 * This is a PORT in hexagonal architecture.
 *
 * Implementation will be provided by infrastructure layer (Adapter).
 * Domain layer defines WHAT operations are needed,
 * Infrastructure layer defines HOW to implement them.
 *
 * @author CashBee Team
 */
public interface AffiliatePlatformRepository {

    /**
     * Save a platform (create or update).
     *
     * @param platform platform to save
     * @return saved platform with generated ID
     */
    AffiliatePlatform save(AffiliatePlatform platform);

    /**
     * Find platform by ID.
     *
     * @param id platform ID
     * @return Optional containing platform if found, empty otherwise
     */
    Optional<AffiliatePlatform> findById(Long id);

    /**
     * Find platform by code (e.g., "shopee", "lazada").
     *
     * @param code platform code
     * @return Optional containing platform if found, empty otherwise
     */
    Optional<AffiliatePlatform> findByCode(String code);

    /**
     * Find all platforms.
     *
     * @return list of all platforms
     */
    List<AffiliatePlatform> findAll();

    /**
     * Find all active platforms.
     *
     * @return list of platforms with status = ACTIVE
     */
    List<AffiliatePlatform> findAllActive();

    /**
     * Delete platform by ID.
     *
     * @param id platform ID
     */
    void deleteById(Long id);

    /**
     * Check if platform exists by code.
     *
     * @param code platform code
     * @return true if platform exists
     */
    boolean existsByCode(String code);
}
