package com.cashbee.domain.model;

import com.cashbee.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffiliateOrder domain model.
 *
 * Following TDD approach - tests written BEFORE implementation.
 *
 * @author CashBee Team
 */
@DisplayName("AffiliateOrder Domain Model Tests")
class AffiliateOrderTest {

    @Test
    @DisplayName("Should create order with default status PENDING")
    void shouldCreateOrderWithDefaultStatus() {
        // Given
        var order = AffiliateOrder.builder()
            .platformId(1L)
            .userId(100L)
            .orderId("SHOPEE_12345")
            .commissionAmount(new BigDecimal("50000.00"))
            .orderTime(LocalDateTime.now())
            .build();

        // Then
        assertNotNull(order);
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    @DisplayName("Should approve order")
    void shouldApproveOrder() {
        // Given
        var order = createPendingOrder();

        // When
        order.approve();

        // Then
        assertEquals(OrderStatus.APPROVED, order.getOrderStatus());
        assertNotNull(order.getConfirmTime());
    }

    @Test
    @DisplayName("Should mark order as paid")
    void shouldMarkOrderAsPaid() {
        // Given
        var order = createApprovedOrder();

        // When
        order.markAsPaid();

        // Then
        assertEquals(OrderStatus.PAID, order.getOrderStatus());
        assertNotNull(order.getPaidTime());
    }

    @Test
    @DisplayName("Should cancel order")
    void shouldCancelOrder() {
        // Given
        var order = createPendingOrder();

        // When
        order.cancel();

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    @DisplayName("Should reject order")
    void shouldRejectOrder() {
        // Given
        var order = createPendingOrder();

        // When
        order.reject();

        // Then
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
    }

    @Test
    @DisplayName("Should check if order is pending")
    void shouldCheckIfPending() {
        // Given
        var order = createPendingOrder();

        // Then
        assertTrue(order.isPending());
        assertFalse(order.isApproved());
        assertFalse(order.isPaid());
    }

    @Test
    @DisplayName("Should check if order is approved")
    void shouldCheckIfApproved() {
        // Given
        var order = createApprovedOrder();

        // Then
        assertTrue(order.isApproved());
        assertFalse(order.isPending());
        assertFalse(order.isPaid());
    }

    @Test
    @DisplayName("Should check if order is paid")
    void shouldCheckIfPaid() {
        // Given
        var order = createPaidOrder();

        // Then
        assertTrue(order.isPaid());
        assertFalse(order.isPending());
        assertFalse(order.isApproved());
    }

    @Test
    @DisplayName("Should check if order is cancelled or rejected")
    void shouldCheckIfCancelledOrRejected() {
        // Given
        var cancelledOrder = createPendingOrder();
        cancelledOrder.cancel();

        var rejectedOrder = createPendingOrder();
        rejectedOrder.reject();

        // Then
        assertTrue(cancelledOrder.isCancelled());
        assertTrue(rejectedOrder.isRejected());
    }

    @Test
    @DisplayName("Should check if order can receive cashback")
    void shouldCheckIfCanReceiveCashback() {
        // Only APPROVED and PAID orders can receive cashback
        var pendingOrder = createPendingOrder();
        var approvedOrder = createApprovedOrder();
        var paidOrder = createPaidOrder();
        var cancelledOrder = createPendingOrder();
        cancelledOrder.cancel();

        assertFalse(pendingOrder.canReceiveCashback());
        assertTrue(approvedOrder.canReceiveCashback());
        assertTrue(paidOrder.canReceiveCashback());
        assertFalse(cancelledOrder.canReceiveCashback());
    }

    @Test
    @DisplayName("Should validate order data")
    void shouldValidateOrderData() {
        // Given - Valid order
        var order = createValidOrder();

        // Then - Should not throw
        assertDoesNotThrow(() -> order.validate());
    }

    @Test
    @DisplayName("Should fail validation if orderId is blank")
    void shouldFailValidationIfOrderIdBlank() {
        // Given
        var order = AffiliateOrder.builder()
            .platformId(1L)
            .userId(100L)
            .orderId("") // Blank!
            .commissionAmount(new BigDecimal("50000.00"))
            .orderTime(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> order.validate());
    }

    @Test
    @DisplayName("Should fail validation if commission is negative")
    void shouldFailValidationIfCommissionNegative() {
        // Given
        var order = AffiliateOrder.builder()
            .platformId(1L)
            .userId(100L)
            .orderId("ORDER_123")
            .commissionAmount(new BigDecimal("-100.00")) // Negative!
            .orderTime(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> order.validate());
    }

    @Test
    @DisplayName("Should fail validation if userId is null")
    void shouldFailValidationIfUserIdNull() {
        // Given
        var order = AffiliateOrder.builder()
            .platformId(1L)
            .userId(null) // Null!
            .orderId("ORDER_123")
            .commissionAmount(new BigDecimal("50000.00"))
            .orderTime(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> order.validate());
    }

    @Test
    @DisplayName("Should check if order is from import")
    void shouldCheckIfFromImport() {
        // Given
        var importedOrder = AffiliateOrder.builder()
            .orderId("ORDER_123")
            .source("IMPORT")
            .build();

        var apiOrder = AffiliateOrder.builder()
            .orderId("ORDER_456")
            .source("API")
            .build();

        // Then
        assertTrue(importedOrder.isFromImport());
        assertFalse(apiOrder.isFromImport());
    }

    @Test
    @DisplayName("Should soft delete order")
    void shouldSoftDeleteOrder() {
        // Given
        var order = createValidOrder();

        // When
        order.delete();

        // Then
        assertTrue(order.isDeleted());
        assertNotNull(order.getDeletedAt());
    }

    @Test
    @DisplayName("Should restore deleted order")
    void shouldRestoreDeletedOrder() {
        // Given
        var order = createValidOrder();
        order.delete();

        // When
        order.restore();

        // Then
        assertFalse(order.isDeleted());
        assertNull(order.getDeletedAt());
    }

    // ===== Helper Methods =====

    private AffiliateOrder createPendingOrder() {
        return AffiliateOrder.builder()
            .platformId(1L)
            .userId(100L)
            .orderId("ORDER_PENDING")
            .orderStatus(OrderStatus.PENDING)
            .commissionAmount(new BigDecimal("50000.00"))
            .currency("VND")
            .orderTime(LocalDateTime.now())
            .source("IMPORT")
            .build();
    }

    private AffiliateOrder createApprovedOrder() {
        var order = createPendingOrder();
        order.setOrderId("ORDER_APPROVED");
        order.approve();
        return order;
    }

    private AffiliateOrder createPaidOrder() {
        var order = createApprovedOrder();
        order.setOrderId("ORDER_PAID");
        order.markAsPaid();
        return order;
    }

    private AffiliateOrder createValidOrder() {
        return AffiliateOrder.builder()
            .platformId(1L)
            .userId(100L)
            .clickId("CLICK_12345")
            .orderId("ORDER_VALID_123")
            .orderStatus(OrderStatus.PENDING)
            .productName("iPhone 15 Pro Max")
            .productPrice(new BigDecimal("29990000.00"))
            .commissionAmount(new BigDecimal("1499500.00"))
            .currency("VND")
            .orderTime(LocalDateTime.now())
            .source("IMPORT")
            .build();
    }
}
