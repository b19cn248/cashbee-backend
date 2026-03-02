package com.cashbee.presentation.controller;

import com.cashbee.application.dto.auth.ForgotPasswordRequest;
import com.cashbee.application.dto.auth.ForgotPasswordResponse;
import com.cashbee.application.dto.auth.RegisterRequest;
import com.cashbee.application.dto.auth.RegisterResponse;
import com.cashbee.application.dto.auth.ResendOtpRequest;
import com.cashbee.application.dto.auth.ResendOtpResponse;
import com.cashbee.application.dto.auth.ResetPasswordRequest;
import com.cashbee.application.dto.auth.ResetPasswordResponse;
import com.cashbee.application.dto.auth.VerifyOtpRequest;
import com.cashbee.application.dto.auth.VerifyOtpResponse;
import com.cashbee.application.dto.auth.VerifyResetOtpRequest;
import com.cashbee.application.dto.auth.VerifyResetOtpResponse;
import com.cashbee.application.usecase.auth.ForgotPasswordUseCase;
import com.cashbee.application.usecase.auth.RegisterUserUseCase;
import com.cashbee.application.usecase.auth.ResendOtpUseCase;
import com.cashbee.application.usecase.auth.ResetPasswordUseCase;
import com.cashbee.application.usecase.auth.VerifyOtpUseCase;
import com.cashbee.application.usecase.auth.VerifyResetOtpUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Authentication operations.
 * <p>
 * This controller handles public authentication endpoints that don't require
 * authentication (e.g., registration, login).
 * <p>
 * Endpoints:
 * - POST /api/auth/register - Register new user (public)
 * <p>
 * Security Notes:
 * - /api/auth/register is configured as public endpoint in SecurityConfig
 * - No authentication required to access this endpoint
 * - User creation includes both Keycloak and local database
 * <p>
 * Flow for Registration:
 * 1. Mobile app collects user information
 * 2. App sends POST request to /api/auth/register
 * 3. System creates user in Keycloak (with password)
 * 4. System saves user to local database
 * 5. System auto-creates wallet with zero balance
 * 6. Returns user info + referral code
 * 7. User can immediately log in using username/password
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

@Tag(name = "Authentication", description = "Public authentication endpoints (registration, OTP verification, login)")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final VerifyResetOtpUseCase verifyResetOtpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    /**
     * Register a new user.
     * <p>
     * This is a public endpoint (no authentication required) that creates a new user account.
     * <p>
     * Process:
     * 1. Validates input data (username, email, password, referral code)
     * 2. Checks for duplicate username/email in Keycloak
     * 3. Validates referral code if provided (format + existence + active status)
     * 4. Creates user in Keycloak with password and assigns USER role
     * 5. Generates unique referral code for the new user
     * 6. Saves user to local database
     * 7. Auto-creates wallet with balance = 0
     * 8. Returns user info + referral code
     * <p>
     * Error Handling:
     * - 400 Bad Request: Invalid input (validation errors)
     * - 409 Conflict: Duplicate username/email
     * - 400 Bad Request: Invalid/inactive referral code
     * - 500 Internal Server Error: System failure
     * <p>
     * Example Request:
     * <pre>
     * POST /api/auth/register
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "SecurePass123",
     *   "fullName": "John Doe",
     *   "phone": "0901234567",
     *   "referredBy": "CB4F7A9K"
     * }
     * </pre>
     * <p>
     * Example Response:
     * <pre>
     * {
     *   "success": true,
     *   "message": "Registration successful! Your referral code is: CBXYZ123",
     *   "data": {
     *     "userId": 123,
     *     "keycloakId": "uuid-here",
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "fullName": "John Doe",
     *     "phone": "0901234567",
     *     "referralCode": "CBXYZ123",
     *     "referredBy": "CB4F7A9K",
     *     "walletId": 456,
     *     "status": "ACTIVE",
     *     "createdAt": "2025-11-14T10:30:00"
     *   }
     * }
     * </pre>
     *
     * @param request Registration request with user data
     * @return API response with registered user information
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Create a new user account with Keycloak authentication and local database storage. " +
                      "Automatically creates wallet and generates unique referral code. " +
                      "This is a public endpoint - no authentication required."
    )
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("API: Registration request received: username={}, email={}",
            request.getUsername(), request.getEmail());

        // Execute registration use case
        RegisterResponse response = registerUserUseCase.execute(request);

        log.info("API: User registered successfully: userId={}, username={}, referralCode={}",
            response.getUserId(), response.getUsername(), response.getReferralCode());

        // Return 200 OK (not 201 Created anymore - registration not complete yet)
        return ResponseEntity
                .ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * Verify OTP and complete user registration.
     * <p>
     * After user receives OTP email, they must verify it to complete registration.
     * This endpoint enables the Keycloak user, creates local DB user, and creates wallet.
     *
     * @param request OTP verification request
     * @return API response with user information
     */
    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP and complete registration",
        description = "Verify the OTP code sent to email. " +
                      "Completes user registration by enabling Keycloak account, " +
                      "creating local database user, and creating wallet. " +
                      "After successful verification, user can login."
    )
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        log.info("API: OTP verification request received: email={}", request.email());

        // Execute OTP verification use case
        VerifyOtpResponse response = verifyOtpUseCase.execute(request);

        log.info("API: OTP verified successfully: userId={}, username={}",
            response.userId(), response.username());

        // Return 201 Created (registration now complete)
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Email verified successfully! Your account is now active."));
    }

    /**
     * Resend OTP verification email.
     * <p>
     * If user didn't receive OTP, or it expired, they can request a new one.
     * Subject to cooldown period and daily limit.
     *
     * @param request Resend OTP request
     * @return API response with masked email
     */
    @PostMapping("/resend-otp")
    @Operation(
        summary = "Resend OTP verification email",
        description = "Request a new OTP code to be sent to email. " +
                      "Subject to cooldown period (30 seconds) and daily limit (5 resends). " +
                      "Resets attempt count and extends expiry time."
    )
    public ResponseEntity<ApiResponse<ResendOtpResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        log.info("API: Resend OTP request received: email={}", request.email());

        // Execute resend OTP use case
        ResendOtpResponse response = resendOtpUseCase.execute(request);

        log.info("API: OTP resent successfully: email={}", request.email());

        return ResponseEntity
                .ok(ApiResponse.success(response, response.message()));
    }

    // ==================== Password Reset Endpoints ====================

    /**
     * Forgot password - send OTP to user's email (step 1).
     * <p>
     * User provides their email. If the email exists in the system,
     * an OTP code is sent for password reset verification.
     *
     * @param request Forgot password request with email
     * @return API response with masked email and OTP expiry info
     */
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Forgot password - send reset OTP",
        description = "Send a password reset OTP to the user's email address. " +
                      "If the email exists in the system, an OTP code is sent. " +
                      "This is a public endpoint - no authentication required."
    )
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("API: Forgot password request received: email={}", request.email());

        ForgotPasswordResponse response = forgotPasswordUseCase.execute(request);

        log.info("API: Forgot password OTP sent successfully");

        return ResponseEntity
                .ok(ApiResponse.success(response, response.message()));
    }

    /**
     * Verify reset OTP and obtain a reset token (step 2).
     * <p>
     * User provides the OTP code received via email.
     * If valid, a reset token (UUID) is returned for step 3.
     *
     * @param request Verify reset OTP request with email and OTP code
     * @return API response with reset token
     */
    @PostMapping("/verify-reset-otp")
    @Operation(
        summary = "Verify password reset OTP",
        description = "Verify the OTP code sent for password reset. " +
                      "Returns a reset token (UUID) that must be used in the next step. " +
                      "This is a public endpoint - no authentication required."
    )
    public ResponseEntity<ApiResponse<VerifyResetOtpResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {

        log.info("API: Verify reset OTP request received: email={}", request.email());

        VerifyResetOtpResponse response = verifyResetOtpUseCase.execute(request);

        log.info("API: Reset OTP verified successfully");

        return ResponseEntity
                .ok(ApiResponse.success(response, response.message()));
    }

    /**
     * Reset password using the reset token (step 3).
     * <p>
     * User provides the reset token and new password.
     * The password is reset in Keycloak.
     *
     * @param request Reset password request with email, reset token, and new password
     * @return API response with success message
     */
    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Reset the user's password using the reset token obtained from OTP verification. " +
                      "The reset token must be used within 10 minutes of verification. " +
                      "This is a public endpoint - no authentication required."
    )
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("API: Reset password request received: email={}", request.email());

        ResetPasswordResponse response = resetPasswordUseCase.execute(request);

        log.info("API: Password reset successfully");

        return ResponseEntity
                .ok(ApiResponse.success(response, response.message()));
    }
}
