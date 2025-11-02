package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.dto.payout.RejectPayoutRequestCommand;
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
 * Use case for rejecting payout request.
 *
 * Business Scenario:
 * Admin reviews payout request and rejects it with reason.
 * Balance must be unlocked (returned to available).
 *
 * Flow:
 * 1. Find payout request by ID
 * 2. Find wallet by ID
 * 3. Validate reason is provided
 * 4. Validate status is REQUESTED
 * 5. Reject payout (status → REJECTED)
 * 6. Unlock balance (return to available)
 * 7. Save both payout request and wallet
 * 8. Return response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RejectPayoutRequestUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final UserWalletRepository walletRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute reject payout request operation.
     *
     * @param command Reject payout request command
     * @return Updated payout request response
     * @throws NullPointerException if command is null
     * @throws NotFoundException if payout request or wallet not found
     * @throws IllegalArgumentException if reason is null or blank
     * @throws IllegalStateException if status is not REQUESTED
     */
    @Transactional
    public PayoutRequestResponse execute(RejectPayoutRequestCommand command) {
        log.info("Rejecting payout request: payoutRequestId={}, adminId={}, reason={}",
                command.getPayoutRequestId(), command.getAdminId(), command.getReason());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("RejectPayoutRequestCommand cannot be null");
        }

        // Step 2: Validate rejection reason is provided
        if (command.getReason() == null || command.getReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required and cannot be blank");
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

        // Step 5: Reject payout (domain method handles state validation)
        payoutRequest.reject(command.getReason(), command.getAdminId());

        // Step 6: Unlock balance (return to available)
        wallet.unlockBalance(payoutRequest.getAmount());

        // Step 7: Save both entities
        walletRepository.save(wallet);
        PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

        log.info("Payout request rejected successfully: id={}, userId={}, adminId={}, balanceUnlocked={}",
                savedPayoutRequest.getId(),
                savedPayoutRequest.getUserId(),
                command.getAdminId(),
                payoutRequest.getAmount());

        // Step 8: Map to response DTO
        return payoutRequestMapper.toResponse(savedPayoutRequest);
    }
}
