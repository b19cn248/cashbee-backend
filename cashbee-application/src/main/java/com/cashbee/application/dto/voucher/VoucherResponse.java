package com.cashbee.application.dto.voucher;

import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for voucher data.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private Long id;

    private String code;

    private String title;

    private String description;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    private BigDecimal minOrderValue;

    private String originalLink;

    private String affiliateLink;

    private String voucherSaveLink;

    private VoucherCategory category;

    private String platform;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private String validTimeSlot;

    private VoucherStatus status;

    private Integer viewCount;

    private Integer clickCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Formatted discount display text.
     * Example: "30.000đ" or "20%"
     */
    private String displayDiscount;
}
