package com.cashbee.application.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user registration.
 *
 * Contains all information about the newly registered user:
 * - User identification: userId, keycloakId, username, email
 * - Profile info: fullName, phone
 * - Referral info: referralCode (user's own code), referredBy (referrer's code)
 * - Wallet info: walletId
 * - Timestamps: createdAt
 *
 * This response is sent back to mobile app after successful registration.
 * Mobile app should:
 * 1. Store userId and referralCode locally
 * 2. Display success message with referral code
 * 3. Navigate user to login screen or auto-login
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    /**
     * Internal database user ID.
     * Used for all subsequent API calls.
     */
    private Long userId;

    /**
     * Keycloak user UUID.
     * Links local user to Keycloak authentication.
     */
    private String keycloakId;

    /**
     * Username (login identifier).
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * User's full name.
     */
    private String fullName;

    /**
     * Phone number (Vietnamese format).
     */
    private String phone;

    /**
     * User's unique referral code.
     * Format: CB + 6 alphanumeric characters (e.g., CB4F7A9K)
     *
     * User can share this code with friends to earn referral bonuses.
     * Mobile app should display this prominently in user profile.
     */
    private String referralCode;

    /**
     * Referral code of the person who referred this user.
     * Null if user was not referred by anyone.
     */
    private String referredBy;

    /**
     * User's wallet ID.
     * Automatically created during registration with balance = 0.
     */
    private Long walletId;

    /**
     * User account status.
     * Should be "ACTIVE" after successful registration.
     */
    private String status;

    /**
     * Registration timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Success message for mobile app.
     * Example: "Registration successful! Your referral code is CB4F7A9K"
     * Or: "OTP has been sent to your email. Please verify to complete registration."
     */
    private String message;

    /**
     * Indicates if OTP verification is required.
     * If true, frontend should navigate to OTP verification screen.
     * If false, registration is complete (old flow - not used anymore).
     */
    private boolean requiresOtp;

    /**
     * Masked email for display on OTP screen.
     * Example: "j***@ex***ple.com"
     * Only populated when requiresOtp = true.
     */
    private String maskedEmail;

    /**
     * OTP expiry time in seconds.
     * Example: 300 (5 minutes)
     * Only populated when requiresOtp = true.
     */
    private Integer expiresIn;

    /**
     * Check if user was referred by someone.
     *
     * @return true if user has a referrer
     */
    public boolean hasReferrer() {
        return referredBy != null && !referredBy.isBlank();
    }

    /**
     * Factory method for OTP-required response (new flow).
     * Used when registration creates disabled Keycloak user and sends OTP.
     *
     * @param email User's email
     * @param maskedEmail Masked email for display
     * @param expiresIn OTP expiry in seconds
     * @return RegisterResponse indicating OTP is required
     */
    public static RegisterResponse requiresOtpVerification(String email, String maskedEmail, int expiresIn) {
        return RegisterResponse.builder()
            .email(email)
            .maskedEmail(maskedEmail)
            .requiresOtp(true)
            .expiresIn(expiresIn)
            .message("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra và xác thực để hoàn tất đăng ký.")
            .build();
    }
}
