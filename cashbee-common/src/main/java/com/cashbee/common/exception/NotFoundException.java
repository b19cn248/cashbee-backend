package com.cashbee.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Typically maps to HTTP 404 Not Found.
 *
 * @author CashBee Team
 */
public class NotFoundException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public NotFoundException(String message, Object... args) {
        super(DEFAULT_ERROR_CODE, message, args);
    }

    public NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for creating NotFoundException with entity type and ID.
     *
     * @param entityType Type of entity (e.g., "User", "Order")
     * @param id Entity identifier
     * @return NotFoundException instance
     */
    public static NotFoundException of(String entityType, Object id) {
        return new NotFoundException(
            String.format("%s not found with id: %s", entityType, id),
            entityType, id
        );
    }

    /**
     * Factory method for creating NotFoundException with entity type and field.
     *
     * @param entityType Type of entity
     * @param fieldName Field name
     * @param fieldValue Field value
     * @return NotFoundException instance
     */
    public static NotFoundException ofField(String entityType, String fieldName, Object fieldValue) {
        return new NotFoundException(
            String.format("%s not found with %s: %s", entityType, fieldName, fieldValue),
            entityType, fieldName, fieldValue
        );
    }
}
