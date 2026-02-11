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
        log.debug("Saving wallet: userId={}, balance={}, pendingBalance={}",
            wallet.getUserId(), wallet.getBalance(), wallet.getPendingBalance());

        // Validate domain invariants before saving
        wallet.validate();

        // Ensure proper BigDecimal scale
        wallet.scaleBalances();

        // CRITICAL FIX: Always create entity directly from domain model
        // DO NOT reload entity from DB/persistence context as it may have stale data
        // The entity ID will be preserved during mapping, allowing JPA to perform UPDATE
        UserWalletJpaEntity entity = mapper.toEntity(wallet);

        log.debug("Entity before save: id={}, userId={}, balance={}, pendingBalance={}",
            entity.getId(), entity.getUserId(), entity.getBalance(), entity.getPendingBalance());

        UserWalletJpaEntity saved = jpaRepository.save(entity);
        UserWallet result = mapper.toDomain(saved);

        log.debug("Wallet saved: id={}, userId={}, balance={}, pendingBalance={}",
            result.getId(), result.getUserId(), result.getBalance(), result.getPendingBalance());
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
    @Transactional(readOnly = true)
    public List<UserWallet> findAllById(List<Long> ids) {
        log.debug("Finding wallets by IDs: count={}", ids.size());
        List<UserWalletJpaEntity> entities = jpaRepository.findAllById(ids);
        log.debug("Found {} wallets", entities.size());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional
    public List<UserWallet> saveAll(List<UserWallet> wallets) {
        log.debug("Bulk saving {} wallets", wallets.size());

        // Validate and scale all wallets
        wallets.forEach(wallet -> {
            wallet.validate();
            wallet.scaleBalances();
        });

        List<UserWalletJpaEntity> entities = mapper.toEntityList(wallets);
        List<UserWalletJpaEntity> savedEntities = jpaRepository.saveAll(entities);

        log.debug("Bulk saved {} wallets", savedEntities.size());
        return mapper.toDomainList(savedEntities);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        log.warn("Deleting wallet for userId: {} - This should rarely be used!", userId);
        jpaRepository.deleteByUserId(userId);
        log.debug("Wallet deleted for userId: {}", userId);
    }

    @Override
    // NOTE: Removed @Transactional - this is called from ImportShopeeOrdersUseCase
    // which already has a transaction. Nested @Transactional causes rollback-only issues.
    public boolean confirmPendingBalanceDirectly(Long userId, BigDecimal amount) {
        log.info("confirmPendingBalanceDirectly: userId={}, amount={}", userId, amount);
        int updated = jpaRepository.confirmPendingBalanceDirectly(userId, amount);
        log.info("confirmPendingBalanceDirectly: {} row(s) updated", updated);
        return updated > 0;
    }

    @Override
    // NOTE: Removed @Transactional - called from import flow which has transaction
    public boolean addPendingBalanceDirectly(Long userId, BigDecimal amount) {
        log.info("addPendingBalanceDirectly: userId={}, amount={}", userId, amount);
        int updated = jpaRepository.addPendingBalanceDirectly(userId, amount);
        log.info("addPendingBalanceDirectly: {} row(s) updated", updated);
        return updated > 0;
    }

    @Override
    // NOTE: Removed @Transactional - called from import flow which has transaction
    public boolean addConfirmedBalanceDirectly(Long userId, BigDecimal amount) {
        log.info("addConfirmedBalanceDirectly: userId={}, amount={}", userId, amount);
        int updated = jpaRepository.addConfirmedBalanceDirectly(userId, amount);
        log.info("addConfirmedBalanceDirectly: {} row(s) updated", updated);
        return updated > 0;
    }

    @Override
    // NOTE: Removed @Transactional - called from import flow which has transaction
    public boolean subtractPendingBalanceDirectly(Long userId, BigDecimal amount) {
        log.info("subtractPendingBalanceDirectly: userId={}, amount={}", userId, amount);
        int updated = jpaRepository.subtractPendingBalanceDirectly(userId, amount);
        log.info("subtractPendingBalanceDirectly: {} row(s) updated", updated);
        return updated > 0;
    }

    @Override
    // NOTE: Removed @Transactional - called from import flow which has transaction
    public boolean reverseConfirmedBalanceDirectly(Long userId, BigDecimal amount) {
        log.info("reverseConfirmedBalanceDirectly: userId={}, amount={}", userId, amount);
        int updated = jpaRepository.reverseConfirmedBalanceDirectly(userId, amount);
        log.info("reverseConfirmedBalanceDirectly: {} row(s) updated", updated);
        return updated > 0;
    }
}
