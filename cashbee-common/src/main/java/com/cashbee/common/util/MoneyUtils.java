package com.cashbee.common.util;

import com.cashbee.common.constant.AppConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for money/currency operations.
 * All calculations use BigDecimal for precision.
 *
 * @author CashBee Team
 */
public final class MoneyUtils {

    private static final int SCALE = AppConstants.DECIMAL_SCALE;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final NumberFormat CURRENCY_FORMAT =
        NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    private MoneyUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create BigDecimal from double with proper scale.
     *
     * @param value Double value
     * @return BigDecimal with proper scale
     */
    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Create BigDecimal from string with proper scale.
     *
     * @param value String value
     * @return BigDecimal with proper scale
     */
    public static BigDecimal of(String value) {
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return new BigDecimal(value).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Create BigDecimal from long with proper scale.
     *
     * @param value Long value
     * @return BigDecimal with proper scale
     */
    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Add two money values.
     *
     * @param a First value
     * @param b Second value
     * @return Sum
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Subtract two money values.
     *
     * @param a First value (minuend)
     * @param b Second value (subtrahend)
     * @return Difference
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Multiply money value by quantity.
     *
     * @param amount Money amount
     * @param quantity Quantity
     * @return Product
     */
    public static BigDecimal multiply(BigDecimal amount, BigDecimal quantity) {
        return amount.multiply(quantity).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Divide money value by divisor.
     *
     * @param amount Money amount
     * @param divisor Divisor
     * @return Quotient
     */
    public static BigDecimal divide(BigDecimal amount, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return amount.divide(divisor, SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate percentage of amount.
     * Example: percentage(1000, 70) = 700 (70% of 1000)
     *
     * @param amount Base amount
     * @param percentage Percentage (0-100)
     * @return Result
     */
    public static BigDecimal percentage(BigDecimal amount, BigDecimal percentage) {
        return multiply(amount, divide(percentage, BigDecimal.valueOf(100)));
    }

    /**
     * Calculate cashback amount from commission.
     *
     * @param commission Commission amount
     * @param rate Cashback rate (0-100)
     * @return Cashback amount
     */
    public static BigDecimal calculateCashback(BigDecimal commission, BigDecimal rate) {
        return percentage(commission, rate);
    }

    /**
     * Check if amount is zero.
     *
     * @param amount Amount to check
     * @return true if amount is zero
     */
    public static boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if amount is positive.
     *
     * @param amount Amount to check
     * @return true if amount is greater than zero
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if amount is negative.
     *
     * @param amount Amount to check
     * @return true if amount is less than zero
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if first amount is greater than second.
     *
     * @param a First amount
     * @param b Second amount
     * @return true if a > b
     */
    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    /**
     * Check if first amount is less than second.
     *
     * @param a First amount
     * @param b Second amount
     * @return true if a < b
     */
    public static boolean isLessThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) < 0;
    }

    /**
     * Check if first amount is greater than or equal to second.
     *
     * @param a First amount
     * @param b Second amount
     * @return true if a >= b
     */
    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }

    /**
     * Check if first amount is less than or equal to second.
     *
     * @param a First amount
     * @param b Second amount
     * @return true if a <= b
     */
    public static boolean isLessThanOrEqual(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0;
    }

    /**
     * Get maximum of two amounts.
     *
     * @param a First amount
     * @param b Second amount
     * @return Maximum
     */
    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.max(b);
    }

    /**
     * Get minimum of two amounts.
     *
     * @param a First amount
     * @param b Second amount
     * @return Minimum
     */
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.min(b);
    }

    /**
     * Format amount as currency string.
     * Example: 1000000 → ₫1,000,000
     *
     * @param amount Amount to format
     * @return Formatted currency string
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return CURRENCY_FORMAT.format(0);
        }
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Format amount with thousand separators.
     * Example: 1000000 → 1,000,000.00
     *
     * @param amount Amount to format
     * @return Formatted string
     */
    public static String formatDecimal(BigDecimal amount) {
        if (amount == null) {
            return DECIMAL_FORMAT.format(0);
        }
        return DECIMAL_FORMAT.format(amount);
    }

    /**
     * Ensure amount has proper scale.
     *
     * @param amount Amount to scale
     * @return Scaled amount
     */
    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Get absolute value of amount.
     *
     * @param amount Amount
     * @return Absolute value
     */
    public static BigDecimal abs(BigDecimal amount) {
        return amount.abs();
    }

    /**
     * Negate amount (change sign).
     *
     * @param amount Amount
     * @return Negated amount
     */
    public static BigDecimal negate(BigDecimal amount) {
        return amount.negate();
    }
}
