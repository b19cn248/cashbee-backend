package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.infrastructure.persistence.entity.ReferralRewardJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.ReferralRewardMapper;
import com.cashbee.infrastructure.persistence.repository.ReferralRewardJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing ReferralRewardRepository domain interface using JPA.
 *
 * <p>This is the "Adapter" in Hexagonal Architecture (Ports & Adapters pattern).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferralRewardRepositoryAdapter implements ReferralRewardRepository {

    private final ReferralRewardJpaRepository jpaRepository;
    private final ReferralRewardMapper mapper;

    @Override
    @Transactional
    public ReferralReward save(ReferralReward reward) {
        log.debug("Saving referral reward: userId={}, type={}, milestone={}",
                reward.getUserId(), reward.getRewardType(), reward.getMilestone());

        ReferralRewardJpaEntity entity;

        if (reward.isNew()) {
            entity = mapper.toEntity(reward);
        } else {
            entity = jpaRepository.findById(reward.getId())
                    .orElseGet(() -> mapper.toEntity(reward));
            mapper.updateEntityFromDomain(reward, entity);
        }

        ReferralRewardJpaEntity saved = jpaRepository.save(entity);
        ReferralReward result = mapper.toDomain(saved);

        log.debug("Referral reward saved: id={}", result.getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferralReward> findById(Long id) {
        log.debug("Finding referral reward by id: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReward> findByUserId(Long userId) {
        log.debug("Finding referral rewards by userId: {}", userId);
        List<ReferralRewardJpaEntity> entities = jpaRepository.findByUserId(userId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReward> findByUserIdAndRewardType(Long userId, ReferralRewardType rewardType) {
        log.debug("Finding referral rewards by userId={} and type={}", userId, rewardType);
        List<ReferralRewardJpaEntity> entities =
                jpaRepository.findByUserIdAndRewardType(userId, rewardType.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferralReward> findByUserIdAndMilestone(Long userId, Integer milestone) {
        log.debug("Finding referral reward by userId={} and milestone={}", userId, milestone);
        return jpaRepository.findByUserIdAndMilestone(userId, milestone)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndMilestone(Long userId, Integer milestone) {
        log.debug("Checking if referral reward exists: userId={}, milestone={}", userId, milestone);
        return jpaRepository.existsByUserIdAndMilestone(userId, milestone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReward> findByStatus(ReferralRewardStatus status) {
        log.debug("Finding referral rewards by status: {}", status);
        List<ReferralRewardJpaEntity> entities = jpaRepository.findByStatus(status.name());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReward> findByReferrerId(Long referrerId) {
        log.debug("Finding referral rewards by referrerId: {}", referrerId);
        List<ReferralRewardJpaEntity> entities = jpaRepository.findByReferrerId(referrerId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        log.debug("Counting referral rewards by userId: {}", userId);
        return jpaRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByReferrerId(Long referrerId) {
        log.debug("Counting referral rewards by referrerId: {}", referrerId);
        return jpaRepository.countByReferrerId(referrerId);
    }
}
