package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.CashbackStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for cashback table.
 * Infrastructure layer entity with JPA annotations.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "cashback", indexes = {
    @Index(name = "idx_cashback_user", columnList = "user_id"),
    @Index(name = "idx_cashback_order", columnList = "order_id"),
    @Index(name = "idx_cashback_status", columnList = "status"),
    @Index(name = "idx_cashback_user_status", columnList = "user_id,status"),
    @Index(name = "idx_cashback_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    @Column(name = "commission_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "cashback_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal cashbackAmount;

    @Column(name = "cashback_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal cashbackRate;

    @Column(name = "policy_id")
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CashbackStatus status = CashbackStatus.PENDING;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
