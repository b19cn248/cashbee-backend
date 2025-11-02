package com.cashbee.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 * Can hold multiple field-level validation errors.
 * Typically maps to HTTP 400 Bad Request.
 *
 * @author CashBee Team
 */
@Getter
public class ValidationException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(DEFAULT_ERROR_CODE, message);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(DEFAULT_ERROR_CODE, message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Add a field-level validation error.
     *
     * @param field Field name
     * @param message Error message
     * @return This ValidationException instance (for method chaining)
     */
    public ValidationException addFieldError(String field, String message) {
        this.fieldErrors.put(field, message);
        return this;
    }

    /**
     * Check if there are any field-level errors.
     *
     * @return true if there are field errors
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    /**
     * Factory method for creating ValidationException with a single field error.
     *
     * @param field Field name
     * @param message Error message
     * @return ValidationException instance
     */
    public static ValidationException of(String field, String message) {
        ValidationException exception = new ValidationException("Validation failed");
        exception.addFieldError(field, message);
        return exception;
    }
}
