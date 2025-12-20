package com.cashbee.application.util.affiliate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ShopeeFoodAffiliateLinkBuilder.
 *
 * Tests affiliate link generation for ShopeeFood URLs using the endpoint:
 * https://shopeefood.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={TRACKING}
 *
 * @author CashBee Team
 */
@DisplayName("ShopeeFoodAffiliateLinkBuilder")
class ShopeeFoodAffiliateLinkBuilderTest {

    private ShopeeFoodAffiliateLinkBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ShopeeFoodAffiliateLinkBuilder();
    }

    @Nested
    @DisplayName("build() method")
    class BuildMethod {

        @Test
        @DisplayName("Should build affiliate link from ShopeeFood detail URL")
        void shouldBuildAffiliateLinkFromDetailUrl() {
            // Given
            String originalUrl = "https://shopeefood.vn/now-food/cheap-meal/detail?itemId=278729114&restaurantId=1158256";
            String affiliateId = "14354840000";
            String trackingCode = "CB1_100_20251220";

            // When
            String result = builder.build(originalUrl, affiliateId, trackingCode);

            // Then
            assertThat(result).startsWith("https://shopeefood.vn/an_redir?origin_link=");
            assertThat(result).contains("affiliate_id=14354840000");
            assertThat(result).contains("sub_id=CB1_100_20251220");
            // URL should be encoded
            assertThat(result).contains("https%3A%2F%2Fshopeefood.vn");
        }

        @Test
        @DisplayName("Should build affiliate link from ShopeeFood restaurant URL")
        void shouldBuildAffiliateLinkFromRestaurantUrl() {
            // Given
            String originalUrl = "https://shopeefood.vn/ha-noi/link-food-trung-van";
            String affiliateId = "14354840000";
            String trackingCode = "CB1_user123_campaign1";

            // When
            String result = builder.build(originalUrl, affiliateId, trackingCode);

            // Then
            assertThat(result).startsWith("https://shopeefood.vn/an_redir?origin_link=");
            assertThat(result).contains("affiliate_id=14354840000");
            assertThat(result).contains("sub_id=CB1_user123_campaign1");
        }

        @Test
        @DisplayName("Should URL encode the origin link properly")
        void shouldUrlEncodeOriginLinkProperly() {
            // Given
            String originalUrl = "https://shopeefood.vn/ho-chi-minh/cơm-tấm-bà-năm?query=test";
            String affiliateId = "123";
            String trackingCode = "track1";

            // When
            String result = builder.build(originalUrl, affiliateId, trackingCode);

            // Then: Special characters should be encoded
            assertThat(result).doesNotContain("?query=test"); // Query should be encoded
            assertThat(result).contains("%3F"); // ? encoded
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should throw exception for null URL")
        void shouldThrowExceptionForNullUrl() {
            assertThatThrownBy(() -> builder.build(null, "123", "track"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL is required");
        }

        @Test
        @DisplayName("Should throw exception for blank URL")
        void shouldThrowExceptionForBlankUrl() {
            assertThatThrownBy(() -> builder.build("  ", "123", "track"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL is required");
        }

        @Test
        @DisplayName("Should throw exception for non-ShopeeFood URL")
        void shouldThrowExceptionForNonShopeeFoodUrl() {
            assertThatThrownBy(() -> builder.build("https://shopee.vn/product/123/456", "123", "track"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ShopeeFood URL");
        }

        @Test
        @DisplayName("Should throw exception for null affiliate ID")
        void shouldThrowExceptionForNullAffiliateId() {
            assertThatThrownBy(() -> builder.build("https://shopeefood.vn/ha-noi/restaurant", null, "track"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Affiliate ID is required");
        }

        @Test
        @DisplayName("Should throw exception for null tracking code")
        void shouldThrowExceptionForNullTrackingCode() {
            assertThatThrownBy(() -> builder.build("https://shopeefood.vn/ha-noi/restaurant", "123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking code is required");
        }
    }

    @Nested
    @DisplayName("isValidShopeeFoodUrl() method")
    class IsValidShopeeFoodUrl {

        @Test
        @DisplayName("Should return true for shopeefood.vn URL")
        void shouldReturnTrueForShopeefoodVnUrl() {
            assertThat(builder.isValidShopeeFoodUrl("https://shopeefood.vn/ha-noi/restaurant"))
                .isTrue();
        }

        @Test
        @DisplayName("Should return true for http URL")
        void shouldReturnTrueForHttpUrl() {
            assertThat(builder.isValidShopeeFoodUrl("http://shopeefood.vn/ha-noi/restaurant"))
                .isTrue();
        }

        @Test
        @DisplayName("Should return false for shopee.vn URL")
        void shouldReturnFalseForShopeeVnUrl() {
            assertThat(builder.isValidShopeeFoodUrl("https://shopee.vn/product/123/456"))
                .isFalse();
        }

        @Test
        @DisplayName("Should return false for null URL")
        void shouldReturnFalseForNullUrl() {
            assertThat(builder.isValidShopeeFoodUrl(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for blank URL")
        void shouldReturnFalseForBlankUrl() {
            assertThat(builder.isValidShopeeFoodUrl("  ")).isFalse();
        }
    }

    @Nested
    @DisplayName("getRedirectBase() method")
    class GetRedirectBase {

        @Test
        @DisplayName("Should return ShopeeFood redirect base URL")
        void shouldReturnShopeeFoodRedirectBase() {
            assertThat(builder.getRedirectBase())
                .isEqualTo("https://shopeefood.vn/an_redir");
        }
    }
}
