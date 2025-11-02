package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.infrastructure.persistence.entity.TransactionJpaEntity;
import com.cashbee.infrastructure.persistence.repository.TransactionJpaRepository;
import com.cashbee.infrastructure.persistence.mapper.TransactionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements TransactionRepository port using JPA.
 *
 * This adapter lives in the infrastructure layer and connects the domain
 * layer (port) to the actual database implementation (JPA).
 *
 * Responsibilities:
 * - Convert between domain models and JPA entities
 * - Delegate database operations to Spring Data JPA repository
 * - Handle pagination and sorting
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Saving transaction: userId={}, type={}, amount={}",
                transaction.getUserId(), transaction.getType(), transaction.getAmount());

        TransactionJpaEntity entity = mapper.toEntity(transaction);
        TransactionJpaEntity savedEntity = jpaRepository.save(entity);

        log.debug("Transaction saved with ID: {}", savedEntity.getId());
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        log.debug("Finding transaction by ID: {}", id);

        return jpaRepository.findById(id)
                .map(entity -> {
                    log.debug("Transaction found: id={}", id);
                    return mapper.toDomain(entity);
                });
    }

    @Override
    public List<Transaction> findByUserId(Long userId) {
        log.debug("Finding all transactions for user: {}", userId);

        List<TransactionJpaEntity> entities = jpaRepository.findByUserId(userId);
        log.debug("Found {} transactions for user {}", entities.size(), userId);

        return mapper.toDomainList(entities);
    }

    @Override
    public List<Transaction> findByUserId(Long userId, int page, int size) {
        log.debug("Finding transactions for user {} (page={}, size={})", userId, page, size);

        // Sort by createdAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<TransactionJpaEntity> entities = jpaRepository.findByUserId(userId, pageable);

        log.debug("Found {} transactions for user {} (page {})", entities.size(), userId, page);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<Transaction> findByWalletId(Long walletId) {
        log.debug("Finding all transactions for wallet: {}", walletId);

        List<TransactionJpaEntity> entities = jpaRepository.findByWalletId(walletId);
        log.debug("Found {} transactions for wallet {}", entities.size(), walletId);

        return mapper.toDomainList(entities);
    }

    @Override
    public List<Transaction> findByUserIdAndDateRange(Long userId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.debug("Finding transactions for user {} from {} to {}", userId, fromDate, toDate);

        List<TransactionJpaEntity> entities = jpaRepository.findByUserIdAndDateRange(userId, fromDate, toDate);
        log.debug("Found {} transactions for user {} in date range", entities.size(), userId);

        return mapper.toDomainList(entities);
    }

    @Override
    public long countByUserId(Long userId) {
        log.debug("Counting transactions for user: {}", userId);

        long count = jpaRepository.countByUserId(userId);
        log.debug("User {} has {} transactions", userId, count);

        return count;
    }

    @Override
    public long count() {
        log.debug("Counting all transactions");

        long count = jpaRepository.count();
        log.debug("Total transactions in system: {}", count);

        return count;
    }

    @Override
    public void deleteById(Long id) {
        log.warn("Deleting transaction by ID: {} (should be rare for audit trail)", id);
        jpaRepository.deleteById(id);
        log.warn("Transaction deleted: id={}", id);
    }
}
