package com.cashbee.presentation.controller;

import com.cashbee.application.dto.auth.RegisterRequest;
import com.cashbee.application.dto.auth.RegisterResponse;
import com.cashbee.application.dto.auth.ResendOtpRequest;
import com.cashbee.application.dto.auth.ResendOtpResponse;
import com.cashbee.application.dto.auth.VerifyOtpRequest;
import com.cashbee.application.dto.auth.VerifyOtpResponse;
import com.cashbee.application.usecase.auth.RegisterUserUseCase;
import com.cashbee.application.usecase.auth.ResendOtpUseCase;
import com.cashbee.application.usecase.auth.VerifyOtpUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 7. User can immediately login using username/password
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Public authentication endpoints (registration, OTP verification, login)")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;

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
     * If user didn't receive OTP or it expired, they can request a new one.
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
}
