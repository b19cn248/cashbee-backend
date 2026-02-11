package com.cashbee.domain.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Flash Sale product.
 *
 * This is a DOMAIN layer DTO used for data transfer between
 * Infrastructure (adapters) and Application (use cases).
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleProductData {

    /**
     * Product ID.
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
     * Original product link (e.g., Shopee link).
     */
    private String link;

    /**
     * Flash sale price.
     */
    private Long salePrice;

    /**
     * Original price before discount.
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
}
