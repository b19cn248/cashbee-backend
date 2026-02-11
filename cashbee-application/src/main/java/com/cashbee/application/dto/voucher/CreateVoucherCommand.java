package com.cashbee.application.dto.voucher;

import com.cashbee.domain.enums.DiscountType;
import com.cashbee.domain.enums.VoucherCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Command DTO for creating a new voucher.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherCommand {

    /**
     * Voucher code (e.g., AFFZOSI0).
     * Can be null if voucher only has a save link.
     */
    private String code;

    /**
     * Title/summary of the voucher.
     * Example: "Giảm 30k từ 99k"
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * Detailed description of the voucher.
     */
    private String description;

    /**
     * Type of discount: FIXED_AMOUNT or PERCENTAGE.
     */
    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    /**
     * Discount value.
     * For FIXED_AMOUNT: actual amount in VND (e.g., 30000)
     * For PERCENTAGE: percentage value (e.g., 20 for 20%)
     */
    private BigDecimal discountValue;

    /**
     * Maximum discount amount for percentage vouchers.
     */
    private BigDecimal maxDiscount;

    /**
     * Minimum order value required to use this voucher.
     */
    private BigDecimal minOrderValue;

    /**
     * Original Shopee link from Telegram message.
     * Will be automatically converted to affiliate link.
     */
    private String originalLink;

    /**
     * Link to save/claim the voucher.
     */
    private String voucherSaveLink;

    /**
     * Category of the voucher.
     */
    private VoucherCategory category;

    /**
     * Platform (SHOPEE, LAZADA, TIKTOK).
     * Defaults to SHOPEE if not provided.
     */
    @Builder.Default
    private String platform = "SHOPEE";

    /**
     * Time slot when voucher is valid (e.g., "9H-11H").
     */
    private String validTimeSlot;

    /**
     * Start time when voucher becomes valid.
     */
    private LocalDateTime validFrom;

    /**
     * End time when voucher expires.
     */
    private LocalDateTime validUntil;
}
