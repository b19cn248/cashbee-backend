package com.cashbee.domain.repository;

import com.cashbee.domain.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository Port (Hexagonal Architecture).
 *
 * This is a port (interface) in the domain layer that defines
 * what operations we need for Transaction persistence.
 *
 * The actual implementation (adapter) will be in the infrastructure layer.
 *
 * @author CashBee Team
 */
public interface TransactionRepository {

    /**
     * Save a transaction.
     *
     * @param transaction Transaction to save
     * @return Saved transaction with generated ID
     */
    Transaction save(Transaction transaction);

    /**
     * Find transaction by ID.
     *
     * @param id Transaction ID
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findById(Long id);

    /**
     * Find all transactions for a user.
     *
     * @param userId User ID
     * @return List of transactions
     */
    List<Transaction> findByUserId(Long userId);

    /**
     * Find transactions for a user with pagination.
     *
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of transactions (paginated)
     */
    List<Transaction> findByUserId(Long userId, int page, int size);

    /**
     * Find transactions for a wallet.
     *
     * @param walletId Wallet ID
     * @return List of transactions
     */
    List<Transaction> findByWalletId(Long walletId);

    /**
     * Find transactions for a user within date range.
     *
     * @param userId User ID
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of transactions
     */
    List<Transaction> findByUserIdAndDateRange(Long userId, LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Count total transactions for a user.
     *
     * @param userId User ID
     * @return Total count
     */
    long countByUserId(Long userId);

    /**
     * Count all transactions in the system.
     *
     * @return Total count of all transactions
     */
    long count();

    /**
     * Save multiple transactions in bulk.
     * Used for batch operations to reduce DB round-trips.
     *
     * @param transactions List of transactions to save
     * @return List of saved transactions
     */
    List<Transaction> saveAll(List<Transaction> transactions);

    /**
     * Delete transaction by ID.
     * Note: Normally transactions should NOT be deleted (audit trail).
     * This is here for admin corrections only.
     *
     * @param id Transaction ID
     */
    void deleteById(Long id);
}
