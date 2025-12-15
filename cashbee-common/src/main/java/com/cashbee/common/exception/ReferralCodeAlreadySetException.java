package com.cashbee.common.exception;

/**
 * Exception thrown when user tries to set referral code but already has one.
 * Typically maps to HTTP 409 Conflict.
 *
 * @author CashBee Team
 */
public class ReferralCodeAlreadySetException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "REFERRAL_CODE_ALREADY_SET";

    public ReferralCodeAlreadySetException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ReferralCodeAlreadySetException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for user already has referral code set.
     *
     * @param userId The user ID
     * @return ReferralCodeAlreadySetException instance
     */
    public static ReferralCodeAlreadySetException forUser(Long userId) {
        return new ReferralCodeAlreadySetException(
            String.format("User %d already has a referral code set", userId)
        );
    }

    /**
     * Factory method with existing referral code info.
     *
     * @param userId The user ID
     * @param existingCode The existing referral code
     * @return ReferralCodeAlreadySetException instance
     */
    public static ReferralCodeAlreadySetException forUser(Long userId, String existingCode) {
        return new ReferralCodeAlreadySetException(
            String.format("User %d already referred by code: %s", userId, existingCode)
        );
    }
}
