package com.cashbee.application.usecase.wallet;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Use case for recalculating wallet balances from cashback data.
 *
 * This ensures wallet balances are always accurate by recalculating
 * from the source of truth (cashback table) after import operations.
 *
 * Calculation Logic:
 * - pending_balance = SUM(cashback_amount) WHERE status IN (PENDING, CONFIRMED)
 * - total_earned = SUM(cashback_amount) WHERE status = PAID
 * - balance = total_earned - total_withdrawn
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecalculateWalletUseCase {

    private final CashbackRepository cashbackRepository;
    private final UserWalletRepository walletRepository;

    /**
     * Recalculate wallet balances for a specific user.
     *
     * @param userId User ID
     */
    @Transactional
    public void execute(Long userId) {
        log.info("Recalculating wallet for user {}", userId);

        // Get or create wallet
        UserWallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> {
                log.info("Wallet not found for user {}, creating new wallet", userId);
                UserWallet newWallet = UserWallet.builder()
                    .userId(userId)
                    .build();
                return walletRepository.save(newWallet);
            });

        // Calculate pending_balance from PENDING and CONFIRMED cashbacks
        BigDecimal pendingBalance = cashbackRepository.sumCashbackAmountByUserIdAndStatusIn(
            userId,
            List.of(CashbackStatus.PENDING, CashbackStatus.CONFIRMED)
        );

        // Calculate total_earned from PAID cashbacks
        BigDecimal totalEarned = cashbackRepository.sumCashbackAmountByUserIdAndStatus(
            userId,
            CashbackStatus.PAID
        );

        // Calculate balance = total_earned - total_withdrawn
        // Note: total_withdrawn is kept as-is (not recalculated from cashback)
        BigDecimal balance = totalEarned.subtract(wallet.getTotalWithdrawn());

        // Ensure balance is not negative
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Calculated balance is negative for user {}, setting to 0", userId);
            balance = BigDecimal.ZERO;
        }

        // Update wallet
        BigDecimal oldBalance = wallet.getBalance();
        BigDecimal oldPendingBalance = wallet.getPendingBalance();
        BigDecimal oldTotalEarned = wallet.getTotalEarned();

        wallet.setBalance(balance);
        wallet.setPendingBalance(pendingBalance);
        wallet.setTotalEarned(totalEarned);
        wallet.scaleBalances();

        walletRepository.save(wallet);

        log.info("Recalculated wallet for user {}: balance {} → {}, pendingBalance {} → {}, totalEarned {} → {}",
            userId,
            oldBalance, balance,
            oldPendingBalance, pendingBalance,
            oldTotalEarned, totalEarned);
    }

    /**
     * Recalculate wallets for multiple users.
     *
     * @param userIds Set of user IDs
     */
    @Transactional
    public void executeForUsers(Set<Long> userIds) {
        log.info("Recalculating wallets for {} users", userIds.size());

        for (Long userId : userIds) {
            try {
                execute(userId);
            } catch (Exception e) {
                log.error("Failed to recalculate wallet for user {}: {}", userId, e.getMessage(), e);
            }
        }

        log.info("Completed recalculating wallets for {} users", userIds.size());
    }
}
