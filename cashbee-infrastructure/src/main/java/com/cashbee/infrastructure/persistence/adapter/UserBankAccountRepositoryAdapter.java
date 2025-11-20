package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.UserBankAccount;
import com.cashbee.domain.repository.UserBankAccountRepository;
import com.cashbee.infrastructure.persistence.entity.UserBankAccountJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.UserBankAccountMapper;
import com.cashbee.infrastructure.persistence.repository.UserBankAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter implementing UserBankAccountRepository domain interface.
 *
 * This is the implementation of the repository port in hexagonal architecture.
 * It bridges the domain layer with the infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserBankAccountRepositoryAdapter implements UserBankAccountRepository {

    private final UserBankAccountJpaRepository jpaRepository;

    @Override
    public Optional<UserBankAccount> findByUserId(Long userId) {
        log.debug("Repository: Finding bank account for user {}", userId);
        return jpaRepository.findByUserId(userId)
                .map(UserBankAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public UserBankAccount save(UserBankAccount bankAccount) {
        log.debug("Repository: Saving bank account for user {}", bankAccount.getUserId());

        // Validate domain model before persisting
        bankAccount.validate();

        // Convert to JPA entity
        UserBankAccountJpaEntity entity = UserBankAccountMapper.toEntity(bankAccount);

        // Save to database
        UserBankAccountJpaEntity savedEntity = jpaRepository.save(entity);

        // Convert back to domain model
        return UserBankAccountMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        log.debug("Repository: Checking if bank account exists for user {}", userId);
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        log.debug("Repository: Deleting bank account for user {}", userId);
        jpaRepository.deleteByUserId(userId);
    }
}
