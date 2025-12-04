package com.cashbee.domain.model;

import com.cashbee.domain.enums.UserLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * CashbackPolicy Domain Model. Pure business object without any infrastructure concerns.
 * <p>
 * Represents a cashback policy that defines how much cashback users receive from affiliate
 * commissions. Different policies can apply based on: - Platform (Shopee, Lazada, etc.) - User
 * level (NORMAL, VIP, SUPER) - Date range (effectiveFrom, effectiveTo) - Order value
 * (minOrderValue)
 * <p>
 * Example: "VIP users get 80% of commission as cashback for Shopee orders"
 * <p>
 * This model is a pure POJO with NO JPA annotations.
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
public class CashbackPolicy {

  /**
   * Internal database ID (auto-generated).
   */
  private Long id;

  /**
   * Policy name for display (e.g., "VIP Shopee Policy").
   */
  private String policyName;

  /**
   * Policy code for identification (e.g., "VIP_SHOPEE_2025"). Must be unique.
   */
  private String policyCode;

  /**
   * Platform ID this policy applies to. Null means applies to ALL platforms. Foreign key to
   * affiliate_platform table.
   */
  private Long platformId;

  /**
   * User level this policy applies to. Different levels get different cashback rates.
   */
  private UserLevel userLevel;

  /**
   * Cashback rate as percentage of commission. Example: 70.00 means user gets 70% of commission as
   * cashback. Range: 0.00 - 100.00
   */
  private BigDecimal cashbackRate;

  /**
   * Minimum order value for policy to apply. Orders below this amount don't qualify for cashback.
   * Null or zero means no minimum.
   */
  private BigDecimal minOrderValue;

  /**
   * Maximum cashback amount per order (cap). Null means no cap. Example: If set to 100,000 VND,
   * user can't get more than that per order.
   */
  private BigDecimal maxCashbackPerOrder;

  /**
   * Whether policy is currently active. Inactive policies are ignored.
   */
  @Builder.Default
  private Boolean isActive = true;

  /**
   * Policy priority for conflict resolution. Higher priority wins if multiple policies match.
   * Default: 0
   */
  @Builder.Default
  private Integer priority = 0;

  /**
   * Policy effective start date/time. Policy only applies from this date onwards.
   */
  private LocalDateTime effectiveFrom;

  /**
   * Policy effective end date/time. Null means no end date (permanent).
   */
  private LocalDateTime effectiveTo;

  /**
   * Timestamp when policy was created.
   */
  private LocalDateTime createdAt;

  /**
   * Timestamp when policy was last updated.
   */
  private LocalDateTime updatedAt;

  // ===== Business Logic Methods =====

  /**
   * Check if policy is active.
   *
   * @return true if policy is active
   */
  public boolean isActive() {
    return this.isActive != null && this.isActive;
  }

  /**
   * Activate the policy.
   */
  public void activate() {
    this.isActive = true;
  }

  /**
   * Deactivate the policy.
   */
  public void deactivate() {
    this.isActive = false;
  }

  /**
   * Check if policy is effective right now. Policy must be: - Active - Started (effectiveFrom <=
   * now) - Not expired (effectiveTo >= now or null)
   *
   * @return true if policy is effective now
   */
  public boolean isEffectiveNow() {
    LocalDateTime now = LocalDateTime.now();

    // Must be active
    if (!isActive()) {
      return false;
    }

    // Must have started
    if (this.effectiveFrom != null && this.effectiveFrom.isAfter(now)) {
      return false;
    }

    // Must not be expired
    return this.effectiveTo == null || !this.effectiveTo.isBefore(now);
  }

  /**
   * Check if policy has expired.
   *
   * @return true if policy has an end date and it's in the past
   */
  public boolean isExpired() {
    if (this.effectiveTo == null) {
      return false; // No end date = never expires
    }
    return this.effectiveTo.isBefore(LocalDateTime.now());
  }

  /**
   * Calculate cashback amount from commission amount. Applies cashback rate and respects max
   * cashback cap.
   * <p>
   * Formula: cashback = commission * (rate / 100) If result > maxCashback, return maxCashback
   *
   * @param commissionAmount original commission from platform
   * @return cashback amount to give to user
   */
  public BigDecimal calculateCashback(BigDecimal commissionAmount) {
    if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    // Calculate: commission * rate / 100
    BigDecimal rate = this.cashbackRate.divide(
        new BigDecimal("100"),
        2,
        RoundingMode.HALF_UP
    );

    BigDecimal cashback = commissionAmount.multiply(rate)
        .setScale(2, RoundingMode.HALF_UP);

    // Apply max cap if exists
    if (this.maxCashbackPerOrder != null
        && cashback.compareTo(this.maxCashbackPerOrder) > 0) {
      return this.maxCashbackPerOrder;
    }

    return cashback;
  }

  /**
   * Check if policy is applicable to given order value.
   *
   * @param orderValue order value to check
   * @return true if order value meets minimum requirement
   */
  public boolean isApplicableToOrderValue(BigDecimal orderValue) {
    if (orderValue == null) {
      return false;
    }

    // If no minimum, always applicable
    if (this.minOrderValue == null || this.minOrderValue.compareTo(BigDecimal.ZERO) == 0) {
      return true;
    }

    // Order value must be >= minimum
    return orderValue.compareTo(this.minOrderValue) >= 0;
  }

  /**
   * Check if policy applies to given platform.
   *
   * @param platformId platform ID to check
   * @return true if policy applies to this platform
   */
  public boolean isApplicableToPlatform(Long platformId) {
    // If policy platformId is null, applies to all platforms
    if (this.platformId == null) {
      return true;
    }

    // Otherwise, must match exactly
    return this.platformId.equals(platformId);
  }

  /**
   * Check if policy is for given user level.
   *
   * @param userLevel user level to check
   * @return true if policy is for this user level
   */
  public boolean isForUserLevel(UserLevel userLevel) {
    return this.userLevel == userLevel;
  }

  /**
   * Check if this is a new policy (not persisted yet).
   *
   * @return true if id is null
   */
  public boolean isNew() {
    return this.id == null;
  }

  /**
   * Validate policy data. Throws exception if validation fails.
   */
  public void validate() {
    if (this.policyName == null || this.policyName.isBlank()) {
      throw new IllegalStateException("Policy name is required");
    }

    if (this.policyCode == null || this.policyCode.isBlank()) {
      throw new IllegalStateException("Policy code is required");
    }

    if (this.userLevel == null) {
      throw new IllegalStateException("User level is required");
    }

    if (this.cashbackRate == null) {
      throw new IllegalStateException("Cashback rate is required");
    }

    // Cashback rate must be 0-100
    if (this.cashbackRate.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalStateException("Cashback rate cannot be negative");
    }

    if (this.cashbackRate.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalStateException("Cashback rate cannot exceed 100%");
    }

    if (this.effectiveFrom == null) {
      throw new IllegalStateException("Effective from date is required");
    }

    // If both dates exist, effectiveTo must be after effectiveFrom
    if (this.effectiveTo != null && this.effectiveTo.isBefore(this.effectiveFrom)) {
      throw new IllegalStateException("Effective to date must be after effective from date");
    }

    // Priority cannot be negative
    if (this.priority != null && this.priority < 0) {
      throw new IllegalStateException("Priority cannot be negative");
    }
  }
}
