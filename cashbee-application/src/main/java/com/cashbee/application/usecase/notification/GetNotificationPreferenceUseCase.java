package com.cashbee.application.usecase.notification;

import com.cashbee.application.dto.notification.NotificationPreferenceResponse;
import com.cashbee.domain.model.UserNotificationPreference;
import com.cashbee.domain.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for retrieving user notification preferences.
 *
 * Features:
 * - Get current notification preferences
 * - Return default preferences if none set
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetNotificationPreferenceUseCase {

    private final UserNotificationPreferenceRepository preferenceRepository;

    /**
     * Get notification preferences for a user.
     * If no preferences exist, returns default preferences (all enabled).
     *
     * @param userId User ID
     * @return Notification preferences
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse execute(Long userId) {
        log.debug("Getting notification preferences for user: {}", userId);

        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreferenceResponse::fromDomain)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Create default preferences response (all enabled).
     */
    private NotificationPreferenceResponse createDefaultPreferences(Long userId) {
        log.debug("No preferences found for user {}, returning defaults", userId);

        return NotificationPreferenceResponse.builder()
                .userId(userId)
                // Email preferences - all enabled by default
                .emailPaymentInvoice(true)
                .emailCashbackConfirmed(true)
                .emailOrderMatched(true)
                .emailPromotional(true)
                // Push preferences - all enabled by default
                .pushPaymentInvoice(true)
                .pushCashbackConfirmed(true)
                .pushOrderMatched(true)
                .pushPromotional(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check if a specific preference is enabled for a user.
     *
     * @param userId User ID
     * @param preferenceType Type of preference to check
     * @return true if enabled (or default if not set)
     */
    @Transactional(readOnly = true)
    public boolean isPreferenceEnabled(Long userId, PreferenceType preferenceType) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> getPreferenceValue(pref, preferenceType))
                .orElse(true); // Default to enabled
    }

    private Boolean getPreferenceValue(UserNotificationPreference pref, PreferenceType type) {
        Boolean value = switch (type) {
            case EMAIL_PAYMENT_INVOICE -> pref.getEmailPaymentInvoice();
            case EMAIL_CASHBACK_CONFIRMED -> pref.getEmailCashbackConfirmed();
            case EMAIL_ORDER_MATCHED -> pref.getEmailOrderMatched();
            case EMAIL_PROMOTIONAL -> pref.getEmailPromotional();
            case PUSH_PAYMENT_INVOICE -> pref.getPushPaymentInvoice();
            case PUSH_CASHBACK_CONFIRMED -> pref.getPushCashbackConfirmed();
            case PUSH_ORDER_MATCHED -> pref.getPushOrderMatched();
            case PUSH_PROMOTIONAL -> pref.getPushPromotional();
        };
        return value != null ? value : true; // Default to enabled if null
    }

    /**
     * Enum for preference types.
     */
    public enum PreferenceType {
        EMAIL_PAYMENT_INVOICE,
        EMAIL_CASHBACK_CONFIRMED,
        EMAIL_ORDER_MATCHED,
        EMAIL_PROMOTIONAL,
        PUSH_PAYMENT_INVOICE,
        PUSH_CASHBACK_CONFIRMED,
        PUSH_ORDER_MATCHED,
        PUSH_PROMOTIONAL
    }
}
