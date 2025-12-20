package com.cashbee.application.util.affiliate;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * ShopeeFood-specific affiliate link builder.
 *
 * Builds affiliate links for ShopeeFood URLs using the endpoint:
 * https://shopeefood.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
 *
 * This is similar to Shopee Mall's link builder but uses ShopeeFood's redirect service.
 *
 * Key differences from Shopee Mall:
 * - Base URL: shopeefood.vn/an_redir (vs s.shopee.vn/an_redir)
 * - Accepts shopeefood.vn URLs only
 *
 * @author CashBee Team
 */
@Component
public class ShopeeFoodAffiliateLinkBuilder {

    private static final String SHOPEEFOOD_REDIRECT_BASE = "https://shopeefood.vn/an_redir";

    /**
     * Build ShopeeFood affiliate link from original product/restaurant URL.
     *
     * Format: https://shopeefood.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
     *
     * @param originalUrl Original ShopeeFood URL (will be URL-encoded)
     * @param affiliateId Publisher's affiliate ID
     * @param trackingCode Unique tracking code
     * @return Complete ShopeeFood affiliate URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String build(String originalUrl, String affiliateId, String trackingCode) {
        validateParameters(originalUrl, affiliateId, trackingCode);

        // URL encode the entire original URL
        String encodedUrl = urlEncode(originalUrl);

        // Build the affiliate redirect URL
        return String.format(
            "%s?origin_link=%s&affiliate_id=%s&sub_id=%s",
            SHOPEEFOOD_REDIRECT_BASE,
            encodedUrl,
            affiliateId,
            trackingCode
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

    /**
     * Get the redirect service base URL (for testing purposes).
     *
     * @return ShopeeFood redirect base URL
     */
    public String getRedirectBase() {
        return SHOPEEFOOD_REDIRECT_BASE;
    }
}
