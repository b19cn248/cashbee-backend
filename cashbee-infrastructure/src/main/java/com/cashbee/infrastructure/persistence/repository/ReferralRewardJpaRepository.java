package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.ReferralRewardJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Find all unpaid rewards for a user.
     * Unpaid = status='GRANTED' AND paid_batch_id IS NULL
     */
    @Query("SELECT r FROM ReferralRewardJpaEntity r " +
           "WHERE r.userId = :userId " +
           "AND r.status = 'GRANTED' " +
           "AND r.paidBatchId IS NULL")
    List<ReferralRewardJpaEntity> findUnpaidByUserId(@Param("userId") Long userId);

    /**
     * Sum unpaid bonus amount for a user.
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ReferralRewardJpaEntity r " +
           "WHERE r.userId = :userId " +
           "AND r.status = 'GRANTED' " +
           "AND r.paidBatchId IS NULL")
    BigDecimal sumUnpaidAmountByUserId(@Param("userId") Long userId);

    /**
     * Mark rewards as paid by batch.
     */
    @Modifying
    @Query("UPDATE ReferralRewardJpaEntity r " +
           "SET r.status = 'PAID', r.paidBatchId = :batchId, r.paidAt = CURRENT_TIMESTAMP " +
           "WHERE r.id IN :rewardIds")
    int markAsPaidByBatch(@Param("rewardIds") List<Long> rewardIds, @Param("batchId") Long batchId);

    /**
     * Find all rewards paid by a specific batch.
     */
    List<ReferralRewardJpaEntity> findByPaidBatchId(Long paidBatchId);

    /**
     * Find rewards paid by a batch for a specific user.
     */
    List<ReferralRewardJpaEntity> findByPaidBatchIdAndUserId(Long paidBatchId, Long userId);
}
