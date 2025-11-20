package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.batch.ExportBatchTransferResponse;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for getting batch export history.
 *
 * Allows admins to view past exports with pagination.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetBatchExportHistoryUseCase {

    private final BatchTransferExportRepository batchTransferExportRepository;

    /**
     * Get all batch export history.
     *
     * @param pageable Pagination parameters
     * @return Page of export responses
     */
    @Transactional(readOnly = true)
    public Page<ExportBatchTransferResponse> execute(Pageable pageable) {
        log.info("GetBatchExportHistoryUseCase: Getting batch export history (page: {})", pageable.getPageNumber());

        Page<BatchTransferExport> batchExports = batchTransferExportRepository.findAll(pageable);

        return batchExports.map(this::toResponse);
    }

    /**
     * Get batch export history by type.
     *
     * @param exportType Export type (MANUAL or SCHEDULED)
     * @param pageable Pagination parameters
     * @return Page of export responses
     */
    @Transactional(readOnly = true)
    public Page<ExportBatchTransferResponse> executeByType(String exportType, Pageable pageable) {
        log.info("GetBatchExportHistoryUseCase: Getting batch export history by type: {} (page: {})",
                exportType, pageable.getPageNumber());

        Page<BatchTransferExport> batchExports = batchTransferExportRepository.findByExportType(exportType, pageable);

        return batchExports.map(this::toResponse);
    }

    /**
     * Convert domain model to response DTO.
     */
    private ExportBatchTransferResponse toResponse(BatchTransferExport batchExport) {
        return ExportBatchTransferResponse.builder()
                .batchCode(batchExport.getBatchCode())
                .fileName(batchExport.getFileName())
                .totalUsers(batchExport.getTotalUsers())
                .totalAmount(batchExport.getTotalAmount())
                .exportedAt(batchExport.getCreatedAt())
                .message(String.format(
                        "%s export: %d users, %,d VND",
                        batchExport.getExportType().name(),
                        batchExport.getTotalUsers(),
                        batchExport.getTotalAmount().longValue()
                ))
                .build();
    }
}
