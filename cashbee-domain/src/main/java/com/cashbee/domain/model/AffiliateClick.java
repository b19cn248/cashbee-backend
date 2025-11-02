package com.cashbee.domain.model;

import com.cashbee.domain.enums.ClickStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AffiliateClick Domain Model.
 * Represents a tracking link created when user converts a Shopee URL.
 *
 * This is the KEY entity for tracking user → order mapping!
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class AffiliateClick {

    /**
     * Internal database ID.
     */
    private Long id;

    /**
     * User who created this tracking link.
     */
    private Long userId;

    /**
     * Platform (Shopee, Lazada, etc.)
     */
    private Long platformId;

    /**
     * Shop ID from Shopee URL.
     */
    private String shopId;

    /**
     * Item ID from Shopee URL.
     */
    private String itemId;

    /**
     * Product name parsed from URL.
     */
    private String productName;

    /**
     * Unique tracking code embedded in affiliate link.
     * Format: CB{userId}_{clickId}_{timestamp}
     * Example: CB1_100_20251101160530
     */
    private String trackingCode;

    /**
     * Full affiliate tracking URL generated.
     */
    private String trackingUrl;

    /**
     * Original Shopee URL user pasted.
     */
    private String originalUrl;

    /**
     * When this tracking link was created.
     */
    private LocalDateTime createdAt;

    /**
     * When user actually clicked this link (if clicked).
     */
    private LocalDateTime clickedAt;

    /**
     * Whether this click resulted in an order.
     */
    @Builder.Default
    private Boolean orderMatched = false;

    /**
     * Order ID that matched this click (from CSV import).
     */
    private Long matchedOrderId;

    /**
     * Current status of this click.
     */
    @Builder.Default
    private ClickStatus status = ClickStatus.CREATED;

    // ===== Business Logic Methods =====

    /**
     * Mark this click as clicked (user redirected to Shopee).
     */
    public void markAsClicked() {
        this.status = ClickStatus.CLICKED;
        this.clickedAt = LocalDateTime.now();
    }

    /**
     * Match this click with an order from CSV import.
     */
    public void matchWithOrder(Long orderId) {
        this.orderMatched = true;
        this.matchedOrderId = orderId;
        this.status = ClickStatus.MATCHED;
    }

    /**
     * Mark this click as expired (no order after X days).
     */
    public void markAsExpired() {
        this.status = ClickStatus.EXPIRED;
    }

    /**
     * Check if this click is still trackable.
     */
    public boolean isTrackable() {
        return this.status == ClickStatus.CREATED
            || this.status == ClickStatus.CLICKED;
    }

    /**
     * Check if this click resulted in a purchase.
     */
    public boolean hasOrder() {
        return this.orderMatched && this.matchedOrderId != null;
    }

    /**
     * Calculate age of this click in days.
     */
    public long getAgeInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(
            this.createdAt,
            LocalDateTime.now()
        );
    }

    /**
     * Check if click has expired (older than X days).
     * Shopee typically tracks 7-30 days.
     */
    public boolean isExpired(int maxDays) {
        return getAgeInDays() > maxDays;
    }

    /**
     * Validate click data.
     */
    public void validate() {
        if (this.userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (this.platformId == null) {
            throw new IllegalStateException("Platform ID is required");
        }

        if (this.trackingCode == null || this.trackingCode.isBlank()) {
            throw new IllegalStateException("Tracking code is required");
        }

        if (this.trackingUrl == null || this.trackingUrl.isBlank()) {
            throw new IllegalStateException("Tracking URL is required");
        }
    }

    /**
     * Check if this is a new click (not persisted yet).
     */
    public boolean isNew() {
        return this.id == null;
    }
}
