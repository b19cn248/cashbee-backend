package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.UnlockBalanceCommand;
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
 * Use case for unlocking wallet balance.
 *
 * Business Scenario:
 * When a payout request is rejected or cancelled, we need to return
 * the locked money back to available balance so user can use it again.
 *
 * Flow:
 * 1. User requests payout of 500 → locked balance = 500
 * 2. Admin rejects payout
 * 3. System unlocks 500 (locked: 500 → 0, balance: +500)
 * 4. User can request payout again or spend the money
 *
 * This ensures:
 * - Cancelled payouts return money to user
 * - Accounting remains accurate
 * - User experience is good (money not permanently locked)
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnlockBalanceUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Execute unlock balance operation.
     *
     * @param command Unlock balance command
     * @return Updated wallet response
     * @throws NotFoundException if wallet not found
     * @throws IllegalArgumentException if insufficient locked balance or invalid amount
     */
    @Transactional
    public WalletResponse execute(UnlockBalanceCommand command) {
        log.info("Unlocking balance: userId={}, amount={}, description={}",
                command.getUserId(), command.getAmount(), command.getDescription());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("UnlockBalanceCommand cannot be null");
        }

        // Step 2: Find wallet by user ID
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found for user ID: " + command.getUserId()
                ));

        // Step 3: Unlock balance using domain logic
        // This will throw IllegalArgumentException if:
        // - amount is invalid (negative/zero)
        // - insufficient locked balance
        wallet.unlockBalance(command.getAmount());

        // Step 4: Save updated wallet
        UserWallet savedWallet = walletRepository.save(wallet);

        log.info("Balance unlocked successfully: userId={}, newBalance={}, newLockedBalance={}",
                savedWallet.getUserId(),
                savedWallet.getBalance(),
                savedWallet.getLockedBalance());

        // Step 5: Map to response DTO
        return walletMapper.toResponse(savedWallet);
    }
}
