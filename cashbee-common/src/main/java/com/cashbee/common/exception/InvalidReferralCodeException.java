package com.cashbee.common.exception;

/**
 * Exception thrown when referral code is invalid or doesn't exist.
 * Typically maps to HTTP 400 Bad Request.
 *
 * @author CashBee Team
 */
public class InvalidReferralCodeException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "INVALID_REFERRAL_CODE";

    public InvalidReferralCodeException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public InvalidReferralCodeException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Factory method for invalid referral code.
     *
     * @param referralCode The invalid referral code
     * @return InvalidReferralCodeException instance
     */
    public static InvalidReferralCodeException of(String referralCode) {
        return new InvalidReferralCodeException(
            String.format("Invalid referral code: %s", referralCode)
        );
    }

    /**
     * Factory method for referral code not found.
     *
     * @param referralCode The referral code
     * @return InvalidReferralCodeException instance
     */
    public static InvalidReferralCodeException notFound(String referralCode) {
        return new InvalidReferralCodeException(
            "REFERRAL_CODE_NOT_FOUND",
            String.format("Referral code not found: %s", referralCode)
        );
    }

    /**
     * Factory method for expired referral code.
     *
     * @param referralCode The expired referral code
     * @return InvalidReferralCodeException instance
     */
    public static InvalidReferralCodeException expired(String referralCode) {
        return new InvalidReferralCodeException(
            "REFERRAL_CODE_EXPIRED",
            String.format("Referral code has expired: %s", referralCode)
        );
    }
}
