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
 * Utility class to parse Shopee and ShopeeFood product URLs.
 *
 * Shopee Mall URL formats supported:
 * 1. Standard format: https://shopee.vn/product/{shop_id}/{item_id}
 * 2. Shop name format: https://shopee.vn/{shop_name}/{shop_id}/{item_id}
 * 3. Short format: https://shopee.vn/-i.{shop_id}.{item_id}
 * 4. Universal link: https://shopee.vn/universal-link/{item_id}
 * 5. Product name format: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
 * 6. Full URL from homepage: https://shopee.vn/{url-encoded-name}-i.{shop_id}.{item_id}?extraParams={json}
 * 7. Shortened link: https://s.shopee.vn/{short_code} (auto-expanded to format 2)
 *
 * ShopeeFood URL formats supported:
 * 8. Shortened link: https://shopeefood.shopee.vn/u/{short_code}
 * 9. Detail URL: https://shopeefood.vn/now-food/cheap-meal/detail?itemId={id}&restaurantId={id}
 * 10. Restaurant URL: https://shopeefood.vn/{city}/{restaurant-slug}
 *
 * Version History:
 * - v1.0: Initial version with standard, short, universal link formats
 * - v1.1: Added logging and URL encoding validation for full URLs from homepage
 * - v1.2: Added support for shortened links (s.shopee.vn) with auto-expansion
 * - v1.3: Added shop name format (/{shop_name}/{shop_id}/{item_id}) for expanded shortened links
 * - v1.4: Added ShopeeFood support (shopeefood.vn, shopeefood.shopee.vn)
 *
 * @author CashBee Team
 */
@Component
@Slf4j
public class ShopeeUrlParser {

    private final ShopeeUrlExpanderService urlExpanderService;

    /**
     * Constructor with dependency injection.
     *
     * @param urlExpanderService Service to expand shortened URLs
     */
    public ShopeeUrlParser(ShopeeUrlExpanderService urlExpanderService) {
        this.urlExpanderService = urlExpanderService;
    }

    // Pattern 1: https://shopee.vn/product/{shop_id}/{item_id}
    // Example: https://shopee.vn/product/101480242/1635050758
    private static final Pattern STANDARD_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/product/(\\d+)/(\\d+)"
    );

    // Pattern 2: https://shopee.vn/{shop_name}/{shop_id}/{item_id} (NEW)
    // Example: https://shopee.vn/opaanlp/281960897/29266558866
    // This is the format used by Shopee shortened links after expansion
    // - {shop_name} = shop slug (e.g., "opaanlp")
    // - {shop_id} = numeric shop ID (e.g., 281960897)
    // - {item_id} = numeric item ID (e.g., 29266558866)
    private static final Pattern SHOP_NAME_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/([^/]+)/(\\d+)/(\\d+)(?:\\?.*)?$"
    );

    // Pattern 3: https://shopee.vn/-i.{shop_id}.{item_id}
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

    // ============== SHOPEEFOOD PATTERNS ==============

    // Pattern 6: ShopeeFood detail URL with itemId and restaurantId
    // https://shopeefood.vn/now-food/cheap-meal/detail?itemId=278729114&restaurantId=1158256
    // Note: Parameters can be in any order
    private static final Pattern SHOPEEFOOD_DETAIL_PATTERN = Pattern.compile(
        "https?://shopeefood\\.vn/.*\\?.*(?:itemId=(\\d+))"
    );

    // Pattern to extract restaurantId from ShopeeFood URL
    private static final Pattern SHOPEEFOOD_RESTAURANT_ID_PATTERN = Pattern.compile(
        "restaurantId=(\\d+)"
    );

    // Pattern 7: ShopeeFood restaurant page URL
    // https://shopeefood.vn/{city}/{restaurant-slug}
    // Example: https://shopeefood.vn/ha-noi/link-food-trung-van
    private static final Pattern SHOPEEFOOD_RESTAURANT_PATTERN = Pattern.compile(
        "https?://shopeefood\\.vn/([a-z-]+)/([a-z0-9-]+)(?:\\?.*)?$"
    );

    // Pattern to detect ShopeeFood domains
    private static final Pattern SHOPEEFOOD_DOMAIN_PATTERN = Pattern.compile(
        "https?://(?:shopeefood\\.vn|shopeefood\\.shopee\\.vn)/.*"
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
     * ENHANCED in v1.2:
     * - Added support for shortened links (s.shopee.vn)
     * - Auto-expand shortened links before parsing
     *
     * @param url Shopee product URL (can be full URL or shortened link)
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

        // Step 2: Check if this is a shortened link and expand it
        String urlToProcess = url;
        String expandedUrl = null;  // Track if URL was expanded

        if (urlExpanderService.isShortenedUrl(url)) {
            log.info("Detected shortened URL, expanding: {}", url);
            try {
                urlToProcess = urlExpanderService.expandUrl(url);
                expandedUrl = urlToProcess;  // Save expanded URL
                log.info("Successfully expanded shortened URL: {} -> {}", url, urlToProcess);
            } catch (IllegalArgumentException e) {
                log.error("Failed to expand shortened URL: {}", url, e);
                throw new IllegalArgumentException("Failed to expand shortened link: " + e.getMessage(), e);
            }
        }

        // Step 3: Validate URL encoding (security check)
        if (!isValidUrlEncoding(urlToProcess)) {
            log.error("Invalid URL encoding in URL: {}", urlToProcess);
            throw new IllegalArgumentException("Invalid URL encoding format. URL contains malformed percent-encoding.");
        }

        // Step 4: Check if this is a ShopeeFood URL
        if (isShopeeFoodUrl(urlToProcess)) {
            return parseShopeeFoodUrl(url, urlToProcess, expandedUrl);
        }

        // Step 5: Try Pattern 1 - Standard format (Shopee Mall)
        Matcher matcher = STANDARD_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String shopId = matcher.group(1);
            String itemId = matcher.group(2);
            log.info("Parsed as STANDARD format - Shop ID: {}, Item ID: {}", shopId, itemId);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .expandedUrl(expandedUrl)
                .shopId(shopId)
                .itemId(itemId)
                .format("standard")
                .platform("shopee")
                .build();
        }

        // Step 6: Try Pattern 2 - Shop name format (from shortened links)
        // Format: https://shopee.vn/{shop_name}/{shop_id}/{item_id}
        matcher = SHOP_NAME_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String shopName = matcher.group(1);
            String shopId = matcher.group(2);
            String itemId = matcher.group(3);

            log.info("Parsed as SHOP_NAME format - Shop Name: '{}', Shop ID: {}, Item ID: {}",
                shopName, shopId, itemId);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .expandedUrl(expandedUrl)
                .shopId(shopId)
                .itemId(itemId)
                .format("shop_name")
                .platform("shopee")
                .build();
        }

        // Step 7: Try Pattern 3, 4, 5 - Short format, product name, or full URL from homepage
        matcher = SHORT_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String shopId = matcher.group(1);
            String itemId = matcher.group(2);

            String productNamePart = extractProductNamePart(urlToProcess);
            String decodedName = decodeForLogging(productNamePart);

            log.info("Parsed as SHORT/PRODUCT format - Shop ID: {}, Item ID: {}, Product: '{}'",
                shopId, itemId, decodedName);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .expandedUrl(expandedUrl)
                .shopId(shopId)
                .itemId(itemId)
                .format("short")
                .platform("shopee")
                .build();
        }

        // Step 8: Try Pattern 6 - Universal link (no shop_id available)
        matcher = UNIVERSAL_LINK_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String itemId = matcher.group(1);
            log.info("Parsed as UNIVERSAL LINK format - Item ID: {} (no shop_id)", itemId);

            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .expandedUrl(expandedUrl)
                .shopId(null)
                .itemId(itemId)
                .format("universal")
                .platform("shopee")
                .build();
        }

        // Step 9: No pattern matched - invalid URL
        log.error("Failed to parse URL - no pattern matched: {}", urlToProcess);
        throw new IllegalArgumentException("Invalid Shopee URL format: " + urlToProcess);
    }

    /**
     * Check if URL belongs to ShopeeFood domain.
     *
     * Supported domains:
     * - shopeefood.vn
     * - shopeefood.shopee.vn
     *
     * @param url URL to check
     * @return true if ShopeeFood URL
     */
    public boolean isShopeeFoodUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return SHOPEEFOOD_DOMAIN_PATTERN.matcher(url).matches();
    }

    /**
     * Parse ShopeeFood URL and extract restaurant/item information.
     *
     * @param originalUrl Original URL from user
     * @param urlToProcess URL to parse (may be expanded)
     * @param expandedUrl Expanded URL if was shortened, null otherwise
     * @return ParsedShopeeUrl with ShopeeFood data
     */
    private ParsedShopeeUrl parseShopeeFoodUrl(String originalUrl, String urlToProcess, String expandedUrl) {
        log.debug("Parsing ShopeeFood URL: {}", urlToProcess);

        // Try Pattern 1: Detail URL with itemId
        Matcher matcher = SHOPEEFOOD_DETAIL_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String itemId = matcher.group(1);

            // Extract restaurantId if present
            String restaurantId = null;
            Matcher restaurantMatcher = SHOPEEFOOD_RESTAURANT_ID_PATTERN.matcher(urlToProcess);
            if (restaurantMatcher.find()) {
                restaurantId = restaurantMatcher.group(1);
            }

            log.info("Parsed as SHOPEEFOOD_DETAIL format - Item ID: {}, Restaurant ID: {}",
                itemId, restaurantId);

            return ParsedShopeeUrl.builder()
                .originalUrl(originalUrl)
                .expandedUrl(expandedUrl)
                .itemId(itemId)
                .restaurantId(restaurantId)
                .format("shopeefood_detail")
                .platform("shopeefood")
                .build();
        }

        // Try Pattern 2: Restaurant page URL
        matcher = SHOPEEFOOD_RESTAURANT_PATTERN.matcher(urlToProcess);
        if (matcher.find()) {
            String city = matcher.group(1);
            String restaurantSlug = matcher.group(2);

            log.info("Parsed as SHOPEEFOOD_RESTAURANT format - City: {}, Restaurant: {}",
                city, restaurantSlug);

            return ParsedShopeeUrl.builder()
                .originalUrl(originalUrl)
                .expandedUrl(expandedUrl)
                .city(city)
                .restaurantSlug(restaurantSlug)
                .format("shopeefood_restaurant")
                .platform("shopeefood")
                .build();
        }

        // ShopeeFood URL but no pattern matched
        log.error("Failed to parse ShopeeFood URL - no pattern matched: {}", urlToProcess);
        throw new IllegalArgumentException("Invalid ShopeeFood URL format: " + urlToProcess);
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
     * DTO to hold parsed Shopee/ShopeeFood URL data.
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
         * Expanded URL (if original URL was a shortened link).
         */
        private String expandedUrl;

        /**
         * Platform: "shopee" for Shopee Mall, "shopeefood" for ShopeeFood.
         */
        private String platform;

        /**
         * Shop ID extracted from URL (Shopee Mall only).
         * May be null for universal links or ShopeeFood.
         */
        private String shopId;

        /**
         * Item ID (Product ID) extracted from URL.
         */
        private String itemId;

        /**
         * Restaurant ID (ShopeeFood only).
         */
        private String restaurantId;

        /**
         * City slug (ShopeeFood restaurant URL only).
         * Example: "ha-noi", "ho-chi-minh"
         */
        private String city;

        /**
         * Restaurant slug (ShopeeFood restaurant URL only).
         * Example: "link-food-trung-van"
         */
        private String restaurantSlug;

        /**
         * URL format detected.
         * Shopee: "standard", "shop_name", "short", "universal"
         * ShopeeFood: "shopeefood_detail", "shopeefood_restaurant"
         */
        private String format;

        /**
         * Check if this URL has shop_id.
         */
        public boolean hasShopId() {
            return shopId != null && !shopId.isBlank();
        }

        /**
         * Check if this is a ShopeeFood URL.
         */
        public boolean isShopeeFood() {
            return "shopeefood".equals(platform);
        }

        /**
         * Check if original URL was a shortened link that was expanded.
         */
        public boolean wasExpanded() {
            return expandedUrl != null && !expandedUrl.isBlank();
        }

        /**
         * Get the URL to use for building affiliate links.
         * Returns expandedUrl if available, otherwise returns originalUrl.
         *
         * @return URL suitable for affiliate link building
         */
        public String getUrlForAffiliateLink() {
            return wasExpanded() ? expandedUrl : originalUrl;
        }
    }
}
