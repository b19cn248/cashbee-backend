package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.ReferrerTierConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ReferrerTierConfig entity.
 *
 * @author CashBee Team
 */
@Repository
public interface ReferrerTierConfigJpaRepository extends JpaRepository<ReferrerTierConfigJpaEntity, Long> {

    /**
     * Find by tier name.
     */
    Optional<ReferrerTierConfigJpaEntity> findByTierName(String tierName);

    /**
     * Find all active tier configs ordered by min_referrals ascending.
     */
    @Query("SELECT t FROM ReferrerTierConfigJpaEntity t WHERE t.isActive = true ORDER BY t.minReferrals ASC")
    List<ReferrerTierConfigJpaEntity> findAllActiveOrderByMinReferrals();

    /**
     * Find the highest qualifying tier for given referral count.
     * Returns the tier with highest min_referrals that is <= activatedReferrals.
     */
    @Query("SELECT t FROM ReferrerTierConfigJpaEntity t " +
           "WHERE t.isActive = true AND t.minReferrals <= :activatedReferrals " +
           "ORDER BY t.minReferrals DESC LIMIT 1")
    Optional<ReferrerTierConfigJpaEntity> findTierByReferralCount(@Param("activatedReferrals") int activatedReferrals);
}
