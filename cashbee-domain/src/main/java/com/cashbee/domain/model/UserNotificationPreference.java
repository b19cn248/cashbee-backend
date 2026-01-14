package com.cashbee.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * UserNotificationPreference Domain Model.
 *
 * Stores user preferences for email and push notifications.
 * Each user has one preference record (one-to-one with User).
 *
 * Notification Types:
 * - Payment Invoice: When batch payment is completed
 * - Cashback Confirmed: When order cashback is confirmed
 * - Order Matched: When order is matched with affiliate click
 * - Promotional: Marketing and promotional content
 *
 * Default Behavior:
 * - If no preference record exists, system uses default values
 * - Email for payment invoice: ON by default
 * - Push notifications: ON by default
 * - Promotional: OFF by default
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "userId"})
public class UserNotificationPreference {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Reference to user (one-to-one relationship).
     */
    private Long userId;

    // ===== Email Preferences =====

    /**
     * Receive email for payment invoice.
     * Default: true (important financial notification)
     */
    @Builder.Default
    private Boolean emailPaymentInvoice = true;

    /**
     * Receive email when cashback is confirmed.
     * Default: true (important notification)
     */
    @Builder.Default
    private Boolean emailCashbackConfirmed = true;

    /**
     * Receive email when order is matched.
     * Default: false (frequent, may be annoying)
     */
    @Builder.Default
    private Boolean emailOrderMatched = false;

    /**
     * Receive promotional emails.
     * Default: false (requires explicit opt-in)
     */
    @Builder.Default
    private Boolean emailPromotional = false;

    // ===== Push Notification Preferences =====

    /**
     * Receive push notification for payment invoice.
     * Default: true (important financial notification)
     */
    @Builder.Default
    private Boolean pushPaymentInvoice = true;

    /**
     * Receive push notification when cashback is confirmed.
     * Default: true
     */
    @Builder.Default
    private Boolean pushCashbackConfirmed = true;

    /**
     * Receive push notification when order is matched.
     * Default: true (real-time feedback is useful)
     */
    @Builder.Default
    private Boolean pushOrderMatched = true;

    /**
     * Receive promotional push notifications.
     * Default: true (less intrusive than email)
     */
    @Builder.Default
    private Boolean pushPromotional = true;

    // ===== Timestamps =====

    /**
     * Timestamp when preference was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when preference was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }
    }

    /**
     * Create default preferences for a user.
     *
     * @param userId User ID
     * @return New preference with default values
     */
    public static UserNotificationPreference createDefault(Long userId) {
        return UserNotificationPreference.builder()
                .userId(userId)
                .emailPaymentInvoice(true)
                .emailCashbackConfirmed(true)
                .emailOrderMatched(false)
                .emailPromotional(false)
                .pushPaymentInvoice(true)
                .pushCashbackConfirmed(true)
                .pushOrderMatched(true)
                .pushPromotional(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check if email should be sent for payment invoice.
     */
    public boolean shouldEmailPaymentInvoice() {
        return Boolean.TRUE.equals(this.emailPaymentInvoice);
    }

    /**
     * Check if email should be sent for cashback confirmed.
     */
    public boolean shouldEmailCashbackConfirmed() {
        return Boolean.TRUE.equals(this.emailCashbackConfirmed);
    }

    /**
     * Check if email should be sent for order matched.
     */
    public boolean shouldEmailOrderMatched() {
        return Boolean.TRUE.equals(this.emailOrderMatched);
    }

    /**
     * Check if promotional email should be sent.
     */
    public boolean shouldEmailPromotional() {
        return Boolean.TRUE.equals(this.emailPromotional);
    }

    /**
     * Check if push notification should be sent for payment invoice.
     */
    public boolean shouldPushPaymentInvoice() {
        return Boolean.TRUE.equals(this.pushPaymentInvoice);
    }

    /**
     * Check if push notification should be sent for cashback confirmed.
     */
    public boolean shouldPushCashbackConfirmed() {
        return Boolean.TRUE.equals(this.pushCashbackConfirmed);
    }

    /**
     * Check if push notification should be sent for order matched.
     */
    public boolean shouldPushOrderMatched() {
        return Boolean.TRUE.equals(this.pushOrderMatched);
    }

    /**
     * Check if promotional push notification should be sent.
     */
    public boolean shouldPushPromotional() {
        return Boolean.TRUE.equals(this.pushPromotional);
    }

    /**
     * Enable all email notifications.
     */
    public void enableAllEmails() {
        this.emailPaymentInvoice = true;
        this.emailCashbackConfirmed = true;
        this.emailOrderMatched = true;
        this.emailPromotional = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Disable all email notifications.
     */
    public void disableAllEmails() {
        this.emailPaymentInvoice = false;
        this.emailCashbackConfirmed = false;
        this.emailOrderMatched = false;
        this.emailPromotional = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enable all push notifications.
     */
    public void enableAllPush() {
        this.pushPaymentInvoice = true;
        this.pushCashbackConfirmed = true;
        this.pushOrderMatched = true;
        this.pushPromotional = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Disable all push notifications.
     */
    public void disableAllPush() {
        this.pushPaymentInvoice = false;
        this.pushCashbackConfirmed = false;
        this.pushOrderMatched = false;
        this.pushPromotional = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update timestamp when any preference changes.
     */
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
