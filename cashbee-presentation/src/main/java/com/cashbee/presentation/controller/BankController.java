package com.cashbee.presentation.controller;

import com.cashbee.application.dto.bank.BankResponse;
import com.cashbee.application.usecase.bank.GetAllBanksUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Bank operations.
 *
 * Endpoints:
 * - GET /api/banks - Get all active banks
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor

@Tag(name = "Bank Management", description = "Bank listing endpoints")
public class BankController {
    private static final Logger log = LoggerFactory.getLogger(BankController.class);

    private final GetAllBanksUseCase getAllBanksUseCase;

    /**
     * Get all active banks.
     *
     * This endpoint returns list of all supported banks that users
     * can select when setting up their bank account information.
     *
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/banks');
     * const banks = response.data;
     * // Display as dropdown: banks.map(b => ({ value: b.bankCode, label: b.bankName }))
     * </pre>
     *
     * @return List of active banks
     */
    @GetMapping
    @Operation(summary = "Get all active banks",
            description = "Retrieve list of all active banks for user selection")
    public ResponseEntity<ApiResponse<List<BankResponse>>> getAllBanks() {
        log.info("API: Getting all active banks");

        List<BankResponse> banks = getAllBanksUseCase.execute();

        log.info("API: Found {} active banks", banks.size());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(banks));
    }
}
