package com.cashbee.application.dto.affiliate;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for affiliate platform data.
 *
 * Used to transfer platform information to the presentation layer.
 * Excludes sensitive data like API keys and secrets.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePlatformResponse {

    /**
     * Platform ID.
     */
    private Long id;

    /**
     * Platform name (e.g., "Shopee", "Lazada").
     */
    private String name;

    /**
     * Platform code (e.g., "shopee", "lazada").
     */
    private String code;

    /**
     * Default commission rate (percentage).
     * Example: 5.00 means 5%
     */
    private BigDecimal defaultCommissionRate;

    /**
     * Platform status (ACTIVE or INACTIVE).
     */
    private String status;

    /**
     * Whether platform has API credentials configured.
     */
    private Boolean hasApiCredentials;

    /**
     * Base URL for platform API (if configured).
     */
    private String baseUrl;
}
