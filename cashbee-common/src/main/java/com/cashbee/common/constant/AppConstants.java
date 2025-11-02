package com.cashbee.common.constant;

/**
 * Application-wide constants.
 *
 * @author CashBee Team
 */
public final class AppConstants {

    // Application Info
    public static final String APP_NAME = "CashBee";
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;

    // Pagination
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String MAX_PAGE_SIZE = "100";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // Date/Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATETIME_FORMAT_WITH_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String TIME_ZONE = "Asia/Ho_Chi_Minh";

    // Wallet
    public static final String DEFAULT_CURRENCY = "VND";
    public static final int DECIMAL_SCALE = 2;

    // File Upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final String[] ALLOWED_EXCEL_TYPES = {
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    };
    public static final String[] ALLOWED_JSON_TYPES = {
        "application/json"
    };

    // Code Generation
    public static final String REFERRAL_CODE_PREFIX = "CB";
    public static final int REFERRAL_CODE_LENGTH = 8;
    public static final String BATCH_CODE_PREFIX = "BATCH";
    public static final String TRANSACTION_CODE_PREFIX = "TXN";
    public static final String PAYOUT_CODE_PREFIX = "PAYOUT";

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 50;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final String PHONE_REGEX = "^0[0-9]{9,10}$"; // Vietnamese phone format
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // Request Headers
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CLIENT_IP = "X-Forwarded-For";

    // Cache Keys
    public static final String CACHE_USER = "user";
    public static final String CACHE_WALLET = "wallet";
    public static final String CACHE_PLATFORM = "platform";
    public static final String CACHE_POLICY = "policy";
    public static final String CACHE_CONFIG = "config";

    // Cache TTL (seconds)
    public static final long CACHE_TTL_SHORT = 300; // 5 minutes
    public static final long CACHE_TTL_MEDIUM = 1800; // 30 minutes
    public static final long CACHE_TTL_LONG = 3600; // 1 hour

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}
