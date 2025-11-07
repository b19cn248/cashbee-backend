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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * Get all orders for the current user with PAGINATION.
     *
     * Endpoint: GET /api/orders/my
     *
     * This is the KEY endpoint for users to view their orders.
     * Now supports pagination to prevent memory issues with large result sets.
     * In production, userId would come from JWT token.
     * For now, we use a request parameter.
     *
     * @param userId User ID (TODO: Extract from JWT token in production)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param sort Sort field (default: createdAt,desc)
     * @return Paginated list of orders
     */
    @GetMapping("/api/orders/my")
    @Operation(summary = "Get current user's orders (paginated)",
               description = "Retrieve orders for the authenticated user with pagination support")
    public ResponseEntity<ApiResponse<Page<AffiliateOrderResponse>>> getMyOrders(
        @RequestParam Long userId, // TODO: Extract from JWT token in production
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        log.info("Getting orders for user: {} (page: {}, size: {})", userId, page, size);

        // Validate and limit page size to prevent abuse
        if (size > 100) {
            size = 100;
            log.warn("Page size exceeds maximum (100), limiting to 100");
        }

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        var orders = getUserOrdersUseCase.execute(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Get all orders for the current user WITHOUT pagination (DEPRECATED).
     * Use the paginated version instead for better performance.
     *
     * @deprecated Use getMyOrders() with pagination parameters instead
     */
    @Deprecated
    @GetMapping("/api/orders/my/all")
    @Operation(summary = "Get all user's orders (deprecated)",
               description = "Retrieve all orders without pagination. Use paginated version instead.")
    public ResponseEntity<ApiResponse<List<AffiliateOrderResponse>>> getAllMyOrders(
        @RequestParam Long userId
    ) {
        log.warn("Using deprecated non-paginated endpoint for user: {}", userId);
        var orders = getUserOrdersUseCase.executeAll(userId);
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
