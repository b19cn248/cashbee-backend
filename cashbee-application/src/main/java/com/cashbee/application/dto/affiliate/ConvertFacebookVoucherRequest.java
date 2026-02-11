package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for converting Shopee link to affiliate link for Facebook voucher.
 *
 * This is a simple link conversion without tracking.
 * User copies the converted link, pastes it on Facebook, and clicks from Facebook
 * to receive Facebook-exclusive vouchers (20-25% off).
 *
 * Flow:
 * 1. User pastes Shopee product URL
 * 2. System converts to affiliate link with Cashbee's affiliate_id
 * 3. User copies converted link to Facebook comment/post
 * 4. User clicks link FROM Facebook
 * 5. Shopee detects Facebook traffic and shows exclusive vouchers
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertFacebookVoucherRequest {

    /**
     * Original Shopee product URL pasted by user.
     *
     * Supported formats:
     * - https://shopee.vn/product/{shop_id}/{item_id}
     * - https://shopee.vn/{product-name}-i.{shop_id}.{item_id}
     * - https://s.shopee.vn/{short_code} (shortened links)
     * - https://shopee.vn/universal-link/{item_id}
     */
    @NotBlank(message = "Shopee URL is required")
    private String shopeeUrl;
}
