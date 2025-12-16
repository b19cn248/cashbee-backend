package com.cashbee.application.dto.user;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for user data.
 *
 * This is returned by application services and exposed to the presentation layer.
 * Contains basic user information without sensitive data.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /**
     * Database ID.
     */
    private Long id;

    /**
     * Keycloak user ID.
     */
    private String keycloakId;

    /**
     * Username.
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
     * Phone number.
     */
    private String phone;

    /**
     * User's unique referral code.
     */
    private String referralCode;

    /**
     * Referral code of the user who referred this user.
     */
    private String referredBy;

    /**
     * User status (ACTIVE, INACTIVE, BANNED).
     */
    private String status;

    /**
     * User tier level (NORMAL, VIP, SUPER, DIAMOND).
     * Determines cashback rate percentage.
     */
    private String userLevel;

    /**
     * Account creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     */
    private LocalDateTime updatedAt;

    /**
     * Bank account number (if user has set up).
     */
    private String accountNumber;

    /**
     * Bank account holder name (if user has set up).
     */
    private String accountName;

    /**
     * Bank code (e.g., VPBANK, ACB).
     */
    private String bankCode;

    /**
     * Full bank name (e.g., "Ngân hàng TMCP Việt Nam Thịnh Vượng").
     */
    private String bankName;
}
