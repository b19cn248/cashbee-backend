package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.payout.GetPayoutRequestsQuery;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GetPayoutRequestsUseCase
 *
 * Business Scenarios:
 * 1. User views their own payout requests (filtered by userId)
 * 2. Admin views all payout requests
 * 3. Admin views payout requests filtered by status
 * 4. Pagination support for large result sets
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPayoutRequestsUseCase Tests (TDD)")
class GetPayoutRequestsUseCaseTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private com.cashbee.application.port.PayoutRequestMapper payoutRequestMapper;

    @InjectMocks
    private GetPayoutRequestsUseCase getPayoutRequestsUseCase;

    private List<PayoutRequest> payoutRequests;
    private PayoutRequest payoutRequest1;
    private PayoutRequest payoutRequest2;
    private PayoutRequest payoutRequest3;

    @BeforeEach
    void setUp() {
        // Given: Multiple payout requests with different statuses
        payoutRequest1 = PayoutRequest.builder()
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

        payoutRequest2 = PayoutRequest.builder()
                .id(2L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("300000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .status(PayoutStatus.PROCESSING)
                .requestedAt(LocalDateTime.now().minusDays(1))
                .processedAt(LocalDateTime.now())
                .processedBy("admin123")
                .build();

        payoutRequest3 = PayoutRequest.builder()
                .id(3L)
                .userId(200L)
                .walletId(20L)
                .amount(new BigDecimal("1000000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("9876543210")
                .accountName("TRAN THI B")
                .bankName("Techcombank")
                .status(PayoutStatus.PAID)
                .requestedAt(LocalDateTime.now().minusDays(2))
                .processedAt(LocalDateTime.now().minusDays(1))
                .processedBy("admin456")
                .completedAt(LocalDateTime.now())
                .build();

        payoutRequests = Arrays.asList(payoutRequest1, payoutRequest2, payoutRequest3);
    }

    // ============================================================
    // TEST 1: Get Payout Requests by User ID - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should get payout requests by userId with pagination")
    void execute_GetsPayoutRequestsByUserId_WithPagination() {
        // Given: Query for user's payout requests
        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(100L)
                .page(0)
                .size(10)
                .build();

        // Mock repository response (only payoutRequest1 and payoutRequest2 for userId=100)
        List<PayoutRequest> userPayouts = Arrays.asList(payoutRequest1, payoutRequest2);
        when(payoutRequestRepository.findByUserId(eq(100L), eq(0), eq(10))).thenReturn(userPayouts);
        when(payoutRequestRepository.countByUserId(eq(100L))).thenReturn(2L);

        // Mock mapper responses
        PayoutRequestResponse response1 = createMockResponse(payoutRequest1);
        PayoutRequestResponse response2 = createMockResponse(payoutRequest2);
        when(payoutRequestMapper.toResponse(payoutRequest1)).thenReturn(response1);
        when(payoutRequestMapper.toResponse(payoutRequest2)).thenReturn(response2);

        // When: Execute use case
        PageResponse<PayoutRequestResponse> result = getPayoutRequestsUseCase.execute(query);

        // Then: Verify response
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(100L);
        assertThat(result.getContent().get(1).getUserId()).isEqualTo(100L);

        // Verify: Repository was called with correct parameters
        verify(payoutRequestRepository, times(1)).findByUserId(eq(100L), eq(0), eq(10));
        verify(payoutRequestRepository, times(1)).countByUserId(eq(100L));
        verify(payoutRequestMapper, times(2)).toResponse(any(PayoutRequest.class));
    }

    // ============================================================
    // TEST 2: Get All Payout Requests (Admin View)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should get all payout requests when userId is null")
    void execute_GetsAllPayoutRequests_WhenUserIdIsNull() {
        // Given: Query without userId (admin view)
        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(null)
                .page(0)
                .size(10)
                .build();

        // Mock repository response (all payout requests)
        when(payoutRequestRepository.findAll(eq(0), eq(10))).thenReturn(payoutRequests);
        when(payoutRequestRepository.countAll()).thenReturn(3L);

        // Mock mapper responses
        when(payoutRequestMapper.toResponse(any(PayoutRequest.class))).thenAnswer(i -> createMockResponse(i.getArgument(0)));

        // When: Execute use case
        PageResponse<PayoutRequestResponse> result = getPayoutRequestsUseCase.execute(query);

        // Then: Verify response
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);

        // Verify: Repository was called for all payouts
        verify(payoutRequestRepository, times(1)).findAll(eq(0), eq(10));
        verify(payoutRequestRepository, times(1)).countAll();
        verify(payoutRequestRepository, never()).findByUserId(any(), anyInt(), anyInt());
        verify(payoutRequestMapper, times(3)).toResponse(any(PayoutRequest.class));
    }

    // ============================================================
    // TEST 3: Get Payout Requests Filtered by Status
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should get payout requests filtered by status")
    void execute_GetsPayoutRequestsByStatus_WhenStatusProvided() {
        // Given: Query with status filter
        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(null)
                .status(PayoutStatus.REQUESTED)
                .page(0)
                .size(10)
                .build();

        // Mock repository response (only REQUESTED status)
        List<PayoutRequest> requestedPayouts = Collections.singletonList(payoutRequest1);
        when(payoutRequestRepository.findByStatus(eq(PayoutStatus.REQUESTED), eq(0), eq(10))).thenReturn(requestedPayouts);
        when(payoutRequestRepository.countByStatus(eq(PayoutStatus.REQUESTED))).thenReturn(1L);

        // Mock mapper response
        PayoutRequestResponse response1 = createMockResponse(payoutRequest1);
        when(payoutRequestMapper.toResponse(payoutRequest1)).thenReturn(response1);

        // When: Execute use case
        PageResponse<PayoutRequestResponse> result = getPayoutRequestsUseCase.execute(query);

        // Then: Verify response
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PayoutStatus.REQUESTED);

        // Verify: Repository was called with status filter
        verify(payoutRequestRepository, times(1)).findByStatus(eq(PayoutStatus.REQUESTED), eq(0), eq(10));
        verify(payoutRequestRepository, times(1)).countByStatus(eq(PayoutStatus.REQUESTED));
        verify(payoutRequestMapper, times(1)).toResponse(any(PayoutRequest.class));
    }

    // ============================================================
    // TEST 4: Get Payout Requests by UserId and Status
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should get payout requests by userId and status")
    void execute_GetsPayoutRequestsByUserIdAndStatus_WhenBothProvided() {
        // Given: Query with both userId and status
        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(100L)
                .status(PayoutStatus.PROCESSING)
                .page(0)
                .size(10)
                .build();

        // Mock repository response (userId=100 and status=PROCESSING)
        List<PayoutRequest> filteredPayouts = Collections.singletonList(payoutRequest2);
        when(payoutRequestRepository.findByUserIdAndStatus(eq(100L), eq(PayoutStatus.PROCESSING), eq(0), eq(10)))
                .thenReturn(filteredPayouts);
        when(payoutRequestRepository.countByUserIdAndStatus(eq(100L), eq(PayoutStatus.PROCESSING)))
                .thenReturn(1L);

        // Mock mapper response
        PayoutRequestResponse response2 = createMockResponse(payoutRequest2);
        when(payoutRequestMapper.toResponse(payoutRequest2)).thenReturn(response2);

        // When: Execute use case
        PageResponse<PayoutRequestResponse> result = getPayoutRequestsUseCase.execute(query);

        // Then: Verify response
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PayoutStatus.PROCESSING);

        // Verify: Repository was called with both filters
        verify(payoutRequestRepository, times(1)).findByUserIdAndStatus(eq(100L), eq(PayoutStatus.PROCESSING), eq(0), eq(10));
        verify(payoutRequestRepository, times(1)).countByUserIdAndStatus(eq(100L), eq(PayoutStatus.PROCESSING));
        verify(payoutRequestMapper, times(1)).toResponse(any(PayoutRequest.class));
    }

    // ============================================================
    // TEST 5: Empty Result
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should return empty page when no payout requests found")
    void execute_ReturnsEmptyPage_WhenNoPayoutsFound() {
        // Given: Query for user with no payout requests
        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(999L)
                .page(0)
                .size(10)
                .build();

        // Mock repository response (empty)
        when(payoutRequestRepository.findByUserId(eq(999L), eq(0), eq(10))).thenReturn(Collections.emptyList());
        when(payoutRequestRepository.countByUserId(eq(999L))).thenReturn(0L);

        // When: Execute use case
        PageResponse<PayoutRequestResponse> result = getPayoutRequestsUseCase.execute(query);

        // Then: Verify empty response
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);

        // Verify: Repository was called but mapper was NOT
        verify(payoutRequestRepository, times(1)).findByUserId(eq(999L), eq(0), eq(10));
        verify(payoutRequestRepository, times(1)).countByUserId(eq(999L));
        verify(payoutRequestMapper, never()).toResponse(any());
    }

    // ============================================================
    // TEST 6: Null Query Validation
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when query is null")
    void execute_ThrowsException_WhenQueryIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> getPayoutRequestsUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(payoutRequestRepository, never()).findByUserId(any(), anyInt(), anyInt());
        verify(payoutRequestRepository, never()).findAll(anyInt(), anyInt());
        verify(payoutRequestRepository, never()).findByStatus(any(), anyInt(), anyInt());
        verify(payoutRequestRepository, never()).findByUserIdAndStatus(any(), any(), anyInt(), anyInt());
    }

    // ============================================================
    // Helper Method
    // ============================================================

    private PayoutRequestResponse createMockResponse(PayoutRequest payoutRequest) {
        return PayoutRequestResponse.builder()
                .id(payoutRequest.getId())
                .userId(payoutRequest.getUserId())
                .walletId(payoutRequest.getWalletId())
                .amount(payoutRequest.getAmount())
                .payoutMethod(payoutRequest.getPayoutMethod())
                .accountNumber(payoutRequest.getAccountNumber())
                .accountName(payoutRequest.getAccountName())
                .bankName(payoutRequest.getBankName())
                .status(payoutRequest.getStatus())
                .requestedAt(payoutRequest.getRequestedAt())
                .processedAt(payoutRequest.getProcessedAt())
                .processedBy(payoutRequest.getProcessedBy())
                .completedAt(payoutRequest.getCompletedAt())
                .rejectionReason(payoutRequest.getRejectionReason())
                .build();
    }

    // NOTE: These tests will FAIL because GetPayoutRequestsUseCase doesn't exist yet!
}
