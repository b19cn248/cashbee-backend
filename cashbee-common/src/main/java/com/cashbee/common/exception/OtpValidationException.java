package com.cashbee.common.exception;

import lombok.Getter;

/**
 * Exception thrown when OTP validation fails with an incorrect code.
 * Carries structured data (attemptsRemaining) so the API response can include it.
 * Maps to HTTP 400 Bad Request.
 *
 * @author CashBee Team
 */
@Getter
public class OtpValidationException extends BusinessException {

    private final int attemptsRemaining;

    public OtpValidationException(String errorCode, String message, int attemptsRemaining) {
        super(errorCode, message);
        this.attemptsRemaining = attemptsRemaining;
    }
}
