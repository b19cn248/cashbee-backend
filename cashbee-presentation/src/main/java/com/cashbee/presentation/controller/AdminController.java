package com.cashbee.presentation.controller;

import com.cashbee.application.dto.admin.SystemStatisticsResponse;
import com.cashbee.application.usecase.admin.GetSystemStatisticsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Tag(name = "Admin Dashboard", description = "APIs for admin dashboard and system management")
public class AdminController {

    private final GetSystemStatisticsUseCase getSystemStatisticsUseCase;

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
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SystemStatisticsResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
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
}
