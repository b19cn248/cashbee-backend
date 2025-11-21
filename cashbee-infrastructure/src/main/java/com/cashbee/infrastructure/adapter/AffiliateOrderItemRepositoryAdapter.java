package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.infrastructure.mapper.AffiliateOrderItemMapper;
import com.cashbee.infrastructure.repository.AffiliateOrderItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for AffiliateOrderItemRepository.
 * This is the ADAPTER in hexagonal architecture.
 * Bridges domain layer (port) with infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class AffiliateOrderItemRepositoryAdapter implements AffiliateOrderItemRepository {

    private final AffiliateOrderItemJpaRepository jpaRepository;
    private final AffiliateOrderItemMapper mapper;

    @Override
    public AffiliateOrderItem save(AffiliateOrderItem item) {
        var entity = mapper.toEntity(item);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<AffiliateOrderItem> saveAll(List<AffiliateOrderItem> items) {
        var entities = items.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());

        var savedEntities = jpaRepository.saveAll(entities);

        return savedEntities.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<AffiliateOrderItem> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<AffiliateOrderItem> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateOrderItem> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        jpaRepository.deleteByOrderId(orderId);
    }

    @Override
    public long countByOrderId(Long orderId) {
        return jpaRepository.countByOrderId(orderId);
    }

    @Override
    public Optional<AffiliateOrderItem> findByOrderIdAndItemId(Long orderId, String itemId) {
        return jpaRepository.findByOrderIdAndItemId(orderId, itemId)
            .map(mapper::toDomain);
    }
}
