package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.invoice.GetUserInvoicesQuery;
import com.cashbee.application.dto.invoice.InvoiceSummaryResponse;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving user's payment invoices.
 *
 * Features:
 * - Paginated list of invoices
 * - Optional date range filter
 * - Returns summary view for list display
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserInvoicesUseCase {

    private final PaymentInvoiceRepository invoiceRepository;

    /**
     * Get paginated list of invoices for a user.
     *
     * @param query Query with filters and pagination
     * @return Paginated invoice summaries
     */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> execute(GetUserInvoicesQuery query) {
        log.debug("Getting invoices for user: userId={}, page={}, size={}",
                query.getUserId(), query.getPage(), query.getSize());

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());

        Page<PaymentInvoice> invoicePage;

        if (query.getStartDate() != null && query.getEndDate() != null) {
            // Query with date range filter
            invoicePage = invoiceRepository.findByUserIdAndCreatedAtBetween(
                    query.getUserId(),
                    query.getStartDate(),
                    query.getEndDate(),
                    pageable);
        } else {
            // Query without date filter
            invoicePage = invoiceRepository.findByUserId(query.getUserId(), pageable);
        }

        List<InvoiceSummaryResponse> content = invoicePage.getContent().stream()
                .map(InvoiceSummaryResponse::fromDomain)
                .collect(Collectors.toList());

        log.debug("Found {} invoices (total: {})",
                content.size(), invoicePage.getTotalElements());

        return PageResponse.<InvoiceSummaryResponse>builder()
                .content(content)
                .page(invoicePage.getNumber())
                .size(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .totalPages(invoicePage.getTotalPages())
                .first(invoicePage.isFirst())
                .last(invoicePage.isLast())
                .build();
    }

    /**
     * Get total invoice count for a user.
     *
     * @param userId User ID
     * @return Total invoice count
     */
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        return invoiceRepository.countByUserId(userId);
    }
}
