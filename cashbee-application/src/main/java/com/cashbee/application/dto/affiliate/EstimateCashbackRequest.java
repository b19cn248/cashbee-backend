package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for estimating cashback amount.
 *
 * User provides a Shopee product URL to get estimated cashback amount
 * they will receive when purchasing through CashBee.
 *
 * This endpoint can be public (no authentication required) to allow
 * users to check cashback before signing up.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateCashbackRequest {

    /**
     * Shopee product URL.
     * Supported formats:
     * - https://shopee.vn/product/shopId/itemId
     * - https://shopee.vn/-i.shopId.itemId
     * - https://s.shopee.vn/... (shortened links)
     */
    @NotBlank(message = "Shopee URL is required")
    private String shopeeUrl;
}
