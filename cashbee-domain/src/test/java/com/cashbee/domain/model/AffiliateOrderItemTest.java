package com.cashbee.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffiliateOrderItem domain model.
 *
 * Following TDD approach - tests written BEFORE implementation.
 *
 * @author CashBee Team
 */
@DisplayName("AffiliateOrderItem Domain Model Tests")
class AffiliateOrderItemTest {

    @Test
    @DisplayName("Should create order item with all fields")
    void shouldCreateOrderItemWithAllFields() {
        // Given
        var item = createValidOrderItem();

        // Then
        assertNotNull(item);
        assertEquals("ITEM_12345", item.getItemId());
        assertEquals("iPhone 15 Pro Max", item.getItemName());
        assertEquals(1, item.getQuantity());
        assertEquals(new BigDecimal("29990000.00"), item.getActualAmount());
    }

    @Test
    @DisplayName("Should calculate total item value")
    void shouldCalculateTotalItemValue() {
        // Given
        var item = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .quantity(3)
            .actualAmount(new BigDecimal("100000.00"))
            .build();

        // When
        var total = item.calculateTotalValue();

        // Then
        assertEquals(new BigDecimal("300000.00"), total);
    }

    @Test
    @DisplayName("Should validate order item data")
    void shouldValidateOrderItemData() {
        // Given
        var item = createValidOrderItem();

        // Then - Should not throw
        assertDoesNotThrow(() -> item.validate());
    }

    @Test
    @DisplayName("Should fail validation if itemId is blank")
    void shouldFailValidationIfItemIdBlank() {
        // Given
        var item = AffiliateOrderItem.builder()
            .itemId("") // Blank!
            .itemName("Product")
            .quantity(1)
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> item.validate());
    }

    @Test
    @DisplayName("Should fail validation if itemName is blank")
    void shouldFailValidationIfItemNameBlank() {
        // Given
        var item = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("") // Blank!
            .quantity(1)
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> item.validate());
    }

    @Test
    @DisplayName("Should fail validation if quantity is zero or negative")
    void shouldFailValidationIfQuantityInvalid() {
        // Given
        var zeroQuantity = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .quantity(0) // Zero!
            .build();

        var negativeQuantity = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .quantity(-1) // Negative!
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> zeroQuantity.validate());
        assertThrows(IllegalStateException.class, () -> negativeQuantity.validate());
    }

    @Test
    @DisplayName("Should check if item has shop information")
    void shouldCheckIfHasShopInfo() {
        // Given
        var itemWithShop = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .shopId("SHOP_456")
            .shopName("Test Shop")
            .build();

        var itemWithoutShop = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .build();

        // Then
        assertTrue(itemWithShop.hasShopInfo());
        assertFalse(itemWithoutShop.hasShopInfo());
    }

    @Test
    @DisplayName("Should check if item has category information")
    void shouldCheckIfHasCategoryInfo() {
        // Given
        var itemWithCategory = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .categoryLv1("Electronics")
            .categoryLv2("Phones")
            .categoryLv3("Smartphones")
            .build();

        var itemWithoutCategory = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .build();

        // Then
        assertTrue(itemWithCategory.hasCategoryInfo());
        assertFalse(itemWithoutCategory.hasCategoryInfo());
    }

    @Test
    @DisplayName("Should check if item has commission information")
    void shouldCheckIfHasCommissionInfo() {
        // Given
        var itemWithCommission = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .itemCommission(new BigDecimal("5000.00"))
            .build();

        var itemWithoutCommission = AffiliateOrderItem.builder()
            .itemId("ITEM_123")
            .itemName("Product")
            .build();

        // Then
        assertTrue(itemWithCommission.hasCommissionInfo());
        assertFalse(itemWithoutCommission.hasCommissionInfo());
    }

    // ===== Helper Methods =====

    private AffiliateOrderItem createValidOrderItem() {
        return AffiliateOrderItem.builder()
            .orderId(1L)
            .itemId("ITEM_12345")
            .itemName("iPhone 15 Pro Max")
            .quantity(1)
            .actualAmount(new BigDecimal("29990000.00"))
            .itemCommission(new BigDecimal("1499500.00"))
            .shopId("SHOP_789")
            .shopName("Apple Official Store")
            .categoryLv1("Electronics")
            .categoryLv2("Mobile Phones")
            .categoryLv3("Smartphones")
            .imgUrl("https://example.com/iphone15.jpg")
            .brandCommissionRate(new BigDecimal("5.00"))
            .platformCommissionRate(new BigDecimal("5.00"))
            .build();
    }
}
