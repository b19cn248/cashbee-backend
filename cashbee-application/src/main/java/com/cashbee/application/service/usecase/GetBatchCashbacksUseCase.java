package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.batch.BatchCashbackResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import com.cashbee.domain.repository.CashbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for getting all cashbacks paid by a specific batch.
 * Enables traceability: batchCode → cashbacks → orders
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetBatchCashbacksUseCase {

    private final BatchTransferExportRepository batchExportRepository;
    private final CashbackRepository cashbackRepository;

    /**
     * Get all cashbacks paid by a batch.
     *
     * @param batchCode Batch code (e.g., BATCH_20251119_001)
     * @return Response with batch info and list of cashbacks
     */
    @Transactional(readOnly = true)
    public BatchCashbackResponse execute(String batchCode) {
        log.info("GetBatchCashbacksUseCase: Getting cashbacks for batch {}", batchCode);

        // 1. Find batch by code
        BatchTransferExport batch = batchExportRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> NotFoundException.of("BATCH_NOT_FOUND",
                        "Batch not found: " + batchCode));

        // 2. Get all cashbacks paid by this batch
        List<Cashback> cashbacks = cashbackRepository.findByPaidBatchId(batch.getId());

        log.info("GetBatchCashbacksUseCase: Found {} cashbacks for batch {}", cashbacks.size(), batchCode);

        // 3. Calculate totals
        BigDecimal totalCashbackAmount = cashbacks.stream()
                .map(Cashback::getCashbackAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Map to response
        List<BatchCashbackResponse.CashbackDetail> cashbackDetails = cashbacks.stream()
                .map(this::toCashbackDetail)
                .collect(Collectors.toList());

        return BatchCashbackResponse.builder()
                .batchCode(batch.getBatchCode())
                .status(batch.getStatus().name())
                .completedAt(batch.getCreatedAt())  // Use createdAt as batch doesn't have completedAt
                .totalCashbacks(cashbacks.size())
                .totalCashbackAmount(totalCashbackAmount)
                .cashbacks(cashbackDetails)
                .build();
    }

    private BatchCashbackResponse.CashbackDetail toCashbackDetail(Cashback cashback) {
        return BatchCashbackResponse.CashbackDetail.builder()
                .cashbackId(cashback.getId())
                .userId(cashback.getUserId())
                .orderId(cashback.getOrderId())
                .orderItemId(cashback.getOrderItemId())
                .cashbackAmount(cashback.getCashbackAmount())
                .commissionAmount(cashback.getCommissionAmount())
                .paidAt(cashback.getPaidAt())
                .createdAt(cashback.getCreatedAt())
                .build();
    }
}
