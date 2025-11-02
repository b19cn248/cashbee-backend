package com.cashbee.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for affiliate_order_item table.
 * Infrastructure layer entity with JPA annotations.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "affiliate_order_item", indexes = {
    @Index(name = "idx_item_order", columnList = "order_id"),
    @Index(name = "idx_item_id", columnList = "item_id"),
    @Index(name = "idx_item_shop", columnList = "shop_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliateOrderItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "item_id", length = 50, nullable = false)
    private String itemId;

    @Column(name = "item_name", length = 255, nullable = false)
    private String itemName;

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "item_commission", precision = 12, scale = 2)
    private BigDecimal itemCommission;

    @Column(name = "shop_id", length = 50)
    private String shopId;

    @Column(name = "shop_name", length = 255)
    private String shopName;

    @Column(name = "category_lv1", length = 100)
    private String categoryLv1;

    @Column(name = "category_lv2", length = 100)
    private String categoryLv2;

    @Column(name = "category_lv3", length = 100)
    private String categoryLv3;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(name = "brand_commission_rate", precision = 8, scale = 4)
    private BigDecimal brandCommissionRate;

    @Column(name = "platform_commission_rate", precision = 8, scale = 4)
    private BigDecimal platformCommissionRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
