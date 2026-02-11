package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.UserNotificationPreference;
import com.cashbee.infrastructure.persistence.entity.UserNotificationPreferenceJpaEntity;

/**
 * Mapper between UserNotificationPreference domain model and UserNotificationPreferenceJpaEntity.
 *
 * This mapper converts between domain objects (business logic)
 * and JPA entities (database persistence).
 *
 * @author CashBee Team
 */
public class UserNotificationPreferenceMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    public static UserNotificationPreference toDomain(UserNotificationPreferenceJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserNotificationPreference.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                // Email preferences
                .emailPaymentInvoice(entity.getEmailPaymentInvoice())
                .emailCashbackConfirmed(entity.getEmailCashbackConfirmed())
                .emailOrderMatched(entity.getEmailOrderMatched())
                .emailPromotional(entity.getEmailPromotional())
                // Push notification preferences
                .pushPaymentInvoice(entity.getPushPaymentInvoice())
                .pushCashbackConfirmed(entity.getPushCashbackConfirmed())
                .pushOrderMatched(entity.getPushOrderMatched())
                .pushPromotional(entity.getPushPromotional())
                // Timestamps
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    public static UserNotificationPreferenceJpaEntity toEntity(UserNotificationPreference domain) {
        if (domain == null) {
            return null;
        }

        return UserNotificationPreferenceJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                // Email preferences
                .emailPaymentInvoice(domain.getEmailPaymentInvoice())
                .emailCashbackConfirmed(domain.getEmailCashbackConfirmed())
                .emailOrderMatched(domain.getEmailOrderMatched())
                .emailPromotional(domain.getEmailPromotional())
                // Push notification preferences
                .pushPaymentInvoice(domain.getPushPaymentInvoice())
                .pushCashbackConfirmed(domain.getPushCashbackConfirmed())
                .pushOrderMatched(domain.getPushOrderMatched())
                .pushPromotional(domain.getPushPromotional())
                // Timestamps
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    // Private constructor to prevent instantiation
    private UserNotificationPreferenceMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
}
