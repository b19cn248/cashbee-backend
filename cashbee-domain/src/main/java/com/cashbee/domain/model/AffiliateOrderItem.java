package com.cashbee.domain.model;

import com.cashbee.domain.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AffiliateOrderItem Domain Model.
 * Pure business object without any infrastructure concerns.
 *
 * Represents individual items within an affiliate order.
 * One order can have multiple items.
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
@ToString
@EqualsAndHashCode(of = {"id"})
public class AffiliateOrderItem {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Order ID (foreign key to affiliate_order).
     */
    private Long orderId;

    /**
     * Platform item ID.
     */
    private String itemId;

    /**
     * Item name/title.
     */
    private String itemName;

    /**
     * Quantity ordered.
     */
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Actual item price (total for this item).
     */
    private BigDecimal actualAmount;

    /**
     * Commission earned from this specific item.
     */
    private BigDecimal itemCommission;

    /**
     * Shop ID on the platform.
     */
    private String shopId;

    /**
     * Shop name.
     */
    private String shopName;

    /**
     * Category level 1 (e.g., "Electronics").
     */
    private String categoryLv1;

    /**
     * Category level 2 (e.g., "Mobile Phones").
     */
    private String categoryLv2;

    /**
     * Category level 3 (e.g., "Smartphones").
     */
    private String categoryLv3;

    /**
     * Product image URL.
     */
    private String imgUrl;

    /**
     * Brand commission rate (percentage).
     */
    private BigDecimal brandCommissionRate;

    /**
     * Platform commission rate (percentage).
     */
    private BigDecimal platformCommissionRate;

    /**
     * Item status (PENDING, APPROVED, PAID, CANCELLED).
     * Each item can have different status within the same order.
     */
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Timestamp when entity was created.
     */
    private LocalDateTime createdAt;

    // ===== Business Logic Methods =====

    /**
     * Calculate total value of this item (actualAmount * quantity).
     * In most cases, actualAmount already includes quantity,
     * but this method provides flexibility.
     *
     * @return total value
     */
    public BigDecimal calculateTotalValue() {
        if (this.actualAmount == null) {
            return BigDecimal.ZERO;
        }

        if (this.quantity == null || this.quantity <= 0) {
            return this.actualAmount;
        }

        return this.actualAmount.multiply(BigDecimal.valueOf(this.quantity));
    }

    /**
     * Check if this item has shop information.
     */
    public boolean hasShopInfo() {
        return this.shopId != null && !this.shopId.isBlank()
            && this.shopName != null && !this.shopName.isBlank();
    }

    /**
     * Check if this item has category information.
     */
    public boolean hasCategoryInfo() {
        return (this.categoryLv1 != null && !this.categoryLv1.isBlank())
            || (this.categoryLv2 != null && !this.categoryLv2.isBlank())
            || (this.categoryLv3 != null && !this.categoryLv3.isBlank());
    }

    /**
     * Check if this item has commission information.
     */
    public boolean hasCommissionInfo() {
        return this.itemCommission != null
            && this.itemCommission.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this is a new item (not persisted yet).
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Check if item is completed (APPROVED or PAID).
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.APPROVED || this.status == OrderStatus.PAID;
    }

    /**
     * Check if item is pending.
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * Check if item is cancelled.
     */
    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    /**
     * Validate item data.
     * Throws exception if validation fails.
     */
    public void validate() {
        if (this.itemId == null || this.itemId.isBlank()) {
            throw new IllegalStateException("Item ID is required");
        }

        if (this.itemName == null || this.itemName.isBlank()) {
            throw new IllegalStateException("Item name is required");
        }

        if (this.quantity == null || this.quantity <= 0) {
            throw new IllegalStateException("Quantity must be greater than zero");
        }
    }
}
