package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.VoucherResponse;
import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Use case for retrieving vouchers with filtering and pagination.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetVouchersUseCase {

    private final PromotionVoucherRepository voucherRepository;

    /**
     * Execute use case to get vouchers with filtering.
     *
     * @param status Filter by status (optional, defaults to ACTIVE)
     * @param category Filter by category (optional)
     * @param platform Filter by platform (optional)
     * @param pageable Pagination information
     * @return Page of VoucherResponse
     */
    public Page<VoucherResponse> execute(
        VoucherStatus status,
        VoucherCategory category,
        String platform,
        Pageable pageable
    ) {
        // Default to ACTIVE if no status provided
        VoucherStatus effectiveStatus = status != null ? status : VoucherStatus.ACTIVE;

        log.info("Getting vouchers - status: {}, category: {}, platform: {}, page: {}, size: {}",
            effectiveStatus, category, platform, pageable.getPageNumber(), pageable.getPageSize());

        Page<PromotionVoucher> voucherPage;

        // Apply filters based on provided parameters
        if (category != null && platform != null) {
            voucherPage = voucherRepository.findByStatusAndCategoryAndPlatform(
                effectiveStatus, category, platform, pageable);
        } else if (category != null) {
            voucherPage = voucherRepository.findByStatusAndCategory(
                effectiveStatus, category, pageable);
        } else if (platform != null) {
            voucherPage = voucherRepository.findByStatusAndPlatform(
                effectiveStatus, platform, pageable);
        } else {
            voucherPage = voucherRepository.findByStatus(effectiveStatus, pageable);
        }

        log.info("Found {} vouchers (total: {})", voucherPage.getNumberOfElements(), voucherPage.getTotalElements());

        return voucherPage.map(this::mapToResponse);
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
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(discountValue.longValue()) + "đ";
        }
    }
}
