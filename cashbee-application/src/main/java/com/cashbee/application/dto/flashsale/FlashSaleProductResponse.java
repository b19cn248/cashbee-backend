package com.cashbee.application.dto.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Flash Sale product.
 *
 * Contains product information from buichung.vn with converted affiliate link.
 * Used by FlashSaleController to return product data to frontend.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleProductResponse {

    /**
     * Product ID (from buichung.vn).
     */
    private String id;

    /**
     * Product image URL.
     */
    private String imageUrl;

    /**
     * Product title/name.
     */
    private String title;

    /**
     * Original Shopee link (from buichung.vn).
     * Usually a shortened link like https://s.shopee.vn/xxx
     */
    private String originalLink;

    /**
     * Affiliate link with CashBee's affiliate ID.
     * Format: https://s.shopee.vn/an_redir?origin_link=...&affiliate_id=...&sub_id=flashsale
     *
     * When users click this link and make a purchase,
     * CashBee earns commission from Shopee.
     */
    private String affiliateLink;

    /**
     * Flash sale price in VND.
     */
    private Long salePrice;

    /**
     * Original price before discount in VND.
     */
    private Long originalPrice;

    /**
     * Discount percentage (e.g., 99 for 99% off).
     */
    private Integer discountPercent;

    /**
     * Stock quantity left.
     */
    private Integer stockLeft;

    /**
     * Time slot identifier (e.g., "16-12 Khung: 00:00").
     */
    private String timeSlot;

    /**
     * Platform name (always "shopee" for now).
     */
    private String platform;

    /**
     * Calculate savings amount.
     *
     * @return Amount saved (originalPrice - salePrice)
     */
    public Long getSavingsAmount() {
        if (originalPrice == null || salePrice == null) {
            return null;
        }
        return originalPrice - salePrice;
    }

    /**
     * Check if product is still in stock.
     *
     * @return true if stockLeft > 0
     */
    public boolean isInStock() {
        return stockLeft != null && stockLeft > 0;
    }
}
