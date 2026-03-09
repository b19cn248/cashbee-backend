package com.cashbee.application.usecase.auth;

import com.cashbee.application.dto.auth.ChangePasswordRequest;
import com.cashbee.common.constant.ErrorCode;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.domain.port.IdentityProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordUseCase Tests (TDD)")
class ChangePasswordUseCaseTest {

    @Mock
    private IdentityProviderPort identityProvider;

    @InjectMocks
    private ChangePasswordUseCase changePasswordUseCase;

    private IdentityProviderPort.IdentityUser mockUser;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Should change password successfully when old password is correct")
    void execute_ChangesPassword_WhenOldPasswordCorrect() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "OldPassword1")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1", "NewPassword2");

        // When
        changePasswordUseCase.execute("kc-uuid-123", request);

        // Then
        verify(identityProvider).resetUserPassword("kc-uuid-123", "NewPassword2");
    }

    @Test
    @DisplayName("Should throw BadRequestException with INVALID_CREDENTIALS when old password is wrong")
    void execute_ThrowsInvalidCredentials_WhenOldPasswordWrong() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "WrongPassword1")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword1", "NewPassword2");

        // When & Then
        assertThatThrownBy(() -> changePasswordUseCase.execute("kc-uuid-123", request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when new password too short")
    void execute_ThrowsValidationError_WhenNewPasswordTooShort() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "OldPassword1")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1", "Ab1");

        // When & Then
        assertThatThrownBy(() -> changePasswordUseCase.execute("kc-uuid-123", request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when new password same as old")
    void execute_ThrowsValidationError_WhenNewPasswordSameAsOld() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "SamePassword1")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("SamePassword1", "SamePassword1");

        // When & Then
        assertThatThrownBy(() -> changePasswordUseCase.execute("kc-uuid-123", request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when new password has no uppercase")
    void execute_ThrowsValidationError_WhenNewPasswordNoUppercase() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "OldPassword1")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1", "alllower1");

        // When & Then
        assertThatThrownBy(() -> changePasswordUseCase.execute("kc-uuid-123", request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException with VALIDATION_ERROR when new password has no digit")
    void execute_ThrowsValidationError_WhenNewPasswordNoDigit() {
        // Given
        when(identityProvider.getUserById("kc-uuid-123")).thenReturn(Optional.of(mockUser));
        when(identityProvider.verifyUserCredentials("testuser", "OldPassword1")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword1", "NoDigitPassword");

        // When & Then
        assertThatThrownBy(() -> changePasswordUseCase.execute("kc-uuid-123", request))
            .isInstanceOf(BadRequestException.class)
            .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(identityProvider, never()).resetUserPassword(anyString(), anyString());
    }
}
