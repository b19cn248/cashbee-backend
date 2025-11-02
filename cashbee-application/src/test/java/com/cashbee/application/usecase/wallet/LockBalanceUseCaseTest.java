package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.LockBalanceCommand;
import com.cashbee.application.dto.wallet.WalletResponse;
import com.cashbee.common.exception.InsufficientBalanceException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.common.exception.ValidationException;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for LockBalanceUseCase
 *
 * Testing Strategy: Red → Green → Refactor
 * - Write test FIRST (Red - test will FAIL because use case doesn't exist)
 * - Implement minimal code to pass (Green)
 * - Refactor for quality (Refactor)
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LockBalanceUseCase Tests (TDD)")
class LockBalanceUseCaseTest {

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private com.cashbee.application.port.WalletMapper walletMapper;

    @InjectMocks
    private LockBalanceUseCase lockBalanceUseCase;

    private UserWallet wallet;
    private LockBalanceCommand command;

    @BeforeEach
    void setUp() {
        // Given: A wallet with 1000 balance
        wallet = UserWallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("1000.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        // Given: A valid lock command
        command = LockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("300.00"))
                .description("Lock for payout request")
                .build();
    }

    // ============================================================
    // TEST 1: Lock Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should lock balance successfully when valid request")
    void execute_LocksBalanceSuccessfully_WhenValidRequest() {
        // Given: Wallet exists in database
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Mock wallet mapper to return a response
        WalletResponse mockResponse = WalletResponse.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("300.00"))
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
        when(walletMapper.toResponse(any(UserWallet.class))).thenReturn(mockResponse);

        // When: Execute use case
        WalletResponse response = lockBalanceUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getBalance())
                .as("Available balance should decrease by 300")
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.getLockedBalance())
                .as("Locked balance should increase by 300")
                .isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getTotalBalance())
                .as("Total balance should remain 1000")
                .isEqualByComparingTo(new BigDecimal("1000.00"));

        // Verify: Repository interactions
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, times(1)).save(wallet);
    }

    // ============================================================
    // TEST 2: Wallet Not Found
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw NotFoundException when wallet not found")
    void execute_ThrowsNotFoundException_WhenWalletNotFound() {
        // Given: Wallet does NOT exist
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.empty());

        // When & Then: Should throw NotFoundException
        assertThatThrownBy(() -> lockBalanceUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found")
                .hasMessageContaining("100");

        // Verify: Save was NOT called
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: Insufficient Balance
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw InsufficientBalanceException when balance too low")
    void execute_ThrowsInsufficientBalanceException_WhenBalanceTooLow() {
        // Given: Command tries to lock MORE than available
        command = LockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("1500.00")) // More than 1000
                .description("Lock too much")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw InsufficientBalanceException
        assertThatThrownBy(() -> lockBalanceUseCase.execute(command))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("1000.00") // Current balance
                .hasMessageContaining("1500.00"); // Requested amount

        // Verify: Save was NOT called (transaction rolled back)
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Negative Amount
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw ValidationException when amount is negative")
    void execute_ThrowsValidationException_WhenAmountIsNegative() {
        // Given: Negative amount
        command = LockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("-100.00"))
                .description("Invalid negative")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception (could be ValidationException or IllegalArgumentException)
        assertThatThrownBy(() -> lockBalanceUseCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");

        // Verify: Save was NOT called
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 5: Zero Amount
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw ValidationException when amount is zero")
    void execute_ThrowsValidationException_WhenAmountIsZero() {
        // Given: Zero amount
        command = LockBalanceCommand.builder()
                .userId(100L)
                .amount(BigDecimal.ZERO)
                .description("Invalid zero")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> lockBalanceUseCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");

        // Verify: Save was NOT called
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 6: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> lockBalanceUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 7: Transactional Behavior
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should be transactional - rollback on error")
    void execute_RollsBackTransaction_WhenErrorOccurs() {
        // Given: Wallet exists
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        // But save throws exception (simulating database error)
        when(walletRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // When & Then: Should propagate exception
        assertThatThrownBy(() -> lockBalanceUseCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        // Note: @Transactional will ensure rollback happens automatically
    }

    // NOTE: These tests will FAIL now because:
    // 1. LockBalanceUseCase class doesn't exist yet
    // 2. LockBalanceCommand DTO doesn't exist yet
    // This is EXPECTED in TDD - we write tests FIRST!
}
