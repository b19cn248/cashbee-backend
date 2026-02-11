package com.cashbee.infrastructure.entity;

import com.cashbee.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for affiliate_order table.
 * Infrastructure layer entity with JPA annotations.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "affiliate_order", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id", unique = true),
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_platform", columnList = "platform_id"),
    @Index(name = "idx_order_status", columnList = "order_status"),
    @Index(name = "idx_order_import_batch", columnList = "import_batch_id"),
    @Index(name = "idx_order_time", columnList = "order_time"),
    @Index(name = "idx_order_fallback_match", columnList = "is_fallback_match")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateOrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "click_id", length = 100)
    private String clickId;

    @Column(name = "order_id", length = 100, nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20, nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "product_price", precision = 12, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "commission_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "paid_time")
    private LocalDateTime paidTime;

    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "IMPORT";

    @Column(name = "import_batch_id")
    private Long importBatchId;

    /**
     * Whether this order was matched using fallback (context-based) matching.
     * TRUE = Matched by context (itemId + shopId + time window) because Sub_id1 was missing
     * FALSE = Matched by tracking code (Sub_id1) - normal flow
     */
    @Column(name = "is_fallback_match")
    @Builder.Default
    private Boolean fallbackMatch = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
