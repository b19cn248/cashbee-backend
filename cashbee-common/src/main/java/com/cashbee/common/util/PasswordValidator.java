package com.cashbee.common.util;

import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;

/**
 * Utility class for validating password strength requirements.
 * Shared across use cases that deal with password creation or change.
 *
 * @author CashBee Team
 */
public final class PasswordValidator {

    private PasswordValidator() {
        // Utility class — no instantiation
    }

    /**
     * Validate password meets minimum strength requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     *
     * @param password the password to validate
     * @throws BadRequestException if any requirement is not met
     */
    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "Password must be at least 8 characters long.");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "Password must contain at least one uppercase letter.");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "Password must contain at least one lowercase letter.");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                "Password must contain at least one digit.");
        }
    }
}
