package com.cashbee.application.dto.referraladmin;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for referrer tier configuration data.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferrerTierConfigResponse {

    private Long id;
    private String tierName;
    private Integer minReferrals;
    private BigDecimal commissionRate;
    private BigDecimal bonusPerActivation;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
