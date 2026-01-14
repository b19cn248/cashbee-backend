package com.cashbee.presentation.controller;

import com.cashbee.application.dto.referraladmin.CreateMilestoneRequest;
import com.cashbee.application.dto.referraladmin.MilestoneConfigResponse;
import com.cashbee.application.dto.referraladmin.UpdateMilestoneRequest;
import com.cashbee.application.usecase.referraladmin.CreateMilestoneUseCase;
import com.cashbee.application.usecase.referraladmin.DeleteMilestoneUseCase;
import com.cashbee.application.usecase.referraladmin.GetAllMilestonesUseCase;
import com.cashbee.application.usecase.referraladmin.GetMilestoneByIdUseCase;
import com.cashbee.application.usecase.referraladmin.UpdateMilestoneUseCase;
import com.cashbee.domain.enums.MilestoneType;
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
 * REST Controller for Referral Milestone Admin operations.
 * Provides CRUD endpoints for managing milestone configurations.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestone Admin", description = "Admin operations for referral milestone configurations")
public class MilestoneAdminController {
    private static final Logger log = LoggerFactory.getLogger(MilestoneAdminController.class);

    private final GetAllMilestonesUseCase getAllMilestonesUseCase;
    private final GetMilestoneByIdUseCase getMilestoneByIdUseCase;
    private final CreateMilestoneUseCase createMilestoneUseCase;
    private final UpdateMilestoneUseCase updateMilestoneUseCase;
    private final DeleteMilestoneUseCase deleteMilestoneUseCase;

    /**
     * Get all milestone configurations with optional filters.
     */
    @GetMapping
    @Operation(
        summary = "Get all milestones",
        description = "Retrieve all milestone configurations with optional filtering by type and active status"
    )
    public ResponseEntity<ApiResponse<List<MilestoneConfigResponse>>> getAllMilestones(
        @Parameter(description = "Filter by milestone type (WITH_REFERRER or WITHOUT_REFERRER)")
        @RequestParam(required = false) String milestoneType,
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) Boolean isActive
    ) {
        log.info("API: Getting all milestones - type: {}, isActive: {}", milestoneType, isActive);

        MilestoneType type = null;
        if (milestoneType != null && !milestoneType.isEmpty()) {
            type = MilestoneType.valueOf(milestoneType.toUpperCase());
        }

        var milestones = getAllMilestonesUseCase.execute(type, isActive);

        return ResponseEntity.ok(
            ApiResponse.success(milestones, "Retrieved " + milestones.size() + " milestones")
        );
    }

    /**
     * Get a milestone configuration by ID.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get milestone by ID",
        description = "Retrieve a specific milestone configuration by its ID"
    )
    public ResponseEntity<ApiResponse<MilestoneConfigResponse>> getMilestoneById(
        @Parameter(description = "Milestone ID", required = true)
        @PathVariable Long id
    ) {
        log.info("API: Getting milestone by ID: {}", id);

        var milestone = getMilestoneByIdUseCase.execute(id);

        return ResponseEntity.ok(
            ApiResponse.success(milestone, "Milestone retrieved successfully")
        );
    }

    /**
     * Create a new milestone configuration.
     */
    @PostMapping
    @Operation(
        summary = "Create milestone",
        description = "Create a new milestone configuration for the referral system"
    )
    public ResponseEntity<ApiResponse<MilestoneConfigResponse>> createMilestone(
        @Valid @RequestBody CreateMilestoneRequest request
    ) {
        log.info("API: Creating milestone - type: {}, ordersRequired: {}",
            request.getMilestoneType(), request.getOrdersRequired());

        var created = createMilestoneUseCase.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(created, "Milestone created successfully")
        );
    }

    /**
     * Update an existing milestone configuration.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update milestone",
        description = "Update an existing milestone configuration. Only provided fields will be updated (partial update)."
    )
    public ResponseEntity<ApiResponse<MilestoneConfigResponse>> updateMilestone(
        @Parameter(description = "Milestone ID to update", required = true)
        @PathVariable Long id,
        @Valid @RequestBody UpdateMilestoneRequest request
    ) {
        log.info("API: Updating milestone ID: {}", id);

        var updated = updateMilestoneUseCase.execute(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(updated, "Milestone updated successfully")
        );
    }

    /**
     * Delete a milestone configuration.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete milestone",
        description = "Delete a milestone configuration by ID"
    )
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
        @Parameter(description = "Milestone ID to delete", required = true)
        @PathVariable Long id
    ) {
        log.info("API: Deleting milestone ID: {}", id);

        deleteMilestoneUseCase.execute(id);

        return ResponseEntity.ok(
            ApiResponse.success(null, "Milestone deleted successfully")
        );
    }
}
