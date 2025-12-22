package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.infrastructure.persistence.entity.UserJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.cashbee.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing UserRepository domain interface using JPA.
 *
 * This is the "Adapter" in Hexagonal Architecture (Ports & Adapters pattern).
 * It implements the domain's port (UserRepository interface) and adapts it
 * to the infrastructure technology (JPA/Spring Data).
 *
 * Key responsibilities:
 * - Maps between domain models and JPA entities
 * - Delegates persistence operations to Spring Data JPA repository
 * - Handles transactions
 * - Provides clean abstraction over database operations
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    @Transactional
    public User save(User user) {
        log.debug("Saving user: keycloakId={}, username={}", user.getKeycloakId(), user.getUsername());

        UserJpaEntity entity;

        if (user.isNew()) {
            // New user - create entity
            entity = mapper.toEntity(user);
        } else {
            // Existing user - update entity
            entity = jpaRepository.findById(user.getId())
                .orElseGet(() -> mapper.toEntity(user));

            // Update fields from domain model
            mapper.updateEntityFromDomain(user, entity);
        }

        UserJpaEntity saved = jpaRepository.save(entity);
        User result = mapper.toDomain(saved);

        log.debug("User saved: id={}, keycloakId={}", result.getId(), result.getKeycloakId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakId(String keycloakId) {
        log.debug("Finding user by keycloakId: {}", keycloakId);
        return jpaRepository.findByKeycloakId(keycloakId)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return jpaRepository.findByUsername(username)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByReferralCode(String referralCode) {
        log.debug("Finding user by referralCode: {}", referralCode);
        return jpaRepository.findByReferralCode(referralCode)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        log.debug("Checking if user exists by id: {}", id);
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByKeycloakId(String keycloakId) {
        log.debug("Checking if user exists by keycloakId: {}", keycloakId);
        return jpaRepository.existsByKeycloakId(keycloakId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        log.debug("Checking if user exists by email: {}", email);
        return jpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        log.debug("Checking if user exists by username: {}", username);
        return jpaRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByReferralCode(String referralCode) {
        log.debug("Checking if user exists by referralCode: {}", referralCode);
        return jpaRepository.existsByReferralCode(referralCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        log.debug("Checking if user exists by phone: {}", phone);
        return jpaRepository.existsByPhone(phone);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByReferredBy(String referredBy) {
        log.debug("Checking if user exists by referredBy: {}", referredBy);
        return jpaRepository.existsByReferredBy(referredBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByReferredBy(String referralCode) {
        log.debug("Finding users referred by: {}", referralCode);
        List<UserJpaEntity> entities = jpaRepository.findByReferredBy(referralCode);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        log.debug("Finding all active users");
        List<UserJpaEntity> entities = jpaRepository.findAllActive();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        log.debug("Finding all active users with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<UserJpaEntity> entityPage = jpaRepository.findAllActive(pageable);
        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByStatus(UserStatus status, Pageable pageable) {
        log.debug("Finding users by status: status={}, page={}, size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());
        Page<UserJpaEntity> entityPage = jpaRepository.findByStatusActive(status.name(), pageable);
        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchByEmailOrUsername(String keyword, Pageable pageable) {
        log.debug("Searching users by keyword: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());
        Page<UserJpaEntity> entityPage = jpaRepository.searchByEmailOrUsername(keyword, pageable);
        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.debug("Soft deleting user by id: {}", id);

        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(entity);
            log.debug("User soft deleted: id={}", id);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        log.debug("Counting all active users");
        return jpaRepository.countActive();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        log.debug("Counting users by status: {}", status);
        return jpaRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findUsersWithOrdersInDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        log.debug("Finding users with orders in date range: fromDate={}, toDate={}, page={}, size={}",
                fromDate, toDate, pageable.getPageNumber(), pageable.getPageSize());

        Page<UserJpaEntity> entityPage = jpaRepository.findUsersWithOrdersInDateRange(
                fromDate, toDate, pageable);

        log.debug("Found {} users with orders in date range", entityPage.getTotalElements());
        return entityPage.map(mapper::toDomain);
    }
}
