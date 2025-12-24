package com.cashbee.application.dto.referral;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for getting list of users referred by current user.
 *
 * This DTO contains:
 * - User's own referral code
 * - Summary statistics (total, active referrals)
 * - Detailed list of referred users with privacy-masked info
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyReferralsResponse {

    /**
     * User's own referral code (for sharing).
     */
    private String myReferralCode;

    /**
     * Total number of users who used this referral code.
     */
    private Integer totalReferrals;

    /**
     * Number of referrals still within 3-month commission period.
     */
    private Integer activeReferrals;

    /**
     * Detailed list of referred users.
     */
    private List<ReferredUserInfo> referrals;

    /**
     * Information about a referred user.
     * Personal info is masked for privacy.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReferredUserInfo {

        /**
         * Masked name of referred user (e.g., "Ng****").
         */
        private String name;

        /**
         * Masked email (e.g., "ng***@gmail.com").
         */
        private String email;

        /**
         * User status (ACTIVE, SUSPENDED, BANNED).
         */
        private String status;

        /**
         * When the user joined (registered).
         */
        private LocalDateTime joinedAt;

        /**
         * Number of completed orders by this user.
         */
        private Integer completedOrders;

        /**
         * Whether referral is activated (user reached 3 orders).
         * When activated, referrer receives 5% commission.
         */
        private boolean referralActivated;

        /**
         * When referral was activated (user reached 3 orders).
         */
        private LocalDateTime referralActivatedAt;

        /**
         * When the 3-month commission period expires.
         * Null if not yet activated.
         */
        private LocalDateTime referralExpiresAt;

        /**
         * Whether still within 3-month commission period.
         */
        private boolean withinCommissionPeriod;
    }
}
