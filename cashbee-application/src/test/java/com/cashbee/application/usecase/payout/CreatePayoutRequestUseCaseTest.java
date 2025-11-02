package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CreatePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for CreatePayoutRequestUseCase
 *
 * Business Scenario:
 * User requests payout (withdrawal) from their wallet to bank/Momo/ZaloPay.
 * System validates balance, locks the amount, and creates payout request.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePayoutRequestUseCase Tests (TDD)")
class CreatePayoutRequestUseCaseTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private com.cashbee.application.port.PayoutRequestMapper payoutRequestMapper;

    @InjectMocks
    private CreatePayoutRequestUseCase createPayoutRequestUseCase;

    private CreatePayoutRequestCommand command;
    private UserWallet wallet;
    private PayoutRequest payoutRequest;

    @BeforeEach
    void setUp() {
        // Given: A user with sufficient balance
        wallet = UserWallet.builder()
                .id(10L)
                .userId(100L)
                .balance(new BigDecimal("1000000.00")) // 1M VND available
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(new BigDecimal("1000000.00"))
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        // Given: A valid payout request command
        command = CreatePayoutRequestCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("500000.00")) // 500k VND
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .build();

        // Mock payout request (what repository will return)
        payoutRequest = PayoutRequest.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // TEST 1: Create Payout Request - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should create payout request and lock balance when valid command")
    void execute_CreatesPayoutAndLocksBalance_WhenValidCommand() {
        // Given: Wallet exists with sufficient balance
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenReturn(payoutRequest);

        // Mock mapper
        PayoutRequestResponse mockResponse = PayoutRequestResponse.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(mockResponse);

        // When: Execute use case
        PayoutRequestResponse response = createPayoutRequestUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(response.getStatus()).isEqualTo(PayoutStatus.REQUESTED);

        // Verify: Balance was locked
        verify(walletRepository, times(1)).findByUserId(100L);
        verify(walletRepository, times(1)).save(wallet);

        // Verify: Payout request was saved
        verify(payoutRequestRepository, times(1)).save(any(PayoutRequest.class));
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
        assertThatThrownBy(() -> createPayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");

        // Verify: No payout request created
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: Insufficient Balance
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when insufficient balance")
    void execute_ThrowsException_WhenInsufficientBalance() {
        // Given: Wallet with insufficient balance
        wallet = UserWallet.builder()
                .id(10L)
                .userId(100L)
                .balance(new BigDecimal("30000.00")) // Only 30k, need 500k
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(new BigDecimal("30000.00"))
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> createPayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        // Verify: No payout request created
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Amount Below Minimum
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount below minimum (50k)")
    void execute_ThrowsException_WhenAmountBelowMinimum() {
        // Given: Command with amount below 50k minimum
        command = CreatePayoutRequestCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("40000.00")) // Below 50k minimum
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123")
                .accountName("Test")
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> createPayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50000")
                .hasMessageContaining("minimum");

        // Verify: No payout request created
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 5: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> createPayoutRequestUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(walletRepository, never()).findByUserId(any());
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 6: Different Payout Methods
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should support all payout methods")
    void execute_SupportsAllPayoutMethods() {
        // Test MOMO
        command = CreatePayoutRequestCommand.builder()
                .userId(100L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.MOMO)
                .accountNumber("0987654321")
                .accountName("NGUYEN VAN A")
                .build();

        PayoutRequest momoPayoutRequest = PayoutRequest.builder()
                .id(2L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.MOMO)
                .accountNumber("0987654321")
                .accountName("NGUYEN VAN A")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenReturn(momoPayoutRequest);
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(
                PayoutRequestResponse.builder()
                        .id(2L).userId(100L).walletId(10L)
                        .amount(new BigDecimal("500000.00"))
                        .payoutMethod(PayoutMethod.MOMO)
                        .accountNumber("0987654321")
                        .accountName("NGUYEN VAN A")
                        .status(PayoutStatus.REQUESTED)
                        .requestedAt(LocalDateTime.now())
                        .build()
        );

        PayoutRequestResponse response = createPayoutRequestUseCase.execute(command);

        assertThat(response.getPayoutMethod()).isEqualTo(PayoutMethod.MOMO);
        verify(payoutRequestRepository, times(1)).save(any(PayoutRequest.class));
    }

    // NOTE: These tests will FAIL because CreatePayoutRequestUseCase doesn't exist yet!
}
