package com.cashbee.application.dto.cashback;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for cashback policy data.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackPolicyResponse {

    private Long id;
    private String policyName;
    private String policyCode;
    private Long platformId;
    private String platformName; // Will be set by use case if needed
    private String userLevel;
    private BigDecimal cashbackRate;
    private BigDecimal minOrderValue;
    private BigDecimal maxCashbackPerOrder;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
