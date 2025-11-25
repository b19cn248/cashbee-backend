package com.cashbee.presentation.controller;

import com.cashbee.application.dto.user.UpdateUserCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.dto.user.UserSyncCommand;
import com.cashbee.application.usecase.user.GetUserByKeycloakIdUseCase;
import com.cashbee.application.usecase.user.SyncUserFromKeycloakUseCase;
import com.cashbee.application.usecase.user.UpdateUserUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User operations.
 * <p>
 * Endpoints:
 * - POST /api/users/sync - Sync user from Keycloak
 * - GET /api/users/me - Get current authenticated user info
 * - GET /api/users/keycloak/{keycloakId} - Get user by Keycloak ID
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

@Tag(name = "User Management", description = "User management endpoints")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final SyncUserFromKeycloakUseCase syncUserFromKeycloakUseCase;
    private final GetUserByKeycloakIdUseCase getUserByKeycloakIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final SecurityUtils securityUtils;

    /**
     * Sync user from Keycloak to local database.
     * <p>
     * This endpoint is called when:
     * - User logs in for the first time (creates new user + wallet)
     * - User logs in and their Keycloak data has changed (updates user)
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
     * This endpoint automatically extracts userId from JWT token,
     * so frontend doesn't need to send userId explicitly.
     *
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
     *
     * This endpoint allows users to update their profile including:
     * - Basic information (fullName, phone)
     * - Bank account information (accountNumber, bankCode)
     *
     * All fields are optional - only provided fields will be updated.
     *
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
     * @param jwt JWT token (auto-injected by Spring Security)
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
}
