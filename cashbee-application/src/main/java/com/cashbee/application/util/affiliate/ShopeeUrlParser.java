package com.cashbee.application.util.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse Shopee product URLs and extract shop_id, item_id.
 *
 * Shopee URL formats supported:
 * 1. Standard format: https://shopee.vn/product/{shop_id}/{item_id}
 * 2. Short format: https://shopee.vn/-i.{shop_id}.{item_id}
 * 3. Universal link: https://shopee.vn/universal-link/{item_id}
 * 4. Product name format: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
 * 5. Full URL from homepage (NEW): https://shopee.vn/{url-encoded-name}-i.{shop_id}.{item_id}?extraParams={json}
 *
 * Version History:
 * - v1.0: Initial version with standard, short, universal link formats
 * - v1.1: Added logging and URL encoding validation for full URLs from homepage
 *
 * @author CashBee Team
 */
@Component
@Slf4j
public class ShopeeUrlParser {

    // Pattern 1: https://shopee.vn/product/{shop_id}/{item_id}
    // Example: https://shopee.vn/product/101480242/1635050758
    private static final Pattern STANDARD_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/product/(\\d+)/(\\d+)"
    );

    // Pattern 2: https://shopee.vn/-i.{shop_id}.{item_id}
    // Pattern 3: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
    // Pattern 4: https://shopee.vn/{url-encoded-name}-i.{shop_id}.{item_id}?extraParams={json}
    //
    // ENHANCED: Now explicitly handles query parameters with (?:\\?.*)?
    // - (?:...) = Non-capturing group (don't create extra group)
    // - \\? = Literal question mark (escaped)
    // - .* = Match any characters (query params)
    // - ? = Make the query params optional
    //
    // Examples:
    // - https://shopee.vn/-i.123.456
    // - https://shopee.vn/Product-i.123.456
    // - https://shopee.vn/B%C3%BAt-x%C3%B3a-i.123.456
    // - https://shopee.vn/B%C3%BAt-x%C3%B3a-i.123.456?extraParams=%7B%22key%22%3A%22value%22%7D
    private static final Pattern SHORT_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/.*-i\\.(\\d+)\\.(\\d+)(?:\\?.*)?$"
    );

    // Pattern 5: https://shopee.vn/universal-link/{item_id}?{params}
    // Example: https://shopee.vn/universal-link/1635050758?param=value
    private static final Pattern UNIVERSAL_LINK_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/universal-link/(\\d+)(?:\\?.*)?$"
    );

    /**
     * Validate if URL encoding is safe and properly formatted.
     *
     * This method checks if the URL contains proper percent-encoding.
     * Valid encoded characters: %XX where X is a hexadecimal digit.
     *
     * @param url URL to validate
     * @return true if URL encoding is valid or no encoding present
     */
    private boolean isValidUrlEncoding(String url) {
        // Pattern to match invalid percent encoding (% not followed by two hex digits)
        Pattern invalidEncodingPattern = Pattern.compile("%(?![0-9A-Fa-f]{2})");
        Matcher matcher = invalidEncodingPattern.matcher(url);

        if (matcher.find()) {
            log.warn("Invalid URL encoding detected in URL: {}", url);
            return false;
        }

        return true;
    }

    /**
     * Decode URL-encoded string for logging purposes.
     *
     * This helps make logs more readable by showing the actual product name
     * instead of the encoded version.
     *
     * @param encoded URL-encoded string
     * @return Decoded string, or original if decoding fails
     */
    private String decodeForLogging(String encoded) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            log.debug("Could not decode URL for logging (non-critical): {}", encoded);
            return encoded;  // Return original if decoding fails
        }
    }

    /**
     * Parse Shopee URL and extract product information.
     *
     * ENHANCED in v1.1:
     * - Added URL encoding validation
     * - Added detailed logging for debugging
     * - Improved query parameter handling
     *
     * @param url Shopee product URL
     * @return ParsedShopeeUrl containing shop_id and item_id
     * @throws IllegalArgumentException if URL format is invalid or URL encoding is malformed
     */
    public ParsedShopeeUrl parse(String url) {
        log.debug("Parsing Shopee URL: {}", url);

        // Step 1: Validate input is not null/blank
        if (url == null || url.isBlank()) {
            log.error("Received null or blank URL");
            throw new IllegalArgumentException("Shopee URL cannot be empty");
        }

        // Step 2: Validate URL encoding (security check)
        if (!isValidUrlEncoding(url)) {
            log.error("Invalid URL encoding in URL: {}", url);
            throw new IllegalArgumentException("Invalid URL encoding format. URL contains malformed percent-encoding.");
        }

        // Step 3: Try Pattern 1 - Standard format
        Matcher matcher = STANDARD_PATTERN.matcher(url);
        if (matcher.find()) {
            String shopId = matcher.group(1);
            String itemId = matcher.group(2);
            log.info("Parsed as STANDARD format - Shop ID: {}, Item ID: {}", shopId, itemId);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(shopId)
                .itemId(itemId)
                .format("standard")
                .build();
        }

        // Step 4: Try Pattern 2, 3, 4 - Short format, product name, or full URL from homepage
        matcher = SHORT_PATTERN.matcher(url);
        if (matcher.find()) {
            String shopId = matcher.group(1);
            String itemId = matcher.group(2);

            // Extract product name part for logging (if URL encoded, decode it)
            String productNamePart = extractProductNamePart(url);
            String decodedName = decodeForLogging(productNamePart);

            log.info("Parsed as SHORT/PRODUCT format - Shop ID: {}, Item ID: {}, Product: '{}'",
                shopId, itemId, decodedName);

            // Check if URL has query parameters
            if (url.contains("?")) {
                String queryPart = url.substring(url.indexOf("?") + 1);
                log.debug("Query parameters detected: {}", queryPart);
            }

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(shopId)
                .itemId(itemId)
                .format("short")
                .build();
        }

        // Step 5: Try Pattern 5 - Universal link (no shop_id available)
        matcher = UNIVERSAL_LINK_PATTERN.matcher(url);
        if (matcher.find()) {
            String itemId = matcher.group(1);
            log.info("Parsed as UNIVERSAL LINK format - Item ID: {} (no shop_id)", itemId);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(null)  // Universal links don't have shop_id
                .itemId(itemId)
                .format("universal")
                .build();
        }

        // Step 6: No pattern matched - invalid URL
        log.error("Failed to parse URL - no pattern matched: {}", url);
        throw new IllegalArgumentException("Invalid Shopee URL format: " + url);
    }

    /**
     * Extract product name part from URL for logging.
     *
     * Extracts the part between domain and "-i." pattern.
     * Example: https://shopee.vn/B%C3%BAt-x%C3%B3a-i.123.456 → "B%C3%BAt-x%C3%B3a"
     *
     * @param url Shopee URL
     * @return Product name part (may be URL encoded)
     */
    private String extractProductNamePart(String url) {
        try {
            // Remove protocol and domain
            String path = url.replaceFirst("https?://shopee\\.vn/", "");

            // Find the position of "-i."
            int indexOfPattern = path.indexOf("-i.");
            if (indexOfPattern > 0) {
                return path.substring(0, indexOfPattern);
            }

            return "unknown";
        } catch (Exception e) {
            log.debug("Could not extract product name from URL: {}", url);
            return "unknown";
        }
    }

    /**
     * Check if the URL is a valid Shopee URL.
     *
     * @param url URL to check
     * @return true if valid Shopee URL, false otherwise
     */
    public boolean isValidShopeeUrl(String url) {
        try {
            parse(url);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract item ID only from URL (without parsing shop_id).
     * Useful when you only need the product ID.
     *
     * @param url Shopee product URL
     * @return item_id as String
     * @throws IllegalArgumentException if URL format is invalid
     */
    public String extractItemId(String url) {
        ParsedShopeeUrl parsed = parse(url);
        return parsed.getItemId();
    }

    /**
     * DTO to hold parsed Shopee URL data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedShopeeUrl {
        /**
         * Original URL provided by user.
         */
        private String originalUrl;

        /**
         * Shop ID extracted from URL.
         * May be null for universal links.
         */
        private String shopId;

        /**
         * Item ID (Product ID) extracted from URL.
         */
        private String itemId;

        /**
         * URL format detected: "standard", "short", or "universal".
         */
        private String format;

        /**
         * Check if this URL has shop_id.
         *
         * @return true if shop_id is present
         */
        public boolean hasShopId() {
            return shopId != null && !shopId.isBlank();
        }
    }
}
