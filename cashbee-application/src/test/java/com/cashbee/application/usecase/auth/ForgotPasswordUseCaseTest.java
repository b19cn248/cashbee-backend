package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ForgotPasswordRequest;
import com.cashbee.application.dto.auth.ForgotPasswordResponse;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.common.exception.TooManyRequestsException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.port.EmailPort;
import com.cashbee.domain.port.IdentityProviderPort;
import com.cashbee.domain.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForgotPasswordUseCase Tests (TDD)")
class ForgotPasswordUseCaseTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private IdentityProviderPort identityProvider;

    @Mock
    private EmailPort emailPort;

    @InjectMocks
    private ForgotPasswordUseCase forgotPasswordUseCase;

    private ForgotPasswordRequest request;
    private IdentityProviderPort.IdentityUser mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordUseCase, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(forgotPasswordUseCase, "resendCooldownSeconds", 60);

        request = new ForgotPasswordRequest("user@example.com");

        mockUser = new IdentityProviderPort.IdentityUser() {
            @Override public String getId() { return "kc-uuid-123"; }
            @Override public String getUsername() { return "testuser"; }
            @Override public String getEmail() { return "user@example.com"; }
            @Override public String getFirstName() { return "Test"; }
            @Override public String getLastName() { return "User"; }
            @Override public boolean isEnabled() { return true; }
        };
    }

    @Test
    @DisplayName("Should send reset OTP when user exists and no prior OTP")
    void execute_SendsOtp_WhenUserExistsAndNoExistingOtp() {
        // Given
        when(identityProvider.getUserByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.empty());
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification otp = inv.getArgument(0);
            otp.setId(1L);
            return otp;
        });

        // When
        ForgotPasswordResponse response = forgotPasswordUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.maskedEmail()).isNotNull();
        assertThat(response.maskedEmail()).contains("@");
        assertThat(response.maskedEmail()).doesNotContain("user");
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        verify(emailPort).sendPasswordResetOtpEmail(eq("user@example.com"), anyString(), eq(5));
        verify(otpRepository).save(any(OtpVerification.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when user email does not exist")
    void execute_ThrowsNotFoundException_WhenEmailNotFound() {
        // Given
        when(identityProvider.getUserByEmail("user@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
            .isInstanceOf(NotFoundException.class);

        verify(otpRepository, never()).save(any());
        verify(emailPort, never()).sendPasswordResetOtpEmail(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user is disabled")
    void execute_ThrowsNotFoundException_WhenUserDisabled() {
        // Given
        IdentityProviderPort.IdentityUser disabledUser = new IdentityProviderPort.IdentityUser() {
            @Override public String getId() { return "kc-uuid-123"; }
            @Override public String getUsername() { return "testuser"; }
            @Override public String getEmail() { return "user@example.com"; }
            @Override public String getFirstName() { return "Test"; }
            @Override public String getLastName() { return "User"; }
            @Override public boolean isEnabled() { return false; }
        };
        when(identityProvider.getUserByEmail("user@example.com")).thenReturn(Optional.of(disabledUser));

        // When & Then
        assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
            .isInstanceOf(NotFoundException.class);

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw TooManyRequestsException with OTP_RESEND_COOLDOWN when resend too soon")
    void execute_ThrowsException_WhenResendCooldownActive() {
        // Given - existing OTP sent 30 seconds ago (cooldown is 60s)
        OtpVerification existingOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusSeconds(30))
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .verified(false)
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .lastResendAt(LocalDateTime.now().minusSeconds(30)) // sent 30 seconds ago
            .build();

        when(identityProvider.getUserByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(existingOtp));

        // When & Then
        assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
            .isInstanceOf(TooManyRequestsException.class)
            .satisfies(ex -> assertThat(((TooManyRequestsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OTP_RESEND_COOLDOWN));

        verify(otpRepository, never()).save(any());
        verify(emailPort, never()).sendPasswordResetOtpEmail(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Should resend OTP when cooldown has passed")
    void execute_ResendsOtp_WhenCooldownHasPassed() {
        // Given - existing OTP sent 2 minutes ago (cooldown is 60s)
        OtpVerification existingOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusMinutes(2))
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .verified(false)
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .lastResendAt(LocalDateTime.now().minusMinutes(2)) // 2 minutes ago — cooldown passed
            .build();

        when(identityProvider.getUserByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(existingOtp));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ForgotPasswordResponse response = forgotPasswordUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        verify(emailPort).sendPasswordResetOtpEmail(eq("user@example.com"), anyString(), eq(5));
        verify(otpRepository).save(existingOtp);
    }
}
