package com.cashbee.presentation.controller;

import com.cashbee.application.dto.transaction.GetTransactionHistoryQuery;
import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.application.usecase.transaction.GetTransactionHistoryUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Transaction operations.
 *
 * Endpoints:
 * - GET /api/transactions/user/{userId} - Get transaction history with pagination
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor

@Tag(name = "Transaction Management", description = "Transaction history and audit trail endpoints")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    /**
     * Get transaction history for a user with pagination.
     *
     * Returns list of transactions sorted by createdAt DESC (newest first).
     *
     * Query Parameters:
     * - page: Page number (0-indexed), default = 0
     * - size: Page size, default = 20
     *
     * Example: GET /api/transactions/user/100?page=0&size=10
     *
     * @param userId User ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return List of transaction responses (paginated)
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get transaction history",
            description = "Get paginated transaction history for a user (newest first)"
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (transactions per page)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("API: Getting transaction history: userId={}, page={}, size={}", userId, page, size);

        // Build query
        GetTransactionHistoryQuery query = GetTransactionHistoryQuery.builder()
                .userId(userId)
                .page(page)
                .size(size)
                .build();

        // Execute use case
        List<TransactionResponse> transactions = getTransactionHistoryUseCase.execute(query);

        log.info("API: Found {} transactions for user {} (page {})", transactions.size(), userId, page);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(transactions));
    }
}
