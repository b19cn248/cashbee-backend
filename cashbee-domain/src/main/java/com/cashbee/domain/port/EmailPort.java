package com.cashbee.domain.port;

/**
 * Port interface for email sending operations.
 * Follows the Port pattern in Hexagonal Architecture.
 *
 * This abstraction allows the domain layer to send emails without knowing
 * the implementation details (SMTP, SendGrid, AWS SES, etc.)
 */
public interface EmailPort {

    /**
     * Send OTP verification email to user
     *
     * @param to Recipient email address
     * @param otpCode The 6-digit OTP code
     * @param expiryMinutes How many minutes until OTP expires
     * @throws EmailSendException if email sending fails
     */
    void sendOtpEmail(String to, String otpCode, int expiryMinutes);

    /**
     * Send welcome email after successful registration
     *
     * @param to Recipient email address
     * @param username User's username
     * @param fullName User's full name
     * @param referralCode User's unique referral code
     * @throws EmailSendException if email sending fails
     */
    void sendWelcomeEmail(String to, String username, String fullName, String referralCode);

    /**
     * Send password reset OTP email (future feature)
     *
     * @param to Recipient email address
     * @param otpCode The 6-digit OTP code
     * @param expiryMinutes How many minutes until OTP expires
     * @throws EmailSendException if email sending fails
     */
    void sendPasswordResetOtpEmail(String to, String otpCode, int expiryMinutes);

    /**
     * Send email change verification email (future feature)
     *
     * @param to Recipient email address
     * @param otpCode The 6-digit OTP code
     * @param expiryMinutes How many minutes until OTP expires
     * @throws EmailSendException if email sending fails
     */
    default void sendEmailChangeOtpEmail(String to, String otpCode, int expiryMinutes) {
        throw new UnsupportedOperationException("Email change OTP email not yet implemented");
    }

    /**
     * Exception thrown when email sending fails
     */
    class EmailSendException extends RuntimeException {
        public EmailSendException(String message) {
            super(message);
        }

        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
