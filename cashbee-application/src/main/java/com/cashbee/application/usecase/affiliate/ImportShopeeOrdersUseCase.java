package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.ImportOrdersRequest;
import com.cashbee.application.dto.affiliate.ImportOrdersResponse;
import com.cashbee.application.util.affiliate.ShopeeCSVParser;
import com.cashbee.application.util.affiliate.TrackingCodeGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.ImportStatus;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.ImportBatch;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case for importing Shopee orders from CSV file.
 *
 * Flow:
 * 1. Create ImportBatch record
 * 2. Parse CSV file
 * 3. For each row:
 *    - Check for duplicates
 *    - Extract tracking code
 *    - Find user ID from tracking code
 *    - Create AffiliateOrder
 *    - Match with AffiliateClick if available
 * 4. Update ImportBatch with results
 * 5. Return statistics
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportShopeeOrdersUseCase {

    private final ImportBatchRepository batchRepository;
    private final AffiliateOrderRepository orderRepository;
    private final AffiliateClickRepository clickRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final ShopeeCSVParser csvParser;
    private final TrackingCodeGenerator trackingCodeGenerator;

    /**
     * Execute use case to import orders from CSV.
     *
     * @param request Import request with file and options
     * @return Import response with statistics
     * @throws BusinessException if import fails
     */
    @Transactional
    public ImportOrdersResponse execute(ImportOrdersRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("UseCase: Starting import from file: {}", request.getFileName());

        // Step 1: Get platform
        String platformCode = request.getPlatformCode() != null
            ? request.getPlatformCode()
            : "shopee";

        AffiliatePlatform platform = platformRepository.findByCode(platformCode)
            .orElseThrow(() -> {
                log.error("UseCase: Platform not found: {}", platformCode);
                return new NotFoundException("Platform not found: " + platformCode);
            });

        // Step 2: Create ImportBatch record
        ImportBatch batch = ImportBatch.builder()
            .platformId(platform.getId())
            .fileName(request.getFileName())
            .status(ImportStatus.PROCESSING)
            .totalRows(0)
            .successCount(0)
            .failedCount(0)
            .skippedCount(0)
            .importedBy(request.getImportedBy())
            .createdAt(startTime)
            .build();

        batch = batchRepository.save(batch);
        log.info("UseCase: Created ImportBatch with ID: {}", batch.getId());

        // Step 3: Parse CSV file
        List<ShopeeCSVParser.ShopeeOrderRecord> records;
        try {
            records = csvParser.parse(request.getFileInputStream());
            batch.setTotalRows(records.size());
            batchRepository.save(batch);
            log.info("UseCase: Parsed {} records from CSV", records.size());
        } catch (IOException e) {
            log.error("UseCase: Failed to parse CSV file", e);
            batch.setStatus(ImportStatus.FAILED);
            batch.setErrorMessage("Failed to parse CSV file: " + e.getMessage());
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            return buildErrorResponse(batch, e.getMessage(), startTime);
        }

        // Step 4: Process each record
        List<ImportOrdersResponse.ImportErrorDetail> errors = new ArrayList<>();
        int matchedCount = 0;

        for (ShopeeCSVParser.ShopeeOrderRecord record : records) {
            try {
                // Skip records with parse errors
                if (record.hasError()) {
                    batch.incrementFailed();
                    errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                        .rowNumber(record.getRowNumber())
                        .orderId(record.getOrderId())
                        .error(record.getParseError())
                        .rawData(record.getRawData())
                        .build());
                    continue;
                }

                // Skip cancelled orders
                if (record.isCancelled()) {
                    batch.incrementSkipped();
                    log.debug("Skipping cancelled order: {}", record.getOrderId());
                    continue;
                }

                // Check for duplicates
                if (orderRepository.existsByOrderId(record.getOrderId())) {
                    if (request.getSkipDuplicates()) {
                        batch.incrementSkipped();
                        log.debug("Skipping duplicate order: {}", record.getOrderId());
                        continue;
                    } else {
                        throw new BusinessException("Duplicate order ID: " + record.getOrderId());
                    }
                }

                // Extract user ID from tracking code
                Long userId = null;
                AffiliateClick click = null;

                if (record.hasTrackingCode()) {
                    try {
                        userId = trackingCodeGenerator.extractUserId(record.getTrackingCode());
                        log.debug("Extracted user ID {} from tracking code {}", userId, record.getTrackingCode());

                        // Find the click if autoMatch is enabled
                        if (request.getAutoMatch()) {
                            click = clickRepository.findByTrackingCode(record.getTrackingCode())
                                .orElse(null);

                            if (click != null) {
                                log.debug("Found click {} for tracking code {}",
                                    click.getId(), record.getTrackingCode());
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Failed to extract user ID from tracking code: {}", record.getTrackingCode());
                    }
                }

                // If no user ID found, skip this order
                if (userId == null) {
                    batch.incrementSkipped();
                    log.warn("No user ID found for order {}, skipping", record.getOrderId());
                    continue;
                }

                // Create AffiliateOrder
                AffiliateOrder order = AffiliateOrder.builder()
                    .platformId(platform.getId())
                    .userId(userId)
                    .orderId(record.getOrderId())
                    .clickId(record.getTrackingCode())
                    .productName(record.getItemName())
                    .productPrice(record.getPrice())
                    .commissionAmount(record.getTotalCommission())
                    .currency("VND")
                    .orderTime(record.getOrderTime())
                    .orderStatus(record.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING)
                    .source("IMPORT")
                    .importBatchId(batch.getId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

                // Validate order
                order.validate();

                // Save order
                order = orderRepository.save(order);
                log.debug("Created order {} for user {}", order.getId(), userId);

                // Match with click if found
                if (click != null && request.getAutoMatch()) {
                    click.matchWithOrder(order.getId());
                    clickRepository.save(click);
                    matchedCount++;
                    log.debug("Matched order {} with click {}", order.getId(), click.getId());
                }

                batch.incrementSuccess();

            } catch (Exception e) {
                batch.incrementFailed();
                log.error("Failed to process CSV row {}: {}", record.getRowNumber(), e.getMessage(), e);
                errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                    .rowNumber(record.getRowNumber())
                    .orderId(record.getOrderId())
                    .error(e.getMessage())
                    .rawData(record.getRawData())
                    .build());
            }
        }

        // Step 5: Finalize import batch
        LocalDateTime endTime = LocalDateTime.now();

        if (batch.getFailedCount() == 0) {
            batch.setStatus(ImportStatus.COMPLETED);
        } else if (batch.getSuccessCount() > 0) {
            batch.setStatus(ImportStatus.PARTIAL);
        } else {
            batch.setStatus(ImportStatus.FAILED);
        }

        batch.setCompletedAt(endTime);
        batch = batchRepository.save(batch);

        log.info("UseCase: Import completed. Success: {}, Failed: {}, Skipped: {}, Matched: {}",
            batch.getSuccessCount(), batch.getFailedCount(), batch.getSkippedCount(), matchedCount);

        // Step 6: Build response
        long durationSeconds = Duration.between(startTime, endTime).getSeconds();

        return ImportOrdersResponse.builder()
            .batchId(batch.getId())
            .platformName(platform.getName())
            .platformCode(platform.getCode())
            .fileName(batch.getFileName())
            .status(batch.getStatus())
            .totalRows(batch.getTotalRows())
            .successCount(batch.getSuccessCount())
            .failedCount(batch.getFailedCount())
            .skippedCount(batch.getSkippedCount())
            .matchedCount(matchedCount)
            .successRate(batch.getSuccessRate())
            .errors(errors)
            .startedAt(startTime)
            .completedAt(endTime)
            .durationSeconds(durationSeconds)
            .importedBy(request.getImportedBy())
            .message(buildMessage(batch, matchedCount))
            .build();
    }

    /**
     * Build error response when import fails.
     */
    private ImportOrdersResponse buildErrorResponse(ImportBatch batch, String errorMessage, LocalDateTime startTime) {
        return ImportOrdersResponse.builder()
            .batchId(batch.getId())
            .platformCode("shopee")
            .fileName(batch.getFileName())
            .status(ImportStatus.FAILED)
            .totalRows(0)
            .successCount(0)
            .failedCount(0)
            .skippedCount(0)
            .matchedCount(0)
            .successRate(0.0)
            .errorMessage(errorMessage)
            .errors(new ArrayList<>())
            .startedAt(startTime)
            .completedAt(LocalDateTime.now())
            .durationSeconds(0L)
            .importedBy(batch.getImportedBy())
            .message("Import failed: " + errorMessage)
            .build();
    }

    /**
     * Build user-friendly message.
     */
    private String buildMessage(ImportBatch batch, int matchedCount) {
        if (batch.getStatus() == ImportStatus.COMPLETED) {
            return String.format("Successfully imported %d orders. %d orders matched with clicks.",
                batch.getSuccessCount(), matchedCount);
        } else if (batch.getStatus() == ImportStatus.PARTIAL) {
            return String.format("Partially imported %d orders (%d failed, %d skipped). %d orders matched.",
                batch.getSuccessCount(), batch.getFailedCount(), batch.getSkippedCount(), matchedCount);
        } else {
            return String.format("Import failed. %d orders failed to import.",
                batch.getFailedCount());
        }
    }
}
