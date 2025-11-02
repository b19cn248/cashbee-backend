package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.UnlockBalanceCommand;
import com.cashbee.application.dto.wallet.WalletResponse;
import com.cashbee.common.exception.NotFoundException;
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
 * TDD Tests for UnlockBalanceUseCase
 *
 * Business Scenario:
 * When a payout request is rejected or cancelled, we need to "unlock"
 * the locked balance back to available balance.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnlockBalanceUseCase Tests (TDD)")
class UnlockBalanceUseCaseTest {

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private com.cashbee.application.port.WalletMapper walletMapper;

    @InjectMocks
    private UnlockBalanceUseCase unlockBalanceUseCase;

    private UserWallet wallet;
    private UnlockBalanceCommand command;

    @BeforeEach
    void setUp() {
        // Given: A wallet with 700 available balance and 300 locked
        wallet = UserWallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("300.00")) // Already locked from previous request
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        // Given: A valid unlock command
        command = UnlockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("300.00"))
                .description("Payout request cancelled")
                .build();
    }

    // ============================================================
    // TEST 1: Unlock Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should unlock balance successfully when valid request")
    void execute_UnlocksBalanceSuccessfully_WhenValidRequest() {
        // Given: Wallet exists with locked balance
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Mock wallet mapper
        WalletResponse mockResponse = WalletResponse.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("1000.00")) // 700 + 300 unlocked
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO) // 300 - 300 unlocked
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
        when(walletMapper.toResponse(any(UserWallet.class))).thenReturn(mockResponse);

        // When: Execute use case
        WalletResponse response = unlockBalanceUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getBalance())
                .as("Available balance should increase by 300")
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getLockedBalance())
                .as("Locked balance should decrease by 300")
                .isEqualByComparingTo(BigDecimal.ZERO);

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
        assertThatThrownBy(() -> unlockBalanceUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found")
                .hasMessageContaining("100");

        // Verify: Save was NOT called
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: Insufficient Locked Balance
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when unlocking more than locked balance")
    void execute_ThrowsException_WhenInsufficientLockedBalance() {
        // Given: Command tries to unlock MORE than locked amount
        command = UnlockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("500.00")) // More than 300 locked
                .description("Invalid unlock")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> unlockBalanceUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300.00") // Current locked balance
                .hasMessageContaining("500.00"); // Requested amount

        // Verify: Save was NOT called
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Negative Amount
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is negative")
    void execute_ThrowsException_WhenAmountIsNegative() {
        // Given: Negative amount
        command = UnlockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("-100.00"))
                .description("Invalid negative")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> unlockBalanceUseCase.execute(command))
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
    @DisplayName("🔴 RED TEST: Should throw exception when amount is zero")
    void execute_ThrowsException_WhenAmountIsZero() {
        // Given: Zero amount
        command = UnlockBalanceCommand.builder()
                .userId(100L)
                .amount(BigDecimal.ZERO)
                .description("Invalid zero")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> unlockBalanceUseCase.execute(command))
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
        assertThatThrownBy(() -> unlockBalanceUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 7: Partial Unlock
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should unlock partial amount successfully")
    void execute_UnlocksPartialAmount_Successfully() {
        // Given: Unlock only 100 out of 300 locked
        command = UnlockBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("100.00"))
                .description("Partial unlock")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Mock response: balance should be 800 (700+100), locked should be 200 (300-100)
        WalletResponse mockResponse = WalletResponse.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("800.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("200.00"))
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
        when(walletMapper.toResponse(any(UserWallet.class))).thenReturn(mockResponse);

        // When: Execute use case
        WalletResponse response = unlockBalanceUseCase.execute(command);

        // Then: Verify partial unlock
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(response.getLockedBalance()).isEqualByComparingTo(new BigDecimal("200.00"));

        verify(walletRepository, times(1)).save(wallet);
    }

    // NOTE: These tests will FAIL because UnlockBalanceUseCase doesn't exist yet!
}
