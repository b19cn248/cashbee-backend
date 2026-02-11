package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.infrastructure.persistence.entity.PromotionVoucherJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between PromotionVoucher domain model and PromotionVoucherJpaEntity.
 *
 * @author CashBee Team
 */
@Component
public class PromotionVoucherMapper {

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain PromotionVoucher domain model
     * @return PromotionVoucherJpaEntity
     */
    public PromotionVoucherJpaEntity toEntity(PromotionVoucher domain) {
        if (domain == null) {
            return null;
        }

        return PromotionVoucherJpaEntity.builder()
            .id(domain.getId())
            .code(domain.getCode())
            .title(domain.getTitle())
            .description(domain.getDescription())
            .discountType(domain.getDiscountType())
            .discountValue(domain.getDiscountValue())
            .maxDiscount(domain.getMaxDiscount())
            .minOrderValue(domain.getMinOrderValue())
            .originalLink(domain.getOriginalLink())
            .affiliateLink(domain.getAffiliateLink())
            .voucherSaveLink(domain.getVoucherSaveLink())
            .category(domain.getCategory())
            .platform(domain.getPlatform())
            .validFrom(domain.getValidFrom())
            .validUntil(domain.getValidUntil())
            .validTimeSlot(domain.getValidTimeSlot())
            .status(domain.getStatus())
            .viewCount(domain.getViewCount())
            .clickCount(domain.getClickCount())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity PromotionVoucherJpaEntity
     * @return PromotionVoucher domain model
     */
    public PromotionVoucher toDomain(PromotionVoucherJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return PromotionVoucher.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .discountType(entity.getDiscountType())
            .discountValue(entity.getDiscountValue())
            .maxDiscount(entity.getMaxDiscount())
            .minOrderValue(entity.getMinOrderValue())
            .originalLink(entity.getOriginalLink())
            .affiliateLink(entity.getAffiliateLink())
            .voucherSaveLink(entity.getVoucherSaveLink())
            .category(entity.getCategory())
            .platform(entity.getPlatform())
            .validFrom(entity.getValidFrom())
            .validUntil(entity.getValidUntil())
            .validTimeSlot(entity.getValidTimeSlot())
            .status(entity.getStatus())
            .viewCount(entity.getViewCount())
            .clickCount(entity.getClickCount())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
