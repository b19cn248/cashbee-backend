package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating platform affiliate configuration.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlatformConfigRequest {

    /**
     * Affiliate ID from platform.
     * Example: "cashbee_vn_123456"
     */
    @NotBlank(message = "Affiliate ID is required")
    private String affiliateId;

    /**
     * Link template with placeholders.
     * Example: "https://shopee.vn/universal-link/{product_id}?pid={affiliate_id}&af_sub1={tracking_code}"
     */
    @NotBlank(message = "Link template is required")
    private String linkTemplate;

    /**
     * Enable/disable tracking.
     */
    private Boolean trackingEnabled;
}
