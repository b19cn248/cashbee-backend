package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ValidateReferralCodeResponse;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidateReferralCodeUseCase.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class ValidateReferralCodeUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ValidateReferralCodeUseCase useCase;

    private User referrer;

    @BeforeEach
    void setUp() {
        referrer = User.builder()
                .id(1L)
                .keycloakId("referrer-keycloak-id")
                .username("referrer_user")
                .email("referrer@example.com")
                .fullName("Nguyen Van A")
                .referralCode("REFCODE1")
                .userLevel(UserLevel.VIP)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Valid referral codes")
    class ValidReferralCodes {

        @Test
        @DisplayName("Should return valid response for existing active referral code")
        void shouldReturnValidForExistingCode() {
            // Given
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When
            ValidateReferralCodeResponse response = useCase.execute("REFCODE1");

            // Then
            assertThat(response.isValid()).isTrue();
            assertThat(response.getReferralCode()).isEqualTo("REFCODE1");
            assertThat(response.getReferrerName()).isNotNull();
            assertThat(response.getMessage()).contains("valid");
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            // Given
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When
            ValidateReferralCodeResponse response = useCase.execute("refcode1");

            // Then
            assertThat(response.isValid()).isTrue();
            verify(userRepository).findByReferralCode("REFCODE1");
        }

        @Test
        @DisplayName("Should mask referrer name for privacy")
        void shouldMaskReferrerName() {
            // Given
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When
            ValidateReferralCodeResponse response = useCase.execute("REFCODE1");

            // Then
            assertThat(response.getReferrerName()).isNotEqualTo("Nguyen Van A");
            assertThat(response.getReferrerName()).contains("*");
        }
    }

    @Nested
    @DisplayName("Invalid referral codes")
    class InvalidReferralCodes {

        @Test
        @DisplayName("Should return invalid for non-existent code")
        void shouldReturnInvalidForNonExistentCode() {
            // Given
            when(userRepository.findByReferralCode("INVALID1"))
                    .thenReturn(Optional.empty());

            // When
            ValidateReferralCodeResponse response = useCase.execute("INVALID1");

            // Then
            assertThat(response.isValid()).isFalse();
            assertThat(response.getReferralCode()).isEqualTo("INVALID1");
            assertThat(response.getReferrerName()).isNull();
            assertThat(response.getMessage()).contains("not found");
        }

        @Test
        @DisplayName("Should return invalid for banned user's code")
        void shouldReturnInvalidForBannedUserCode() {
            // Given
            referrer.setStatus(UserStatus.BANNED);
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When
            ValidateReferralCodeResponse response = useCase.execute("REFCODE1");

            // Then
            assertThat(response.isValid()).isFalse();
            assertThat(response.getMessage()).contains("inactive");
        }

        @Test
        @DisplayName("Should return invalid for suspended user's code")
        void shouldReturnInvalidForSuspendedUserCode() {
            // Given
            referrer.setStatus(UserStatus.SUSPENDED);
            when(userRepository.findByReferralCode("REFCODE1"))
                    .thenReturn(Optional.of(referrer));

            // When
            ValidateReferralCodeResponse response = useCase.execute("REFCODE1");

            // Then
            assertThat(response.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should handle null/empty code gracefully")
        void shouldHandleNullEmptyCode() {
            // When
            ValidateReferralCodeResponse responseNull = useCase.execute(null);
            ValidateReferralCodeResponse responseEmpty = useCase.execute("");
            ValidateReferralCodeResponse responseBlank = useCase.execute("   ");

            // Then
            assertThat(responseNull.isValid()).isFalse();
            assertThat(responseEmpty.isValid()).isFalse();
            assertThat(responseBlank.isValid()).isFalse();
        }
    }
}
