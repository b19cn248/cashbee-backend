package com.cashbee.infrastructure.external.tui3gang;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing product info from Tui3Gang API response.
 *
 * API: POST https://api.tui3gang.com/api/v1/shopee/get-info-product
 * Request: { "url": "https://shopee.vn/product/shopId/itemId", "idUser": "notlogin" }
 *
 * Response example:
 * [{
 *   "itemId": 41251529357,
 *   "shopId": 76219330,
 *   "productName": "Cây Lau Nhà...",
 *   "imageUrl": "https://cf.shopee.vn/file/...",
 *   "sellerCommissionRate": "0.1",
 *   "shopeeCommissionRate": "0.05",
 *   "commissionRate": "0.15",
 *   "commission": 17009.91,
 *   "priceMin": "188999",
 *   "productLink": "https://shopee.vn/product/76219330/41251529357",
 *   "shortUrl": "/Lu842lHzpP",
 *   "finalUrl": "https://s.shopee.vn/an_redir?..."
 * }]
 *
 * Note: commission field contains 60% of full commission for user.
 * CashBee will recalculate to give 80% instead.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tui3GangProductInfo {

    /**
     * Shopee item ID.
     */
    @JsonProperty("itemId")
    private Long itemId;

    /**
     * Shopee shop ID.
     */
    @JsonProperty("shopId")
    private Long shopId;

    /**
     * Product name.
     */
    @JsonProperty("productName")
    private String productName;

    /**
     * Product image URL.
     */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * Seller commission rate (e.g., "0.1" = 10%).
     */
    @JsonProperty("sellerCommissionRate")
    private String sellerCommissionRate;

    /**
     * Shopee commission rate (e.g., "0.05" = 5%).
     */
    @JsonProperty("shopeeCommissionRate")
    private String shopeeCommissionRate;

    /**
     * Total commission rate (seller + shopee).
     */
    @JsonProperty("commissionRate")
    private String commissionRate;

    /**
     * Commission amount in VND.
     * This is 60% of full commission that Tui3Gang gives to users.
     */
    @JsonProperty("commission")
    private Double commission;

    /**
     * Minimum price in VND (as string).
     */
    @JsonProperty("priceMin")
    private String priceMin;

    /**
     * Shopee product link.
     */
    @JsonProperty("productLink")
    private String productLink;

    /**
     * Short URL path.
     */
    @JsonProperty("shortUrl")
    private String shortUrl;

    /**
     * Final affiliate redirect URL.
     */
    @JsonProperty("finalUrl")
    private String finalUrl;
}
