package com.cashbee.application.util.affiliate;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ShopeeFood-specific affiliate link builder.
 *
 * Builds affiliate links using ShopeeFood's an_redir redirect service.
 *
 * Format: https://shopeefood.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
 *
 * @author CashBee Team
 */
@Component
public class ShopeeFoodAffiliateLinkBuilder {

    private static final String SHOPEEFOOD_REDIRECT_BASE = "https://shopeefood.vn/an_redir";

    /**
     * Build ShopeeFood affiliate link from original product/restaurant URL.
     *
     * Uses ShopeeFood's an_redir redirect service.
     * Format: https://shopeefood.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
     *
     * @param originalUrl Original ShopeeFood URL
     * @param affiliateId Publisher's affiliate ID
     * @param trackingCode Unique tracking code (sub_id)
     * @return Complete ShopeeFood affiliate URL using an_redir format
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String build(String originalUrl, String affiliateId, String trackingCode) {
        validateParameters(originalUrl, affiliateId, trackingCode);

        String cleanUrl = stripTrackingParams(originalUrl);
        String encodedUrl = urlEncode(cleanUrl);

        return String.format(
            "%s?origin_link=%s&affiliate_id=%s&sub_id=%s",
            SHOPEEFOOD_REDIRECT_BASE,
            encodedUrl,
            affiliateId,
            urlEncode(trackingCode)
        );
    }

    /**
     * Get the ShopeeFood redirect base URL.
     *
     * @return ShopeeFood redirect base URL
     */
    public String getRedirectBase() {
        return SHOPEEFOOD_REDIRECT_BASE;
    }

    private static final Set<String> ALLOWED_PARAMS = Set.of("itemId", "restaurantId");

    /**
     * Strip tracking/affiliate params from a ShopeeFood URL, keeping only
     * functional params (itemId, restaurantId) needed for the page to work.
     *
     * @param url Original ShopeeFood URL potentially containing tracking params
     * @return Clean URL with only allowed query parameters
     */
    private String stripTrackingParams(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return url;
            }

            Map<String, String> kept = new LinkedHashMap<>();
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                String key = eq > 0 ? pair.substring(0, eq) : pair;
                String value = eq > 0 ? pair.substring(eq + 1) : "";
                if (ALLOWED_PARAMS.contains(key)) {
                    kept.put(key, value);
                }
            }

            String newQuery = null;
            if (!kept.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : kept.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    sb.append(entry.getKey()).append('=').append(entry.getValue());
                }
                newQuery = sb.toString();
            }

            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, null).toString();
        } catch (URISyntaxException e) {
            int queryIndex = url.indexOf('?');
            return queryIndex > 0 ? url.substring(0, queryIndex) : url;
        }
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
