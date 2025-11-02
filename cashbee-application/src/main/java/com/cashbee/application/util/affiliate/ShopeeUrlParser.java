package com.cashbee.application.util.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

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
 *
 * @author CashBee Team
 */
@Component
public class ShopeeUrlParser {

    // Pattern 1: https://shopee.vn/product/{shop_id}/{item_id}
    private static final Pattern STANDARD_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/product/(\\d+)/(\\d+)"
    );

    // Pattern 2: https://shopee.vn/-i.{shop_id}.{item_id}
    // Pattern 3: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
    private static final Pattern SHORT_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/.*-i\\.(\\d+)\\.(\\d+)"
    );

    // Pattern 4: https://shopee.vn/universal-link/{item_id}?{params}
    private static final Pattern UNIVERSAL_LINK_PATTERN = Pattern.compile(
        "https?://shopee\\.vn/universal-link/(\\d+)"
    );

    /**
     * Parse Shopee URL and extract product information.
     *
     * @param url Shopee product URL
     * @return ParsedShopeeUrl containing shop_id and item_id
     * @throws IllegalArgumentException if URL format is invalid
     */
    public ParsedShopeeUrl parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Shopee URL cannot be empty");
        }

        // Try Pattern 1: Standard format
        Matcher matcher = STANDARD_PATTERN.matcher(url);
        if (matcher.find()) {
            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(matcher.group(1))
                .itemId(matcher.group(2))
                .format("standard")
                .build();
        }

        // Try Pattern 2 & 3: Short format or product name format
        matcher = SHORT_PATTERN.matcher(url);
        if (matcher.find()) {
            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(matcher.group(1))
                .itemId(matcher.group(2))
                .format("short")
                .build();
        }

        // Try Pattern 4: Universal link (no shop_id available)
        matcher = UNIVERSAL_LINK_PATTERN.matcher(url);
        if (matcher.find()) {
            return ParsedShopeeUrl.builder()
                .originalUrl(url)
                .shopId(null)  // Universal links don't have shop_id
                .itemId(matcher.group(1))
                .format("universal")
                .build();
        }

        throw new IllegalArgumentException("Invalid Shopee URL format: " + url);
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
