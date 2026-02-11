package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.CreateVoucherCommand;
import com.cashbee.application.dto.voucher.VoucherResponse;
import com.cashbee.application.util.affiliate.ShopeeAffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeUrlParser;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Use case for creating a new promotion voucher.
 *
 * Features:
 * - Auto-generate affiliate link from original Shopee link
 * - Validate and parse discount information
 * - Generate display discount text (e.g., "30.000đ" or "20%")
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateVoucherUseCase {

    private final PromotionVoucherRepository voucherRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final ShopeeUrlParser shopeeUrlParser;
    private final ShopeeAffiliateLinkBuilder shopeeAffiliateLinkBuilder;

    /**
     * Execute use case to create a new voucher.
     *
     * @param command Command containing voucher data
     * @return VoucherResponse with created voucher data
     */
    @Transactional
    public VoucherResponse execute(CreateVoucherCommand command) {
        log.info("Creating new voucher with title: {}", command.getTitle());

        // Step 1: Build affiliate link if original link is provided
        String affiliateLink = null;
        if (command.getOriginalLink() != null && !command.getOriginalLink().isBlank()) {
            affiliateLink = buildAffiliateLink(command.getOriginalLink());
        }

        // Step 2: Create domain model
        LocalDateTime now = LocalDateTime.now();
        PromotionVoucher voucher = PromotionVoucher.builder()
            .code(command.getCode())
            .title(command.getTitle())
            .description(command.getDescription())
            .discountType(command.getDiscountType())
            .discountValue(command.getDiscountValue())
            .maxDiscount(command.getMaxDiscount())
            .minOrderValue(command.getMinOrderValue())
            .originalLink(command.getOriginalLink())
            .affiliateLink(affiliateLink)
            .voucherSaveLink(command.getVoucherSaveLink())
            .category(command.getCategory() != null ? command.getCategory() : VoucherCategory.OTHER)
            .platform(command.getPlatform() != null ? command.getPlatform() : "SHOPEE")
            .validFrom(command.getValidFrom())
            .validUntil(command.getValidUntil())
            .validTimeSlot(command.getValidTimeSlot())
            .status(VoucherStatus.ACTIVE)
            .viewCount(0)
            .clickCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Step 3: Save voucher
        PromotionVoucher savedVoucher = voucherRepository.save(voucher);
        log.info("Voucher created successfully with ID: {}", savedVoucher.getId());

        // Step 4: Map to response
        return mapToResponse(savedVoucher);
    }

    /**
     * Build affiliate link from original Shopee URL.
     *
     * @param originalLink Original Shopee link (can be shortened)
     * @return Affiliate link or null if building fails
     */
    private String buildAffiliateLink(String originalLink) {
        try {
            // Parse URL (handles shortened links automatically)
            ShopeeUrlParser.ParsedShopeeUrl parsedUrl = shopeeUrlParser.parse(originalLink);
            String urlForAffiliate = parsedUrl.getUrlForAffiliateLink();

            log.debug("Parsed URL for affiliate link building: {}", urlForAffiliate);

            // Get Shopee platform configuration
            AffiliatePlatform platform = platformRepository.findByCode("shopee")
                .orElse(null);

            if (platform == null || platform.getAffiliateId() == null) {
                log.warn("Shopee affiliate platform not configured, skipping affiliate link generation");
                return null;
            }

            // Build affiliate link with voucher tracking code
            String trackingCode = "voucher_" + System.currentTimeMillis();
            String affiliateLink = shopeeAffiliateLinkBuilder.build(
                urlForAffiliate,
                platform.getAffiliateId(),
                trackingCode
            );

            log.info("Generated affiliate link: {}", affiliateLink);
            return affiliateLink;

        } catch (IllegalArgumentException e) {
            log.warn("Failed to build affiliate link from URL: {}. Error: {}", originalLink, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error building affiliate link from URL: {}", originalLink, e);
            return null;
        }
    }

    /**
     * Map domain model to response DTO.
     *
     * @param voucher Domain voucher
     * @return Response DTO
     */
    private VoucherResponse mapToResponse(PromotionVoucher voucher) {
        return VoucherResponse.builder()
            .id(voucher.getId())
            .code(voucher.getCode())
            .title(voucher.getTitle())
            .description(voucher.getDescription())
            .discountType(voucher.getDiscountType())
            .discountValue(voucher.getDiscountValue())
            .maxDiscount(voucher.getMaxDiscount())
            .minOrderValue(voucher.getMinOrderValue())
            .originalLink(voucher.getOriginalLink())
            .affiliateLink(voucher.getAffiliateLink())
            .voucherSaveLink(voucher.getVoucherSaveLink())
            .category(voucher.getCategory())
            .platform(voucher.getPlatform())
            .validFrom(voucher.getValidFrom())
            .validUntil(voucher.getValidUntil())
            .validTimeSlot(voucher.getValidTimeSlot())
            .status(voucher.getStatus())
            .viewCount(voucher.getViewCount())
            .clickCount(voucher.getClickCount())
            .createdAt(voucher.getCreatedAt())
            .updatedAt(voucher.getUpdatedAt())
            .displayDiscount(formatDisplayDiscount(voucher.getDiscountType(), voucher.getDiscountValue()))
            .build();
    }

    /**
     * Format discount value for display.
     *
     * @param discountType Type of discount
     * @param discountValue Discount value
     * @return Formatted display text (e.g., "30.000đ" or "20%")
     */
    private String formatDisplayDiscount(DiscountType discountType, BigDecimal discountValue) {
        if (discountValue == null) {
            return null;
        }

        if (discountType == DiscountType.PERCENTAGE) {
            return discountValue.stripTrailingZeros().toPlainString() + "%";
        } else {
            // Format as Vietnamese currency (30000 -> "30.000đ")
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(discountValue.longValue()) + "đ";
        }
    }
}
