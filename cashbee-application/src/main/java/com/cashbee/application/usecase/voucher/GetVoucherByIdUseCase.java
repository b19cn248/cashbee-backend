package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.VoucherResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Use case for retrieving a single voucher by ID.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetVoucherByIdUseCase {

    private final PromotionVoucherRepository voucherRepository;

    /**
     * Execute use case to get a voucher by ID.
     *
     * @param voucherId Voucher ID
     * @return VoucherResponse
     * @throws NotFoundException if voucher not found
     */
    public VoucherResponse execute(Long voucherId) {
        log.info("Getting voucher by ID: {}", voucherId);

        PromotionVoucher voucher = voucherRepository.findById(voucherId)
            .orElseThrow(() -> {
                log.warn("Voucher not found with ID: {}", voucherId);
                return new NotFoundException("Voucher not found with id: " + voucherId);
            });

        log.info("Found voucher: {} - {}", voucher.getId(), voucher.getTitle());

        return mapToResponse(voucher);
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
