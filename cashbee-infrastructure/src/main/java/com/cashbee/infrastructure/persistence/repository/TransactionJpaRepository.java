package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.TransactionSourceType;
import com.cashbee.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA Repository for Transaction.
 *
 * Provides database access methods using Spring Data JPA.
 *
 * @author CashBee Team
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    /**
     * Find all transactions for a user.
     *
     * @param userId User ID
     * @return List of transactions
     */
    List<TransactionJpaEntity> findByUserId(Long userId);

    /**
     * Find transactions for a user with pagination.
     *
     * @param userId User ID
     * @param pageable Pagination info
     * @return List of transactions (paginated)
     */
    List<TransactionJpaEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * Find transactions for a wallet.
     *
     * @param walletId Wallet ID
     * @return List of transactions
     */
    List<TransactionJpaEntity> findByWalletId(Long walletId);

    /**
     * Find transactions for a user within date range.
     *
     * @param userId User ID
     * @param fromDate Start date
     * @param toDate End date
     * @return List of transactions
     */
    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.userId = :userId " +
            "AND t.createdAt >= :fromDate AND t.createdAt <= :toDate " +
            "ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Count total transactions for a user.
     *
     * @param userId User ID
     * @return Total count
     */
    long countByUserId(Long userId);

    /**
     * Check if transaction exists by source ID and source type.
     * Used for anti-duplicate protection (Layer 3).
     *
     * @param sourceId   Source entity ID (e.g., referrer_commission.id)
     * @param sourceType Source type enum (e.g., REFERRER_COMMISSION)
     * @return true if transaction already exists
     */
    boolean existsBySourceIdAndSourceType(Long sourceId, TransactionSourceType sourceType);
}
