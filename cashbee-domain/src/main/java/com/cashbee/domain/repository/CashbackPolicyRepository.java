package com.cashbee.domain.repository;

import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.CashbackPolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CashbackPolicy entity.
 * This is a PORT in hexagonal architecture.
 *
 * Implementation will be provided by infrastructure layer (Adapter).
 *
 * @author CashBee Team
 */
public interface CashbackPolicyRepository {

    /**
     * Save a policy (create or update).
     *
     * @param policy policy to save
     * @return saved policy with generated ID
     */
    CashbackPolicy save(CashbackPolicy policy);

    /**
     * Find policy by ID.
     *
     * @param id policy ID
     * @return Optional containing policy if found
     */
    Optional<CashbackPolicy> findById(Long id);

    /**
     * Find policy by code.
     *
     * @param code policy code
     * @return Optional containing policy if found
     */
    Optional<CashbackPolicy> findByCode(String code);

    /**
     * Find all policies.
     *
     * @return list of all policies
     */
    List<CashbackPolicy> findAll();

    /**
     * Find all active policies.
     *
     * @return list of active policies (isActive = true)
     */
    List<CashbackPolicy> findAllActive();

    /**
     * Find active policy for specific platform and user level.
     * Returns the highest priority policy that matches criteria.
     *
     * This is the KEY method for cashback calculation!
     *
     * @param platformId platform ID (can be null for universal policies)
     * @param userLevel user level
     * @param now current date/time for checking effective dates
     * @return Optional containing best matching policy
     */
    Optional<CashbackPolicy> findActivePolicyFor(
        Long platformId,
        UserLevel userLevel,
        LocalDateTime now
    );

    /**
     * Find all policies for a specific platform.
     *
     * @param platformId platform ID
     * @return list of policies for this platform
     */
    List<CashbackPolicy> findByPlatformId(Long platformId);

    /**
     * Find all policies for a specific user level.
     *
     * @param userLevel user level
     * @return list of policies for this user level
     */
    List<CashbackPolicy> findByUserLevel(UserLevel userLevel);

    /**
     * Delete policy by ID.
     *
     * @param id policy ID
     */
    void deleteById(Long id);

    /**
     * Check if policy exists by code.
     *
     * @param code policy code
     * @return true if policy exists
     */
    boolean existsByCode(String code);
}
