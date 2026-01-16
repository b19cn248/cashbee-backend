package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.transaction.CreateTransactionCommand;
import com.cashbee.application.usecase.transaction.CreateTransactionUseCase;
import com.cashbee.domain.enums.ReferrerCommissionStatus;
import com.cashbee.domain.enums.TransactionSourceType;
import com.cashbee.domain.enums.TransactionStatus;
import com.cashbee.domain.enums.TransactionType;
import com.cashbee.domain.model.ReferrerCommission;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Use Case: Pay referrer commission to wallet.
 *
 * <p>This use case is triggered when a referee's cashback is confirmed
 * (PENDING → CONFIRMED). It adds the commission to the referrer's wallet
 * and updates the commission status to PAID.
 *
 * <h3>Anti-Duplicate Protection (3 Layers):</h3>
 * <ol>
 *   <li>Database UNIQUE constraint on source_order_id (Layer 1)</li>
 *   <li>Status check: only process CONFIRMED commissions (Layer 2)</li>
 *   <li>Transaction existence check before insert (Layer 3)</li>
 * </ol>
 *
 * <h3>Flow:</h3>
 * <pre>
 * Referee cashback: PENDING → CONFIRMED
 *         ↓
 * Find ReferrerCommission by sourceOrderId
 *         ↓
 * Check status == CONFIRMED
 *         ↓
 * Check no existing transaction
 *         ↓
 * Add to referrer's wallet (balance, NOT pendingBalance)
 *         ↓
 * Create transaction record
 *         ↓
 * Update commission status: CONFIRMED → PAID
 * </pre>
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayReferrerCommissionUseCase {

    private final ReferrerCommissionRepository commissionRepository;
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CreateTransactionUseCase createTransactionUseCase;

    /**
     * Pay referrer commission when referee's cashback is confirmed.
     *
     * @param orderId The order ID that triggered the cashback confirmation
     */
    @Transactional
    public void execute(Long orderId) {
        log.debug("Processing referrer commission payment for order: {}", orderId);

        // 1. Find commission by order ID
        Optional<ReferrerCommission> commissionOpt = commissionRepository.findBySourceOrderId(orderId);
        if (commissionOpt.isEmpty()) {
            log.debug("No referrer commission found for order: {}", orderId);
            return;
        }

        ReferrerCommission commission = commissionOpt.get();

        // 2. Layer 2: Status check - only process CONFIRMED commissions
        if (commission.getStatus() != ReferrerCommissionStatus.CONFIRMED) {
            log.debug("Commission not in CONFIRMED status, skipping: orderId={}, status={}",
                    orderId, commission.getStatus());
            return;
        }

        // 3. Layer 3: Check for existing transaction (prevent duplicates)
        if (transactionRepository.existsBySourceIdAndSourceType(
                commission.getId(), TransactionSourceType.REFERRER_COMMISSION)) {
            log.warn("Transaction already exists for commission: commissionId={}, orderId={}",
                    commission.getId(), orderId);
            return;
        }

        // 4. Pay commission to referrer's wallet
        payCommissionToWallet(commission);

        log.info("Referrer commission paid: referrerId={}, orderId={}, amount={}, commissionRate={}%",
                commission.getReferrerId(), orderId, commission.getCommissionAmount(),
                commission.getCommissionRate());
    }

    /**
     * Pay commission to referrer's wallet and update status.
     */
    private void payCommissionToWallet(ReferrerCommission commission) {
        Long referrerId = commission.getReferrerId();
        BigDecimal amount = commission.getCommissionAmount();

        // Get or create wallet
        UserWallet wallet = walletRepository.findByUserId(referrerId)
                .orElseGet(() -> {
                    log.info("Wallet not found for referrer {}, creating new wallet", referrerId);
                    UserWallet newWallet = UserWallet.builder()
                            .userId(referrerId)
                            .build();
                    return walletRepository.save(newWallet);
                });

        // Record balance before
        BigDecimal balanceBefore = wallet.getBalance();

        // Add commission to wallet (directly to balance, not pending)
        // Commission is already confirmed when referee's order is confirmed
        wallet.addBonus(amount);
        walletRepository.save(wallet);

        // Record balance after
        BigDecimal balanceAfter = wallet.getBalance();

        // Create transaction for audit trail
        CreateTransactionCommand transactionCommand = CreateTransactionCommand.builder()
                .userId(referrerId)
                .walletId(wallet.getId())
                .type(TransactionType.BONUS)
                .amount(amount)
                .description("Referrer commission from order #" + commission.getSourceOrderId())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .sourceType(TransactionSourceType.REFERRER_COMMISSION)
                .sourceId(commission.getId())
                .build();

        createTransactionUseCase.execute(transactionCommand);

        // Update commission status: CONFIRMED → PAID
        commission.markAsPaid();
        commissionRepository.save(commission);

        log.debug("Commission paid to wallet: referrerId={}, amount={}, balance: {} -> {}",
                referrerId, amount, balanceBefore, balanceAfter);
    }
}
