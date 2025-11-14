package com.cashbee.application.usecase.cashback;

import com.cashbee.application.dto.wallet.AddPendingBalanceCommand;
import com.cashbee.application.dto.wallet.ConfirmPendingBalanceCommand;
import com.cashbee.application.usecase.transaction.CreateTransactionUseCase;
import com.cashbee.application.usecase.wallet.AddPendingBalanceUseCase;
import com.cashbee.application.usecase.wallet.ConfirmPendingBalanceUseCase;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for adding cashback to user wallet.
 *
 * Business Logic:
 * 1. If order completed → Add to balance directly (confirmed cashback)
 * 2. If order pending → Add to pending_balance (wait for completion)
 * 3. Create wallet transaction for audit trail
 * 4. Update cashback status to PAID
 *
 * Flow cho đơn hàng "Hoàn thành":
 * - Cashback status: CONFIRMED → PAID
 * - Wallet: balance += cashbackAmount
 * - Transaction: type=CASHBACK, status=SUCCESS
 *
 * Flow cho đơn hàng "Đang chờ xử lý":
 * - Cashback status: PENDING (không cộng tiền)
 * - Wallet: pending_balance += cashbackAmount
 * - Transaction: type=CASHBACK, status=PENDING
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddCashbackToWalletUseCase {

    private final CashbackRepository cashbackRepository;
    private final UserWalletRepository walletRepository;
    private final AddPendingBalanceUseCase addPendingBalanceUseCase;
    private final ConfirmPendingBalanceUseCase confirmPendingBalanceUseCase;
    private final CreateTransactionUseCase createTransactionUseCase;

    /**
     * Add confirmed cashback to user wallet (for completed orders).
     *
     * NEW BEHAVIOR (FIXED):
     * - For orders that are ALREADY completed when imported from CSV
     * - Add cashback DIRECTLY to balance (not through pendingBalance)
     * - This prevents InsufficientBalanceException when pendingBalance is 0
     *
     * @param cashbackId Cashback ID
     */
    @Transactional
    public void addConfirmedCashback(Long cashbackId) {
        log.info("UseCase: Adding confirmed cashback {} to wallet", cashbackId);

        // Get cashback
        Cashback cashback = cashbackRepository.findById(cashbackId)
            .orElseThrow(() -> new NotFoundException("Cashback not found: " + cashbackId));

        // Validate cashback is confirmed
        if (!cashback.isConfirmed()) {
            throw new BusinessException("Cashback must be in CONFIRMED status, current: " + cashback.getStatus());
        }

        // Check if already paid
        if (cashback.isPaid()) {
            log.warn("UseCase: Cashback {} already paid, skipping", cashbackId);
            return;
        }

        // FIXED: Add directly to balance (for imported completed orders)
        // OLD CODE: confirmPendingBalanceUseCase.execute() - caused InsufficientBalanceException
        // NEW CODE: Use addConfirmedCashbackDirectly() - no pending balance required

        UserWallet wallet = walletRepository.findByUserId(cashback.getUserId())
            .orElseThrow(() -> new NotFoundException("WALLET_NOT_FOUND",
                "Wallet not found for user: " + cashback.getUserId()));

        wallet.addConfirmedCashbackDirectly(cashback.getCashbackAmount());
        walletRepository.save(wallet);

        log.info("UseCase: Added confirmed cashback directly to balance: userId={}, amount={}",
            cashback.getUserId(), cashback.getCashbackAmount());

        // Update cashback status to PAID
        Cashback updatedCashback = cashback.withStatus(
            CashbackStatus.PAID,
            "Cashback added to wallet"
        );
        cashbackRepository.save(updatedCashback);

        log.info("UseCase: Successfully added confirmed cashback {} ({} VND) to wallet of user {}",
            cashbackId, cashback.getCashbackAmount(), cashback.getUserId());
    }

    /**
     * Add pending cashback to user wallet (for pending orders).
     * This adds to pending_balance, not actual balance.
     *
     * @param cashbackId Cashback ID
     */
    @Transactional
    public void addPendingCashback(Long cashbackId) {
        log.info("UseCase: Adding pending cashback {} to pending balance", cashbackId);

        // Get cashback
        Cashback cashback = cashbackRepository.findById(cashbackId)
            .orElseThrow(() -> new NotFoundException("Cashback not found: " + cashbackId));

        // Validate cashback is pending
        if (!cashback.isPending()) {
            throw new BusinessException("Cashback must be in PENDING status, current: " + cashback.getStatus());
        }

        // Add to pending_balance (wait for order completion)
        AddPendingBalanceCommand command = AddPendingBalanceCommand.builder()
            .userId(cashback.getUserId())
            .amount(cashback.getCashbackAmount())
            .description("Pending cashback from order #" + cashback.getOrderId())
            .build();

        addPendingBalanceUseCase.execute(command);

        log.info("UseCase: Successfully added pending cashback {} ({} VND) to pending balance of user {}",
            cashbackId, cashback.getCashbackAmount(), cashback.getUserId());
    }

    /**
     * Confirm pending cashback when order completes.
     * Move from pending_balance → balance.
     *
     * @param orderId Order ID
     */
    @Transactional
    public void confirmCashbackForOrder(Long orderId) {
        log.info("UseCase: Confirming cashback for order {}", orderId);

        // Get cashback
        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));

        // If already paid, skip
        if (cashback.isPaid() || cashback.isConfirmed()) {
            log.warn("UseCase: Cashback for order {} already confirmed/paid, skipping", orderId);
            return;
        }

        // Update status to CONFIRMED
        Cashback confirmedCashback = cashback.withStatus(
            CashbackStatus.CONFIRMED,
            "Order completed, cashback confirmed"
        );
        cashbackRepository.save(confirmedCashback);

        // Move from pending_balance to balance
        ConfirmPendingBalanceCommand command = ConfirmPendingBalanceCommand.builder()
            .userId(cashback.getUserId())
            .amount(cashback.getCashbackAmount())
            .description("Cashback confirmed for order #" + orderId)
            .build();

        confirmPendingBalanceUseCase.execute(command);

        // Update status to PAID
        Cashback paidCashback = confirmedCashback.withStatus(
            CashbackStatus.PAID,
            "Cashback moved to available balance"
        );
        cashbackRepository.save(paidCashback);

        log.info("UseCase: Successfully confirmed cashback for order {} ({} VND)",
            orderId, cashback.getCashbackAmount());
    }

    /**
     * Cancel cashback when PENDING order is cancelled.
     * Only handles PENDING cashback. For PAID cashback, use reversePaidCashbackForOrder().
     *
     * @param orderId Order ID
     */
    @Transactional
    public void cancelCashbackForOrder(Long orderId) {
        log.info("UseCase: Cancelling cashback for order {}", orderId);

        // Get cashback
        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));

        // If already paid, use reversePaidCashbackForOrder() instead
        if (cashback.isPaid()) {
            log.warn("UseCase: Cashback already paid, delegating to reversePaidCashbackForOrder");
            reversePaidCashbackForOrder(orderId);
            return;
        }

        // If already cancelled, skip
        if (cashback.isCancelled()) {
            log.warn("UseCase: Cashback already cancelled, skipping");
            return;
        }

        // Reverse pending balance if cashback is pending
        if (cashback.isPending()) {
            UserWallet wallet = walletRepository.findByUserId(cashback.getUserId())
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + cashback.getUserId()));

            wallet.reversePendingCashback(cashback.getCashbackAmount());
            walletRepository.save(wallet);

            log.info("UseCase: Reversed pending balance {} VND for user {}",
                cashback.getCashbackAmount(), cashback.getUserId());
        }

        // Update status to CANCELLED
        Cashback cancelledCashback = cashback.withStatus(
            CashbackStatus.CANCELLED,
            "Order cancelled"
        );
        cashbackRepository.save(cancelledCashback);

        log.info("UseCase: Successfully cancelled cashback for order {}", orderId);
    }

    /**
     * Reverse paid cashback when COMPLETED order is cancelled.
     * This reverses both balance and totalEarned.
     *
     * Use case: Order COMPLETED → CANCELLED (refund scenario)
     *
     * @param orderId Order ID
     */
    @Transactional
    public void reversePaidCashbackForOrder(Long orderId) {
        log.info("UseCase: Reversing paid cashback for order {}", orderId);

        // Get cashback
        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));

        // Only handle paid cashback
        if (!cashback.isPaid()) {
            throw new BusinessException("Cannot reverse non-paid cashback. Current status: " + cashback.getStatus());
        }

        // Reverse confirmed cashback from wallet
        UserWallet wallet = walletRepository.findByUserId(cashback.getUserId())
            .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + cashback.getUserId()));

        wallet.reverseConfirmedCashback(cashback.getCashbackAmount());
        walletRepository.save(wallet);

        log.info("UseCase: Reversed confirmed cashback {} VND from user {} (balance & totalEarned)",
            cashback.getCashbackAmount(), cashback.getUserId());

        // Update status to CANCELLED
        Cashback cancelledCashback = cashback.withStatus(
            CashbackStatus.CANCELLED,
            "Order cancelled after payment - cashback reversed"
        );
        cashbackRepository.save(cancelledCashback);

        log.info("UseCase: Successfully reversed paid cashback for order {}", orderId);
    }
}
