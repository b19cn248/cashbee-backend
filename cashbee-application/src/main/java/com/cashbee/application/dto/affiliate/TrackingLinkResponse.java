package com.cashbee.application.dto.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO containing the generated affiliate tracking link.
 * Returns to user after successfully creating a tracking link.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingLinkResponse {

    /**
     * Click ID (auto-generated database ID).
     */
    private Long clickId;

    /**
     * Generated affiliate tracking URL.
     * This is what user clicks to go to Shopee with tracking.
     * Example: "https://shopee.vn/universal-link/123456789?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_100_20251101160530"
     */
    private String trackingUrl;

    /**
     * Unique tracking code encoded in URL.
     * Format: CB{userId}_{clickId}_{timestamp}
     * Example: "CB1_100_20251101160530"
     */
    private String trackingCode;

    /**
     * Original Shopee URL provided by user.
     */
    private String originalUrl;

    /**
     * Product name extracted from URL or fetched from platform.
     * May be null if product name cannot be determined.
     */
    private String productName;

    /**
     * Shop ID extracted from Shopee URL.
     */
    private String shopId;

    /**
     * Item ID (Product ID) extracted from Shopee URL.
     */
    private String itemId;

    /**
     * Platform name (e.g., "Shopee", "Lazada").
     */
    private String platformName;

    /**
     * Platform code (e.g., "shopee", "lazada").
     */
    private String platformCode;

    /**
     * Estimated cashback rate (percentage).
     * Example: 5.00 means 5% cashback.
     * Based on platform's default commission rate.
     */
    private BigDecimal estimatedCashbackRate;

    /**
     * Timestamp when tracking link was created.
     */
    private LocalDateTime createdAt;

    /**
     * Instructions or message to display to user.
     * Example: "Click this link to shop on Shopee and earn cashback!"
     */
    private String message;
}
