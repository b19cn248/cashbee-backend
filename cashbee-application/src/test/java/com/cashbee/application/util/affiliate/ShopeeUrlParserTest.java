package com.cashbee.application.util.affiliate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for ShopeeUrlParser
 *
 * Test Coverage:
 * 1. Standard format: https://shopee.vn/product/{shop_id}/{item_id}
 * 2. Short format: https://shopee.vn/-i.{shop_id}.{item_id}
 * 3. Product name format: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
 * 4. Full URL format from homepage with URL encoding and query params (NEW)
 * 5. Universal link format: https://shopee.vn/universal-link/{item_id}
 * 6. Invalid URLs and edge cases
 * 7. URL encoding validation
 *
 * @author CashBee Team
 */
@DisplayName("ShopeeUrlParser Tests (TDD)")
class ShopeeUrlParserTest {

    private ShopeeUrlParser parser;

    @BeforeEach
    void setUp() {
        parser = new ShopeeUrlParser();
    }

    /**
     * ===================================================================
     * TEST GROUP 1: STANDARD FORMAT
     * Format: https://shopee.vn/product/{shop_id}/{item_id}
     * ===================================================================
     */
    @Nested
    @DisplayName("Standard Format Tests")
    class StandardFormatTests {

        @Test
        @DisplayName("Should parse standard format with HTTPS")
        void shouldParseStandardFormatHttps() {
            // Given
            String url = "https://shopee.vn/product/101480242/1635050758";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("101480242");
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("standard");
            assertThat(result.hasShopId()).isTrue();
        }

        @Test
        @DisplayName("Should parse standard format with HTTP")
        void shouldParseStandardFormatHttp() {
            // Given
            String url = "http://shopee.vn/product/123456/789012";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("123456");
            assertThat(result.getItemId()).isEqualTo("789012");
            assertThat(result.getFormat()).isEqualTo("standard");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 2: SHORT FORMAT
     * Format: https://shopee.vn/-i.{shop_id}.{item_id}
     * ===================================================================
     */
    @Nested
    @DisplayName("Short Format Tests")
    class ShortFormatTests {

        @Test
        @DisplayName("Should parse short format")
        void shouldParseShortFormat() {
            // Given
            String url = "https://shopee.vn/-i.101480242.1635050758";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("101480242");
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("short");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 3: PRODUCT NAME FORMAT (SIMPLE)
     * Format: https://shopee.vn/Product-Name-i.{shop_id}.{item_id}
     * ===================================================================
     */
    @Nested
    @DisplayName("Product Name Format Tests (Simple)")
    class ProductNameFormatTests {

        @Test
        @DisplayName("Should parse product name format with simple ASCII name")
        void shouldParseProductNameSimple() {
            // Given
            String url = "https://shopee.vn/Bút-xóa-nước-i.101480242.1635050758";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("101480242");
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("short");
        }

        @Test
        @DisplayName("Should parse product name format with multiple dashes")
        void shouldParseProductNameMultipleDashes() {
            // Given
            String url = "https://shopee.vn/Product-Name-With-Many-Dashes-i.123456.789012";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("123456");
            assertThat(result.getItemId()).isEqualTo("789012");
            assertThat(result.getFormat()).isEqualTo("short");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 4: FULL URL FORMAT FROM HOMEPAGE (NEW - TDD)
     * Format: https://shopee.vn/{encoded-product-name}-i.{shop_id}.{item_id}?extraParams={json}
     *
     * This is the main new feature we're testing!
     * ===================================================================
     */
    @Nested
    @DisplayName("Full URL Format from Homepage Tests (NEW)")
    class FullUrlFormatFromHomepageTests {

        @Test
        @DisplayName("Should parse full URL with URL encoding and query params")
        void shouldParseFullUrlWithEncodingAndQueryParams() {
            // Given - Real URL from Shopee homepage
            String url = "https://shopee.vn/B%C3%BAt-x%C3%B3a-n%C6%B0%E1%BB%9Bc-12ml-Thi%C3%AAn-Long-CP-02-Vi%E1%BA%BFt-x%C3%B3a-s%E1%BA%A1ch-nhanh-kh%C3%B4-an-to%C3%A0n-s%E1%BB%AD-d%E1%BB%A5ng-ph%C3%B9-h%E1%BB%A3p-h%E1%BB%8Dc-sinh-sinh-vi%C3%AAn-v%C4%83n-ph%C3%B2ng-i.101480242.1635050758?extraParams=%7B%22display_model_id%22%3A32116686645%7D";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("101480242");
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("short");
            assertThat(result.hasShopId()).isTrue();
        }

        @Test
        @DisplayName("Should parse full URL with URL encoding without query params")
        void shouldParseFullUrlWithEncodingNoQueryParams() {
            // Given
            String url = "https://shopee.vn/B%C3%BAt-x%C3%B3a-n%C6%B0%E1%BB%9Bc-i.101480242.1635050758";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("101480242");
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("short");
        }

        @Test
        @DisplayName("Should parse URL with complex query parameters")
        void shouldParseUrlWithComplexQueryParams() {
            // Given - URL with multiple query parameters
            String url = "https://shopee.vn/Product-Name-i.123456.789012?param1=value1&param2=value2&extraParams=%7B%22key%22%3A%22value%22%7D";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("123456");
            assertThat(result.getItemId()).isEqualTo("789012");
            assertThat(result.getFormat()).isEqualTo("short");
        }

        @Test
        @DisplayName("Should parse very long product name with URL encoding")
        void shouldParseLongProductNameWithEncoding() {
            // Given - Very long product name
            String url = "https://shopee.vn/This-Is-A-Very-Long-Product-Name-With-Many-Words-And-Special-Characters-%C3%A1%C3%A0%C3%A2%C3%A3-i.999888.777666?source=search";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isEqualTo("999888");
            assertThat(result.getItemId()).isEqualTo("777666");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 5: UNIVERSAL LINK FORMAT
     * Format: https://shopee.vn/universal-link/{item_id}
     * ===================================================================
     */
    @Nested
    @DisplayName("Universal Link Format Tests")
    class UniversalLinkFormatTests {

        @Test
        @DisplayName("Should parse universal link format")
        void shouldParseUniversalLink() {
            // Given
            String url = "https://shopee.vn/universal-link/1635050758";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getShopId()).isNull();
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("universal");
            assertThat(result.hasShopId()).isFalse();
        }

        @Test
        @DisplayName("Should parse universal link with query params")
        void shouldParseUniversalLinkWithQueryParams() {
            // Given
            String url = "https://shopee.vn/universal-link/1635050758?param=value";

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItemId()).isEqualTo("1635050758");
            assertThat(result.getFormat()).isEqualTo("universal");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 6: INVALID URLS AND EDGE CASES
     * ===================================================================
     */
    @Nested
    @DisplayName("Invalid URL and Edge Case Tests")
    class InvalidUrlTests {

        @Test
        @DisplayName("Should throw exception for null URL")
        void shouldThrowExceptionForNullUrl() {
            // When & Then
            assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shopee URL cannot be empty");
        }

        @Test
        @DisplayName("Should throw exception for empty URL")
        void shouldThrowExceptionForEmptyUrl() {
            // When & Then
            assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shopee URL cannot be empty");
        }

        @Test
        @DisplayName("Should throw exception for blank URL")
        void shouldThrowExceptionForBlankUrl() {
            // When & Then
            assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shopee URL cannot be empty");
        }

        @Test
        @DisplayName("Should throw exception for non-Shopee URL")
        void shouldThrowExceptionForNonShopeeUrl() {
            // Given
            String url = "https://google.com/product/123/456";

            // When & Then
            assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Shopee URL format");
        }

        @Test
        @DisplayName("Should throw exception for Shopee URL without IDs")
        void shouldThrowExceptionForUrlWithoutIds() {
            // Given
            String url = "https://shopee.vn/";

            // When & Then
            assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Shopee URL format");
        }

        @Test
        @DisplayName("Should throw exception for malformed product URL")
        void shouldThrowExceptionForMalformedUrl() {
            // Given
            String url = "https://shopee.vn/product/notanumber/alsonotanumber";

            // When & Then
            assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Shopee URL format");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 7: URL VALIDATION METHODS
     * ===================================================================
     */
    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("isValidShopeeUrl should return true for valid URL")
        void isValidShouldReturnTrueForValidUrl() {
            // Given
            String url = "https://shopee.vn/product/123456/789012";

            // When
            boolean isValid = parser.isValidShopeeUrl(url);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("isValidShopeeUrl should return true for full URL from homepage")
        void isValidShouldReturnTrueForFullUrl() {
            // Given
            String url = "https://shopee.vn/B%C3%BAt-x%C3%B3a-i.101480242.1635050758?extraParams=test";

            // When
            boolean isValid = parser.isValidShopeeUrl(url);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("isValidShopeeUrl should return false for invalid URL")
        void isValidShouldReturnFalseForInvalidUrl() {
            // Given
            String url = "https://google.com/product/123/456";

            // When
            boolean isValid = parser.isValidShopeeUrl(url);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("extractItemId should extract item ID from valid URL")
        void extractItemIdShouldWork() {
            // Given
            String url = "https://shopee.vn/product/123456/789012";

            // When
            String itemId = parser.extractItemId(url);

            // Then
            assertThat(itemId).isEqualTo("789012");
        }

        @Test
        @DisplayName("extractItemId should extract item ID from full URL")
        void extractItemIdShouldWorkForFullUrl() {
            // Given
            String url = "https://shopee.vn/B%C3%BAt-i.101480242.1635050758?params=test";

            // When
            String itemId = parser.extractItemId(url);

            // Then
            assertThat(itemId).isEqualTo("1635050758");
        }
    }

    /**
     * ===================================================================
     * TEST GROUP 8: BACKWARD COMPATIBILITY
     * Ensure all existing URL formats still work after changes
     * ===================================================================
     */
    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("All URL formats should be parseable")
        void allFormatsShouldBeParseable() {
            // Given - Collection of all supported formats
            String[] urls = {
                "https://shopee.vn/product/123456/789012",                    // standard
                "https://shopee.vn/-i.123456.789012",                         // short
                "https://shopee.vn/Product-Name-i.123456.789012",            // product name
                "https://shopee.vn/B%C3%BAt-i.123456.789012",                // encoded
                "https://shopee.vn/Product-i.123456.789012?param=value",     // with query
                "https://shopee.vn/universal-link/789012"                     // universal
            };

            // When & Then - All should parse successfully
            for (String url : urls) {
                assertThatCode(() -> parser.parse(url))
                    .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("All formats should extract correct item IDs")
        void allFormatsShouldExtractCorrectItemIds() {
            // Given
            String[] urls = {
                "https://shopee.vn/product/111/222",
                "https://shopee.vn/-i.111.222",
                "https://shopee.vn/Name-i.111.222",
                "https://shopee.vn/B%C3%BAt-i.111.222?p=v"
            };

            // When & Then
            for (String url : urls) {
                ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);
                assertThat(result.getItemId()).isEqualTo("222");
            }
        }
    }
}
