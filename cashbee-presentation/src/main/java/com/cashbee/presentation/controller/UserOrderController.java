package com.cashbee.presentation.controller;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.order.GetUserOrdersQuery;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.application.usecase.order.GetUserOrdersUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User Order operations.
 *
 * Endpoints:
 * - GET /api/orders/me - Get current user's orders from JWT token (RECOMMENDED)
 * - GET /api/users/{userId}/orders - Get user's orders by ID (for admin)
 *
 * This controller allows users to view their affiliate orders
 * and check the status of each order (PENDING, APPROVED, PAID, CANCELLED).
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor

@Tag(name = "User Orders", description = "Endpoints for users to view their affiliate orders")
public class UserOrderController {
    private static final Logger log = LoggerFactory.getLogger(UserOrderController.class);

    private final GetUserOrdersUseCase getUserOrdersUseCase;
    private final SecurityUtils securityUtils;

    // ============================================================
    // ENDPOINT 1: GET /api/orders/me (RECOMMENDED for frontend/mobile)
    // ============================================================

    /**
     * Get current user's orders using JWT token.
     *
     * This is the RECOMMENDED endpoint for frontend/mobile apps because:
     * - User ID is automatically extracted from JWT token (secure)
     * - User cannot access other users' orders
     * - No need to pass userId in URL
     *
     * How it works:
     * 1. Frontend sends request with Authorization header: "Bearer <token>"
     * 2. Spring Security validates the JWT token
     * 3. We extract userId from the token using SecurityUtils
     * 4. We fetch orders for that userId
     *
     * Query Parameters:
     * - status: Filter by order status (PENDING, APPROVED, PAID, CANCELLED) - optional
     * - page: Page number (0-indexed), default = 0
     * - size: Page size (max 50), default = 10
     *
     * Example requests:
     * - GET /api/orders/me                    - Get all my orders
     * - GET /api/orders/me?status=PENDING     - Get only my PENDING orders
     * - GET /api/orders/me?page=1&size=20     - Get page 2 with 20 items
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param status Optional status filter
     * @param page Page number (default: 0)
     * @param size Page size (default: 10, max: 50)
     * @return Paginated response with orders and their items
     */
    @GetMapping("/orders/me")
    @Operation(
            summary = "Get my orders",
            description = "Get current authenticated user's orders. " +
                    "User ID is automatically extracted from JWT token. " +
                    "Returns orders sorted by order time (newest first)."
    )
    public ResponseEntity<ApiResponse<PageResponse<AffiliateOrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,

            @Parameter(description = "Filter by order status (PENDING, APPROVED, PAID, CANCELLED)")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 50)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        // Step 1: Extract userId from JWT token
        // SecurityUtils reads the JWT claims and finds our internal userId
        Long userId = securityUtils.getCurrentUserId(jwt);

        log.info("API: Getting orders for current user {} (status: {}, page: {}, size: {})",
                userId, status, page, size);

        // Step 2: Build query with extracted userId
        GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                .userId(userId)
                .status(status)
                .page(page)
                .size(size)
                .build();

        // Step 3: Execute existing use case (reuse business logic)
        PageResponse<AffiliateOrderResponse> orderPage = getUserOrdersUseCase.execute(query);

        log.info("API: Returning {} orders for current user {} (page {} of {})",
                orderPage.getContent().size(),
                userId,
                orderPage.getPage(),
                orderPage.getTotalPages());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(orderPage));
    }

    // ============================================================
    // ENDPOINT 2: GET /api/users/{userId}/orders (for admin)
    // ============================================================

    /**
     * Get orders for a specific user with pagination.
     *
     * Returns list of orders sorted by orderTime DESC (newest first).
     * Each order includes its items with full details.
     *
     * Query Parameters:
     * - status: Filter by order status (PENDING, APPROVED, PAID, CANCELLED) - optional
     * - page: Page number (0-indexed), default = 0
     * - size: Page size (max 50), default = 10
     *
     * Example:
     * - GET /api/users/100/orders                    - Get all orders for user 100
     * - GET /api/users/100/orders?status=PENDING    - Get only PENDING orders
     * - GET /api/users/100/orders?page=1&size=20    - Get page 2 with 20 items
     *
     * @param userId User ID
     * @param status Optional status filter
     * @param page Page number (default: 0)
     * @param size Page size (default: 10, max: 50)
     * @return Paginated response with orders and their items
     */
    @GetMapping("/users/{userId}/orders")
    @Operation(
            summary = "Get user orders",
            description = "Get paginated orders for a user with optional status filter. " +
                    "Returns orders sorted by order time (newest first), including order items."
    )
    public ResponseEntity<ApiResponse<PageResponse<AffiliateOrderResponse>>> getUserOrders(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,

            @Parameter(description = "Filter by order status (PENDING, APPROVED, PAID, CANCELLED)")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 50)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: Getting orders for user {} (status: {}, page: {}, size: {})",
                userId, status, page, size);

        // Build query with all parameters
        GetUserOrdersQuery query = GetUserOrdersQuery.builder()
                .userId(userId)
                .status(status)
                .page(page)
                .size(size)
                .build();

        // Execute use case
        PageResponse<AffiliateOrderResponse> orderPage = getUserOrdersUseCase.execute(query);

        log.info("API: Returning {} orders for user {} (page {} of {})",
                orderPage.getContent().size(),
                userId,
                orderPage.getPage(),
                orderPage.getTotalPages());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(orderPage));
    }
}
