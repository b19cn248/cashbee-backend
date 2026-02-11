package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import com.cashbee.infrastructure.persistence.entity.PromotionVoucherJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.PromotionVoucherMapper;
import com.cashbee.infrastructure.persistence.repository.PromotionVoucherJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of PromotionVoucherRepository.
 * Bridges domain layer (PromotionVoucher) with infrastructure layer (PromotionVoucherJpaEntity).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
public class PromotionVoucherRepositoryAdapter implements PromotionVoucherRepository {

    private final PromotionVoucherJpaRepository jpaRepository;
    private final PromotionVoucherMapper mapper;

    @Override
    public Optional<PromotionVoucher> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<PromotionVoucher> findByCode(String code) {
        return jpaRepository.findByCode(code)
            .map(mapper::toDomain);
    }

    @Override
    public List<PromotionVoucher> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Page<PromotionVoucher> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionVoucher> findByStatus(VoucherStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionVoucher> findByStatusAndCategory(
            VoucherStatus status,
            VoucherCategory category,
            Pageable pageable) {
        return jpaRepository.findByStatusAndCategory(status, category, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionVoucher> findByStatusAndPlatform(
            VoucherStatus status,
            String platform,
            Pageable pageable) {
        return jpaRepository.findByStatusAndPlatform(status, platform, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionVoucher> findByStatusAndCategoryAndPlatform(
            VoucherStatus status,
            VoucherCategory category,
            String platform,
            Pageable pageable) {
        return jpaRepository.findByStatusAndCategoryAndPlatform(status, category, platform, pageable)
            .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionVoucher> findByStatusActive(Pageable pageable) {
        return findByStatus(VoucherStatus.ACTIVE, pageable);
    }

    @Override
    public PromotionVoucher save(PromotionVoucher voucher) {
        PromotionVoucherJpaEntity entity = mapper.toEntity(voucher);
        PromotionVoucherJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<PromotionVoucher> saveAll(List<PromotionVoucher> vouchers) {
        List<PromotionVoucherJpaEntity> entities = vouchers.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        List<PromotionVoucherJpaEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public long countByStatus(VoucherStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public int incrementViewCount(Long id) {
        return jpaRepository.incrementViewCount(id);
    }

    @Override
    public int incrementClickCount(Long id) {
        return jpaRepository.incrementClickCount(id);
    }
}
