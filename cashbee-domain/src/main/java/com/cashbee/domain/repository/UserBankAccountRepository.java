package com.cashbee.domain.repository;

import com.cashbee.domain.model.UserBankAccount;

import java.util.Optional;

/**
 * Repository interface for UserBankAccount domain model.
 *
 * This is a domain-level interface (port in hexagonal architecture).
 * Implementation will be in infrastructure layer.
 *
 * @author CashBee Team
 */
public interface UserBankAccountRepository {

    /**
     * Find bank account by user ID.
     *
     * @param userId User ID
     * @return Bank account if found
     */
    Optional<UserBankAccount> findByUserId(Long userId);

    /**
     * Save bank account.
     *
     * @param bankAccount Bank account to save
     * @return Saved bank account
     */
    UserBankAccount save(UserBankAccount bankAccount);

    /**
     * Check if user has bank account.
     *
     * @param userId User ID
     * @return true if bank account exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete bank account by user ID.
     *
     * @param userId User ID
     */
    void deleteByUserId(Long userId);
}
