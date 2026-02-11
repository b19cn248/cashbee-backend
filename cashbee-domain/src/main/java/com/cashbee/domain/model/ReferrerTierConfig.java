package com.cashbee.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ReferrerTierConfig Domain Model.
 * Defines tiered commission rates for referrers based on their referral count.
 *
 * <p>Tier Levels:
 * <ul>
 *   <li>BRONZE (default): 0+ referrals, 5% commission</li>
 *   <li>SILVER: 5+ referrals, 7% commission + 5k bonus per activation</li>
 *   <li>GOLD: 20+ referrals, 10% commission + 10k bonus per activation</li>
 * </ul>
 *
 * <p>This model is a pure POJO with NO JPA annotations.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class ReferrerTierConfig {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Tier name: BRONZE, SILVER, GOLD.
     */
    private String tierName;

    /**
     * Minimum number of activated referrals required for this tier.
     */
    private Integer minReferrals;

    /**
     * Commission rate for this tier (e.g., 5.00 for 5%).
     */
    private BigDecimal commissionRate;

    /**
     * Bonus amount paid to referrer for each new referral activation.
     */
    @Builder.Default
    private BigDecimal bonusPerActivation = BigDecimal.ZERO;

    /**
     * Description of this tier.
     */
    private String description;

    /**
     * Whether this tier is active.
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Get commission rate as decimal (e.g., 0.05 for 5%).
     *
     * @return commission rate as decimal
     */
    public BigDecimal getCommissionRateDecimal() {
        if (commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return commissionRate.divide(new BigDecimal("100"));
    }

    /**
     * Calculate commission amount from original platform commission.
     *
     * @param originalCommission original commission from platform
     * @return calculated commission amount
     */
    public BigDecimal calculateCommission(BigDecimal originalCommission) {
        if (originalCommission == null || originalCommission.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalCommission.multiply(getCommissionRateDecimal())
                .setScale(0, java.math.RoundingMode.FLOOR);
    }

    /**
     * Check if referrer qualifies for this tier based on referral count.
     *
     * @param activatedReferrals number of activated referrals
     * @return true if referrer qualifies
     */
    public boolean qualifiesFor(int activatedReferrals) {
        return activatedReferrals >= minReferrals;
    }

    /**
     * Check if this tier is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Check if this tier has bonus per activation.
     *
     * @return true if bonus exists
     */
    public boolean hasBonusPerActivation() {
        return bonusPerActivation != null && bonusPerActivation.compareTo(BigDecimal.ZERO) > 0;
    }
}
