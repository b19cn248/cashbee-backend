package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.BankJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for BankJpaEntity.
 * Provides CRUD operations and custom queries for bank data.
 *
 * @author CashBee Team
 */
@Repository
public interface BankJpaRepository extends JpaRepository<BankJpaEntity, Long> {

    /**
     * Find bank by bank code.
     *
     * @param bankCode Bank code
     * @return Optional containing BankJpaEntity if found
     */
    Optional<BankJpaEntity> findByBankCode(String bankCode);

    /**
     * Find all active banks.
     *
     * @param isActive Active status
     * @return List of active banks
     */
    List<BankJpaEntity> findAllByIsActive(Boolean isActive);

    /**
     * Check if bank exists by bank code.
     *
     * @param bankCode Bank code
     * @return true if exists
     */
    boolean existsByBankCode(String bankCode);
}
