package com.cashbee.application.service.usecase;

import com.cashbee.application.usecase.invoice.GeneratePaymentInvoiceUseCase;
import com.cashbee.application.usecase.invoice.SendInvoiceEmailUseCase;
import com.cashbee.application.usecase.wallet.RecalculateWalletUseCase;
import com.cashbee.domain.enums.BatchItemStatus;
import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.model.BatchTransferItem;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.BatchCashbackSnapshotRepository;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import com.cashbee.domain.repository.BatchTransferItemRepository;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.ReferrerCommissionRepository;
import com.cashbee.domain.repository.TransactionRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression test for CompleteBatchTransferUseCase.
 *
 * Bug: Catch-all block (lines 446-464) updates ALL CONFIRMED cashbacks to PAID
 * for a user, including cashbacks confirmed AFTER the batch was created.
 * Only snapshot-based cashbacks should be marked as PAID.
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompleteBatchTransferUseCase - Catch-All Regression Tests")
class CompleteBatchTransferUseCaseTest {

    @Mock
    private BatchTransferExportRepository batchExportRepository;

    @Mock
    private BatchTransferItemRepository batchItemRepository;

    @Mock
    private BatchCashbackSnapshotRepository batchCashbackSnapshotRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CashbackRepository cashbackRepository;

    @Mock
    private ReferralRewardRepository referralRewardRepository;

    @Mock
    private ReferrerCommissionRepository referrerCommissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AffiliatePlatformRepository platformRepository;

    @Mock
    private GeneratePaymentInvoiceUseCase generatePaymentInvoiceUseCase;

    @Mock
    private SendInvoiceEmailUseCase sendInvoiceEmailUseCase;

    @Mock
    private RecalculateWalletUseCase recalculateWalletUseCase;

    @InjectMocks
    private CompleteBatchTransferUseCase completeBatchTransferUseCase;

    private static final String BATCH_CODE = "BATCH_20260303_001";
    private static final Long BATCH_ID = 24L;
    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 59L;
    private static final Long WALLET_ID = 59L;
    private static final Long CASHBACK_A_ID = 100L; // In snapshot (confirmed before batch)
    private static final Long CASHBACK_B_ID = 200L; // NOT in snapshot (confirmed after batch)

    private BatchTransferExport batch;
    private BatchTransferItem item;
    private UserWallet wallet;

    @BeforeEach
    void setUp() {
        batch = BatchTransferExport.builder()
                .id(BATCH_ID)
                .batchCode(BATCH_CODE)
                .status(ExportStatus.PENDING)
                .totalUsers(1)
                .totalAmount(new BigDecimal("58752"))
                .createdAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .build();

        item = BatchTransferItem.builder()
                .id(1L)
                .batchId(BATCH_ID)
                .userId(USER_ID)
                .walletId(WALLET_ID)
                .amount(new BigDecimal("58752"))
                .accountNumber("1234567890")
                .accountName("HOANG AN")
                .bankName("Vietcombank")
                .status(BatchItemStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .build();

        wallet = UserWallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .balance(new BigDecimal("255464"))
                .pendingBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarned(new BigDecimal("255464"))
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
    }

    // ============================================================
    // REGRESSION TEST: Catch-all must NOT mark non-snapshot cashbacks
    // ============================================================

    @Test
    @DisplayName("Cashbacks confirmed after batch creation should NOT be marked as PAID")
    void execute_ShouldOnlyUpdateSnapshotCashbacks_NotAllConfirmedCashbacks() {
        // Given: Batch exists and is PENDING
        when(batchExportRepository.findByBatchCodeForUpdate(BATCH_CODE))
                .thenReturn(Optional.of(batch));
        when(batchExportRepository.save(any(BatchTransferExport.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Given: One pending item
        when(batchItemRepository.findByBatchIdAndStatus(BATCH_ID, BatchItemStatus.PENDING))
                .thenReturn(List.of(item));

        // Given: Wallet exists with sufficient balance
        when(walletRepository.findAllById(any()))
                .thenReturn(List.of(wallet));

        // Given: Snapshot contains ONLY cashback_A (confirmed before batch creation)
        when(batchCashbackSnapshotRepository.findCashbackIdsByBatchId(BATCH_ID))
                .thenReturn(List.of(CASHBACK_A_ID));
        when(batchCashbackSnapshotRepository.findCashbackIdsByBatchIdAndUserId(BATCH_ID, USER_ID))
                .thenReturn(List.of(CASHBACK_A_ID));

        // Given: No referral rewards or commissions
        when(referralRewardRepository.findUnpaidByUserId(USER_ID)).thenReturn(List.of());
        when(referrerCommissionRepository.findUnpaidByReferrerId(USER_ID)).thenReturn(List.of());

        // Given: Platform data for invoice
        when(platformRepository.findAll()).thenReturn(List.of());

        // Given: Cashback update returns 1
        when(cashbackRepository.updateStatusByCashbackIdsWithBatchId(
                eq(List.of(CASHBACK_A_ID)),
                eq(CashbackStatus.CONFIRMED),
                eq(CashbackStatus.PAID),
                eq(BATCH_ID)
        )).thenReturn(1);

        // Given: Invoice generation (batch query returns paid cashbacks)
        when(cashbackRepository.findByPaidBatchId(BATCH_ID)).thenReturn(List.of());

        // When: Execute batch completion
        completeBatchTransferUseCase.execute(BATCH_CODE, ADMIN_ID);

        // Then: ONLY snapshot-based update should be called
        verify(cashbackRepository).updateStatusByCashbackIdsWithBatchId(
                eq(List.of(CASHBACK_A_ID)),
                eq(CashbackStatus.CONFIRMED),
                eq(CashbackStatus.PAID),
                eq(BATCH_ID)
        );

        // Then: Catch-all method should NEVER be called
        // This is the critical assertion — the bug was that this method
        // marked ALL CONFIRMED cashbacks as PAID (including cashback_B
        // which was confirmed after batch creation)
        verify(cashbackRepository, never()).updateStatusByUserIdAndStatusWithBatchId(
                anyLong(), any(CashbackStatus.class), any(CashbackStatus.class), anyLong()
        );
    }
}
