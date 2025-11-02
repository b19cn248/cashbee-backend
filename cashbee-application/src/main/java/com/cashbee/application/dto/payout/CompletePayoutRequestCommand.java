package com.cashbee.application.dto.payout;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Command DTO for completing payout request.
 *
 * Used when admin marks payout as completed after transferring money.
 *
 * Business Flow:
 * 1. Admin transfers money to user's bank account
 * 2. Admin marks payout as completed with transaction reference
 * 3. System changes status from PROCESSING → PAID
 * 4. System deducts locked balance (moves to totalWithdrawn)
 * 5. System creates transaction record for audit trail
 * 6. System records completion time
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class CompletePayoutRequestCommand {

    /**
     * Payout request ID to complete.
     */
    private final Long payoutRequestId;

    /**
     * Admin ID who is completing the payout.
     */
    private final String adminId;

    /**
     * External transaction reference (optional).
     * Example: Bank transaction ID, payment gateway reference
     */
    private final String transactionReference;
}
