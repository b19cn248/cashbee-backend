package com.cashbee.application.dto.transaction;

import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Transaction.
 *
 * Returned to clients via REST API.
 * Contains all transaction details for display in transaction history.
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class TransactionResponse {

    /**
     * Transaction ID.
     */
    private final Long id;

    /**
     * User ID who owns this transaction.
     */
    private final Long userId;

    /**
     * Wallet ID affected by this transaction.
     */
    private final Long walletId;

    /**
     * Type of transaction.
     */
    private final TransactionType type;

    /**
     * Transaction amount.
     */
    private final BigDecimal amount;

    /**
     * Human-readable description.
     */
    private final String description;

    /**
     * Balance before transaction.
     */
    private final BigDecimal balanceBefore;

    /**
     * Balance after transaction.
     */
    private final BigDecimal balanceAfter;

    /**
     * Transaction status.
     */
    private final TransactionStatus status;

    /**
     * When transaction was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;
}
