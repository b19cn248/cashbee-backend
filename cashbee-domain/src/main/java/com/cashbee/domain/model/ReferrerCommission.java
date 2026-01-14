package com.cashbee.domain.model;

import com.cashbee.domain.enums.ReferrerCommissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ReferrerCommission Domain Model.
 * Tracks 5% commission from referee's orders paid to referrer.
 *
 * <p>Commission Rules:
 * <ul>
 *   <li>Referrer receives 5% of original platform commission (e.g., Shopee commission)</li>
 *   <li>Commission is paid by the app (not deducted from referee's cashback)</li>
 *   <li>Commission period: 3 months after referee reaches 3 orders</li>
 *   <li>No cap on commission amount</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * Order value: 1,000,000 VND
 * Shopee commission: 10,000 VND
 * Referee cashback (80%): 8,000 VND
 * Referrer commission (5%): 500 VND (paid by app)
 * </pre>
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
public class ReferrerCommission {

    /**
     * Default commission rate: 5%.
     */
    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("5.00");

    /**
     * Commission rate as decimal for calculations: 0.05.
     */
    private static final BigDecimal COMMISSION_RATE_DECIMAL = new BigDecimal("0.05");

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Referrer who receives the commission.
     * Foreign key to user table.
     */
    private Long referrerId;

    /**
     * Referee whose order generated this commission.
     * Foreign key to user table.
     */
    private Long refereeId;

    /**
     * Source order that generated this commission.
     * Foreign key to affiliate_order table.
     */
    private Long sourceOrderId;

    /**
     * Original commission from platform (e.g., Shopee).
     * This is the base amount for calculating referrer commission.
     */
    private BigDecimal originalCommission;

    /**
     * Commission rate: 5.00 (5%).
     */
    @Builder.Default
    private BigDecimal commissionRate = DEFAULT_COMMISSION_RATE;

    /**
     * Commission amount for referrer.
     * Calculated as: originalCommission * 5%
     */
    private BigDecimal commissionAmount;

    /**
     * Commission status: PENDING, CONFIRMED, or PAID.
     */
    @Builder.Default
    private ReferrerCommissionStatus status = ReferrerCommissionStatus.PENDING;

    /**
     * Timestamp when commission was confirmed (order status = PAID).
     */
    private LocalDateTime confirmedAt;

    /**
     * Timestamp when commission was paid to referrer's wallet.
     */
    private LocalDateTime paidAt;

    /**
     * Timestamp when record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Expiration date for this commission.
     * Set to 3 months after referee's referral activation.
     */
    private LocalDateTime expiresAt;

    // ===== Business Logic Methods =====

    /**
     * Calculate commission amount from original platform commission.
     * Formula: originalCommission * 5% (rounded down to nearest integer)
     *
     * @param originalCommission original commission from platform
     * @return calculated commission amount
     */
    public static BigDecimal calculateCommission(BigDecimal originalCommission) {
        if (originalCommission == null || originalCommission.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalCommission.multiply(COMMISSION_RATE_DECIMAL)
                .setScale(0, RoundingMode.FLOOR);
    }

    /**
     * Check if commission has expired.
     *
     * @return true if current time is after expiration date
     */
    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if commission is still within the valid period.
     *
     * @return true if not expired
     */
    public boolean isWithinValidPeriod() {
        return !isExpired();
    }

    /**
     * Check if commission is pending.
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return this.status == ReferrerCommissionStatus.PENDING;
    }

    /**
     * Check if commission is confirmed.
     *
     * @return true if status is CONFIRMED
     */
    public boolean isConfirmed() {
        return this.status == ReferrerCommissionStatus.CONFIRMED;
    }

    /**
     * Check if commission is paid.
     *
     * @return true if status is PAID
     */
    public boolean isPaid() {
        return this.status == ReferrerCommissionStatus.PAID;
    }

    /**
     * Confirm the commission (order has been paid).
     */
    public void confirm() {
        if (this.status != ReferrerCommissionStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm commission in status: " + this.status
            );
        }
        this.status = ReferrerCommissionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * Mark commission as paid to referrer's wallet.
     */
    public void markAsPaid() {
        if (this.status != ReferrerCommissionStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot mark as paid commission in status: " + this.status
            );
        }
        this.status = ReferrerCommissionStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Check if this is a new commission (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Validate commission data.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (this.referrerId == null) {
            throw new IllegalStateException("Referrer ID is required");
        }
        if (this.refereeId == null) {
            throw new IllegalStateException("Referee ID is required");
        }
        if (this.sourceOrderId == null) {
            throw new IllegalStateException("Source order ID is required");
        }
        if (this.originalCommission == null) {
            throw new IllegalStateException("Original commission is required");
        }
        if (this.originalCommission.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Original commission cannot be negative");
        }
        if (this.commissionAmount == null) {
            throw new IllegalStateException("Commission amount is required");
        }
        if (this.commissionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Commission amount cannot be negative");
        }
        if (this.status == null) {
            throw new IllegalStateException("Status is required");
        }
        if (this.expiresAt == null) {
            throw new IllegalStateException("Expiration date is required");
        }
    }

    // ===== Factory Method =====

    /**
     * Create a new referrer commission from an order using default 5% rate.
     *
     * @param referrerId         referrer user ID
     * @param refereeId          referee user ID
     * @param sourceOrderId      source order ID
     * @param originalCommission original platform commission
     * @param expiresAt          commission expiration date
     * @return new ReferrerCommission instance
     */
    public static ReferrerCommission create(Long referrerId, Long refereeId,
                                             Long sourceOrderId, BigDecimal originalCommission,
                                             LocalDateTime expiresAt) {
        BigDecimal commissionAmount = calculateCommission(originalCommission);

        return ReferrerCommission.builder()
                .referrerId(referrerId)
                .refereeId(refereeId)
                .sourceOrderId(sourceOrderId)
                .originalCommission(originalCommission)
                .commissionRate(DEFAULT_COMMISSION_RATE)
                .commissionAmount(commissionAmount)
                .status(ReferrerCommissionStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a new referrer commission from an order with custom commission rate.
     * Used by tier system where referrers have different commission rates.
     *
     * @param referrerId         referrer user ID
     * @param refereeId          referee user ID
     * @param sourceOrderId      source order ID
     * @param originalCommission original platform commission
     * @param commissionRate     custom commission rate (e.g., 5.00, 7.00, 10.00)
     * @param expiresAt          commission expiration date
     * @return new ReferrerCommission instance
     */
    public static ReferrerCommission createWithRate(Long referrerId, Long refereeId,
                                                     Long sourceOrderId, BigDecimal originalCommission,
                                                     BigDecimal commissionRate, LocalDateTime expiresAt) {
        BigDecimal rateDecimal = commissionRate.divide(new BigDecimal("100"));
        BigDecimal commissionAmount = originalCommission.multiply(rateDecimal)
                .setScale(0, RoundingMode.FLOOR);

        return ReferrerCommission.builder()
                .referrerId(referrerId)
                .refereeId(refereeId)
                .sourceOrderId(sourceOrderId)
                .originalCommission(originalCommission)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .status(ReferrerCommissionStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
