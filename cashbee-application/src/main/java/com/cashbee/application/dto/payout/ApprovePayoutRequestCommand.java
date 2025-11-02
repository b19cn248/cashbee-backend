package com.cashbee.application.dto.payout;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Command DTO for approving payout request.
 *
 * Used when admin approves a payout request.
 *
 * Business Flow:
 * 1. Admin reviews payout request
 * 2. Admin approves it
 * 3. System changes status from REQUESTED → PROCESSING
 * 4. System records who approved and when
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class ApprovePayoutRequestCommand {

    /**
     * Payout request ID to approve.
     */
    private final Long payoutRequestId;

    /**
     * Admin ID who is approving.
     */
    private final String adminId;
}
