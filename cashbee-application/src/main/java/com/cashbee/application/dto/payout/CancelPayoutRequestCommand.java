package com.cashbee.application.dto.payout;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Command DTO for cancelling payout request.
 *
 * Used when user or admin cancels a payout request.
 *
 * Business Flow:
 * 1. User decides to cancel payout request (before approval)
 * 2. OR Admin cancels for various reasons
 * 3. System changes status from REQUESTED → CANCELLED
 * 4. System unlocks balance (returns to available)
 * 5. System records who cancelled, when, and why
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class CancelPayoutRequestCommand {

    /**
     * Payout request ID to cancel.
     */
    private final Long payoutRequestId;

    /**
     * User ID who is cancelling (for authorization check).
     */
    private final Long userId;

    /**
     * Reason for cancellation (required).
     * Example: "User changed mind", "Duplicate request", "Admin decision"
     */
    private final String reason;
}
