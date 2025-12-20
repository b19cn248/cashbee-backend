package com.cashbee.application.util.affiliate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ShopeeUrlParser - ShopeeFood URL support.
 *
 * Tests the parsing of ShopeeFood URLs in various formats:
 * 1. Shortened link: https://shopeefood.shopee.vn/u/{short_code}
 * 2. Full detail URL: https://shopeefood.vn/now-food/cheap-meal/detail?itemId=...&restaurantId=...
 * 3. Restaurant URL: https://shopeefood.vn/{city}/{restaurant-slug}
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShopeeUrlParser - ShopeeFood Support")
class ShopeeUrlParserShopeeFoodTest {

    @Mock
    private ShopeeUrlExpanderService urlExpanderService;

    private ShopeeUrlParser parser;

    @BeforeEach
    void setUp() {
        parser = new ShopeeUrlParser(urlExpanderService);
    }

    @Nested
    @DisplayName("ShopeeFood Shortened URLs")
    class ShopeeFoodShortenedUrls {

        @Test
        @DisplayName("Should parse ShopeeFood shortened URL after expansion")
        void shouldParseShopeeFoodShortenedUrl() {
            // Given: A ShopeeFood shortened URL
            String shortenedUrl = "https://shopeefood.shopee.vn/u/je8DiJT";
            String expandedUrl = "https://shopeefood.vn/now-food/cheap-meal/detail?fromSource=1&itemId=278729114&restaurantId=1158256&storeId=1158256";

            when(urlExpanderService.isShortenedUrl(shortenedUrl)).thenReturn(true);
            when(urlExpanderService.expandUrl(shortenedUrl)).thenReturn(expandedUrl);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(shortenedUrl);

            // Then: Should extract correct information
            assertThat(result).isNotNull();
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getItemId()).isEqualTo("278729114");
            assertThat(result.getRestaurantId()).isEqualTo("1158256");
            assertThat(result.getOriginalUrl()).isEqualTo(shortenedUrl);
            assertThat(result.getExpandedUrl()).isEqualTo(expandedUrl);
            assertThat(result.wasExpanded()).isTrue();
        }
    }

    @Nested
    @DisplayName("ShopeeFood Full Detail URLs")
    class ShopeeFoodFullDetailUrls {

        @Test
        @DisplayName("Should parse ShopeeFood detail URL with itemId and restaurantId")
        void shouldParseShopeeFoodDetailUrl() {
            // Given: A full ShopeeFood detail URL
            String url = "https://shopeefood.vn/now-food/cheap-meal/detail?fromSource=1&itemId=278729114&restaurantId=1158256&storeId=1158256";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should extract correct information
            assertThat(result).isNotNull();
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getItemId()).isEqualTo("278729114");
            assertThat(result.getRestaurantId()).isEqualTo("1158256");
            assertThat(result.getFormat()).isEqualTo("shopeefood_detail");
            assertThat(result.wasExpanded()).isFalse();
        }

        @Test
        @DisplayName("Should parse ShopeeFood URL with parameters in different order")
        void shouldParseShopeeFoodUrlWithDifferentParamOrder() {
            // Given: URL with restaurantId before itemId
            String url = "https://shopeefood.vn/now-food/cheap-meal/detail?restaurantId=1158256&itemId=278729114";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should extract correct information regardless of param order
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getItemId()).isEqualTo("278729114");
            assertThat(result.getRestaurantId()).isEqualTo("1158256");
        }

        @Test
        @DisplayName("Should parse ShopeeFood URL with only itemId (no restaurantId)")
        void shouldParseShopeeFoodUrlWithOnlyItemId() {
            // Given: URL with only itemId
            String url = "https://shopeefood.vn/now-food/detail?itemId=278729114";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should extract itemId, restaurantId is null
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getItemId()).isEqualTo("278729114");
            assertThat(result.getRestaurantId()).isNull();
        }
    }

    @Nested
    @DisplayName("ShopeeFood Restaurant URLs")
    class ShopeeFoodRestaurantUrls {

        @Test
        @DisplayName("Should parse ShopeeFood restaurant URL with city and slug")
        void shouldParseShopeeFoodRestaurantUrl() {
            // Given: A restaurant page URL
            String url = "https://shopeefood.vn/ha-noi/link-food-trung-van";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should identify as ShopeeFood restaurant
            assertThat(result).isNotNull();
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getFormat()).isEqualTo("shopeefood_restaurant");
            assertThat(result.getCity()).isEqualTo("ha-noi");
            assertThat(result.getRestaurantSlug()).isEqualTo("link-food-trung-van");
        }

        @Test
        @DisplayName("Should parse ShopeeFood Ho Chi Minh restaurant URL")
        void shouldParseShopeeFoodHCMRestaurantUrl() {
            // Given: A HCM restaurant page URL
            String url = "https://shopeefood.vn/ho-chi-minh/indochine-saigon-restaurant";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should identify as ShopeeFood restaurant
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
            assertThat(result.getCity()).isEqualTo("ho-chi-minh");
            assertThat(result.getRestaurantSlug()).isEqualTo("indochine-saigon-restaurant");
        }
    }

    @Nested
    @DisplayName("Platform Detection")
    class PlatformDetection {

        @Test
        @DisplayName("Should detect Shopee Mall URL as 'shopee' platform")
        void shouldDetectShopeeMallAsShopee() {
            // Given: A regular Shopee URL
            String url = "https://shopee.vn/product/123456/789012";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should be 'shopee' platform
            assertThat(result.getPlatform()).isEqualTo("shopee");
        }

        @Test
        @DisplayName("Should detect ShopeeFood URL as 'shopeefood' platform")
        void shouldDetectShopeeFoodAsShopeefood() {
            // Given: A ShopeeFood URL
            String url = "https://shopeefood.vn/now-food/detail?itemId=123";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When: Parsing the URL
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: Should be 'shopeefood' platform
            assertThat(result.getPlatform()).isEqualTo("shopeefood");
        }

        @Test
        @DisplayName("isShopeeFoodUrl should return true for ShopeeFood domain")
        void isShopeeFoodUrlShouldReturnTrueForShopeeFoodDomain() {
            // Given
            String url = "https://shopeefood.vn/ha-noi/restaurant";

            // When & Then
            assertThat(parser.isShopeeFoodUrl(url)).isTrue();
        }

        @Test
        @DisplayName("isShopeeFoodUrl should return true for shopeefood.shopee.vn domain")
        void isShopeeFoodUrlShouldReturnTrueForShopeeFoodShopeeDomain() {
            // Given
            String url = "https://shopeefood.shopee.vn/u/abc123";

            // When & Then
            assertThat(parser.isShopeeFoodUrl(url)).isTrue();
        }

        @Test
        @DisplayName("isShopeeFoodUrl should return false for Shopee Mall URL")
        void isShopeeFoodUrlShouldReturnFalseForShopeeMall() {
            // Given
            String url = "https://shopee.vn/product/123/456";

            // When & Then
            assertThat(parser.isShopeeFoodUrl(url)).isFalse();
        }
    }

    @Nested
    @DisplayName("getUrlForAffiliateLink method")
    class GetUrlForAffiliateLinkMethod {

        @Test
        @DisplayName("Should return expanded URL for ShopeeFood shortened link")
        void shouldReturnExpandedUrlForShopeeFoodShortenedLink() {
            // Given
            String shortenedUrl = "https://shopeefood.shopee.vn/u/je8DiJT";
            String expandedUrl = "https://shopeefood.vn/now-food/detail?itemId=278729114&restaurantId=1158256";

            when(urlExpanderService.isShortenedUrl(shortenedUrl)).thenReturn(true);
            when(urlExpanderService.expandUrl(shortenedUrl)).thenReturn(expandedUrl);

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(shortenedUrl);

            // Then: getUrlForAffiliateLink should return expanded URL
            assertThat(result.getUrlForAffiliateLink()).isEqualTo(expandedUrl);
        }

        @Test
        @DisplayName("Should return original URL for full ShopeeFood URL")
        void shouldReturnOriginalUrlForFullShopeeFoodUrl() {
            // Given
            String url = "https://shopeefood.vn/now-food/detail?itemId=278729114";

            when(urlExpanderService.isShortenedUrl(url)).thenReturn(false);

            // When
            ShopeeUrlParser.ParsedShopeeUrl result = parser.parse(url);

            // Then: getUrlForAffiliateLink should return original URL
            assertThat(result.getUrlForAffiliateLink()).isEqualTo(url);
        }
    }
}
