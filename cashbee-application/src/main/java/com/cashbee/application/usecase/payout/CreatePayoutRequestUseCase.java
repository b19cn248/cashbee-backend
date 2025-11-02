package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CreatePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.port.PayoutRequestMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.PayoutStatus;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for creating payout request.
 *
 * Business Scenario:
 * User requests payout (withdrawal) from their wallet.
 * System validates balance, locks the amount, and creates payout request for admin review.
 *
 * Flow:
 * 1. Validate user has wallet
 * 2. Validate sufficient available balance
 * 3. Validate amount meets minimum requirement (50,000 VND)
 * 4. Lock balance (move from available to locked)
 * 5. Create payout request with status REQUESTED
 * 6. Return payout request response
 *
 * This ensures:
 * - User cannot spend money that is being withdrawn
 * - Admin can review and approve/reject payout
 * - Audit trail of all payout requests
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePayoutRequestUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final UserWalletRepository walletRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute create payout request operation.
     *
     * @param command Create payout request command
     * @return Created payout request response
     * @throws NullPointerException if command is null
     * @throws NotFoundException if wallet not found
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public PayoutRequestResponse execute(CreatePayoutRequestCommand command) {
        log.info("Creating payout request: userId={}, amount={}, method={}",
                command.getUserId(), command.getAmount(), command.getPayoutMethod());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("CreatePayoutRequestCommand cannot be null");
        }

        // Step 2: Find user's wallet
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found for user ID: " + command.getUserId()
                ));

        // Step 3: Validate sufficient balance
        if (wallet.getBalance().compareTo(command.getAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Available: " + wallet.getBalance() +
                            ", Requested: " + command.getAmount()
            );
        }

        // Step 4: Lock balance for payout
        wallet.lockBalance(command.getAmount());

        // Step 5: Save wallet with locked balance
        walletRepository.save(wallet);

        log.info("Balance locked for payout: userId={}, amount={}, newLockedBalance={}",
                command.getUserId(), command.getAmount(), wallet.getLockedBalance());

        // Step 6: Build payout request domain model
        PayoutRequest payoutRequest = PayoutRequest.builder()
                .userId(command.getUserId())
                .walletId(wallet.getId())
                .amount(command.getAmount())
                .payoutMethod(command.getPayoutMethod())
                .accountNumber(command.getAccountNumber())
                .accountName(command.getAccountName())
                .bankName(command.getBankName())
                .status(PayoutStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        // Step 7: Validate payout request
        payoutRequest.validate();

        // Step 8: Save payout request
        PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

        log.info("Payout request created successfully: id={}, userId={}, amount={}",
                savedPayoutRequest.getId(),
                savedPayoutRequest.getUserId(),
                savedPayoutRequest.getAmount());

        // Step 9: Map to response DTO
        return payoutRequestMapper.toResponse(savedPayoutRequest);
    }
}
