package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.ReferrerCommissionStatus;
import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.infrastructure.persistence.entity.ReferrerCommissionJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.ReferrerCommissionMapper;
import com.cashbee.infrastructure.persistence.repository.ReferrerCommissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing ReferrerCommissionRepository domain interface using JPA.
 *
 * <p>This is the "Adapter" in Hexagonal Architecture (Ports & Adapters pattern).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferrerCommissionRepositoryAdapter implements ReferrerCommissionRepository {

    private final ReferrerCommissionJpaRepository jpaRepository;
    private final ReferrerCommissionMapper mapper;

    @Override
    @Transactional
    public ReferrerCommission save(ReferrerCommission commission) {
        log.debug("Saving referrer commission: referrerId={}, refereeId={}, orderId={}",
                commission.getReferrerId(), commission.getRefereeId(), commission.getSourceOrderId());

        ReferrerCommissionJpaEntity entity;

        if (commission.isNew()) {
            entity = mapper.toEntity(commission);
        } else {
            entity = jpaRepository.findById(commission.getId())
                    .orElseGet(() -> mapper.toEntity(commission));
            mapper.updateEntityFromDomain(commission, entity);
        }

        ReferrerCommissionJpaEntity saved = jpaRepository.save(entity);
        ReferrerCommission result = mapper.toDomain(saved);

        log.debug("Referrer commission saved: id={}, amount={}",
                result.getId(), result.getCommissionAmount());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferrerCommission> findById(Long id) {
        log.debug("Finding referrer commission by id: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findByReferrerId(Long referrerId) {
        log.debug("Finding referrer commissions by referrerId: {}", referrerId);
        List<ReferrerCommissionJpaEntity> entities = jpaRepository.findByReferrerId(referrerId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findByReferrerIdAndStatus(Long referrerId, ReferrerCommissionStatus status) {
        log.debug("Finding referrer commissions by referrerId={} and status={}", referrerId, status);
        List<ReferrerCommissionJpaEntity> entities =
                jpaRepository.findByReferrerIdAndStatus(referrerId, status.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findByRefereeId(Long refereeId) {
        log.debug("Finding referrer commissions by refereeId: {}", refereeId);
        List<ReferrerCommissionJpaEntity> entities = jpaRepository.findByRefereeId(refereeId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferrerCommission> findBySourceOrderId(Long sourceOrderId) {
        log.debug("Finding referrer commission by sourceOrderId: {}", sourceOrderId);
        return jpaRepository.findBySourceOrderId(sourceOrderId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySourceOrderId(Long sourceOrderId) {
        log.debug("Checking if referrer commission exists for orderId: {}", sourceOrderId);
        return jpaRepository.existsBySourceOrderId(sourceOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findByStatus(ReferrerCommissionStatus status) {
        log.debug("Finding referrer commissions by status: {}", status);
        List<ReferrerCommissionJpaEntity> entities = jpaRepository.findByStatus(status.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findExpiredPendingCommissions(LocalDateTime now) {
        log.debug("Finding expired pending commissions before: {}", now);
        List<ReferrerCommissionJpaEntity> entities = jpaRepository.findExpiredPendingCommissions(now);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumCommissionByReferrerId(Long referrerId) {
        log.debug("Summing total commission for referrerId: {}", referrerId);
        BigDecimal sum = jpaRepository.sumCommissionByReferrerId(referrerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumPendingCommissionByReferrerId(Long referrerId) {
        log.debug("Summing pending commission for referrerId: {}", referrerId);
        BigDecimal sum = jpaRepository.sumPendingCommissionByReferrerId(referrerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumConfirmedCommissionByReferrerId(Long referrerId) {
        log.debug("Summing confirmed commission for referrerId: {}", referrerId);
        BigDecimal sum = jpaRepository.sumConfirmedCommissionByReferrerId(referrerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumPaidCommissionByReferrerId(Long referrerId) {
        log.debug("Summing paid commission for referrerId: {}", referrerId);
        BigDecimal sum = jpaRepository.sumPaidCommissionByReferrerId(referrerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByReferrerId(Long referrerId) {
        log.debug("Counting commissions by referrerId: {}", referrerId);
        return jpaRepository.countByReferrerId(referrerId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRefereeId(Long refereeId) {
        log.debug("Counting commissions by refereeId: {}", refereeId);
        return jpaRepository.countByRefereeId(refereeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerCommission> findByReferrerIdAndRefereeId(Long referrerId, Long refereeId) {
        log.debug("Finding commissions by referrerId={} and refereeId={}", referrerId, refereeId);
        List<ReferrerCommissionJpaEntity> entities =
                jpaRepository.findByReferrerIdAndRefereeId(referrerId, refereeId);
        return mapper.toDomainList(entities);
    }
}
