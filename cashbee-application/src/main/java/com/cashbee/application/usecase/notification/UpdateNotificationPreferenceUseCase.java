package com.cashbee.application.usecase.notification;

import com.cashbee.application.dto.notification.NotificationPreferenceResponse;
import com.cashbee.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.cashbee.domain.model.UserNotificationPreference;
import com.cashbee.domain.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for updating user notification preferences.
 *
 * Features:
 * - Partial updates (only provided fields are updated)
 * - Create new preferences if none exist
 * - Return updated preferences
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateNotificationPreferenceUseCase {

    private final UserNotificationPreferenceRepository preferenceRepository;

    /**
     * Update notification preferences for a user.
     * Only provided fields will be updated.
     *
     * @param command Update command with fields to change
     * @return Updated preferences
     */
    @Transactional
    public NotificationPreferenceResponse execute(UpdateNotificationPreferenceCommand command) {
        log.info("Updating notification preferences for user: {}", command.getUserId());

        // Check if any update is provided
        if (!command.hasAnyUpdate()) {
            log.debug("No preference updates provided, returning current preferences");
            return preferenceRepository.findByUserId(command.getUserId())
                    .map(NotificationPreferenceResponse::fromDomain)
                    .orElseGet(() -> createAndSaveDefaultPreferences(command.getUserId()));
        }

        // Find existing or create new
        UserNotificationPreference preference = preferenceRepository.findByUserId(command.getUserId())
                .orElseGet(() -> createDefaultPreference(command.getUserId()));

        // Apply updates (only non-null fields)
        applyUpdates(preference, command);

        // Update timestamp
        preference.setUpdatedAt(LocalDateTime.now());

        // Save
        UserNotificationPreference saved = preferenceRepository.save(preference);

        log.info("Notification preferences updated for user: {}", command.getUserId());

        return NotificationPreferenceResponse.fromDomain(saved);
    }

    /**
     * Apply partial updates from command to preference.
     */
    private void applyUpdates(UserNotificationPreference preference, UpdateNotificationPreferenceCommand command) {
        // Email preferences
        if (command.getEmailPaymentInvoice() != null) {
            preference.setEmailPaymentInvoice(command.getEmailPaymentInvoice());
            log.debug("Updated emailPaymentInvoice to {}", command.getEmailPaymentInvoice());
        }
        if (command.getEmailCashbackConfirmed() != null) {
            preference.setEmailCashbackConfirmed(command.getEmailCashbackConfirmed());
        }
        if (command.getEmailOrderMatched() != null) {
            preference.setEmailOrderMatched(command.getEmailOrderMatched());
        }
        if (command.getEmailPromotional() != null) {
            preference.setEmailPromotional(command.getEmailPromotional());
        }

        // Push preferences
        if (command.getPushPaymentInvoice() != null) {
            preference.setPushPaymentInvoice(command.getPushPaymentInvoice());
        }
        if (command.getPushCashbackConfirmed() != null) {
            preference.setPushCashbackConfirmed(command.getPushCashbackConfirmed());
        }
        if (command.getPushOrderMatched() != null) {
            preference.setPushOrderMatched(command.getPushOrderMatched());
        }
        if (command.getPushPromotional() != null) {
            preference.setPushPromotional(command.getPushPromotional());
        }
    }

    /**
     * Create default preference (all enabled).
     */
    private UserNotificationPreference createDefaultPreference(Long userId) {
        log.debug("Creating new preference for user: {}", userId);

        return UserNotificationPreference.builder()
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create and save default preferences, return as response.
     */
    private NotificationPreferenceResponse createAndSaveDefaultPreferences(Long userId) {
        UserNotificationPreference preference = createDefaultPreference(userId);
        UserNotificationPreference saved = preferenceRepository.save(preference);
        return NotificationPreferenceResponse.fromDomain(saved);
    }

    /**
     * Toggle all email preferences on/off.
     *
     * @param userId User ID
     * @param enabled Enable or disable all email notifications
     * @return Updated preferences
     */
    @Transactional
    public NotificationPreferenceResponse toggleAllEmailPreferences(Long userId, boolean enabled) {
        log.info("Toggling all email preferences for user {} to {}", userId, enabled);

        UpdateNotificationPreferenceCommand command = UpdateNotificationPreferenceCommand.builder()
                .userId(userId)
                .emailPaymentInvoice(enabled)
                .emailCashbackConfirmed(enabled)
                .emailOrderMatched(enabled)
                .emailPromotional(enabled)
                .build();

        return execute(command);
    }

    /**
     * Toggle all push preferences on/off.
     *
     * @param userId User ID
     * @param enabled Enable or disable all push notifications
     * @return Updated preferences
     */
    @Transactional
    public NotificationPreferenceResponse toggleAllPushPreferences(Long userId, boolean enabled) {
        log.info("Toggling all push preferences for user {} to {}", userId, enabled);

        UpdateNotificationPreferenceCommand command = UpdateNotificationPreferenceCommand.builder()
                .userId(userId)
                .pushPaymentInvoice(enabled)
                .pushCashbackConfirmed(enabled)
                .pushOrderMatched(enabled)
                .pushPromotional(enabled)
                .build();

        return execute(command);
    }
}
