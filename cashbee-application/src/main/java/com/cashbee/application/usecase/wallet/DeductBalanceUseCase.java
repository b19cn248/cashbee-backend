package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.DeductBalanceCommand;
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
 * Use case for deducting locked balance (completing payout).
 *
 * Business Scenario:
 * When a payout is successfully paid to user's bank/momo account,
 * we need to permanently remove the locked balance and update statistics.
 *
 * Flow:
 * 1. User requests payout of 500 → locked balance = 500
 * 2. Admin approves payout
 * 3. Payment processor pays 500 to user's account
 * 4. System deducts locked balance (locked: 500 → 0)
 * 5. System updates totalWithdrawn (totalWithdrawn: +500)
 *
 * This ensures:
 * - Money is permanently removed from wallet after successful payout
 * - Accurate statistics tracking
 * - Audit trail of withdrawals
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeductBalanceUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Execute deduct balance operation.
     *
     * @param command Deduct balance command
     * @return Updated wallet response
     * @throws NotFoundException if wallet not found
     * @throws IllegalArgumentException if insufficient locked balance or invalid amount
     */
    @Transactional
    public WalletResponse execute(DeductBalanceCommand command) {
        log.info("Deducting locked balance: userId={}, amount={}, description={}",
                command.getUserId(), command.getAmount(), command.getDescription());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("DeductBalanceCommand cannot be null");
        }

        // Step 2: Find wallet by user ID
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found for user ID: " + command.getUserId()
                ));

        // Step 3: Deduct locked balance using domain logic
        // This will throw IllegalArgumentException if:
        // - amount is invalid (negative/zero)
        // - insufficient locked balance
        wallet.deductLockedBalance(command.getAmount());

        // Step 4: Save updated wallet
        UserWallet savedWallet = walletRepository.save(wallet);

        log.info("Locked balance deducted successfully: userId={}, newLockedBalance={}, totalWithdrawn={}",
                savedWallet.getUserId(),
                savedWallet.getLockedBalance(),
                savedWallet.getTotalWithdrawn());

        // Step 5: Map to response DTO
        return walletMapper.toResponse(savedWallet);
    }
}
