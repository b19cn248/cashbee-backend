package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ReferrerCommission JPA Entity for database persistence.
 *
 * <p>Maps to 'referrer_commission' table in database.
 * Tracks 5% commission from referee's orders paid to referrer.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "referrer_commission", indexes = {
        @Index(name = "idx_referrer_comm_referrer_id", columnList = "referrer_id"),
        @Index(name = "idx_referrer_comm_referee_id", columnList = "referee_id"),
        @Index(name = "idx_referrer_comm_source_order", columnList = "source_order_id"),
        @Index(name = "idx_referrer_comm_status", columnList = "status"),
        @Index(name = "idx_referrer_comm_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferrerCommissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Referrer who receives the commission.
     */
    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    /**
     * Referee whose order generated this commission.
     */
    @Column(name = "referee_id", nullable = false)
    private Long refereeId;

    /**
     * Source order that generated this commission.
     */
    @Column(name = "source_order_id", nullable = false)
    private Long sourceOrderId;

    /**
     * Original commission from platform (e.g., Shopee).
     */
    @Column(name = "original_commission", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalCommission;

    /**
     * Commission rate: 5.00 (5%).
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    /**
     * Commission amount for referrer (5% x original_commission).
     */
    @Column(name = "commission_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;

    /**
     * Commission status: PENDING, CONFIRMED, PAID.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Timestamp when commission was confirmed (order PAID).
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Timestamp when commission was paid to referrer wallet.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Timestamp when record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Commission expires 3 months after referee activation.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Batch ID that paid this commission.
     */
    @Column(name = "paid_batch_id")
    private Long paidBatchId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.commissionRate == null) {
            this.commissionRate = new BigDecimal("5.00");
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
