package com.cashbee.application.dto.referraladmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for grant missing rewards operation.
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantMissingRewardsResponse {

    /**
     * User ID that was processed.
     */
    private Long userId;

    /**
     * Username for display.
     */
    private String username;

    /**
     * Number of missing milestones found.
     */
    private Integer missingMilestonesCount;

    /**
     * Number of rewards successfully granted.
     */
    private Integer rewardsGranted;

    /**
     * Total referee bonus granted.
     */
    private BigDecimal totalRefereeBonusGranted;

    /**
     * Total referrer bonus granted.
     */
    private BigDecimal totalReferrerBonusGranted;

    /**
     * Whether referral was activated.
     */
    private Boolean referralActivated;

    /**
     * Details of each granted reward.
     */
    private List<GrantedRewardDetail> grantedRewards;

    /**
     * Any errors or skipped rewards.
     */
    private List<String> warnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrantedRewardDetail {
        private Integer milestone;
        private String description;
        private BigDecimal refereeBonusGranted;
        private BigDecimal referrerBonusGranted;
        private Long referrerId;
        private String referrerUsername;
        private Boolean activatedReferral;
    }
}
