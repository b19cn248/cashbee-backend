package com.cashbee.domain.repository;

import com.cashbee.domain.model.Bank;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Bank entity.
 * Defines operations for bank data access.
 *
 * This is a port (Hexagonal Architecture) that will be implemented
 * by the infrastructure layer (adapter).
 *
 * @author CashBee Team
 */
public interface BankRepository {

    /**
     * Find bank by bank code.
     *
     * @param bankCode Bank code (e.g., "VPBANK", "ACB")
     * @return Optional containing Bank if found
     */
    Optional<Bank> findByBankCode(String bankCode);

    /**
     * Find all active banks.
     *
     * @return List of active banks
     */
    List<Bank> findAllActive();

    /**
     * Find all banks (including inactive).
     *
     * @return List of all banks
     */
    List<Bank> findAll();

    /**
     * Find bank by ID.
     *
     * @param id Bank ID
     * @return Optional containing Bank if found
     */
    Optional<Bank> findById(Long id);

    /**
     * Save bank (create or update).
     *
     * @param bank Bank to save
     * @return Saved bank with generated ID
     */
    Bank save(Bank bank);

    /**
     * Delete bank by ID.
     *
     * @param id Bank ID
     */
    void deleteById(Long id);

    /**
     * Check if bank exists by bank code.
     *
     * @param bankCode Bank code
     * @return true if bank exists
     */
    boolean existsByBankCode(String bankCode);

    /**
     * Count all banks.
     *
     * @return Total number of banks
     */
    long count();
}
