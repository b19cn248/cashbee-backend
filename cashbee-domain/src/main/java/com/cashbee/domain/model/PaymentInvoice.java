package com.cashbee.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PaymentInvoice Domain Model.
 *
 * Represents a payment invoice/receipt generated when a batch transfer is completed.
 * Each user in a batch gets one invoice with:
 * - Payment details (amount, bank info)
 * - Cashback summary by platform (Shopee, Lazada, Tiki, TikTok, etc.)
 * - Email delivery status
 *
 * Invoice Number Format: INV-YYYYMMDD-XXXXX
 * Example: INV-20260104-00001
 *
 * Business Purpose:
 * - Provide users with official payment receipt
 * - Traceability: Invoice -> Batch -> Cashbacks -> Orders
 * - Email notification with summary (Option B approach)
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "invoiceNumber"})
public class PaymentInvoice {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Unique invoice number.
     * Format: INV-YYYYMMDD-XXXXX
     * Example: INV-20260104-00001
     */
    private String invoiceNumber;

    /**
     * Reference to user.
     */
    private Long userId;

    /**
     * Reference to batch_transfer_export.
     */
    private Long batchId;

    /**
     * Reference to batch_transfer_item.
     */
    private Long batchItemId;

    /**
     * Reference to transaction (withdrawal transaction).
     */
    private Long transactionId;

    // ===== Payment Details =====

    /**
     * Total payment amount.
     */
    private BigDecimal amount;

    /**
     * Currency code (default: VND).
     */
    @Builder.Default
    private String currency = "VND";

    /**
     * Transfer status: SUCCESS, FAILED, PARTIAL.
     */
    private String transferStatus;

    /**
     * Time when transfer was completed.
     */
    private LocalDateTime transferTime;

    // ===== User Info Snapshot (denormalized) =====

    /**
     * User's full name at time of payment.
     */
    private String userFullName;

    /**
     * User's email at time of payment.
     */
    private String userEmail;

    /**
     * User's phone at time of payment.
     */
    private String userPhone;

    // ===== Bank Info Snapshot =====

    /**
     * Bank short name (e.g., Vietcombank, BIDV).
     */
    private String bankName;

    /**
     * Bank full name.
     */
    private String bankFullName;

    /**
     * Bank account number.
     */
    private String accountNumber;

    /**
     * Account holder name.
     */
    private String accountName;

    // ===== Cashback Summary by Platform (Option B: Summary approach) =====

    /**
     * Total number of orders in this payment.
     */
    @Builder.Default
    private Integer totalOrders = 0;

    /**
     * Total cashback amount from all platforms.
     */
    @Builder.Default
    private BigDecimal totalCashbackAmount = BigDecimal.ZERO;

    /**
     * Number of Shopee orders.
     */
    @Builder.Default
    private Integer shopeeOrders = 0;

    /**
     * Total cashback from Shopee.
     */
    @Builder.Default
    private BigDecimal shopeeAmount = BigDecimal.ZERO;

    /**
     * Number of Lazada orders.
     */
    @Builder.Default
    private Integer lazadaOrders = 0;

    /**
     * Total cashback from Lazada.
     */
    @Builder.Default
    private BigDecimal lazadaAmount = BigDecimal.ZERO;

    /**
     * Number of Tiki orders.
     */
    @Builder.Default
    private Integer tikiOrders = 0;

    /**
     * Total cashback from Tiki.
     */
    @Builder.Default
    private BigDecimal tikiAmount = BigDecimal.ZERO;

    /**
     * Number of TikTok Shop orders.
     */
    @Builder.Default
    private Integer tiktokOrders = 0;

    /**
     * Total cashback from TikTok Shop.
     */
    @Builder.Default
    private BigDecimal tiktokAmount = BigDecimal.ZERO;

    /**
     * Number of orders from other platforms.
     */
    @Builder.Default
    private Integer otherOrders = 0;

    /**
     * Total cashback from other platforms.
     */
    @Builder.Default
    private BigDecimal otherAmount = BigDecimal.ZERO;

    // ===== Additional Info =====

    /**
     * Description of the payment.
     */
    private String description;

    /**
     * Remark (from batch transfer).
     */
    private String remark;

    // ===== Email Tracking =====

    /**
     * Whether email has been sent.
     */
    @Builder.Default
    private Boolean emailSent = false;

    /**
     * Timestamp when email was sent.
     */
    private LocalDateTime emailSentAt;

    /**
     * Error message if email sending failed.
     */
    private String emailError;

    // ===== Timestamps =====

    /**
     * Timestamp when invoice was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when invoice was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (batchId == null) {
            throw new IllegalStateException("Batch ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Amount must be non-negative");
        }

        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalStateException("Invoice number is required");
        }
    }

    /**
     * Generate invoice number.
     * Format: INV-YYYYMMDD-XXXXX
     *
     * @param sequence Sequence number for the day
     * @return Generated invoice number
     */
    public static String generateInvoiceNumber(long sequence) {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("INV-%s-%05d", dateStr, sequence);
    }

    /**
     * Mark email as sent successfully.
     */
    public void markEmailSent() {
        this.emailSent = true;
        this.emailSentAt = LocalDateTime.now();
        this.emailError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark email as failed.
     *
     * @param error Error message
     */
    public void markEmailFailed(String error) {
        this.emailSent = false;
        this.emailError = error;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if email should be sent (not yet sent and no permanent error).
     */
    public boolean shouldSendEmail() {
        return !Boolean.TRUE.equals(this.emailSent) && this.userEmail != null && !this.userEmail.isBlank();
    }

    /**
     * Get masked account number for display (show last 4 digits).
     * Example: ****1234
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "****" + last4;
    }

    /**
     * Get formatted amount with currency.
     * Example: 1,500,000 VND
     */
    public String getFormattedAmount() {
        if (amount == null) {
            return "0 " + (currency != null ? currency : "VND");
        }
        return String.format("%,.0f %s", amount, currency != null ? currency : "VND");
    }

    /**
     * Add platform cashback stats.
     *
     * @param platformCode Platform code (SHOPEE, LAZADA, TIKI, TIKTOK)
     * @param orderCount   Number of orders
     * @param cashbackAmt  Total cashback amount
     */
    public void addPlatformStats(String platformCode, int orderCount, BigDecimal cashbackAmt) {
        if (platformCode == null) {
            return;
        }

        String upperCode = platformCode.toUpperCase();
        switch (upperCode) {
            case "SHOPEE" -> {
                this.shopeeOrders = (this.shopeeOrders != null ? this.shopeeOrders : 0) + orderCount;
                this.shopeeAmount = (this.shopeeAmount != null ? this.shopeeAmount : BigDecimal.ZERO).add(cashbackAmt);
            }
            case "LAZADA" -> {
                this.lazadaOrders = (this.lazadaOrders != null ? this.lazadaOrders : 0) + orderCount;
                this.lazadaAmount = (this.lazadaAmount != null ? this.lazadaAmount : BigDecimal.ZERO).add(cashbackAmt);
            }
            case "TIKI" -> {
                this.tikiOrders = (this.tikiOrders != null ? this.tikiOrders : 0) + orderCount;
                this.tikiAmount = (this.tikiAmount != null ? this.tikiAmount : BigDecimal.ZERO).add(cashbackAmt);
            }
            case "TIKTOK", "TIKTOKSHOP" -> {
                this.tiktokOrders = (this.tiktokOrders != null ? this.tiktokOrders : 0) + orderCount;
                this.tiktokAmount = (this.tiktokAmount != null ? this.tiktokAmount : BigDecimal.ZERO).add(cashbackAmt);
            }
            default -> {
                this.otherOrders = (this.otherOrders != null ? this.otherOrders : 0) + orderCount;
                this.otherAmount = (this.otherAmount != null ? this.otherAmount : BigDecimal.ZERO).add(cashbackAmt);
            }
        }

        // Update totals
        this.totalOrders = (this.totalOrders != null ? this.totalOrders : 0) + orderCount;
        this.totalCashbackAmount = (this.totalCashbackAmount != null ? this.totalCashbackAmount : BigDecimal.ZERO).add(cashbackAmt);
    }
}
