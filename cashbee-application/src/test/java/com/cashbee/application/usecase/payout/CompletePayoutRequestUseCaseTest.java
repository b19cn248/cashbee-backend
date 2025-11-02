package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CompletePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.TransactionRepository;
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
 * TDD Tests for CompletePayoutRequestUseCase
 *
 * Business Scenario:
 * Admin marks payout as completed after successfully transferring money.
 * Status changes from PROCESSING → PAID.
 * Locked balance must be deducted (moved to totalWithdrawn).
 * Transaction record must be created for audit trail.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompletePayoutRequestUseCase Tests (TDD)")
class CompletePayoutRequestUseCaseTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private com.cashbee.application.port.PayoutRequestMapper payoutRequestMapper;

    @InjectMocks
    private CompletePayoutRequestUseCase completePayoutRequestUseCase;

    private CompletePayoutRequestCommand command;
    private PayoutRequest payoutRequest;
    private UserWallet wallet;

    @BeforeEach
    void setUp() {
        // Given: A wallet with locked balance
        wallet = UserWallet.builder()
                .id(10L)
                .userId(100L)
                .balance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(new BigDecimal("500000.00")) // Locked for payout
                .totalEarned(new BigDecimal("1000000.00"))
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        // Given: A payout request with PROCESSING status
        payoutRequest = PayoutRequest.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .status(PayoutStatus.PROCESSING)
                .requestedAt(LocalDateTime.now().minusDays(1))
                .processedAt(LocalDateTime.now())
                .processedBy("admin123")
                .build();

        // Given: Admin completion command
        command = CompletePayoutRequestCommand.builder()
                .payoutRequestId(1L)
                .adminId("admin123")
                .transactionReference("TXN-123456")
                .build();
    }

    // ============================================================
    // TEST 1: Complete Payout - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should complete payout and deduct locked balance when status is PROCESSING")
    void execute_CompletesPayoutAndDeductsBalance_WhenStatusIsProcessing() {
        // Given: Payout request and wallet exist
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

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
                .status(PayoutStatus.PAID)
                .requestedAt(LocalDateTime.now().minusDays(1))
                .processedAt(LocalDateTime.now())
                .processedBy("admin123")
                .completedAt(LocalDateTime.now())
                .build();
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(mockResponse);

        // When: Execute use case
        PayoutRequestResponse response = completePayoutRequestUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PayoutStatus.PAID);
        assertThat(response.getCompletedAt()).isNotNull();

        // Verify: Locked balance was deducted
        verify(walletRepository, times(1)).findById(10L);
        verify(walletRepository, times(1)).save(wallet);

        // Verify: Payout request was completed
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, times(1)).save(payoutRequest);

        // Verify: Transaction was created
        verify(transactionRepository, times(1)).save(any(Transaction.class));
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
        assertThatThrownBy(() -> completePayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payout request not found");

        // Verify: No saves
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
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
        assertThatThrownBy(() -> completePayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");

        // Verify: Payout was NOT saved
        verify(payoutRequestRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Invalid Status (Not PROCESSING)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when status is not PROCESSING")
    void execute_ThrowsException_WhenStatusIsNotProcessing() {
        // Given: Payout still REQUESTED (not yet approved)
        payoutRequest = PayoutRequest.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123")
                .accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        // When & Then: Should throw IllegalStateException
        assertThatThrownBy(() -> completePayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROCESSING")
                .hasMessageContaining("complete");

        // Verify: No saves
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // TEST 5: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> completePayoutRequestUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(payoutRequestRepository, never()).findById(any());
        verify(payoutRequestRepository, never()).save(any());
        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // TEST 6: Transaction Reference Recorded
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should create transaction record with reference")
    void execute_CreatesTransactionWithReference_WhenCompleted() {
        // Given: Payout request and wallet exist
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(UserWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        PayoutRequestResponse mockResponse = PayoutRequestResponse.builder()
                .id(1L)
                .status(PayoutStatus.PAID)
                .build();
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(mockResponse);

        // When: Execute use case
        completePayoutRequestUseCase.execute(command);

        // Then: Verify transaction was created with correct details
        verify(transactionRepository, times(1)).save(argThat(transaction ->
                transaction.getUserId().equals(100L) &&
                transaction.getWalletId().equals(10L) &&
                transaction.getType().equals(TransactionType.WITHDRAW) &&
                transaction.getAmount().compareTo(new BigDecimal("500000.00")) == 0 &&
                transaction.getStatus().equals(TransactionStatus.SUCCESS) &&
                transaction.getDescription().contains("TXN-123456")
        ));
    }

    // NOTE: These tests will FAIL because CompletePayoutRequestUseCase doesn't exist yet!
}
