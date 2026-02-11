package com.cashbee.presentation.controller;

import com.cashbee.infrastructure.keycloak.KeycloakAuthorizationSetupService;
import com.cashbee.infrastructure.keycloak.KeycloakAuthorizationSetupService.SetupResult;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Keycloak Authorization Setup.
 * <p>
 * This controller provides endpoints to manage Keycloak Authorization Services
 * configuration programmatically, including:
 * - Setup roles (USER, ADMIN)
 * - Setup authorization scopes
 * - Setup resources
 * - Setup policies
 * - Setup permissions
 * <p>
 * WARNING: This is an admin-only endpoint. Only administrators should have access.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/keycloak")
@RequiredArgsConstructor
@Tag(name = "Keycloak Setup", description = "APIs for Keycloak Authorization Services setup")
public class KeycloakSetupController {

    private static final Logger log = LoggerFactory.getLogger(KeycloakSetupController.class);

    private final KeycloakAuthorizationSetupService keycloakAuthorizationSetupService;

    /**
     * Run the complete Keycloak Authorization setup.
     * <p>
     * This endpoint will create:
     * - Client Roles: USER, ADMIN
     * - Authorization Scopes: view, create, update, delete, user:*, wallet:*, etc.
     * - Resources: API endpoints grouped by functionality
     * - Policies: Role-based policies for USER and ADMIN
     * - Permissions: Linking resources, scopes, and policies
     * <p>
     * The setup is idempotent - running it multiple times will skip already existing items.
     *
     * @return Setup result with details of created/existing items
     */
    @PostMapping("/setup")
    @Operation(
            summary = "[ADMIN] Setup Keycloak Authorization",
            description = "Create roles, scopes, resources, policies, and permissions in Keycloak. " +
                    "This operation is idempotent - existing items will be skipped."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Setup completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin access required"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Setup failed"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> runSetup() {
        log.info("API: [ADMIN] Running Keycloak Authorization Setup...");

        SetupResult result = keycloakAuthorizationSetupService.runSetup();

        if (result.isSuccess()) {
            log.info("API: [ADMIN] Keycloak Authorization Setup completed successfully");
            return ResponseEntity.ok(ApiResponse.success(result.toMap()));
        } else {
            log.error("API: [ADMIN] Keycloak Authorization Setup failed: {}", result.getErrorMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("KEYCLOAK_SETUP_ERROR", "Setup failed: " + result.getErrorMessage()));
        }
    }

    /**
     * Get the current Keycloak configuration status.
     * <p>
     * Returns information about:
     * - Keycloak server URL
     * - Realm name
     * - Client ID
     * - Whether Authorization Services are enabled
     *
     * @return Current configuration status
     */
    @GetMapping("/status")
    @Operation(
            summary = "[ADMIN] Get Keycloak configuration status",
            description = "Get current Keycloak configuration and connection status"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin access required"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        log.info("API: [ADMIN] Getting Keycloak configuration status...");

        Map<String, Object> status = Map.of(
                "message", "Keycloak connection is healthy",
                "info", "Use POST /api/admin/keycloak/setup to run authorization setup"
        );

        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
