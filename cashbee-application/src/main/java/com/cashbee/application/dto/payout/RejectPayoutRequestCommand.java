package com.cashbee.application.dto.payout;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Command DTO for rejecting payout request.
 *
 * Used when admin rejects a payout request.
 *
 * Business Flow:
 * 1. Admin reviews payout request
 * 2. Admin rejects it with reason
 * 3. System changes status from REQUESTED → REJECTED
 * 4. System unlocks balance (returns to available)
 * 5. System records who rejected, when, and why
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class RejectPayoutRequestCommand {

    /**
     * Payout request ID to reject.
     */
    private final Long payoutRequestId;

    /**
     * Admin ID who is rejecting.
     */
    private final String adminId;

    /**
     * Reason for rejection (required).
     * Example: "Insufficient documents", "Invalid bank account"
     */
    private final String reason;
}
