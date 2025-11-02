package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.UserWalletRepository;
import com.cashbee.infrastructure.persistence.entity.UserWalletJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.UserWalletMapper;
import com.cashbee.infrastructure.persistence.repository.UserWalletJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing UserWalletRepository domain interface using JPA.
 *
 * This is the "Adapter" in Hexagonal Architecture (Ports & Adapters pattern).
 * It implements the domain's port (UserWalletRepository interface) and adapts it
 * to the infrastructure technology (JPA/Spring Data).
 *
 * Key responsibilities:
 * - Maps between domain models and JPA entities
 * - Delegates persistence operations to Spring Data JPA repository
 * - Handles transactions
 * - Ensures domain invariants are maintained before persistence
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserWalletRepositoryAdapter implements UserWalletRepository {

    private final UserWalletJpaRepository jpaRepository;
    private final UserWalletMapper mapper;

    @Override
    @Transactional
    public UserWallet save(UserWallet wallet) {
        log.debug("Saving wallet: userId={}, balance={}", wallet.getUserId(), wallet.getBalance());

        // Validate domain invariants before saving
        wallet.validate();

        // Ensure proper BigDecimal scale
        wallet.scaleBalances();

        UserWalletJpaEntity entity;

        if (wallet.isNew()) {
            // New wallet - create entity
            entity = mapper.toEntity(wallet);
        } else {
            // Existing wallet - update entity
            entity = jpaRepository.findById(wallet.getId())
                .orElseGet(() -> mapper.toEntity(wallet));

            // Update fields from domain model
            mapper.updateEntityFromDomain(wallet, entity);
        }

        UserWalletJpaEntity saved = jpaRepository.save(entity);
        UserWallet result = mapper.toDomain(saved);

        log.debug("Wallet saved: id={}, userId={}, balance={}",
            result.getId(), result.getUserId(), result.getBalance());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserWallet> findById(Long id) {
        log.debug("Finding wallet by id: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserWallet> findByUserId(Long userId) {
        log.debug("Finding wallet by userId: {}", userId);
        return jpaRepository.findByUserId(userId)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        log.debug("Checking if wallet exists for userId: {}", userId);
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWallet> findAll() {
        log.debug("Finding all wallets");
        List<UserWalletJpaEntity> entities = jpaRepository.findAll();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWallet> findByBalanceGreaterThan(BigDecimal minBalance) {
        log.debug("Finding wallets with balance > {}", minBalance);
        List<UserWalletJpaEntity> entities = jpaRepository.findByBalanceGreaterThan(minBalance);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWallet> findWithPendingBalance() {
        log.debug("Finding wallets with pending balance");
        List<UserWalletJpaEntity> entities = jpaRepository.findWithPendingBalance();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWallet> findWithLockedBalance() {
        log.debug("Finding wallets with locked balance");
        List<UserWalletJpaEntity> entities = jpaRepository.findWithLockedBalance();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBalance() {
        log.debug("Calculating total balance across all wallets");
        BigDecimal total = jpaRepository.calculateTotalBalance();
        log.debug("Total balance: {}", total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPendingBalance() {
        log.debug("Calculating total pending balance across all wallets");
        BigDecimal total = jpaRepository.calculateTotalPendingBalance();
        log.debug("Total pending balance: {}", total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalLockedBalance() {
        log.debug("Calculating total locked balance across all wallets");
        BigDecimal total = jpaRepository.calculateTotalLockedBalance();
        log.debug("Total locked balance: {}", total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalEarned() {
        log.debug("Calculating total earned across all users");
        BigDecimal total = jpaRepository.calculateTotalEarned();
        log.debug("Total earned: {}", total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalWithdrawn() {
        log.debug("Calculating total withdrawn across all users");
        BigDecimal total = jpaRepository.calculateTotalWithdrawn();
        log.debug("Total withdrawn: {}", total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        log.debug("Counting all wallets");
        return jpaRepository.count();
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        log.warn("Deleting wallet for userId: {} - This should rarely be used!", userId);
        jpaRepository.deleteByUserId(userId);
        log.debug("Wallet deleted for userId: {}", userId);
    }
}
