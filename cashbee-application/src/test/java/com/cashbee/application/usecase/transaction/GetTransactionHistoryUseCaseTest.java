package com.cashbee.application.usecase.transaction;

import com.cashbee.application.dto.transaction.GetTransactionHistoryQuery;
import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.repository.TransactionRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GetTransactionHistoryUseCase
 *
 * Business Scenario:
 * Users need to view their transaction history with pagination.
 * This provides transparency and audit trail.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionHistoryUseCase Tests (TDD)")
class GetTransactionHistoryUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private com.cashbee.application.port.TransactionMapper transactionMapper;

    @InjectMocks
    private GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    private GetTransactionHistoryQuery query;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        // Given: A query for transaction history with pagination
        query = GetTransactionHistoryQuery.builder()
                .userId(100L)
                .page(0)
                .size(10)
                .build();

        // Mock transactions (what repository will return)
        Transaction tx1 = Transaction.builder()
                .id(1L).userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Cashback from order #1")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now.minusDays(2))
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L).userId(100L).walletId(10L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("200.00"))
                .description("Withdrawal to bank")
                .balanceBefore(new BigDecimal("1500.00"))
                .balanceAfter(new BigDecimal("1300.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now.minusDays(1))
                .build();

        Transaction tx3 = Transaction.builder()
                .id(3L).userId(100L).walletId(10L)
                .type(TransactionType.BONUS)
                .amount(new BigDecimal("100.00"))
                .description("Sign-up bonus")
                .balanceBefore(new BigDecimal("1300.00"))
                .balanceAfter(new BigDecimal("1400.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        transactions = Arrays.asList(tx1, tx2, tx3);
    }

    // ============================================================
    // TEST 1: Get Transaction History - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should get transaction history successfully with pagination")
    void execute_GetsTransactionHistory_WhenValidQuery() {
        // Given: Repository returns transactions
        when(transactionRepository.findByUserId(eq(100L), eq(0), eq(10)))
                .thenReturn(transactions);

        // Mock mapper
        List<TransactionResponse> mockResponses = Arrays.asList(
                TransactionResponse.builder()
                        .id(1L).userId(100L).walletId(10L)
                        .type(TransactionType.CASHBACK)
                        .amount(new BigDecimal("500.00"))
                        .description("Cashback from order #1")
                        .balanceBefore(new BigDecimal("1000.00"))
                        .balanceAfter(new BigDecimal("1500.00"))
                        .status(TransactionStatus.SUCCESS)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build(),
                TransactionResponse.builder()
                        .id(2L).userId(100L).walletId(10L)
                        .type(TransactionType.WITHDRAW)
                        .amount(new BigDecimal("200.00"))
                        .description("Withdrawal to bank")
                        .balanceBefore(new BigDecimal("1500.00"))
                        .balanceAfter(new BigDecimal("1300.00"))
                        .status(TransactionStatus.SUCCESS)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                TransactionResponse.builder()
                        .id(3L).userId(100L).walletId(10L)
                        .type(TransactionType.BONUS)
                        .amount(new BigDecimal("100.00"))
                        .description("Sign-up bonus")
                        .balanceBefore(new BigDecimal("1300.00"))
                        .balanceAfter(new BigDecimal("1400.00"))
                        .status(TransactionStatus.SUCCESS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        when(transactionMapper.toResponseList(anyList())).thenReturn(mockResponses);

        // When: Execute use case
        List<TransactionResponse> responses = getTransactionHistoryUseCase.execute(query);

        // Then: Verify response
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        assertThat(responses.get(2).getId()).isEqualTo(3L);

        // Verify: Repository called with correct params
        verify(transactionRepository, times(1)).findByUserId(100L, 0, 10);
    }

    // ============================================================
    // TEST 2: Null Query
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when query is null")
    void execute_ThrowsException_WhenQueryIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> getTransactionHistoryUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(transactionRepository, never()).findByUserId(anyLong(), anyInt(), anyInt());
    }

    // ============================================================
    // TEST 3: Empty Transaction History
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should return empty list when user has no transactions")
    void execute_ReturnsEmptyList_WhenNoTransactions() {
        // Given: Repository returns empty list
        when(transactionRepository.findByUserId(eq(100L), eq(0), eq(10)))
                .thenReturn(List.of());
        when(transactionMapper.toResponseList(anyList())).thenReturn(List.of());

        // When: Execute use case
        List<TransactionResponse> responses = getTransactionHistoryUseCase.execute(query);

        // Then: Verify empty list
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();

        // Verify: Repository called
        verify(transactionRepository, times(1)).findByUserId(100L, 0, 10);
    }

    // ============================================================
    // TEST 4: Different Page Sizes
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should respect page size in query")
    void execute_RespectsPageSize_WhenSpecified() {
        // Given: Query with page size 5
        query = GetTransactionHistoryQuery.builder()
                .userId(100L)
                .page(0)
                .size(5)
                .build();

        when(transactionRepository.findByUserId(eq(100L), eq(0), eq(5)))
                .thenReturn(transactions.subList(0, 2));
        when(transactionMapper.toResponseList(anyList())).thenReturn(List.of());

        // When: Execute use case
        getTransactionHistoryUseCase.execute(query);

        // Then: Verify repository called with size 5
        verify(transactionRepository, times(1)).findByUserId(100L, 0, 5);
    }

    // ============================================================
    // TEST 5: Different Pages
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should handle pagination correctly")
    void execute_HandlesPagination_WhenMultiplePages() {
        // Given: Query for page 2
        query = GetTransactionHistoryQuery.builder()
                .userId(100L)
                .page(2)
                .size(10)
                .build();

        when(transactionRepository.findByUserId(eq(100L), eq(2), eq(10)))
                .thenReturn(List.of());
        when(transactionMapper.toResponseList(anyList())).thenReturn(List.of());

        // When: Execute use case
        getTransactionHistoryUseCase.execute(query);

        // Then: Verify repository called with page 2
        verify(transactionRepository, times(1)).findByUserId(100L, 2, 10);
    }

    // NOTE: These tests will FAIL because GetTransactionHistoryUseCase doesn't exist yet!
}
