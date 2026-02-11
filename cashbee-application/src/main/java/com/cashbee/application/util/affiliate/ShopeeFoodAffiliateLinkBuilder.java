package com.cashbee.application.util.affiliate;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * ShopeeFood-specific affiliate link builder.
 *
 * Builds affiliate links by appending tracking parameters directly to ShopeeFood URLs.
 *
 * Format: {ORIGINAL_URL}?affiliate_id={ID}&sub_id={TRACKING}
 *
 * This approach works more reliably than redirect-based methods.
 *
 * @author CashBee Team
 */
@Component
public class ShopeeFoodAffiliateLinkBuilder {

    /**
     * Build ShopeeFood affiliate link from original product/restaurant URL.
     *
     * Appends affiliate tracking parameters directly to the original URL.
     * Format: {ORIGINAL_URL}?affiliate_id={ID}&sub_id={TRACKING}
     *
     * @param originalUrl Original ShopeeFood URL
     * @param affiliateId Publisher's affiliate ID
     * @param trackingCode Unique tracking code (sub_id)
     * @return Complete ShopeeFood affiliate URL with tracking parameters
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String build(String originalUrl, String affiliateId, String trackingCode) {
        validateParameters(originalUrl, affiliateId, trackingCode);

        // Determine separator: ? if no existing query params, & if there are
        String separator = originalUrl.contains("?") ? "&" : "?";

        // Build affiliate URL by appending tracking parameters
        return String.format(
            "%s%saffiliate_id=%s&sub_id=%s",
            originalUrl,
            separator,
            affiliateId,
            urlEncode(trackingCode)
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

        if (!isValidShopeeFoodUrl(originalUrl)) {
            throw new IllegalArgumentException("URL must be a valid ShopeeFood URL (shopeefood.vn)");
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
     * Validate if a URL is a valid ShopeeFood URL.
     *
     * @param url URL to validate
     * @return true if valid ShopeeFood URL
     */
    public boolean isValidShopeeFoodUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        return url.startsWith("https://shopeefood.vn/") || url.startsWith("http://shopeefood.vn/");
    }
}
