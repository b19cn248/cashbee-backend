package com.cashbee.infrastructure.external.shoppingtietkiem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product info response from STK {@code POST /api/affiliate/product-info}.
 *
 * Example:
 * <pre>
 * {
 *   "productName": "...",
 *   "imageUrl": "...",
 *   "sales": 44,
 *   "priceMin": 345000,
 *   "shopName": "HDRBOOKS",
 *   "commissionRate": 0.035,
 *   "userCashbackRate": 2.5,
 *   "estimatedCashback": 12075,
 *   "productLink": "https://shopee.vn/..."
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShoppingTietKiemProductInfo {

    private String productName;
    private String imageUrl;
    private Double rating;
    private Integer ratingCount;
    private Integer sales;
    private BigDecimal priceMin;
    private String shopName;

    /**
     * Total commission rate as decimal (e.g. 0.035 = 3.5%).
     */
    private BigDecimal commissionRate;

    /**
     * User cashback rate as percentage (e.g. 2.5 = 2.5%). Not used for CashBee calc.
     */
    private BigDecimal userCashbackRate;

    /**
     * Estimated cashback amount in VND from STK.
     */
    private BigDecimal estimatedCashback;

    private String productLink;
}
