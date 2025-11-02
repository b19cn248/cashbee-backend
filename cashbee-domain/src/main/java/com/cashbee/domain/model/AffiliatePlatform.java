package com.cashbee.domain.model;

import com.cashbee.domain.enums.PlatformStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AffiliatePlatform Domain Model.
 * Pure business object without any infrastructure concerns.
 *
 * Represents an affiliate marketing platform (e.g., Shopee, Lazada, TikTok)
 * that provides affiliate links and commissions.
 *
 * This model is a pure POJO with NO JPA annotations.
 * All persistence logic is handled in the infrastructure layer.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"apiKey", "apiSecret"})
@EqualsAndHashCode(of = {"id"})
public class AffiliatePlatform {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Platform name (e.g., "Shopee", "Lazada", "TikTok Shop").
     * Must be unique.
     */
    private String name;

    /**
     * Platform code (e.g., "shopee", "lazada", "tiktok").
     * Used as identifier in code and URLs.
     * Must be unique and lowercase.
     */
    private String code;

    /**
     * API key for platform integration.
     * Optional - only needed if using platform API.
     */
    private String apiKey;

    /**
     * API secret for platform integration.
     * Optional - only needed if using platform API.
     */
    private String apiSecret;

    /**
     * Base URL for platform API endpoints.
     * Optional - only needed if using platform API.
     */
    private String baseUrl;

    /**
     * Affiliate ID (or Publisher ID) provided by platform.
     * Example: "cashbee_vn_123456"
     * This is used to generate tracking links.
     */
    private String affiliateId;

    /**
     * Link template for generating affiliate tracking URLs.
     * Placeholders: {product_id}, {affiliate_id}, {tracking_code}
     * Example: "https://shopee.vn/universal-link/{product_id}?pid={affiliate_id}&af_sub1={tracking_code}"
     */
    private String linkTemplate;

    /**
     * Whether tracking is enabled for this platform.
     */
    @Builder.Default
    private Boolean trackingEnabled = true;

    /**
     * Default commission rate from platform (percentage).
     * Example: 5.00 means 5% commission.
     * This is the commission the platform pays to us.
     */
    private BigDecimal defaultCommissionRate;

    /**
     * Platform status.
     */
    @Builder.Default
    private PlatformStatus status = PlatformStatus.ACTIVE;

    /**
     * Timestamp when platform was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when platform was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Check if platform is active.
     *
     * @return true if platform status is ACTIVE
     */
    public boolean isActive() {
        return this.status == PlatformStatus.ACTIVE;
    }

    /**
     * Check if platform is inactive.
     *
     * @return true if platform status is INACTIVE
     */
    public boolean isInactive() {
        return this.status == PlatformStatus.INACTIVE;
    }

    /**
     * Activate the platform.
     * Changes status to ACTIVE.
     */
    public void activate() {
        this.status = PlatformStatus.ACTIVE;
    }

    /**
     * Deactivate the platform.
     * Changes status to INACTIVE.
     */
    public void deactivate() {
        this.status = PlatformStatus.INACTIVE;
    }

    /**
     * Check if this is a new platform (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Check if platform has API credentials configured.
     *
     * @return true if both apiKey and apiSecret are set
     */
    public boolean hasApiCredentials() {
        return this.apiKey != null && !this.apiKey.isBlank()
            && this.apiSecret != null && !this.apiSecret.isBlank();
    }

    /**
     * Validate platform data.
     * Throws exception if validation fails.
     */
    public void validate() {
        if (this.name == null || this.name.isBlank()) {
            throw new IllegalStateException("Platform name is required");
        }
        if (this.code == null || this.code.isBlank()) {
            throw new IllegalStateException("Platform code is required");
        }
        if (this.status == null) {
            throw new IllegalStateException("Platform status is required");
        }

        // Code must be lowercase
        if (!this.code.equals(this.code.toLowerCase())) {
            throw new IllegalStateException("Platform code must be lowercase");
        }

        // If commission rate is set, it must be non-negative
        if (this.defaultCommissionRate != null
            && this.defaultCommissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Commission rate cannot be negative");
        }
    }
}
