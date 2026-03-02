package com.cashbee.application.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for verifying the password reset OTP (step 2).
 * User provides their email and the OTP code received via email.
 */
public record VerifyResetOtpRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email,

    @NotBlank(message = "Mã OTP không được để trống")
    String otpCode
) {
}
