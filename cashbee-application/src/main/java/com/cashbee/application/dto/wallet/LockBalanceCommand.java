package com.cashbee.application.dto.wallet;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Command DTO for locking wallet balance.
 *
 * Used when user requests payout - locks the requested amount
 * so it cannot be spent while payout is being processed.
 *
 * Business Flow:
 * 1. User requests payout
 * 2. System locks the requested amount (balance → lockedBalance)
 * 3. Admin processes payout
 * 4. System either:
 *    - Deducts locked balance (if approved)
 *    - Unlocks balance (if rejected)
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class LockBalanceCommand {

    /**
     * User ID whose wallet to lock.
     */
    private final Long userId;

    /**
     * Amount to lock (must be positive and <= available balance).
     */
    private final BigDecimal amount;

    /**
     * Description/reason for locking (e.g., "Payout request #123").
     */
    private final String description;
}
