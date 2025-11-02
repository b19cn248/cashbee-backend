package com.cashbee.domain.model;

import com.cashbee.common.exception.InsufficientBalanceException;
import com.cashbee.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for UserWallet Domain Model
 *
 * Testing Strategy: Red → Green → Refactor
 * - Write test first (Red)
 * - Write minimal code to pass (Green)
 * - Refactor for better quality (Refactor)
 *
 * @author CashBee Team
 */
@DisplayName("UserWallet Domain Model Tests")
class UserWalletTest {

    private UserWallet wallet;

    @BeforeEach
    void setUp() {
        // Given: A wallet with initial balance of 1000
        wallet = UserWallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("1000.00"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
    }

    // ============================================================
    // TEST 1: Lock Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Lock balance successfully when sufficient balance exists")
    void lockBalance_Success_WhenSufficientBalance() {
        // Given: Wallet has 1000 balance (set in setUp)
        BigDecimal amountToLock = new BigDecimal("300.00");

        // When: Lock 300 from balance
        wallet.lockBalance(amountToLock);

        // Then:
        // - Available balance should decrease by 300 (1000 - 300 = 700)
        // - Locked balance should increase by 300 (0 + 300 = 300)
        assertThat(wallet.getBalance())
                .as("Available balance should be 700 after locking 300")
                .isEqualTo(new BigDecimal("700.00"));

        assertThat(wallet.getLockedBalance())
                .as("Locked balance should be 300")
                .isEqualTo(new BigDecimal("300.00"));

        // Total balance should remain the same (1000)
        assertThat(wallet.getTotalBalance())
                .as("Total balance should still be 1000 (balance + locked)")
                .isEqualTo(new BigDecimal("1000.00"));
    }

    // ============================================================
    // TEST 2: Lock Balance - Insufficient Balance (Edge Case)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when locking more than available balance")
    void lockBalance_ThrowsException_WhenInsufficientBalance() {
        // Given: Wallet has only 1000 balance
        BigDecimal amountToLock = new BigDecimal("1500.00"); // Try to lock MORE than available

        // When & Then: Should throw InsufficientBalanceException
        assertThatThrownBy(() -> wallet.lockBalance(amountToLock))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("1000.00") // Current balance
                .hasMessageContaining("1500.00"); // Requested amount

        // Verify: Balance should NOT change after failed operation
        assertThat(wallet.getBalance())
                .as("Balance should remain unchanged after failed lock attempt")
                .isEqualTo(new BigDecimal("1000.00"));

        assertThat(wallet.getLockedBalance())
                .as("Locked balance should remain 0")
                .isEqualTo(BigDecimal.ZERO);
    }

    // ============================================================
    // TEST 3: Lock Balance - Negative Amount (Edge Case)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when locking negative amount")
    void lockBalance_ThrowsException_WhenNegativeAmount() {
        // Given: Negative amount
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // When & Then: Should throw ValidationException or IllegalArgumentException
        assertThatThrownBy(() -> wallet.lockBalance(negativeAmount))
                .isInstanceOf(RuntimeException.class) // Could be ValidationException or IllegalArgumentException
                .hasMessageContaining("positive");

        // Verify: No state change
        assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(wallet.getLockedBalance()).isEqualTo(BigDecimal.ZERO);
    }

    // ============================================================
    // TEST 4: Lock Balance - Zero Amount (Edge Case)
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when locking zero amount")
    void lockBalance_ThrowsException_WhenZeroAmount() {
        // Given: Zero amount
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When & Then: Should throw exception
        assertThatThrownBy(() -> wallet.lockBalance(zeroAmount))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");
    }

    // ============================================================
    // TEST 5: Lock Balance - Multiple Locks (Cumulative)
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Should accumulate locked balance from multiple lock operations")
    void lockBalance_AccumulatesLockedBalance_WhenMultipleLocks() {
        // Given: Wallet with 1000 balance

        // When: Lock 3 times
        wallet.lockBalance(new BigDecimal("200.00")); // Lock 200
        wallet.lockBalance(new BigDecimal("150.00")); // Lock 150
        wallet.lockBalance(new BigDecimal("100.00")); // Lock 100

        // Then:
        // - Total locked = 200 + 150 + 100 = 450
        // - Available balance = 1000 - 450 = 550
        assertThat(wallet.getLockedBalance())
                .as("Locked balance should be sum of all locks")
                .isEqualTo(new BigDecimal("450.00"));

        assertThat(wallet.getBalance())
                .as("Available balance should decrease by total locked amount")
                .isEqualTo(new BigDecimal("550.00"));

        assertThat(wallet.getTotalBalance())
                .as("Total balance should remain 1000")
                .isEqualTo(new BigDecimal("1000.00"));
    }

    // ============================================================
    // TEST 6: Unlock Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Unlock balance successfully returns locked funds to available balance")
    void unlockBalance_Success_WhenSufficientLockedBalance() {
        // Given: Lock 300 first
        wallet.lockBalance(new BigDecimal("300.00"));
        // Now: balance = 700, locked = 300

        // When: Unlock 100
        wallet.unlockBalance(new BigDecimal("100.00"));

        // Then:
        // - Locked balance = 300 - 100 = 200
        // - Available balance = 700 + 100 = 800
        assertThat(wallet.getLockedBalance())
                .as("Locked balance should decrease by unlocked amount")
                .isEqualTo(new BigDecimal("200.00"));

        assertThat(wallet.getBalance())
                .as("Available balance should increase by unlocked amount")
                .isEqualTo(new BigDecimal("800.00"));

        assertThat(wallet.getTotalBalance())
                .as("Total balance should remain unchanged")
                .isEqualTo(new BigDecimal("1000.00"));
    }

    // ============================================================
    // TEST 7: Unlock Balance - Insufficient Locked Balance
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when unlocking more than locked balance")
    void unlockBalance_ThrowsException_WhenInsufficientLockedBalance() {
        // Given: Only 300 locked
        wallet.lockBalance(new BigDecimal("300.00"));

        // When & Then: Try to unlock 500 (more than locked 300)
        assertThatThrownBy(() -> wallet.unlockBalance(new BigDecimal("500.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300.00")
                .hasMessageContaining("500.00");
    }

    // ============================================================
    // TEST 8: Deduct Locked Balance - Happy Path
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Deduct locked balance successfully (complete payout)")
    void deductLockedBalance_Success_WhenSufficientLockedBalance() {
        // Given: Lock 300
        wallet.lockBalance(new BigDecimal("300.00"));
        BigDecimal initialTotalWithdrawn = wallet.getTotalWithdrawn();

        // When: Deduct 300 (complete the payout)
        wallet.deductLockedBalance(new BigDecimal("300.00"));

        // Then:
        // - Locked balance = 0 (all deducted)
        // - Available balance = 700 (unchanged)
        // - Total withdrawn increased by 300
        assertThat(wallet.getLockedBalance())
                .as("Locked balance should be 0 after deduction")
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(wallet.getBalance())
                .as("Available balance should remain 700")
                .isEqualTo(new BigDecimal("700.00"));

        assertThat(wallet.getTotalWithdrawn())
                .as("Total withdrawn should increase by deducted amount")
                .isEqualTo(initialTotalWithdrawn.add(new BigDecimal("300.00")));

        assertThat(wallet.getTotalBalance())
                .as("Total balance should decrease by deducted amount")
                .isEqualTo(new BigDecimal("700.00")); // Only available balance remains
    }

    // ============================================================
    // TEST 9: Deduct Locked Balance - Insufficient Locked Balance
    // ============================================================

    @Test
    @DisplayName("🔴 RED TEST: Should throw exception when deducting more than locked balance")
    void deductLockedBalance_ThrowsException_WhenInsufficientLockedBalance() {
        // Given: Only 300 locked
        wallet.lockBalance(new BigDecimal("300.00"));

        // When & Then: Try to deduct 500 (more than locked 300)
        assertThatThrownBy(() -> wallet.deductLockedBalance(new BigDecimal("500.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300.00")
                .hasMessageContaining("500.00");
    }

    // ============================================================
    // TEST 10: Full Payout Workflow (Lock → Deduct)
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Complete payout workflow - Lock then Deduct")
    void fullPayoutWorkflow_LockThenDeduct_Success() {
        // Given: Wallet with 1000 balance
        BigDecimal payoutAmount = new BigDecimal("250.00");

        // When: Full payout workflow
        // Step 1: User requests payout → Lock balance
        wallet.lockBalance(payoutAmount);
        assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("750.00"));
        assertThat(wallet.getLockedBalance()).isEqualTo(new BigDecimal("250.00"));

        // Step 2: Admin approves and processes → Deduct locked balance
        wallet.deductLockedBalance(payoutAmount);

        // Then: Final state
        assertThat(wallet.getBalance())
                .as("Available balance should be 750")
                .isEqualTo(new BigDecimal("750.00"));

        assertThat(wallet.getLockedBalance())
                .as("Locked balance should be 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(wallet.getTotalWithdrawn())
                .as("Total withdrawn should be 250")
                .isEqualTo(new BigDecimal("250.00"));

        assertThat(wallet.getTotalBalance())
                .as("Total balance should be 750 (original 1000 - 250 withdrawn)")
                .isEqualTo(new BigDecimal("750.00"));
    }

    // ============================================================
    // TEST 11: Cancelled Payout Workflow (Lock → Unlock)
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Cancelled payout workflow - Lock then Unlock")
    void cancelledPayoutWorkflow_LockThenUnlock_Success() {
        // Given: Wallet with 1000 balance
        BigDecimal payoutAmount = new BigDecimal("250.00");

        // When: Payout requested then cancelled
        // Step 1: User requests payout → Lock balance
        wallet.lockBalance(payoutAmount);

        // Step 2: Admin rejects payout → Unlock balance
        wallet.unlockBalance(payoutAmount);

        // Then: Everything returns to initial state
        assertThat(wallet.getBalance())
                .as("Available balance should return to 1000")
                .isEqualTo(new BigDecimal("1000.00"));

        assertThat(wallet.getLockedBalance())
                .as("Locked balance should be 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(wallet.getTotalWithdrawn())
                .as("Total withdrawn should remain 0")
                .isEqualTo(BigDecimal.ZERO);
    }

    // ============================================================
    // TEST 12: Add Pending Balance
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Add pending balance successfully")
    void addPendingBalance_Success() {
        // Given: Initial wallet

        // When: Add pending balance (new cashback)
        wallet.addPendingBalance(new BigDecimal("50.00"));

        // Then:
        assertThat(wallet.getPendingBalance())
                .as("Pending balance should increase")
                .isEqualTo(new BigDecimal("50.00"));

        assertThat(wallet.getBalance())
                .as("Available balance should remain unchanged")
                .isEqualTo(new BigDecimal("1000.00"));
    }

    // ============================================================
    // TEST 13: Confirm Pending Balance
    // ============================================================

    @Test
    @DisplayName("✅ GREEN TEST: Confirm pending balance successfully")
    void confirmPendingBalance_Success() {
        // Given: Wallet with pending balance
        wallet.addPendingBalance(new BigDecimal("50.00"));
        BigDecimal initialTotalEarned = wallet.getTotalEarned();

        // When: Confirm pending balance
        wallet.confirmPendingBalance(new BigDecimal("50.00"));

        // Then:
        assertThat(wallet.getPendingBalance())
                .as("Pending balance should be 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(wallet.getBalance())
                .as("Available balance should increase by confirmed amount")
                .isEqualTo(new BigDecimal("1050.00"));

        assertThat(wallet.getTotalEarned())
                .as("Total earned should increase")
                .isEqualTo(initialTotalEarned.add(new BigDecimal("50.00")));
    }
}
