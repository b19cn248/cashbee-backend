package com.cashbee.infrastructure.external.chietkhau;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing product info from ChietKhau.Pro API response.
 *
 * Maps the "productInfo" object in API response:
 * {
 *   "status": "success",
 *   "productInfo": {
 *     "itemId": "13360908969",
 *     "productName": "...",
 *     "shopName": "MINI.KIDS",
 *     "price": "258999",
 *     "sales": 306,
 *     "imageUrl": "https://cf.shopee.vn/file/...",
 *     "commission": 9427.56,
 *     "productLink": "https://shopee.vn/product/...",
 *     "isLimitCap": false,
 *     "cap": 10400
 *   }
 * }
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChietKhauProductInfo {

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("shopName")
    private String shopName;

    /**
     * Product price in VND (as string from API).
     */
    @JsonProperty("price")
    private String price;

    /**
     * Number of sales.
     */
    @JsonProperty("sales")
    private Integer sales;

    /**
     * Product image URL.
     */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * Commission amount in VND that ChietKhau.Pro pays to user.
     * This is approximately 52% of Shopee's commission.
     */
    @JsonProperty("commission")
    private Double commission;

    /**
     * Shopee product link.
     */
    @JsonProperty("productLink")
    private String productLink;

    /**
     * Whether commission is capped.
     */
    @JsonProperty("isLimitCap")
    private Boolean isLimitCap;

    /**
     * Maximum commission cap in VND (typically 10,400).
     */
    @JsonProperty("cap")
    private Integer cap;
}
