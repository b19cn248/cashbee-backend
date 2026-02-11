package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.UserNotificationPreference;
import com.cashbee.domain.repository.UserNotificationPreferenceRepository;
import com.cashbee.infrastructure.persistence.entity.UserNotificationPreferenceJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.UserNotificationPreferenceMapper;
import com.cashbee.infrastructure.persistence.repository.UserNotificationPreferenceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementation of UserNotificationPreferenceRepository.
 *
 * This adapter bridges the domain layer with the infrastructure layer,
 * converting between domain models and JPA entities.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferenceRepositoryAdapter implements UserNotificationPreferenceRepository {

    private final UserNotificationPreferenceJpaRepository jpaRepository;

    @Override
    public UserNotificationPreference save(UserNotificationPreference preference) {
        log.debug("Saving notification preference for user: {}", preference.getUserId());
        UserNotificationPreferenceJpaEntity entity = UserNotificationPreferenceMapper.toEntity(preference);
        UserNotificationPreferenceJpaEntity savedEntity = jpaRepository.save(entity);
        return UserNotificationPreferenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserNotificationPreference> findById(Long id) {
        return jpaRepository.findById(id)
                .map(UserNotificationPreferenceMapper::toDomain);
    }

    @Override
    public Optional<UserNotificationPreference> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .map(UserNotificationPreferenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        log.debug("Deleting notification preference for user: {}", userId);
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public List<Long> findUserIdsWithEmailPaymentInvoiceEnabled(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findUserIdsWithEmailPaymentInvoiceEnabled(userIds);
    }

    @Override
    public List<Long> findUserIdsWithEmailCashbackConfirmedEnabled(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findUserIdsWithEmailCashbackConfirmedEnabled(userIds);
    }

    @Override
    public List<Long> findUserIdsWithEmailPromotionalEnabled(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findUserIdsWithEmailPromotionalEnabled(userIds);
    }
}
