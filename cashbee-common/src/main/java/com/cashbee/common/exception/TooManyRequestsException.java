package com.cashbee.common.exception;

/**
 * Exception thrown when a rate limit or cooldown is violated.
 * Maps to HTTP 429 Too Many Requests.
 *
 * @author CashBee Team
 */
public class TooManyRequestsException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "TOO_MANY_REQUESTS";

    public TooManyRequestsException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public TooManyRequestsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
