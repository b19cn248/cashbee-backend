package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.ReferrerCommissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ReferrerCommission entity.
 *
 * @author CashBee Team
 */
@Repository
public interface ReferrerCommissionJpaRepository extends JpaRepository<ReferrerCommissionJpaEntity, Long> {

    /**
     * Find all commissions for a referrer.
     */
    List<ReferrerCommissionJpaEntity> findByReferrerId(Long referrerId);

    /**
     * Find commissions by referrer and status.
     */
    List<ReferrerCommissionJpaEntity> findByReferrerIdAndStatus(Long referrerId, String status);

    /**
     * Find all commissions from a referee.
     */
    List<ReferrerCommissionJpaEntity> findByRefereeId(Long refereeId);

    /**
     * Find commission by source order.
     */
    Optional<ReferrerCommissionJpaEntity> findBySourceOrderId(Long sourceOrderId);

    /**
     * Check if commission exists for order.
     */
    boolean existsBySourceOrderId(Long sourceOrderId);

    /**
     * Find commissions by status.
     */
    List<ReferrerCommissionJpaEntity> findByStatus(String status);

    /**
     * Find expired pending commissions.
     */
    @Query("SELECT c FROM ReferrerCommissionJpaEntity c " +
            "WHERE c.status = 'PENDING' AND c.expiresAt < :now")
    List<ReferrerCommissionJpaEntity> findExpiredPendingCommissions(@Param("now") LocalDateTime now);

    /**
     * Sum total commission for referrer.
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferrerCommissionJpaEntity c " +
            "WHERE c.referrerId = :referrerId")
    BigDecimal sumCommissionByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Sum pending commission for referrer.
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferrerCommissionJpaEntity c " +
            "WHERE c.referrerId = :referrerId AND c.status = 'PENDING'")
    BigDecimal sumPendingCommissionByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Sum confirmed commission for referrer.
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferrerCommissionJpaEntity c " +
            "WHERE c.referrerId = :referrerId AND c.status = 'CONFIRMED'")
    BigDecimal sumConfirmedCommissionByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Sum paid commission for referrer.
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferrerCommissionJpaEntity c " +
            "WHERE c.referrerId = :referrerId AND c.status = 'PAID'")
    BigDecimal sumPaidCommissionByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Count commissions by referrer.
     */
    long countByReferrerId(Long referrerId);

    /**
     * Count commissions by referee.
     */
    long countByRefereeId(Long refereeId);

    /**
     * Find commissions between referrer and referee.
     */
    List<ReferrerCommissionJpaEntity> findByReferrerIdAndRefereeId(Long referrerId, Long refereeId);

    /**
     * Find all unpaid commissions for a referrer.
     * Unpaid = status IN ('CONFIRMED', 'PAID') AND paid_batch_id IS NULL
     */
    @Query("SELECT c FROM ReferrerCommissionJpaEntity c " +
           "WHERE c.referrerId = :referrerId " +
           "AND c.status IN ('CONFIRMED', 'PAID') " +
           "AND c.paidBatchId IS NULL")
    List<ReferrerCommissionJpaEntity> findUnpaidByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Sum unpaid commission amount for a referrer.
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferrerCommissionJpaEntity c " +
           "WHERE c.referrerId = :referrerId " +
           "AND c.status IN ('CONFIRMED', 'PAID') " +
           "AND c.paidBatchId IS NULL")
    BigDecimal sumUnpaidCommissionByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Mark commissions as paid by batch.
     */
    @Modifying
    @Query("UPDATE ReferrerCommissionJpaEntity c " +
           "SET c.status = 'PAID', c.paidBatchId = :batchId, c.paidAt = CURRENT_TIMESTAMP " +
           "WHERE c.id IN :commissionIds")
    int markAsPaidByBatch(@Param("commissionIds") List<Long> commissionIds, @Param("batchId") Long batchId);

    /**
     * Find commissions paid by a batch for a specific referrer.
     */
    List<ReferrerCommissionJpaEntity> findByPaidBatchIdAndReferrerId(Long paidBatchId, Long referrerId);
}
