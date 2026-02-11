package com.cashbee.presentation.controller;

import com.cashbee.application.dto.referraladmin.CreateTierRequest;
import com.cashbee.application.dto.referraladmin.ReferrerTierConfigResponse;
import com.cashbee.application.dto.referraladmin.UpdateTierRequest;
import com.cashbee.application.usecase.referraladmin.CreateTierUseCase;
import com.cashbee.application.usecase.referraladmin.DeleteTierUseCase;
import com.cashbee.application.usecase.referraladmin.GetAllTiersUseCase;
import com.cashbee.application.usecase.referraladmin.GetTierByIdUseCase;
import com.cashbee.application.usecase.referraladmin.UpdateTierUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Referrer Tier Admin operations.
 * Provides CRUD endpoints for managing referrer tier configurations.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/referrer-tiers")
@RequiredArgsConstructor
@Tag(name = "Referrer Tier Admin", description = "Admin operations for referrer tier configurations")
public class ReferrerTierAdminController {
    private static final Logger log = LoggerFactory.getLogger(ReferrerTierAdminController.class);

    private final GetAllTiersUseCase getAllTiersUseCase;
    private final GetTierByIdUseCase getTierByIdUseCase;
    private final CreateTierUseCase createTierUseCase;
    private final UpdateTierUseCase updateTierUseCase;
    private final DeleteTierUseCase deleteTierUseCase;

    /**
     * Get all referrer tier configurations.
     */
    @GetMapping
    @Operation(
        summary = "Get all tiers",
        description = "Retrieve all referrer tier configurations"
    )
    public ResponseEntity<ApiResponse<List<ReferrerTierConfigResponse>>> getAllTiers() {
        log.info("API: Getting all referrer tiers");

        var tiers = getAllTiersUseCase.execute();

        return ResponseEntity.ok(
            ApiResponse.success(tiers, "Retrieved " + tiers.size() + " tiers")
        );
    }

    /**
     * Get a referrer tier configuration by ID.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get tier by ID",
        description = "Retrieve a specific referrer tier configuration by its ID"
    )
    public ResponseEntity<ApiResponse<ReferrerTierConfigResponse>> getTierById(
        @Parameter(description = "Tier ID", required = true)
        @PathVariable Long id
    ) {
        log.info("API: Getting tier by ID: {}", id);

        var tier = getTierByIdUseCase.execute(id);

        return ResponseEntity.ok(
            ApiResponse.success(tier, "Tier retrieved successfully")
        );
    }

    /**
     * Create a new referrer tier configuration.
     */
    @PostMapping
    @Operation(
        summary = "Create tier",
        description = "Create a new referrer tier configuration"
    )
    public ResponseEntity<ApiResponse<ReferrerTierConfigResponse>> createTier(
        @Valid @RequestBody CreateTierRequest request
    ) {
        log.info("API: Creating tier - name: {}, minReferrals: {}",
            request.getTierName(), request.getMinReferrals());

        var created = createTierUseCase.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(created, "Tier created successfully")
        );
    }

    /**
     * Update an existing referrer tier configuration.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update tier",
        description = "Update an existing referrer tier configuration. Only provided fields will be updated (partial update)."
    )
    public ResponseEntity<ApiResponse<ReferrerTierConfigResponse>> updateTier(
        @Parameter(description = "Tier ID to update", required = true)
        @PathVariable Long id,
        @Valid @RequestBody UpdateTierRequest request
    ) {
        log.info("API: Updating tier ID: {}", id);

        var updated = updateTierUseCase.execute(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(updated, "Tier updated successfully")
        );
    }

    /**
     * Delete a referrer tier configuration.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete tier",
        description = "Delete a referrer tier configuration by ID"
    )
    public ResponseEntity<ApiResponse<Void>> deleteTier(
        @Parameter(description = "Tier ID to delete", required = true)
        @PathVariable Long id
    ) {
        log.info("API: Deleting tier ID: {}", id);

        deleteTierUseCase.execute(id);

        return ResponseEntity.ok(
            ApiResponse.success(null, "Tier deleted successfully")
        );
    }
}
