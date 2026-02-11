package com.cashbee.application.dto.voucher;

import lombok.*;

/**
 * Response DTO for voucher tracking (view/click).
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTrackingResponse {

    /**
     * Voucher ID.
     */
    private Long voucherId;

    /**
     * Current view count (for view tracking).
     */
    private Integer viewCount;

    /**
     * Current click count (for click tracking).
     */
    private Integer clickCount;

    /**
     * Affiliate link to redirect user (for click tracking).
     */
    private String affiliateLink;
}
