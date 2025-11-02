package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.model.CashbackPolicy;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import com.cashbee.infrastructure.mapper.CashbackPolicyMapper;
import com.cashbee.infrastructure.persistence.repository.CashbackPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of CashbackPolicyRepository.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CashbackPolicyRepositoryAdapter implements CashbackPolicyRepository {

    private final CashbackPolicyJpaRepository jpaRepository;
    private final CashbackPolicyMapper mapper;

    @Override
    public CashbackPolicy save(CashbackPolicy policy) {
        log.debug("Saving cashback policy: {}", policy.getPolicyCode());

        var entity = mapper.toEntity(policy);
        var savedEntity = jpaRepository.save(entity);
        var savedDomain = mapper.toDomain(savedEntity);

        log.info("Saved cashback policy with ID: {}", savedDomain.getId());
        return savedDomain;
    }

    @Override
    public Optional<CashbackPolicy> findById(Long id) {
        log.debug("Finding cashback policy by ID: {}", id);

        return jpaRepository.findById(id)
            .map(entity -> {
                log.debug("Found policy: {}", entity.getPolicyCode());
                return mapper.toDomain(entity);
            });
    }

    @Override
    public Optional<CashbackPolicy> findByCode(String code) {
        log.debug("Finding cashback policy by code: {}", code);

        return jpaRepository.findByPolicyCode(code)
            .map(entity -> {
                log.debug("Found policy: {}", entity.getPolicyName());
                return mapper.toDomain(entity);
            });
    }

    @Override
    public List<CashbackPolicy> findAll() {
        log.debug("Finding all cashback policies");

        var policies = jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());

        log.info("Found {} cashback policies", policies.size());
        return policies;
    }

    @Override
    public List<CashbackPolicy> findAllActive() {
        log.debug("Finding all active cashback policies");

        var policies = jpaRepository.findByIsActiveTrue().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());

        log.info("Found {} active cashback policies", policies.size());
        return policies;
    }

    @Override
    public Optional<CashbackPolicy> findActivePolicyFor(
        Long platformId,
        UserLevel userLevel,
        LocalDateTime now
    ) {
        log.debug("Finding active policy for platform: {}, userLevel: {}, now: {}",
            platformId, userLevel, now);

        var policy = jpaRepository.findActivePolicyFor(platformId, userLevel, now)
            .map(entity -> {
                log.info("Found active policy: {} (priority: {})",
                    entity.getPolicyCode(), entity.getPriority());
                return mapper.toDomain(entity);
            });

        if (policy.isEmpty()) {
            log.warn("No active policy found for platform: {}, userLevel: {}",
                platformId, userLevel);
        }

        return policy;
    }

    @Override
    public List<CashbackPolicy> findByPlatformId(Long platformId) {
        log.debug("Finding policies for platform: {}", platformId);

        return jpaRepository.findByPlatformId(platformId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<CashbackPolicy> findByUserLevel(UserLevel userLevel) {
        log.debug("Finding policies for user level: {}", userLevel);

        return jpaRepository.findByUserLevel(userLevel).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        log.info("Deleting cashback policy with ID: {}", id);
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        log.debug("Checking if policy exists by code: {}", code);
        boolean exists = jpaRepository.existsByPolicyCode(code);
        log.debug("Policy {} exists: {}", code, exists);
        return exists;
    }
}
