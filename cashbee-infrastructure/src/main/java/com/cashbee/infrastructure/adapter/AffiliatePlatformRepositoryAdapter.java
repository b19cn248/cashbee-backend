package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.PlatformStatus;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.infrastructure.mapper.AffiliatePlatformMapper;
import com.cashbee.infrastructure.persistence.repository.AffiliatePlatformJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of AffiliatePlatformRepository.
 * This is the ADAPTER in hexagonal architecture.
 *
 * Implements the domain repository interface (PORT)
 * using JPA repository (infrastructure technology).
 *
 * Domain layer depends on the interface (PORT).
 * Infrastructure layer provides the implementation (ADAPTER).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AffiliatePlatformRepositoryAdapter implements AffiliatePlatformRepository {

    private final AffiliatePlatformJpaRepository jpaRepository;
    private final AffiliatePlatformMapper mapper;

    @Override
    public AffiliatePlatform save(AffiliatePlatform platform) {
        log.debug("Saving affiliate platform: {}", platform);

        var entity = mapper.toEntity(platform);
        var savedEntity = jpaRepository.save(entity);
        var savedDomain = mapper.toDomain(savedEntity);

        log.info("Saved affiliate platform with ID: {}", savedDomain.getId());
        return savedDomain;
    }

    @Override
    public Optional<AffiliatePlatform> findById(Long id) {
        log.debug("Finding affiliate platform by ID: {}", id);

        return jpaRepository.findById(id)
            .map(entity -> {
                log.debug("Found affiliate platform: {}", entity.getCode());
                return mapper.toDomain(entity);
            });
    }

    @Override
    public Optional<AffiliatePlatform> findByCode(String code) {
        log.debug("Finding affiliate platform by code: {}", code);

        return jpaRepository.findByCode(code)
            .map(entity -> {
                log.debug("Found affiliate platform: {}", entity.getName());
                return mapper.toDomain(entity);
            });
    }

    @Override
    public List<AffiliatePlatform> findAll() {
        log.debug("Finding all affiliate platforms");

        var platforms = jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());

        log.info("Found {} affiliate platforms", platforms.size());
        return platforms;
    }

    @Override
    public List<AffiliatePlatform> findAllActive() {
        log.debug("Finding all active affiliate platforms");

        var platforms = jpaRepository.findByStatus(PlatformStatus.ACTIVE).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());

        log.info("Found {} active affiliate platforms", platforms.size());
        return platforms;
    }

    @Override
    public void deleteById(Long id) {
        log.info("Deleting affiliate platform with ID: {}", id);
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        log.debug("Checking if affiliate platform exists by code: {}", code);
        boolean exists = jpaRepository.existsByCode(code);
        log.debug("Platform {} exists: {}", code, exists);
        return exists;
    }
}
