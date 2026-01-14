package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.invoice.GetUserInvoicesQuery;
import com.cashbee.application.dto.invoice.InvoiceSummaryResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for GetUserInvoicesUseCase.
 *
 * Business Scenarios:
 * 1. User views their invoices with pagination
 * 2. User filters invoices by date range
 * 3. Empty result when no invoices found
 * 4. Count invoices for user
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserInvoicesUseCase Tests")
class GetUserInvoicesUseCaseTest {

    @Mock
    private PaymentInvoiceRepository invoiceRepository;

    @InjectMocks
    private GetUserInvoicesUseCase getUserInvoicesUseCase;

    // Test data
    private PaymentInvoice invoice1;
    private PaymentInvoice invoice2;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        invoice1 = PaymentInvoice.builder()
                .id(1L)
                .invoiceNumber("INV-20260104-00001")
                .userId(USER_ID)
                .batchId(10L)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .transferStatus("SUCCESS")
                .totalOrders(5)
                .shopeeOrders(3)
                .shopeeAmount(new BigDecimal("80000"))
                .lazadaOrders(2)
                .lazadaAmount(new BigDecimal("70000"))
                .transferTime(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        invoice2 = PaymentInvoice.builder()
                .id(2L)
                .invoiceNumber("INV-20260103-00015")
                .userId(USER_ID)
                .batchId(9L)
                .amount(new BigDecimal("85000"))
                .currency("VND")
                .transferStatus("SUCCESS")
                .totalOrders(3)
                .tikiOrders(3)
                .tikiAmount(new BigDecimal("85000"))
                .transferTime(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Happy Path - Get User Invoices
    // ============================================================

    @Nested
    @DisplayName("Get User Invoices - Happy Path")
    class GetUserInvoicesHappyPath {

        @Test
        @DisplayName("Should get user invoices with pagination")
        void execute_GetsUserInvoicesWithPagination_Success() {
            // Given
            GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                    .userId(USER_ID)
                    .page(0)
                    .size(10)
                    .build();

            List<PaymentInvoice> invoices = Arrays.asList(invoice1, invoice2);
            Page<PaymentInvoice> invoicePage = new PageImpl<>(invoices, PageRequest.of(0, 10), 2);

            when(invoiceRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(invoicePage);

            // When
            PageResponse<InvoiceSummaryResponse> result = getUserInvoicesUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            // Verify first invoice data
            InvoiceSummaryResponse first = result.getContent().get(0);
            assertThat(first.getId()).isEqualTo(1L);
            assertThat(first.getInvoiceNumber()).isEqualTo("INV-20260104-00001");
            assertThat(first.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(first.getCurrency()).isEqualTo("VND");
            assertThat(first.getTransferStatus()).isEqualTo("SUCCESS");
            assertThat(first.getTotalOrders()).isEqualTo(5);

            // Verify repository called correctly
            verify(invoiceRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get invoices filtered by date range")
        void execute_GetsInvoicesFilteredByDateRange_Success() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();

            GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                    .userId(USER_ID)
                    .startDate(startDate)
                    .endDate(endDate)
                    .page(0)
                    .size(10)
                    .build();

            List<PaymentInvoice> invoices = Collections.singletonList(invoice1);
            Page<PaymentInvoice> invoicePage = new PageImpl<>(invoices, PageRequest.of(0, 10), 1);

            when(invoiceRepository.findByUserIdAndCreatedAtBetween(
                    eq(USER_ID), eq(startDate), eq(endDate), any(Pageable.class)))
                    .thenReturn(invoicePage);

            // When
            PageResponse<InvoiceSummaryResponse> result = getUserInvoicesUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            // Verify correct repository method was called (with date range)
            verify(invoiceRepository).findByUserIdAndCreatedAtBetween(
                    eq(USER_ID), eq(startDate), eq(endDate), any(Pageable.class));
            verify(invoiceRepository, never()).findByUserId(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should use default pagination")
        void execute_UsesDefaultPagination_WhenNotSpecified() {
            // Given
            GetUserInvoicesQuery query = GetUserInvoicesQuery.forUser(USER_ID);

            Page<PaymentInvoice> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(invoiceRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            getUserInvoicesUseCase.execute(query);

            // Then - verify default pagination (page=0, size=20)
            verify(invoiceRepository).findByUserId(eq(USER_ID), argThat(pageable ->
                    pageable.getPageNumber() == 0 && pageable.getPageSize() == 20
            ));
        }
    }

    // ============================================================
    // TEST GROUP 2: Empty Results
    // ============================================================

    @Nested
    @DisplayName("Empty Results")
    class EmptyResults {

        @Test
        @DisplayName("Should return empty page when user has no invoices")
        void execute_ReturnsEmptyPage_WhenNoInvoicesFound() {
            // Given
            GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                    .userId(999L)
                    .page(0)
                    .size(10)
                    .build();

            Page<PaymentInvoice> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(invoiceRepository.findByUserId(eq(999L), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<InvoiceSummaryResponse> result = getUserInvoicesUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
        }
    }

    // ============================================================
    // TEST GROUP 3: Count by User ID
    // ============================================================

    @Nested
    @DisplayName("Count by User ID")
    class CountByUserId {

        @Test
        @DisplayName("Should return count for user")
        void countByUserId_ReturnsCount_Success() {
            // Given
            when(invoiceRepository.countByUserId(USER_ID)).thenReturn(25L);

            // When
            long count = getUserInvoicesUseCase.countByUserId(USER_ID);

            // Then
            assertThat(count).isEqualTo(25L);
            verify(invoiceRepository).countByUserId(USER_ID);
        }

        @Test
        @DisplayName("Should return 0 when user has no invoices")
        void countByUserId_ReturnsZero_WhenNoInvoices() {
            // Given
            when(invoiceRepository.countByUserId(999L)).thenReturn(0L);

            // When
            long count = getUserInvoicesUseCase.countByUserId(999L);

            // Then
            assertThat(count).isEqualTo(0L);
        }
    }

    // ============================================================
    // TEST GROUP 4: Pagination
    // ============================================================

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("Should return correct page info for middle page")
        void execute_ReturnsCorrectPageInfo_ForMiddlePage() {
            // Given
            GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                    .userId(USER_ID)
                    .page(1)
                    .size(10)
                    .build();

            List<PaymentInvoice> invoices = Collections.singletonList(invoice1);
            Page<PaymentInvoice> invoicePage = new PageImpl<>(invoices, PageRequest.of(1, 10), 25);

            when(invoiceRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(invoicePage);

            // When
            PageResponse<InvoiceSummaryResponse> result = getUserInvoicesUseCase.execute(query);

            // Then
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isFirst()).isFalse();
            assertThat(result.isLast()).isFalse();
        }

        @Test
        @DisplayName("Should return last page correctly")
        void execute_ReturnsCorrectPageInfo_ForLastPage() {
            // Given
            GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                    .userId(USER_ID)
                    .page(2)
                    .size(10)
                    .build();

            List<PaymentInvoice> invoices = Collections.singletonList(invoice1);
            Page<PaymentInvoice> invoicePage = new PageImpl<>(invoices, PageRequest.of(2, 10), 25);

            when(invoiceRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(invoicePage);

            // When
            PageResponse<InvoiceSummaryResponse> result = getUserInvoicesUseCase.execute(query);

            // Then
            assertThat(result.getPage()).isEqualTo(2);
            assertThat(result.isLast()).isTrue();
        }
    }
}
