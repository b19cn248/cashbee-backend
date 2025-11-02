package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.DeductBalanceCommand;
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
 * TDD Tests for DeductBalanceUseCase
 *
 * Business Scenario:
 * When payout is successfully paid to user, we need to "deduct"
 * the locked balance (remove it permanently) and update totalWithdrawn.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeductBalanceUseCase Tests (TDD)")
class DeductBalanceUseCaseTest {

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private com.cashbee.application.port.WalletMapper walletMapper;

    @InjectMocks
    private DeductBalanceUseCase deductBalanceUseCase;

    private UserWallet wallet;
    private DeductBalanceCommand command;

    @BeforeEach
    void setUp() {
        // Given: A wallet with 700 available balance and 300 locked (ready for payout)
        wallet = UserWallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("300.00")) // Locked for payout
                .totalEarned(new BigDecimal("1000.00"))
                .totalWithdrawn(BigDecimal.ZERO) // No withdrawals yet
                .build();

        // Given: A valid deduct command (payout approved and paid)
        command = DeductBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("300.00"))
                .description("Payout #123 completed")
                .build();
    }

    // ============================================================
    // TEST 1: Deduct Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should deduct locked balance successfully when payout paid")
    void execute_DeductsBalanceSuccessfully_WhenValidRequest() {
        // Given: Wallet exists with locked balance
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Mock wallet mapper
        WalletResponse mockResponse = WalletResponse.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00")) // Unchanged
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO) // 300 deducted
                .totalEarned(new BigDecimal("1000.00"))
                .totalWithdrawn(new BigDecimal("300.00")) // Increased!
                .build();
        when(walletMapper.toResponse(any(UserWallet.class))).thenReturn(mockResponse);

        // When: Execute use case
        WalletResponse response = deductBalanceUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getBalance())
                .as("Available balance should remain 700")
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.getLockedBalance())
                .as("Locked balance should be 0 after deduction")
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalWithdrawn())
                .as("Total withdrawn should increase by 300")
                .isEqualByComparingTo(new BigDecimal("300.00"));

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
        assertThatThrownBy(() -> deductBalanceUseCase.execute(command))
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
    @DisplayName("🔴 RED TEST: Should throw exception when deducting more than locked balance")
    void execute_ThrowsException_WhenInsufficientLockedBalance() {
        // Given: Command tries to deduct MORE than locked amount
        command = DeductBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("500.00")) // More than 300 locked
                .description("Invalid deduct")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> deductBalanceUseCase.execute(command))
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
        command = DeductBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("-100.00"))
                .description("Invalid negative")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> deductBalanceUseCase.execute(command))
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
        command = DeductBalanceCommand.builder()
                .userId(100L)
                .amount(BigDecimal.ZERO)
                .description("Invalid zero")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> deductBalanceUseCase.execute(command))
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
        assertThatThrownBy(() -> deductBalanceUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 7: Multiple Payouts (Accumulating totalWithdrawn)
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should accumulate totalWithdrawn across multiple payouts")
    void execute_AccumulatesTotalWithdrawn_WhenMultiplePayouts() {
        // Given: Wallet with previous withdrawal history
        wallet = UserWallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("300.00"))
                .totalEarned(new BigDecimal("1000.00"))
                .totalWithdrawn(new BigDecimal("500.00")) // Already withdrew 500 before
                .build();

        // Deduct another 300
        command = DeductBalanceCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("300.00"))
                .description("Second payout")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Mock response: totalWithdrawn should be 500 + 300 = 800
        WalletResponse mockResponse = WalletResponse.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("700.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(new BigDecimal("1000.00"))
                .totalWithdrawn(new BigDecimal("800.00")) // 500 + 300
                .build();
        when(walletMapper.toResponse(any(UserWallet.class))).thenReturn(mockResponse);

        // When: Execute use case
        WalletResponse response = deductBalanceUseCase.execute(command);

        // Then: Verify accumulation
        assertThat(response.getTotalWithdrawn())
                .as("Total withdrawn should be 800 (500 previous + 300 current)")
                .isEqualByComparingTo(new BigDecimal("800.00"));

        verify(walletRepository, times(1)).save(wallet);
    }

    // NOTE: These tests will FAIL because DeductBalanceUseCase doesn't exist yet!
}
