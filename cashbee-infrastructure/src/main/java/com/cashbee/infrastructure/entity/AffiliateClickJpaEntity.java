package com.cashbee.infrastructure.entity;

import com.cashbee.domain.enums.ClickStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity for affiliate_click table.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "affiliate_click", indexes = {
    @Index(name = "idx_click_tracking_code", columnList = "tracking_code", unique = true),
    @Index(name = "idx_click_user", columnList = "user_id"),
    @Index(name = "idx_click_user_platform", columnList = "user_id,platform_id"),
    @Index(name = "idx_click_status", columnList = "status"),
    @Index(name = "idx_click_created_at", columnList = "created_at"),
    @Index(name = "idx_click_order_matched", columnList = "order_matched")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateClickJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    @Column(name = "shop_id", length = 50)
    private String shopId;

    @Column(name = "item_id", length = 50)
    private String itemId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "tracking_code", length = 100, nullable = false, unique = true)
    private String trackingCode;

    @Column(name = "tracking_url", columnDefinition = "TEXT", nullable = false)
    private String trackingUrl;

    @Column(name = "original_url", columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "order_matched", nullable = false)
    @Builder.Default
    private Boolean orderMatched = false;

    @Column(name = "matched_order_id")
    private Long matchedOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ClickStatus status = ClickStatus.CREATED;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
