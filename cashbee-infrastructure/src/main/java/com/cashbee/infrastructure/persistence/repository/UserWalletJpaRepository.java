package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.UserWalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
