package com.cashbee.domain.model;

import com.cashbee.domain.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AffiliateOrder Domain Model.
 * Pure business object without any infrastructure concerns.
 *
 * Represents an order from affiliate platforms (Shopee, Lazada, etc.)
 * that generates commission and cashback for users.
 *
 * This model is a pure POJO with NO JPA annotations.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"deletedAt"})
@EqualsAndHashCode(of = {"id"})
public class AffiliateOrder {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Platform ID (foreign key to affiliate_platform).
     */
    private Long platformId;

    /**
     * User ID (foreign key to user).
     */
    private Long userId;

    /**
     * Affiliate click ID from platform.
     * Used to track which user generated this order.
     */
    private String clickId;

    /**
     * Platform order ID (e.g., Shopee order_sn).
     * Must be unique across the system.
     */
    private String orderId;

    /**
     * Order status in the cashback lifecycle.
     */
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    /**
     * Product name (summary, may be truncated).
     */
    private String productName;

    /**
     * Total product price.
     */
    private BigDecimal productPrice;

    /**
     * Total commission amount from platform.
     * This is what the platform pays us.
     */
    private BigDecimal commissionAmount;

    /**
     * Currency code (VND, USD, etc.).
     */
    @Builder.Default
    private String currency = "VND";

    /**
     * When user placed the order.
     */
    private LocalDateTime orderTime;

    /**
     * When platform confirmed the order (status → APPROVED).
     */
    private LocalDateTime confirmTime;

    /**
     * When order was actually paid (status → PAID).
     */
    private LocalDateTime paidTime;

    /**
     * Source of this order data.
     * IMPORT = from file upload
     * API = from platform API
     */
    @Builder.Default
    private String source = "IMPORT";

    /**
     * Import batch ID if this order came from file import.
     * Null if from API.
     */
    private Long importBatchId;

    /**
     * Whether this order was matched using fallback (context-based) matching.
     *
     * TRUE = Order matched by context (itemId + shopId + time window) because Sub_id1 was missing
     * FALSE/NULL = Order matched by tracking code (Sub_id1) - normal flow
     *
     * This flag helps track orders that may need review as they were matched
     * by heuristics rather than explicit tracking codes.
     */
    @Builder.Default
    private Boolean fallbackMatch = false;

    /**
     * Timestamp when entity was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when entity was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Soft delete timestamp.
     * Null if not deleted.
     */
    private LocalDateTime deletedAt;

    // ===== Business Logic Methods =====

    /**
     * Check if order is pending.
     */
    public boolean isPending() {
        return this.orderStatus == OrderStatus.PENDING;
    }

    /**
     * Check if order is approved.
     */
    public boolean isApproved() {
        return this.orderStatus == OrderStatus.APPROVED;
    }

    /**
     * Check if order is paid.
     */
    public boolean isPaid() {
        return this.orderStatus == OrderStatus.PAID;
    }

    /**
     * Check if order is cancelled.
     */
    public boolean isCancelled() {
        return this.orderStatus == OrderStatus.CANCELLED;
    }

    /**
     * Check if order is rejected.
     */
    public boolean isRejected() {
        return this.orderStatus == OrderStatus.REJECTED;
    }

    /**
     * Check if order can receive cashback.
     * Only APPROVED and PAID orders are eligible.
     */
    public boolean canReceiveCashback() {
        return this.orderStatus == OrderStatus.APPROVED
            || this.orderStatus == OrderStatus.PAID;
    }

    /**
     * Approve the order.
     * Sets status to APPROVED and records confirm time.
     */
    public void approve() {
        this.orderStatus = OrderStatus.APPROVED;
        this.confirmTime = LocalDateTime.now();
    }

    /**
     * Mark order as paid.
     * Sets status to PAID and records paid time.
     */
    public void markAsPaid() {
        this.orderStatus = OrderStatus.PAID;
        this.paidTime = LocalDateTime.now();
    }

    /**
     * Cancel the order.
     */
    public void cancel() {
        this.orderStatus = OrderStatus.CANCELLED;
    }

    /**
     * Reject the order.
     */
    public void reject() {
        this.orderStatus = OrderStatus.REJECTED;
    }

    /**
     * Check if order is from file import.
     */
    public boolean isFromImport() {
        return "IMPORT".equalsIgnoreCase(this.source);
    }

    /**
     * Check if order is from API.
     */
    public boolean isFromApi() {
        return "API".equalsIgnoreCase(this.source);
    }

    /**
     * Check if this is a new order (not persisted yet).
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Check if order is deleted (soft delete).
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Soft delete order.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restore deleted order.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Validate order data.
     * Throws exception if validation fails.
     */
    public void validate() {
        if (this.platformId == null) {
            throw new IllegalStateException("Platform ID is required");
        }

        if (this.userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (this.orderId == null || this.orderId.isBlank()) {
            throw new IllegalStateException("Order ID is required");
        }

        if (this.commissionAmount == null) {
            throw new IllegalStateException("Commission amount is required");
        }

        if (this.commissionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Commission amount cannot be negative");
        }

        if (this.orderStatus == null) {
            throw new IllegalStateException("Order status is required");
        }
    }
}
