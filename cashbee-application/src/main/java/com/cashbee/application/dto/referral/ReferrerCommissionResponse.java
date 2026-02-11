package com.cashbee.application.dto.referral;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for referrer commission details.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferrerCommissionResponse {

    /**
     * Commission ID.
     */
    private Long id;

    /**
     * ID of the referrer who receives commission.
     */
    private Long referrerId;

    /**
     * ID of the referee whose order generated commission.
     */
    private Long refereeId;

    /**
     * Name of referee (masked for privacy).
     */
    private String refereeName;

    /**
     * Source order ID that generated this commission.
     */
    private Long sourceOrderId;

    /**
     * Original commission amount from platform (Shopee).
     */
    private BigDecimal originalCommission;

    /**
     * Commission rate applied (default 5%).
     */
    private BigDecimal commissionRate;

    /**
     * Commission amount (5% of original commission).
     */
    private BigDecimal commissionAmount;

    /**
     * Commission status (PENDING, CONFIRMED, PAID).
     */
    private String status;

    /**
     * When commission expires (3 months from referee's first 3 orders).
     */
    private LocalDateTime expiresAt;

    /**
     * When commission was confirmed.
     */
    private LocalDateTime confirmedAt;

    /**
     * When commission was paid.
     */
    private LocalDateTime paidAt;

    /**
     * When commission was created.
     */
    private LocalDateTime createdAt;

    /**
     * Whether this commission is still within the active period.
     */
    private boolean isActive;
}
