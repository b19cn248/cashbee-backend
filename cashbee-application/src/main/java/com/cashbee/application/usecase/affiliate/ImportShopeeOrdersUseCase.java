package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.ImportOrdersRequest;
import com.cashbee.application.dto.affiliate.ImportOrdersResponse;
import com.cashbee.application.usecase.cashback.AddCashbackToWalletUseCase;
import com.cashbee.application.usecase.cashback.CalculateCashbackUseCase;
import com.cashbee.application.usecase.cashback.UpdateCashbackOnOrderStatusChangeUseCase;
import com.cashbee.application.util.affiliate.ShopeeCSVParser;
import com.cashbee.application.util.affiliate.TrackingCodeGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.ImportStatus;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.enums.UpdateMode;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.model.ImportBatch;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.ImportBatchRepository;
import com.cashbee.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
    private final UserRepository userRepository;
    private final ShopeeCSVParser csvParser;
    private final TrackingCodeGenerator trackingCodeGenerator;
    private final CalculateCashbackUseCase calculateCashbackUseCase;
    private final AddCashbackToWalletUseCase addCashbackToWalletUseCase;
    private final UpdateCashbackOnOrderStatusChangeUseCase updateCashbackOnOrderStatusChangeUseCase;
    private final EntityManager entityManager;

    /**
     * Batch size for processing CSV records.
     * This prevents OutOfMemoryError by processing records in smaller chunks.
     */
    private static final int BATCH_SIZE = 200;

    /**
     * Execute use case to import orders from CSV.
     * Uses streaming and batch processing to prevent OutOfMemoryError.
     *
     * @param request Import request with file and options
     * @return Import response with statistics
     * @throws BusinessException if import fails
     */
    @Transactional(propagation = Propagation.REQUIRED)
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
        final Long batchId = batch.getId();
        log.info("UseCase: Created ImportBatch with ID: {}", batchId);

        // Step 3: Parse CSV file using STREAMING to prevent OutOfMemoryError
        List<ImportOrdersResponse.ImportErrorDetail> errors = new ArrayList<>();
        final int[] matchedCount = {0};  // Use array to allow modification in lambda
        final int[] totalRowsProcessed = {0};

        try {
            // Use streaming parser to process records in batches
            csvParser.parseStreaming(request.getFileInputStream(), BATCH_SIZE, recordBatch -> {
                // Process this batch in a separate transaction
                processBatch(recordBatch, platform, batchId, request, errors, matchedCount, totalRowsProcessed);
            });

            // Update total rows after parsing
            batch.setTotalRows(totalRowsProcessed[0]);
            batchRepository.save(batch);
            log.info("UseCase: Parsed {} records from CSV using streaming", totalRowsProcessed[0]);

        } catch (IOException e) {
            log.error("UseCase: Failed to parse CSV file", e);
            batch.setStatus(ImportStatus.FAILED);
            batch.setErrorMessage("Failed to parse CSV file: " + e.getMessage());
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            return buildErrorResponse(batch, e.getMessage(), startTime);
        }

        // Step 4: Finalize import batch
        // Reload batch to get updated counts
        batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new NotFoundException("Import batch not found"));

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

        log.info("UseCase: Import completed. Success: {}, Updated: {}, Failed: {}, Skipped: {}, Matched: {}",
            batch.getSuccessCount(), batch.getUpdatedCount(), batch.getFailedCount(),
            batch.getSkippedCount(), matchedCount[0]);

        // Step 5: Build response
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
            .updatedCount(batch.getUpdatedCount())
            .matchedCount(matchedCount[0])
            .successRate(batch.getSuccessRate())
            .errors(errors)
            .startedAt(startTime)
            .completedAt(endTime)
            .durationSeconds(durationSeconds)
            .importedBy(request.getImportedBy())
            .message(buildMessage(batch, matchedCount[0]))
            .build();
    }

    /**
     * Process a batch of CSV records.
     * This method is called for each batch during streaming parsing.
     * Each batch is processed with flush & clear to prevent memory buildup.
     *
     * @param recordBatch List of records in this batch
     * @param platform Affiliate platform
     * @param batchId Import batch ID
     * @param request Import request
     * @param errors List to collect errors
     * @param matchedCount Counter for matched orders
     * @param totalRowsProcessed Counter for total rows processed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processBatch(List<ShopeeCSVParser.ShopeeOrderRecord> recordBatch,
                                AffiliatePlatform platform,
                                Long batchId,
                                ImportOrdersRequest request,
                                List<ImportOrdersResponse.ImportErrorDetail> errors,
                                int[] matchedCount,
                                int[] totalRowsProcessed) {

        ImportBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new NotFoundException("Import batch not found"));

        int processedInThisBatch = 0;

        for (ShopeeCSVParser.ShopeeOrderRecord record : recordBatch) {
            totalRowsProcessed[0]++;
            processedInThisBatch++;

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

                // Check for duplicates and handle based on updateMode
                AffiliateOrder existingOrder = orderRepository.findByOrderId(record.getOrderId())
                    .orElse(null);

                if (existingOrder != null) {
                    // Order already exists - handle based on updateMode
                    if (request.getUpdateMode() == UpdateMode.SKIP) {
                        batch.incrementSkipped();
                        log.debug("Skipping duplicate order: {}", record.getOrderId());
                        continue;
                    } else if (request.getUpdateMode() == UpdateMode.UPDATE) {
                        // Update existing order
                        updateExistingOrder(existingOrder, record, batch);
                        continue;
                    }
                }

                // Extract user ID from tracking code
                Long userId = null;
                AffiliateClick click = null;

                if (record.hasTrackingCode()) {
                    try {
                        userId = trackingCodeGenerator.extractUserId(record.getTrackingCode());
                        log.debug("Extracted user ID {} from tracking code {}", userId, record.getTrackingCode());

                        // Validate that user exists in database
                        if (!userRepository.existsById(userId)) {
                            batch.incrementSkipped();
                            log.warn("User ID {} extracted from tracking code {} does not exist in database, skipping order {}",
                                userId, record.getTrackingCode(), record.getOrderId());
                            errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                                .rowNumber(record.getRowNumber())
                                .orderId(record.getOrderId())
                                .error(String.format("User ID %d does not exist in database (tracking code: %s)",
                                    userId, record.getTrackingCode()))
                                .rawData(record.getRawData())
                                .build());
                            continue;
                        }

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
                        batch.incrementSkipped();
                        log.warn("Failed to extract user ID from tracking code: {}", record.getTrackingCode());
                        errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                            .rowNumber(record.getRowNumber())
                            .orderId(record.getOrderId())
                            .error("Invalid tracking code format: " + e.getMessage())
                            .rawData(record.getRawData())
                            .build());
                        continue;
                    }
                }

                // If no user ID found, skip this order
                if (userId == null) {
                    batch.incrementSkipped();
                    log.warn("No tracking code found for order {}, skipping", record.getOrderId());
                    errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                        .rowNumber(record.getRowNumber())
                        .orderId(record.getOrderId())
                        .error("No tracking code (Sub_id1) found in CSV record")
                        .rawData(record.getRawData())
                        .build());
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
                    .commissionAmount(record.getCommissionForCashback())
                    .currency("VND")
                    .orderTime(record.getOrderTime())
                    .orderStatus(record.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING)
                    .source("IMPORT")
                    .importBatchId(batchId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

                // Validate order
                order.validate();

                // Save order
                order = orderRepository.save(order);
                log.debug("Created order {} for user {}", order.getId(), userId);

                // Calculate and add cashback
                try {
                    // Skip if commission is zero or null
                    if (record.getCommissionForCashback() != null &&
                        record.getCommissionForCashback().compareTo(java.math.BigDecimal.ZERO) > 0) {

                        // Determine if order is completed
                        boolean isOrderCompleted = record.isCompleted();

                        // Calculate cashback
                        Cashback cashback = calculateCashbackUseCase.execute(
                            userId,
                            order.getId(),
                            platform.getId(),
                            record.getCommissionForCashback(),
                            isOrderCompleted
                        );

                        log.info("Created cashback {} with amount {} VND (status: {})",
                            cashback.getId(), cashback.getCashbackAmount(), cashback.getStatus());

                        // Add cashback to wallet based on order status
                        if (cashback.isConfirmed()) {
                            // Order completed → Add to balance directly
                            addCashbackToWalletUseCase.addConfirmedCashback(cashback.getId());
                            log.info("Added confirmed cashback to balance for user {}", userId);
                        } else if (cashback.isPending()) {
                            // Order pending → Add to pending_balance
                            addCashbackToWalletUseCase.addPendingCashback(cashback.getId());
                            log.info("Added pending cashback to pending balance for user {}", userId);
                        }

                    } else {
                        log.debug("Order {} has no commission, skipping cashback creation", order.getId());
                    }

                } catch (Exception e) {
                    // Don't fail entire import if cashback fails
                    log.error("Failed to create cashback for order {}: {}",
                        order.getId(), e.getMessage(), e);
                    // Continue with next record
                }

                // Match with click if found
                if (click != null && request.getAutoMatch()) {
                    click.matchWithOrder(order.getId());
                    clickRepository.save(click);
                    matchedCount[0]++;
                    log.debug("Matched order {} with click {}", order.getId(), click.getId());
                }

                batch.incrementSuccess();

                // Flush and clear every 50 records to prevent memory buildup
                if (processedInThisBatch % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.debug("Flushed and cleared EntityManager after {} records in batch", processedInThisBatch);
                }

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

        // Save batch after processing all records in this batch
        batchRepository.save(batch);

        // Final flush and clear for this batch
        entityManager.flush();
        entityManager.clear();

        log.info("Completed batch processing: {} records processed, {} successful, {} failed, {} skipped",
            processedInThisBatch, batch.getSuccessCount(), batch.getFailedCount(), batch.getSkippedCount());
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
     * Update existing order with new data from CSV.
     *
     * @param existingOrder Existing order from database
     * @param record CSV record with new data
     * @param batch Import batch
     */
    private void updateExistingOrder(AffiliateOrder existingOrder,
                                     ShopeeCSVParser.ShopeeOrderRecord record,
                                     ImportBatch batch) {

        log.info("UseCase: Updating existing order: {} (old status: {})",
            existingOrder.getOrderId(), existingOrder.getOrderStatus());

        try {
            // Get old status before update
            OrderStatus oldStatus = existingOrder.getOrderStatus();
            OrderStatus newStatus = record.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING;

            // Check if status changed
            boolean statusChanged = oldStatus != newStatus;

            // Update order fields
            existingOrder.setOrderStatus(newStatus);
            existingOrder.setProductName(record.getItemName());
            existingOrder.setProductPrice(record.getPrice());
            existingOrder.setCommissionAmount(record.getCommissionForCashback());
            existingOrder.setOrderTime(record.getOrderTime());

            // Update time fields based on status
            if (newStatus == OrderStatus.APPROVED && existingOrder.getConfirmTime() == null) {
                existingOrder.setConfirmTime(LocalDateTime.now());
            }
            if (newStatus == OrderStatus.PAID && existingOrder.getPaidTime() == null) {
                existingOrder.setPaidTime(LocalDateTime.now());
            }

            existingOrder.setUpdatedAt(LocalDateTime.now());

            // Validate and save
            existingOrder.validate();
            orderRepository.save(existingOrder);

            log.info("UseCase: Updated order {} (status: {} → {})",
                existingOrder.getOrderId(), oldStatus, newStatus);

            // Update cashback if status changed
            if (statusChanged) {
                log.info("UseCase: Order status changed, updating cashback...");
                updateCashbackOnOrderStatusChangeUseCase.execute(
                    existingOrder.getId(),
                    oldStatus,
                    newStatus
                );
            }

            // Increment updated count
            batch.incrementUpdated();

        } catch (Exception e) {
            batch.incrementFailed();
            log.error("UseCase: Failed to update order {}: {}",
                existingOrder.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Build user-friendly message.
     */
    private String buildMessage(ImportBatch batch, int matchedCount) {
        StringBuilder message = new StringBuilder();

        if (batch.getStatus() == ImportStatus.COMPLETED) {
            message.append(String.format("Successfully processed %d rows. ", batch.getTotalRows()));
            if (batch.getSuccessCount() > 0) {
                message.append(String.format("%d new orders imported. ", batch.getSuccessCount()));
            }
            if (batch.getUpdatedCount() > 0) {
                message.append(String.format("%d orders updated. ", batch.getUpdatedCount()));
            }
            if (matchedCount > 0) {
                message.append(String.format("%d orders matched with clicks.", matchedCount));
            }
        } else if (batch.getStatus() == ImportStatus.PARTIAL) {
            message.append(String.format("Partially processed: %d new, %d updated, %d failed, %d skipped. ",
                batch.getSuccessCount(), batch.getUpdatedCount(),
                batch.getFailedCount(), batch.getSkippedCount()));
            if (matchedCount > 0) {
                message.append(String.format("%d orders matched.", matchedCount));
            }
        } else {
            message.append(String.format("Import failed. %d orders failed to import.",
                batch.getFailedCount()));
        }

        return message.toString().trim();
    }
}
