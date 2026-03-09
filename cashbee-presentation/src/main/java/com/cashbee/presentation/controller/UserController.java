package com.cashbee.presentation.controller;

import com.cashbee.application.dto.auth.ChangePasswordRequest;
import com.cashbee.application.dto.user.CheckEmailExistsResponse;
import com.cashbee.application.dto.user.CheckFirstLoginResponse;
import com.cashbee.application.dto.user.CheckPhoneExistsResponse;
import com.cashbee.application.dto.user.CheckReferralCodeExistsResponse;
import com.cashbee.application.dto.user.CheckReferredByExistsResponse;
import com.cashbee.application.dto.user.UpdateUserProfileCommand;
import com.cashbee.application.dto.user.UpdateUserCommand;
import com.cashbee.application.dto.user.UpdateUserLevelCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.dto.user.UserSyncCommand;
import com.cashbee.application.usecase.auth.ChangePasswordUseCase;
import com.cashbee.application.usecase.user.CheckFirstLoginUseCase;
import com.cashbee.application.usecase.user.CheckUserExistsByEmailUseCase;
import com.cashbee.application.usecase.user.CheckUserExistsByPhoneUseCase;
import com.cashbee.application.usecase.user.CheckUserExistsByReferralCodeUseCase;
import com.cashbee.application.usecase.user.CheckUserExistsByReferredByUseCase;
import com.cashbee.application.usecase.user.GetUserByKeycloakIdUseCase;
import com.cashbee.application.usecase.user.UpdateUserProfileUseCase;
import com.cashbee.application.usecase.user.SyncUserFromKeycloakUseCase;
import com.cashbee.application.usecase.user.UpdateUserLevelUseCase;
import com.cashbee.application.usecase.user.UpdateUserUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for User operations.
 * <p>
 * Endpoints: - POST /api/users/sync - Sync user from Keycloak - GET /api/users/me - Get current
 * authenticated user info - GET /api/users/keycloak/{keycloakId} - Get user by Keycloak ID
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

@Tag(name = "User Management", description = "User management endpoints")
public class UserController {

  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  private final ChangePasswordUseCase changePasswordUseCase;
  private final SyncUserFromKeycloakUseCase syncUserFromKeycloakUseCase;
  private final GetUserByKeycloakIdUseCase getUserByKeycloakIdUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final UpdateUserLevelUseCase updateUserLevelUseCase;
  private final UpdateUserProfileUseCase updateUserProfileUseCase;
  private final CheckUserExistsByEmailUseCase checkUserExistsByEmailUseCase;
  private final CheckUserExistsByPhoneUseCase checkUserExistsByPhoneUseCase;
  private final CheckUserExistsByReferralCodeUseCase checkUserExistsByReferralCodeUseCase;
  private final CheckUserExistsByReferredByUseCase checkUserExistsByReferredByUseCase;
  private final CheckFirstLoginUseCase checkFirstLoginUseCase;
  private final SecurityUtils securityUtils;

  /**
   * Sync user from Keycloak to local database.
   * <p>
   * This endpoint is called when: - User logs in for the first time (creates new user + wallet) -
   * User logs in and their Keycloak data has changed (updates user)
   *
   * @param command User data from Keycloak
   * @return Synced user response
   */
  @PostMapping("/sync")
  @Operation(summary = "Sync user from Keycloak",
      description = "Synchronize user data from Keycloak to local database")
  public ResponseEntity<ApiResponse<UserResponse>> syncUser(
      @Valid @RequestBody UserSyncCommand command) {

    log.info("API: Syncing user from Keycloak: keycloakId={}", command.getKeycloakId());

    UserResponse user = syncUserFromKeycloakUseCase.execute(command);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user, "User synced successfully"));
  }

  /**
   * Get current authenticated user information.
   * <p>
   * This endpoint automatically extracts userId from JWT token, so frontend doesn't need to send
   * userId explicitly.
   * <p>
   * Usage (Frontend):
   * <pre>
   * const response = await fetch('/api/users/me', {
   *   headers: { 'Authorization': `Bearer ${token}` }
   * });
   * const user = response.data;  // Contains userId, email, etc.
   * </pre>
   *
   * @param jwt JWT token (auto-injected by Spring Security)
   * @return Current user information
   */
  @GetMapping("/me")
  @Operation(summary = "Get current user",
      description = "Get current authenticated user information from JWT token")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
      @AuthenticationPrincipal Jwt jwt) {

    log.info("API: Getting current user from JWT token");

    // Extract keycloakId from JWT and get user
    String keycloakId = securityUtils.getKeycloakUserId(jwt);
    UserResponse user = getUserByKeycloakIdUseCase.execute(keycloakId);

    log.info("API: Current user: userId={}, email={}", user.getId(), user.getEmail());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user));
  }

  /**
   * Get user by Keycloak ID.
   *
   * @param keycloakId Keycloak user ID
   * @return User response
   */
  @GetMapping("/keycloak/{keycloakId}")
  @Operation(summary = "Get user by Keycloak ID",
      description = "Retrieve user information by Keycloak user ID")
  public ResponseEntity<ApiResponse<UserResponse>> getUserByKeycloakId(
      @PathVariable String keycloakId) {

    log.info("API: Getting user by keycloakId: {}", keycloakId);

    UserResponse user = getUserByKeycloakIdUseCase.execute(keycloakId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user));
  }

  /**
   * Update current user information.
   * <p>
   * This endpoint allows users to update their profile including: - Basic information (fullName,
   * phone) - Bank account information (accountNumber, bankCode)
   * <p>
   * All fields are optional - only provided fields will be updated.
   * <p>
   * Usage (Frontend):
   * <pre>
   * const response = await fetch('/api/users/me', {
   *   method: 'PUT',
   *   headers: {
   *     'Authorization': `Bearer ${token}`,
   *     'Content-Type': 'application/json'
   *   },
   *   body: JSON.stringify({
   *     fullName: "Nguyen Van A",
   *     phone: "0987654321",
   *     accountNumber: "1234567890",
   *     bankCode: "VPBANK"
   *   })
   * });
   * </pre>
   *
   * @param jwt     JWT token (auto-injected by Spring Security)
   * @param command Update command with new values
   * @return Updated user information
   */
  @PutMapping("/me")
  @Operation(summary = "Update current user",
      description = "Update current user's profile information including bank account")
  public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateUserCommand command) {

    log.info("API: Updating current user");

    // Extract keycloakId from JWT
    String keycloakId = securityUtils.getKeycloakUserId(jwt);

    // Execute update
    UserResponse user = updateUserUseCase.execute(keycloakId, command);

    log.info("API: User updated successfully: userId={}", user.getId());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user, "User updated successfully"));
  }

  /**
   * Update user profile information.
   * <p>
   * This API allows updating:
   * - phone: Phone number
   * - referredBy: Referral code of the person who referred this user
   * - accountNumber: Bank account number
   * - accountName: Bank account holder name
   * - bankName: Bank name
   * - bankCode: Bank code
   * <p>
   * All fields are optional - only provided fields will be updated.
   * <p>
   * Note: referredBy can only be set once. If user already has referredBy,
   * it cannot be changed.
   * <p>
   * Usage (Frontend):
   * <pre>
   * const response = await fetch('/api/users/me/profile', {
   *   method: 'PUT',
   *   headers: {
   *     'Authorization': `Bearer ${token}`,
   *     'Content-Type': 'application/json'
   *   },
   *   body: JSON.stringify({
   *     phone: "0987654321",
   *     referredBy: "CB4F7A9K",
   *     accountNumber: "1234567890",
   *     accountName: "NGUYEN VAN A",
   *     bankName: "Ngan hang TMCP Viet Nam Thinh Vuong",
   *     bankCode: "VPBANK"
   *   })
   * });
   * </pre>
   *
   * @param jwt     JWT token (auto-injected by Spring Security)
   * @param command Command containing profile fields to update
   * @return Updated user information
   */
  @PutMapping("/me/profile")
  @Operation(summary = "Update user profile",
      description = "Update current user's profile including phone, referral code, and bank account information")
  public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateUserProfileCommand command) {

    log.info("API: Updating profile for current user");

    // Extract keycloakId from JWT
    String keycloakId = securityUtils.getKeycloakUserId(jwt);

    // Execute update
    UserResponse user = updateUserProfileUseCase.execute(keycloakId, command);

    log.info("API: Profile updated successfully: userId={}", user.getId());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user, "Profile updated successfully"));
  }

  /**
   * Update user level (admin operation).
   * <p>
   * This endpoint allows admin to change a user's tier level.
   * Useful for promoting special customers to DIAMOND level (100% cashback).
   * <p>
   * Available levels: NORMAL (80%), VIP (83%), SUPER (85%), DIAMOND (100%)
   *
   * @param userId  User ID to update
   * @param command Command containing new user level
   * @return Updated user information
   */
  @PutMapping("/{userId}/level")
  @Operation(summary = "Update user level",
      description = "Admin endpoint to update user's tier level (NORMAL, VIP, SUPER, DIAMOND)")
  public ResponseEntity<ApiResponse<UserResponse>> updateUserLevel(
      @PathVariable Long userId,
      @Valid @RequestBody UpdateUserLevelCommand command) {

    log.info("API: Updating user level: userId={}, newLevel={}", userId, command.getUserLevel());

    UserResponse user = updateUserLevelUseCase.execute(userId, command);

    log.info("API: User level updated successfully: userId={}, level={}",
        user.getId(), user.getUserLevel());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(user, "User level updated successfully"));
  }

  /**
   * Check if email already exists in the system.
   * <p>
   * This endpoint is useful for:
   * - Checking email before registration
   * - Validating email in forms
   * - Preventing duplicate user creation
   * <p>
   * Usage:
   * <pre>
   * GET /api/users/check-email?email=test@gmail.com
   *
   * Response:
   * {
   *   "status": "success",
   *   "data": {
   *     "email": "test@gmail.com",
   *     "exists": true
   *   }
   * }
   * </pre>
   *
   * @param email Email to check
   * @return Response containing email and exists flag (true/false)
   */
  @GetMapping("/check-email")
  @Operation(summary = "Check if email exists",
      description = "Check if an email address is already registered in the system")
  public ResponseEntity<ApiResponse<CheckEmailExistsResponse>> checkEmailExists(
      @RequestParam @Email(message = "Invalid email format") String email) {

    log.info("API: Checking if email exists: {}", email);

    CheckEmailExistsResponse response = checkUserExistsByEmailUseCase.execute(email);

    log.info("API: Email {} exists: {}", email, response.isExists());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  /**
   * Check if phone number already exists in the system.
   * <p>
   * This endpoint is useful for:
   * - Checking phone number before registration
   * - Validating phone in forms
   * - Preventing duplicate user creation
   * <p>
   * Usage:
   * <pre>
   * GET /api/users/check-phone?phone=0987654321
   *
   * Response:
   * {
   *   "status": "success",
   *   "data": {
   *     "phone": "0987654321",
   *     "exists": true
   *   }
   * }
   * </pre>
   *
   * @param phone Phone number to check
   * @return Response containing phone and exists flag (true/false)
   */
  @GetMapping("/check-phone")
  @Operation(summary = "Check if phone exists",
      description = "Check if a phone number is already registered in the system")
  public ResponseEntity<ApiResponse<CheckPhoneExistsResponse>> checkPhoneExists(
      @RequestParam String phone) {

    log.info("API: Checking if phone exists: {}", phone);

    CheckPhoneExistsResponse response = checkUserExistsByPhoneUseCase.execute(phone);

    log.info("API: Phone {} exists: {}", phone, response.isExists());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  /**
   * Check if referral code has been used by someone during registration.
   * <p>
   * This endpoint checks if the given referral code (referredBy) has already been
   * used by another user when they registered.
   * <p>
   * Note the difference:
   * - referralCode: The code OF a user (user creates to share with others)
   * - referredBy: The code a user USED during registration (code of the person who referred them)
   * <p>
   * Usage:
   * <pre>
   * GET /api/users/check-referred-by?referredBy=CB123456
   *
   * Response:
   * {
   *   "status": "success",
   *   "data": {
   *     "referredBy": "CB123456",
   *     "exists": true
   *   }
   * }
   * </pre>
   *
   * @param referredBy Referral code to check
   * @return Response containing referredBy and exists flag (true/false)
   */
  @GetMapping("/check-referred-by")
  @Operation(summary = "Check if referral code is used",
      description = "Check if a referral code has already been used by someone during registration")
  public ResponseEntity<ApiResponse<CheckReferredByExistsResponse>> checkReferredByExists(
      @RequestParam String referredBy) {

    log.info("API: Checking if referredBy exists: {}", referredBy);

    CheckReferredByExistsResponse response = checkUserExistsByReferredByUseCase.execute(referredBy);

    log.info("API: ReferredBy {} exists: {}", referredBy, response.isExists());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  /**
   * Check if referral code exists in the system.
   * <p>
   * This endpoint checks if the given referral code belongs to any user in the system.
   * <p>
   * Note the difference:
   * - referralCode: The code OF a user (user creates to share with others)
   * - referredBy: The code a user USED during registration (code of the person who referred them)
   * <p>
   * Use case:
   * - Before user enters a referral code, validate if it's a valid code that belongs to someone
   * <p>
   * Usage:
   * <pre>
   * GET /api/users/check-referral-code?referralCode=CB4F7A9K
   *
   * Response:
   * {
   *   "status": "success",
   *   "data": {
   *     "referralCode": "CB4F7A9K",
   *     "exists": true
   *   }
   * }
   * </pre>
   *
   * @param referralCode Referral code to check
   * @return Response containing referralCode and exists flag (true/false)
   */
  @GetMapping("/check-referral-code")
  @Operation(summary = "Check if referral code exists",
      description = "Check if a referral code belongs to any user in the system")
  public ResponseEntity<ApiResponse<CheckReferralCodeExistsResponse>> checkReferralCodeExists(
      @RequestParam String referralCode) {

    log.info("API: Checking if referralCode exists: {}", referralCode);

    CheckReferralCodeExistsResponse response = checkUserExistsByReferralCodeUseCase.execute(referralCode);

    log.info("API: ReferralCode {} exists: {}", referralCode, response.isExists());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  /**
   * Check if this is the user's first login.
   * <p>
   * This endpoint checks whether the current user has ever logged in before.
   * If this is the first login, it will:
   * 1. Mark the user as having logged in
   * 2. Update the last login timestamp
   * 3. Return firstLogin = true
   * <p>
   * On subsequent calls, it will return firstLogin = false.
   * <p>
   * Use case:
   * - Frontend can show onboarding/welcome screen on first login
   * - Track new user activation
   * <p>
   * Usage:
   * <pre>
   * GET /api/users/me/first-login
   *
   * Response:
   * {
   *   "status": "success",
   *   "data": {
   *     "firstLogin": true
   *   }
   * }
   * </pre>
   *
   * @param jwt JWT token (auto-injected by Spring Security)
   * @return Response indicating whether this is the first login
   */
  @GetMapping("/me/first-login")
  @Operation(summary = "Check first login",
      description = "Check if this is the user's first login and mark as logged in if so")
  public ResponseEntity<ApiResponse<CheckFirstLoginResponse>> checkFirstLogin(
      @AuthenticationPrincipal Jwt jwt) {

    log.info("API: Checking first login for current user");

    String keycloakId = securityUtils.getKeycloakUserId(jwt);
    CheckFirstLoginResponse response = checkFirstLoginUseCase.execute(keycloakId);

    log.info("API: First login check result: firstLogin={}", response.isFirstLogin());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  /**
   * Change password for the authenticated user.
   *
   * @param jwt     JWT token (auto-injected by Spring Security)
   * @param request Change password request with old and new password
   * @return Success response
   */
  @PutMapping("/change-password")
  @Operation(summary = "Change password",
      description = "Change the current user's password. Requires the current password for verification. " +
                    "New password must be at least 8 characters with uppercase, lowercase, and digit.")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody ChangePasswordRequest request) {

    log.info("API: Change password request received");

    String keycloakId = securityUtils.getKeycloakUserId(jwt);
    changePasswordUseCase.execute(keycloakId, request);

    log.info("API: Password changed successfully for keycloakId: {}", keycloakId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ApiResponse.success(null, "Password changed successfully."));
  }
}
