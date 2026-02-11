package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.BatchCashbackSnapshot;
import com.cashbee.domain.repository.BatchCashbackSnapshotRepository;
import com.cashbee.infrastructure.persistence.mapper.BatchCashbackSnapshotMapper;
import com.cashbee.infrastructure.persistence.repository.BatchCashbackSnapshotJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter implementation of BatchCashbackSnapshotRepository.
 * Bridges domain layer with infrastructure layer (JPA).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class BatchCashbackSnapshotRepositoryAdapter implements BatchCashbackSnapshotRepository {

    private final BatchCashbackSnapshotJpaRepository jpaRepository;
    private final BatchCashbackSnapshotMapper mapper;

    @Override
    public BatchCashbackSnapshot save(BatchCashbackSnapshot snapshot) {
        var entity = mapper.toEntity(snapshot);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<BatchCashbackSnapshot> saveAll(List<BatchCashbackSnapshot> snapshots) {
        var entities = snapshots.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        var savedEntities = jpaRepository.saveAll(entities);

        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchCashbackSnapshot> findByBatchId(Long batchId) {
        return jpaRepository.findByBatchId(batchId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchCashbackSnapshot> findByBatchIdAndUserId(Long batchId, Long userId) {
        return jpaRepository.findByBatchIdAndUserId(batchId, userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findCashbackIdsByBatchId(Long batchId) {
        return jpaRepository.findCashbackIdsByBatchId(batchId);
    }

    @Override
    public List<Long> findCashbackIdsByBatchIdAndUserId(Long batchId, Long userId) {
        return jpaRepository.findCashbackIdsByBatchIdAndUserId(batchId, userId);
    }

    @Override
    public long countByBatchId(Long batchId) {
        return jpaRepository.countByBatchId(batchId);
    }

    @Override
    public void deleteByBatchId(Long batchId) {
        jpaRepository.deleteByBatchId(batchId);
    }

    @Override
    public boolean existsByBatchIdAndCashbackId(Long batchId, Long cashbackId) {
        return jpaRepository.existsByBatchIdAndCashbackId(batchId, cashbackId);
    }
}
