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
import java.util.List;

/**
 * Use Case: Grant missing referrer commissions (Retroactive Fix).
 *
 * <p>This use case processes all CONFIRMED commissions that were never paid
 * to referrers' wallets due to a bug in the original implementation.
 *
 * <h3>Problem:</h3>
 * The referrer_commission table recorded 5% commissions correctly, but
 * the commission amounts were never added to the referrer's wallet balance.
 *
 * <h3>Solution:</h3>
 * This use case finds all CONFIRMED commissions and pays them to wallets
 * using the same 3-layer anti-duplicate protection as PayReferrerCommissionUseCase.
 *
 * <h3>Usage:</h3>
 * This is a one-time admin operation to fix historical data.
 * Run via admin endpoint: POST /api/admin/referral/grant-missing-commissions
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrantMissingReferrerCommissionsUseCase {

    private final ReferrerCommissionRepository commissionRepository;
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CreateTransactionUseCase createTransactionUseCase;

    /**
     * Result of the retroactive fix operation.
     */
    public record GrantResult(
            int totalFound,
            int successCount,
            int skippedCount,
            int errorCount,
            BigDecimal totalAmountPaid
    ) {}

    /**
     * Execute the retroactive fix for all CONFIRMED commissions.
     *
     * @return Result summary
     */
    @Transactional
    public GrantResult execute() {
        log.info("Starting retroactive fix for missing referrer commissions");

        // Find all CONFIRMED commissions (not yet paid to wallet)
        List<ReferrerCommission> confirmedCommissions =
                commissionRepository.findByStatus(ReferrerCommissionStatus.CONFIRMED);

        int totalFound = confirmedCommissions.size();
        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        BigDecimal totalAmountPaid = BigDecimal.ZERO;

        log.info("Found {} CONFIRMED commissions to process", totalFound);

        for (ReferrerCommission commission : confirmedCommissions) {
            try {
                boolean paid = processCommission(commission);
                if (paid) {
                    successCount++;
                    totalAmountPaid = totalAmountPaid.add(commission.getCommissionAmount());
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Error processing commission {}: {}",
                        commission.getId(), e.getMessage(), e);
            }
        }

        log.info("Retroactive fix completed: total={}, success={}, skipped={}, errors={}, amountPaid={}",
                totalFound, successCount, skippedCount, errorCount, totalAmountPaid);

        return new GrantResult(totalFound, successCount, skippedCount, errorCount, totalAmountPaid);
    }

    /**
     * Process a single commission.
     *
     * @param commission Commission to process
     * @return true if paid, false if skipped
     */
    private boolean processCommission(ReferrerCommission commission) {
        // Layer 3: Check for existing transaction (prevent duplicates)
        if (transactionRepository.existsBySourceIdAndSourceType(
                commission.getId(), TransactionSourceType.REFERRER_COMMISSION)) {
            log.debug("Transaction already exists for commission {}, skipping", commission.getId());
            return false;
        }

        // Pay commission to wallet
        payCommissionToWallet(commission);

        log.info("Paid retroactive commission: referrerId={}, orderId={}, amount={}",
                commission.getReferrerId(), commission.getSourceOrderId(), commission.getCommissionAmount());

        return true;
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
                .description("Referrer commission (retroactive) from order #" + commission.getSourceOrderId())
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

        log.debug("Retroactive commission paid: referrerId={}, amount={}, balance: {} -> {}",
                referrerId, amount, balanceBefore, balanceAfter);
    }
}
