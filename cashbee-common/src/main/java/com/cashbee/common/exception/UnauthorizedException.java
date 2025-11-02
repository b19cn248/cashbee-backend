package com.cashbee.common.exception;

/**
 * Exception thrown when authentication fails or token is invalid.
 * Typically maps to HTTP 401 Unauthorized.
 *
 * @author CashBee Team
 */
public class UnauthorizedException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(DEFAULT_ERROR_CODE, message, cause);
    }

    /**
     * Factory method for invalid token.
     *
     * @return UnauthorizedException instance
     */
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("Invalid or expired token");
    }

    /**
     * Factory method for missing token.
     *
     * @return UnauthorizedException instance
     */
    public static UnauthorizedException missingToken() {
        return new UnauthorizedException("Authorization token is required");
    }
}
