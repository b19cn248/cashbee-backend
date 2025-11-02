package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.infrastructure.mapper.AffiliateClickMapper;
import com.cashbee.infrastructure.repository.AffiliateClickJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter for AffiliateClickRepository.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class AffiliateClickRepositoryAdapter implements AffiliateClickRepository {

    private final AffiliateClickJpaRepository jpaRepository;
    private final AffiliateClickMapper mapper;

    @Override
    public AffiliateClick save(AffiliateClick click) {
        var entity = mapper.toEntity(click);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AffiliateClick> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AffiliateClick> findByTrackingCode(String trackingCode) {
        return jpaRepository.findByTrackingCode(trackingCode).map(mapper::toDomain);
    }

    @Override
    public List<AffiliateClick> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateClick> findByUserIdAndPlatformId(Long userId, Long platformId) {
        return jpaRepository.findByUserIdAndPlatformId(userId, platformId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateClick> findByStatus(ClickStatus status) {
        return jpaRepository.findByStatus(status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateClick> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliateClick> findExpiredClicks(LocalDateTime cutoffDate) {
        return jpaRepository.findExpiredClicks(cutoffDate).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public long countByOrderMatchedTrue() {
        return jpaRepository.countByOrderMatchedTrue();
    }

    @Override
    public double getConversionRate() {
        Double rate = jpaRepository.calculateConversionRate();
        return rate != null ? rate : 0.0;
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
