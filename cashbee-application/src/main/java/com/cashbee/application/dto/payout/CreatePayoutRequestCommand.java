package com.cashbee.application.dto.payout;

import com.cashbee.domain.enums.PayoutMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Command DTO for creating payout request.
 *
 * Used when user requests withdrawal from their wallet.
 *
 * Business Flow:
 * 1. User requests payout via API
 * 2. System validates balance
 * 3. System locks balance
 * 4. System creates payout request with status REQUESTED
 * 5. Admin reviews and approves/rejects
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class CreatePayoutRequestCommand {

    /**
     * User ID requesting payout.
     */
    private final Long userId;

    /**
     * Amount to withdraw (must be >= 50,000 VND).
     */
    private final BigDecimal amount;

    /**
     * Payout method (BANK, MOMO, ZALOPAY).
     */
    private final PayoutMethod payoutMethod;

    /**
     * Account number (bank account or phone number).
     */
    private final String accountNumber;

    /**
     * Account holder name.
     */
    private final String accountName;

    /**
     * Bank name (required for BANK method).
     */
    private final String bankName;
}
