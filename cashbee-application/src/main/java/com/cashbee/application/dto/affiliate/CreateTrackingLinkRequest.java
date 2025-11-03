package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an affiliate tracking link.
 *
 * User provides the original Shopee product URL, and system generates affiliate tracking URL.
 *
 * SECURITY NOTE:
 * - User ID is NO LONGER sent in request body (removed for security)
 * - User ID is extracted from JWT token by the controller
 * - This prevents users from creating tracking links for other users
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
     * ⚠️ REMOVED FIELD - DO NOT ADD BACK
     *
     * userId field has been removed for security reasons:
     * - Previously: Client sent userId in request body (insecure - could be forged)
     * - Now: Controller extracts userId from JWT token (secure - cannot be forged)
     *
     * Migration guide:
     * - Frontend should NOT send userId in request body
     * - Frontend MUST send valid JWT token in Authorization header
     * - Backend will automatically extract userId from token
     *
     * Old request:
     * {
     *   "shopeeUrl": "...",
     *   "userId": 123  ← REMOVED
     * }
     *
     * New request:
     * Headers: { "Authorization": "Bearer <JWT_TOKEN>" }
     * Body: {
     *   "shopeeUrl": "..."
     * }
     */
}
