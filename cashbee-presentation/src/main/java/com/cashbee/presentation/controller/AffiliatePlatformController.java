package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.application.dto.affiliate.UpdatePlatformConfigRequest;
import com.cashbee.application.usecase.affiliate.GetAffiliatePlatformsUseCase;
import com.cashbee.application.usecase.affiliate.GetPlatformByCodeUseCase;
import com.cashbee.application.usecase.affiliate.UpdatePlatformConfigUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Affiliate Platform operations.
 *
 * Endpoints:
 * - GET /api/admin/platforms - Get all platforms
 * - GET /api/admin/platforms/code/{code} - Get platform by code
 *
 * Admin-only endpoints for managing affiliate platforms.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/platforms")
@RequiredArgsConstructor

@Tag(name = "Affiliate Platform Management", description = "Manage affiliate platforms (Shopee, Lazada, etc.)")
public class AffiliatePlatformController {
    private static final Logger log = LoggerFactory.getLogger(AffiliatePlatformController.class);

    private final GetAffiliatePlatformsUseCase getPlatformsUseCase;
    private final GetPlatformByCodeUseCase getPlatformByCodeUseCase;
    private final UpdatePlatformConfigUseCase updatePlatformConfigUseCase;

    /**
     * Get all affiliate platforms.
     *
     * Returns list of all platforms (both active and inactive).
     * Admin use this to view and manage platforms.
     *
     * @return list of platforms
     */
    @GetMapping
    @Operation(
        summary = "Get all affiliate platforms",
        description = "Retrieve list of all affiliate platforms (Shopee, Lazada, TikTok, etc.)"
    )
    public ResponseEntity<ApiResponse<List<AffiliatePlatformResponse>>> getAllPlatforms() {
        log.info("API: Getting all affiliate platforms");

        var platforms = getPlatformsUseCase.execute();

        return ResponseEntity.ok(
            ApiResponse.success(platforms, "Retrieved " + platforms.size() + " platforms")
        );
    }

    /**
     * Get platform by code.
     *
     * Returns platform details for specific code (e.g., "shopee").
     *
     * @param code platform code
     * @return platform details
     */
    @GetMapping("/code/{code}")
    @Operation(
        summary = "Get platform by code",
        description = "Retrieve affiliate platform details by code (e.g., 'shopee', 'lazada')"
    )
    public ResponseEntity<ApiResponse<AffiliatePlatformResponse>> getPlatformByCode(
        @PathVariable String code) {

        log.info("API: Getting platform by code: {}", code);

        var platform = getPlatformByCodeUseCase.execute(code);

        return ResponseEntity.ok(
            ApiResponse.success(platform, "Platform retrieved successfully")
        );
    }

    /**
     * Update platform affiliate configuration.
     *
     * Admin use this to configure affiliate ID and link template.
     *
     * @param code platform code
     * @param request update request
     * @return updated platform
     */
    @PutMapping("/code/{code}/config")
    @Operation(
        summary = "Update platform affiliate configuration",
        description = "Update affiliate ID and link template for platform"
    )
    public ResponseEntity<ApiResponse<AffiliatePlatformResponse>> updatePlatformConfig(
        @PathVariable String code,
        @Valid @RequestBody UpdatePlatformConfigRequest request) {

        log.info("API: Updating platform config for: {}", code);

        var platform = updatePlatformConfigUseCase.execute(code, request);

        return ResponseEntity.ok(
            ApiResponse.success(platform, "Platform configuration updated successfully")
        );
    }
}
