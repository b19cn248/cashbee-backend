package com.cashbee.common.exception;

/**
 * Exception thrown when user doesn't have permission to access a resource.
 * Typically maps to HTTP 403 Forbidden.
 *
 * @author CashBee Team
 */
public class ForbiddenException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for insufficient permissions.
     *
     * @return ForbiddenException instance
     */
    public static ForbiddenException insufficientPermissions() {
        return new ForbiddenException("You don't have permission to perform this action");
    }

    /**
     * Factory method for resource access denied.
     *
     * @param resource Resource name
     * @return ForbiddenException instance
     */
    public static ForbiddenException accessDenied(String resource) {
        return new ForbiddenException(String.format("Access denied to %s", resource));
    }
}
