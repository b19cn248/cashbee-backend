package com.cashbee.application.dto.notification;

import com.cashbee.domain.model.UserNotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user notification preferences.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private Long id;
    private Long userId;

    // Email preferences
    private Boolean emailPaymentInvoice;
    private Boolean emailCashbackConfirmed;
    private Boolean emailOrderMatched;
    private Boolean emailPromotional;

    // Push notification preferences
    private Boolean pushPaymentInvoice;
    private Boolean pushCashbackConfirmed;
    private Boolean pushOrderMatched;
    private Boolean pushPromotional;

    private LocalDateTime updatedAt;

    /**
     * Create response from domain model.
     */
    public static NotificationPreferenceResponse fromDomain(UserNotificationPreference preference) {
        if (preference == null) {
            return null;
        }

        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .emailPaymentInvoice(preference.getEmailPaymentInvoice())
                .emailCashbackConfirmed(preference.getEmailCashbackConfirmed())
                .emailOrderMatched(preference.getEmailOrderMatched())
                .emailPromotional(preference.getEmailPromotional())
                .pushPaymentInvoice(preference.getPushPaymentInvoice())
                .pushCashbackConfirmed(preference.getPushCashbackConfirmed())
                .pushOrderMatched(preference.getPushOrderMatched())
                .pushPromotional(preference.getPushPromotional())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
