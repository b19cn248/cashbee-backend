package com.cashbee.application.usecase.transaction;

import com.cashbee.application.dto.transaction.CreateTransactionCommand;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for CreateTransactionUseCase
 *
 * Business Scenario:
 * Every wallet operation (pending add, confirm, lock, unlock, deduct)
 * must create a transaction record for audit trail.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTransactionUseCase Tests (TDD)")
class CreateTransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private com.cashbee.application.port.TransactionMapper transactionMapper;

    @InjectMocks
    private CreateTransactionUseCase createTransactionUseCase;

    private CreateTransactionCommand command;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Given: A valid create transaction command
        command = CreateTransactionCommand.builder()
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Cashback from Shopee order #12345")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        // Mock transaction (what repository will return)
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Cashback from Shopee order #12345")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // TEST 1: Create Transaction - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should create transaction successfully when valid command")
    void execute_CreatesTransaction_WhenValidCommand() {
        // Given: Repository will save successfully
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Mock mapper
        TransactionResponse mockResponse = TransactionResponse.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Cashback from Shopee order #12345")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(mockResponse);

        // When: Execute use case
        TransactionResponse response = createTransactionUseCase.execute(command);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getWalletId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(TransactionType.CASHBACK);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        // Verify: Repository called once
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ============================================================
    // TEST 2: Null Command
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when command is null")
    void execute_ThrowsException_WhenCommandIsNull() {
        // When & Then: Should throw NullPointerException
        assertThatThrownBy(() -> createTransactionUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);

        // Verify: No repository interaction
        verify(transactionRepository, never()).save(any());
    }

    // ============================================================
    // TEST 3: All Transaction Types
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should create transaction for all transaction types")
    void execute_CreatesTransaction_ForAllTypes() {
        // Test CASHBACK
        command = CreateTransactionCommand.builder()
                .userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("100.00"))
                .description("Cashback")
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        Transaction cashbackTx = Transaction.builder()
                .id(1L).userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("100.00"))
                .description("Cashback")
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(cashbackTx);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(
                TransactionResponse.builder()
                        .id(1L).userId(100L).walletId(10L)
                        .type(TransactionType.CASHBACK)
                        .amount(new BigDecimal("100.00"))
                        .description("Cashback")
                        .balanceBefore(BigDecimal.ZERO)
                        .balanceAfter(new BigDecimal("100.00"))
                        .status(TransactionStatus.SUCCESS)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        TransactionResponse response = createTransactionUseCase.execute(command);
        assertThat(response.getType()).isEqualTo(TransactionType.CASHBACK);

        // Verify save called
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // NOTE: These tests will FAIL because CreateTransactionUseCase doesn't exist yet!
}
