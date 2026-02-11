package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving detailed payment invoice information.
 *
 * Features:
 * - Get full invoice details by ID or invoice number
 * - Validates user ownership
 * - Returns complete invoice with platform breakdown
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetInvoiceDetailUseCase {

    private final PaymentInvoiceRepository invoiceRepository;

    /**
     * Get invoice detail by ID.
     *
     * @param invoiceId Invoice ID
     * @param userId User ID for ownership validation
     * @return Full invoice details
     * @throws NotFoundException if invoice not found
     * @throws SecurityException if user doesn't own the invoice
     */
    @Transactional(readOnly = true)
    public PaymentInvoiceResponse execute(Long invoiceId, Long userId) {
        log.debug("Getting invoice detail: invoiceId={}, userId={}", invoiceId, userId);

        PaymentInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));

        // Validate user ownership
        if (!invoice.getUserId().equals(userId)) {
            log.warn("User {} attempted to access invoice {} belonging to user {}",
                    userId, invoiceId, invoice.getUserId());
            throw new SecurityException("Access denied to invoice: " + invoiceId);
        }

        log.debug("Found invoice: invoiceNumber={}, amount={}",
                invoice.getInvoiceNumber(), invoice.getAmount());

        return PaymentInvoiceResponse.fromDomain(invoice);
    }

    /**
     * Get invoice detail by invoice number.
     *
     * @param invoiceNumber Invoice number (e.g., INV-20250103-00001)
     * @param userId User ID for ownership validation
     * @return Full invoice details
     * @throws NotFoundException if invoice not found
     * @throws SecurityException if user doesn't own the invoice
     */
    @Transactional(readOnly = true)
    public PaymentInvoiceResponse executeByInvoiceNumber(String invoiceNumber, Long userId) {
        log.debug("Getting invoice detail by number: invoiceNumber={}, userId={}", invoiceNumber, userId);

        PaymentInvoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceNumber));

        // Validate user ownership
        if (!invoice.getUserId().equals(userId)) {
            log.warn("User {} attempted to access invoice {} belonging to user {}",
                    userId, invoiceNumber, invoice.getUserId());
            throw new SecurityException("Access denied to invoice: " + invoiceNumber);
        }

        return PaymentInvoiceResponse.fromDomain(invoice);
    }

    /**
     * Get invoice detail by ID (admin access - no ownership check).
     *
     * @param invoiceId Invoice ID
     * @return Full invoice details
     * @throws NotFoundException if invoice not found
     */
    @Transactional(readOnly = true)
    public PaymentInvoiceResponse executeForAdmin(Long invoiceId) {
        log.debug("Admin getting invoice detail: invoiceId={}", invoiceId);

        PaymentInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));

        return PaymentInvoiceResponse.fromDomain(invoice);
    }
}
