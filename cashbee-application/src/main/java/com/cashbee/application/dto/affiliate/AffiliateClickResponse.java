package com.cashbee.application.dto.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for affiliate click (link) information.
 * Used by Admin to view all tracking links created by users.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateClickResponse {

    /**
     * Click ID (database ID).
     */
    private Long id;

    /**
     * User ID who created this link.
     */
    private Long userId;

    /**
     * Username of the user who created this link.
     */
    private String username;

    /**
     * Platform ID (e.g., 1 = Shopee).
     */
    private Long platformId;

    /**
     * Platform name (e.g., "Shopee").
     */
    private String platformName;

    /**
     * Shop ID from URL.
     */
    private String shopId;

    /**
     * Item/Product ID from URL.
     */
    private String itemId;

    /**
     * Product name (if available).
     */
    private String productName;

    /**
     * Unique tracking code embedded in URL.
     * Format: CB{userId}_{clickId}_{timestamp}
     */
    private String trackingCode;

    /**
     * Full generated affiliate tracking URL.
     */
    private String trackingUrl;

    /**
     * Original URL user pasted.
     */
    private String originalUrl;

    /**
     * When the tracking link was created.
     */
    private LocalDateTime createdAt;

    /**
     * When user clicked the link (if clicked).
     */
    private LocalDateTime clickedAt;

    /**
     * Whether this click has been matched with an order.
     */
    private Boolean orderMatched;

    /**
     * Matched order ID (if matched).
     */
    private Long matchedOrderId;

    /**
     * Current status: CREATED, CLICKED, MATCHED, EXPIRED.
     */
    private String status;
}
