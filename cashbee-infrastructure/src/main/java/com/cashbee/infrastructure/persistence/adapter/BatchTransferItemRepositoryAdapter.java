package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.domain.model.BatchTransferItem;
import com.cashbee.domain.repository.BatchTransferItemRepository;
import com.cashbee.infrastructure.persistence.mapper.BatchTransferItemMapper;
import com.cashbee.infrastructure.persistence.repository.BatchTransferItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of BatchTransferItemRepository.
 * Bridges domain layer with infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class BatchTransferItemRepositoryAdapter implements BatchTransferItemRepository {

    private final BatchTransferItemJpaRepository jpaRepository;
    private final BatchTransferItemMapper mapper;

    @Override
    public BatchTransferItem save(BatchTransferItem item) {
        var entity = mapper.toEntity(item);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<BatchTransferItem> saveAll(List<BatchTransferItem> items) {
        var entities = items.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        var savedEntities = jpaRepository.saveAll(entities);

        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BatchTransferItem> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<BatchTransferItem> findByBatchId(Long batchId) {
        return jpaRepository.findByBatchId(batchId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchTransferItem> findByBatchIdAndStatus(Long batchId, BatchItemStatus status) {
        return jpaRepository.findByBatchIdAndStatus(batchId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByBatchId(Long batchId) {
        return jpaRepository.countByBatchId(batchId);
    }

    @Override
    public long countByBatchIdAndStatus(Long batchId, BatchItemStatus status) {
        return jpaRepository.countByBatchIdAndStatus(batchId, status);
    }
}
