package com.cashbee.application.usecase.order;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.order.GetUserOrdersQuery;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GetUserOrdersUseCase.
 *
 * Business Scenarios:
 * 1. User views their own orders with pagination
 * 2. User filters orders by status
 * 3. Orders include their items
 * 4. Empty result when no orders
 * 5. Invalid query validation
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserOrdersUseCase Tests (TDD)")
class GetUserOrdersUseCaseTest {

    @Mock
    private AffiliateOrderRepository orderRepository;

    @Mock
    private AffiliateOrderItemRepository orderItemRepository;

    @Mock
    private AffiliatePlatformRepository platformRepository;

    @InjectMocks
    private GetUserOrdersUseCase getUserOrdersUseCase;

    // Test data
    private AffiliateOrder order1;
    private AffiliateOrder order2;
    private AffiliateOrderItem item1;
    private AffiliateOrderItem item2;

    @BeforeEach
    void setUp() {
        // Setup test orders
        order1 = AffiliateOrder.builder()
                .id(1L)
                .userId(100L)
                .platformId(1L)
                .orderId("ORDER-001")
                .productName("Product A")
                .productPrice(new BigDecimal("199000"))
                .commissionAmount(new BigDecimal("1990"))
                .orderStatus(OrderStatus.PENDING)
                .orderTime(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        order2 = AffiliateOrder.builder()
                .id(2L)
                .userId(100L)
                .platformId(1L)
                .orderId("ORDER-002")
                .productName("Product B")
                .productPrice(new BigDecimal("299000"))
                .commissionAmount(new BigDecimal("2990"))
                .orderStatus(OrderStatus.APPROVED)
                .orderTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // Setup test items
        item1 = AffiliateOrderItem.builder()
                .id(101L)
                .orderId(1L)
                .itemId("ITEM-001")
                .itemName("Item A")
                .quantity(1)
                .actualAmount(new BigDecimal("199000"))
                .itemCommission(new BigDecimal("1990"))
                .shopName("Shop A")
                .status(OrderStatus.PENDING)
                .build();

        item2 = AffiliateOrderItem.builder()
                .id(102L)
                .orderId(2L)
                .itemId("ITEM-002")
                .itemName("Item B")
                .quantity(2)
                .actualAmount(new BigDecimal("299000"))
                .itemCommission(new BigDecimal("2990"))
                .shopName("Shop B")
                .status(OrderStatus.APPROVED)
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Happy Path - Get User Orders
    // ============================================================

    @Nested
    @DisplayName("Get User Orders - Happy Path")
    class GetUserOrdersHappyPath {

        @Test
        @DisplayName("Should get user orders with pagination")
        void execute_GetsUserOrdersWithPagination_Success() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .page(0)
                    .size(10)
                    .build();

            List<AffiliateOrder> orders = Arrays.asList(order1, order2);
            Page<AffiliateOrder> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 2);

            when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(orderPage);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.singletonList(item1));
            when(orderItemRepository.findByOrderId(2L)).thenReturn(Collections.singletonList(item2));

            // When
            PageResponse<AffiliateOrderResponse> result = getUserOrdersUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            // Verify order data
            AffiliateOrderResponse firstOrder = result.getContent().get(0);
            assertThat(firstOrder.getOrderId()).isEqualTo("ORDER-001");
            assertThat(firstOrder.getUserId()).isEqualTo(100L);
            assertThat(firstOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

            // Verify items are included
            assertThat(firstOrder.getItems()).isNotNull();
            assertThat(firstOrder.getItems()).hasSize(1);
            assertThat(firstOrder.getItems().get(0).getItemId()).isEqualTo("ITEM-001");

            // Verify repository calls
            verify(orderRepository).findByUserId(eq(100L), any(Pageable.class));
            verify(orderItemRepository).findByOrderId(1L);
            verify(orderItemRepository).findByOrderId(2L);
        }

        @Test
        @DisplayName("Should get orders filtered by status")
        void execute_GetsOrdersFilteredByStatus_Success() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .status(OrderStatus.APPROVED)
                    .page(0)
                    .size(10)
                    .build();

            List<AffiliateOrder> orders = Collections.singletonList(order2);
            Page<AffiliateOrder> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

            // Note: OrderStatus.APPROVED maps to CashbackStatus.CONFIRMED in the use case
            // So we need to mock findByUserIdAndCashbackStatus instead of findByUserIdAndStatus
            when(orderRepository.findByUserIdAndCashbackStatus(eq(100L), eq(CashbackStatus.CONFIRMED), any(Pageable.class)))
                    .thenReturn(orderPage);
            when(orderItemRepository.findByOrderId(2L)).thenReturn(Collections.singletonList(item2));

            // When
            PageResponse<AffiliateOrderResponse> result = getUserOrdersUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            AffiliateOrderResponse order = result.getContent().get(0);
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);

            // Verify correct repository method was called (with CashbackStatus)
            verify(orderRepository).findByUserIdAndCashbackStatus(eq(100L), eq(CashbackStatus.CONFIRMED), any(Pageable.class));
            verify(orderRepository, never()).findByUserId(anyLong(), any(Pageable.class));
        }
    }

    // ============================================================
    // TEST GROUP 2: Empty Results
    // ============================================================

    @Nested
    @DisplayName("Empty Results")
    class EmptyResults {

        @Test
        @DisplayName("Should return empty page when user has no orders")
        void execute_ReturnsEmptyPage_WhenNoOrdersFound() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(999L)
                    .page(0)
                    .size(10)
                    .build();

            Page<AffiliateOrder> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(orderRepository.findByUserId(eq(999L), any(Pageable.class))).thenReturn(emptyPage);

            // When
            PageResponse<AffiliateOrderResponse> result = getUserOrdersUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);

            // Verify item repository was NOT called
            verify(orderItemRepository, never()).findByOrderId(anyLong());
        }
    }

    // ============================================================
    // TEST GROUP 3: Validation
    // ============================================================

    @Nested
    @DisplayName("Query Validation")
    class QueryValidation {

        @Test
        @DisplayName("Should throw exception when query is null")
        void execute_ThrowsException_WhenQueryIsNull() {
            // When & Then
            assertThatThrownBy(() -> getUserOrdersUseCase.execute(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null");

            // Verify no repository interaction
            verifyNoInteractions(orderRepository);
            verifyNoInteractions(orderItemRepository);
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void execute_ThrowsException_WhenUserIdIsNull() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(null)
                    .page(0)
                    .size(10)
                    .build();

            // When & Then
            assertThatThrownBy(() -> getUserOrdersUseCase.execute(query))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID");

            // Verify no repository interaction
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("Should normalize negative page to 0")
        void execute_NormalizesNegativePage_ToZero() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .page(-5)
                    .size(10)
                    .build();

            Page<AffiliateOrder> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(emptyPage);

            // When
            getUserOrdersUseCase.execute(query);

            // Then - verify page was normalized to 0
            verify(orderRepository).findByUserId(eq(100L), argThat(pageable ->
                    pageable.getPageNumber() == 0
            ));
        }

        @Test
        @DisplayName("Should cap page size to maximum 50")
        void execute_CapsPageSize_ToMaximum50() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .page(0)
                    .size(100) // Too large
                    .build();

            Page<AffiliateOrder> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(emptyPage);

            // When
            getUserOrdersUseCase.execute(query);

            // Then - verify size was capped to 50
            verify(orderRepository).findByUserId(eq(100L), argThat(pageable ->
                    pageable.getPageSize() == 50
            ));
        }
    }

    // ============================================================
    // TEST GROUP 4: Order Items
    // ============================================================

    @Nested
    @DisplayName("Order Items")
    class OrderItems {

        @Test
        @DisplayName("Should include item status in response")
        void execute_IncludesItemStatus_InResponse() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .page(0)
                    .size(10)
                    .build();

            List<AffiliateOrder> orders = Collections.singletonList(order1);
            Page<AffiliateOrder> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

            when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(orderPage);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.singletonList(item1));

            // When
            PageResponse<AffiliateOrderResponse> result = getUserOrdersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getItems()).hasSize(1);
            assertThat(result.getContent().get(0).getItems().get(0).getStatus())
                    .isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Should calculate total items count")
        void execute_CalculatesTotalItemsCount() {
            // Given
            GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                    .userId(100L)
                    .page(0)
                    .size(10)
                    .build();

            AffiliateOrderItem item3 = AffiliateOrderItem.builder()
                    .id(103L)
                    .orderId(1L)
                    .itemId("ITEM-003")
                    .itemName("Item C")
                    .quantity(1)
                    .status(OrderStatus.PENDING)
                    .build();

            List<AffiliateOrder> orders = Collections.singletonList(order1);
            Page<AffiliateOrder> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

            when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(orderPage);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Arrays.asList(item1, item3));

            // When
            PageResponse<AffiliateOrderResponse> result = getUserOrdersUseCase.execute(query);

            // Then
            assertThat(result.getContent().get(0).getTotalItems()).isEqualTo(2);
        }
    }
}
