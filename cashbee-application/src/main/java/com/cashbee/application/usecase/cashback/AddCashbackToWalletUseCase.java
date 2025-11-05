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
import com.cashbee.domain.repository.CashbackRepository;
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
    private final AddPendingBalanceUseCase addPendingBalanceUseCase;
    private final ConfirmPendingBalanceUseCase confirmPendingBalanceUseCase;
    private final CreateTransactionUseCase createTransactionUseCase;

    /**
     * Add confirmed cashback to user wallet (for completed orders).
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

        // Add to balance directly (order already completed)
        ConfirmPendingBalanceCommand command = ConfirmPendingBalanceCommand.builder()
            .userId(cashback.getUserId())
            .amount(cashback.getCashbackAmount())
            .description("Cashback from order #" + cashback.getOrderId())
            .build();

        confirmPendingBalanceUseCase.execute(command);

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
     * Cancel cashback when order is cancelled.
     *
     * @param orderId Order ID
     */
    @Transactional
    public void cancelCashbackForOrder(Long orderId) {
        log.info("UseCase: Cancelling cashback for order {}", orderId);

        // Get cashback
        Cashback cashback = cashbackRepository.findByOrderId(orderId)
            .orElseThrow(() -> new NotFoundException("Cashback not found for order: " + orderId));

        // If already paid, cannot cancel
        if (cashback.isPaid()) {
            throw new BusinessException("Cannot cancel cashback that has already been paid");
        }

        // Update status to CANCELLED
        Cashback cancelledCashback = cashback.withStatus(
            CashbackStatus.CANCELLED,
            "Order cancelled"
        );
        cashbackRepository.save(cancelledCashback);

        log.info("UseCase: Successfully cancelled cashback for order {}", orderId);
    }
}
