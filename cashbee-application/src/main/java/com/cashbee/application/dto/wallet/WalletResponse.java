package com.cashbee.application.dto.wallet;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for wallet data.
 *
 * Contains wallet balance information and statistics.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    /**
     * Wallet database ID.
     */
    private Long id;

    /**
     * User ID who owns this wallet.
     */
    private Long userId;

    /**
     * Available balance (can be withdrawn).
     */
    private BigDecimal balance;

    /**
     * Pending balance (not yet confirmed).
     */
    private BigDecimal pendingBalance;

    /**
     * Locked balance (reserved for pending payouts).
     */
    private BigDecimal lockedBalance;

    /**
     * Total amount earned (lifetime).
     */
    private BigDecimal totalEarned;

    /**
     * Total amount withdrawn (lifetime).
     */
    private BigDecimal totalWithdrawn;

    /**
     * Wallet creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     */
    private LocalDateTime updatedAt;

    /**
     * Calculate total balance across all states.
     */
    public BigDecimal getTotalBalance() {
        return balance.add(pendingBalance).add(lockedBalance);
    }

    /**
     * Check if wallet has sufficient available balance.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
