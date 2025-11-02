package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.infrastructure.persistence.entity.PayoutRequestJpaEntity;
import com.cashbee.infrastructure.persistence.repository.PayoutRequestJpaRepository;
import com.cashbee.infrastructure.persistence.mapper.PayoutRequestPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements PayoutRequestRepository port using JPA.
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
public class PayoutRequestRepositoryAdapter implements PayoutRequestRepository {

    private final PayoutRequestJpaRepository jpaRepository;
    private final PayoutRequestPersistenceMapper mapper;

    @Override
    public PayoutRequest save(PayoutRequest payoutRequest) {
        log.debug("Saving payout request: userId={}, amount={}, method={}",
                payoutRequest.getUserId(), payoutRequest.getAmount(), payoutRequest.getPayoutMethod());

        PayoutRequestJpaEntity entity = mapper.toEntity(payoutRequest);
        PayoutRequestJpaEntity savedEntity = jpaRepository.save(entity);

        log.debug("Payout request saved with ID: {}", savedEntity.getId());
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PayoutRequest> findById(Long id) {
        log.debug("Finding payout request by ID: {}", id);

        return jpaRepository.findById(id)
                .map(entity -> {
                    log.debug("Payout request found: id={}", id);
                    return mapper.toDomain(entity);
                });
    }

    @Override
    public List<PayoutRequest> findByUserId(Long userId) {
        log.debug("Finding all payout requests for user: {}", userId);

        List<PayoutRequestJpaEntity> entities = jpaRepository.findByUserId(userId);
        log.debug("Found {} payout requests for user {}", entities.size(), userId);

        return mapper.toDomainList(entities);
    }

    @Override
    public List<PayoutRequest> findByUserId(Long userId, int page, int size) {
        log.debug("Finding payout requests for user {} (page={}, size={})", userId, page, size);

        // Sort by requestedAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        List<PayoutRequestJpaEntity> entities = jpaRepository.findByUserId(userId, pageable);

        log.debug("Found {} payout requests for user {} (page {})", entities.size(), userId, page);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<PayoutRequest> findByStatus(PayoutStatus status) {
        log.debug("Finding all payout requests with status: {}", status);

        List<PayoutRequestJpaEntity> entities = jpaRepository.findByStatus(status);
        log.debug("Found {} payout requests with status {}", entities.size(), status);

        return mapper.toDomainList(entities);
    }

    @Override
    public List<PayoutRequest> findByStatus(PayoutStatus status, int page, int size) {
        log.debug("Finding payout requests with status {} (page={}, size={})", status, page, size);

        // Sort by requestedAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        List<PayoutRequestJpaEntity> entities = jpaRepository.findByStatus(status, pageable);

        log.debug("Found {} payout requests with status {} (page {})", entities.size(), status, page);
        return mapper.toDomainList(entities);
    }

    @Override
    public List<PayoutRequest> findAll(int page, int size) {
        log.debug("Finding all payout requests (page={}, size={})", page, size);

        // Sort by requestedAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        List<PayoutRequestJpaEntity> entities = jpaRepository.findAll(pageable).getContent();

        log.debug("Found {} payout requests (page {})", entities.size(), page);
        return mapper.toDomainList(entities);
    }

    @Override
    public long countByUserId(Long userId) {
        log.debug("Counting payout requests for user: {}", userId);

        long count = jpaRepository.countByUserId(userId);
        log.debug("User {} has {} payout requests", userId, count);

        return count;
    }

    @Override
    public long countByStatus(PayoutStatus status) {
        log.debug("Counting payout requests with status: {}", status);

        long count = jpaRepository.countByStatus(status);
        log.debug("Status {} has {} payout requests", status, count);

        return count;
    }

    @Override
    public List<PayoutRequest> findByUserIdAndStatus(Long userId, PayoutStatus status, int page, int size) {
        log.debug("Finding payout requests for user {} with status {} (page={}, size={})",
                userId, status, page, size);

        // Sort by requestedAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        List<PayoutRequestJpaEntity> entities = jpaRepository.findByUserIdAndStatus(userId, status, pageable);

        log.debug("Found {} payout requests for user {} with status {} (page {})",
                entities.size(), userId, status, page);
        return mapper.toDomainList(entities);
    }

    @Override
    public long countByUserIdAndStatus(Long userId, PayoutStatus status) {
        log.debug("Counting payout requests for user {} with status: {}", userId, status);

        long count = jpaRepository.countByUserIdAndStatus(userId, status);
        log.debug("User {} has {} payout requests with status {}", userId, count, status);

        return count;
    }

    @Override
    public long countAll() {
        log.debug("Counting all payout requests");

        long count = jpaRepository.count();
        log.debug("Total payout requests: {}", count);

        return count;
    }

    @Override
    public void deleteById(Long id) {
        log.warn("Deleting payout request by ID: {} (should be rare for audit trail)", id);
        jpaRepository.deleteById(id);
        log.warn("Payout request deleted: id={}", id);
    }
}
