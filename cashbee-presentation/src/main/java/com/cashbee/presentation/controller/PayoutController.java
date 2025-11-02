package com.cashbee.presentation.controller;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.payout.*;
import com.cashbee.application.usecase.payout.*;
import com.cashbee.domain.enums.PayoutStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payout Management.
 *
 * Provides endpoints for:
 * - Users: Create payout requests, view their own payouts
 * - Admins: View all payouts, approve/reject requests
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payout Management", description = "APIs for managing payout requests")
public class PayoutController {

    private final CreatePayoutRequestUseCase createPayoutRequestUseCase;
    private final ApprovePayoutRequestUseCase approvePayoutRequestUseCase;
    private final RejectPayoutRequestUseCase rejectPayoutRequestUseCase;
    private final CompletePayoutRequestUseCase completePayoutRequestUseCase;
    private final CancelPayoutRequestUseCase cancelPayoutRequestUseCase;
    private final GetPayoutRequestsUseCase getPayoutRequestsUseCase;

    // ============================================================
    // USER ENDPOINTS
    // ============================================================

    /**
     * Create a new payout request.
     *
     * Users can request to withdraw money from their wallet.
     * Minimum amount: 50,000 VND.
     * Balance will be locked until payout is processed.
     *
     * @param command Create payout request command
     * @return Created payout request response
     */
    @PostMapping("/request")
    @Operation(
            summary = "Create payout request",
            description = "User requests to withdraw money from wallet. Minimum 50,000 VND. Balance will be locked."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Payout request created successfully",
                    content = @Content(schema = @Schema(implementation = PayoutRequestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (insufficient balance, below minimum, etc.)"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PayoutRequestResponse> createPayoutRequest(
            @RequestBody @Parameter(description = "Payout request details", required = true)
            CreatePayoutRequestCommand command
    ) {
        log.info("API: Creating payout request for userId={}, amount={}",
                command.getUserId(), command.getAmount());

        PayoutRequestResponse response = createPayoutRequestUseCase.execute(command);

        log.info("API: Payout request created successfully: id={}, userId={}",
                response.getId(), response.getUserId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get payout requests for a specific user.
     *
     * Returns paginated list of payout requests ordered by requested date (newest first).
     *
     * @param userId User ID
     * @param status Optional status filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated payout requests
     */
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user's payout requests",
            description = "Retrieve paginated list of payout requests for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout requests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PageResponse<PayoutRequestResponse>> getUserPayoutRequests(
            @PathVariable @Parameter(description = "User ID", required = true) Long userId,
            @RequestParam(required = false) @Parameter(description = "Filter by status") PayoutStatus status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size
    ) {
        log.info("API: Getting payout requests for userId={}, status={}, page={}, size={}",
                userId, status, page, size);

        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(userId)
                .status(status)
                .page(page)
                .size(size)
                .build();

        PageResponse<PayoutRequestResponse> response = getPayoutRequestsUseCase.execute(query);

        log.info("API: Found {} payout requests for user {} (page {}/{})",
                response.getTotalElements(), userId, response.getPage() + 1, response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ADMIN ENDPOINTS
    // ============================================================

    /**
     * Get all payout requests (Admin only).
     *
     * Returns paginated list of all payout requests with optional filtering.
     * Ordered by requested date (newest first).
     *
     * @param userId Optional user ID filter
     * @param status Optional status filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated payout requests
     */
    @GetMapping("/admin/all")
    @Operation(
            summary = "[ADMIN] Get all payout requests",
            description = "Retrieve paginated list of all payout requests with optional filters"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout requests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PageResponse<PayoutRequestResponse>> getAllPayoutRequests(
            @RequestParam(required = false) @Parameter(description = "Filter by user ID") Long userId,
            @RequestParam(required = false) @Parameter(description = "Filter by status") PayoutStatus status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-indexed)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size
    ) {
        log.info("API: [ADMIN] Getting all payout requests: userId={}, status={}, page={}, size={}",
                userId, status, page, size);

        GetPayoutRequestsQuery query = GetPayoutRequestsQuery.builder()
                .userId(userId)
                .status(status)
                .page(page)
                .size(size)
                .build();

        PageResponse<PayoutRequestResponse> response = getPayoutRequestsUseCase.execute(query);

        log.info("API: [ADMIN] Found {} payout requests (page {}/{})",
                response.getTotalElements(), response.getPage() + 1, response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Approve a payout request (Admin only).
     *
     * Changes status from REQUESTED → PROCESSING.
     * Balance remains locked until payout is completed.
     *
     * @param payoutRequestId Payout request ID
     * @param adminId Admin ID who is approving
     * @return Updated payout request response
     */
    @PutMapping("/admin/{payoutRequestId}/approve")
    @Operation(
            summary = "[ADMIN] Approve payout request",
            description = "Approve a payout request. Status changes from REQUESTED to PROCESSING."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout request approved successfully",
                    content = @Content(schema = @Schema(implementation = PayoutRequestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (already processed, etc.)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Payout request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PayoutRequestResponse> approvePayoutRequest(
            @PathVariable @Parameter(description = "Payout request ID", required = true) Long payoutRequestId,
            @RequestParam @Parameter(description = "Admin ID", required = true) String adminId
    ) {
        log.info("API: [ADMIN] Approving payout request: id={}, adminId={}", payoutRequestId, adminId);

        ApprovePayoutRequestCommand command = ApprovePayoutRequestCommand.builder()
                .payoutRequestId(payoutRequestId)
                .adminId(adminId)
                .build();

        PayoutRequestResponse response = approvePayoutRequestUseCase.execute(command);

        log.info("API: [ADMIN] Payout request approved: id={}, status={}", response.getId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Reject a payout request (Admin only).
     *
     * Changes status from REQUESTED → REJECTED.
     * Locked balance will be unlocked (returned to available).
     *
     * @param payoutRequestId Payout request ID
     * @param adminId Admin ID who is rejecting
     * @param reason Rejection reason (required)
     * @return Updated payout request response
     */
    @PutMapping("/admin/{payoutRequestId}/reject")
    @Operation(
            summary = "[ADMIN] Reject payout request",
            description = "Reject a payout request with reason. Status changes from REQUESTED to REJECTED. Balance will be unlocked."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout request rejected successfully",
                    content = @Content(schema = @Schema(implementation = PayoutRequestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (already processed, missing reason, etc.)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Payout request or wallet not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PayoutRequestResponse> rejectPayoutRequest(
            @PathVariable @Parameter(description = "Payout request ID", required = true) Long payoutRequestId,
            @RequestParam @Parameter(description = "Admin ID", required = true) String adminId,
            @RequestParam @Parameter(description = "Rejection reason", required = true) String reason
    ) {
        log.info("API: [ADMIN] Rejecting payout request: id={}, adminId={}, reason={}",
                payoutRequestId, adminId, reason);

        RejectPayoutRequestCommand command = RejectPayoutRequestCommand.builder()
                .payoutRequestId(payoutRequestId)
                .adminId(adminId)
                .reason(reason)
                .build();

        PayoutRequestResponse response = rejectPayoutRequestUseCase.execute(command);

        log.info("API: [ADMIN] Payout request rejected: id={}, status={}", response.getId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Complete a payout request (Admin only).
     *
     * Admin marks payout as completed after successfully transferring money.
     * Status changes from PROCESSING → PAID.
     * Locked balance will be deducted (moved to totalWithdrawn).
     * Transaction record will be created for audit trail.
     *
     * @param payoutRequestId Payout request ID
     * @param adminId Admin ID who is completing
     * @param transactionReference External transaction reference (optional)
     * @return Updated payout request response
     */
    @PutMapping("/admin/{payoutRequestId}/complete")
    @Operation(
            summary = "[ADMIN] Complete payout request",
            description = "Mark payout as completed after transferring money. Status changes from PROCESSING to PAID. Balance will be deducted."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout request completed successfully",
                    content = @Content(schema = @Schema(implementation = PayoutRequestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (not in PROCESSING status, etc.)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "Payout request or wallet not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PayoutRequestResponse> completePayoutRequest(
            @PathVariable @Parameter(description = "Payout request ID", required = true) Long payoutRequestId,
            @RequestParam @Parameter(description = "Admin ID", required = true) String adminId,
            @RequestParam(required = false) @Parameter(description = "External transaction reference") String transactionReference
    ) {
        log.info("API: [ADMIN] Completing payout request: id={}, adminId={}, reference={}",
                payoutRequestId, adminId, transactionReference);

        CompletePayoutRequestCommand command = CompletePayoutRequestCommand.builder()
                .payoutRequestId(payoutRequestId)
                .adminId(adminId)
                .transactionReference(transactionReference)
                .build();

        PayoutRequestResponse response = completePayoutRequestUseCase.execute(command);

        log.info("API: [ADMIN] Payout request completed: id={}, status={}", response.getId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a payout request (User or Admin).
     *
     * User can cancel their own payout request if it's still REQUESTED.
     * Status changes from REQUESTED → CANCELLED.
     * Locked balance will be unlocked (returned to available).
     *
     * @param payoutRequestId Payout request ID
     * @param userId User ID who is cancelling
     * @param reason Cancellation reason (required)
     * @return Updated payout request response
     */
    @PutMapping("/{payoutRequestId}/cancel")
    @Operation(
            summary = "Cancel payout request",
            description = "Cancel a payout request. Status changes from REQUESTED to CANCELLED. Balance will be unlocked."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payout request cancelled successfully",
                    content = @Content(schema = @Schema(implementation = PayoutRequestResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (already processed, missing reason, etc.)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User can only cancel their own requests"),
            @ApiResponse(responseCode = "404", description = "Payout request or wallet not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PayoutRequestResponse> cancelPayoutRequest(
            @PathVariable @Parameter(description = "Payout request ID", required = true) Long payoutRequestId,
            @RequestParam @Parameter(description = "User ID", required = true) Long userId,
            @RequestParam @Parameter(description = "Cancellation reason", required = true) String reason
    ) {
        log.info("API: Cancelling payout request: id={}, userId={}, reason={}",
                payoutRequestId, userId, reason);

        CancelPayoutRequestCommand command = CancelPayoutRequestCommand.builder()
                .payoutRequestId(payoutRequestId)
                .userId(userId)
                .reason(reason)
                .build();

        PayoutRequestResponse response = cancelPayoutRequestUseCase.execute(command);

        log.info("API: Payout request cancelled: id={}, status={}", response.getId(), response.getStatus());

        return ResponseEntity.ok(response);
    }
}
