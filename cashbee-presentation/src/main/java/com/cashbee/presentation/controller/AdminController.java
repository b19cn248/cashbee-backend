package com.cashbee.presentation.controller;

import com.cashbee.application.dto.admin.SystemStatisticsResponse;
import com.cashbee.application.dto.affiliate.AffiliateClickResponse;
import com.cashbee.application.dto.affiliate.GetAffiliateClicksQuery;
import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.user.GetUsersQuery;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.usecase.admin.GetSystemStatisticsUseCase;
import com.cashbee.application.usecase.affiliate.GetAffiliateClicksUseCase;
import com.cashbee.application.usecase.user.GetUsersUseCase;
import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Admin Dashboard.
 *
 * Provides endpoints for:
 * - System statistics and overview
 * - User management
 * - Wallet management
 * - System monitoring
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor

@Tag(name = "Admin Dashboard", description = "APIs for admin dashboard and system management")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final GetSystemStatisticsUseCase getSystemStatisticsUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final GetAffiliateClicksUseCase getAffiliateClicksUseCase;

    // ============================================================
    // SYSTEM STATISTICS
    // ============================================================

    /**
     * Get comprehensive system statistics.
     *
     * Returns overview of the entire system including:
     * - User and wallet counts
     * - Balance statistics (total, locked, pending, earned, withdrawn)
     * - Payout statistics by status
     * - Transaction counts
     *
     * Useful for admin dashboard homepage.
     *
     * @return System statistics response
     */
    @GetMapping("/dashboard/statistics")
    @Operation(
            summary = "[ADMIN] Get system statistics",
            description = "Retrieve comprehensive overview of system including users, wallets, balances, payouts, and transactions"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SystemStatisticsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<SystemStatisticsResponse> getSystemStatistics() {
        log.info("API: [ADMIN] Getting system statistics");

        SystemStatisticsResponse response = getSystemStatisticsUseCase.execute();

        log.info("API: [ADMIN] System statistics retrieved: users={}, wallets={}, balance={}, payouts={}, transactions={}",
                response.getTotalUsers(),
                response.getTotalWallets(),
                response.getTotalBalance(),
                response.getTotalPayouts(),
                response.getTotalTransactions());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // USER MANAGEMENT
    // ============================================================

    /**
     * Get all users with pagination and filtering.
     *
     * This endpoint allows Admin to:
     * - View all users in the system with pagination
     * - Filter users by status (ACTIVE, SUSPENDED, BANNED)
     * - Search users by email or username
     *
     * Example requests:
     * - GET /api/admin/users                       → All users, page 0, size 20
     * - GET /api/admin/users?page=1&size=50        → Page 1 with 50 items
     * - GET /api/admin/users?status=ACTIVE         → Only active users
     * - GET /api/admin/users?search=john@          → Search by email/username
     *
     * @param status Filter by user status (optional)
     * @param search Search keyword for email or username (optional)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated list of users
     */
    @GetMapping("/users")
    @Operation(
            summary = "[ADMIN] Get all users",
            description = "Retrieve paginated list of users with optional status filter and search"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @Parameter(description = "Filter by status (ACTIVE, SUSPENDED, BANNED)")
            @RequestParam(required = false) UserStatus status,

            @Parameter(description = "Search by email or username")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("API: [ADMIN] Getting users list (status={}, search={}, page={}, size={})",
                status, search, page, size);

        // Build query from request parameters
        GetUsersQuery query = GetUsersQuery.builder()
                .status(status)
                .search(search)
                .page(page)
                .size(size)
                .build();

        // Execute use case
        PageResponse<UserResponse> result = getUsersUseCase.execute(query);

        log.info("API: [ADMIN] Users retrieved: {} users (page {} of {})",
                result.getContent().size(), result.getPage(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ============================================================
    // AFFILIATE CLICKS MANAGEMENT
    // ============================================================

    /**
     * Get all affiliate clicks (tracking links) with pagination and filtering.
     *
     * This endpoint allows Admin to:
     * - View all tracking links created by users
     * - Filter by specific user, platform, status
     * - Search by tracking code or product name
     * - Paginate through results
     *
     * Example requests:
     * - GET /api/admin/affiliate-clicks                    → All clicks, page 0, size 20
     * - GET /api/admin/affiliate-clicks?userId=1           → Clicks by user 1
     * - GET /api/admin/affiliate-clicks?status=MATCHED     → Only matched clicks
     * - GET /api/admin/affiliate-clicks?orderMatched=true  → Clicks that resulted in orders
     * - GET /api/admin/affiliate-clicks?search=CB1_        → Search by tracking code
     *
     * @param userId Filter by specific user ID (optional)
     * @param platformId Filter by platform ID (optional)
     * @param status Filter by click status: CREATED, CLICKED, MATCHED, EXPIRED (optional)
     * @param orderMatched Filter by whether click resulted in order (optional)
     * @param search Search keyword for tracking code or product name (optional)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated list of affiliate clicks
     */
    @GetMapping("/affiliate-clicks")
    @Operation(
            summary = "[ADMIN] Get all affiliate clicks (tracking links)",
            description = "Retrieve paginated list of tracking links created by users with optional filters"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Affiliate clicks retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PageResponse<AffiliateClickResponse>>> getAffiliateClicks(
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Filter by platform ID")
            @RequestParam(required = false) Long platformId,

            @Parameter(description = "Filter by status (CREATED, CLICKED, MATCHED, EXPIRED)")
            @RequestParam(required = false) ClickStatus status,

            @Parameter(description = "Filter by order matched flag")
            @RequestParam(required = false) Boolean orderMatched,

            @Parameter(description = "Search by tracking code or product name")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("API: [ADMIN] Getting affiliate clicks (userId={}, platformId={}, status={}, orderMatched={}, search={}, page={}, size={})",
                userId, platformId, status, orderMatched, search, page, size);

        // Build query from request parameters
        GetAffiliateClicksQuery query = GetAffiliateClicksQuery.builder()
                .userId(userId)
                .platformId(platformId)
                .status(status)
                .orderMatched(orderMatched)
                .search(search)
                .page(page)
                .size(size)
                .build();

        // Execute use case
        PageResponse<AffiliateClickResponse> result = getAffiliateClicksUseCase.execute(query);

        log.info("API: [ADMIN] Affiliate clicks retrieved: {} clicks (page {} of {})",
                result.getContent().size(), result.getPage(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
