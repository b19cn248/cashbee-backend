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

    /**
     * Total amount (cashback + bonus + referrer commission).
     * This is the actual amount transferred to the user's bank account.
     * @deprecated Use totalAmount instead. Kept for backward compatibility.
     */
    private BigDecimal amount;

    /**
     * Cashback amount from orders (tiền hoàn từ đơn hàng).
     */
    private BigDecimal cashbackAmount;

    /**
     * Milestone bonus amount (thưởng mốc).
     */
    private BigDecimal bonusAmount;

    /**
     * Referrer commission amount (hoa hồng giới thiệu - 5% từ đơn hàng của người được giới thiệu).
     */
    private BigDecimal referrerCommissionAmount;

    /**
     * Total amount = cashbackAmount + bonusAmount + referrerCommissionAmount.
     * This is the actual amount transferred to the user's bank account.
     */
    private BigDecimal totalAmount;

    private String currency;
    private String transferStatus;
    private Integer totalOrders;
    private LocalDateTime transferTime;
    private LocalDateTime createdAt;

    /**
     * Create summary from domain model.
     *
     * Các loại tiền trong invoice:
     * - cashback: tính từ tổng các platform (shopee + lazada + tiki + tiktok + other)
     *   Lý do: field totalCashbackAmount có thể = 0 với dữ liệu cũ, nhưng platform amounts luôn đúng
     * - bonus: từ invoice.bonusAmount (thưởng mốc milestone)
     * - referrerCommission: từ invoice.referrerCommissionAmount (hoa hồng 5% từ đơn hàng của referee)
     *
     * Lưu ý: User có thể chỉ có bonus + commission mà không có cashback (ví dụ: chỉ giới thiệu người khác).
     */
    public static InvoiceSummaryResponse fromDomain(com.cashbee.domain.model.PaymentInvoice invoice) {
        if (invoice == null) {
            return null;
        }

        // Use actual transfer amount as source of truth (not platform breakdown which can be wrong)
        BigDecimal total = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;

        BigDecimal bonus = invoice.getBonusAmount() != null
                ? invoice.getBonusAmount()
                : BigDecimal.ZERO;

        BigDecimal referrerCommission = invoice.getReferrerCommissionAmount() != null
                ? invoice.getReferrerCommissionAmount()
                : BigDecimal.ZERO;

        // Derive cashback = total - bonus - commission (reliable since amount is always correct)
        // Guard against negative: some legacy invoices have commission not included in transfer amount
        BigDecimal cashback = total.subtract(bonus).subtract(referrerCommission);
        if (cashback.compareTo(BigDecimal.ZERO) < 0) {
            cashback = BigDecimal.ZERO;
        }

        return InvoiceSummaryResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                // Breakdown amounts
                .cashbackAmount(cashback)
                .bonusAmount(bonus)
                .referrerCommissionAmount(referrerCommission)
                .totalAmount(total)
                // amount = totalAmount (backward compatible cho mobile app cũ)
                .amount(total)
                // Other fields
                .currency(invoice.getCurrency())
                .transferStatus(invoice.getTransferStatus())
                .totalOrders(invoice.getTotalOrders())
                .transferTime(invoice.getTransferTime())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /**
     * Tính tổng cashback từ các platform amounts.
     *
     * Formula: cashback = shopeeAmount + lazadaAmount + tikiAmount + tiktokAmount + otherAmount
     *
     * Lý do dùng cách này thay vì totalCashbackAmount:
     * - Dữ liệu cũ có totalCashbackAmount = 0 nhưng platform amounts đúng
     * - Platform amounts là source of truth (được set đúng khi tạo invoice)
     */
    private static BigDecimal calculateTotalCashbackFromPlatforms(com.cashbee.domain.model.PaymentInvoice invoice) {
        BigDecimal total = BigDecimal.ZERO;

        if (invoice.getShopeeAmount() != null) {
            total = total.add(invoice.getShopeeAmount());
        }
        if (invoice.getLazadaAmount() != null) {
            total = total.add(invoice.getLazadaAmount());
        }
        if (invoice.getTikiAmount() != null) {
            total = total.add(invoice.getTikiAmount());
        }
        if (invoice.getTiktokAmount() != null) {
            total = total.add(invoice.getTiktokAmount());
        }
        if (invoice.getOtherAmount() != null) {
            total = total.add(invoice.getOtherAmount());
        }

        return total;
    }
}
