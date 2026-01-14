package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for PaymentInvoice details.
 *
 * Contains full invoice information including:
 * - Invoice metadata (number, status, timestamps)
 * - Payment details (amount, bank info)
 * - Cashback breakdown by platform
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInvoiceResponse {

    private Long id;
    private String invoiceNumber;

    // User info
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;

    // Payment details
    private BigDecimal amount;
    private String currency;
    private String transferStatus;
    private LocalDateTime transferTime;

    // Bank info
    private String bankName;
    private String bankFullName;
    private String accountNumber;
    private String accountName;

    // Cashback summary
    private Integer totalOrders;
    private BigDecimal totalCashbackAmount;

    // Platform breakdown
    private PlatformBreakdown shopee;
    private PlatformBreakdown lazada;
    private PlatformBreakdown tiki;
    private PlatformBreakdown tiktok;
    private PlatformBreakdown other;

    // Metadata
    private String description;
    private String remark;
    private LocalDateTime createdAt;

    /**
     * Platform cashback breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformBreakdown {
        private String platformName;
        private Integer orderCount;
        private BigDecimal amount;
    }

    /**
     * Create response from domain model.
     */
    public static PaymentInvoiceResponse fromDomain(com.cashbee.domain.model.PaymentInvoice invoice) {
        if (invoice == null) {
            return null;
        }

        return PaymentInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .userId(invoice.getUserId())
                .userFullName(invoice.getUserFullName())
                .userEmail(invoice.getUserEmail())
                .userPhone(invoice.getUserPhone())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .transferStatus(invoice.getTransferStatus())
                .transferTime(invoice.getTransferTime())
                .bankName(invoice.getBankName())
                .bankFullName(invoice.getBankFullName())
                .accountNumber(maskAccountNumber(invoice.getAccountNumber()))
                .accountName(invoice.getAccountName())
                .totalOrders(invoice.getTotalOrders())
                .totalCashbackAmount(invoice.getTotalCashbackAmount())
                .shopee(PlatformBreakdown.builder()
                        .platformName("Shopee")
                        .orderCount(invoice.getShopeeOrders())
                        .amount(invoice.getShopeeAmount())
                        .build())
                .lazada(PlatformBreakdown.builder()
                        .platformName("Lazada")
                        .orderCount(invoice.getLazadaOrders())
                        .amount(invoice.getLazadaAmount())
                        .build())
                .tiki(PlatformBreakdown.builder()
                        .platformName("Tiki")
                        .orderCount(invoice.getTikiOrders())
                        .amount(invoice.getTikiAmount())
                        .build())
                .tiktok(PlatformBreakdown.builder()
                        .platformName("TikTok Shop")
                        .orderCount(invoice.getTiktokOrders())
                        .amount(invoice.getTiktokAmount())
                        .build())
                .other(PlatformBreakdown.builder()
                        .platformName("Other")
                        .orderCount(invoice.getOtherOrders())
                        .amount(invoice.getOtherAmount())
                        .build())
                .description(invoice.getDescription())
                .remark(invoice.getRemark())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /**
     * Mask account number for security (show last 4 digits only).
     */
    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        int length = accountNumber.length();
        return "*".repeat(length - 4) + accountNumber.substring(length - 4);
    }
}
