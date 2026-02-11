package com.cashbee.domain.model;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.enums.UserLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReferralReward domain model.
 *
 * @author CashBee Team
 */
@DisplayName("ReferralReward Domain Model Tests")
class ReferralRewardTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create milestone bonus reward")
        void shouldCreateMilestoneBonus() {
            // Given
            Long userId = 1L;
            Long referrerId = 2L;
            Integer milestone = 3;
            BigDecimal amount = new BigDecimal("10000");

            // When
            ReferralReward reward = ReferralReward.createMilestoneBonus(
                    userId, referrerId, milestone, amount
            );

            // Then
            assertNotNull(reward);
            assertEquals(userId, reward.getUserId());
            assertEquals(referrerId, reward.getReferrerId());
            assertEquals(ReferralRewardType.MILESTONE_BONUS, reward.getRewardType());
            assertEquals(milestone, reward.getMilestone());
            assertEquals(amount, reward.getAmount());
            assertEquals(ReferralRewardStatus.GRANTED, reward.getStatus());
            assertNotNull(reward.getGrantedAt());
            assertNotNull(reward.getCreatedAt());
            assertNull(reward.getNewTier());
        }

        @Test
        @DisplayName("Should create tier upgrade reward")
        void shouldCreateTierUpgrade() {
            // Given
            Long userId = 1L;
            Long referrerId = 2L;
            Integer milestone = 40;
            UserLevel newTier = UserLevel.VIP;

            // When
            ReferralReward reward = ReferralReward.createTierUpgrade(
                    userId, referrerId, milestone, newTier
            );

            // Then
            assertNotNull(reward);
            assertEquals(userId, reward.getUserId());
            assertEquals(referrerId, reward.getReferrerId());
            assertEquals(ReferralRewardType.TIER_UPGRADE, reward.getRewardType());
            assertEquals(milestone, reward.getMilestone());
            assertEquals(newTier, reward.getNewTier());
            assertEquals(BigDecimal.ZERO, reward.getAmount());
            assertEquals(ReferralRewardStatus.GRANTED, reward.getStatus());
        }
    }

    @Nested
    @DisplayName("Business Logic")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should identify milestone bonus type")
        void shouldIdentifyMilestoneBonus() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .build();

            // Then
            assertTrue(reward.isMilestoneBonus());
            assertFalse(reward.isTierUpgrade());
        }

        @Test
        @DisplayName("Should identify tier upgrade type")
        void shouldIdentifyTierUpgrade() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .rewardType(ReferralRewardType.TIER_UPGRADE)
                    .build();

            // Then
            assertFalse(reward.isMilestoneBonus());
            assertTrue(reward.isTierUpgrade());
        }

        @Test
        @DisplayName("Should grant reward")
        void shouldGrantReward() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .status(ReferralRewardStatus.PENDING)
                    .build();

            // When
            reward.grant();

            // Then
            assertEquals(ReferralRewardStatus.GRANTED, reward.getStatus());
            assertNotNull(reward.getGrantedAt());
        }

        @Test
        @DisplayName("Should check if new reward")
        void shouldCheckIfNewReward() {
            // Given
            ReferralReward newReward = ReferralReward.builder().build();
            ReferralReward existingReward = ReferralReward.builder().id(1L).build();

            // Then
            assertTrue(newReward.isNew());
            assertFalse(existingReward.isNew());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid milestone bonus")
        void shouldPassValidationForValidMilestoneBonus() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .userId(1L)
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(3)
                    .amount(new BigDecimal("10000"))
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then - should not throw
            assertDoesNotThrow(reward::validate);
        }

        @Test
        @DisplayName("Should pass validation for valid tier upgrade")
        void shouldPassValidationForValidTierUpgrade() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .userId(1L)
                    .rewardType(ReferralRewardType.TIER_UPGRADE)
                    .milestone(40)
                    .newTier(UserLevel.VIP)
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then - should not throw
            assertDoesNotThrow(reward::validate);
        }

        @Test
        @DisplayName("Should fail validation when userId is null")
        void shouldFailValidationWhenUserIdNull() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(3)
                    .amount(new BigDecimal("10000"))
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    reward::validate
            );
            assertEquals("User ID is required", ex.getMessage());
        }

        @Test
        @DisplayName("Should fail validation for invalid milestone")
        void shouldFailValidationForInvalidMilestone() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .userId(1L)
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(5) // Invalid milestone
                    .amount(new BigDecimal("10000"))
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    reward::validate
            );
            assertTrue(ex.getMessage().contains("Invalid milestone"));
        }

        @Test
        @DisplayName("Should fail validation when amount missing for bonus")
        void shouldFailValidationWhenAmountMissingForBonus() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .userId(1L)
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(3)
                    .amount(BigDecimal.ZERO)
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    reward::validate
            );
            assertEquals("Amount is required for MILESTONE_BONUS", ex.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when newTier missing for upgrade")
        void shouldFailValidationWhenNewTierMissingForUpgrade() {
            // Given
            ReferralReward reward = ReferralReward.builder()
                    .userId(1L)
                    .rewardType(ReferralRewardType.TIER_UPGRADE)
                    .milestone(40)
                    .status(ReferralRewardStatus.GRANTED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    reward::validate
            );
            assertEquals("New tier is required for TIER_UPGRADE", ex.getMessage());
        }
    }
}
