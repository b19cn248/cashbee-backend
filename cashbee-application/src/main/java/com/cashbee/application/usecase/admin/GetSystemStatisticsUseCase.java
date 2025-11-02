package com.cashbee.application.usecase.admin;

import com.cashbee.application.dto.admin.SystemStatisticsResponse;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Use case for getting system statistics.
 *
 * Business Scenario:
 * Admin needs comprehensive overview of the entire system including:
 * - User and wallet counts
 * - Balance statistics (total, locked, pending, earned, withdrawn)
 * - Payout statistics by status
 * - Transaction counts
 *
 * This provides admin with real-time insights into:
 * - System health and activity
 * - Financial metrics
 * - Operational metrics (pending payouts, etc.)
 *
 * Flow:
 * 1. Query user and wallet counts
 * 2. Query balance aggregates
 * 3. Query payout statistics by status
 * 4. Query transaction count
 * 5. Build and return comprehensive statistics
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetSystemStatisticsUseCase {

    private final UserRepository userRepository;
    private final UserWalletRepository walletRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Execute get system statistics operation.
     *
     * @return System statistics response
     */
    @Transactional(readOnly = true)
    public SystemStatisticsResponse execute() {
        log.info("Getting system statistics");

        // Step 1: Get user and wallet counts
        long totalUsers = userRepository.count();
        long totalWallets = walletRepository.count();

        log.debug("User statistics: totalUsers={}, totalWallets={}", totalUsers, totalWallets);

        // Step 2: Get balance statistics
        BigDecimal totalBalance = walletRepository.getTotalBalance();
        BigDecimal totalLockedBalance = walletRepository.getTotalLockedBalance();
        BigDecimal totalPendingBalance = walletRepository.getTotalPendingBalance();
        BigDecimal totalEarned = walletRepository.getTotalEarned();
        BigDecimal totalWithdrawn = walletRepository.getTotalWithdrawn();

        log.debug("Balance statistics: total={}, locked={}, pending={}, earned={}, withdrawn={}",
                totalBalance, totalLockedBalance, totalPendingBalance, totalEarned, totalWithdrawn);

        // Step 3: Get payout statistics by status
        long payoutsRequested = payoutRequestRepository.countByStatus(PayoutStatus.REQUESTED);
        long payoutsProcessing = payoutRequestRepository.countByStatus(PayoutStatus.PROCESSING);
        long payoutsPaid = payoutRequestRepository.countByStatus(PayoutStatus.PAID);
        long payoutsRejected = payoutRequestRepository.countByStatus(PayoutStatus.REJECTED);
        long payoutsCancelled = payoutRequestRepository.countByStatus(PayoutStatus.CANCELLED);
        long totalPayouts = payoutsRequested + payoutsProcessing + payoutsPaid + payoutsRejected + payoutsCancelled;

        log.debug("Payout statistics: requested={}, processing={}, paid={}, rejected={}, cancelled={}, total={}",
                payoutsRequested, payoutsProcessing, payoutsPaid, payoutsRejected, payoutsCancelled, totalPayouts);

        // Step 4: Get transaction count
        long totalTransactions = transactionRepository.count();

        log.debug("Transaction statistics: total={}", totalTransactions);

        // Step 5: Build response
        SystemStatisticsResponse response = SystemStatisticsResponse.builder()
                // User & Wallet statistics
                .totalUsers(totalUsers)
                .totalWallets(totalWallets)
                // Balance statistics
                .totalBalance(totalBalance)
                .totalLockedBalance(totalLockedBalance)
                .totalPendingBalance(totalPendingBalance)
                .totalEarned(totalEarned)
                .totalWithdrawn(totalWithdrawn)
                // Payout statistics
                .payoutsRequested(payoutsRequested)
                .payoutsProcessing(payoutsProcessing)
                .payoutsPaid(payoutsPaid)
                .payoutsRejected(payoutsRejected)
                .payoutsCancelled(payoutsCancelled)
                .totalPayouts(totalPayouts)
                // Transaction statistics
                .totalTransactions(totalTransactions)
                .build();

        log.info("System statistics retrieved successfully: users={}, wallets={}, balance={}, payouts={}, transactions={}",
                totalUsers, totalWallets, totalBalance, totalPayouts, totalTransactions);

        return response;
    }
}
