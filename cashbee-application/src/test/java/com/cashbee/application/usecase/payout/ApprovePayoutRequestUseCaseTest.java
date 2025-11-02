package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.ApprovePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.repository.PayoutRequestRepository;
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
 * TDD Tests for ApprovePayoutRequestUseCase
 *
 * Business Scenario:
 * Admin reviews payout request and approves it for processing.
 * Status changes from REQUESTED → PROCESSING.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovePayoutRequestUseCase Tests (TDD)")
class ApprovePayoutRequestUseCaseTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private com.cashbee.application.port.PayoutRequestMapper payoutRequestMapper;

    @InjectMocks
    private ApprovePayoutRequestUseCase approvePayoutRequestUseCase;

    private ApprovePayoutRequestCommand command;
    private PayoutRequest payoutRequest;

    @BeforeEach
    void setUp() {
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

        // Given: Admin approval command
        command = ApprovePayoutRequestCommand.builder()
                .payoutRequestId(1L)
                .adminId("admin123")
                .build();
    }

    // ============================================================
    // TEST 1: Approve Payout - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should approve payout request when status is REQUESTED")
    void execute_ApprovesPayout_WhenStatusIsRequested() {
        // Given: Payout request exists
        when(payoutRequestRepository.findById(1L)).thenReturn(Optional.of(payoutRequest));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> i.getArgument(0));

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
                .status(PayoutStatus.PROCESSING)
                .requestedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .processedBy("admin123")
                .build();
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenReturn(mockResponse);

        // When: Execute use case
        PayoutRequestResponse response = approvePayoutRequestUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        assertThat(response.getProcessedBy()).isEqualTo("admin123");
        assertThat(response.getProcessedAt()).isNotNull();

        // Verify: Repository interactions
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
        assertThatThrownBy(() -> approvePayoutRequestUseCase.execute(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payout request not found");

        // Verify: Save was NOT called
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: Invalid Status (Not REQUESTED)
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

        // When & Then: Should throw IllegalStateException
        assertThatThrownBy(() -> approvePayoutRequestUseCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REQUESTED")
                .hasMessageContaining("approve");

        // Verify: Save was NOT called
        verify(payoutRequestRepository, times(1)).findById(1L);
        verify(payoutRequestRepository, never()).save(any());
    }

    // ============================================================
    // TEST 4: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> approvePayoutRequestUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(payoutRequestRepository, never()).findById(any());
        verify(payoutRequestRepository, never()).save(any());
    }

    // NOTE: These tests will FAIL because ApprovePayoutRequestUseCase doesn't exist yet!
}
