package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import com.cashbee.infrastructure.persistence.mapper.ReferrerTierConfigMapper;
import com.cashbee.infrastructure.persistence.repository.ReferrerTierConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter for ReferrerTierConfig.
 * Implements the domain repository interface using JPA.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class ReferrerTierConfigRepositoryAdapter implements ReferrerTierConfigRepository {

    private final ReferrerTierConfigJpaRepository jpaRepository;
    private final ReferrerTierConfigMapper mapper;

    @Override
    public Optional<ReferrerTierConfig> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ReferrerTierConfig> findByTierName(String tierName) {
        return jpaRepository.findByTierName(tierName)
                .map(mapper::toDomain);
    }

    @Override
    public List<ReferrerTierConfig> findAllActiveOrderByMinReferrals() {
        return mapper.toDomainList(jpaRepository.findAllActiveOrderByMinReferrals());
    }

    @Override
    public Optional<ReferrerTierConfig> findTierByReferralCount(int activatedReferrals) {
        return jpaRepository.findTierByReferralCount(activatedReferrals)
                .map(mapper::toDomain);
    }

    @Override
    public ReferrerTierConfig save(ReferrerTierConfig tierConfig) {
        var entity = mapper.toEntity(tierConfig);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ReferrerTierConfig> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
