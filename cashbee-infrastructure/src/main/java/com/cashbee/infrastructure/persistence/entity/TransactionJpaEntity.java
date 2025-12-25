package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.TransactionSourceType;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for Transaction table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the Transaction domain model.
 *
 * Table: transactions
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_user_id", columnList = "user_id"),
        @Index(name = "idx_transaction_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at"),
        @Index(name = "idx_transaction_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who owns this transaction (FK to users table).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Wallet affected by this transaction (FK to user_wallets table).
     */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /**
     * Type of transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    /**
     * Transaction amount (always positive).
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Human-readable description.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Balance before transaction.
     */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    /**
     * Balance after transaction.
     */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Transaction status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * Source type indicating where this transaction originated from.
     * Examples: ORDER (cashback), MILESTONE_BONUS, REFERRER_BONUS, PAYOUT, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private TransactionSourceType sourceType;

    /**
     * ID of the source record (FK to related table based on sourceType).
     */
    @Column(name = "source_id")
    private Long sourceId;

    /**
     * When transaction was created (auto-populated).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
