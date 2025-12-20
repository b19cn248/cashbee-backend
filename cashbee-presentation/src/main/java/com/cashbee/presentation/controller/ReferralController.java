package com.cashbee.presentation.controller;

import com.cashbee.application.dto.referral.*;
import com.cashbee.application.usecase.referral.*;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Referral operations.
 * <p>
 * Endpoints:
 * - POST /api/referral/set-code - Set referral code (become a referee)
 * - GET /api/referral/validate/{code} - Validate a referral code
 * - GET /api/referral/stats - Get referral statistics for current user
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Referral", description = "Referral system endpoints")
public class ReferralController {

    private final SetReferralCodeUseCase setReferralCodeUseCase;
    private final ValidateReferralCodeUseCase validateReferralCodeUseCase;
    private final GetReferralStatsUseCase getReferralStatsUseCase;
    private final SecurityUtils securityUtils;

    /**
     * Set referral code for the current user (become a referee).
     * <p>
     * This endpoint is called when:
     * - User registers with a referral code
     * - User adds bank account and enters referral code
     * <p>
     * Business rules:
     * - User can only set referral code once
     * - Cannot use own referral code (self-referral)
     * - Referral code must belong to an active user
     * <p>
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/referral/set-code', {
     *   method: 'POST',
     *   headers: {
     *     'Authorization': `Bearer ${token}`,
     *     'Content-Type': 'application/json'
     *   },
     *   body: JSON.stringify({ referralCode: 'REFCODE1' })
     * });
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param command Command containing the referral code
     * @return Response with referral setup result
     */
    @PostMapping("/set-code")
    @Operation(
            summary = "Set referral code",
            description = "Set a referral code to become a referee. Can only be done once."
    )
    public ResponseEntity<ApiResponse<SetReferralCodeResponse>> setReferralCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetReferralCodeCommand command) {

        log.info("API: Setting referral code for user");

        String keycloakId = securityUtils.getKeycloakUserId(jwt);
        SetReferralCodeResponse response = setReferralCodeUseCase.execute(keycloakId, command);

        log.info("API: Referral code set successfully");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response, "Referral code applied successfully"));
    }

    /**
     * Validate a referral code.
     * <p>
     * This endpoint is used to check if a referral code is valid
     * before the user submits it. Provides immediate feedback.
     * <p>
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/referral/validate/REFCODE1', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const { valid, referrerName, message } = response.data;
     * </pre>
     *
     * @param code The referral code to validate
     * @return Validation response indicating if code is valid
     */
    @GetMapping("/validate/{code}")
    @Operation(
            summary = "Validate referral code",
            description = "Check if a referral code is valid before submitting"
    )
    public ResponseEntity<ApiResponse<ValidateReferralCodeResponse>> validateReferralCode(
            @Parameter(description = "Referral code to validate")
            @PathVariable String code) {

        log.info("API: Validating referral code: {}", code);

        ValidateReferralCodeResponse response = validateReferralCodeUseCase.execute(code);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * Get referral statistics for the current user.
     * <p>
     * Returns comprehensive referral information including:
     * - User's own referral progress (as referee)
     * - User's referral earnings (as referrer)
     * - Milestone progress and achievements
     * - Commission summary
     * <p>
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/referral/stats', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const stats = response.data;
     * // stats.myReferralCode - share this with friends
     * // stats.totalReferrals - number of people referred
     * // stats.totalCommissionEarned - total earnings from referrals
     * // stats.milestones - progress towards milestones
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @return Referral statistics for the user
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get referral statistics",
            description = "Get referral statistics for the current user including progress, earnings, and milestones"
    )
    public ResponseEntity<ApiResponse<ReferralStatsResponse>> getReferralStats(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("API: Getting referral stats for user");

        String keycloakId = securityUtils.getKeycloakUserId(jwt);
        ReferralStatsResponse response = getReferralStatsUseCase.execute(keycloakId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * Get user's referral code (for sharing).
     * <p>
     * Simplified endpoint to just get the user's referral code.
     *
     * @param jwt JWT token
     * @return User's referral code
     */
    @GetMapping("/my-code")
    @Operation(
            summary = "Get my referral code",
            description = "Get the current user's referral code for sharing with friends"
    )
    public ResponseEntity<ApiResponse<String>> getMyReferralCode(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("API: Getting referral code for user");

        String keycloakId = securityUtils.getKeycloakUserId(jwt);
        ReferralStatsResponse stats = getReferralStatsUseCase.execute(keycloakId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(stats.getMyReferralCode()));
    }
}
