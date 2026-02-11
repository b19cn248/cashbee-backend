package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Promotion Voucher JPA Entity for database persistence.
 * This is the infrastructure model with JPA annotations.
 *
 * Maps to 'promotion_voucher' table in database.
 * Separate from domain model (PromotionVoucher) to follow Hexagonal Architecture.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "promotion_voucher", indexes = {
    @Index(name = "idx_voucher_code", columnList = "code"),
    @Index(name = "idx_voucher_status", columnList = "status"),
    @Index(name = "idx_voucher_category", columnList = "category"),
    @Index(name = "idx_voucher_platform", columnList = "platform"),
    @Index(name = "idx_voucher_valid_until", columnList = "valid_until"),
    @Index(name = "idx_voucher_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionVoucherJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "original_link", length = 500)
    private String originalLink;

    @Column(name = "affiliate_link", length = 500)
    private String affiliateLink;

    @Column(name = "voucher_save_link", length = 500)
    private String voucherSaveLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private VoucherCategory category;

    @Column(name = "platform", length = 30)
    private String platform;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "valid_time_slot", length = 50)
    private String validTimeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoucherStatus status;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "click_count", nullable = false)
    private Integer clickCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VoucherStatus.ACTIVE;
        }
        if (this.platform == null) {
            this.platform = "SHOPEE";
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.clickCount == null) {
            this.clickCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
