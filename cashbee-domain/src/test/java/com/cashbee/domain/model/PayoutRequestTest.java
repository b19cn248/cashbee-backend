package com.cashbee.domain.model;

import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for PayoutRequest Domain Model
 *
 * Business Scenario:
 * Users request payout (withdrawal) when they want to cash out their balance.
 * Admin reviews and approves/rejects the request.
 * System processes approved payouts and transfers money to user's account.
 *
 * @author CashBee Team
 */
@DisplayName("PayoutRequest Domain Model Tests (TDD)")
class PayoutRequestTest {

    private PayoutRequest payoutRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Given: A standard payout request
        payoutRequest = PayoutRequest.builder()
                .id(1L)
                .userId(100L)
                .walletId(10L)
                .amount(new BigDecimal("500000.00")) // 500k VND
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .bankName("Vietcombank")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();
    }

    // ============================================================
    // TEST 1: Builder - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should build payout request successfully with all required fields")
    void builder_CreatesPayoutRequest_WhenAllFieldsProvided() {
        // Then: Verify all fields
        assertThat(payoutRequest.getId()).isEqualTo(1L);
        assertThat(payoutRequest.getUserId()).isEqualTo(100L);
        assertThat(payoutRequest.getWalletId()).isEqualTo(10L);
        assertThat(payoutRequest.getAmount())
                .isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(payoutRequest.getPayoutMethod()).isEqualTo(PayoutMethod.BANK);
        assertThat(payoutRequest.getAccountNumber()).isEqualTo("1234567890");
        assertThat(payoutRequest.getAccountName()).isEqualTo("NGUYEN VAN A");
        assertThat(payoutRequest.getBankName()).isEqualTo("Vietcombank");
        assertThat(payoutRequest.getStatus()).isEqualTo(PayoutStatus.REQUESTED);
        assertThat(payoutRequest.getRequestedAt()).isEqualTo(now);
    }

    // ============================================================
    // TEST 2: Validation - Amount
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is null")
    void validate_ThrowsException_WhenAmountIsNull() {
        // Given: Payout request with null amount
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(null)
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is zero or negative")
    void validate_ThrowsException_WhenAmountIsZeroOrNegative() {
        // Test zero amount
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(BigDecimal.ZERO)
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("greater than zero");

        // Test negative amount
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("-100.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when amount is below minimum (50,000)")
    void validate_ThrowsException_WhenAmountBelowMinimum() {
        // Given: Payout request with amount below minimum
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("40000.00")) // Below 50k minimum
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        // When & Then: Validation should fail
        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50000")
                .hasMessageContaining("minimum");
    }

    // ============================================================
    // TEST 3: Validation - Required Fields
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when userId is null")
    void validate_ThrowsException_WhenUserIdIsNull() {
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(null).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when walletId is null")
    void validate_ThrowsException_WhenWalletIdIsNull() {
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(null)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when payoutMethod is null")
    void validate_ThrowsException_WhenPayoutMethodIsNull() {
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(null)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payoutMethod");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when accountNumber is null or blank")
    void validate_ThrowsException_WhenAccountNumberIsNullOrBlank() {
        // Null account number
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber(null).accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountNumber");

        // Blank account number
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("   ").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountNumber");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when accountName is null or blank")
    void validate_ThrowsException_WhenAccountNameIsNullOrBlank() {
        // Null account name
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName(null)
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountName");

        // Blank account name
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("   ")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountName");
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when status is null")
    void validate_ThrowsException_WhenStatusIsNull() {
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(null)
                .requestedAt(now)
                .build();

        assertThatThrownBy(() -> payoutRequest.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    // ============================================================
    // TEST 4: Validation - All Valid
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should pass validation when all fields are valid")
    void validate_Success_WhenAllFieldsValid() {
        // Given: Valid payout request (from setUp)

        // When & Then: Should not throw exception
        assertThatCode(() -> payoutRequest.validate())
                .doesNotThrowAnyException();
    }

    // ============================================================
    // TEST 5: Business Logic - Approve Payout
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should approve payout when status is REQUESTED")
    void approve_UpdatesStatusToProcessing_WhenStatusIsRequested() {
        // Given: Payout with REQUESTED status

        // When: Approve payout
        payoutRequest.approve();

        // Then: Status should be PROCESSING
        assertThat(payoutRequest.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        assertThat(payoutRequest.getProcessedAt()).isNotNull();
        assertThat(payoutRequest.getProcessedBy()).isNotNull();
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when approving non-REQUESTED payout")
    void approve_ThrowsException_WhenStatusIsNotRequested() {
        // Given: Payout already PROCESSING
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.PROCESSING)
                .requestedAt(now)
                .build();

        // When & Then: Should throw exception
        assertThatThrownBy(() -> payoutRequest.approve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REQUESTED")
                .hasMessageContaining("approve");
    }

    // ============================================================
    // TEST 6: Business Logic - Reject Payout
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should reject payout when status is REQUESTED")
    void reject_UpdatesStatusToRejected_WhenStatusIsRequested() {
        // Given: Payout with REQUESTED status
        String reason = "Insufficient balance";

        // When: Reject payout
        payoutRequest.reject(reason);

        // Then: Status should be REJECTED
        assertThat(payoutRequest.getStatus()).isEqualTo(PayoutStatus.REJECTED);
        assertThat(payoutRequest.getRejectionReason()).isEqualTo(reason);
        assertThat(payoutRequest.getProcessedAt()).isNotNull();
        assertThat(payoutRequest.getProcessedBy()).isNotNull();
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when rejecting without reason")
    void reject_ThrowsException_WhenReasonIsNullOrBlank() {
        // When & Then: Reject with null reason
        assertThatThrownBy(() -> payoutRequest.reject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        // Reject with blank reason
        assertThatThrownBy(() -> payoutRequest.reject("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    // ============================================================
    // TEST 7: Business Logic - Complete Payout
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should complete payout when status is PROCESSING")
    void complete_UpdatesStatusToPaid_WhenStatusIsProcessing() {
        // Given: Payout with PROCESSING status
        payoutRequest = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .status(PayoutStatus.PROCESSING)
                .requestedAt(now)
                .processedAt(now)
                .build();

        // When: Complete payout
        payoutRequest.complete();

        // Then: Status should be PAID
        assertThat(payoutRequest.getStatus()).isEqualTo(PayoutStatus.PAID);
        assertThat(payoutRequest.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when completing non-PROCESSING payout")
    void complete_ThrowsException_WhenStatusIsNotProcessing() {
        // Given: Payout with REQUESTED status (not PROCESSING)

        // When & Then: Should throw exception
        assertThatThrownBy(() -> payoutRequest.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROCESSING")
                .hasMessageContaining("complete");
    }

    // ============================================================
    // TEST 8: Business Logic - Cancel Payout
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should cancel payout when status is REQUESTED")
    void cancel_UpdatesStatusToCancelled_WhenStatusIsRequested() {
        // Given: Payout with REQUESTED status
        String reason = "User cancelled";

        // When: Cancel payout
        payoutRequest.cancel(reason);

        // Then: Status should be CANCELLED
        assertThat(payoutRequest.getStatus()).isEqualTo(PayoutStatus.CANCELLED);
        assertThat(payoutRequest.getRejectionReason()).isEqualTo(reason);
    }

    // ============================================================
    // TEST 9: Different Payout Methods
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should support all payout methods")
    void builder_SupportsAllPayoutMethods() {
        // Test BANK
        PayoutRequest bank = PayoutRequest.builder()
                .id(1L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.BANK)
                .accountNumber("123").accountName("Test")
                .bankName("Vietcombank")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();
        assertThat(bank.getPayoutMethod()).isEqualTo(PayoutMethod.BANK);
        assertThat(bank.getBankName()).isEqualTo("Vietcombank");

        // Test MOMO
        PayoutRequest momo = PayoutRequest.builder()
                .id(2L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.MOMO)
                .accountNumber("0987654321").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();
        assertThat(momo.getPayoutMethod()).isEqualTo(PayoutMethod.MOMO);

        // Test ZALOPAY
        PayoutRequest zalopay = PayoutRequest.builder()
                .id(3L).userId(100L).walletId(10L)
                .amount(new BigDecimal("500000.00"))
                .payoutMethod(PayoutMethod.ZALOPAY)
                .accountNumber("0123456789").accountName("Test")
                .status(PayoutStatus.REQUESTED)
                .requestedAt(now)
                .build();
        assertThat(zalopay.getPayoutMethod()).isEqualTo(PayoutMethod.ZALOPAY);
    }

    // NOTE: These tests will FAIL because PayoutRequest doesn't exist yet!
}
