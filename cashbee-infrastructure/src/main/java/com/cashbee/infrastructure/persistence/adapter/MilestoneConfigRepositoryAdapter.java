package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import com.cashbee.infrastructure.persistence.entity.MilestoneConfigJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.MilestoneConfigMapper;
import com.cashbee.infrastructure.persistence.repository.MilestoneConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing MilestoneConfigRepository domain interface using JPA.
 *
 * <p>This is the "Adapter" in Hexagonal Architecture (Ports & Adapters pattern).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MilestoneConfigRepositoryAdapter implements MilestoneConfigRepository {

    private final MilestoneConfigJpaRepository jpaRepository;
    private final MilestoneConfigMapper mapper;

    @Override
    @Transactional
    public MilestoneConfig save(MilestoneConfig config) {
        log.debug("Saving milestone config: type={}, ordersRequired={}",
                config.getMilestoneType(), config.getOrdersRequired());

        MilestoneConfigJpaEntity entity;

        if (config.getId() == null) {
            entity = mapper.toEntity(config);
        } else {
            entity = jpaRepository.findById(config.getId())
                    .orElseGet(() -> mapper.toEntity(config));
            mapper.updateEntityFromDomain(config, entity);
        }

        MilestoneConfigJpaEntity saved = jpaRepository.save(entity);
        MilestoneConfig result = mapper.toDomain(saved);

        log.debug("Milestone config saved: id={}", result.getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MilestoneConfig> findById(Long id) {
        log.debug("Finding milestone config by id: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findAllActive() {
        log.debug("Finding all active milestone configs");
        List<MilestoneConfigJpaEntity> entities = jpaRepository.findByIsActiveTrue();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findByMilestoneType(MilestoneType milestoneType) {
        log.debug("Finding milestone configs by type: {}", milestoneType);
        List<MilestoneConfigJpaEntity> entities =
                jpaRepository.findByMilestoneType(milestoneType.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findActiveByMilestoneType(MilestoneType milestoneType) {
        log.debug("Finding active milestone configs by type: {}", milestoneType);
        List<MilestoneConfigJpaEntity> entities =
                jpaRepository.findByMilestoneTypeAndIsActiveTrue(milestoneType.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MilestoneConfig> findByMilestoneTypeAndOrdersRequired(
            MilestoneType milestoneType,
            Integer ordersRequired
    ) {
        log.debug("Finding milestone config by type={} and ordersRequired={}",
                milestoneType, ordersRequired);
        return jpaRepository
                .findByMilestoneTypeAndOrdersRequired(milestoneType.name(), ordersRequired)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MilestoneConfig> findActiveByMilestoneTypeAndOrdersRequired(
            MilestoneType milestoneType,
            Integer ordersRequired
    ) {
        log.debug("Finding active milestone config by type={} and ordersRequired={}",
                milestoneType, ordersRequired);
        return jpaRepository
                .findByMilestoneTypeAndOrdersRequiredAndIsActiveTrue(milestoneType.name(), ordersRequired)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findNextMilestones(MilestoneType milestoneType, Integer currentOrders) {
        log.debug("Finding next milestones for type={} with currentOrders={}",
                milestoneType, currentOrders);
        List<MilestoneConfigJpaEntity> entities =
                jpaRepository.findNextMilestones(milestoneType.name(), currentOrders);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMilestoneTypeAndOrdersRequired(
            MilestoneType milestoneType,
            Integer ordersRequired
    ) {
        log.debug("Checking if milestone config exists: type={}, ordersRequired={}",
                milestoneType, ordersRequired);
        return jpaRepository.existsByMilestoneTypeAndOrdersRequired(milestoneType.name(), ordersRequired);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.debug("Deleting milestone config by id: {}", id);
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findAll() {
        log.debug("Finding all milestone configs");
        List<MilestoneConfigJpaEntity> entities = jpaRepository.findAll();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneConfig> findActiveByMilestoneTypeAndOrdersRequiredLessThanOrEqual(
            MilestoneType milestoneType,
            Integer maxOrders
    ) {
        log.debug("Finding active milestones for type={} with ordersRequired <= {}",
                milestoneType, maxOrders);
        List<MilestoneConfigJpaEntity> entities = jpaRepository
                .findActiveByMilestoneTypeAndOrdersRequiredLessThanOrEqual(
                        milestoneType.name(),
                        maxOrders
                );
        return mapper.toDomainList(entities);
    }
}
