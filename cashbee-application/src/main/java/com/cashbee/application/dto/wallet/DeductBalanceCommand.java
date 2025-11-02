package com.cashbee.application.dto.wallet;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Command DTO for deducting locked balance.
 *
 * Used when payout is successfully paid - deducts the locked balance
 * permanently and updates totalWithdrawn statistics.
 *
 * Business Flow:
 * 1. User requests payout → balance locked
 * 2. Admin approves payout
 * 3. Payout is successfully paid to user's account
 * 4. System deducts locked balance (lockedBalance → removed)
 * 5. System updates totalWithdrawn statistic
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class DeductBalanceCommand {

    /**
     * User ID whose wallet to deduct from.
     */
    private final Long userId;

    /**
     * Amount to deduct (must be positive and <= locked balance).
     */
    private final BigDecimal amount;

    /**
     * Description/reason for deduction (e.g., "Payout #123 completed").
     */
    private final String description;
}
