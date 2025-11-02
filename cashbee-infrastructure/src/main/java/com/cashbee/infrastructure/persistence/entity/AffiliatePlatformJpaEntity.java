package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.PlatformStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for affiliate_platform table.
 * This is the INFRASTRUCTURE representation with JPA annotations.
 *
 * Separated from domain model to keep domain pure.
 * Mapped to domain model using MapStruct mapper.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "affiliate_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePlatformJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "api_secret", length = 255)
    private String apiSecret;

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Column(name = "affiliate_id", length = 100)
    private String affiliateId;

    @Column(name = "link_template", columnDefinition = "TEXT")
    private String linkTemplate;

    @Column(name = "tracking_enabled", nullable = false)
    @Builder.Default
    private Boolean trackingEnabled = true;

    @Column(name = "default_commission_rate", precision = 5, scale = 2)
    private BigDecimal defaultCommissionRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlatformStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set timestamps before persisting entity.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PlatformStatus.ACTIVE;
        }
    }

    /**
     * Update timestamp before updating entity.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
