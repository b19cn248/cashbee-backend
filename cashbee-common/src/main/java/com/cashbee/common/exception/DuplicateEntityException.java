package com.cashbee.common.exception;

/**
 * Exception thrown when attempting to create an entity that already exists.
 * Typically maps to HTTP 409 Conflict.
 *
 * @author CashBee Team
 */
public class DuplicateEntityException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "DUPLICATE_ENTITY";

    public DuplicateEntityException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public DuplicateEntityException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for duplicate entity by field.
     *
     * @param entityType Type of entity
     * @param fieldName Field name
     * @param fieldValue Field value
     * @return DuplicateEntityException instance
     */
    public static DuplicateEntityException of(String entityType, String fieldName, Object fieldValue) {
        return new DuplicateEntityException(
            String.format("%s already exists with %s: %s", entityType, fieldName, fieldValue)
        );
    }

    /**
     * Factory method for duplicate email.
     *
     * @param email Email address
     * @return DuplicateEntityException instance
     */
    public static DuplicateEntityException email(String email) {
        return of("User", "email", email);
    }

    /**
     * Factory method for duplicate username.
     *
     * @param username Username
     * @return DuplicateEntityException instance
     */
    public static DuplicateEntityException username(String username) {
        return of("User", "username", username);
    }

    /**
     * Factory method for duplicate order ID.
     *
     * @param orderId Order ID
     * @return DuplicateEntityException instance
     */
    public static DuplicateEntityException orderId(String orderId) {
        return of("Order", "orderId", orderId);
    }

    /**
     * Factory method for duplicate bank account.
     * Thrown when the same bank account (bankCode + accountNumber) is already registered by another user.
     *
     * @param bankCode      Bank code (e.g., "VPBANK", "VCB")
     * @param accountNumber Account number (will be partially masked in error message)
     * @return DuplicateEntityException instance
     */
    public static DuplicateEntityException bankAccount(String bankCode, String accountNumber) {
        String masked = accountNumber != null && accountNumber.length() > 4
                ? "***" + accountNumber.substring(accountNumber.length() - 4)
                : "***";
        return new DuplicateEntityException(
                "DUPLICATE_BANK_ACCOUNT",
                String.format("Bank account already registered by another user: %s/%s", bankCode, masked)
        );
    }
}
