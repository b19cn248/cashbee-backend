package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for PayoutRequest table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the PayoutRequest domain model.
 *
 * Table: payout_requests
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "payout_requests", indexes = {
        @Index(name = "idx_payout_user_id", columnList = "user_id"),
        @Index(name = "idx_payout_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_payout_status", columnList = "status"),
        @Index(name = "idx_payout_requested_at", columnList = "requested_at"),
        @Index(name = "idx_payout_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PayoutRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who requested payout (FK to users table).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Wallet to withdraw from (FK to user_wallets table).
     */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /**
     * Amount to withdraw.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Payout method (BANK, MOMO, ZALOPAY).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false, length = 20)
    private PayoutMethod payoutMethod;

    /**
     * Account number (bank account or phone number).
     */
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Account holder name.
     */
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    /**
     * Bank name (for BANK method).
     */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /**
     * Current status of payout request.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status;

    /**
     * When payout was requested (auto-populated).
     */
    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /**
     * When payout was processed (approved/rejected).
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Who processed the payout (admin ID or SYSTEM).
     */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    /**
     * When payout was completed (money transferred).
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Reason for rejection/cancellation.
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
