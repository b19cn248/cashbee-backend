package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.CreateTrackingLinkRequest;
import com.cashbee.application.dto.affiliate.EstimateCashbackRequest;
import com.cashbee.application.dto.affiliate.EstimateCashbackResponse;
import com.cashbee.application.dto.affiliate.TrackingLinkResponse;
import com.cashbee.application.usecase.affiliate.CreateTrackingLinkUseCase;
import com.cashbee.application.usecase.affiliate.EstimateCashbackUseCase;
import com.cashbee.application.usecase.affiliate.HandleClickRedirectUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

/**
 * REST Controller for Affiliate Tracking operations.
 *
 * Endpoints:
 * - POST /api/affiliate/tracking/create-link - Create tracking link
 * - POST /api/affiliate/tracking/estimate-cashback - Estimate cashback amount
 * - GET /api/affiliate/tracking/redirect/{clickId} - Handle click redirect
 *
 * User endpoints for creating tracking links and clicking them.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/affiliate/tracking")
@RequiredArgsConstructor

@Tag(name = "Affiliate Tracking", description = "Create tracking links and handle click redirects")
public class AffiliateTrackingController {
    private static final Logger log = LoggerFactory.getLogger(AffiliateTrackingController.class);

    private final CreateTrackingLinkUseCase createTrackingLinkUseCase;
    private final EstimateCashbackUseCase estimateCashbackUseCase;
    private final HandleClickRedirectUseCase handleClickRedirectUseCase;
    private final SecurityUtils securityUtils;

    /**
     * Create affiliate tracking link.
     *
     * SECURITY UPDATE:
     * - This endpoint now REQUIRES authentication (JWT token)
     * - User ID is extracted from JWT token, NOT from request body
     * - This prevents users from creating tracking links for other users
     *
     * User provides Shopee product URL, system generates affiliate tracking link.
     *
     * Flow:
     * 1. User logs in → receives JWT token from Keycloak
     * 2. User copies Shopee product URL
     * 3. User pastes URL into CashBee app
     * 4. Frontend sends request with JWT token in Authorization header
     * 5. Backend extracts user ID from JWT token (secure, cannot be forged)
     * 6. System generates tracking link for authenticated user
     * 7. User clicks tracking link → redirects to Shopee with tracking
     *
     * @param request Request containing Shopee URL (NO userId - extracted from JWT)
     * @param jwt JWT token injected by Spring Security (contains authenticated user info)
     * @return Generated tracking link
     */
    @PostMapping("/create-link")
    @Operation(
        summary = "Create affiliate tracking link",
        description = "Convert Shopee product URL to affiliate tracking link that earns cashback. Requires authentication."
    )
    public ResponseEntity<ApiResponse<TrackingLinkResponse>> createTrackingLink(
        @Valid @RequestBody CreateTrackingLinkRequest request,
        @AuthenticationPrincipal Jwt jwt) {

        // Extract user ID from JWT token (secure - cannot be forged by client)
        Long userId = securityUtils.getCurrentUserId(jwt);

        log.info("API: Creating tracking link for authenticated user {} with URL: {}",
            userId, request.getShopeeUrl());

        // Pass userId from token to use case
        TrackingLinkResponse response = createTrackingLinkUseCase.execute(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(response, "Tracking link created successfully")
        );
    }

    /**
     * Estimate cashback amount for a Shopee product.
     *
     * REQUIRES AUTHENTICATION - user must be logged in.
     *
     * Formula:
     * - commissionRate = sellerCommissionRate + shopeeCommissionRate
     * - estimatedCashback = commissionRate * price
     *
     * @param request Request containing Shopee URL
     * @param jwt JWT token from authenticated user (validates user is logged in)
     * @return Estimated cashback amount and product details
     */
    @PostMapping("/estimate-cashback")
    @Operation(
        summary = "Estimate cashback amount",
        description = "Get estimated cashback amount for a Shopee product. Requires authentication."
    )
    public ResponseEntity<ApiResponse<EstimateCashbackResponse>> estimateCashback(
        @Valid @RequestBody EstimateCashbackRequest request,
        @AuthenticationPrincipal Jwt jwt) {

        log.info("API: Estimating cashback for URL: {}", request.getShopeeUrl());

        EstimateCashbackResponse response = estimateCashbackUseCase.execute(request);

        return ResponseEntity.ok(
            ApiResponse.success(response, "Cashback estimated successfully")
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
