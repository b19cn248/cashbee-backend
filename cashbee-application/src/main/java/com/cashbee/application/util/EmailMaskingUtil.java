package com.cashbee.application.util;

/**
 * Utility class for masking email addresses for display purposes.
 */
public class EmailMaskingUtil {

    private EmailMaskingUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Mask email address for security/privacy.
     * Examples:
     * - "john@example.com" -> "j***@ex***ple.com"
     * - "alice.smith@gmail.com" -> "a***@gm***il.com"
     *
     * @param email Email address to mask
     * @return Masked email address
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }

        String localPart = parts[0];
        String domain = parts[1];

        // Mask local part (keep first character)
        String maskedLocal = maskString(localPart);

        // Mask domain (keep first 2 characters and last part after last dot)
        String maskedDomain = maskDomain(domain);

        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * Mask a string by keeping first character and replacing rest with ***
     */
    private static String maskString(String str) {
        if (str == null || str.length() <= 1) {
            return str;
        }
        return str.charAt(0) + "***";
    }

    /**
     * Mask domain part of email
     * Examples:
     * - "example.com" -> "ex***ple.com"
     * - "gmail.com" -> "gm***il.com"
     * - "mail.google.com" -> "ma***le.com"
     */
    private static String maskDomain(String domain) {
        if (domain == null || domain.length() <= 3) {
            return domain;
        }

        // Find last dot
        int lastDotIndex = domain.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            // No dot or dot at start - just mask normally
            return maskString(domain);
        }

        String beforeLastDot = domain.substring(0, lastDotIndex);
        String afterLastDot = domain.substring(lastDotIndex + 1);

        // Keep first 2 chars of domain part before last dot
        String maskedBeforeDot;
        if (beforeLastDot.length() <= 2) {
            maskedBeforeDot = beforeLastDot;
        } else {
            maskedBeforeDot = beforeLastDot.substring(0, 2) + "***" +
                             beforeLastDot.substring(beforeLastDot.length() - 2);
        }

        return maskedBeforeDot + "." + afterLastDot;
    }
}
