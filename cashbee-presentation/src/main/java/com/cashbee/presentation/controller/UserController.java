package com.cashbee.presentation.controller;

import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.dto.user.UserSyncCommand;
import com.cashbee.application.usecase.user.GetUserByKeycloakIdUseCase;
import com.cashbee.application.usecase.user.SyncUserFromKeycloakUseCase;
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
 * REST Controller for User operations.
 *
 * Endpoints:
 * - POST /api/users/sync - Sync user from Keycloak
 * - GET /api/users/keycloak/{keycloakId} - Get user by Keycloak ID
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User management endpoints")
public class UserController {

    private final SyncUserFromKeycloakUseCase syncUserFromKeycloakUseCase;
    private final GetUserByKeycloakIdUseCase getUserByKeycloakIdUseCase;

    /**
     * Sync user from Keycloak to local database.
     *
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
}
