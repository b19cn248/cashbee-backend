package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ReferralStatsResponse;
import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetReferralStatsUseCase.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class GetReferralStatsUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReferralRewardRepository referralRewardRepository;

    @Mock
    private ReferrerCommissionRepository referrerCommissionRepository;

    @InjectMocks
    private GetReferralStatsUseCase useCase;

    private User user;
    private User referrer;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .keycloakId("user-keycloak-id")
                .username("test_user")
                .email("test@example.com")
                .fullName("Test User")
                .referralCode("MYCODE01")
                .referredBy("REFCODE1")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(5)
                .referralActivatedAt(LocalDateTime.now().minusDays(10))
                .status(UserStatus.ACTIVE)
                .build();

        referrer = User.builder()
                .id(2L)
                .keycloakId("referrer-keycloak-id")
                .username("referrer_user")
                .email("referrer@example.com")
                .fullName("Referrer Name")
                .referralCode("REFCODE1")
                .userLevel(UserLevel.VIP)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Basic stats")
    class BasicStats {

        @Test
        @DisplayName("Should return user's basic referral info")
        void shouldReturnBasicInfo() {
            // Given
            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            assertThat(response.getMyReferralCode()).isEqualTo("MYCODE01");
            assertThat(response.getReferredByCode()).isEqualTo("REFCODE1");
            assertThat(response.getCurrentTier()).isEqualTo("NORMAL");
            assertThat(response.getTotalCompletedOrders()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should mask referrer name for privacy")
        void shouldMaskReferrerName() {
            // Given
            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            assertThat(response.getReferredByName()).isNotEqualTo("Referrer Name");
            assertThat(response.getReferredByName()).contains("*");
        }
    }

    @Nested
    @DisplayName("Milestone progress")
    class MilestoneProgress {

        @Test
        @DisplayName("Should calculate next milestone correctly")
        void shouldCalculateNextMilestone() {
            // Given
            user.setTotalCompletedOrders(5);

            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            // Next milestone after 5 orders is 10
            assertThat(response.getNextMilestone()).isEqualTo(10);
            assertThat(response.getOrdersToNextMilestone()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should include milestone progress list")
        void shouldIncludeMilestoneProgress() {
            // Given
            ReferralReward reward3 = ReferralReward.builder()
                    .id(1L)
                    .userId(1L)
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(3)
                    .amount(new BigDecimal("10000"))
                    .status(ReferralRewardStatus.GRANTED)
                    .grantedAt(LocalDateTime.now().minusDays(5))
                    .build();

            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(List.of(reward3));
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            assertThat(response.getMilestones()).isNotEmpty();
            assertThat(response.getMilestones()).anyMatch(m -> m.getMilestone() == 3 && m.isAchieved());
        }
    }

    @Nested
    @DisplayName("Referral earnings (as referrer)")
    class ReferralEarnings {

        @Test
        @DisplayName("Should return commission totals")
        void shouldReturnCommissionTotals() {
            // Given
            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(
                            User.builder().id(3L).username("referee1").build(),
                            User.builder().id(4L).username("referee2").build()
                    ));
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(new BigDecimal("50000"));
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(new BigDecimal("20000"));
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(new BigDecimal("15000"));
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(new BigDecimal("15000"));

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            assertThat(response.getTotalReferrals()).isEqualTo(2);
            assertThat(response.getTotalCommissionEarned()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(response.getPendingCommission()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(response.getConfirmedCommission()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(response.getPaidCommission()).isEqualByComparingTo(new BigDecimal("15000"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByKeycloakId("unknown"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.execute("unknown"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Users without referrer")
    class UsersWithoutReferrer {

        @Test
        @DisplayName("Should handle user with no referrer")
        void shouldHandleUserWithNoReferrer() {
            // Given
            user.setReferredBy(null);

            when(userRepository.findByKeycloakId("user-keycloak-id"))
                    .thenReturn(Optional.of(user));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());
            when(referralRewardRepository.findByUserId(1L))
                    .thenReturn(Collections.emptyList());
            when(referrerCommissionRepository.sumCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPendingCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumConfirmedCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);
            when(referrerCommissionRepository.sumPaidCommissionByReferrerId(1L))
                    .thenReturn(BigDecimal.ZERO);

            // When
            ReferralStatsResponse response = useCase.execute("user-keycloak-id");

            // Then
            assertThat(response.getReferredByCode()).isNull();
            assertThat(response.getReferredByName()).isNull();
            assertThat(response.getMyReferralCode()).isEqualTo("MYCODE01");
        }
    }
}
