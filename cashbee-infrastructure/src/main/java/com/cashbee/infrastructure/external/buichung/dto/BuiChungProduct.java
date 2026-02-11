package com.cashbee.infrastructure.external.buichung.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a product from buichung.vn Flash Sale API.
 *
 * Maps to the product structure in productCache from /api/data response.
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuiChungProduct {

    /**
     * Product ID (MongoDB ObjectId format).
     */
    @JsonProperty("_id")
    private String id;

    /**
     * Product image URL.
     */
    private String img;

    /**
     * Product title/name.
     */
    private String title;

    /**
     * Shopee product link (usually shortened: https://s.shopee.vn/xxx).
     */
    private String link;

    /**
     * Flash sale price (as string from API).
     */
    private String price;

    /**
     * Original price before discount.
     */
    @JsonProperty("original_price")
    private Long originalPrice;

    /**
     * Discount percentage (e.g., 99 for 99% off).
     */
    private Integer percent;

    /**
     * Stock quantity left.
     */
    private Integer amount;

    /**
     * Time slot identifier (e.g., "16-12 Khung: 00:00").
     */
    private String time;

    /**
     * Parse price string to Long.
     *
     * @return Price as Long, or null if parsing fails
     */
    public Long getPriceAsLong() {
        if (price == null || price.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(price.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
