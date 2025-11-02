package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.CreateTrackingLinkRequest;
import com.cashbee.application.dto.affiliate.TrackingLinkResponse;
import com.cashbee.application.usecase.affiliate.CreateTrackingLinkUseCase;
import com.cashbee.application.usecase.affiliate.HandleClickRedirectUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

/**
 * REST Controller for Affiliate Tracking operations.
 *
 * Endpoints:
 * - POST /api/affiliate/tracking/create-link - Create tracking link
 * - GET /api/affiliate/tracking/redirect/{clickId} - Handle click redirect
 *
 * User endpoints for creating tracking links and clicking them.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/affiliate/tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Affiliate Tracking", description = "Create tracking links and handle click redirects")
public class AffiliateTrackingController {

    private final CreateTrackingLinkUseCase createTrackingLinkUseCase;
    private final HandleClickRedirectUseCase handleClickRedirectUseCase;

    /**
     * Create affiliate tracking link.
     *
     * User provides Shopee product URL, system generates affiliate tracking link.
     *
     * Flow:
     * 1. User copies Shopee product URL
     * 2. User pastes URL into CashBee app
     * 3. System generates tracking link
     * 4. User clicks tracking link → redirects to Shopee with tracking
     *
     * @param request Request containing Shopee URL and user ID
     * @return Generated tracking link
     */
    @PostMapping("/create-link")
    @Operation(
        summary = "Create affiliate tracking link",
        description = "Convert Shopee product URL to affiliate tracking link that earns cashback"
    )
    public ResponseEntity<ApiResponse<TrackingLinkResponse>> createTrackingLink(
        @Valid @RequestBody CreateTrackingLinkRequest request) {

        log.info("API: Creating tracking link for user {} with URL: {}",
            request.getUserId(), request.getShopeeUrl());

        TrackingLinkResponse response = createTrackingLinkUseCase.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(response, "Tracking link created successfully")
        );
    }

    /**
     * Handle click redirect.
     *
     * When user clicks tracking link, this endpoint:
     * 1. Records the click event (status → CLICKED)
     * 2. Redirects user to actual Shopee affiliate URL
     *
     * This is a GET endpoint that performs HTTP 302 redirect.
     *
     * @param clickId Click ID from URL path
     * @return Redirect to Shopee affiliate URL
     */
    @GetMapping("/redirect/{clickId}")
    @Operation(
        summary = "Handle click redirect",
        description = "Track click event and redirect user to Shopee with affiliate tracking"
    )
    public RedirectView handleClickRedirect(@PathVariable Long clickId) {
        log.info("API: Handling click redirect for click ID: {}", clickId);

        // Track the click and get redirect URL
        String redirectUrl = handleClickRedirectUseCase.execute(clickId);

        log.info("API: Redirecting click {} to: {}", clickId, redirectUrl);

        // Perform HTTP 302 redirect
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(redirectUrl);
        redirectView.setStatusCode(HttpStatus.FOUND);  // 302 redirect
        return redirectView;
    }

    /**
     * Alternative redirect endpoint using tracking code.
     *
     * Allows redirect by tracking code instead of click ID.
     * Useful if frontend stores tracking code instead of click ID.
     *
     * @param trackingCode Tracking code (e.g., CB1_100_20251101160530)
     * @return Redirect to Shopee affiliate URL
     */
    @GetMapping("/redirect/code/{trackingCode}")
    @Operation(
        summary = "Handle click redirect by tracking code",
        description = "Track click event and redirect using tracking code"
    )
    public RedirectView handleClickRedirectByCode(@PathVariable String trackingCode) {
        log.info("API: Handling click redirect for tracking code: {}", trackingCode);

        // Track the click and get redirect URL
        String redirectUrl = handleClickRedirectUseCase.executeByTrackingCode(trackingCode);

        log.info("API: Redirecting tracking code {} to: {}", trackingCode, redirectUrl);

        // Perform HTTP 302 redirect
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(redirectUrl);
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    /**
     * Get tracking link details (for testing/debugging).
     *
     * Returns the tracking URL without performing redirect.
     * Useful for testing or showing link preview to user.
     *
     * @param clickId Click ID
     * @return Tracking link details
     */
    @GetMapping("/link/{clickId}")
    @Operation(
        summary = "Get tracking link details",
        description = "Get tracking link information without redirecting (for testing)"
    )
    public ResponseEntity<ApiResponse<String>> getTrackingLink(@PathVariable Long clickId) {
        log.info("API: Getting tracking link for click ID: {}", clickId);

        String trackingUrl = handleClickRedirectUseCase.execute(clickId);

        return ResponseEntity.ok(
            ApiResponse.success(trackingUrl, "Tracking link retrieved successfully")
        );
    }
}
