package com.cashbee.application.usecase.wallet;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
 * - pending_balance = SUM(cashback_amount) WHERE status = PENDING
 *   (Đơn đang chờ xác nhận, có thể bị hủy)
 * - balance = SUM(cashback_amount) WHERE status = CONFIRMED
 *   (Đơn đã chốt, user có thể rút tiền)
 * - total_earned = SUM(cashback_amount) WHERE status IN (CONFIRMED, PAID)
 *   (Tổng tiền đã kiếm được)
 *
 * Flow:
 * - PENDING: Đơn đang xử lý → cộng vào pending_balance
 * - CONFIRMED: Đơn hoàn thành → chuyển từ pending_balance sang balance
 * - PAID: Đã thanh toán cho user → balance giảm, total_withdrawn tăng
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

        // Calculate pending_balance from PENDING cashbacks only
        // (Đơn đang chờ xác nhận, có thể bị hủy)
        BigDecimal pendingBalance = cashbackRepository.sumCashbackAmountByUserIdAndStatus(
            userId,
            CashbackStatus.PENDING
        );

        // Calculate balance from CONFIRMED cashbacks
        // (Đơn đã chốt, user có thể rút tiền)
        BigDecimal balance = cashbackRepository.sumCashbackAmountByUserIdAndStatus(
            userId,
            CashbackStatus.CONFIRMED
        );

        // Calculate total_earned from CONFIRMED + PAID cashbacks
        // (Tổng tiền đã kiếm được)
        BigDecimal totalEarned = cashbackRepository.sumCashbackAmountByUserIdAndStatusIn(
            userId,
            List.of(CashbackStatus.CONFIRMED, CashbackStatus.PAID)
        );

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

    /**
     * Recalculate wallets for multiple users in a NEW transaction.
     *
     * IMPORTANT: This method uses REQUIRES_NEW propagation, which means:
     * - It will SUSPEND the current transaction (if any)
     * - Create a BRAND NEW transaction
     * - Query the database DIRECTLY (sees committed data only)
     *
     * Use this method when calling from another @Transactional method
     * where data has been saved but may not be visible yet due to
     * persistence context issues (flush/clear).
     *
     * @param userIds Set of user IDs
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeForUsersInNewTransaction(Set<Long> userIds) {
        log.info("Recalculating wallets for {} users in NEW transaction", userIds.size());

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
