package com.cashbee.presentation.controller;

import com.cashbee.application.dto.cashback.CashbackPolicyResponse;
import com.cashbee.application.usecase.cashback.GetActiveCashbackPolicyUseCase;
import com.cashbee.application.usecase.cashback.GetCashbackPoliciesUseCase;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Cashback Policy operations.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/cashback-policies")
@RequiredArgsConstructor

@Tag(name = "Cashback Policy Management", description = "Manage cashback calculation policies")
public class CashbackPolicyController {
    private static final Logger log = LoggerFactory.getLogger(CashbackPolicyController.class);

    private final GetCashbackPoliciesUseCase getPoliciesUseCase;
    private final GetActiveCashbackPolicyUseCase getActivePolicyUseCase;

    /**
     * Get all cashback policies.
     */
    @GetMapping
    @Operation(
        summary = "Get all cashback policies",
        description = "Retrieve list of all cashback policies (both active and inactive)"
    )
    public ResponseEntity<ApiResponse<List<CashbackPolicyResponse>>> getAllPolicies() {
        log.info("API: Getting all cashback policies");

        var policies = getPoliciesUseCase.execute();

        return ResponseEntity.ok(
            ApiResponse.success(policies, "Retrieved " + policies.size() + " policies")
        );
    }

    /**
     * Get active policy for platform and user level.
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active cashback policy",
        description = "Get active policy for specific platform and user level (used for cashback calculation)"
    )
    public ResponseEntity<ApiResponse<CashbackPolicyResponse>> getActivePolicy(
        @RequestParam Long platformId,
        @RequestParam String userLevel
    ) {
        log.info("API: Getting active policy for platform: {}, userLevel: {}",
            platformId, userLevel);

        var policy = getActivePolicyUseCase.execute(
            platformId,
            UserLevel.valueOf(userLevel.toUpperCase())
        );

        return ResponseEntity.ok(
            ApiResponse.success(policy, "Active policy retrieved successfully")
        );
    }
}
