package com.cashbee.domain.repository;

import com.cashbee.domain.model.UserWallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * UserWallet Repository Interface (Port).
 *
 * This is a domain interface that defines the contract for wallet persistence.
 * The infrastructure layer will provide the actual implementation.
 *
 * @author CashBee Team
 */
public interface UserWalletRepository {

    /**
     * Save a wallet (insert or update).
     *
     * @param wallet Wallet to save
     * @return Saved wallet with generated ID if new
     */
    UserWallet save(UserWallet wallet);

    /**
     * Find wallet by internal ID.
     *
     * @param id Wallet ID
     * @return Optional containing wallet if found
     */
    Optional<UserWallet> findById(Long id);

    /**
     * Find wallet by user ID.
     * Each user has exactly one wallet.
     *
     * @param userId User ID
     * @return Optional containing wallet if found
     */
    Optional<UserWallet> findByUserId(Long userId);

    /**
     * Check if wallet exists for user.
     *
     * @param userId User ID
     * @return true if wallet exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Find all wallets.
     *
     * @return List of all wallets
     */
    List<UserWallet> findAll();

    /**
     * Find wallets with balance greater than specified amount.
     * Useful for reports and analytics.
     *
     * @param minBalance Minimum balance
     * @return List of wallets
     */
    List<UserWallet> findByBalanceGreaterThan(BigDecimal minBalance);

    /**
     * Find wallets with pending balance.
     * Useful for identifying users with unconfirmed cashback.
     *
     * @return List of wallets with pending balance > 0
     */
    List<UserWallet> findWithPendingBalance();

    /**
     * Find wallets with locked balance.
     * Useful for identifying users with pending withdrawals.
     *
     * @return List of wallets with locked balance > 0
     */
    List<UserWallet> findWithLockedBalance();

    /**
     * Calculate total balance across all wallets.
     * Useful for financial reporting.
     *
     * @return Sum of all wallet balances
     */
    BigDecimal calculateTotalBalance();

    /**
     * Calculate total pending balance across all wallets.
     *
     * @return Sum of all pending balances
     */
    BigDecimal calculateTotalPendingBalance();

    /**
     * Calculate total locked balance across all wallets.
     *
     * @return Sum of all locked balances
     */
    BigDecimal calculateTotalLockedBalance();

    /**
     * Calculate total earned cashback across all users.
     *
     * @return Sum of all totalEarned
     */
    BigDecimal calculateTotalEarned();

    /**
     * Calculate total withdrawn amount across all users.
     *
     * @return Sum of all totalWithdrawn
     */
    BigDecimal calculateTotalWithdrawn();

    /**
     * Count total wallets.
     *
     * @return Number of wallets
     */
    long count();

    /**
     * Get total balance across all wallets (alias for calculateTotalBalance).
     * Useful for admin dashboard statistics.
     *
     * @return Sum of all wallet balances
     */
    default BigDecimal getTotalBalance() {
        return calculateTotalBalance();
    }

    /**
     * Get total pending balance across all wallets (alias for calculateTotalPendingBalance).
     *
     * @return Sum of all pending balances
     */
    default BigDecimal getTotalPendingBalance() {
        return calculateTotalPendingBalance();
    }

    /**
     * Get total locked balance across all wallets (alias for calculateTotalLockedBalance).
     *
     * @return Sum of all locked balances
     */
    default BigDecimal getTotalLockedBalance() {
        return calculateTotalLockedBalance();
    }

    /**
     * Get total earned cashback across all users (alias for calculateTotalEarned).
     *
     * @return Sum of all totalEarned
     */
    default BigDecimal getTotalEarned() {
        return calculateTotalEarned();
    }

    /**
     * Get total withdrawn amount across all users (alias for calculateTotalWithdrawn).
     *
     * @return Sum of all totalWithdrawn
     */
    default BigDecimal getTotalWithdrawn() {
        return calculateTotalWithdrawn();
    }

    /**
     * Delete wallet by user ID.
     * NOTE: Should rarely be used due to financial data integrity.
     *
     * @param userId User ID
     */
    void deleteByUserId(Long userId);
}
