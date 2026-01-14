package com.cashbee.application.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command DTO for updating user notification preferences.
 *
 * All fields are optional - only provided fields will be updated.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferenceCommand {

    /**
     * User ID (required).
     */
    private Long userId;

    // Email preferences (optional - null means no change)
    private Boolean emailPaymentInvoice;
    private Boolean emailCashbackConfirmed;
    private Boolean emailOrderMatched;
    private Boolean emailPromotional;

    // Push notification preferences (optional - null means no change)
    private Boolean pushPaymentInvoice;
    private Boolean pushCashbackConfirmed;
    private Boolean pushOrderMatched;
    private Boolean pushPromotional;

    /**
     * Check if any email preference is being updated.
     */
    public boolean hasEmailPreferenceUpdate() {
        return emailPaymentInvoice != null ||
               emailCashbackConfirmed != null ||
               emailOrderMatched != null ||
               emailPromotional != null;
    }

    /**
     * Check if any push preference is being updated.
     */
    public boolean hasPushPreferenceUpdate() {
        return pushPaymentInvoice != null ||
               pushCashbackConfirmed != null ||
               pushOrderMatched != null ||
               pushPromotional != null;
    }

    /**
     * Check if any preference is being updated.
     */
    public boolean hasAnyUpdate() {
        return hasEmailPreferenceUpdate() || hasPushPreferenceUpdate();
    }
}
