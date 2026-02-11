package com.cashbee.application.usecase.wallet;

import com.cashbee.application.dto.wallet.AddPendingBalanceCommand;
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
 * Use case for adding pending balance to user wallet.
 *
 * Pending balance is used when cashback is earned but not yet confirmed.
 * This happens when:
 * - A purchase is made through affiliate link
 * - The order is pending confirmation from the affiliate network
 * - The cashback amount is calculated but not yet available for withdrawal
 *
 * Business Rules:
 * - Pending balance cannot be withdrawn until confirmed
 * - Pending balance is shown separately in the wallet
 * - After confirmation period, pending balance becomes available balance
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddPendingBalanceUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Add pending balance to user wallet.
     * If wallet doesn't exist, creates one automatically.
     *
     * @param command Command containing userId and amount
     * @return Updated wallet response
     */
    @Transactional
    public WalletResponse execute(AddPendingBalanceCommand command) {
        log.info("Adding pending balance: userId={}, amount={}, description={}",
            command.getUserId(), command.getAmount(), command.getDescription());

        // Get or create wallet
        UserWallet wallet = walletRepository.findByUserId(command.getUserId())
            .orElseGet(() -> {
                log.info("Wallet not found for user {}, creating new wallet", command.getUserId());
                UserWallet newWallet = UserWallet.builder()
                    .userId(command.getUserId())
                    .build();
                return walletRepository.save(newWallet);
            });

        // Add pending balance using domain logic
        wallet.addPendingBalance(command.getAmount());

        // Save wallet
        wallet = walletRepository.save(wallet);

        log.info("Pending balance added successfully: userId={}, newPendingBalance={}",
            command.getUserId(), wallet.getPendingBalance());

        return walletMapper.toResponse(wallet);
    }
}
