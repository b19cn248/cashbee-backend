package com.cashbee.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing the authenticated user's password.
 * The user must provide their current password and a new password.
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Mat khau cu khong duoc de trong")
    String oldPassword,

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    String newPassword
) {
}
