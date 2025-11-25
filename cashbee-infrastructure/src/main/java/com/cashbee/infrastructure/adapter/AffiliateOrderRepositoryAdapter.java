package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.infrastructure.mapper.AffiliateOrderMapper;
import com.cashbee.infrastructure.repository.AffiliateOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for AffiliateOrderRepository.
 * This is the ADAPTER in hexagonal architecture.
 * Bridges domain layer (port) with infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class AffiliateOrderRepositoryAdapter implements AffiliateOrderRepository {

    private final AffiliateOrderJpaRepository jpaRepository;
    private final AffiliateOrderMapper mapper;

    @Override
    public AffiliateOrder save(AffiliateOrder order) {
        var entity = mapper.toEntity(order);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AffiliateOrder> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<AffiliateOrder> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public List<AffiliateOrder> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public List<AffiliateOrder> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Page<AffiliateOrder> findByUserId(Long userId, Pageable pageable) {
        Page<com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity> entityPage =
            jpaRepository.findByUserId(userId, pageable);
        return entityPage.map(mapper::toDomain);
    }

    @Override
    @Deprecated
    public List<AffiliateOrder> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return jpaRepository.findByUserIdAndOrderStatus(userId, status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Page<AffiliateOrder> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable) {
        Page<com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity> entityPage =
            jpaRepository.findByUserIdAndOrderStatus(userId, status, pageable);
        return entityPage.map(mapper::toDomain);
    }

    @Override
    public List<AffiliateOrder> findByPlatformId(Long platformId) {
        return jpaRepository.findByPlatformId(platformId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateOrder> findByImportBatchId(Long importBatchId) {
        return jpaRepository.findByImportBatchId(importBatchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateOrder> findByStatus(OrderStatus status) {
        return jpaRepository.findByOrderStatus(status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }
}
