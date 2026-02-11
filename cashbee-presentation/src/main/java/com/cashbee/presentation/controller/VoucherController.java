package com.cashbee.presentation.controller;

import com.cashbee.application.dto.voucher.BatchImportResponse;
import com.cashbee.application.dto.voucher.CreateVoucherCommand;
import com.cashbee.application.dto.voucher.UpdateVoucherStatusCommand;
import com.cashbee.application.dto.voucher.VoucherResponse;
import com.cashbee.application.dto.voucher.VoucherTrackingResponse;
import com.cashbee.application.usecase.voucher.BatchImportVouchersUseCase;
import com.cashbee.application.usecase.voucher.CreateVoucherUseCase;
import com.cashbee.application.usecase.voucher.GetVoucherByIdUseCase;
import com.cashbee.application.usecase.voucher.GetVouchersUseCase;
import com.cashbee.application.usecase.voucher.TrackVoucherClickUseCase;
import com.cashbee.application.usecase.voucher.TrackVoucherViewUseCase;
import com.cashbee.application.usecase.voucher.UpdateVoucherStatusUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Voucher/Promotion Code Management.
 *
 * Provides APIs for:
 * - Creating and importing vouchers
 * - Retrieving vouchers with filtering
 * - Tracking views and clicks
 * - Updating voucher status
 *
 * All endpoints are PUBLIC (no authentication required).
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Voucher Management", description = "APIs for managing promotion vouchers")
public class VoucherController {

    private final CreateVoucherUseCase createVoucherUseCase;
    private final GetVouchersUseCase getVouchersUseCase;
    private final GetVoucherByIdUseCase getVoucherByIdUseCase;
    private final TrackVoucherViewUseCase trackVoucherViewUseCase;
    private final TrackVoucherClickUseCase trackVoucherClickUseCase;
    private final UpdateVoucherStatusUseCase updateVoucherStatusUseCase;
    private final BatchImportVouchersUseCase batchImportVouchersUseCase;

    /**
     * Create a new voucher.
     *
     * @param command Voucher creation command
     * @return Created voucher
     */
    @PostMapping
    @Operation(
        summary = "Create a new voucher",
        description = "Creates a new promotion voucher. If originalLink is provided, " +
                      "an affiliate link will be automatically generated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Voucher created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody CreateVoucherCommand command) {
        log.info("REST request to create voucher: {}", command.getTitle());

        VoucherResponse response = createVoucherUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Voucher created successfully"));
    }

    /**
     * Batch import multiple vouchers.
     *
     * @param commands List of voucher commands
     * @return Batch import result with statistics
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Batch import vouchers",
        description = "Import multiple vouchers at once. Existing vouchers (same code) will be updated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch import completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ApiResponse<BatchImportResponse>> batchImportVouchers(
            @Valid @RequestBody List<CreateVoucherCommand> commands) {
        log.info("REST request to batch import {} vouchers", commands.size());

        BatchImportResponse response = batchImportVouchersUseCase.execute(commands);

        String message = response.getFailed() > 0
            ? "Batch import completed with some errors"
            : "Batch import completed";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Get list of vouchers with filtering and pagination.
     *
     * @param status Filter by status (default: ACTIVE)
     * @param category Filter by category
     * @param platform Filter by platform
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort field and direction (e.g., "createdAt,desc")
     * @return Page of vouchers
     */
    @GetMapping
    @Operation(
        summary = "Get vouchers",
        description = "Retrieve vouchers with optional filtering by status, category, and platform."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vouchers retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getVouchers(
            @Parameter(description = "Filter by status (default: ACTIVE)")
            @RequestParam(required = false) VoucherStatus status,

            @Parameter(description = "Filter by category")
            @RequestParam(required = false) VoucherCategory category,

            @Parameter(description = "Filter by platform (e.g., SHOPEE)")
            @RequestParam(required = false) String platform,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., createdAt,desc)")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        log.info("REST request to get vouchers - status: {}, category: {}, platform: {}, page: {}, size: {}",
            status, category, platform, page, size);

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        Page<VoucherResponse> response = getVouchersUseCase.execute(status, category, platform, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get voucher by ID.
     *
     * @param id Voucher ID
     * @return Voucher details
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get voucher by ID",
        description = "Retrieve a specific voucher by its ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Voucher found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Voucher not found")
    })
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(
            @Parameter(description = "Voucher ID")
            @PathVariable Long id) {
        log.info("REST request to get voucher by ID: {}", id);

        VoucherResponse response = getVoucherByIdUseCase.execute(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Track voucher view.
     *
     * @param id Voucher ID
     * @return Updated view count
     */
    @PostMapping("/{id}/view")
    @Operation(
        summary = "Track voucher view",
        description = "Increment the view count when a user views a voucher."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View tracked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Voucher not found")
    })
    public ResponseEntity<ApiResponse<VoucherTrackingResponse>> trackView(
            @Parameter(description = "Voucher ID")
            @PathVariable Long id) {
        log.info("REST request to track view for voucher ID: {}", id);

        VoucherTrackingResponse response = trackVoucherViewUseCase.execute(id);

        return ResponseEntity.ok(ApiResponse.success(response, "View tracked"));
    }

    /**
     * Track voucher click.
     *
     * @param id Voucher ID
     * @return Updated click count and affiliate link
     */
    @PostMapping("/{id}/click")
    @Operation(
        summary = "Track voucher click",
        description = "Increment the click count and return the affiliate link for redirect."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Click tracked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Voucher not found")
    })
    public ResponseEntity<ApiResponse<VoucherTrackingResponse>> trackClick(
            @Parameter(description = "Voucher ID")
            @PathVariable Long id) {
        log.info("REST request to track click for voucher ID: {}", id);

        VoucherTrackingResponse response = trackVoucherClickUseCase.execute(id);

        return ResponseEntity.ok(ApiResponse.success(response, "Click tracked"));
    }

    /**
     * Update voucher status.
     *
     * @param id Voucher ID
     * @param command Status update command
     * @return Updated voucher
     */
    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update voucher status",
        description = "Update the status of a voucher (ACTIVE, INACTIVE, EXPIRED)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Voucher not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status")
    })
    public ResponseEntity<ApiResponse<VoucherResponse>> updateStatus(
            @Parameter(description = "Voucher ID")
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherStatusCommand command) {
        log.info("REST request to update status for voucher ID: {} to {}", id, command.getStatus());

        VoucherResponse response = updateVoucherStatusUseCase.execute(id, command);

        return ResponseEntity.ok(ApiResponse.success(response, "Voucher status updated"));
    }
}
