package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.LockBalanceCommand;
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
 * Use case for locking wallet balance.
 *
 * Business Scenario:
 * When a user requests a payout, we need to "lock" that amount
 * so they cannot spend it while the payout is being processed.
 *
 * Flow:
 * 1. User requests payout of 500
 * 2. System locks 500 (balance: 1000 → 500, locked: 0 → 500)
 * 3. Admin reviews and either:
 *    - Approves: deduct locked balance (UnlockBalanceUseCase)
 *    - Rejects: unlock balance back (DeductBalanceUseCase)
 *
 * This ensures:
 * - Money is reserved for payout
 * - User cannot double-spend
 * - Accounting is accurate
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockBalanceUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Execute lock balance operation.
     *
     * @param command Lock balance command
     * @return Updated wallet response
     * @throws NotFoundException if wallet not found
     * @throws com.cashbee.common.exception.InsufficientBalanceException if insufficient balance
     * @throws IllegalArgumentException if amount is invalid (negative or zero)
     */
    @Transactional
    public WalletResponse execute(LockBalanceCommand command) {
        log.info("Locking balance: userId={}, amount={}, description={}",
                command.getUserId(), command.getAmount(), command.getDescription());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("LockBalanceCommand cannot be null");
        }

        // Step 2: Find wallet by user ID
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
                .orElseThrow(() -> NotFoundException.of(
                        "WALLET_NOT_FOUND",
                        "Wallet not found for user ID: " + command.getUserId()
                ));

        // Step 3: Lock balance using domain logic
        // This will throw InsufficientBalanceException if balance too low
        // This will throw IllegalArgumentException if amount invalid
        wallet.lockBalance(command.getAmount());

        // Step 4: Save updated wallet
        UserWallet savedWallet = walletRepository.save(wallet);

        log.info("Balance locked successfully: userId={}, newBalance={}, newLockedBalance={}",
                savedWallet.getUserId(),
                savedWallet.getBalance(),
                savedWallet.getLockedBalance());

        // Step 5: Map to response DTO
        return walletMapper.toResponse(savedWallet);
    }
}
