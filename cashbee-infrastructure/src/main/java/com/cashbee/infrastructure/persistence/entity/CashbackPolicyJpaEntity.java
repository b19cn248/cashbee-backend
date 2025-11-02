package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.UserLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for cashback_policy table.
 * This is the INFRASTRUCTURE representation with JPA annotations.
 *
 * Separated from domain model to keep domain pure.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "cashback_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackPolicyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "policy_code", nullable = false, unique = true, length = 50)
    private String policyCode;

    @Column(name = "platform_id")
    private Long platformId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_level", nullable = false, length = 20)
    private UserLevel userLevel;

    @Column(name = "cashback_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cashbackRate;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "max_cashback_per_order", precision = 12, scale = 2)
    private BigDecimal maxCashbackPerOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.priority == null) {
            this.priority = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
