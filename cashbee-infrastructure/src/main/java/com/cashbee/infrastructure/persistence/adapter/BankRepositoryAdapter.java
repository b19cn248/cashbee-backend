package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.Bank;
import com.cashbee.domain.repository.BankRepository;
import com.cashbee.infrastructure.persistence.entity.BankJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.BankMapper;
import com.cashbee.infrastructure.persistence.repository.BankJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of BankRepository.
 * Bridges domain layer (port) with infrastructure layer (JPA).
 *
 * This is the "adapter" in Hexagonal Architecture:
 * - Implements domain repository interface (port)
 * - Uses infrastructure components (JPA repository, mapper)
 * - Translates between domain models and JPA entities
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BankRepositoryAdapter implements BankRepository {

    private final BankJpaRepository bankJpaRepository;
    private final BankMapper bankMapper;

    @Override
    public Optional<Bank> findByBankCode(String bankCode) {
        log.debug("Finding bank by code: {}", bankCode);
        return bankJpaRepository.findByBankCode(bankCode)
                .map(bankMapper::toDomain);
    }

    @Override
    public List<Bank> findAllActive() {
        log.debug("Finding all active banks");
        return bankJpaRepository.findAllByIsActive(true)
                .stream()
                .map(bankMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bank> findAll() {
        log.debug("Finding all banks");
        return bankJpaRepository.findAll()
                .stream()
                .map(bankMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Bank> findById(Long id) {
        log.debug("Finding bank by ID: {}", id);
        return bankJpaRepository.findById(id)
                .map(bankMapper::toDomain);
    }

    @Override
    public Bank save(Bank bank) {
        log.debug("Saving bank: {}", bank.getBankCode());

        BankJpaEntity entity = bankMapper.toEntity(bank);
        BankJpaEntity savedEntity = bankJpaRepository.save(entity);

        log.info("Bank saved successfully: id={}, code={}",
                savedEntity.getId(), savedEntity.getBankCode());

        return bankMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        log.warn("Deleting bank: id={}", id);
        bankJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByBankCode(String bankCode) {
        log.debug("Checking if bank exists: {}", bankCode);
        return bankJpaRepository.existsByBankCode(bankCode);
    }

    @Override
    public long count() {
        return bankJpaRepository.count();
    }
}
