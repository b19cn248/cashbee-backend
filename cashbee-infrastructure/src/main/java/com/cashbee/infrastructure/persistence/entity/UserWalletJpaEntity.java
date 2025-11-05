package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UserWallet JPA Entity for database persistence.
 * This is the infrastructure model with JPA annotations.
 *
 * Maps to 'user_wallet' table in database.
 * Separate from domain model (UserWallet) to follow Hexagonal Architecture.
 *
 * Note: Uses userId (Long) instead of User object relationship
 * to avoid lazy loading issues and maintain clean separation.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "user_wallet", indexes = {
    @Index(name = "idx_wallet_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Foreign key to user table.
     * Using Long instead of @OneToOne relationship for clean separation.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        ensureBalancesNotNull();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        ensureBalancesNotNull();
    }

    @PostLoad
    protected void onLoad() {
        // Critical: Fix NULL values after loading from database
        // This handles legacy data where database may have NULL values
        ensureBalancesNotNull();
    }

    /**
     * Ensure all balance fields are not null.
     * Database may have NULL values if constraints were added later.
     * This method protects against NullPointerException in calculations.
     */
    private void ensureBalancesNotNull() {
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
        if (this.pendingBalance == null) {
            this.pendingBalance = BigDecimal.ZERO;
        }
        if (this.lockedBalance == null) {
            this.lockedBalance = BigDecimal.ZERO;
        }
        if (this.totalEarned == null) {
            this.totalEarned = BigDecimal.ZERO;
        }
        if (this.totalWithdrawn == null) {
            this.totalWithdrawn = BigDecimal.ZERO;
        }
    }
}
