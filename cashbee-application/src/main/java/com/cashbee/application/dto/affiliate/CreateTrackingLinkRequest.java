package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an affiliate tracking link.
 * User provides the original Shopee product URL, and system generates affiliate tracking URL.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrackingLinkRequest {

    /**
     * Original Shopee product URL pasted by user.
     * Example: "https://shopee.vn/product/123456789/987654321"
     * or "https://shopee.vn/-i.123456789.987654321"
     */
    @NotBlank(message = "Shopee URL is required")
    private String shopeeUrl;

    /**
     * Platform code (e.g., "shopee", "lazada", "tiktok").
     * Optional - can be auto-detected from URL if not provided.
     */
    private String platformCode;

    /**
     * User ID who is creating the tracking link.
     * This will be encoded into the tracking code.
     */
    @NotNull(message = "User ID is required")
    private Long userId;
}
