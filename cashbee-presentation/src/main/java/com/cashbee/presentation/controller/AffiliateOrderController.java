package com.cashbee.presentation.controller;

import com.cashbee.application.dto.request.CreateAffiliateOrderRequest;
import com.cashbee.application.dto.request.UpdateOrderStatusRequest;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.application.usecase.CreateAffiliateOrderUseCase;
import com.cashbee.application.usecase.GetOrderByIdUseCase;
import com.cashbee.application.usecase.UpdateOrderStatusUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Affiliate Order operations (Admin).
 * Handles admin endpoints for managing affiliate orders.
 *
 * NOTE: For user-facing order listing, use UserOrderController instead
 * at GET /api/users/{userId}/orders
 *
 * @author CashBee Team
 */

@RestController
@RequiredArgsConstructor
@Tag(name = "Affiliate Orders (Admin)", description = "Admin APIs for affiliate order management")
public class AffiliateOrderController {
    private static final Logger log = LoggerFactory.getLogger(AffiliateOrderController.class);

    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final CreateAffiliateOrderUseCase createAffiliateOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

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
