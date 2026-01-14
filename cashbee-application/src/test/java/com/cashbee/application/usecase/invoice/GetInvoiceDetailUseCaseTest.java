package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for GetInvoiceDetailUseCase.
 *
 * Business Scenarios:
 * 1. User views their own invoice by ID
 * 2. User views their own invoice by invoice number
 * 3. User attempts to view another user's invoice (access denied)
 * 4. Admin views any invoice
 * 5. Invoice not found scenarios
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetInvoiceDetailUseCase Tests")
class GetInvoiceDetailUseCaseTest {

    @Mock
    private PaymentInvoiceRepository invoiceRepository;

    @InjectMocks
    private GetInvoiceDetailUseCase getInvoiceDetailUseCase;

    // Test data
    private PaymentInvoice testInvoice;
    private static final Long INVOICE_ID = 1L;
    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private static final String INVOICE_NUMBER = "INV-20260104-00001";

    @BeforeEach
    void setUp() {
        testInvoice = PaymentInvoice.builder()
                .id(INVOICE_ID)
                .invoiceNumber(INVOICE_NUMBER)
                .userId(OWNER_USER_ID)
                .batchId(10L)
                .batchItemId(50L)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .transferStatus("SUCCESS")
                .totalOrders(5)
                .shopeeOrders(3)
                .shopeeAmount(new BigDecimal("80000"))
                .lazadaOrders(2)
                .lazadaAmount(new BigDecimal("70000"))
                .tikiOrders(0)
                .tikiAmount(BigDecimal.ZERO)
                .tiktokOrders(0)
                .tiktokAmount(BigDecimal.ZERO)
                .otherOrders(0)
                .otherAmount(BigDecimal.ZERO)
                .bankName("Vietcombank")
                .accountNumber("1234567890")
                .transferTime(LocalDateTime.now().minusHours(2))
                .emailSent(true)
                .emailSentAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Get Invoice by ID - Happy Path
    // ============================================================

    @Nested
    @DisplayName("Get Invoice by ID - Happy Path")
    class GetInvoiceByIdHappyPath {

        @Test
        @DisplayName("Should get invoice when user is owner")
        void execute_GetsInvoice_WhenUserIsOwner() {
            // Given
            when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(testInvoice));

            // When
            PaymentInvoiceResponse result = getInvoiceDetailUseCase.execute(INVOICE_ID, OWNER_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(INVOICE_ID);
            assertThat(result.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
            assertThat(result.getUserId()).isEqualTo(OWNER_USER_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(result.getCurrency()).isEqualTo("VND");
            assertThat(result.getTransferStatus()).isEqualTo("SUCCESS");
            assertThat(result.getTotalOrders()).isEqualTo(5);

            // Verify platform breakdown
            assertThat(result.getShopee()).isNotNull();
            assertThat(result.getShopee().getOrderCount()).isEqualTo(3);
            assertThat(result.getShopee().getAmount()).isEqualByComparingTo(new BigDecimal("80000"));
            assertThat(result.getLazada()).isNotNull();
            assertThat(result.getLazada().getOrderCount()).isEqualTo(2);
            assertThat(result.getLazada().getAmount()).isEqualByComparingTo(new BigDecimal("70000"));

            // Verify bank info
            assertThat(result.getBankName()).isEqualTo("Vietcombank");

            verify(invoiceRepository).findById(INVOICE_ID);
        }

        @Test
        @DisplayName("Should return complete platform breakdown")
        void execute_ReturnsCompletePlatformBreakdown_Success() {
            // Given - Invoice with all platforms
            testInvoice = PaymentInvoice.builder()
                    .id(INVOICE_ID)
                    .invoiceNumber(INVOICE_NUMBER)
                    .userId(OWNER_USER_ID)
                    .batchId(10L)
                    .amount(new BigDecimal("500000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(10)
                    .shopeeOrders(3)
                    .shopeeAmount(new BigDecimal("100000"))
                    .lazadaOrders(2)
                    .lazadaAmount(new BigDecimal("80000"))
                    .tikiOrders(2)
                    .tikiAmount(new BigDecimal("120000"))
                    .tiktokOrders(2)
                    .tiktokAmount(new BigDecimal("150000"))
                    .otherOrders(1)
                    .otherAmount(new BigDecimal("50000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(testInvoice));

            // When
            PaymentInvoiceResponse result = getInvoiceDetailUseCase.execute(INVOICE_ID, OWNER_USER_ID);

            // Then
            assertThat(result.getShopee().getOrderCount()).isEqualTo(3);
            assertThat(result.getLazada().getOrderCount()).isEqualTo(2);
            assertThat(result.getTiki().getOrderCount()).isEqualTo(2);
            assertThat(result.getTiktok().getOrderCount()).isEqualTo(2);
            assertThat(result.getOther().getOrderCount()).isEqualTo(1);
            assertThat(result.getTotalOrders()).isEqualTo(10);
        }
    }

    // ============================================================
    // TEST GROUP 2: Get Invoice by Invoice Number - Happy Path
    // ============================================================

    @Nested
    @DisplayName("Get Invoice by Invoice Number - Happy Path")
    class GetInvoiceByNumberHappyPath {

        @Test
        @DisplayName("Should get invoice by invoice number when user is owner")
        void executeByInvoiceNumber_GetsInvoice_WhenUserIsOwner() {
            // Given
            when(invoiceRepository.findByInvoiceNumber(INVOICE_NUMBER))
                    .thenReturn(Optional.of(testInvoice));

            // When
            PaymentInvoiceResponse result = getInvoiceDetailUseCase.executeByInvoiceNumber(
                    INVOICE_NUMBER, OWNER_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
            assertThat(result.getUserId()).isEqualTo(OWNER_USER_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));

            verify(invoiceRepository).findByInvoiceNumber(INVOICE_NUMBER);
        }
    }

    // ============================================================
    // TEST GROUP 3: Access Denied (Not Owner)
    // ============================================================

    @Nested
    @DisplayName("Access Denied - Not Owner")
    class AccessDenied {

        @Test
        @DisplayName("Should throw SecurityException when user is not owner (by ID)")
        void execute_ThrowsSecurityException_WhenNotOwner() {
            // Given
            when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(testInvoice));

            // When & Then
            assertThatThrownBy(() -> getInvoiceDetailUseCase.execute(INVOICE_ID, OTHER_USER_ID))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Access denied to invoice: " + INVOICE_ID);

            verify(invoiceRepository).findById(INVOICE_ID);
        }

        @Test
        @DisplayName("Should throw SecurityException when user is not owner (by number)")
        void executeByInvoiceNumber_ThrowsSecurityException_WhenNotOwner() {
            // Given
            when(invoiceRepository.findByInvoiceNumber(INVOICE_NUMBER))
                    .thenReturn(Optional.of(testInvoice));

            // When & Then
            assertThatThrownBy(() -> getInvoiceDetailUseCase.executeByInvoiceNumber(
                    INVOICE_NUMBER, OTHER_USER_ID))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Access denied to invoice: " + INVOICE_NUMBER);
        }
    }

    // ============================================================
    // TEST GROUP 4: Invoice Not Found
    // ============================================================

    @Nested
    @DisplayName("Invoice Not Found")
    class InvoiceNotFound {

        @Test
        @DisplayName("Should throw NotFoundException when invoice ID not found")
        void execute_ThrowsNotFoundException_WhenInvoiceIdNotFound() {
            // Given
            Long nonExistentId = 9999L;
            when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> getInvoiceDetailUseCase.execute(nonExistentId, OWNER_USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Invoice not found: " + nonExistentId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when invoice number not found")
        void executeByInvoiceNumber_ThrowsNotFoundException_WhenNumberNotFound() {
            // Given
            String nonExistentNumber = "INV-99999999-99999";
            when(invoiceRepository.findByInvoiceNumber(nonExistentNumber))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> getInvoiceDetailUseCase.executeByInvoiceNumber(
                    nonExistentNumber, OWNER_USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Invoice not found: " + nonExistentNumber);
        }
    }

    // ============================================================
    // TEST GROUP 5: Admin Access
    // ============================================================

    @Nested
    @DisplayName("Admin Access")
    class AdminAccess {

        @Test
        @DisplayName("Should allow admin to access any invoice")
        void executeForAdmin_GetsAnyInvoice_Success() {
            // Given
            when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(testInvoice));

            // When - No userId check for admin
            PaymentInvoiceResponse result = getInvoiceDetailUseCase.executeForAdmin(INVOICE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(INVOICE_ID);
            assertThat(result.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));

            verify(invoiceRepository).findById(INVOICE_ID);
        }

        @Test
        @DisplayName("Should throw NotFoundException for admin when invoice not found")
        void executeForAdmin_ThrowsNotFoundException_WhenNotFound() {
            // Given
            Long nonExistentId = 9999L;
            when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> getInvoiceDetailUseCase.executeForAdmin(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Invoice not found: " + nonExistentId);
        }
    }
}
