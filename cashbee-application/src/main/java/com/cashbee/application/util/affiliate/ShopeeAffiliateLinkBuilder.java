package com.cashbee.application.util.affiliate;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Shopee-specific affiliate link builder.
 *
 * Builds affiliate links using Shopee's an_redir redirect service.
 *
 * Format: https://s.shopee.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
 *
 * @author CashBee Team
 */
@Component
public class ShopeeAffiliateLinkBuilder {

    private static final String SHOPEE_REDIRECT_BASE = "https://s.shopee.vn/an_redir";

    /**
     * Build Shopee affiliate link from original product URL.
     *
     * Uses Shopee's an_redir redirect service.
     * Format: https://s.shopee.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
     *
     * @param originalUrl Original Shopee product/shop URL
     * @param affiliateId Publisher's affiliate ID
     * @param trackingCode Unique tracking code (sub_id)
     * @return Complete Shopee affiliate URL using an_redir format
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String build(String originalUrl, String affiliateId, String trackingCode) {
        validateParameters(originalUrl, affiliateId, trackingCode);

        String encodedUrl = urlEncode(originalUrl);

        return String.format(
            "%s?origin_link=%s&affiliate_id=%s&sub_id=%s",
            SHOPEE_REDIRECT_BASE,
            encodedUrl,
            affiliateId,
            urlEncode(trackingCode)
        );
    }

    /**
     * Build Shopee affiliate link with advanced sub_id tracking.
     *
     * Sub ID format: value1-value2-value3-value4-value5
     * - value1: Primary tracking code
     * - value2: User ID or secondary tracking
     * - value3: Campaign ID
     * - value4: Source (web, mobile, etc.)
     * - value5: Custom parameter
     *
     * @param originalUrl Original Shopee product/shop URL
     * @param affiliateId Publisher's affiliate ID
     * @param subId1 Primary tracking code (required)
     * @param subId2 User ID or secondary tracking (optional)
     * @param subId3 Campaign ID (optional)
     * @param subId4 Source (optional)
     * @param subId5 Custom parameter (optional)
     * @return Complete Shopee affiliate URL with advanced tracking
     */
    public String buildWithAdvancedTracking(
        String originalUrl,
        String affiliateId,
        String subId1,
        String subId2,
        String subId3,
        String subId4,
        String subId5
    ) {
        validateParameters(originalUrl, affiliateId, subId1);

        // Build sub_id with 5 hyphen-separated values
        String subId = buildSubId(subId1, subId2, subId3, subId4, subId5);

        String encodedUrl = urlEncode(originalUrl);

        return String.format(
            "%s?origin_link=%s&affiliate_id=%s&sub_id=%s",
            SHOPEE_REDIRECT_BASE,
            encodedUrl,
            affiliateId,
            urlEncode(subId)
        );
    }

    /**
     * Get the Shopee redirect base URL.
     *
     * @return Shopee redirect base URL
     */
    public String getRedirectBase() {
        return SHOPEE_REDIRECT_BASE;
    }

    /**
     * Build sub_id parameter with 5 values.
     *
     * Format: value1-value2-value3-value4-value5
     *
     * @param subId1 Primary tracking code (required)
     * @param subId2 Secondary value (optional)
     * @param subId3 Tertiary value (optional)
     * @param subId4 Quaternary value (optional)
     * @param subId5 Quinary value (optional)
     * @return Sub ID string with 5 values separated by hyphens
     */
    private String buildSubId(String subId1, String subId2, String subId3, String subId4, String subId5) {
        return String.format(
            "%s-%s-%s-%s-%s",
            subId1 != null ? subId1 : "",
            subId2 != null ? subId2 : "",
            subId3 != null ? subId3 : "",
            subId4 != null ? subId4 : "",
            subId5 != null ? subId5 : ""
        );
    }

    /**
     * Validate required parameters.
     *
     * @param originalUrl Original URL
     * @param affiliateId Affiliate ID
     * @param trackingCode Tracking code
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private void validateParameters(String originalUrl, String affiliateId, String trackingCode) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("Original URL is required");
        }

        if (!isValidShopeeUrl(originalUrl)) {
            throw new IllegalArgumentException("URL must be a valid Shopee URL (shopee.vn)");
        }

        if (affiliateId == null || affiliateId.isBlank()) {
            throw new IllegalArgumentException("Affiliate ID is required");
        }

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code is required");
        }
    }

    /**
     * URL encode a value for safe inclusion in URL.
     *
     * @param value Value to encode
     * @return URL encoded value
     * @throws RuntimeException if encoding fails (should never happen with UTF-8)
     */
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }

        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported, this should never happen
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * Validate if a URL is a valid Shopee URL.
     *
     * Accepts:
     * - https://shopee.vn/... (standard product URLs)
     * - http://shopee.vn/... (non-HTTPS version)
     *
     * @param url URL to validate
     * @return true if valid Shopee URL
     */
    public boolean isValidShopeeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        return url.startsWith("https://shopee.vn/")
            || url.startsWith("http://shopee.vn/");
    }
}
