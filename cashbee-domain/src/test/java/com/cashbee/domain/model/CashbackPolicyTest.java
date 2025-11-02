package com.cashbee.domain.model;

import com.cashbee.domain.enums.UserLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CashbackPolicy domain model.
 *
 * Following TDD approach - tests are written BEFORE implementation.
 *
 * @author CashBee Team
 */
@DisplayName("CashbackPolicy Domain Model Tests")
class CashbackPolicyTest {

    @Test
    @DisplayName("Should create policy with default values")
    void shouldCreatePolicyWithDefaults() {
        // Given
        var policy = CashbackPolicy.builder()
            .policyName("Default Policy")
            .policyCode("DEFAULT_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .effectiveFrom(LocalDateTime.now())
            .build();

        // Then
        assertNotNull(policy);
        assertEquals("Default Policy", policy.getPolicyName());
        assertEquals("DEFAULT_001", policy.getPolicyCode());
        assertTrue(policy.getIsActive()); // Default should be true
        assertEquals(0, policy.getPriority()); // Default should be 0
    }

    @Test
    @DisplayName("Should activate policy")
    void shouldActivatePolicy() {
        // Given
        var policy = createInactivePolicy();

        // When
        policy.activate();

        // Then
        assertTrue(policy.isActive());
        assertTrue(policy.getIsActive());
    }

    @Test
    @DisplayName("Should deactivate policy")
    void shouldDeactivatePolicy() {
        // Given
        var policy = createActivePolicy();

        // When
        policy.deactivate();

        // Then
        assertFalse(policy.isActive());
        assertFalse(policy.getIsActive());
    }

    @Test
    @DisplayName("Should check if policy is effective now")
    void shouldCheckIfEffectiveNow() {
        // Given - Policy effective from yesterday, no end date
        var policy = CashbackPolicy.builder()
            .policyName("Test Policy")
            .policyCode("TEST_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .effectiveTo(null) // No end date
            .build();

        // Then
        assertTrue(policy.isEffectiveNow());
    }

    @Test
    @DisplayName("Should not be effective if not started yet")
    void shouldNotBeEffectiveIfNotStarted() {
        // Given - Policy starts tomorrow
        var policy = CashbackPolicy.builder()
            .policyName("Future Policy")
            .policyCode("FUTURE_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().plusDays(1))
            .build();

        // Then
        assertFalse(policy.isEffectiveNow());
    }

    @Test
    @DisplayName("Should not be effective if already expired")
    void shouldNotBeEffectiveIfExpired() {
        // Given - Policy ended yesterday
        var policy = CashbackPolicy.builder()
            .policyName("Expired Policy")
            .policyCode("EXPIRED_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(10))
            .effectiveTo(LocalDateTime.now().minusDays(1))
            .build();

        // Then
        assertFalse(policy.isEffectiveNow());
        assertTrue(policy.isExpired());
    }

    @Test
    @DisplayName("Should calculate cashback from commission amount")
    void shouldCalculateCashbackFromCommission() {
        // Given - Policy with 70% cashback rate
        var policy = createActivePolicy();
        var commissionAmount = new BigDecimal("100.00");

        // When
        var cashback = policy.calculateCashback(commissionAmount);

        // Then
        assertEquals(new BigDecimal("70.00"), cashback);
    }

    @Test
    @DisplayName("Should apply max cashback cap if exceeded")
    void shouldApplyMaxCashbackCap() {
        // Given - Policy with max cashback 50.00
        var policy = CashbackPolicy.builder()
            .policyName("Capped Policy")
            .policyCode("CAPPED_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .maxCashbackPerOrder(new BigDecimal("50.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .build();

        var commissionAmount = new BigDecimal("100.00"); // Would give 70.00 without cap

        // When
        var cashback = policy.calculateCashback(commissionAmount);

        // Then
        assertEquals(new BigDecimal("50.00"), cashback); // Capped at max
    }

    @Test
    @DisplayName("Should check if applicable to order value")
    void shouldCheckIfApplicableToOrderValue() {
        // Given - Policy with min order value 100
        var policy = CashbackPolicy.builder()
            .policyName("Min Order Policy")
            .policyCode("MIN_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .minOrderValue(new BigDecimal("100.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .build();

        // Then
        assertTrue(policy.isApplicableToOrderValue(new BigDecimal("100.00")));
        assertTrue(policy.isApplicableToOrderValue(new BigDecimal("150.00")));
        assertFalse(policy.isApplicableToOrderValue(new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Should check if applicable to platform")
    void shouldCheckIfApplicableToPlatform() {
        // Given - Policy specific to platform ID 1
        var policy = CashbackPolicy.builder()
            .policyName("Shopee Policy")
            .policyCode("SHOPEE_001")
            .platformId(1L)
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .build();

        // Then
        assertTrue(policy.isApplicableToPlatform(1L));
        assertFalse(policy.isApplicableToPlatform(2L));
    }

    @Test
    @DisplayName("Should be applicable to all platforms if platformId is null")
    void shouldBeApplicableToAllPlatformsIfNull() {
        // Given - Policy with null platformId (applies to all)
        var policy = CashbackPolicy.builder()
            .policyName("Universal Policy")
            .policyCode("UNIVERSAL_001")
            .platformId(null)
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .isActive(true)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .build();

        // Then
        assertTrue(policy.isApplicableToPlatform(1L));
        assertTrue(policy.isApplicableToPlatform(2L));
        assertTrue(policy.isApplicableToPlatform(999L));
    }

    @Test
    @DisplayName("Should validate policy data")
    void shouldValidatePolicyData() {
        // Given - Valid policy
        var policy = createActivePolicy();

        // Then - Should not throw exception
        assertDoesNotThrow(() -> policy.validate());
    }

    @Test
    @DisplayName("Should fail validation if policy name is blank")
    void shouldFailValidationIfNameBlank() {
        // Given
        var policy = CashbackPolicy.builder()
            .policyName("")
            .policyCode("TEST_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .effectiveFrom(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> policy.validate());
    }

    @Test
    @DisplayName("Should fail validation if cashback rate is negative")
    void shouldFailValidationIfRateNegative() {
        // Given
        var policy = CashbackPolicy.builder()
            .policyName("Test Policy")
            .policyCode("TEST_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("-10.00")) // Negative!
            .effectiveFrom(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> policy.validate());
    }

    @Test
    @DisplayName("Should fail validation if cashback rate exceeds 100%")
    void shouldFailValidationIfRateExceeds100() {
        // Given
        var policy = CashbackPolicy.builder()
            .policyName("Test Policy")
            .policyCode("TEST_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("150.00")) // >100%!
            .effectiveFrom(LocalDateTime.now())
            .build();

        // Then
        assertThrows(IllegalStateException.class, () -> policy.validate());
    }

    @Test
    @DisplayName("Should check if policy is for specific user level")
    void shouldCheckIfForUserLevel() {
        // Given
        var normalPolicy = CashbackPolicy.builder()
            .policyName("Normal Policy")
            .policyCode("NORMAL_001")
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .effectiveFrom(LocalDateTime.now())
            .build();

        var vipPolicy = CashbackPolicy.builder()
            .policyName("VIP Policy")
            .policyCode("VIP_001")
            .userLevel(UserLevel.VIP)
            .cashbackRate(new BigDecimal("80.00"))
            .effectiveFrom(LocalDateTime.now())
            .build();

        // Then
        assertTrue(normalPolicy.isForUserLevel(UserLevel.NORMAL));
        assertFalse(normalPolicy.isForUserLevel(UserLevel.VIP));

        assertTrue(vipPolicy.isForUserLevel(UserLevel.VIP));
        assertFalse(vipPolicy.isForUserLevel(UserLevel.NORMAL));
    }

    // ===== Helper Methods =====

    private CashbackPolicy createActivePolicy() {
        return CashbackPolicy.builder()
            .policyName("Test Active Policy")
            .policyCode("ACTIVE_001")
            .platformId(1L)
            .userLevel(UserLevel.NORMAL)
            .cashbackRate(new BigDecimal("70.00"))
            .minOrderValue(BigDecimal.ZERO)
            .maxCashbackPerOrder(null)
            .isActive(true)
            .priority(0)
            .effectiveFrom(LocalDateTime.now().minusDays(1))
            .effectiveTo(null)
            .build();
    }

    private CashbackPolicy createInactivePolicy() {
        var policy = createActivePolicy();
        policy.setIsActive(false);
        return policy;
    }
}
