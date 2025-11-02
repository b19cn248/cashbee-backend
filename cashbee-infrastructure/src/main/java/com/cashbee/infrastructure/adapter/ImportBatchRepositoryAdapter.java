package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.ImportStatus;
import com.cashbee.domain.model.ImportBatch;
import com.cashbee.domain.repository.ImportBatchRepository;
import com.cashbee.infrastructure.mapper.ImportBatchMapper;
import com.cashbee.infrastructure.repository.ImportBatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter for ImportBatchRepository.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class ImportBatchRepositoryAdapter implements ImportBatchRepository {

    private final ImportBatchJpaRepository jpaRepository;
    private final ImportBatchMapper mapper;

    @Override
    public ImportBatch save(ImportBatch batch) {
        var entity = mapper.toEntity(batch);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ImportBatch> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ImportBatch> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ImportBatch> findByPlatformId(Long platformId) {
        return jpaRepository.findByPlatformId(platformId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ImportBatch> findByStatus(ImportStatus status) {
        return jpaRepository.findByStatus(status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ImportBatch> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ImportBatch> findLatestByPlatformId(Long platformId) {
        return jpaRepository.findLatestByPlatformId(platformId).map(mapper::toDomain);
    }

    @Override
    public List<ImportBatch> findByImportedBy(Long userId) {
        return jpaRepository.findByImportedBy(userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(ImportStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
