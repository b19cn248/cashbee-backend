package com.cashbee.presentation.controller;

import com.cashbee.application.dto.wallet.AddPendingBalanceCommand;
import com.cashbee.application.dto.wallet.ConfirmPendingBalanceCommand;
import com.cashbee.application.dto.wallet.DeductBalanceCommand;
import com.cashbee.application.dto.wallet.LockBalanceCommand;
import com.cashbee.application.dto.wallet.UnlockBalanceCommand;
import com.cashbee.application.dto.wallet.WalletResponse;
import com.cashbee.application.usecase.wallet.AddPendingBalanceUseCase;
import com.cashbee.application.usecase.wallet.ConfirmPendingBalanceUseCase;
import com.cashbee.application.usecase.wallet.DeductBalanceUseCase;
import com.cashbee.application.usecase.wallet.GetUserWalletUseCase;
import com.cashbee.application.usecase.wallet.LockBalanceUseCase;
import com.cashbee.application.usecase.wallet.UnlockBalanceUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Wallet operations.
 *
 * Endpoints:
 * - GET /api/wallets/user/{userId} - Get wallet by user ID
 * - POST /api/wallets/pending - Add pending balance
 * - POST /api/wallets/confirm - Confirm pending balance
 * - POST /api/wallets/lock - Lock balance for payout
 * - POST /api/wallets/unlock - Unlock balance (cancel payout)
 * - POST /api/wallets/deduct - Deduct locked balance (complete payout)
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Management", description = "Wallet management endpoints")
public class WalletController {

    private final GetUserWalletUseCase getUserWalletUseCase;
    private final AddPendingBalanceUseCase addPendingBalanceUseCase;
    private final ConfirmPendingBalanceUseCase confirmPendingBalanceUseCase;
    private final LockBalanceUseCase lockBalanceUseCase;
    private final UnlockBalanceUseCase unlockBalanceUseCase;
    private final DeductBalanceUseCase deductBalanceUseCase;

    /**
     * Get wallet by user ID.
     *
     * @param userId User ID
     * @return Wallet response
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID",
               description = "Retrieve wallet information for a specific user")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByUserId(
        @PathVariable Long userId) {

        log.info("API: Getting wallet for user: userId={}", userId);

        WalletResponse wallet = getUserWalletUseCase.execute(userId);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet));
    }

    /**
     * Add pending balance to wallet.
     *
     * Used when a new cashback is earned but not yet confirmed.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @PostMapping("/pending")
    @Operation(summary = "Add pending balance",
               description = "Add pending balance to user wallet (cashback not yet confirmed)")
    public ResponseEntity<ApiResponse<WalletResponse>> addPendingBalance(
        @Valid @RequestBody AddPendingBalanceCommand command) {

        log.info("API: Adding pending balance: userId={}, amount={}",
            command.getUserId(), command.getAmount());

        WalletResponse wallet = addPendingBalanceUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet, "Pending balance added successfully"));
    }

    /**
     * Confirm pending balance and make it available.
     *
     * Used when pending cashback is confirmed by the affiliate network.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm pending balance",
               description = "Confirm pending balance and move it to available balance")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmPendingBalance(
        @Valid @RequestBody ConfirmPendingBalanceCommand command) {

        log.info("API: Confirming pending balance: userId={}, amount={}",
            command.getUserId(), command.getAmount());

        WalletResponse wallet = confirmPendingBalanceUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet, "Pending balance confirmed successfully"));
    }

    /**
     * Lock balance for payout request.
     *
     * Used when user requests a payout - locks the balance to prevent spending.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @PostMapping("/lock")
    @Operation(summary = "Lock balance for payout",
               description = "Lock balance when user requests payout (prevents spending)")
    public ResponseEntity<ApiResponse<WalletResponse>> lockBalance(
        @Valid @RequestBody LockBalanceCommand command) {

        log.info("API: Locking balance: userId={}, amount={}",
            command.getUserId(), command.getAmount());

        WalletResponse wallet = lockBalanceUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet, "Balance locked successfully"));
    }

    /**
     * Unlock balance (cancel payout).
     *
     * Used when payout request is rejected or cancelled - returns locked balance to available.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @PostMapping("/unlock")
    @Operation(summary = "Unlock balance",
               description = "Unlock balance when payout is cancelled/rejected (returns to available balance)")
    public ResponseEntity<ApiResponse<WalletResponse>> unlockBalance(
        @Valid @RequestBody UnlockBalanceCommand command) {

        log.info("API: Unlocking balance: userId={}, amount={}",
            command.getUserId(), command.getAmount());

        WalletResponse wallet = unlockBalanceUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet, "Balance unlocked successfully"));
    }

    /**
     * Deduct locked balance (complete payout).
     *
     * Used when payout is successfully paid - permanently removes locked balance and updates totalWithdrawn.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @PostMapping("/deduct")
    @Operation(summary = "Deduct locked balance",
               description = "Deduct locked balance when payout is completed (permanent removal)")
    public ResponseEntity<ApiResponse<WalletResponse>> deductBalance(
        @Valid @RequestBody DeductBalanceCommand command) {

        log.info("API: Deducting locked balance: userId={}, amount={}",
            command.getUserId(), command.getAmount());

        WalletResponse wallet = deductBalanceUseCase.execute(command);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(wallet, "Locked balance deducted successfully"));
    }
}
