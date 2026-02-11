package com.cashbee.application.dto.referraladmin;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for milestone configuration data.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneConfigResponse {

    private Long id;
    private String milestoneType;
    private Integer ordersRequired;
    private BigDecimal refereeBonus;
    private BigDecimal referrerBonus;
    private String newTier;
    private Integer commissionMonths;
    private Boolean activatesReferral;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
