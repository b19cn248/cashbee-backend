package com.cashbee.application.usecase.referral;

import com.cashbee.domain.enums.ReferrerCommissionStatus;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CalculateReferrerCommissionUseCase.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class CalculateReferrerCommissionUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReferrerCommissionRepository referrerCommissionRepository;

    @InjectMocks
    private CalculateReferrerCommissionUseCase useCase;

    @Captor
    private ArgumentCaptor<ReferrerCommission> commissionCaptor;

    private User referee;
    private User referrer;

    @BeforeEach
    void setUp() {
        // Referee with activated referral (3+ orders completed)
        LocalDateTime activatedAt = LocalDateTime.now().minusDays(30); // Activated 30 days ago
        referee = User.builder()
                .id(1L)
                .keycloakId("referee-keycloak-id")
                .username("referee_user")
                .email("referee@example.com")
                .referralCode("REFEREE1")
                .referredBy("REFCODE1")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(5)
                .referralActivatedAt(activatedAt)
                .referralExpiresAt(activatedAt.plusMonths(5)) // Expires 5 months from activation
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
    @DisplayName("Commission calculation")
    class CommissionCalculation {

        @Test
        @DisplayName("Should calculate 5% commission from original commission")
        void shouldCalculate5PercentCommission() {
            // Given: Original commission from Shopee is 100,000 VND
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 123L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referrerCommissionRepository.existsBySourceOrderId(orderId)).thenReturn(false);
            when(referrerCommissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository).save(commissionCaptor.capture());
            ReferrerCommission commission = commissionCaptor.getValue();

            // 5% of 100,000 = 5,000
            assertThat(commission.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(commission.getOriginalCommission()).isEqualByComparingTo(originalCommission);
            assertThat(commission.getCommissionRate()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(commission.getReferrerId()).isEqualTo(2L);
            assertThat(commission.getRefereeId()).isEqualTo(1L);
            assertThat(commission.getSourceOrderId()).isEqualTo(orderId);
            assertThat(commission.getStatus()).isEqualTo(ReferrerCommissionStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should set correct expiration date (3 months from activation)")
        void shouldSetCorrectExpirationDate() {
            // Given
            BigDecimal originalCommission = new BigDecimal("50000");
            Long orderId = 124L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referrerCommissionRepository.existsBySourceOrderId(orderId)).thenReturn(false);
            when(referrerCommissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository).save(commissionCaptor.capture());
            ReferrerCommission commission = commissionCaptor.getValue();

            // Expiration should be 3 months from referral activation
            assertThat(commission.getExpiresAt()).isEqualTo(referee.getReferralExpiresAt());
        }

        @Test
        @DisplayName("Should handle small commission amounts correctly")
        void shouldHandleSmallCommissionAmounts() {
            // Given: Original commission is 1,000 VND
            BigDecimal originalCommission = new BigDecimal("1000");
            Long orderId = 125L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referrerCommissionRepository.existsBySourceOrderId(orderId)).thenReturn(false);
            when(referrerCommissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository).save(commissionCaptor.capture());
            ReferrerCommission commission = commissionCaptor.getValue();

            // 5% of 1,000 = 50
            assertThat(commission.getCommissionAmount()).isEqualByComparingTo(new BigDecimal("50"));
        }
    }

    @Nested
    @DisplayName("No commission scenarios")
    class NoCommissionScenarios {

        @Test
        @DisplayName("Should not create commission if user has no referrer")
        void shouldNotCreateCommissionIfNoReferrer() {
            // Given
            referee.setReferredBy(null);
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 126L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create commission if referral not activated")
        void shouldNotCreateCommissionIfReferralNotActivated() {
            // Given
            referee.setReferralActivatedAt(null); // Not activated yet
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 127L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create commission if referral period expired")
        void shouldNotCreateCommissionIfReferralExpired() {
            // Given: Referral activated 6 months ago, expired 1 month ago (5 month commission period)
            LocalDateTime activatedAt = LocalDateTime.now().minusMonths(6);
            referee.setReferralActivatedAt(activatedAt);
            referee.setReferralExpiresAt(activatedAt.plusMonths(5)); // Expired 1 month ago
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 128L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create duplicate commission for same order")
        void shouldNotCreateDuplicateCommission() {
            // Given
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 129L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(referrerCommissionRepository.existsBySourceOrderId(orderId)).thenReturn(true); // Already exists

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create commission if referrer not found")
        void shouldNotCreateCommissionIfReferrerNotFound() {
            // Given
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 130L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.empty());

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create commission if original commission is zero or null")
        void shouldNotCreateCommissionIfZeroOrNull() {
            // Given
            Long orderId = 131L;

            // When - with null (returns early before querying user)
            useCase.execute(1L, orderId, null);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
            verify(userRepository, never()).findById(any());

            // When - with zero (also returns early)
            useCase.execute(1L, orderId, BigDecimal.ZERO);

            // Then
            verify(referrerCommissionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle user not found gracefully")
        void shouldHandleUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            useCase.execute(999L, 123L, new BigDecimal("100000"));

            // Then - no exception, no save
            verify(referrerCommissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not create commission if referrer is inactive")
        void shouldNotCreateCommissionIfReferrerIsInactive() {
            // Given
            referrer.setStatus(UserStatus.BANNED);
            BigDecimal originalCommission = new BigDecimal("100000");
            Long orderId = 132L;

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));

            // When
            useCase.execute(1L, orderId, originalCommission);

            // Then - should not create commission for inactive referrer
            verify(referrerCommissionRepository, never()).save(any());
        }
    }
}
