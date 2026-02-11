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

    /**
     * Check if the given bank account (bankCode + accountNumber) is already used by another user.
     * Used for duplicate bank account detection to prevent fraud.
     *
     * @param bankCode      Bank code (e.g., "VPBANK", "VCB")
     * @param accountNumber Account number
     * @param excludeUserId User ID to exclude from the check (the current user)
     * @return true if another user already has this bank account
     */
    boolean existsByBankCodeAndAccountNumberAndUserIdNot(String bankCode, String accountNumber, Long excludeUserId);
}
