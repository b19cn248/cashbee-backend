package com.cashbee.infrastructure.external.chietkhau;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the full response from ChietKhau.Pro API.
 *
 * API: POST https://api.chietkhau.pro/api/v1/shopee/product-commission
 * Request: { "link": "https://shopee.vn/product/..." }
 *
 * Response:
 * {
 *   "status": "success",
 *   "productInfo": { ... }
 * }
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChietKhauApiResponse {

    /**
     * API response status: "success" or error message.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Product information including commission.
     */
    @JsonProperty("productInfo")
    private ChietKhauProductInfo productInfo;

    /**
     * Check if API call was successful.
     */
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status) && productInfo != null;
    }
}
