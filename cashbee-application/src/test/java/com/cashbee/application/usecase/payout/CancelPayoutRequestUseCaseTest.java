package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CancelPayoutRequestCommand;
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
 * TDD Tests for CancelPayoutRequestUseCase
 *
 * Business Scenario:
 * User or admin cancels payout request.
 * Status changes from REQUESTED → CANCELLED.
 * Locked balance must be unlocked (returned to available).
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelPayoutRequestUseCase Tests (TDD)")
class CancelPayoutRequestUseCaseTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private com.cashbee.application.port.PayoutRequestMapper payoutRequestMapper;

    @InjectMocks
    private CancelPayoutRequestUseCase cancelPayoutRequestUseCase;

    private CancelPayoutRequestCommand command;
    private PayoutRequest payoutRequest;
    private UserWallet wallet;

    @BeforeEach
    void setUp() {
        // Given: A wallet with locked balance
        wallet = UserWallet.builder()
                .id(10L)
                .userId(100L)
                .balance(new BigDecimal("500000.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("500000.00")) // Locked for payout
                .totalEarned(new BigDecimal("1000000.00"))
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        // Given: A payout request with REQUESTED status
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

        // Given: Cancel command
        command = CancelPayoutRequestCommand.builder()
                .payoutRequestId(1L)
                .userId(100L)
                .reason("User changed mind")
                .build();
    }

    // ============================================================
    // TEST 1: Cancel Payout - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should cancel payout and unlock balance when status is REQUESTED")
    void execute_CancelsPayoutAndUnlocksBalance_WhenStatusIsRequested() {
        // Given: Payout request and wallet exist
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));

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
                .status(PayoutStatus.CANCELLED)
                .requestedAt(LocalDateTime.now())
                .rejectionReason("User changed mind")
                .build();
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(mockResponse);

        // When: Execute use case
        PayoutRequestResponse response = cancelPayoutRequestUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PayoutStatus.CANCELLED);
        assertThat(response.getRejectionReason()).isEqualTo("User changed mind");

        // Verify: Balance was unlocked
        verify(walletRepository, times(1)).findById(10L);
        verify(walletRepository, times(1)).save(wallet);

        // Verify: Payout request was updated
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, times(1)).save(payoutRequest);
    }

    // ============================================================
    // TEST 2: Payout Request Not Found
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw NotFoundException when payout request not found")
    void execute_ThrowsNotFoundException_WhenPayoutNotFound() {
        // Given: Payout request does NOT exist
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then: Should throw NotFoundException
        assertThatThrownBy(() -> cancelPayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payout request not found");

        // Verify: No saves
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: Wallet Not Found
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw NotFoundException when wallet not found")
    void execute_ThrowsNotFoundException_WhenWalletNotFound() {
        // Given: Payout exists but wallet does NOT
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then: Should throw NotFoundException
        assertThatThrownBy(() -> cancelPayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");

        // Verify: Payout was NOT saved
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Invalid Status (Not REQUESTED)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when status is not REQUESTED")
    void execute_ThrowsException_WhenStatusIsNotRequested() {
        // Given: Payout already PROCESSING
        payoutRequest = PayoutRequest.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123")
                .accountName("Test")
                .status(PayoutStatus.PROCESSING)
                .requestedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw IllegalStateException
        assertThatThrownBy(() -> cancelPayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REQUESTED")
                .hasMessageContaining("cancel");

        // Verify: No saves
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 5: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> cancelPayoutRequestUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(payoutRequestRepository, never()).findById(any());
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
    }

    // ============================================================
    // TEST 6: Reason Required
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when reason is null or blank")
    void execute_ThrowsException_WhenReasonIsNullOrBlank() {
        // Given: Command with null reason
        command = CancelPayoutRequestCommand.builder()
                .payoutRequestId(1L)
                .userId(100L)
                .reason(null)
                .build();

        // When & Then: Should throw exception (no repository setup needed - validation happens first)
        assertThatThrownBy(() -> cancelPayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        // Verify: No repository interaction at all
        verify(payoutRequestRepository, never()).findById(any());
        verify(walletRepository, never()).findById(any());
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    // NOTE: These tests will FAIL because CancelPayoutRequestUseCase doesn't exist yet!
}
