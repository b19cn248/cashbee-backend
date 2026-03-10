package com.cashbee.application.dto.invoice;

import com.cashbee.domain.model.PaymentInvoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for InvoiceSummaryResponse.fromDomain().
 *
 * Bug: Invoice list shows wrong amount when platform breakdown data is incorrect.
 * Fix: Derive amounts from invoice.getAmount() (actual transfer) instead of platform sum.
 */
@DisplayName("InvoiceSummaryResponse Tests")
class InvoiceSummaryResponseTest {

    @Nested
    @DisplayName("fromDomain - Amount derivation from actual transfer")
    class FromDomainAmountDerivation {

        @Test
        @DisplayName("Should use invoice.amount as totalAmount, not platform sum - batch 24 bug")
        void fromDomain_UsesActualTransferAmount_NotPlatformSum() {
            // Given: Invoice with correct amount=3547 but wrong shopeeAmount=128866
            PaymentInvoice invoice = PaymentInvoice.builder()
                    .id(1L)
                    .invoiceNumber("INV-20260309-00011")
                    .amount(new BigDecimal("3547"))
                    .shopeeAmount(new BigDecimal("128866"))  // WRONG platform data
                    .shopeeOrders(9)
                    .bonusAmount(BigDecimal.ZERO)
                    .referrerCommissionAmount(BigDecimal.ZERO)
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(3)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            InvoiceSummaryResponse response = InvoiceSummaryResponse.fromDomain(invoice);

            // Then: amount should be 3547 (actual transfer), not 128866 (wrong platform sum)
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("3547"));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3547"));
            assertThat(response.getCashbackAmount()).isEqualByComparingTo(new BigDecimal("3547"));
        }

        @Test
        @DisplayName("Should derive cashbackAmount = amount - bonus - commission")
        void fromDomain_DerivesCashbackFromAmountMinusBonusAndCommission() {
            // Given: Invoice with amount=10000, bonus=2000, commission=1000
            PaymentInvoice invoice = PaymentInvoice.builder()
                    .id(2L)
                    .invoiceNumber("INV-20260309-00012")
                    .amount(new BigDecimal("10000"))
                    .shopeeAmount(new BigDecimal("5000"))
                    .lazadaAmount(new BigDecimal("2000"))
                    .bonusAmount(new BigDecimal("2000"))
                    .referrerCommissionAmount(new BigDecimal("1000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(5)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            InvoiceSummaryResponse response = InvoiceSummaryResponse.fromDomain(invoice);

            // Then: cashback = 10000 - 2000 - 1000 = 7000
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(response.getCashbackAmount()).isEqualByComparingTo(new BigDecimal("7000"));
            assertThat(response.getBonusAmount()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(response.getReferrerCommissionAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("Should handle null amount gracefully")
        void fromDomain_HandlesNullAmount_ReturnsZero() {
            // Given: Invoice with null amount
            PaymentInvoice invoice = PaymentInvoice.builder()
                    .id(3L)
                    .invoiceNumber("INV-20260309-00013")
                    .amount(null)
                    .bonusAmount(BigDecimal.ZERO)
                    .referrerCommissionAmount(BigDecimal.ZERO)
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            InvoiceSummaryResponse response = InvoiceSummaryResponse.fromDomain(invoice);

            // Then
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getCashbackAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle null bonus and commission")
        void fromDomain_HandlesNullBonusAndCommission() {
            // Given
            PaymentInvoice invoice = PaymentInvoice.builder()
                    .id(4L)
                    .invoiceNumber("INV-20260309-00014")
                    .amount(new BigDecimal("5000"))
                    .bonusAmount(null)
                    .referrerCommissionAmount(null)
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(2)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            InvoiceSummaryResponse response = InvoiceSummaryResponse.fromDomain(invoice);

            // Then: cashback = 5000 - 0 - 0 = 5000
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(response.getCashbackAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("Should guard against negative cashback when commission not in transfer")
        void fromDomain_GuardsAgainstNegativeCashback() {
            // Given: Legacy case where commission was not included in transfer amount
            // amount=10000, bonus=10000, commission=257 → cashback would be -257
            PaymentInvoice invoice = PaymentInvoice.builder()
                    .id(5L)
                    .invoiceNumber("INV-20260309-00015")
                    .amount(new BigDecimal("10000"))
                    .bonusAmount(new BigDecimal("10000"))
                    .referrerCommissionAmount(new BigDecimal("257"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            InvoiceSummaryResponse response = InvoiceSummaryResponse.fromDomain(invoice);

            // Then: cashback should be 0, not -257
            assertThat(response.getCashbackAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should return null for null invoice")
        void fromDomain_ReturnsNull_ForNullInvoice() {
            assertThat(InvoiceSummaryResponse.fromDomain(null)).isNull();
        }
    }
}
