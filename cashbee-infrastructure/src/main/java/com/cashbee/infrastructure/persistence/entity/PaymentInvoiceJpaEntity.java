package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for payment_invoice table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the PaymentInvoice domain model.
 *
 * Table: payment_invoice
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "payment_invoice", indexes = {
        @Index(name = "idx_payment_invoice_user", columnList = "user_id"),
        @Index(name = "idx_payment_invoice_batch", columnList = "batch_id"),
        @Index(name = "idx_payment_invoice_created", columnList = "created_at"),
        @Index(name = "idx_payment_invoice_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_payment_invoice_email_sent", columnList = "email_sent")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentInvoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique invoice number.
     * Format: INV-YYYYMMDD-XXXXX
     */
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    /**
     * Reference to user.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Reference to batch_transfer_export.
     */
    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    /**
     * Reference to batch_transfer_item.
     */
    @Column(name = "batch_item_id")
    private Long batchItemId;

    /**
     * Reference to transaction (withdrawal transaction).
     */
    @Column(name = "transaction_id")
    private Long transactionId;

    // ===== Payment Details =====

    /**
     * Total payment amount.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (default: VND).
     */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Transfer status: SUCCESS, FAILED, PARTIAL.
     */
    @Column(name = "transfer_status", nullable = false, length = 30)
    private String transferStatus;

    /**
     * Time when transfer was completed.
     */
    @Column(name = "transfer_time")
    private LocalDateTime transferTime;

    // ===== User Info Snapshot =====

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_phone", length = 20)
    private String userPhone;

    // ===== Bank Info Snapshot =====

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_full_name", length = 255)
    private String bankFullName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 255)
    private String accountName;

    // ===== Cashback Summary by Platform =====

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_cashback_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalCashbackAmount = BigDecimal.ZERO;

    // Shopee
    @Column(name = "shopee_orders", nullable = false)
    @Builder.Default
    private Integer shopeeOrders = 0;

    @Column(name = "shopee_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal shopeeAmount = BigDecimal.ZERO;

    // Lazada
    @Column(name = "lazada_orders", nullable = false)
    @Builder.Default
    private Integer lazadaOrders = 0;

    @Column(name = "lazada_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal lazadaAmount = BigDecimal.ZERO;

    // Tiki
    @Column(name = "tiki_orders", nullable = false)
    @Builder.Default
    private Integer tikiOrders = 0;

    @Column(name = "tiki_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tikiAmount = BigDecimal.ZERO;

    // TikTok Shop
    @Column(name = "tiktok_orders", nullable = false)
    @Builder.Default
    private Integer tiktokOrders = 0;

    @Column(name = "tiktok_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tiktokAmount = BigDecimal.ZERO;

    // Other platforms
    @Column(name = "other_orders", nullable = false)
    @Builder.Default
    private Integer otherOrders = 0;

    @Column(name = "other_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal otherAmount = BigDecimal.ZERO;

    // ===== Bonus Summary (Milestone & Referrer) =====

    /**
     * Number of milestone bonus rewards paid.
     */
    @Column(name = "bonus_orders", nullable = false)
    @Builder.Default
    private Integer bonusOrders = 0;

    /**
     * Total milestone bonus amount (MILESTONE_BONUS type rewards).
     */
    @Column(name = "bonus_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    /**
     * Number of referrer rewards paid (REFERRER_BONUS, REFERRER_COMMISSION).
     */
    @Column(name = "referrer_commission_orders", nullable = false)
    @Builder.Default
    private Integer referrerCommissionOrders = 0;

    /**
     * Total referrer commission amount.
     */
    @Column(name = "referrer_commission_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal referrerCommissionAmount = BigDecimal.ZERO;

    // ===== Additional Info =====

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "remark", length = 255)
    private String remark;

    // ===== Email Tracking =====

    @Column(name = "email_sent", nullable = false, columnDefinition = "TINYINT(1)")
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "email_error", length = 500)
    private String emailError;

    // ===== Timestamps =====

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
