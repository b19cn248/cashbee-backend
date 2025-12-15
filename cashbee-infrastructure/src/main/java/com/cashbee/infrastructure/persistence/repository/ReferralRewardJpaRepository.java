package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.ReferralRewardJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ReferralReward entity.
 *
 * @author CashBee Team
 */
@Repository
public interface ReferralRewardJpaRepository extends JpaRepository<ReferralRewardJpaEntity, Long> {

    /**
     * Find all rewards for a user.
     */
    List<ReferralRewardJpaEntity> findByUserId(Long userId);

    /**
     * Find rewards by user and type.
     */
    List<ReferralRewardJpaEntity> findByUserIdAndRewardType(Long userId, String rewardType);

    /**
     * Find a specific reward by user and milestone.
     */
    Optional<ReferralRewardJpaEntity> findByUserIdAndMilestone(Long userId, Integer milestone);

    /**
     * Check if reward exists for user and milestone.
     */
    boolean existsByUserIdAndMilestone(Long userId, Integer milestone);

    /**
     * Find rewards by status.
     */
    List<ReferralRewardJpaEntity> findByStatus(String status);

    /**
     * Find rewards by referrer.
     */
    List<ReferralRewardJpaEntity> findByReferrerId(Long referrerId);

    /**
     * Count rewards by user.
     */
    long countByUserId(Long userId);

    /**
     * Count rewards by referrer.
     */
    long countByReferrerId(Long referrerId);
}
