package com.cashbee.application.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure OTP (One-Time Password) codes.
 * Uses cryptographically secure random number generation.
 */
public class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int MIN_VALUE = 100000; // 6-digit minimum
    private static final int MAX_VALUE = 999999; // 6-digit maximum

    private OtpGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generate a secure 6-digit OTP code.
     *
     * @return 6-digit OTP as String (e.g., "123456")
     */
    public static String generate() {
        // Generate random number between 100000 and 999999
        int otp = MIN_VALUE + SECURE_RANDOM.nextInt(MAX_VALUE - MIN_VALUE + 1);
        return String.valueOf(otp);
    }

    /**
     * Validate OTP format.
     *
     * @param otp OTP code to validate
     * @return true if OTP is exactly 6 digits
     */
    public static boolean isValidFormat(String otp) {
        if (otp == null || otp.length() != OTP_LENGTH) {
            return false;
        }
        return otp.matches("^\\d{6}$");
    }
}
