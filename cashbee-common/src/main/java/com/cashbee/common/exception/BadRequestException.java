package com.cashbee.common.exception;

/**
 * Exception thrown for bad request scenarios.
 * Maps to HTTP 400 Bad Request.
 * Use this for general request validation failures.
 * For field-level validation, use ValidationException instead.
 *
 * @author CashBee Team
 */
public class BadRequestException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "BAD_REQUEST";

    public BadRequestException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public BadRequestException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(DEFAULT_ERROR_CODE, message, cause);
    }
}
