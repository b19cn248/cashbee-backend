package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.ConfirmPendingBalanceCommand;
import com.cashbee.application.dto.wallet.WalletResponse;
import com.cashbee.application.port.WalletMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for confirming pending balance and making it available.
 *
 * This happens when:
 * - The affiliate network confirms the purchase
 * - The waiting period has passed
 * - The order is not cancelled or returned
 *
 * Business Rules:
 * - Amount must not exceed current pending balance
 * - Confirmed amount moves from pending to available balance
 * - User can now withdraw the confirmed amount
 * - totalEarned is updated to reflect lifetime earnings
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmPendingBalanceUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Confirm pending balance and make it available.
     *
     * @param command Command containing userId and amount to confirm
     * @return Updated wallet response
     * @throws NotFoundException if wallet doesn't exist
     * @throws com.cashbee.common.exception.InsufficientBalanceException if insufficient pending balance
     */
    @Transactional
    public WalletResponse execute(ConfirmPendingBalanceCommand command) {
        log.info("Confirming pending balance: userId={}, amount={}, description={}",
            command.getUserId(), command.getAmount(), command.getDescription());

        // Get wallet
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
            .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND",
                "Wallet not found for user: " + command.getUserId()));

        // Confirm pending balance using domain logic
        wallet.confirmPendingBalance(command.getAmount());

        // Save wallet
        wallet = walletRepository.save(wallet);

        log.info("Pending balance confirmed successfully: userId={}, newBalance={}, remainingPending={}",
            command.getUserId(), wallet.getBalance(), wallet.getPendingBalance());

        return walletMapper.toResponse(wallet);
    }
}
