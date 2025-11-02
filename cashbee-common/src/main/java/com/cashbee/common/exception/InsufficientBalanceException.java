package com.cashbee.common.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when user doesn't have sufficient balance for an operation.
 * Specific to wallet/payout operations.
 *
 * @author CashBee Team
 */
public class InsufficientBalanceException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "INSUFFICIENT_BALANCE";

    public InsufficientBalanceException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    /**
     * Factory method with balance details.
     *
     * @param currentBalance User's current balance
     * @param requiredAmount Amount required
     * @return InsufficientBalanceException instance
     */
    public static InsufficientBalanceException of(BigDecimal currentBalance, BigDecimal requiredAmount) {
        return new InsufficientBalanceException(
            String.format("Insufficient balance. Current: %s, Required: %s",
                currentBalance, requiredAmount)
        );
    }

    /**
     * Factory method for minimum payout amount not met.
     *
     * @param amount Requested amount
     * @param minAmount Minimum amount required
     * @return InsufficientBalanceException instance
     */
    public static InsufficientBalanceException belowMinimum(BigDecimal amount, BigDecimal minAmount) {
        return new InsufficientBalanceException(
            String.format("Payout amount %s is below minimum required amount %s",
                amount, minAmount)
        );
    }
}
