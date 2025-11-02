package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.CompletePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.port.PayoutRequestMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.model.Transaction;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.PayoutRequestRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use case for completing payout request.
 *
 * Business Scenario:
 * Admin has successfully transferred money and marks payout as completed.
 * This is the final step in the payout flow.
 *
 * Flow:
 * 1. Find payout request by ID
 * 2. Find wallet by ID
 * 3. Validate status is PROCESSING
 * 4. Complete payout (status → PAID)
 * 5. Deduct locked balance (move to totalWithdrawn)
 * 6. Create transaction record for audit trail
 * 7. Save payout request and wallet
 * 8. Return response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompletePayoutRequestUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute complete payout request operation.
     *
     * @param command Complete payout request command
     * @return Updated payout request response
     * @throws NullPointerException if command is null
     * @throws NotFoundException if payout request or wallet not found
     * @throws IllegalStateException if status is not PROCESSING
     */
    @Transactional
    public PayoutRequestResponse execute(CompletePayoutRequestCommand command) {
        log.info("Completing payout request: payoutRequestId={}, adminId={}, reference={}",
                command.getPayoutRequestId(), command.getAdminId(), command.getTransactionReference());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("CompletePayoutRequestCommand cannot be null");
        }

        // Step 2: Find payout request by ID
        PayoutRequest payoutRequest = payoutRequestRepository.findById(command.getPayoutRequestId())
                .orElseThrow(() -> NotFoundException.of(
                        "PAYOUT_NOT_FOUND",
                        "Payout request not found with ID: " + command.getPayoutRequestId()
                ));

        // Step 3: Find wallet by ID (to deduct locked balance)
        UserWallet wallet = walletRepository.findById(payoutRequest.getWalletId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found with ID: " + payoutRequest.getWalletId()
                ));

        // Step 4: Complete payout (domain method handles state validation)
        payoutRequest.complete();

        // Step 5: Deduct locked balance (move to totalWithdrawn)
        BigDecimal balanceBefore = wallet.getLockedBalance();
        wallet.deductLockedBalance(payoutRequest.getAmount());
        BigDecimal balanceAfter = wallet.getLockedBalance();

        log.debug("Locked balance deducted: userId={}, amount={}, balanceBefore={}, balanceAfter={}",
                wallet.getUserId(), payoutRequest.getAmount(), balanceBefore, balanceAfter);

        // Step 6: Create transaction record for audit trail
        String description = String.format("Payout completed - %s - %s",
                payoutRequest.getPayoutMethod(),
                command.getTransactionReference() != null ? command.getTransactionReference() : "No reference");

        Transaction transaction = Transaction.builder()
                .userId(wallet.getUserId())
                .walletId(wallet.getId())
                .type(TransactionType.WITHDRAW)
                .amount(payoutRequest.getAmount())
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transaction.validate();
        transactionRepository.save(transaction);

        log.debug("Transaction record created: userId={}, type=PAYOUT, amount={}",
                wallet.getUserId(), payoutRequest.getAmount());

        // Step 7: Save both entities
        walletRepository.save(wallet);
        PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

        log.info("Payout request completed successfully: id={}, userId={}, amount={}, totalWithdrawn={}",
                savedPayoutRequest.getId(),
                savedPayoutRequest.getUserId(),
                payoutRequest.getAmount(),
                wallet.getTotalWithdrawn());

        // Step 8: Map to response DTO
        return payoutRequestMapper.toResponse(savedPayoutRequest);
    }
}
