package com.cashbee.domain.model;

import com.cashbee.common.exception.InsufficientBalanceException;
import com.cashbee.common.util.MoneyUtils;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UserWallet Domain Model.
 * Pure business object managing user's financial balances.
 *
 * Business Rules:
 * - balance: Available balance that can be withdrawn
 * - pendingBalance: Cashback pending confirmation from platform
 * - lockedBalance: Balance locked for withdrawal processing
 * - All amounts must be non-negative
 * - Balance operations are atomic and validated
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
public class UserWallet {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Reference to User ID (foreign key).
     * Each user has exactly one wallet (1:1 relationship).
     */
    private Long userId;

    /**
     * Available balance (can be withdrawn).
     * Must be >= 0.
     */
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Pending balance (cashback pending confirmation).
     * Moved to balance when order is confirmed.
     * Must be >= 0.
     */
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    /**
     * Locked balance (locked for withdrawal processing).
     * Deducted when payout is completed.
     * Must be >= 0.
     */
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    /**
     * Total cashback earned (historical sum).
     * Only increases, never decreases.
     * Must be >= 0.
     */
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    /**
     * Total amount withdrawn (historical sum).
     * Only increases, never decreases.
     * Must be >= 0.
     */
    @Builder.Default
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    /**
     * Timestamp when entity was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when entity was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Add amount to pending balance.
     * Used when cashback is created but not yet confirmed.
     *
     * @param amount Amount to add (must be positive)
     * @throws IllegalArgumentException if amount is negative
     */
    public void addPendingBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.pendingBalance = MoneyUtils.add(this.pendingBalance, amount);
    }

    /**
     * Confirm pending balance and move to available balance.
     * Used when order status changes to APPROVED or PAID.
     *
     * Flow: pendingBalance → balance + totalEarned
     *
     * @param amount Amount to confirm (must be positive and <= pendingBalance)
     * @throws IllegalArgumentException if amount is invalid
     * @throws InsufficientBalanceException if insufficient pending balance
     */
    public void confirmPendingBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.pendingBalance)) {
            throw InsufficientBalanceException.of(this.pendingBalance, amount);
        }
        this.pendingBalance = MoneyUtils.subtract(this.pendingBalance, amount);
        this.balance = MoneyUtils.add(this.balance, amount);
        this.totalEarned = MoneyUtils.add(this.totalEarned, amount);
    }

    /**
     * Lock balance for withdrawal.
     * Used when user requests payout.
     *
     * Flow: balance → lockedBalance
     *
     * @param amount Amount to lock (must be positive and <= balance)
     * @throws IllegalArgumentException if amount is invalid
     * @throws InsufficientBalanceException if insufficient available balance
     */
    public void lockBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.balance)) {
            throw InsufficientBalanceException.of(this.balance, amount);
        }
        this.balance = MoneyUtils.subtract(this.balance, amount);
        this.lockedBalance = MoneyUtils.add(this.lockedBalance, amount);
    }

    /**
     * Unlock balance (cancel withdrawal).
     * Used when payout request is rejected or cancelled.
     *
     * Flow: lockedBalance → balance
     *
     * @param amount Amount to unlock (must be positive and <= lockedBalance)
     * @throws IllegalArgumentException if amount is invalid or insufficient locked balance
     */
    public void unlockBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.lockedBalance)) {
            throw new IllegalArgumentException(
                String.format("Insufficient locked balance. Have: %s, Required: %s",
                    this.lockedBalance, amount)
            );
        }
        this.lockedBalance = MoneyUtils.subtract(this.lockedBalance, amount);
        this.balance = MoneyUtils.add(this.balance, amount);
    }

    /**
     * Deduct locked balance (complete withdrawal).
     * Used when payout is successfully paid.
     *
     * Flow: lockedBalance → (removed) + totalWithdrawn
     *
     * @param amount Amount to deduct (must be positive and <= lockedBalance)
     * @throws IllegalArgumentException if amount is invalid or insufficient locked balance
     */
    public void deductLockedBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.lockedBalance)) {
            throw new IllegalArgumentException(
                String.format("Insufficient locked balance. Have: %s, Required: %s",
                    this.lockedBalance, amount)
            );
        }
        this.lockedBalance = MoneyUtils.subtract(this.lockedBalance, amount);
        this.totalWithdrawn = MoneyUtils.add(this.totalWithdrawn, amount);
    }

    /**
     * Add bonus/adjustment directly to available balance.
     * Used for manual adjustments, bonuses, or rewards.
     *
     * @param amount Amount to add (must be positive)
     * @throws IllegalArgumentException if amount is negative
     */
    public void addBonus(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = MoneyUtils.add(this.balance, amount);
    }

    /**
     * Deduct amount from available balance (admin adjustment).
     * Used for manual corrections or penalties.
     *
     * @param amount Amount to deduct (must be positive and <= balance)
     * @throws IllegalArgumentException if amount is invalid
     * @throws InsufficientBalanceException if insufficient balance
     */
    public void deductBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.balance)) {
            throw InsufficientBalanceException.of(this.balance, amount);
        }
        this.balance = MoneyUtils.subtract(this.balance, amount);
    }

    /**
     * Cancel pending cashback (order cancelled/rejected).
     *
     * @param amount Amount to cancel
     * @throws IllegalArgumentException if amount is invalid or insufficient pending balance
     */
    public void cancelPendingBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (MoneyUtils.isGreaterThan(amount, this.pendingBalance)) {
            throw new IllegalArgumentException(
                String.format("Insufficient pending balance. Have: %s, Required: %s",
                    this.pendingBalance, amount)
            );
        }
        this.pendingBalance = MoneyUtils.subtract(this.pendingBalance, amount);
    }

    // ===== Query Methods =====

    /**
     * Get total balance (available + pending + locked).
     *
     * @return Total balance across all buckets
     */
    public BigDecimal getTotalBalance() {
        return MoneyUtils.add(
            MoneyUtils.add(this.balance, this.pendingBalance),
            this.lockedBalance
        );
    }

    /**
     * Check if user has sufficient available balance for withdrawal.
     *
     * @param amount Required amount
     * @return true if available balance >= amount
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return MoneyUtils.isGreaterThanOrEqual(this.balance, amount);
    }

    /**
     * Check if wallet is completely empty (all balances are zero).
     *
     * @return true if total balance is zero
     */
    public boolean isEmpty() {
        return MoneyUtils.isZero(getTotalBalance());
    }

    /**
     * Check if there is any pending balance.
     *
     * @return true if pending balance > 0
     */
    public boolean hasPendingBalance() {
        return MoneyUtils.isPositive(this.pendingBalance);
    }

    /**
     * Check if there is any locked balance.
     *
     * @return true if locked balance > 0
     */
    public boolean hasLockedBalance() {
        return MoneyUtils.isPositive(this.lockedBalance);
    }

    /**
     * Check if this is a new wallet (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    // ===== Validation =====

    /**
     * Validate that amount is positive.
     *
     * @param amount Amount to validate
     * @throws IllegalArgumentException if amount is null or negative
     */
    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (MoneyUtils.isNegative(amount) || MoneyUtils.isZero(amount)) {
            throw new IllegalArgumentException("Amount must be positive. Got: " + amount);
        }
    }

    /**
     * Validate wallet state.
     * Ensures all invariants are satisfied.
     *
     * @throws IllegalStateException if wallet is in invalid state
     */
    public void validate() {
        if (this.userId == null) {
            throw new IllegalStateException("User ID is required");
        }
        if (MoneyUtils.isNegative(this.balance)) {
            throw new IllegalStateException("Balance cannot be negative");
        }
        if (MoneyUtils.isNegative(this.pendingBalance)) {
            throw new IllegalStateException("Pending balance cannot be negative");
        }
        if (MoneyUtils.isNegative(this.lockedBalance)) {
            throw new IllegalStateException("Locked balance cannot be negative");
        }
        if (MoneyUtils.isNegative(this.totalEarned)) {
            throw new IllegalStateException("Total earned cannot be negative");
        }
        if (MoneyUtils.isNegative(this.totalWithdrawn)) {
            throw new IllegalStateException("Total withdrawn cannot be negative");
        }
    }

    /**
     * Ensure all BigDecimal amounts have proper scale.
     * Called before persistence.
     */
    public void scaleBalances() {
        this.balance = MoneyUtils.scale(this.balance);
        this.pendingBalance = MoneyUtils.scale(this.pendingBalance);
        this.lockedBalance = MoneyUtils.scale(this.lockedBalance);
        this.totalEarned = MoneyUtils.scale(this.totalEarned);
        this.totalWithdrawn = MoneyUtils.scale(this.totalWithdrawn);
    }
}
