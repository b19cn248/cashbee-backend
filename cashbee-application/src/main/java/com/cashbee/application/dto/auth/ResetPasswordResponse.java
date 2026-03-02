package com.cashbee.application.dto.auth;

/**
 * Response DTO for successful password reset (step 3).
 */
public record ResetPasswordResponse(
    String message
) {
    public static ResetPasswordResponse success() {
        return new ResetPasswordResponse("Dat lai mat khau thanh cong");
    }
}
