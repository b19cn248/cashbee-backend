package com.cashbee.common.util;

import com.cashbee.common.constant.AppConstants;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility class for string operations and code generation.
 *
 * @author CashBee Team
 */
public final class StringUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String NUMERIC = "0123456789";

    private StringUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate a unique referral code.
     * Format: CB + 6 random alphanumeric characters (e.g., CB4F7A9K)
     *
     * @return Referral code
     */
    public static String generateReferralCode() {
        return AppConstants.REFERRAL_CODE_PREFIX +
            generateRandomString(AppConstants.REFERRAL_CODE_LENGTH - 2, ALPHANUMERIC);
    }

    /**
     * Generate a unique batch code.
     * Format: BATCH_YYYYMMDD_HHMMSS (e.g., BATCH_20250129_143025)
     *
     * @return Batch code
     */
    public static String generateBatchCode() {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return AppConstants.BATCH_CODE_PREFIX + "_" + timestamp;
    }

    /**
     * Generate a unique transaction code.
     * Format: TXN_YYYYMMDD_HHMMSS_XXX (e.g., TXN_20250129_143025_A8F)
     *
     * @return Transaction code
     */
    public static String generateTransactionCode() {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String random = generateRandomString(3, ALPHANUMERIC);
        return AppConstants.TRANSACTION_CODE_PREFIX + "_" + timestamp + "_" + random;
    }

    /**
     * Generate a unique payout request code.
     * Format: PAYOUT_YYYYMMDD_HHMMSS (e.g., PAYOUT_20250129_143025)
     *
     * @return Payout code
     */
    public static String generatePayoutCode() {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return AppConstants.PAYOUT_CODE_PREFIX + "_" + timestamp;
    }

    /**
     * Generate a random string of specified length using given characters.
     *
     * @param length Length of the string
     * @param characters Characters to use
     * @return Random string
     */
    public static String generateRandomString(int length, String characters) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a random numeric string of specified length.
     *
     * @param length Length of the string
     * @return Random numeric string
     */
    public static String generateRandomNumeric(int length) {
        return generateRandomString(length, NUMERIC);
    }

    /**
     * Generate a unique request ID for tracing.
     *
     * @return Request ID (UUID without hyphens)
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Mask sensitive data (e.g., phone, email, account number).
     * Shows only first 2 and last 2 characters.
     *
     * @param value Value to mask
     * @return Masked value
     */
    public static String maskSensitiveData(String value) {
        if (org.apache.commons.lang3.StringUtils.isBlank(value) || value.length() <= 4) {
            return "****";
        }
        int length = value.length();
        String start = value.substring(0, 2);
        String end = value.substring(length - 2);
        return start + "*".repeat(length - 4) + end;
    }

    /**
     * Mask email address.
     * Example: john.doe@example.com → jo***@example.com
     *
     * @param email Email address
     * @return Masked email
     */
    public static String maskEmail(String email) {
        if (org.apache.commons.lang3.StringUtils.isBlank(email) || !email.contains("@")) {
            return "***@***.com";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return "***@" + domain;
        }

        String maskedLocal = local.substring(0, 2) + "***";
        return maskedLocal + "@" + domain;
    }

    /**
     * Convert string to slug (URL-friendly format).
     *
     * @param text Text to convert
     * @return Slug
     */
    public static String toSlug(String text) {
        if (org.apache.commons.lang3.StringUtils.isBlank(text)) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");
    }

    /**
     * Truncate string to specified length with ellipsis.
     *
     * @param text Text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    public static String truncate(String text, int maxLength) {
        if (org.apache.commons.lang3.StringUtils.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
