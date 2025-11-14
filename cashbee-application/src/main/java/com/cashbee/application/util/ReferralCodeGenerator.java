package com.cashbee.application.util;

import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility class to generate unique referral codes for users.
 *
 * Format: CB + 6 random alphanumeric characters (uppercase)
 * Examples: CB4F7A9K, CBXYZ123, CBABC789
 *
 * Features:
 * - Cryptographically secure random generation
 * - Uniqueness guarantee (checks database before returning)
 * - Retry mechanism if duplicate found (max 10 attempts)
 * - Only uppercase letters and numbers for clarity
 *
 * Usage in Registration:
 * 1. User registers
 * 2. System generates unique referral code
 * 3. Code stored in user.referral_code
 * 4. User shares code with friends
 * 5. Friends use code during registration (referred_by field)
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferralCodeGenerator {

    private final UserRepository userRepository;

    /**
     * Prefix for all referral codes.
     * Makes codes easily recognizable as CashBee referral codes.
     */
    private static final String CODE_PREFIX = "CB";

    /**
     * Length of random part (excluding prefix).
     * Total code length = 2 (prefix) + 6 (random) = 8 characters.
     */
    private static final int RANDOM_LENGTH = 6;

    /**
     * Maximum attempts to generate unique code.
     * If all 10 attempts produce duplicates, throw exception.
     */
    private static final int MAX_ATTEMPTS = 10;

    /**
     * Character set for random code generation.
     * Only uppercase letters and numbers for clarity (no lowercase to avoid confusion).
     * Excludes similar-looking characters: 0/O, 1/I, etc.
     */
    private static final String CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * Secure random generator.
     * Uses cryptographically strong random number generator.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a unique referral code.
     *
     * This method:
     * 1. Generates random code: CB + 6 random chars
     * 2. Checks if code exists in database
     * 3. If exists, retry up to MAX_ATTEMPTS times
     * 4. Returns unique code or throws exception
     *
     * @return Unique referral code (e.g., CB4F7A9K)
     * @throws RuntimeException if unable to generate unique code after MAX_ATTEMPTS
     */
    public String generate() {
        log.debug("Generating unique referral code...");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String code = generateRandomCode();
            log.debug("Attempt {}: Generated code: {}", attempt, code);

            // Check if code already exists
            if (!exists(code)) {
                log.info("Successfully generated unique referral code: {}", code);
                return code;
            }

            log.warn("Attempt {}: Code {} already exists, retrying...", attempt, code);
        }

        // If we reach here, all attempts failed
        log.error("Failed to generate unique referral code after {} attempts", MAX_ATTEMPTS);
        throw new RuntimeException("Unable to generate unique referral code after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Generate random code without uniqueness check.
     *
     * Format: CB + 6 random uppercase alphanumeric characters
     * Example: CB4F7A9K
     *
     * @return Random code (may not be unique)
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_PREFIX);

        for (int i = 0; i < RANDOM_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_SET.length());
            code.append(CHAR_SET.charAt(index));
        }

        return code.toString();
    }

    /**
     * Check if referral code exists in database.
     *
     * @param code Referral code to check
     * @return true if code already exists, false otherwise
     */
    public boolean exists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        boolean exists = userRepository.existsByReferralCode(code);
        log.debug("Checking if code {} exists: {}", code, exists);
        return exists;
    }

    /**
     * Validate referral code format.
     *
     * Checks if code matches the expected format: CB + 6 alphanumeric chars
     *
     * @param code Code to validate
     * @return true if format is valid
     */
    public boolean isValidFormat(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        // Must be exactly 8 characters
        if (code.length() != CODE_PREFIX.length() + RANDOM_LENGTH) {
            return false;
        }

        // Must start with prefix
        if (!code.startsWith(CODE_PREFIX)) {
            return false;
        }

        // Remaining characters must be from CHAR_SET
        String randomPart = code.substring(CODE_PREFIX.length());
        for (char c : randomPart.toCharArray()) {
            if (CHAR_SET.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the code prefix.
     *
     * @return Code prefix (CB)
     */
    public String getCodePrefix() {
        return CODE_PREFIX;
    }

    /**
     * Get the total code length.
     *
     * @return Total length (8 characters: CB + 6 random)
     */
    public int getCodeLength() {
        return CODE_PREFIX.length() + RANDOM_LENGTH;
    }
}
