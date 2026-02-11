package com.cashbee.presentation.controller;

import com.cashbee.application.dto.notification.NotificationPreferenceResponse;
import com.cashbee.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.cashbee.application.usecase.notification.GetNotificationPreferenceUseCase;
import com.cashbee.application.usecase.notification.UpdateNotificationPreferenceUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for User Notification Preference operations.
 *
 * Endpoints:
 * - GET /api/notifications/preferences - Get current user's notification preferences
 * - PUT /api/notifications/preferences - Update notification preferences
 * - PUT /api/notifications/preferences/email - Toggle all email preferences on/off
 * - PUT /api/notifications/preferences/push - Toggle all push preferences on/off
 *
 * Available Preferences:
 * - Email:
 *   - emailPaymentInvoice: Email when payment invoice is generated
 *   - emailCashbackConfirmed: Email when cashback is confirmed
 *   - emailOrderMatched: Email when order is matched with tracking
 *   - emailPromotional: Marketing and promotional emails
 *
 * - Push Notifications:
 *   - pushPaymentInvoice: Push when payment invoice is generated
 *   - pushCashbackConfirmed: Push when cashback is confirmed
 *   - pushOrderMatched: Push when order is matched with tracking
 *   - pushPromotional: Marketing and promotional push notifications
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "User notification preference management")
public class NotificationPreferenceController {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceController.class);

    private final GetNotificationPreferenceUseCase getNotificationPreferenceUseCase;
    private final UpdateNotificationPreferenceUseCase updateNotificationPreferenceUseCase;
    private final SecurityUtils securityUtils;

    /**
     * Get current user's notification preferences.
     *
     * Returns the user's notification preferences. If no preferences
     * have been set, returns default preferences (all enabled).
     *
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/notifications/preferences', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const prefs = response.data;
     * // prefs.emailPaymentInvoice - true/false
     * // prefs.pushCashbackConfirmed - true/false
     * // ... etc
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @return Notification preferences
     */
    @GetMapping
    @Operation(
        summary = "Get notification preferences",
        description = "Get current user's notification preferences. Returns defaults if not set."
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("API: Getting notification preferences for current user");

        Long userId = securityUtils.getCurrentUserId(jwt);

        NotificationPreferenceResponse preferences = getNotificationPreferenceUseCase.execute(userId);

        log.info("API: Retrieved notification preferences for user {}", userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(preferences));
    }

    /**
     * Update notification preferences (partial update).
     *
     * Only provided fields will be updated. If a field is null or not provided,
     * it will retain its current value.
     *
     * Usage (Frontend):
     * <pre>
     * // Update only specific preferences
     * const response = await fetch('/api/notifications/preferences', {
     *   method: 'PUT',
     *   headers: {
     *     'Authorization': `Bearer ${token}`,
     *     'Content-Type': 'application/json'
     *   },
     *   body: JSON.stringify({
     *     emailPaymentInvoice: true,
     *     pushPromotional: false
     *   })
     * });
     * const updatedPrefs = response.data;
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param command Command containing fields to update
     * @return Updated notification preferences
     */
    @PutMapping
    @Operation(
        summary = "Update notification preferences",
        description = "Update specific notification preferences (partial update - only provided fields are changed)"
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {

        log.info("API: Updating notification preferences for current user");

        Long userId = securityUtils.getCurrentUserId(jwt);

        UpdateNotificationPreferenceCommand command = UpdateNotificationPreferenceCommand.builder()
                .userId(userId)
                .emailPaymentInvoice(request.getEmailPaymentInvoice())
                .emailCashbackConfirmed(request.getEmailCashbackConfirmed())
                .emailOrderMatched(request.getEmailOrderMatched())
                .emailPromotional(request.getEmailPromotional())
                .pushPaymentInvoice(request.getPushPaymentInvoice())
                .pushCashbackConfirmed(request.getPushCashbackConfirmed())
                .pushOrderMatched(request.getPushOrderMatched())
                .pushPromotional(request.getPushPromotional())
                .build();

        NotificationPreferenceResponse preferences = updateNotificationPreferenceUseCase.execute(command);

        log.info("API: Updated notification preferences for user {}", userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(preferences, "Preferences updated successfully"));
    }

    /**
     * Toggle all email preferences on/off.
     *
     * Convenience endpoint to enable or disable all email notifications at once.
     *
     * Usage (Frontend):
     * <pre>
     * // Disable all email notifications
     * const response = await fetch('/api/notifications/preferences/email?enabled=false', {
     *   method: 'PUT',
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param enabled true to enable all, false to disable all
     * @return Updated notification preferences
     */
    @PutMapping("/email")
    @Operation(
        summary = "Toggle all email preferences",
        description = "Enable or disable all email notification preferences at once"
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> toggleAllEmailPreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam boolean enabled) {

        log.info("API: Toggling all email preferences to {} for current user", enabled);

        Long userId = securityUtils.getCurrentUserId(jwt);

        NotificationPreferenceResponse preferences =
                updateNotificationPreferenceUseCase.toggleAllEmailPreferences(userId, enabled);

        String message = enabled ? "All email notifications enabled" : "All email notifications disabled";

        log.info("API: {} for user {}", message, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(preferences, message));
    }

    /**
     * Toggle all push preferences on/off.
     *
     * Convenience endpoint to enable or disable all push notifications at once.
     *
     * Usage (Frontend):
     * <pre>
     * // Enable all push notifications
     * const response = await fetch('/api/notifications/preferences/push?enabled=true', {
     *   method: 'PUT',
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param enabled true to enable all, false to disable all
     * @return Updated notification preferences
     */
    @PutMapping("/push")
    @Operation(
        summary = "Toggle all push preferences",
        description = "Enable or disable all push notification preferences at once"
    )
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> toggleAllPushPreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam boolean enabled) {

        log.info("API: Toggling all push preferences to {} for current user", enabled);

        Long userId = securityUtils.getCurrentUserId(jwt);

        NotificationPreferenceResponse preferences =
                updateNotificationPreferenceUseCase.toggleAllPushPreferences(userId, enabled);

        String message = enabled ? "All push notifications enabled" : "All push notifications disabled";

        log.info("API: {} for user {}", message, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(preferences, message));
    }

    /**
     * Request DTO for updating notification preferences.
     * All fields are optional - only provided fields will be updated.
     */
    @lombok.Data
    public static class UpdateNotificationPreferenceRequest {
        // Email preferences
        private Boolean emailPaymentInvoice;
        private Boolean emailCashbackConfirmed;
        private Boolean emailOrderMatched;
        private Boolean emailPromotional;

        // Push preferences
        private Boolean pushPaymentInvoice;
        private Boolean pushCashbackConfirmed;
        private Boolean pushOrderMatched;
        private Boolean pushPromotional;
    }
}
