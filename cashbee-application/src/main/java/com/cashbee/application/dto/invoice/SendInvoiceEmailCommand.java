package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Command DTO for sending invoice emails.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendInvoiceEmailCommand {

    /**
     * Invoice ID to send email for.
     */
    private Long invoiceId;

    /**
     * User email address.
     */
    private String userEmail;

    /**
     * User's full name for personalization.
     */
    private String userName;

    /**
     * Create command for a single invoice.
     */
    public static SendInvoiceEmailCommand forInvoice(Long invoiceId, String email, String name) {
        return SendInvoiceEmailCommand.builder()
                .invoiceId(invoiceId)
                .userEmail(email)
                .userName(name)
                .build();
    }

    /**
     * Batch command for multiple invoices.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchCommand {
        private List<SendInvoiceEmailCommand> commands;
    }
}
