package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity for user_notification_preference table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the UserNotificationPreference domain model.
 *
 * One-to-one relationship with User (user_id is unique).
 *
 * Table: user_notification_preference
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "user_notification_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserNotificationPreferenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to user (one-to-one, unique).
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // ===== Email Preferences =====

    /**
     * Receive email for payment invoice.
     */
    @Column(name = "email_payment_invoice", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean emailPaymentInvoice = true;

    /**
     * Receive email when cashback is confirmed.
     */
    @Column(name = "email_cashback_confirmed", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean emailCashbackConfirmed = true;

    /**
     * Receive email when order is matched.
     */
    @Column(name = "email_order_matched", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean emailOrderMatched = false;

    /**
     * Receive promotional emails.
     */
    @Column(name = "email_promotional", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean emailPromotional = false;

    // ===== Push Notification Preferences =====

    /**
     * Receive push notification for payment invoice.
     */
    @Column(name = "push_payment_invoice", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean pushPaymentInvoice = true;

    /**
     * Receive push notification when cashback is confirmed.
     */
    @Column(name = "push_cashback_confirmed", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean pushCashbackConfirmed = true;

    /**
     * Receive push notification when order is matched.
     */
    @Column(name = "push_order_matched", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean pushOrderMatched = true;

    /**
     * Receive promotional push notifications.
     */
    @Column(name = "push_promotional", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean pushPromotional = true;

    // ===== Timestamps =====

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
