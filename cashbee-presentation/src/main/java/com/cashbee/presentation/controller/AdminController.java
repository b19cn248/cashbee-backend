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
import com.cashbee.application.usecase.wallet.RecalculateWalletUseCase;
import com.cashbee.application.usecase.referral.SyncAllUsersCompletedOrdersUseCase;
import com.cashbee.application.usecase.referral.GrantMissingReferrerCommissionsUseCase;
import com.cashbee.application.usecase.referral.ProcessReferralMilestoneUseCase;
import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.repository.CashbackRepository;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    private final RecalculateWalletUseCase recalculateWalletUseCase;
    private final SyncAllUsersCompletedOrdersUseCase syncAllUsersCompletedOrdersUseCase;
    private final GrantMissingReferrerCommissionsUseCase grantMissingReferrerCommissionsUseCase;
    private final ProcessReferralMilestoneUseCase processReferralMilestoneUseCase;
    private final CashbackRepository cashbackRepository;

    /**
     * Minimum order amount for milestone processing (anti-abuse).
     * Must match the constant in ProcessReferralMilestoneUseCase.
     */
    private static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("100000");

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
     * - Filter users who have orders in a specific date range
     *
     * Example requests:
     * - GET /api/admin/users                                    → All users, page 0, size 20
     * - GET /api/admin/users?page=1&size=50                     → Page 1 with 50 items
     * - GET /api/admin/users?status=ACTIVE                      → Only active users
     * - GET /api/admin/users?search=john@                       → Search by email/username
     * - GET /api/admin/users?orderFromDate=2025-12-18&orderToDate=2025-12-18  → Users with orders today
     * - GET /api/admin/users?orderFromDate=2025-12-15           → Users with orders from Dec 15
     *
     * @param status Filter by user status (optional)
     * @param search Search keyword for email or username (optional)
     * @param orderFromDate Filter users with orders FROM this date (optional, format: YYYY-MM-DD)
     * @param orderToDate Filter users with orders TO this date (optional, format: YYYY-MM-DD)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated list of users
     */
    @GetMapping("/users")
    @Operation(
            summary = "[ADMIN] Get all users",
            description = "Retrieve paginated list of users with optional filters: status, search, and order date range"
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

            @Parameter(description = "Filter users with orders FROM this date (format: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderFromDate,

            @Parameter(description = "Filter users with orders TO this date (format: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderToDate,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("API: [ADMIN] Getting users list (status={}, search={}, orderFromDate={}, orderToDate={}, page={}, size={})",
                status, search, orderFromDate, orderToDate, page, size);

        // Build query from request parameters
        GetUsersQuery query = GetUsersQuery.builder()
                .status(status)
                .search(search)
                .orderFromDate(orderFromDate)
                .orderToDate(orderToDate)
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

    // ============================================================
    // WALLET MANAGEMENT
    // ============================================================

    /**
     * Recalculate wallet balances for a specific user.
     *
     * This endpoint recalculates wallet balances from cashback data (source of truth):
     * - pending_balance = SUM(cashback_amount) WHERE status = PENDING
     * - balance = SUM(cashback_amount) WHERE status = CONFIRMED
     * - total_earned = SUM(cashback_amount) WHERE status IN (CONFIRMED, PAID)
     *
     * Use this to fix wallet inconsistencies after import issues or data corruption.
     *
     * Example: POST /api/admin/wallets/recalculate?userId=20
     *
     * @param userId User ID to recalculate wallet for
     * @return Success message
     */
    @PostMapping("/wallets/recalculate")
    @Operation(
            summary = "[ADMIN] Recalculate wallet balance for a user",
            description = "Recalculate wallet balances from cashback data. Use this to fix wallet inconsistencies."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Wallet recalculated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<String>> recalculateWallet(
            @Parameter(description = "User ID to recalculate wallet for", required = true)
            @RequestParam Long userId
    ) {
        log.info("API: [ADMIN] Recalculating wallet for user {}", userId);

        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_USER_ID", "Invalid user ID"));
        }

        recalculateWalletUseCase.execute(userId);

        log.info("API: [ADMIN] Wallet recalculated successfully for user {}", userId);

        return ResponseEntity.ok(ApiResponse.success("Wallet recalculated successfully for user " + userId));
    }

    // ============================================================
    // REFERRAL DATA SYNC
    // ============================================================

    /**
     * Sync total_completed_orders for all users.
     *
     * This endpoint recalculates the total_completed_orders count for all users
     * based on distinct orders with CONFIRMED or PAID cashback status.
     *
     * Use this to fix data after:
     * - Re-import causing double counting
     * - Orders imported before milestone tracking was implemented
     *
     * Example: POST /api/admin/users/sync-completed-orders
     *
     * @return Number of users updated
     */
    @PostMapping("/users/sync-completed-orders")
    @Operation(
            summary = "[ADMIN] Sync completed orders count for all users",
            description = "Recalculate total_completed_orders for all users from cashback data"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Sync completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<String>> syncAllUsersCompletedOrders() {
        log.info("API: [ADMIN] Starting sync of total_completed_orders for all users");

        int updatedCount = syncAllUsersCompletedOrdersUseCase.execute();

        log.info("API: [ADMIN] Sync completed: {} users updated", updatedCount);

        return ResponseEntity.ok(ApiResponse.success("Sync completed: " + updatedCount + " users updated"));
    }

    // ============================================================
    // REFERRER COMMISSION FIX
    // ============================================================

    /**
     * Grant missing referrer commissions (Retroactive Fix).
     *
     * This endpoint processes all CONFIRMED referrer commissions that were
     * recorded in the database but never paid to referrers' wallets.
     *
     * This is a ONE-TIME fix for historical data due to a bug where the 5%
     * referrer commission was tracked but never actually added to wallet balance.
     *
     * Example: POST /api/admin/referral/grant-missing-commissions
     *
     * @return Summary of the retroactive fix operation
     */
    @PostMapping("/referral/grant-missing-commissions")
    @Operation(
            summary = "[ADMIN] Grant missing referrer commissions (Retroactive Fix)",
            description = "Process all CONFIRMED commissions that were never paid to referrers' wallets. One-time fix for historical data."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Retroactive fix completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<GrantMissingReferrerCommissionsUseCase.GrantResult>> grantMissingCommissions() {
        log.info("API: [ADMIN] Starting retroactive fix for missing referrer commissions");

        GrantMissingReferrerCommissionsUseCase.GrantResult result =
                grantMissingReferrerCommissionsUseCase.execute();

        log.info("API: [ADMIN] Retroactive fix completed: found={}, success={}, skipped={}, errors={}, totalPaid={}",
                result.totalFound(), result.successCount(), result.skippedCount(),
                result.errorCount(), result.totalAmountPaid());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ============================================================
    // MILESTONE RE-PROCESSING
    // ============================================================

    /**
     * Re-process milestones for all users with qualifying orders.
     *
     * This endpoint finds all users who have at least one order with product_price > 100k
     * and re-processes their milestones according to the new MVP configuration.
     *
     * Use this after:
     * - Migration 050 (MVP referral milestones) to grant new bonuses
     * - Any milestone configuration changes
     *
     * Example: POST /api/admin/referral/reprocess-milestones
     *
     * @return Summary of the re-processing operation
     */
    @PostMapping("/referral/reprocess-milestones")
    @Operation(
            summary = "[ADMIN] Re-process milestones for all qualifying users",
            description = "Process milestones for all users with orders > 100k. Use after milestone config changes."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Re-processing completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> reprocessMilestones() {
        log.info("API: [ADMIN] Re-processing milestones for all qualifying users");

        // Get all users with qualifying orders (> 100k, CONFIRMED/PAID)
        List<Long> userIds = cashbackRepository.findUsersWithQualifyingOrders(MIN_ORDER_AMOUNT);

        log.info("API: [ADMIN] Found {} users with qualifying orders", userIds.size());

        int processed = 0;
        int errors = 0;

        for (Long userId : userIds) {
            try {
                // Use reprocessAllMilestones instead of execute
                // execute() only checks EXACT milestone match
                // reprocessAllMilestones() finds ALL qualified milestones (orders_required <= completedOrders)
                processReferralMilestoneUseCase.reprocessAllMilestones(userId);

                // FIX: Recalculate wallet to include referrer commission
                // This ensures balance is calculated from source of truth:
                // - CONFIRMED cashback
                // - Unpaid milestone bonus (referral_reward)
                // - Unpaid referrer commission (referrer_commission)
                recalculateWalletUseCase.execute(userId);

                processed++;
            } catch (Exception e) {
                log.error("Failed to process milestone for user {}: {}", userId, e.getMessage());
                errors++;
            }
        }

        Map<String, Object> result = Map.of(
                "totalUsers", userIds.size(),
                "processed", processed,
                "errors", errors
        );

        log.info("API: [ADMIN] Milestone re-processing completed: totalUsers={}, processed={}, errors={}",
                userIds.size(), processed, errors);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
