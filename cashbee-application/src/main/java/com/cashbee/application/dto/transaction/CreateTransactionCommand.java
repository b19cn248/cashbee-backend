package com.cashbee.application.dto.transaction;

import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Command DTO for creating a transaction record.
 *
 * Used internally by wallet use cases to create transaction history.
 * Every wallet operation should create a transaction for audit trail.
 *
 * Business Flow:
 * 1. User performs wallet operation (add pending, confirm, lock, etc.)
 * 2. Wallet use case executes business logic
 * 3. Use case creates transaction record using this command
 * 4. Transaction is saved to database
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class CreateTransactionCommand {

    /**
     * User ID who owns this transaction.
     */
    private final Long userId;

    /**
     * Wallet ID affected by this transaction.
     */
    private final Long walletId;

    /**
     * Type of transaction (CASHBACK, WITHDRAW, BONUS, etc.).
     */
    private final TransactionType type;

    /**
     * Transaction amount (always positive).
     */
    private final BigDecimal amount;

    /**
     * Human-readable description.
     * Example: "Cashback from Shopee order #12345"
     */
    private final String description;

    /**
     * Balance before transaction was applied.
     */
    private final BigDecimal balanceBefore;

    /**
     * Balance after transaction was applied.
     */
    private final BigDecimal balanceAfter;

    /**
     * Transaction status (SUCCESS, PENDING, FAILED, CANCELLED).
     */
    private final TransactionStatus status;
}
