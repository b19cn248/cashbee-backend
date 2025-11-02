package com.cashbee.domain.model;

import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for Transaction Domain Model
 *
 * Business Scenario:
 * Transaction records all wallet operations for audit trail and history.
 * Every wallet operation (pending add, confirm, lock, unlock, deduct) creates a transaction.
 *
 * @author CashBee Team
 */
@DisplayName("Transaction Domain Model Tests (TDD)")
class TransactionTest {

    private Transaction transaction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Given: A standard transaction
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
                .createdAt(now)
                .build();
    }

    // ============================================================
    // TEST 1: Builder - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should build transaction successfully with all required fields")
    void builder_CreatesTransaction_WhenAllFieldsProvided() {
        // Then: Verify all fields
        assertThat(transaction.getId()).isEqualTo(1L);
        assertThat(transaction.getUserId()).isEqualTo(100L);
        assertThat(transaction.getWalletId()).isEqualTo(10L);
        assertThat(transaction.getType()).isEqualTo(TransactionType.CASHBACK);
        assertThat(transaction.getAmount())
                .isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(transaction.getDescription())
                .isEqualTo("Cashback from Shopee order #12345");
        assertThat(transaction.getBalanceBefore())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(transaction.getBalanceAfter())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(transaction.getCreatedAt()).isEqualTo(now);
    }

    // ============================================================
    // TEST 2: Validation - Amount
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is null")
    void validate_ThrowsException_WhenAmountIsNull() {
        // Given: Transaction with null amount
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(null)
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is zero")
    void validate_ThrowsException_WhenAmountIsZero() {
        // Given: Transaction with zero amount
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(BigDecimal.ZERO)
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is negative")
    void validate_ThrowsException_WhenAmountIsNegative() {
        // Given: Transaction with negative amount
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("-100.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("900.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("greater than zero");
    }

    // ============================================================
    // TEST 3: Validation - User ID
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when userId is null")
    void validate_ThrowsException_WhenUserIdIsNull() {
        // Given: Transaction with null userId
        transaction = Transaction.builder()
                .id(1L)
                .userId(null)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 4: Validation - Wallet ID
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when walletId is null")
    void validate_ThrowsException_WhenWalletIdIsNull() {
        // Given: Transaction with null walletId
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(null)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 5: Validation - Transaction Type
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when type is null")
    void validate_ThrowsException_WhenTypeIsNull() {
        // Given: Transaction with null type
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(null)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 6: Validation - Status
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when status is null")
    void validate_ThrowsException_WhenStatusIsNull() {
        // Given: Transaction with null status
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .status(null)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 7: Validation - Balance Before
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when balanceBefore is null")
    void validate_ThrowsException_WhenBalanceBeforeIsNull() {
        // Given: Transaction with null balanceBefore
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(null)
                .balanceAfter(new BigDecimal("1500.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanceBefore")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 8: Validation - Balance After
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when balanceAfter is null")
    void validate_ThrowsException_WhenBalanceAfterIsNull() {
        // Given: Transaction with null balanceAfter
        transaction = Transaction.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("500.00"))
                .description("Test")
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(null)
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> transaction.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanceAfter")
                .hasMessageContaining("null");
    }

    // ============================================================
    // TEST 9: Validation - All Valid
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should pass validation when all fields are valid")
    void validate_Success_WhenAllFieldsValid() {
        // Given: Valid transaction (from setUp)

        // When & Then: Should not throw exception
        assertThatCode(() -> transaction.validate())
                .doesNotThrowAnyException();
    }

    // ============================================================
    // TEST 10: Business Logic - Different Transaction Types
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should support all transaction types")
    void builder_SupportsAllTransactionTypes() {
        // Test CASHBACK
        Transaction cashback = Transaction.builder()
                .id(1L).userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("100.00"))
                .description("Cashback")
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(cashback.getType()).isEqualTo(TransactionType.CASHBACK);

        // Test WITHDRAW
        Transaction withdraw = Transaction.builder()
                .id(2L).userId(100L).walletId(10L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("50.00"))
                .description("Withdraw")
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("50.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(withdraw.getType()).isEqualTo(TransactionType.WITHDRAW);

        // Test BONUS
        Transaction bonus = Transaction.builder()
                .id(3L).userId(100L).walletId(10L)
                .type(TransactionType.BONUS)
                .amount(new BigDecimal("20.00"))
                .description("Bonus")
                .balanceBefore(new BigDecimal("50.00"))
                .balanceAfter(new BigDecimal("70.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(bonus.getType()).isEqualTo(TransactionType.BONUS);

        // Test REFERRAL
        Transaction referral = Transaction.builder()
                .id(4L).userId(100L).walletId(10L)
                .type(TransactionType.REFERRAL)
                .amount(new BigDecimal("30.00"))
                .description("Referral bonus")
                .balanceBefore(new BigDecimal("70.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(referral.getType()).isEqualTo(TransactionType.REFERRAL);

        // Test REFUND
        Transaction refund = Transaction.builder()
                .id(5L).userId(100L).walletId(10L)
                .type(TransactionType.REFUND)
                .amount(new BigDecimal("25.00"))
                .description("Refund")
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("125.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(refund.getType()).isEqualTo(TransactionType.REFUND);

        // Test ADJUSTMENT
        Transaction adjustment = Transaction.builder()
                .id(6L).userId(100L).walletId(10L)
                .type(TransactionType.ADJUSTMENT)
                .amount(new BigDecimal("10.00"))
                .description("Manual adjustment")
                .balanceBefore(new BigDecimal("125.00"))
                .balanceAfter(new BigDecimal("135.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(adjustment.getType()).isEqualTo(TransactionType.ADJUSTMENT);
    }

    // ============================================================
    // TEST 11: Business Logic - Different Statuses
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should support all transaction statuses")
    void builder_SupportsAllTransactionStatuses() {
        // Test SUCCESS
        Transaction success = Transaction.builder()
                .id(1L).userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("100.00"))
                .description("Success")
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();
        assertThat(success.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        // Test PENDING
        Transaction pending = Transaction.builder()
                .id(2L).userId(100L).walletId(10L)
                .type(TransactionType.CASHBACK)
                .amount(new BigDecimal("100.00"))
                .description("Pending")
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.PENDING)
                .createdAt(now)
                .build();
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);

        // Test FAILED
        Transaction failed = Transaction.builder()
                .id(3L).userId(100L).walletId(10L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("100.00"))
                .description("Failed")
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.FAILED)
                .createdAt(now)
                .build();
        assertThat(failed.getStatus()).isEqualTo(TransactionStatus.FAILED);

        // Test CANCELLED
        Transaction cancelled = Transaction.builder()
                .id(4L).userId(100L).walletId(10L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("100.00"))
                .description("Cancelled")
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("100.00"))
                .status(TransactionStatus.CANCELLED)
                .createdAt(now)
                .build();
        assertThat(cancelled.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
    }

    // NOTE: These tests will FAIL because Transaction domain model doesn't exist yet!
    // This is expected in TDD - RED phase.
}
