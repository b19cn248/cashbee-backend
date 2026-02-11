package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for referrer_tier_config table.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "referrer_tier_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferrerTierConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_name", nullable = false, unique = true, length = 30)
    private String tierName;

    @Column(name = "min_referrals", nullable = false)
    private Integer minReferrals;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "bonus_per_activation", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bonusPerActivation = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
