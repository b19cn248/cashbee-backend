package com.cashbee.application.util.affiliate;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to generate unique tracking codes for affiliate links.
 *
 * Tracking code format: CB{userId}_{clickId}_{timestamp}
 * Example: CB1_100_20251101160530
 *
 * This tracking code is:
 * - Embedded in affiliate URL as af_sub1 parameter
 * - Tracked by Shopee in their system
 * - Returned in CSV import with order data
 * - Used to match orders to users
 *
 * @author CashBee Team
 */
@Component
public class TrackingCodeGenerator {

    private static final String PREFIX = "CB";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate a unique tracking code.
     *
     * Format: CB{userId}_{clickId}_{timestamp}
     *
     * @param userId User ID who created the tracking link
     * @param clickId Click ID (database auto-generated ID)
     * @return Unique tracking code
     */
    public String generate(Long userId, Long clickId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s%d_%d_%s", PREFIX, userId, clickId, timestamp);
    }

    /**
     * Generate a temporary tracking code without click ID.
     * Used when creating AffiliateClick before persisting (to get auto-generated ID).
     *
     * Format: CB{userId}_TEMP_{timestamp}
     *
     * @param userId User ID who is creating the tracking link
     * @return Temporary tracking code
     */
    public String generateTemporary(Long userId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s%d_TEMP_%s", PREFIX, userId, timestamp);
    }

    /**
     * Parse tracking code to extract user ID.
     *
     * Format: CB{userId}_{clickId}_{timestamp}
     *
     * @param trackingCode Tracking code to parse
     * @return User ID extracted from tracking code
     * @throws IllegalArgumentException if tracking code format is invalid
     */
    public Long extractUserId(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be empty");
        }

        if (!trackingCode.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid tracking code format: must start with " + PREFIX);
        }

        try {
            // Remove prefix "CB"
            String withoutPrefix = trackingCode.substring(PREFIX.length());

            // Split by underscore
            String[] parts = withoutPrefix.split("_");

            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid tracking code format: insufficient parts");
            }

            // First part is user ID
            return Long.parseLong(parts[0]);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid tracking code format: user ID is not a number", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tracking code format: " + trackingCode, e);
        }
    }

    /**
     * Parse tracking code to extract click ID.
     *
     * Format: CB{userId}_{clickId}_{timestamp}
     *
     * @param trackingCode Tracking code to parse
     * @return Click ID extracted from tracking code, or null if temporary code
     * @throws IllegalArgumentException if tracking code format is invalid
     */
    public Long extractClickId(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be empty");
        }

        if (!trackingCode.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid tracking code format: must start with " + PREFIX);
        }

        try {
            // Remove prefix "CB"
            String withoutPrefix = trackingCode.substring(PREFIX.length());

            // Split by underscore
            String[] parts = withoutPrefix.split("_");

            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid tracking code format: insufficient parts");
            }

            // Second part is click ID (or "TEMP" for temporary codes)
            if ("TEMP".equals(parts[1])) {
                return null;  // Temporary code, no click ID yet
            }

            return Long.parseLong(parts[1]);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid tracking code format: click ID is not a number", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tracking code format: " + trackingCode, e);
        }
    }

    /**
     * Validate tracking code format.
     *
     * @param trackingCode Tracking code to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String trackingCode) {
        try {
            extractUserId(trackingCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if tracking code is temporary (not yet saved to database).
     *
     * @param trackingCode Tracking code to check
     * @return true if temporary code
     */
    public boolean isTemporary(String trackingCode) {
        if (trackingCode == null || !trackingCode.startsWith(PREFIX)) {
            return false;
        }

        String withoutPrefix = trackingCode.substring(PREFIX.length());
        String[] parts = withoutPrefix.split("_");

        return parts.length >= 2 && "TEMP".equals(parts[1]);
    }
}
