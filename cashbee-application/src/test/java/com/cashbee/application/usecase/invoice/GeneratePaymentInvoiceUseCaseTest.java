package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.GenerateInvoiceCommand;
import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for GeneratePaymentInvoiceUseCase.
 *
 * Business Scenarios:
 * 1. Generate invoice for successful batch transfer
 * 2. Calculate platform breakdown correctly
 * 3. Generate unique invoice number
 * 4. Handle empty platform orders
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratePaymentInvoiceUseCase Tests")
class GeneratePaymentInvoiceUseCaseTest {

    @Mock
    private PaymentInvoiceRepository invoiceRepository;

    @InjectMocks
    private GeneratePaymentInvoiceUseCase generatePaymentInvoiceUseCase;

    // Test data
    private static final Long USER_ID = 100L;
    private static final Long BATCH_ID = 10L;
    private static final Long BATCH_ITEM_ID = 50L;

    @BeforeEach
    void setUp() {
        // Reset any static state if needed
    }

    // ============================================================
    // TEST GROUP 1: Generate Invoice - Happy Path
    // ============================================================

    @Nested
    @DisplayName("Generate Invoice - Happy Path")
    class GenerateInvoiceHappyPath {

        @Test
        @DisplayName("Should generate invoice with correct basic details")
        void execute_GeneratesInvoiceWithCorrectDetails_Success() {
            // Given
            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .batchItemId(BATCH_ITEM_ID)
                    .amount(new BigDecimal("150000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(5)
                    .bankName("Vietcombank")
                    .bankAccountNumber("1234567890")
                    .transferTime(LocalDateTime.now())
                    .build();

            when(invoiceRepository.save(any(PaymentInvoice.class))).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .batchItemId(input.getBatchItemId())
                        .amount(input.getAmount())
                        .currency(input.getCurrency())
                        .transferStatus(input.getTransferStatus())
                        .totalOrders(input.getTotalOrders())
                        .bankName(input.getBankName())
                        .accountNumber(input.getAccountNumber())
                        .transferTime(input.getTransferTime())
                        .emailSent(input.getEmailSent())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            PaymentInvoiceResponse result = generatePaymentInvoiceUseCase.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getInvoiceNumber()).startsWith("INV-");
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(result.getCurrency()).isEqualTo("VND");
            assertThat(result.getTransferStatus()).isEqualTo("SUCCESS");
            assertThat(result.getTotalOrders()).isEqualTo(5);
            assertThat(result.getBankName()).isEqualTo("Vietcombank");

            // Verify save was called
            verify(invoiceRepository).save(any(PaymentInvoice.class));
        }

        @Test
        @DisplayName("Should generate unique invoice number format")
        void execute_GeneratesUniqueInvoiceNumber_CorrectFormat() {
            // Given
            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("50000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(2)
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .currency(input.getCurrency())
                        .transferStatus(input.getTransferStatus())
                        .totalOrders(input.getTotalOrders())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            generatePaymentInvoiceUseCase.execute(command);

            // Then
            PaymentInvoice savedInvoice = captor.getValue();
            String invoiceNumber = savedInvoice.getInvoiceNumber();

            // Verify format: INV-YYYYMMDD-XXXXX
            assertThat(invoiceNumber).matches("INV-\\d{8}-\\d{5}");
        }
    }

    // ============================================================
    // TEST GROUP 2: Platform Breakdown Calculation
    // ============================================================

    @Nested
    @DisplayName("Platform Breakdown Calculation")
    class PlatformBreakdownCalculation {

        @Test
        @DisplayName("Should calculate Shopee orders correctly")
        void execute_CalculatesShopeeOrders_Correctly() {
            // Given
            GenerateInvoiceCommand.PlatformOrderDetail shopeeDetail =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("SHOPEE")
                            .orderCount(3)
                            .totalAmount(new BigDecimal("80000"))
                            .build();

            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("80000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(3)
                    .platformOrders(Collections.singletonList(shopeeDetail))
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .shopeeOrders(input.getShopeeOrders())
                        .shopeeAmount(input.getShopeeAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            PaymentInvoiceResponse result = generatePaymentInvoiceUseCase.execute(command);

            // Then
            PaymentInvoice savedInvoice = captor.getValue();
            assertThat(savedInvoice.getShopeeOrders()).isEqualTo(3);
            assertThat(savedInvoice.getShopeeAmount()).isEqualByComparingTo(new BigDecimal("80000"));
        }

        @Test
        @DisplayName("Should calculate all platforms correctly")
        void execute_CalculatesAllPlatforms_Correctly() {
            // Given
            GenerateInvoiceCommand.PlatformOrderDetail shopeeDetail =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("shopee")
                            .orderCount(2)
                            .totalAmount(new BigDecimal("50000"))
                            .build();

            GenerateInvoiceCommand.PlatformOrderDetail lazadaDetail =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("lazada")
                            .orderCount(3)
                            .totalAmount(new BigDecimal("70000"))
                            .build();

            GenerateInvoiceCommand.PlatformOrderDetail tikiDetail =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("tiki")
                            .orderCount(1)
                            .totalAmount(new BigDecimal("30000"))
                            .build();

            GenerateInvoiceCommand.PlatformOrderDetail tiktokDetail =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("tiktok")
                            .orderCount(2)
                            .totalAmount(new BigDecimal("40000"))
                            .build();

            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("190000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(8)
                    .platformOrders(Arrays.asList(shopeeDetail, lazadaDetail, tikiDetail, tiktokDetail))
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .shopeeOrders(input.getShopeeOrders())
                        .shopeeAmount(input.getShopeeAmount())
                        .lazadaOrders(input.getLazadaOrders())
                        .lazadaAmount(input.getLazadaAmount())
                        .tikiOrders(input.getTikiOrders())
                        .tikiAmount(input.getTikiAmount())
                        .tiktokOrders(input.getTiktokOrders())
                        .tiktokAmount(input.getTiktokAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            generatePaymentInvoiceUseCase.execute(command);

            // Then
            PaymentInvoice savedInvoice = captor.getValue();
            assertThat(savedInvoice.getShopeeOrders()).isEqualTo(2);
            assertThat(savedInvoice.getShopeeAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(savedInvoice.getLazadaOrders()).isEqualTo(3);
            assertThat(savedInvoice.getLazadaAmount()).isEqualByComparingTo(new BigDecimal("70000"));
            assertThat(savedInvoice.getTikiOrders()).isEqualTo(1);
            assertThat(savedInvoice.getTikiAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(savedInvoice.getTiktokOrders()).isEqualTo(2);
            assertThat(savedInvoice.getTiktokAmount()).isEqualByComparingTo(new BigDecimal("40000"));
        }

        @Test
        @DisplayName("Should categorize unknown platforms as 'other'")
        void execute_CategorizeUnknownPlatformsAsOther_Correctly() {
            // Given
            GenerateInvoiceCommand.PlatformOrderDetail unknownDetail1 =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("sendo")
                            .orderCount(2)
                            .totalAmount(new BigDecimal("30000"))
                            .build();

            GenerateInvoiceCommand.PlatformOrderDetail unknownDetail2 =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("amazon")
                            .orderCount(1)
                            .totalAmount(new BigDecimal("20000"))
                            .build();

            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("50000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(3)
                    .platformOrders(Arrays.asList(unknownDetail1, unknownDetail2))
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .otherOrders(input.getOtherOrders())
                        .otherAmount(input.getOtherAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            generatePaymentInvoiceUseCase.execute(command);

            // Then
            PaymentInvoice savedInvoice = captor.getValue();
            assertThat(savedInvoice.getOtherOrders()).isEqualTo(3); // 2 + 1
            assertThat(savedInvoice.getOtherAmount()).isEqualByComparingTo(new BigDecimal("50000")); // 30000 + 20000
        }
    }

    // ============================================================
    // TEST GROUP 3: Edge Cases
    // ============================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null platform orders")
        void execute_HandlesNullPlatformOrders_Success() {
            // Given
            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("100000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(5)
                    .platformOrders(null) // Explicitly null
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .shopeeOrders(input.getShopeeOrders())
                        .shopeeAmount(input.getShopeeAmount())
                        .lazadaOrders(input.getLazadaOrders())
                        .lazadaAmount(input.getLazadaAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            PaymentInvoiceResponse result = generatePaymentInvoiceUseCase.execute(command);

            // Then - Should not throw and all platform amounts should be zero
            assertThat(result).isNotNull();
            PaymentInvoice savedInvoice = captor.getValue();
            assertThat(savedInvoice.getShopeeOrders()).isEqualTo(0);
            assertThat(savedInvoice.getShopeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedInvoice.getLazadaOrders()).isEqualTo(0);
            assertThat(savedInvoice.getLazadaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle empty platform orders list")
        void execute_HandlesEmptyPlatformOrders_Success() {
            // Given
            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("100000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(5)
                    .platformOrders(Collections.emptyList())
                    .build();

            when(invoiceRepository.save(any(PaymentInvoice.class))).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When & Then - Should not throw
            PaymentInvoiceResponse result = generatePaymentInvoiceUseCase.execute(command);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should handle case-insensitive platform codes")
        void execute_HandlesCaseInsensitivePlatformCodes_Correctly() {
            // Given - Mixed case platform codes
            GenerateInvoiceCommand.PlatformOrderDetail shopeeUpper =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("SHOPEE")
                            .orderCount(1)
                            .totalAmount(new BigDecimal("10000"))
                            .build();

            GenerateInvoiceCommand.PlatformOrderDetail lazadaMixed =
                    GenerateInvoiceCommand.PlatformOrderDetail.builder()
                            .platformCode("LaZaDa")
                            .orderCount(1)
                            .totalAmount(new BigDecimal("10000"))
                            .build();

            GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
                    .userId(USER_ID)
                    .batchId(BATCH_ID)
                    .amount(new BigDecimal("20000"))
                    .currency("VND")
                    .transferStatus("SUCCESS")
                    .totalOrders(2)
                    .platformOrders(Arrays.asList(shopeeUpper, lazadaMixed))
                    .build();

            ArgumentCaptor<PaymentInvoice> captor = ArgumentCaptor.forClass(PaymentInvoice.class);
            when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> {
                PaymentInvoice input = invocation.getArgument(0);
                return PaymentInvoice.builder()
                        .id(1L)
                        .invoiceNumber(input.getInvoiceNumber())
                        .userId(input.getUserId())
                        .batchId(input.getBatchId())
                        .amount(input.getAmount())
                        .shopeeOrders(input.getShopeeOrders())
                        .shopeeAmount(input.getShopeeAmount())
                        .lazadaOrders(input.getLazadaOrders())
                        .lazadaAmount(input.getLazadaAmount())
                        .createdAt(input.getCreatedAt())
                        .build();
            });

            // When
            generatePaymentInvoiceUseCase.execute(command);

            // Then - Should recognize both regardless of case
            PaymentInvoice savedInvoice = captor.getValue();
            assertThat(savedInvoice.getShopeeOrders()).isEqualTo(1);
            assertThat(savedInvoice.getLazadaOrders()).isEqualTo(1);
        }
    }

    // ============================================================
    // TEST GROUP 4: Invoice Number Generation from DB
    // ============================================================

    @Nested
    @DisplayName("Invoice Number Generation from DB")
    class InvoiceNumberGenerationFromDb {

        @Test
        @DisplayName("Should generate invoice number based on existing count")
        void generateInvoiceNumberFromDb_GeneratesBasedOnExistingCount() {
            // Given
            when(invoiceRepository.countByInvoiceNumberStartingWith(anyString()))
                    .thenReturn(5L);

            // When
            String invoiceNumber = generatePaymentInvoiceUseCase.generateInvoiceNumberFromDb();

            // Then
            assertThat(invoiceNumber).matches("INV-\\d{8}-00006");
        }

        @Test
        @DisplayName("Should generate first invoice number of the day")
        void generateInvoiceNumberFromDb_GeneratesFirstOfDay() {
            // Given
            when(invoiceRepository.countByInvoiceNumberStartingWith(anyString()))
                    .thenReturn(0L);

            // When
            String invoiceNumber = generatePaymentInvoiceUseCase.generateInvoiceNumberFromDb();

            // Then
            assertThat(invoiceNumber).matches("INV-\\d{8}-00001");
        }
    }
}
