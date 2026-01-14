package com.cashbee.domain.model;

import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Promotion Voucher Domain Model.
 * Represents voucher/promotion codes scraped from Telegram channels.
 *
 * Key features:
 * - Stores discount codes with various discount types (fixed amount, percentage)
 * - Tracks original Shopee links and converted affiliate links
 * - Supports view/click tracking for analytics
 * - Categorizes vouchers for easy filtering
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
public class PromotionVoucher {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Voucher code (e.g., AFFZOSI0, SPVIP20).
     * Can be null if voucher only has a save link.
     */
    private String code;

    /**
     * Title/summary of the voucher.
     * Example: "Giảm 30k từ 99k"
     */
    private String title;

    /**
     * Detailed description of the voucher.
     */
    private String description;

    /**
     * Type of discount: FIXED_AMOUNT or PERCENTAGE.
     */
    private DiscountType discountType;

    /**
     * Discount value.
     * For FIXED_AMOUNT: actual amount in VND (e.g., 30000)
     * For PERCENTAGE: percentage value (e.g., 20 for 20%)
     */
    private BigDecimal discountValue;

    /**
     * Maximum discount amount for percentage vouchers.
     * Only applicable when discountType is PERCENTAGE.
     */
    private BigDecimal maxDiscount;

    /**
     * Minimum order value required to use this voucher.
     */
    private BigDecimal minOrderValue;

    /**
     * Original Shopee link from Telegram message.
     */
    private String originalLink;

    /**
     * Affiliate link generated from original link.
     * Auto-generated when voucher is created.
     */
    private String affiliateLink;

    /**
     * Link to save/claim the voucher.
     */
    private String voucherSaveLink;

    /**
     * Category of the voucher.
     */
    private VoucherCategory category;

    /**
     * Platform (SHOPEE, LAZADA, TIKTOK).
     */
    @Builder.Default
    private String platform = "SHOPEE";

    /**
     * Start time when voucher becomes valid.
     */
    private LocalDateTime validFrom;

    /**
     * End time when voucher expires.
     */
    private LocalDateTime validUntil;

    /**
     * Time slot when voucher is valid (e.g., "9H-11H").
     */
    private String validTimeSlot;

    /**
     * Current status of the voucher.
     */
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    /**
     * Number of times this voucher has been viewed.
     */
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Number of times this voucher link has been clicked.
     */
    @Builder.Default
    private Integer clickCount = 0;

    /**
     * Timestamp when voucher was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when voucher was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Check if voucher is active.
     *
     * @return true if voucher status is ACTIVE
     */
    public boolean isActive() {
        return VoucherStatus.ACTIVE.equals(this.status);
    }

    /**
     * Check if voucher is expired.
     *
     * @return true if voucher status is EXPIRED or validUntil has passed
     */
    public boolean isExpired() {
        if (VoucherStatus.EXPIRED.equals(this.status)) {
            return true;
        }
        if (this.validUntil != null && LocalDateTime.now().isAfter(this.validUntil)) {
            return true;
        }
        return false;
    }

    /**
     * Check if this is a percentage discount voucher.
     *
     * @return true if discount type is PERCENTAGE
     */
    public boolean isPercentageDiscount() {
        return DiscountType.PERCENTAGE.equals(this.discountType);
    }

    /**
     * Check if this is a fixed amount discount voucher.
     *
     * @return true if discount type is FIXED_AMOUNT
     */
    public boolean isFixedAmountDiscount() {
        return DiscountType.FIXED_AMOUNT.equals(this.discountType);
    }

    /**
     * Increment view count.
     */
    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        this.viewCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment click count.
     */
    public void incrementClickCount() {
        if (this.clickCount == null) {
            this.clickCount = 0;
        }
        this.clickCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark voucher as active.
     */
    public void activate() {
        this.status = VoucherStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark voucher as inactive.
     */
    public void deactivate() {
        this.status = VoucherStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark voucher as expired.
     */
    public void expire() {
        this.status = VoucherStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update status.
     *
     * @param newStatus new status to set
     */
    public void updateStatus(VoucherStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this is a new voucher (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Validate voucher data.
     * Throws exception if validation fails.
     */
    public void validate() {
        if (this.title == null || this.title.isBlank()) {
            throw new IllegalStateException("Title is required");
        }
        if (this.discountType == null) {
            throw new IllegalStateException("Discount type is required");
        }
    }

    /**
     * Get display discount text.
     * Example: "30.000đ" or "20%"
     *
     * @return formatted discount string
     */
    public String getDisplayDiscount() {
        if (discountValue == null) {
            return "";
        }
        if (isPercentageDiscount()) {
            return discountValue.stripTrailingZeros().toPlainString() + "%";
        } else {
            return String.format("%,.0fđ", discountValue);
        }
    }
}
