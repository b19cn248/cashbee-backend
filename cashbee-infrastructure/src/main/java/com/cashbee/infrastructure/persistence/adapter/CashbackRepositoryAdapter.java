package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.infrastructure.persistence.entity.CashbackJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.CashbackMapper;
import com.cashbee.infrastructure.persistence.repository.CashbackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of CashbackRepository.
 * Bridges domain layer (Cashback) with infrastructure layer (CashbackJpaEntity).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class CashbackRepositoryAdapter implements CashbackRepository {

    private final CashbackJpaRepository jpaRepository;
    private final CashbackMapper mapper;

    @Override
    public Cashback save(Cashback cashback) {
        CashbackJpaEntity entity = mapper.toEntity(cashback);
        CashbackJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Cashback> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Cashback> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Cashback> findAllByOrderId(Long orderId) {
        return jpaRepository.findAllByOrderId(orderId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cashback> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cashback> findByUserIdAndStatus(Long userId, CashbackStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<Cashback> findByOrderItemId(Long orderItemId) {
        return jpaRepository.findByOrderItemId(orderItemId)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByOrderItemId(Long orderItemId) {
        return jpaRepository.existsByOrderItemId(orderItemId);
    }

    @Override
    public BigDecimal sumCashbackAmountByUserIdAndStatus(Long userId, CashbackStatus status) {
        return jpaRepository.sumCashbackAmountByUserIdAndStatus(userId, status);
    }

    @Override
    public BigDecimal sumCashbackAmountByUserIdAndStatusIn(Long userId, List<CashbackStatus> statuses) {
        return jpaRepository.sumCashbackAmountByUserIdAndStatusIn(userId, statuses);
    }

    @Override
    @Deprecated
    public int updateStatusByUserIdAndStatus(Long userId, CashbackStatus oldStatus, CashbackStatus newStatus) {
        return jpaRepository.updateStatusByUserIdAndStatus(userId, oldStatus, newStatus);
    }

    @Override
    public int updateStatusByUserIdAndStatusWithBatchId(Long userId, CashbackStatus oldStatus, CashbackStatus newStatus, Long batchId) {
        return jpaRepository.updateStatusByUserIdAndStatusWithBatchId(userId, oldStatus, newStatus, batchId);
    }

    @Override
    public int updateStatusByUserIdAndStatusWithBatchIdBeforeDate(
            Long userId,
            CashbackStatus oldStatus,
            CashbackStatus newStatus,
            Long batchId,
            java.time.LocalDateTime batchCreatedAt) {
        return jpaRepository.updateStatusByUserIdAndStatusWithBatchIdBeforeDate(
                userId, oldStatus, newStatus, batchId, batchCreatedAt);
    }

    @Override
    public List<Cashback> findByPaidBatchId(Long batchId) {
        return jpaRepository.findByPaidBatchId(batchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Cashback> findByPaidBatchIdAndUserId(Long batchId, Long userId) {
        return jpaRepository.findByPaidBatchIdAndUserId(batchId, userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumUnpaidConfirmedCashbackByUserId(Long userId) {
        return jpaRepository.sumUnpaidConfirmedCashbackByUserId(userId);
    }

    @Override
    public int updateStatusByCashbackIdsWithBatchId(List<Long> cashbackIds, CashbackStatus oldStatus, CashbackStatus newStatus, Long batchId) {
        if (cashbackIds == null || cashbackIds.isEmpty()) {
            return 0;
        }
        return jpaRepository.updateStatusByCashbackIdsWithBatchId(cashbackIds, oldStatus, newStatus, batchId);
    }

    @Override
    public int countConfirmedOrdersByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        return jpaRepository.countConfirmedOrdersByUserId(userId);
    }

    @Override
    public List<Object[]> findCashbacksWithOrderDetailsByBatchIdAndUserId(Long batchId, Long userId) {
        if (batchId == null || userId == null) {
            return List.of();
        }
        return jpaRepository.findCashbacksWithOrderDetailsByBatchIdAndUserId(batchId, userId);
    }
}
