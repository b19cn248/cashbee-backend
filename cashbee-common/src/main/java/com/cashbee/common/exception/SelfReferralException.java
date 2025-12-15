package com.cashbee.common.exception;

/**
 * Exception thrown when user tries to use their own referral code.
 * Typically maps to HTTP 400 Bad Request.
 *
 * @author CashBee Team
 */
public class SelfReferralException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "SELF_REFERRAL_NOT_ALLOWED";

    public SelfReferralException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public SelfReferralException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for self referral attempt.
     *
     * @param userId The user ID attempting self-referral
     * @return SelfReferralException instance
     */
    public static SelfReferralException forUser(Long userId) {
        return new SelfReferralException(
            String.format("User %d cannot use their own referral code", userId)
        );
    }
}
