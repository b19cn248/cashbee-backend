package com.cashbee.infrastructure.external.peeback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product info response from Peeback {@code /api/shopee/product-info}.
 *
 * Example:
 * <pre>
 * {
 *   "itemId": 25892454268,
 *   "shopId": 528677227,
 *   "productName": "...",
 *   "imageUrl": "https://cf.shopee.vn/file/...",
 *   "priceMin": "76740",
 *   "commissionRate": "0.115",
 *   "sellerCommissionRate": "0.0800",
 *   "shopeeCommissionRate": "0.0350",
 *   "commission": 8825,
 *   "_fromCache": false,
 *   "_source": "stk"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeebackProductInfo {

    private Long itemId;
    private Long shopId;
    private String productName;
    private String imageUrl;
    private String shopName;

    /**
     * Price in VND (API may send number or string).
     */
    private BigDecimal priceMin;

    /**
     * Total commission rate as decimal (e.g. 0.115 = 11.5%).
     */
    private BigDecimal commissionRate;

    /**
     * Seller commission rate as decimal.
     */
    private BigDecimal sellerCommissionRate;

    /**
     * Shopee commission rate as decimal.
     */
    private BigDecimal shopeeCommissionRate;

    /**
     * Estimated commission amount in VND.
     */
    private BigDecimal commission;

    private Integer sales;
    private String productLink;
}
