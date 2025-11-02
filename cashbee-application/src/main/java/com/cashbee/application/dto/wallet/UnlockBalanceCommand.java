package com.cashbee.application.dto.wallet;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Command DTO for unlocking wallet balance.
 *
 * Used when payout request is rejected or cancelled - unlocks the
 * locked balance back to available balance.
 *
 * Business Flow:
 * 1. User requests payout → balance locked
 * 2. Admin rejects payout
 * 3. System unlocks balance (lockedBalance → balance)
 * 4. User can use the money again
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class UnlockBalanceCommand {

    /**
     * User ID whose wallet to unlock.
     */
    private final Long userId;

    /**
     * Amount to unlock (must be positive and <= locked balance).
     */
    private final BigDecimal amount;

    /**
     * Description/reason for unlocking (e.g., "Payout request #123 cancelled").
     */
    private final String description;
}
