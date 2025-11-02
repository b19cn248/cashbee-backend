package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CancelPayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.port.PayoutRequestMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for cancelling payout request.
 *
 * Business Scenario:
 * User or admin cancels payout request before it's processed.
 * Common reasons: user changed mind, duplicate request, admin decision.
 *
 * Flow:
 * 1. Find payout request by ID
 * 2. Find wallet by ID
 * 3. Validate reason is provided
 * 4. Validate status is REQUESTED
 * 5. Cancel payout (status → CANCELLED)
 * 6. Unlock balance (return to available)
 * 7. Save both payout request and wallet
 * 8. Return response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelPayoutRequestUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final UserWalletRepository walletRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute cancel payout request operation.
     *
     * @param command Cancel payout request command
     * @return Updated payout request response
     * @throws NullPointerException if command is null
     * @throws NotFoundException if payout request or wallet not found
     * @throws IllegalArgumentException if reason is null or blank
     * @throws IllegalStateException if status is not REQUESTED
     */
    @Transactional
    public PayoutRequestResponse execute(CancelPayoutRequestCommand command) {
        log.info("Cancelling payout request: payoutRequestId={}, userId={}, reason={}",
                command.getPayoutRequestId(), command.getUserId(), command.getReason());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("CancelPayoutRequestCommand cannot be null");
        }

        // Step 2: Validate cancellation reason is provided
        if (command.getReason() == null || command.getReason().isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required and cannot be blank");
        }

        // Step 3: Find payout request by ID
        PayoutRequest payoutRequest = payoutRequestRepository.findById(command.getPayoutRequestId())
                .orElseThrow(() -> NotFoundException.of(
                        "PAYOUT_NOT_FOUND",
                        "Payout request not found with ID: " + command.getPayoutRequestId()
                ));

        // Step 4: Find wallet by ID (to unlock balance)
        UserWallet wallet = walletRepository.findById(payoutRequest.getWalletId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found with ID: " + payoutRequest.getWalletId()
                ));

        // Step 5: Cancel payout (domain method handles state validation)
        payoutRequest.cancel(command.getReason());

        // Step 6: Unlock balance (return to available)
        wallet.unlockBalance(payoutRequest.getAmount());

        // Step 7: Save both entities
        walletRepository.save(wallet);
        PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

        log.info("Payout request cancelled successfully: id={}, userId={}, balanceUnlocked={}",
                savedPayoutRequest.getId(),
                savedPayoutRequest.getUserId(),
                payoutRequest.getAmount());

        // Step 8: Map to response DTO
        return payoutRequestMapper.toResponse(savedPayoutRequest);
    }
}
