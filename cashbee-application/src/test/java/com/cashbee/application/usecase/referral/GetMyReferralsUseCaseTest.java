package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.GetMyReferralsResponse;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GetMyReferralsUseCase.
 * Tests the functionality of getting list of users referred by current user.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class GetMyReferralsUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetMyReferralsUseCase useCase;

    private User currentUser;
    private User referredUser1;
    private User referredUser2;

    @BeforeEach
    void setUp() {
        // Current user (referrer) - the one who shared their code
        currentUser = User.builder()
                .id(1L)
                .keycloakId("current-user-keycloak-id")
                .username("current_user")
                .email("current@example.com")
                .fullName("Current User")
                .referralCode("MYCODE01")
                .userLevel(UserLevel.NORMAL)
                .status(UserStatus.ACTIVE)
                .build();

        // User who was referred and activated (reached 3 orders, within 3 months)
        referredUser1 = User.builder()
                .id(2L)
                .keycloakId("referred-1-keycloak-id")
                .username("referred_user_1")
                .email("referred1@example.com")
                .fullName("Nguyen Van A")
                .referredBy("MYCODE01")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(5)
                .referralActivatedAt(LocalDateTime.now().minusDays(30)) // Activated 30 days ago
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(45))
                .build();

        // User who was referred but not yet activated (less than 3 orders)
        referredUser2 = User.builder()
                .id(3L)
                .keycloakId("referred-2-keycloak-id")
                .username("referred_user_2")
                .email("referred2@gmail.com")
                .fullName("Tran Thi B")
                .referredBy("MYCODE01")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(1)
                .referralActivatedAt(null) // Not yet activated
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    @Nested
    @DisplayName("Success scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return list of referred users with correct data")
        void shouldReturnListOfReferredUsers() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1, referredUser2));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            assertThat(response.getMyReferralCode()).isEqualTo("MYCODE01");
            assertThat(response.getTotalReferrals()).isEqualTo(2);
            assertThat(response.getReferrals()).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when user has no referrals")
        void shouldReturnEmptyListWhenNoReferrals() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(Collections.emptyList());

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            assertThat(response.getMyReferralCode()).isEqualTo("MYCODE01");
            assertThat(response.getTotalReferrals()).isEqualTo(0);
            assertThat(response.getActiveReferrals()).isEqualTo(0);
            assertThat(response.getReferrals()).isEmpty();
        }

        @Test
        @DisplayName("Should count active referrals correctly (within 3-month period)")
        void shouldCountActiveReferralsCorrectly() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1, referredUser2));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            // referredUser1 is active (activated 30 days ago, within 3 months)
            // referredUser2 is not activated yet
            assertThat(response.getTotalReferrals()).isEqualTo(2);
            assertThat(response.getActiveReferrals()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Privacy - Name masking")
    class PrivacyNameMasking {

        @Test
        @DisplayName("Should mask referred user's name for privacy")
        void shouldMaskReferredUserName() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getName()).isNotEqualTo("Nguyen Van A");
            assertThat(userInfo.getName()).contains("*");
            // Should show first 2 chars and mask the rest: "Ng******"
            assertThat(userInfo.getName()).startsWith("Ng");
        }

        @Test
        @DisplayName("Should mask referred user's email for privacy")
        void shouldMaskReferredUserEmail() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser2));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getEmail()).isNotEqualTo("referred2@gmail.com");
            assertThat(userInfo.getEmail()).contains("*");
            assertThat(userInfo.getEmail()).contains("@");
            // Should show first 2 chars, mask middle, keep domain: "re***@gmail.com"
        }
    }

    @Nested
    @DisplayName("Referral status details")
    class ReferralStatusDetails {

        @Test
        @DisplayName("Should include referral activation status")
        void shouldIncludeReferralActivationStatus() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1, referredUser2));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            // Find activated user (referredUser1)
            GetMyReferralsResponse.ReferredUserInfo activatedUser = response.getReferrals().stream()
                    .filter(GetMyReferralsResponse.ReferredUserInfo::isReferralActivated)
                    .findFirst()
                    .orElse(null);

            assertThat(activatedUser).isNotNull();
            assertThat(activatedUser.getReferralActivatedAt()).isNotNull();
            assertThat(activatedUser.getReferralExpiresAt()).isNotNull();

            // Find non-activated user (referredUser2)
            GetMyReferralsResponse.ReferredUserInfo nonActivatedUser = response.getReferrals().stream()
                    .filter(u -> !u.isReferralActivated())
                    .findFirst()
                    .orElse(null);

            assertThat(nonActivatedUser).isNotNull();
            assertThat(nonActivatedUser.getReferralActivatedAt()).isNull();
            assertThat(nonActivatedUser.getReferralExpiresAt()).isNull();
        }

        @Test
        @DisplayName("Should include completed orders count")
        void shouldIncludeCompletedOrdersCount() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getCompletedOrders()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should include user status")
        void shouldIncludeUserStatus() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should indicate if within commission period")
        void shouldIndicateWithinCommissionPeriod() {
            // Given
            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(referredUser1));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            // referredUser1 was activated 30 days ago, so still within 3-month period
            assertThat(userInfo.isWithinCommissionPeriod()).isTrue();
        }
    }

    @Nested
    @DisplayName("Expired referrals")
    class ExpiredReferrals {

        @Test
        @DisplayName("Should mark expired referral as not within commission period")
        void shouldMarkExpiredReferralCorrectly() {
            // Given
            User expiredReferral = User.builder()
                    .id(4L)
                    .keycloakId("expired-keycloak-id")
                    .username("expired_user")
                    .email("expired@example.com")
                    .fullName("Expired User")
                    .referredBy("MYCODE01")
                    .userLevel(UserLevel.NORMAL)
                    .totalCompletedOrders(10)
                    .referralActivatedAt(LocalDateTime.now().minusMonths(4)) // Activated 4 months ago (expired)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusMonths(5))
                    .build();

            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(expiredReferral));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            assertThat(response.getActiveReferrals()).isEqualTo(0); // Expired, not active
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.isReferralActivated()).isTrue();
            assertThat(userInfo.isWithinCommissionPeriod()).isFalse(); // Expired
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByKeycloakId("unknown-keycloak-id"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.execute("unknown-keycloak-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle user with null fullName gracefully")
        void shouldHandleNullFullName() {
            // Given
            User userWithNullName = User.builder()
                    .id(5L)
                    .keycloakId("null-name-keycloak-id")
                    .username("user_no_name")
                    .email("noname@example.com")
                    .fullName(null) // No full name
                    .referredBy("MYCODE01")
                    .userLevel(UserLevel.NORMAL)
                    .totalCompletedOrders(0)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(userWithNullName));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            // Should use username as fallback
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getName()).isNotNull();
            assertThat(userInfo.getName()).contains("*");
        }

        @Test
        @DisplayName("Should handle user with null completedOrders")
        void shouldHandleNullCompletedOrders() {
            // Given
            User userWithNullOrders = User.builder()
                    .id(6L)
                    .keycloakId("null-orders-keycloak-id")
                    .username("user_null_orders")
                    .email("nullorders@example.com")
                    .fullName("Null Orders User")
                    .referredBy("MYCODE01")
                    .userLevel(UserLevel.NORMAL)
                    .totalCompletedOrders(null) // Null orders
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByKeycloakId("current-user-keycloak-id"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findByReferredBy("MYCODE01"))
                    .thenReturn(List.of(userWithNullOrders));

            // When
            GetMyReferralsResponse response = useCase.execute("current-user-keycloak-id");

            // Then
            GetMyReferralsResponse.ReferredUserInfo userInfo = response.getReferrals().get(0);
            assertThat(userInfo.getCompletedOrders()).isEqualTo(0);
        }
    }
}
