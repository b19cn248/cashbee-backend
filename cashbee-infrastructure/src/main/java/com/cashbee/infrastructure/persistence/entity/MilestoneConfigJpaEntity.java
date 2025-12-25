package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MilestoneConfig JPA Entity for database persistence.
 *
 * <p>Maps to 'milestone_config' table in database.
 * Configures milestone rewards based on completed orders.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "milestone_config", indexes = {
        @Index(name = "idx_milestone_config_type", columnList = "milestone_type"),
        @Index(name = "idx_milestone_config_type_orders", columnList = "milestone_type, orders_required")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Type of milestone: WITH_REFERRER or WITHOUT_REFERRER.
     */
    @Column(name = "milestone_type", nullable = false, length = 30)
    private String milestoneType;

    /**
     * Number of completed orders required to reach this milestone.
     */
    @Column(name = "orders_required", nullable = false)
    private Integer ordersRequired;

    /**
     * Bonus amount for referee (user who completes orders).
     */
    @Column(name = "referee_bonus", nullable = false, precision = 19, scale = 2)
    private BigDecimal refereeBonus;

    /**
     * Bonus amount for referrer (user who shared the code).
     */
    @Column(name = "referrer_bonus", nullable = false, precision = 19, scale = 2)
    private BigDecimal referrerBonus;

    /**
     * New tier to upgrade to (VIP, SUPER) or null if no tier change.
     */
    @Column(name = "new_tier", length = 20)
    private String newTier;

    /**
     * Duration in months for referrer to receive commission.
     */
    @Column(name = "commission_months", nullable = false)
    private Integer commissionMonths;

    /**
     * Whether this milestone activates referral commission.
     */
    @Column(name = "activates_referral", nullable = false)
    private Boolean activatesReferral;

    /**
     * Description for display/logging purposes.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Whether this configuration is active.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Timestamp when record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.refereeBonus == null) {
            this.refereeBonus = BigDecimal.ZERO;
        }
        if (this.referrerBonus == null) {
            this.referrerBonus = BigDecimal.ZERO;
        }
        if (this.commissionMonths == null) {
            this.commissionMonths = 0;
        }
        if (this.activatesReferral == null) {
            this.activatesReferral = false;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
