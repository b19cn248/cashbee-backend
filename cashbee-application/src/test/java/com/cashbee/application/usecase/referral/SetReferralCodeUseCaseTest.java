package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.SetReferralCodeCommand;
import com.cashbee.application.dto.referral.SetReferralCodeResponse;
import com.cashbee.common.exception.InvalidReferralCodeException;
import com.cashbee.common.exception.ReferralCodeAlreadySetException;
import com.cashbee.common.exception.SelfReferralException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SetReferralCodeUseCase.
 * Tests the business logic of setting a referral code for a user.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class SetReferralCodeUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SetReferralCodeUseCase useCase;

    private User referee;
    private User referrer;

    @BeforeEach
    void setUp() {
        // Setup referee (the user who is being referred)
        referee = User.builder()
                .id(1L)
                .keycloakId("referee-keycloak-id")
                .username("referee_user")
                .email("referee@example.com")
                .referralCode("REFEREE1")
                .referredBy(null) // Not yet referred
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(0)
                .status(UserStatus.ACTIVE)
                .build();

        // Setup referrer (the user who is referring)
        referrer = User.builder()
                .id(2L)
                .keycloakId("referrer-keycloak-id")
                .username("referrer_user")
                .email("referrer@example.com")
                .fullName("Referrer Full Name")
                .referralCode("REFCODE1")
                .referredBy(null)
                .userLevel(UserLevel.VIP)
                .totalCompletedOrders(50)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Success scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should set referral code successfully when valid")
        void shouldSetReferralCodeSuccessfully() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFCODE1")
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            SetReferralCodeResponse response = useCase.execute("referee-keycloak-id", command);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReferralCode()).isEqualTo("REFCODE1");
            assertThat(response.getReferrerId()).isEqualTo(2L);
            assertThat(response.getReferrerName()).isNotNull();

            verify(userRepository).findByKeycloakId("referee-keycloak-id");
            verify(userRepository).findByReferralCode("REFCODE1");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should mask referrer name for privacy")
        void shouldMaskReferrerNameForPrivacy() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFCODE1")
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            SetReferralCodeResponse response = useCase.execute("referee-keycloak-id", command);

            // Then
            // Name should be partially masked for privacy
            assertThat(response.getReferrerName()).isNotEqualTo("Referrer Full Name");
            assertThat(response.getReferrerName()).contains("*");
        }
    }

    @Nested
    @DisplayName("Error scenarios")
    class ErrorScenarios {

        @Test
        @DisplayName("Should throw exception when referral code not found")
        void shouldThrowExceptionWhenReferralCodeNotFound() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("INVALID1")
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("INVALID1"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.execute("referee-keycloak-id", command))
                    .isInstanceOf(InvalidReferralCodeException.class)
                    .hasMessageContaining("INVALID1");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user already has referral code")
        void shouldThrowExceptionWhenAlreadyHasReferralCode() {
            // Given
            referee.setReferredBy("EXISTING");
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFCODE1")
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));

            // When/Then
            assertThatThrownBy(() -> useCase.execute("referee-keycloak-id", command))
                    .isInstanceOf(ReferralCodeAlreadySetException.class);

            verify(userRepository, never()).findByReferralCode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to use own referral code")
        void shouldThrowExceptionWhenUsingSelfReferralCode() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFEREE1") // Same as referee's own code
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFEREE1"))
                    .thenReturn(Optional.of(referee)); // Returns the same user

            // When/Then
            assertThatThrownBy(() -> useCase.execute("referee-keycloak-id", command))
                    .isInstanceOf(SelfReferralException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFCODE1")
                    .build();

            when(userRepository.findByKeycloakId("unknown-keycloak-id"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.execute("unknown-keycloak-id", command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when referrer is inactive")
        void shouldThrowExceptionWhenReferrerIsInactive() {
            // Given
            referrer.setStatus(UserStatus.BANNED);
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("REFCODE1")
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When/Then
            assertThatThrownBy(() -> useCase.execute("referee-keycloak-id", command))
                    .isInstanceOf(InvalidReferralCodeException.class)
                    .hasMessageContaining("inactive");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Case insensitivity")
    class CaseInsensitivity {

        @Test
        @DisplayName("Should match referral code case-insensitively")
        void shouldMatchReferralCodeCaseInsensitively() {
            // Given
            SetReferralCodeCommand command = SetReferralCodeCommand.builder()
                    .referralCode("refcode1") // lowercase
                    .build();

            when(userRepository.findByKeycloakId("referee-keycloak-id"))
                    .thenReturn(Optional.of(referee));
            // Repository should be called with uppercase
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            SetReferralCodeResponse response = useCase.execute("referee-keycloak-id", command);

            // Then
            assertThat(response.isSuccess()).isTrue();
            verify(userRepository).findByReferralCode("REFCODE1");
        }
    }
}
