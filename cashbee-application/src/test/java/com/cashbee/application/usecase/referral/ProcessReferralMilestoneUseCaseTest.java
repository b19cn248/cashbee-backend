package com.cashbee.application.usecase.referral;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessReferralMilestoneUseCase.
 * Tests milestone achievements and reward granting.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class ProcessReferralMilestoneUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReferralRewardRepository referralRewardRepository;

    @InjectMocks
    private ProcessReferralMilestoneUseCase useCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<ReferralReward> rewardCaptor;

    private User referee;
    private User referrer;

    @BeforeEach
    void setUp() {
        referee = User.builder()
                .id(1L)
                .keycloakId("referee-keycloak-id")
                .username("referee_user")
                .email("referee@example.com")
                .referralCode("REFEREE1")
                .referredBy("REFCODE1")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(2) // Will become 3 after increment
                .status(UserStatus.ACTIVE)
                .build();

        referrer = User.builder()
                .id(2L)
                .keycloakId("referrer-keycloak-id")
                .username("referrer_user")
                .email("referrer@example.com")
                .referralCode("REFCODE1")
                .userLevel(UserLevel.VIP)
                .totalCompletedOrders(50)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Milestone 3 - First activation")
    class Milestone3 {

        @Test
        @DisplayName("Should grant 10,000 VND bonus at 3 orders")
        void shouldGrantBonusAt3Orders() {
            // Given
            referee.setTotalCompletedOrders(2); // Will be 3 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 3)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.MILESTONE_BONUS);
            assertThat(reward.getMilestone()).isEqualTo(3);
            assertThat(reward.getAmount()).isEqualTo(new BigDecimal("10000"));
            assertThat(reward.getUserId()).isEqualTo(1L);
            assertThat(reward.getReferrerId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should activate referral at 3 orders")
        void shouldActivateReferralAt3Orders() {
            // Given
            referee.setTotalCompletedOrders(2);
            assertThat(referee.isReferralActivated()).isFalse();

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 3)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(3);
            assertThat(savedUser.isReferralActivated()).isTrue();
        }

        @Test
        @DisplayName("Should not grant duplicate reward for milestone 3")
        void shouldNotGrantDuplicateRewardForMilestone3() {
            // Given
            referee.setTotalCompletedOrders(2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 3)).thenReturn(true); // Already granted
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Milestone 10 - Second bonus")
    class Milestone10 {

        @Test
        @DisplayName("Should grant 20,000 VND bonus at 10 orders")
        void shouldGrantBonusAt10Orders() {
            // Given
            referee.setTotalCompletedOrders(9); // Will be 10 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 10)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.MILESTONE_BONUS);
            assertThat(reward.getMilestone()).isEqualTo(10);
            assertThat(reward.getAmount()).isEqualTo(new BigDecimal("20000"));
        }
    }

    @Nested
    @DisplayName("Milestone 40 - VIP tier upgrade")
    class Milestone40 {

        @Test
        @DisplayName("Should upgrade to VIP tier at 40 orders")
        void shouldUpgradeToVipAt40Orders() {
            // Given
            referee.setTotalCompletedOrders(39); // Will be 40 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 40)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.TIER_UPGRADE);
            assertThat(reward.getMilestone()).isEqualTo(40);
            assertThat(reward.getNewTier()).isEqualTo(UserLevel.VIP);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.VIP);
        }
    }

    @Nested
    @DisplayName("Milestone 150 - SUPER tier upgrade")
    class Milestone150 {

        @Test
        @DisplayName("Should upgrade to SUPER tier at 150 orders")
        void shouldUpgradeToSuperAt150Orders() {
            // Given
            referee.setUserLevel(UserLevel.VIP);
            referee.setTotalCompletedOrders(149); // Will be 150 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 150)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.TIER_UPGRADE);
            assertThat(reward.getMilestone()).isEqualTo(150);
            assertThat(reward.getNewTier()).isEqualTo(UserLevel.SUPER);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.SUPER);
        }
    }

    @Nested
    @DisplayName("No referrer scenarios")
    class NoReferrer {

        @Test
        @DisplayName("Should still increment orders for non-referred user")
        void shouldIncrementOrdersForNonReferredUser() {
            // Given
            referee.setReferredBy(null); // No referrer
            referee.setTotalCompletedOrders(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(6);
            verify(referralRewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should still grant tier upgrades for non-referred user at milestone")
        void shouldGrantTierUpgradeForNonReferredUser() {
            // Given
            referee.setReferredBy(null); // No referrer
            referee.setTotalCompletedOrders(39); // Will be 40 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 40)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.VIP);

            // Reward should be created but without referrerId
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();
            assertThat(reward.getReferrerId()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should do nothing if user not found")
        void shouldDoNothingIfUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            useCase.execute(999L);

            // Then
            verify(userRepository, never()).save(any());
            verify(referralRewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null totalCompletedOrders")
        void shouldHandleNullTotalCompletedOrders() {
            // Given
            referee.setTotalCompletedOrders(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(1);
        }
    }
}
