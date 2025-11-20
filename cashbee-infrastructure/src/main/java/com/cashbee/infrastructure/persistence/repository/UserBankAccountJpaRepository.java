package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.UserBankAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for UserBankAccountJpaEntity.
 *
 * This repository provides database access for user bank accounts.
 *
 * @author CashBee Team
 */
@Repository
public interface UserBankAccountJpaRepository extends JpaRepository<UserBankAccountJpaEntity, Long> {

    /**
     * Find bank account by user ID.
     *
     * @param userId User ID
     * @return Bank account if found
     */
    Optional<UserBankAccountJpaEntity> findByUserId(Long userId);

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
