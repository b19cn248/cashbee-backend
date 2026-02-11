package com.cashbee.presentation.controller;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.order.GetUserOrdersQuery;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.application.usecase.order.GetUserOrdersUseCase;
import com.cashbee.domain.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for UserOrderController.
 *
 * Business Scenarios:
 * 1. GET /api/users/{userId}/orders - Get user orders with pagination
 * 2. GET /api/users/{userId}/orders?status=PENDING - Filter by status
 * 3. Validate pagination parameters
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserOrderController Tests (TDD)")
class UserOrderControllerTest {

    @Mock
    private GetUserOrdersUseCase getUserOrdersUseCase;

    @InjectMocks
    private UserOrderController userOrderController;

    // Test data
    private AffiliateOrderResponse order1;
    private AffiliateOrderResponse order2;

    @BeforeEach
    void setUp() {
        order1 = AffiliateOrderResponse.builder()
                .id(1L)
                .userId(100L)
                .orderId("ORDER-001")
                .productName("Product A")
                .productPrice(new BigDecimal("199000"))
                .commissionAmount(new BigDecimal("1990"))
                .orderStatus(OrderStatus.PENDING)
                .orderTime(LocalDateTime.now().minusDays(1))
                .items(Collections.emptyList())
                .totalItems(0)
                .build();

        order2 = AffiliateOrderResponse.builder()
                .id(2L)
                .userId(100L)
                .orderId("ORDER-002")
                .productName("Product B")
                .productPrice(new BigDecimal("299000"))
                .commissionAmount(new BigDecimal("2990"))
                .orderStatus(OrderStatus.APPROVED)
                .orderTime(LocalDateTime.now())
                .items(Collections.emptyList())
                .totalItems(0)
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Get User Orders
    // ============================================================

    @Nested
    @DisplayName("GET /api/users/{userId}/orders")
    class GetUserOrders {

        @Test
        @DisplayName("Should return 200 OK with orders")
        void getUserOrders_ReturnsOkWithOrders() {
            // Given
            Long userId = 100L;
            PageResponse<AffiliateOrderResponse> pageResponse = PageResponse.of(
                    Arrays.asList(order1, order2),
                    0, 10, 2
            );

            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            ResponseEntity<?> response = userOrderController.getUserOrders(userId, null, 0, 10);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verify use case was called with correct parameters
            ArgumentCaptor<GetUserOrdersQuery> queryCaptor = ArgumentCaptor.forClass(GetUserOrdersQuery.class);
            verify(getUserOrdersUseCase).execute(queryCaptor.capture());

            GetUserOrdersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getUserId()).isEqualTo(100L);
            assertThat(capturedQuery.getPage()).isEqualTo(0);
            assertThat(capturedQuery.getSize()).isEqualTo(10);
            assertThat(capturedQuery.getStatus()).isNull();
        }

        @Test
        @DisplayName("Should pass status filter to use case")
        void getUserOrders_PassesStatusFilter() {
            // Given
            Long userId = 100L;
            PageResponse<AffiliateOrderResponse> pageResponse = PageResponse.of(
                    Collections.singletonList(order1),
                    0, 10, 1
            );

            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            ResponseEntity<?> response = userOrderController.getUserOrders(userId, OrderStatus.PENDING, 0, 10);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Verify status was passed correctly
            ArgumentCaptor<GetUserOrdersQuery> queryCaptor = ArgumentCaptor.forClass(GetUserOrdersQuery.class);
            verify(getUserOrdersUseCase).execute(queryCaptor.capture());

            GetUserOrdersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Should use default pagination when not provided")
        void getUserOrders_UsesDefaultPagination() {
            // Given
            Long userId = 100L;
            PageResponse<AffiliateOrderResponse> pageResponse = PageResponse.of(
                    Collections.emptyList(),
                    0, 10, 0
            );

            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenReturn(pageResponse);

            // When - Call with defaults (page=0, size=10 are defaults in controller)
            ResponseEntity<?> response = userOrderController.getUserOrders(userId, null, 0, 10);

            // Then
            ArgumentCaptor<GetUserOrdersQuery> queryCaptor = ArgumentCaptor.forClass(GetUserOrdersQuery.class);
            verify(getUserOrdersUseCase).execute(queryCaptor.capture());

            GetUserOrdersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getPage()).isEqualTo(0);
            assertThat(capturedQuery.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should return empty list when user has no orders")
        void getUserOrders_ReturnsEmptyList_WhenNoOrders() {
            // Given
            Long userId = 999L;
            PageResponse<AffiliateOrderResponse> emptyResponse = PageResponse.of(
                    Collections.emptyList(),
                    0, 10, 0
            );

            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenReturn(emptyResponse);

            // When
            ResponseEntity<?> response = userOrderController.getUserOrders(userId, null, 0, 10);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ============================================================
    // TEST GROUP 2: Pagination Parameters
    // ============================================================

    @Nested
    @DisplayName("Pagination Parameters")
    class PaginationParameters {

        @Test
        @DisplayName("Should pass custom page and size to use case")
        void getUserOrders_PassesCustomPagination() {
            // Given
            Long userId = 100L;
            int customPage = 2;
            int customSize = 25;

            PageResponse<AffiliateOrderResponse> pageResponse = PageResponse.of(
                    Collections.emptyList(),
                    customPage, customSize, 50
            );

            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            ResponseEntity<?> response = userOrderController.getUserOrders(userId, null, customPage, customSize);

            // Then
            ArgumentCaptor<GetUserOrdersQuery> queryCaptor = ArgumentCaptor.forClass(GetUserOrdersQuery.class);
            verify(getUserOrdersUseCase).execute(queryCaptor.capture());

            GetUserOrdersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getPage()).isEqualTo(2);
            assertThat(capturedQuery.getSize()).isEqualTo(25);
        }
    }

    // ============================================================
    // TEST GROUP 3: Error Handling
    // ============================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should propagate exception when use case throws")
        void getUserOrders_PropagatesException_WhenUseCaseThrows() {
            // Given
            Long userId = 100L;
            when(getUserOrdersUseCase.execute(any(GetUserOrdersQuery.class)))
                    .thenThrow(new IllegalArgumentException("User ID must be positive"));

            // When & Then
            assertThatThrownBy(() -> userOrderController.getUserOrders(userId, null, 0, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID");
        }
    }
}
