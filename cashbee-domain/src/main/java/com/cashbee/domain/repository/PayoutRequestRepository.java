package com.cashbee.domain.repository;

import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.model.PayoutRequest;

import java.util.List;
import java.util.Optional;

/**
 * PayoutRequest Repository Port (Hexagonal Architecture).
 *
 * This is a port (interface) in the domain layer that defines
 * what operations we need for PayoutRequest persistence.
 *
 * The actual implementation (adapter) will be in the infrastructure layer.
 *
 * @author CashBee Team
 */
public interface PayoutRequestRepository {

    /**
     * Save a payout request.
     *
     * @param payoutRequest Payout request to save
     * @return Saved payout request with generated ID
     */
    PayoutRequest save(PayoutRequest payoutRequest);

    /**
     * Find payout request by ID.
     *
     * @param id Payout request ID
     * @return Optional containing payout request if found
     */
    Optional<PayoutRequest> findById(Long id);

    /**
     * Find all payout requests for a user.
     *
     * @param userId User ID
     * @return List of payout requests
     */
    List<PayoutRequest> findByUserId(Long userId);

    /**
     * Find payout requests for a user with pagination.
     *
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of payout requests (paginated, sorted by requestedAt DESC)
     */
    List<PayoutRequest> findByUserId(Long userId, int page, int size);

    /**
     * Find payout requests by status.
     *
     * @param status Payout status
     * @return List of payout requests
     */
    List<PayoutRequest> findByStatus(PayoutStatus status);

    /**
     * Find payout requests by status with pagination.
     *
     * @param status Payout status
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of payout requests (paginated, sorted by requestedAt DESC)
     */
    List<PayoutRequest> findByStatus(PayoutStatus status, int page, int size);

    /**
     * Find all payout requests with pagination (for admin).
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of payout requests (paginated, sorted by requestedAt DESC)
     */
    List<PayoutRequest> findAll(int page, int size);

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
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of payout requests (paginated, sorted by requestedAt DESC)
     */
    List<PayoutRequest> findByUserIdAndStatus(Long userId, PayoutStatus status, int page, int size);

    /**
     * Count payout requests by userId and status.
     *
     * @param userId User ID
     * @param status Payout status
     * @return Total count
     */
    long countByUserIdAndStatus(Long userId, PayoutStatus status);

    /**
     * Count all payout requests.
     *
     * @return Total count
     */
    long countAll();

    /**
     * Delete payout request by ID.
     * Note: Normally payouts should NOT be deleted (audit trail).
     * This is here for admin corrections only.
     *
     * @param id Payout request ID
     */
    void deleteById(Long id);
}
