package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary DTO for invoice list display.
 *
 * Contains minimal info for list view:
 * - Invoice number, amount, status
 * - Order count, transfer time
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryResponse {

    private Long id;
    private String invoiceNumber;
    private BigDecimal amount;
    private String currency;
    private String transferStatus;
    private Integer totalOrders;
    private LocalDateTime transferTime;
    private LocalDateTime createdAt;

    /**
     * Create summary from domain model.
     */
    public static InvoiceSummaryResponse fromDomain(com.cashbee.domain.model.PaymentInvoice invoice) {
        if (invoice == null) {
            return null;
        }

        return InvoiceSummaryResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .transferStatus(invoice.getTransferStatus())
                .totalOrders(invoice.getTotalOrders())
                .transferTime(invoice.getTransferTime())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
