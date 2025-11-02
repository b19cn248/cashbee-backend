package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.UserLevel;
import com.cashbee.infrastructure.persistence.entity.CashbackPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for CashbackPolicy.
 *
 * @author CashBee Team
 */
@Repository
public interface CashbackPolicyJpaRepository extends JpaRepository<CashbackPolicyJpaEntity, Long> {

    /**
     * Find policy by code.
     */
    Optional<CashbackPolicyJpaEntity> findByPolicyCode(String policyCode);

    /**
     * Find all active policies.
     */
    List<CashbackPolicyJpaEntity> findByIsActiveTrue();

    /**
     * Find all policies for a platform.
     */
    List<CashbackPolicyJpaEntity> findByPlatformId(Long platformId);

    /**
     * Find all policies for a user level.
     */
    List<CashbackPolicyJpaEntity> findByUserLevel(UserLevel userLevel);

    /**
     * Check if policy exists by code.
     */
    boolean existsByPolicyCode(String policyCode);

    /**
     * Find active policy for platform and user level.
     * This is the KEY query for cashback calculation!
     *
     * Logic:
     * - Policy must be active (isActive = true)
     * - Policy must be effective now (effectiveFrom <= now AND (effectiveTo IS NULL OR effectiveTo >= now))
     * - Policy must match user level
     * - Policy must match platform (platformId = ? OR platformId IS NULL)
     * - Order by priority DESC (highest priority first)
     * - Return first result
     */
    @Query("""
        SELECT p FROM CashbackPolicyJpaEntity p
        WHERE p.isActive = true
          AND p.effectiveFrom <= :now
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :now)
          AND p.userLevel = :userLevel
          AND (p.platformId = :platformId OR p.platformId IS NULL)
        ORDER BY p.priority DESC, p.createdAt DESC
        """)
    Optional<CashbackPolicyJpaEntity> findActivePolicyFor(
        @Param("platformId") Long platformId,
        @Param("userLevel") UserLevel userLevel,
        @Param("now") LocalDateTime now
    );
}
