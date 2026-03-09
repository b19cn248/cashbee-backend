package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.VerifyResetOtpRequest;
import com.cashbee.application.dto.auth.VerifyResetOtpResponse;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.exception.OtpValidationException;
import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyResetOtpUseCase Tests (TDD)")
class VerifyResetOtpUseCaseTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @InjectMocks
    private VerifyResetOtpUseCase verifyResetOtpUseCase;

    private OtpVerification validOtp;

    @BeforeEach
    void setUp() {
        validOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .expiresAt(LocalDateTime.now().plusMinutes(4))
            .verified(false)
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .build();
    }

    @Test
    @DisplayName("Should return reset token when OTP code is correct")
    void execute_ReturnsResetToken_WhenOtpCodeCorrect() {
        // Given
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(validOtp));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "123456");

        // When
        VerifyResetOtpResponse response = verifyResetOtpUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.resetToken()).isNotNull();
        assertThat(response.resetToken()).isNotBlank();
        // UUID format: 8-4-4-4-12 chars separated by hyphens
        assertThat(response.resetToken()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(otpRepository).save(validOtp);
    }

    @Test
    @DisplayName("Should throw OtpValidationException with INVALID_OTP when code is wrong")
    void execute_ThrowsInvalidOtp_WhenCodeWrong() {
        // Given
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(validOtp));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "999999");

        // When & Then
        assertThatThrownBy(() -> verifyResetOtpUseCase.execute(request))
            .isInstanceOf(OtpValidationException.class)
            .satisfies(ex -> assertThat(((OtpValidationException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP));

        // Attempt count incremented
        verify(otpRepository).save(validOtp);
        assertThat(validOtp.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw BadRequestException with OTP_EXPIRED when OTP is expired")
    void execute_ThrowsOtpExpired_WhenOtpExpired() {
        // Given - expired OTP
        OtpVerification expiredOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusMinutes(10))
            .expiresAt(LocalDateTime.now().minusMinutes(5)) // already expired
            .verified(false)
            .attemptCount(0)
            .maxAttempts(3)
            .resendCount(0)
            .build();

        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(expiredOtp));

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "123456");

        // When & Then
        assertThatThrownBy(() -> verifyResetOtpUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OTP_EXPIRED));

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException with MAX_ATTEMPTS_EXCEEDED when attempts exhausted")
    void execute_ThrowsMaxAttemptsExceeded_WhenNoAttemptsLeft() {
        // Given - OTP with max attempts already reached
        OtpVerification maxAttemptsOtp = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .expiresAt(LocalDateTime.now().plusMinutes(4))
            .verified(false)
            .attemptCount(3) // already at max
            .maxAttempts(3)
            .resendCount(0)
            .build();

        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(maxAttemptsOtp));

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "123456");

        // When & Then
        assertThatThrownBy(() -> verifyResetOtpUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MAX_ATTEMPTS_EXCEEDED));

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException with INVALID_OTP when no OTP record found")
    void execute_ThrowsInvalidOtp_WhenNoOtpRecordFound() {
        // Given
        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.empty());

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "123456");

        // When & Then
        assertThatThrownBy(() -> verifyResetOtpUseCase.execute(request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP));

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should report correct attemptsRemaining in exception data when wrong code")
    void execute_ReportsAttemptsRemaining_WhenCodeWrong() {
        // Given - 1 previous attempt (2 remaining after this attempt)
        OtpVerification otpWithOneAttempt = OtpVerification.builder()
            .id(1L)
            .email("user@example.com")
            .otpCode("123456")
            .purpose(OtpPurpose.PASSWORD_RESET)
            .keycloakId("kc-uuid-123")
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .expiresAt(LocalDateTime.now().plusMinutes(4))
            .verified(false)
            .attemptCount(1) // 1 previous attempt
            .maxAttempts(3)
            .resendCount(0)
            .build();

        when(otpRepository.findLatestByEmailAndPurpose("user@example.com", OtpPurpose.PASSWORD_RESET))
            .thenReturn(Optional.of(otpWithOneAttempt));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyResetOtpRequest request = new VerifyResetOtpRequest("user@example.com", "999999");

        // When & Then - after increment, attemptCount=2, remaining=1
        assertThatThrownBy(() -> verifyResetOtpUseCase.execute(request))
            .isInstanceOf(OtpValidationException.class)
            .satisfies(ex -> {
                OtpValidationException otpEx = (OtpValidationException) ex;
                assertThat(otpEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_OTP);
                assertThat(otpEx.getAttemptsRemaining()).isEqualTo(1);
            });
    }
}
