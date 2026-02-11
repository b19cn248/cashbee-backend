package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.UserWalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for UserWalletJpaEntity.
 * Provides database operations for user_wallet table.
 *
 * This is infrastructure-specific and should not be used directly
 * by application or domain layers. Use UserWalletRepository (domain interface) instead.
 *
 * @author CashBee Team
 */
@Repository
public interface UserWalletJpaRepository extends JpaRepository<UserWalletJpaEntity, Long> {

    /**
     * Find wallet by user ID.
     *
     * @param userId User ID
     * @return Optional containing wallet entity if found
     */
    Optional<UserWalletJpaEntity> findByUserId(Long userId);

    /**
     * Check if wallet exists for user.
     *
     * @param userId User ID
     * @return true if wallet exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Find wallets with balance greater than specified amount.
     *
     * @param minBalance Minimum balance
     * @return List of wallet entities
     */
    @Query("SELECT w FROM UserWalletJpaEntity w WHERE w.balance > :minBalance")
    List<UserWalletJpaEntity> findByBalanceGreaterThan(@Param("minBalance") BigDecimal minBalance);

    /**
     * Find wallets with pending balance > 0.
     *
     * @return List of wallet entities
     */
    @Query("SELECT w FROM UserWalletJpaEntity w WHERE w.pendingBalance > 0")
    List<UserWalletJpaEntity> findWithPendingBalance();

    /**
     * Find wallets with locked balance > 0.
     *
     * @return List of wallet entities
     */
    @Query("SELECT w FROM UserWalletJpaEntity w WHERE w.lockedBalance > 0")
    List<UserWalletJpaEntity> findWithLockedBalance();

    /**
     * Calculate total balance across all wallets.
     *
     * @return Sum of all balances
     */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM UserWalletJpaEntity w")
    BigDecimal calculateTotalBalance();

    /**
     * Calculate total pending balance across all wallets.
     *
     * @return Sum of all pending balances
     */
    @Query("SELECT COALESCE(SUM(w.pendingBalance), 0) FROM UserWalletJpaEntity w")
    BigDecimal calculateTotalPendingBalance();

    /**
     * Calculate total locked balance across all wallets.
     *
     * @return Sum of all locked balances
     */
    @Query("SELECT COALESCE(SUM(w.lockedBalance), 0) FROM UserWalletJpaEntity w")
    BigDecimal calculateTotalLockedBalance();

    /**
     * Calculate total earned cashback across all users.
     *
     * @return Sum of all totalEarned
     */
    @Query("SELECT COALESCE(SUM(w.totalEarned), 0) FROM UserWalletJpaEntity w")
    BigDecimal calculateTotalEarned();

    /**
     * Calculate total withdrawn amount across all users.
     *
     * @return Sum of all totalWithdrawn
     */
    @Query("SELECT COALESCE(SUM(w.totalWithdrawn), 0) FROM UserWalletJpaEntity w")
    BigDecimal calculateTotalWithdrawn();

    /**
     * Delete wallet by user ID.
     *
     * @param userId User ID
     */
    void deleteByUserId(Long userId);

    /**
     * CRITICAL: Update wallet balances directly in DB using JPQL.
     * This bypasses JPA persistence context to avoid stale data issues.
     *
     * Used when confirming pending balance: pending_balance → balance.
     * - Subtracts amount from pending_balance
     * - Adds amount to balance
     * - Adds amount to total_earned
     *
     * @param userId User ID
     * @param amount Amount to transfer from pending to balance
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query("UPDATE UserWalletJpaEntity w SET " +
           "w.pendingBalance = w.pendingBalance - :amount, " +
           "w.balance = w.balance + :amount, " +
           "w.totalEarned = w.totalEarned + :amount, " +
           "w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.userId = :userId AND w.pendingBalance >= :amount")
    int confirmPendingBalanceDirectly(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * Add amount to pending balance directly in DB using JPQL.
     * Used when creating new PENDING cashback.
     *
     * @param userId User ID
     * @param amount Amount to add to pending balance
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query("UPDATE UserWalletJpaEntity w SET " +
           "w.pendingBalance = w.pendingBalance + :amount, " +
           "w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.userId = :userId")
    int addPendingBalanceDirectly(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * Add amount directly to balance and total_earned in DB using JPQL.
     * Used when creating new CONFIRMED cashback.
     *
     * @param userId User ID
     * @param amount Amount to add
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query("UPDATE UserWalletJpaEntity w SET " +
           "w.balance = w.balance + :amount, " +
           "w.totalEarned = w.totalEarned + :amount, " +
           "w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.userId = :userId")
    int addConfirmedBalanceDirectly(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * Subtract amount from pending_balance directly in DB using JPQL.
     * Used when PENDING cashback is CANCELLED (order cancelled before completion).
     *
     * @param userId User ID
     * @param amount Amount to subtract from pending balance
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query("UPDATE UserWalletJpaEntity w SET " +
           "w.pendingBalance = w.pendingBalance - :amount, " +
           "w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.userId = :userId AND w.pendingBalance >= :amount")
    int subtractPendingBalanceDirectly(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * Reverse confirmed balance directly in DB using JPQL.
     * Used when CONFIRMED/PAID cashback is CANCELLED (order cancelled after completion/refund).
     * - Subtracts amount from balance
     * - Subtracts amount from total_earned
     *
     * @param userId User ID
     * @param amount Amount to reverse
     * @return Number of rows updated (should be 1)
     */
    @Modifying
    @Query("UPDATE UserWalletJpaEntity w SET " +
           "w.balance = w.balance - :amount, " +
           "w.totalEarned = w.totalEarned - :amount, " +
           "w.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE w.userId = :userId AND w.balance >= :amount")
    int reverseConfirmedBalanceDirectly(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
