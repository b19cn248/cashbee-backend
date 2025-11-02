package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.infrastructure.persistence.entity.PayoutRequestJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for PayoutRequest.
 *
 * Provides database access methods using Spring Data JPA.
 *
 * @author CashBee Team
 */
@Repository
public interface PayoutRequestJpaRepository extends JpaRepository<PayoutRequestJpaEntity, Long> {

    /**
     * Find all payout requests for a user.
     *
     * @param userId User ID
     * @return List of payout requests
     */
    List<PayoutRequestJpaEntity> findByUserId(Long userId);

    /**
     * Find payout requests for a user with pagination.
     *
     * @param userId User ID
     * @param pageable Pagination info
     * @return List of payout requests (paginated)
     */
    List<PayoutRequestJpaEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * Find payout requests by status.
     *
     * @param status Payout status
     * @return List of payout requests
     */
    List<PayoutRequestJpaEntity> findByStatus(PayoutStatus status);

    /**
     * Find payout requests by status with pagination.
     *
     * @param status Payout status
     * @param pageable Pagination info
     * @return List of payout requests (paginated)
     */
    List<PayoutRequestJpaEntity> findByStatus(PayoutStatus status, Pageable pageable);

    /**
     * Count payout requests by user.
     *
     * @param userId User ID
     * @return Total count
     */
    long countByUserId(Long userId);

    /**
     * Count payout requests by status.
     *
     * @param status Payout status
     * @return Total count
     */
    long countByStatus(PayoutStatus status);

    /**
     * Find payout requests by userId and status with pagination.
     *
     * @param userId User ID
     * @param status Payout status
     * @param pageable Pagination info
     * @return List of payout requests (paginated)
     */
    List<PayoutRequestJpaEntity> findByUserIdAndStatus(Long userId, PayoutStatus status, Pageable pageable);

    /**
     * Count payout requests by userId and status.
     *
     * @param userId User ID
     * @param status Payout status
     * @return Total count
     */
    long countByUserIdAndStatus(Long userId, PayoutStatus status);
}
