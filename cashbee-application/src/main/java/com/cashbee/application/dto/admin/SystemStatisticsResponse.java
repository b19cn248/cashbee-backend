package com.cashbee.application.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Response DTO for system statistics.
 *
 * Provides comprehensive overview of the system for admin dashboard:
 * - User and wallet counts
 * - Balance statistics (total, locked, pending, earned, withdrawn)
 * - Payout statistics by status
 * - Transaction count
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class SystemStatisticsResponse {

    // ============================================================
    // User & Wallet Statistics
    // ============================================================

    /**
     * Total number of users in the system.
     */
    private final Long totalUsers;

    /**
     * Total number of wallets in the system.
     */
    private final Long totalWallets;

    // ============================================================
    // Balance Statistics
    // ============================================================

    /**
     * Total available balance across all wallets.
     */
    private final BigDecimal totalBalance;

    /**
     * Total locked balance across all wallets.
     * (Locked for pending payout requests)
     */
    private final BigDecimal totalLockedBalance;

    /**
     * Total pending balance across all wallets.
     * (Pending from unconfirmed transactions)
     */
    private final BigDecimal totalPendingBalance;

    /**
     * Total amount earned by all users (lifetime).
     */
    private final BigDecimal totalEarned;

    /**
     * Total amount withdrawn by all users (lifetime).
     */
    private final BigDecimal totalWithdrawn;

    // ============================================================
    // Payout Statistics
    // ============================================================

    /**
     * Number of payout requests with REQUESTED status.
     */
    private final Long payoutsRequested;

    /**
     * Number of payout requests with PROCESSING status.
     */
    private final Long payoutsProcessing;

    /**
     * Number of payout requests with PAID status.
     */
    private final Long payoutsPaid;

    /**
     * Number of payout requests with REJECTED status.
     */
    private final Long payoutsRejected;

    /**
     * Number of payout requests with CANCELLED status.
     */
    private final Long payoutsCancelled;

    /**
     * Total number of all payout requests.
     */
    private final Long totalPayouts;

    // ============================================================
    // Transaction Statistics
    // ============================================================

    /**
     * Total number of transactions in the system.
     */
    private final Long totalTransactions;
}
