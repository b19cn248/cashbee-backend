package com.cashbee.application.dto.payout;

import com.cashbee.domain.enums.PayoutMethod;
import com.cashbee.domain.enums.PayoutStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for PayoutRequest.
 *
 * Returned to clients via REST API.
 * Contains all payout request details.
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class PayoutRequestResponse {

    /**
     * Payout request ID.
     */
    private final Long id;

    /**
     * User ID who requested payout.
     */
    private final Long userId;

    /**
     * Wallet ID.
     */
    private final Long walletId;

    /**
     * Amount to withdraw.
     */
    private final BigDecimal amount;

    /**
     * Payout method.
     */
    private final PayoutMethod payoutMethod;

    /**
     * Account number.
     */
    private final String accountNumber;

    /**
     * Account holder name.
     */
    private final String accountName;

    /**
     * Bank name (for BANK method).
     */
    private final String bankName;

    /**
     * Current status.
     */
    private final PayoutStatus status;

    /**
     * When payout was requested.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime requestedAt;

    /**
     * When payout was processed (approved/rejected).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime processedAt;

    /**
     * Who processed the payout.
     */
    private final String processedBy;

    /**
     * When payout was completed.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime completedAt;

    /**
     * Reason for rejection/cancellation.
     */
    private final String rejectionReason;
}
