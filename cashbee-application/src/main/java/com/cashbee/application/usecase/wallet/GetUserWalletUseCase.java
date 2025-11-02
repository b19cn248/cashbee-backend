package com.cashbee.application.usecase.wallet;

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
 * Use case for retrieving user wallet information.
 *
 * Business Rules:
 * - Every user must have exactly one wallet
 * - Wallet contains available, pending, and locked balances
 * - Wallet shows lifetime earnings and withdrawals
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserWalletUseCase {

    private final UserWalletRepository walletRepository;
    private final WalletMapper walletMapper;

    /**
     * Get wallet by user ID.
     *
     * @param userId User ID
     * @return Wallet response DTO
     * @throws NotFoundException if wallet doesn't exist
     */
    @Transactional(readOnly = true)
    public WalletResponse execute(Long userId) {
        log.debug("Getting wallet for user: userId={}", userId);

        UserWallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND",
                "Wallet not found for user: " + userId));

        log.debug("Found wallet: id={}, balance={}, pending={}, locked={}",
            wallet.getId(), wallet.getBalance(), wallet.getPendingBalance(), wallet.getLockedBalance());

        return walletMapper.toResponse(wallet);
    }

    /**
     * Get wallet by wallet ID.
     *
     * @param walletId Wallet ID
     * @return Wallet response DTO
     * @throws NotFoundException if wallet doesn't exist
     */
    @Transactional(readOnly = true)
    public WalletResponse executeById(Long walletId) {
        log.debug("Getting wallet by id: walletId={}", walletId);

        UserWallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND",
                "Wallet not found: " + walletId));

        return walletMapper.toResponse(wallet);
    }
}
