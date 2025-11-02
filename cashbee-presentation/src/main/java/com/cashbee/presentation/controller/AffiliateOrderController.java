package com.cashbee.presentation.controller;

import com.cashbee.application.dto.request.CreateAffiliateOrderRequest;
import com.cashbee.application.dto.request.UpdateOrderStatusRequest;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.application.usecase.CreateAffiliateOrderUseCase;
import com.cashbee.application.usecase.GetOrderByIdUseCase;
import com.cashbee.application.usecase.GetUserOrdersUseCase;
import com.cashbee.application.usecase.UpdateOrderStatusUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Affiliate Order operations.
 * Handles endpoints for viewing and managing affiliate orders.
 *
 * @author CashBee Team
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Affiliate Orders", description = "Affiliate order management APIs")
public class AffiliateOrderController {

    private final GetUserOrdersUseCase getUserOrdersUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final CreateAffiliateOrderUseCase createAffiliateOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    /**
     * Get all orders for the current user.
     *
     * Endpoint: GET /api/orders/my
     *
     * This is the KEY endpoint for users to view their orders.
     * In production, userId would come from JWT token.
     * For now, we use a request parameter.
     */
    @GetMapping("/api/orders/my")
    @Operation(summary = "Get current user's orders", description = "Retrieve all orders for the authenticated user")
    public ResponseEntity<ApiResponse<List<AffiliateOrderResponse>>> getMyOrders(
        @RequestParam Long userId // TODO: Extract from JWT token in production
    ) {
        log.info("Getting orders for user: {}", userId);
        var orders = getUserOrdersUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Get a specific order by ID.
     *
     * Endpoint: GET /api/orders/{orderId}
     */
    @GetMapping("/api/orders/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order with all its items")
    public ResponseEntity<ApiResponse<AffiliateOrderResponse>> getOrderById(
        @PathVariable Long orderId
    ) {
        log.info("Getting order: {}", orderId);
        var order = getOrderByIdUseCase.execute(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * Create a new affiliate order (ADMIN only).
     *
     * Endpoint: POST /api/admin/orders
     *
     * This endpoint is used for:
     * 1. Manual order creation by admin
     * 2. Importing orders from affiliate platforms
     */
    @PostMapping("/api/admin/orders")
    @Operation(summary = "Create new order", description = "Create a new affiliate order (admin only)")
    public ResponseEntity<ApiResponse<AffiliateOrderResponse>> createOrder(
        @Valid @RequestBody CreateAffiliateOrderRequest request
    ) {
        log.info("Creating new order: {}", request.getOrderId());
        var order = createAffiliateOrderUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(order));
    }

    /**
     * Update order status (ADMIN only).
     *
     * Endpoint: PUT /api/admin/orders/{orderId}/status
     *
     * This endpoint allows admin to:
     * - Approve pending orders
     * - Mark orders as paid
     * - Cancel or reject orders
     */
    @PutMapping("/api/admin/orders/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order (admin only)")
    public ResponseEntity<ApiResponse<AffiliateOrderResponse>> updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        log.info("Updating order {} status to: {}", orderId, request.getNewStatus());
        var order = updateOrderStatusUseCase.execute(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
