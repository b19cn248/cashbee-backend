package com.cashbee.common.util;

import com.cashbee.common.constant.AppConstants;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date/time operations.
 *
 * @author CashBee Team
 */
public final class DateTimeUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of(AppConstants.TIME_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT);
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern(AppConstants.DATETIME_FORMAT);

    private DateTimeUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get current date/time in default timezone.
     *
     * @return Current LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * Get current date in default timezone.
     *
     * @return Current LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    /**
     * Format LocalDateTime to string.
     *
     * @param dateTime LocalDateTime to format
     * @return Formatted string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Format LocalDate to string.
     *
     * @param date LocalDate to format
     * @return Formatted string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Parse string to LocalDateTime.
     *
     * @param dateTimeString Date/time string
     * @return LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (org.apache.commons.lang3.StringUtils.isBlank(dateTimeString)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
    }

    /**
     * Parse string to LocalDate.
     *
     * @param dateString Date string
     * @return LocalDate
     */
    public static LocalDate parseDate(String dateString) {
        if (org.apache.commons.lang3.StringUtils.isBlank(dateString)) {
            return null;
        }
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }

    /**
     * Get start of day for given date.
     *
     * @param date Date
     * @return Start of day (00:00:00)
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * Get end of day for given date.
     *
     * @param date Date
     * @return End of day (23:59:59)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * Get start of month for given date.
     *
     * @param date Date
     * @return First day of month
     */
    public static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Get end of month for given date.
     *
     * @param date Date
     * @return Last day of month
     */
    public static LocalDate endOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * Calculate days between two dates.
     *
     * @param start Start date
     * @param end End date
     * @return Number of days
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Calculate days between two date/times.
     *
     * @param start Start date/time
     * @param end End date/time
     * @return Number of days
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Check if date is in range.
     *
     * @param date Date to check
     * @param start Start of range (inclusive)
     * @param end End of range (inclusive)
     * @return true if date is in range
     */
    public static boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Check if date/time is in range.
     *
     * @param dateTime DateTime to check
     * @param start Start of range (inclusive)
     * @param end End of range (inclusive)
     * @return true if date/time is in range
     */
    public static boolean isInRange(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * Add days to date.
     *
     * @param date Date
     * @param days Number of days to add
     * @return New date
     */
    public static LocalDate addDays(LocalDate date, long days) {
        return date.plusDays(days);
    }

    /**
     * Add months to date.
     *
     * @param date Date
     * @param months Number of months to add
     * @return New date
     */
    public static LocalDate addMonths(LocalDate date, long months) {
        return date.plusMonths(months);
    }

    /**
     * Convert LocalDateTime to epoch milliseconds.
     *
     * @param dateTime LocalDateTime
     * @return Epoch milliseconds
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * Convert epoch milliseconds to LocalDateTime.
     *
     * @param epochMilli Epoch milliseconds
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), DEFAULT_ZONE);
    }

    /**
     * Check if date is today.
     *
     * @param date Date to check
     * @return true if date is today
     */
    public static boolean isToday(LocalDate date) {
        return date.equals(today());
    }

    /**
     * Check if date/time is past.
     *
     * @param dateTime DateTime to check
     * @return true if date/time is before now
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime.isBefore(now());
    }

    /**
     * Check if date/time is future.
     *
     * @param dateTime DateTime to check
     * @return true if date/time is after now
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(now());
    }
}
