package com.cashbee.common.constant;

/**
 * Centralized error codes for the application.
 * Used for consistent error handling and i18n.
 *
 * @author CashBee Team
 */
public final class ErrorCode {

    // General
    public static final String INTERNAL_SERVER_ERROR = "ERR_INTERNAL_SERVER";
    public static final String VALIDATION_ERROR = "ERR_VALIDATION";
    public static final String NOT_FOUND = "ERR_NOT_FOUND";
    public static final String DUPLICATE_ENTITY = "ERR_DUPLICATE";

    // Authentication & Authorization
    public static final String UNAUTHORIZED = "ERR_UNAUTHORIZED";
    public static final String FORBIDDEN = "ERR_FORBIDDEN";
    public static final String INVALID_TOKEN = "ERR_INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "ERR_TOKEN_EXPIRED";

    // User
    public static final String USER_NOT_FOUND = "ERR_USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "ERR_USER_EXISTS";
    public static final String USER_BANNED = "ERR_USER_BANNED";
    public static final String INVALID_CREDENTIALS = "ERR_INVALID_CREDENTIALS";

    // OTP / Password Reset
    public static final String INVALID_OTP = "ERR_INVALID_OTP";
    public static final String OTP_EXPIRED = "ERR_OTP_EXPIRED";
    public static final String MAX_ATTEMPTS_EXCEEDED = "ERR_MAX_ATTEMPTS_EXCEEDED";
    public static final String OTP_RESEND_COOLDOWN = "ERR_OTP_RESEND_COOLDOWN";
    public static final String INVALID_RESET_TOKEN = "ERR_INVALID_RESET_TOKEN";

    // Wallet
    public static final String WALLET_NOT_FOUND = "ERR_WALLET_NOT_FOUND";
    public static final String INSUFFICIENT_BALANCE = "ERR_INSUFFICIENT_BALANCE";
    public static final String WALLET_LOCKED = "ERR_WALLET_LOCKED";

    // Payout
    public static final String PAYOUT_NOT_FOUND = "ERR_PAYOUT_NOT_FOUND";
    public static final String PAYOUT_ALREADY_PROCESSED = "ERR_PAYOUT_PROCESSED";
    public static final String PAYOUT_BELOW_MINIMUM = "ERR_PAYOUT_BELOW_MIN";
    public static final String PAYOUT_ABOVE_MAXIMUM = "ERR_PAYOUT_ABOVE_MAX";
    public static final String INVALID_PAYMENT_METHOD = "ERR_INVALID_PAYMENT_METHOD";

    // Order & Cashback
    public static final String ORDER_NOT_FOUND = "ERR_ORDER_NOT_FOUND";
    public static final String DUPLICATE_ORDER = "ERR_DUPLICATE_ORDER";
    public static final String CASHBACK_NOT_FOUND = "ERR_CASHBACK_NOT_FOUND";
    public static final String CASHBACK_ALREADY_PAID = "ERR_CASHBACK_PAID";

    // File Processing
    public static final String FILE_PROCESSING_ERROR = "ERR_FILE_PROCESSING";
    public static final String INVALID_FILE_FORMAT = "ERR_INVALID_FILE_FORMAT";
    public static final String FILE_TOO_LARGE = "ERR_FILE_TOO_LARGE";
    public static final String EMPTY_FILE = "ERR_EMPTY_FILE";

    // Import
    public static final String IMPORT_BATCH_NOT_FOUND = "ERR_IMPORT_BATCH_NOT_FOUND";
    public static final String IMPORT_IN_PROGRESS = "ERR_IMPORT_IN_PROGRESS";
    public static final String IMPORT_FAILED = "ERR_IMPORT_FAILED";

    // Platform
    public static final String PLATFORM_NOT_FOUND = "ERR_PLATFORM_NOT_FOUND";
    public static final String PLATFORM_INACTIVE = "ERR_PLATFORM_INACTIVE";

    // Policy
    public static final String POLICY_NOT_FOUND = "ERR_POLICY_NOT_FOUND";
    public static final String NO_APPLICABLE_POLICY = "ERR_NO_POLICY";

    private ErrorCode() {
        // Private constructor to prevent instantiation
    }
}
