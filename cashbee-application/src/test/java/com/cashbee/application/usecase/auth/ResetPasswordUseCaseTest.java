package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ResetPasswordRequest;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordUseCase Tests (TDD)")
class ResetPasswordUseCaseTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private IdentityProviderPort identityProvider;

    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    private OtpVerification verifiedOtp;

    @BeforeEach
    void setUp() {
        verifiedOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .resetToken("550e8400-e29b-41d4-a716-446655440000")
            .createdAt(LocalDateTime.now().minusMinutes(2))
            .expiresAt(LocalDateTime.now().plusMinutes(3))
            .verified(true)
            .verifiedAt(LocalDateTime.now().minusMinutes(1))
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .build();
    }

    @Test
    @DisplayName("Should reset password successfully when token and password are valid")
    void execute_ResetsPassword_WhenTokenAndPasswordValid() {
        // Given
        when(otpRepository.findByEmailAndResetToken("user@example.com",
            "550e8400-e29b-41d4-a716-446655440000", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(verifiedOtp));

        ResetPasswordRequest request = new ResetPasswordRequest(
            "user@example.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NewPassword1"
        );

        // When
        resetPasswordUseCase.execute(request);

        // Then
        verify(identityProvider).resetUserPassword("kc-uuid-123", "NewPassword1");
        verify(otpRepository).delete(verifiedOtp);
    }

    @Test
    @DisplayName("Should throw BadRequestException with INVALID_RESET_TOKEN when token not found")
    void execute_ThrowsInvalidResetToken_WhenTokenNotFound() {
        // Given
        when(otpRepository.findByEmailAndResetToken("user@example.com",
            "bad-token", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest(
            "user@example.com",
            "bad-token",
            "NewPassword1"
        );

        // When & Then
        assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RESET_TOKEN));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
        verify(otpRepository, never()).delete(any(OtpVerification.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when password too short")
    void execute_ThrowsValidationError_WhenPasswordTooShort() {
        // Given
        when(otpRepository.findByEmailAndResetToken("user@example.com",
            "550e8400-e29b-41d4-a716-446655440000", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(verifiedOtp));

        ResetPasswordRequest request = new ResetPasswordRequest(
            "user@example.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "Ab1"  // too short
        );

        // When & Then
        assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
        verify(otpRepository, never()).delete(any(OtpVerification.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when password has no uppercase")
    void execute_ThrowsValidationError_WhenPasswordNoUppercase() {
        // Given
        when(otpRepository.findByEmailAndResetToken("user@example.com",
            "550e8400-e29b-41d4-a716-446655440000", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(verifiedOtp));

        ResetPasswordRequest request = new ResetPasswordRequest(
            "user@example.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "alllowercase1"  // no uppercase
        );

        // When & Then
        assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when password has no digit")
    void execute_ThrowsValidationError_WhenPasswordNoDigit() {
        // Given
        when(otpRepository.findByEmailAndResetToken("user@example.com",
            "550e8400-e29b-41d4-a716-446655440000", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(verifiedOtp));

        ResetPasswordRequest request = new ResetPasswordRequest(
            "user@example.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NoDigitPassword"  // no digit
        );

        // When & Then
        assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }
}
