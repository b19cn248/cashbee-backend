package com.cashbee.application.usecase.admin;

import com.cashbee.application.dto.admin.SystemStatisticsResponse;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GetSystemStatisticsUseCase
 *
 * Business Scenario:
 * Admin needs to view overall system statistics including:
 * - Total users and wallets
 * - Total balance, locked, pending amounts
 * - Total earned and withdrawn
 * - Payout statistics by status
 * - Transaction counts
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetSystemStatisticsUseCase Tests (TDD)")
class GetSystemStatisticsUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GetSystemStatisticsUseCase getSystemStatisticsUseCase;

    // ============================================================
    // TEST 1: Get System Statistics - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should return complete system statistics")
    void execute_ReturnsSystemStatistics_WhenCalled() {
        // Given: Mock repository responses for statistics
        when(userRepository.count()).thenReturn(1000L);
        when(walletRepository.count()).thenReturn(950L);

        when(walletRepository.getTotalBalance()).thenReturn(new BigDecimal("50000000.00"));
        when(walletRepository.getTotalLockedBalance()).thenReturn(new BigDecimal("5000000.00"));
        when(walletRepository.getTotalPendingBalance()).thenReturn(new BigDecimal("1000000.00"));
        when(walletRepository.getTotalEarned()).thenReturn(new BigDecimal("100000000.00"));
        when(walletRepository.getTotalWithdrawn()).thenReturn(new BigDecimal("50000000.00"));

        // Payout statistics
        when(payoutRequestRepository.countByStatus(PayoutStatus.REQUESTED)).thenReturn(25L);
        when(payoutRequestRepository.countByStatus(PayoutStatus.PROCESSING)).thenReturn(10L);
        when(payoutRequestRepository.countByStatus(PayoutStatus.PAID)).thenReturn(500L);
        when(payoutRequestRepository.countByStatus(PayoutStatus.REJECTED)).thenReturn(15L);
        when(payoutRequestRepository.countByStatus(PayoutStatus.CANCELLED)).thenReturn(5L);

        when(transactionRepository.count()).thenReturn(50000L);

        // When: Execute use case
        SystemStatisticsResponse response = getSystemStatisticsUseCase.execute();

        // Then: Verify all statistics are returned
        assertThat(response).isNotNull();

        // User & Wallet statistics
        assertThat(response.getTotalUsers()).isEqualTo(1000L);
        assertThat(response.getTotalWallets()).isEqualTo(950L);

        // Balance statistics
        assertThat(response.getTotalBalance()).isEqualByComparingTo(new BigDecimal("50000000.00"));
        assertThat(response.getTotalLockedBalance()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(response.getTotalPendingBalance()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.getTotalEarned()).isEqualByComparingTo(new BigDecimal("100000000.00"));
        assertThat(response.getTotalWithdrawn()).isEqualByComparingTo(new BigDecimal("50000000.00"));

        // Payout statistics
        assertThat(response.getPayoutsRequested()).isEqualTo(25L);
        assertThat(response.getPayoutsProcessing()).isEqualTo(10L);
        assertThat(response.getPayoutsPaid()).isEqualTo(500L);
        assertThat(response.getPayoutsRejected()).isEqualTo(15L);
        assertThat(response.getPayoutsCancelled()).isEqualTo(5L);
        assertThat(response.getTotalPayouts()).isEqualTo(555L);

        // Transaction statistics
        assertThat(response.getTotalTransactions()).isEqualTo(50000L);

        // Verify: All repository methods were called
        verify(userRepository, times(1)).count();
        verify(walletRepository, times(1)).count();
        verify(walletRepository, times(1)).getTotalBalance();
        verify(walletRepository, times(1)).getTotalLockedBalance();
        verify(walletRepository, times(1)).getTotalPendingBalance();
        verify(walletRepository, times(1)).getTotalEarned();
        verify(walletRepository, times(1)).getTotalWithdrawn();
        verify(payoutRequestRepository, times(5)).countByStatus(any(PayoutStatus.class));
        verify(transactionRepository, times(1)).count();
    }

    // ============================================================
    // TEST 2: System with No Data
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should return zero statistics when system has no data")
    void execute_ReturnsZeroStatistics_WhenNoData() {
        // Given: Empty system
        when(userRepository.count()).thenReturn(0L);
        when(walletRepository.count()).thenReturn(0L);

        when(walletRepository.getTotalBalance()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalLockedBalance()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalPendingBalance()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalEarned()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalWithdrawn()).thenReturn(BigDecimal.ZERO);

        when(payoutRequestRepository.countByStatus(any(PayoutStatus.class))).thenReturn(0L);
        when(transactionRepository.count()).thenReturn(0L);

        // When: Execute use case
        SystemStatisticsResponse response = getSystemStatisticsUseCase.execute();

        // Then: Verify all statistics are zero
        assertThat(response).isNotNull();
        assertThat(response.getTotalUsers()).isEqualTo(0L);
        assertThat(response.getTotalWallets()).isEqualTo(0L);
        assertThat(response.getTotalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalPayouts()).isEqualTo(0L);
        assertThat(response.getTotalTransactions()).isEqualTo(0L);
    }

    // ============================================================
    // TEST 3: Partial Data
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should handle partial data correctly")
    void execute_HandlesPartialData_Correctly() {
        // Given: System with users but no transactions/payouts
        when(userRepository.count()).thenReturn(100L);
        when(walletRepository.count()).thenReturn(100L);

        when(walletRepository.getTotalBalance()).thenReturn(new BigDecimal("10000000.00"));
        when(walletRepository.getTotalLockedBalance()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalPendingBalance()).thenReturn(BigDecimal.ZERO);
        when(walletRepository.getTotalEarned()).thenReturn(new BigDecimal("10000000.00"));
        when(walletRepository.getTotalWithdrawn()).thenReturn(BigDecimal.ZERO);

        when(payoutRequestRepository.countByStatus(any(PayoutStatus.class))).thenReturn(0L);
        when(transactionRepository.count()).thenReturn(0L);

        // When: Execute use case
        SystemStatisticsResponse response = getSystemStatisticsUseCase.execute();

        // Then: Verify statistics reflect partial data
        assertThat(response).isNotNull();
        assertThat(response.getTotalUsers()).isEqualTo(100L);
        assertThat(response.getTotalBalance()).isEqualByComparingTo(new BigDecimal("10000000.00"));
        assertThat(response.getTotalLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalPayouts()).isEqualTo(0L);
        assertThat(response.getTotalTransactions()).isEqualTo(0L);
    }

    // NOTE: These tests will FAIL because GetSystemStatisticsUseCase doesn't exist yet!
}
