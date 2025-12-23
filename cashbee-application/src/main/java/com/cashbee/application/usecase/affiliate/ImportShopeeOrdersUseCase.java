package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.FallbackMatchResult;
import com.cashbee.application.dto.affiliate.ImportOrdersRequest;
import com.cashbee.application.dto.affiliate.ImportOrdersResponse;
import com.cashbee.application.usecase.cashback.CalculateCashbackUseCase;
import com.cashbee.application.usecase.wallet.RecalculateWalletUseCase;
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
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.ImportBatchRepository;
import com.cashbee.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final AffiliateOrderItemRepository orderItemRepository;
    private final AffiliateClickRepository clickRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final UserRepository userRepository;
    private final ShopeeCSVParser csvParser;
    private final TrackingCodeGenerator trackingCodeGenerator;
    private final CalculateCashbackUseCase calculateCashbackUseCase;
    private final RecalculateWalletUseCase recalculateWalletUseCase;
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
        final int[] fallbackMatchedCount = {0};  // Track orders matched via fallback
        final int[] multipleMatchSkippedCount = {0};  // Track orders skipped due to multiple matches
        final int[] totalRowsProcessed = {0};
        final Set<Long> affectedUserIds = new HashSet<>();  // Track users to recalculate wallets

        try {
            // Use streaming parser to process records in batches
            csvParser.parseStreaming(request.getFileInputStream(), BATCH_SIZE, recordBatch -> {
                // Process this batch in a separate transaction
                processBatch(recordBatch, platform, batchId, request, errors, matchedCount,
                    fallbackMatchedCount, multipleMatchSkippedCount, totalRowsProcessed, affectedUserIds);
            });

            // Update total rows after parsing
            // NOTE: Do NOT reload batch here! The batch object is already up-to-date
            // from processBatch(). Reloading after entityManager.clear() may get stale
            // data since the transaction hasn't committed yet, which can cause:
            // 1. Incorrect batch counts (updatedCount = 0 instead of actual value)
            // 2. Potential wallet data corruption from cascade effects
            batch.setTotalRows(totalRowsProcessed[0]);
            batch = batchRepository.save(batch);
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
        // NOTE: Do NOT reload batch here! After entityManager.clear() in processBatch,
        // reloading from DB may get stale data since transaction hasn't committed yet.
        // The batch object is already up-to-date from processBatch.
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

        log.info("UseCase: Import completed. New orders: {}, Updated: {}, Failed: {}, Skipped: {}, " +
                "Orders matched with clicks: {} (fallback: {}, multiple match skipped: {})",
            batch.getSuccessCount(), batch.getUpdatedCount(), batch.getFailedCount(),
            batch.getSkippedCount(), matchedCount[0], fallbackMatchedCount[0], multipleMatchSkippedCount[0]);

        // Q3 - Option B: Rollback if ANY errors occurred
        if (batch.getFailedCount() > 0) {
            String errorMessage = String.format(
                "Import failed with %d errors. Transaction will be rolled back. " +
                "First error: %s",
                batch.getFailedCount(),
                errors.isEmpty() ? "Unknown error" : errors.get(0).getError()
            );
            log.error("UseCase: {}", errorMessage);
            throw new BusinessException("IMPORT_FAILED", errorMessage);
        }

        // Step 5: Recalculate wallets for all affected users
        // CRITICAL FIX: Always recalculate wallet from cashback (source of truth)
        // This ensures wallet stays in sync even when:
        // - Re-importing same file (status unchanged, but wallet may have been reset)
        // - Partial imports with mixed status changes
        // - Any edge cases where direct wallet updates may have failed
        if (!affectedUserIds.isEmpty()) {
            log.info("UseCase: Recalculating wallets for {} affected users: {}",
                affectedUserIds.size(), affectedUserIds);
            recalculateWalletUseCase.executeForUsers(affectedUserIds);
            log.info("UseCase: Wallet recalculation completed for {} users", affectedUserIds.size());
        }

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
            .updatedCount(batch.getUpdatedCount())
            .matchedCount(matchedCount[0])
            .fallbackMatchedCount(fallbackMatchedCount[0])
            .multipleMatchSkippedCount(multipleMatchSkippedCount[0])
            .successRate(batch.getSuccessRate())
            .errors(errors)
            .startedAt(startTime)
            .completedAt(endTime)
            .durationSeconds(durationSeconds)
            .importedBy(request.getImportedBy())
            .message(buildMessage(batch, matchedCount[0], fallbackMatchedCount[0], multipleMatchSkippedCount[0]))
            .build();
    }

    /**
     * Process a batch of CSV records.
     * This method is called for each batch during streaming parsing.
     * Each batch is processed with flush & clear to prevent memory buildup.
     *
     * IMPORTANT: Each CSV row is an ITEM, not an ORDER!
     * Multiple rows can have the same orderId (one order with multiple items).
     * This method groups items by orderId and processes each order once.
     *
     * TRANSACTION STRATEGY (Q3 - Option B):
     * - Uses MANDATORY propagation to run in parent transaction
     * - If ANY error occurs, ENTIRE import will rollback
     * - This ensures data consistency but may fail the whole import for 1 bad record
     *
     * @param recordBatch List of records in this batch
     * @param platform Affiliate platform
     * @param batchId Import batch ID
     * @param request Import request
     * @param errors List to collect errors
     * @param matchedCount Counter for matched orders
     * @param fallbackMatchedCount Counter for orders matched via fallback
     * @param multipleMatchSkippedCount Counter for orders skipped due to multiple matches
     * @param totalRowsProcessed Counter for total rows processed
     * @param affectedUserIds Set to collect affected user IDs
     */
    /**
     * NOTE: Removed @Transactional(propagation = Propagation.MANDATORY) to fix UnexpectedRollbackException.
     *
     * ROOT CAUSE: When @Transactional is present, Spring AOP intercepts exceptions and marks
     * the transaction as rollback-only BEFORE the exception propagates to the caller.
     * Even if we catch the exception inside this method, the transaction is already marked
     * for rollback, causing UnexpectedRollbackException when the outer transaction commits.
     *
     * This method is already running within execute()'s transaction (Propagation.REQUIRED),
     * so removing @Transactional allows proper exception handling without premature rollback marking.
     */
    @SuppressWarnings("java:S3776") // Complexity is acceptable for debug logging
    protected void processBatch(List<ShopeeCSVParser.ShopeeOrderRecord> recordBatch,
                                AffiliatePlatform platform,
                                Long batchId,
                                ImportOrdersRequest request,
                                List<ImportOrdersResponse.ImportErrorDetail> errors,
                                int[] matchedCount,
                                int[] fallbackMatchedCount,
                                int[] multipleMatchSkippedCount,
                                int[] totalRowsProcessed,
                                Set<Long> affectedUserIds) {

        // CRITICAL FIX: Disable Hibernate auto-flush to prevent "null id" errors
        // When auto-flush is enabled (default), Hibernate flushes the session before EVERY query
        // to ensure data consistency. If the session contains entities with null IDs (due to
        // previous failed persist operations), auto-flush will fail with "null id in entity entry".
        //
        // By setting FlushModeType.COMMIT, we tell Hibernate to only flush:
        // 1. When we explicitly call flush()
        // 2. When the transaction commits
        //
        // This allows us to continue processing orders even after some fail, without
        // the failed entities corrupting subsequent operations.
        FlushModeType originalFlushMode = entityManager.getFlushMode();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        log.debug("Set FlushMode to COMMIT (was: {})", originalFlushMode);

        ImportBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new NotFoundException("Import batch not found"));

        // Step 1: Group records by orderId
        // Key: orderId (String), Value: List of items for that order
        Map<String, List<ShopeeCSVParser.ShopeeOrderRecord>> orderItemsMap = new HashMap<>();

        for (ShopeeCSVParser.ShopeeOrderRecord record : recordBatch) {
            totalRowsProcessed[0]++;

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

            // Group by orderId (including cancelled orders - they need to be processed to cancel cashback)
            String orderId = record.getOrderId();
            if (orderId != null && !orderId.isBlank()) {
                orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(record);
            } else {
                batch.incrementSkipped();
                log.warn("Skipping record with no orderId at row {}", record.getRowNumber());
            }
        }

        log.info("Grouped {} items into {} unique orders", recordBatch.size(), orderItemsMap.size());

        // Step 2: Process each unique order
        // IMPORTANT: Track if any error occurred to prevent flush after exception
        // Hibernate session becomes corrupted after an exception, and calling flush()
        // will cause "null id in entity entry" errors for pending entities
        int processedOrders = 0;
        int successfulOrders = 0;
        boolean hasError = false;

        for (Map.Entry<String, List<ShopeeCSVParser.ShopeeOrderRecord>> entry : orderItemsMap.entrySet()) {
            String orderId = entry.getKey();
            List<ShopeeCSVParser.ShopeeOrderRecord> items = entry.getValue();

            try {
                processOrderWithItems(orderId, items, platform, batchId, request, errors,
                    matchedCount, fallbackMatchedCount, multipleMatchSkippedCount, batch, affectedUserIds);
                processedOrders++;
                successfulOrders++;

                // Flush and clear every 50 SUCCESSFUL orders to prevent memory buildup
                // CRITICAL: Only flush when there are no errors - Hibernate session is corrupted after exception
                if (!hasError && successfulOrders % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    // Re-fetch batch after clear to avoid detached entity issues
                    batch = batchRepository.findById(batchId)
                        .orElseThrow(() -> new NotFoundException("Import batch not found"));
                    log.debug("Flushed and cleared EntityManager after {} successful orders", successfulOrders);
                }

            } catch (Exception e) {
                hasError = true;  // Mark that we had an error - don't flush anymore
                batch.incrementFailed();
                log.error("Failed to process order {}: {}", orderId, e.getMessage(), e);
                errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                    .rowNumber(items.get(0).getRowNumber())
                    .orderId(orderId)
                    .error(e.getMessage())
                    .rawData(items.get(0).getRawData())
                    .build());
                processedOrders++;
            }
        }

        // Save batch after processing all records in this batch
        batch = batchRepository.save(batch);

        // Log BEFORE clear to show correct counts
        log.info("Completed batch processing: {} unique orders processed ({} new, {} updated, {} failed, {} skipped)",
            processedOrders, batch.getSuccessCount(), batch.getUpdatedCount(),
            batch.getFailedCount(), batch.getSkippedCount());

        // Restore original flush mode before cleanup
        entityManager.setFlushMode(originalFlushMode);
        log.debug("Restored FlushMode to {}", originalFlushMode);

        // Final flush and clear for this batch
        // With FlushMode.COMMIT, the session should NOT contain corrupted entities
        // since auto-flush was disabled during processing.
        // However, we still check hasError for safety.
        if (!hasError) {
            entityManager.flush();
            entityManager.clear();
        } else {
            log.warn("Skipping final flush due to previous errors - clearing session only");
            // Clear without flush to release memory
            // The transaction manager will handle rollback of uncommitted changes
            entityManager.clear();
        }
    }

    /**
     * Process a single order with all its items.
     * Aggregates commission from all items and creates/updates order once.
     * Creates AffiliateOrderItem records for each item.
     * Calculates cashback ONCE based on total commission.
     *
     * @param orderId Platform order ID
     * @param items List of item records from CSV
     * @param platform Affiliate platform
     * @param batchId Import batch ID
     * @param request Import request
     * @param errors Error list
     * @param matchedCount Match counter
     * @param fallbackMatchedCount Counter for orders matched via fallback
     * @param multipleMatchSkippedCount Counter for orders skipped due to multiple matches
     * @param batch Import batch
     * @param affectedUserIds Set to collect affected user IDs
     */
    private void processOrderWithItems(String orderId,
                                       List<ShopeeCSVParser.ShopeeOrderRecord> items,
                                       AffiliatePlatform platform,
                                       Long batchId,
                                       ImportOrdersRequest request,
                                       List<ImportOrdersResponse.ImportErrorDetail> errors,
                                       int[] matchedCount,
                                       int[] fallbackMatchedCount,
                                       int[] multipleMatchSkippedCount,
                                       ImportBatch batch,
                                       Set<Long> affectedUserIds) {

        log.debug("Processing order {} with {} items", orderId, items.size());

        // Use first item for order-level data (tracking code, order time, etc.)
        ShopeeCSVParser.ShopeeOrderRecord firstItem = items.get(0);

        // Aggregate data from all items
        // IMPORTANT: Items can have DIFFERENT statuses within the same order!
        // We need to track commission separately for completed vs pending vs cancelled vs unpaid items
        BigDecimal totalCommission = BigDecimal.ZERO;           // Total commission from ALL items
        BigDecimal completedCommission = BigDecimal.ZERO;       // Commission from COMPLETED items only
        BigDecimal pendingCommission = BigDecimal.ZERO;         // Commission from PENDING items only
        BigDecimal cancelledCommission = BigDecimal.ZERO;       // Commission from CANCELLED items only
        BigDecimal totalPrice = BigDecimal.ZERO;
        int completedItemCount = 0;
        int pendingItemCount = 0;
        int cancelledItemCount = 0;
        int unpaidItemCount = 0;  // Track unpaid items (Chưa thanh toán)
        StringBuilder productNames = new StringBuilder();

        for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
            BigDecimal itemCommission = item.getCommissionForCashback();

            // Sum commission by status
            if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
                totalCommission = totalCommission.add(itemCommission);

                if (item.isCancelled()) {
                    cancelledCommission = cancelledCommission.add(itemCommission);
                    cancelledItemCount++;
                } else if (item.isUnpaid()) {
                    // Unpaid items - don't add to pending/completed commission
                    // They may become valid later when paid
                    unpaidItemCount++;
                } else if (item.isCompleted()) {
                    completedCommission = completedCommission.add(itemCommission);
                    completedItemCount++;
                } else {
                    pendingCommission = pendingCommission.add(itemCommission);
                    pendingItemCount++;
                }
            } else if (item.isCancelled()) {
                // Count cancelled items even if commission is 0
                cancelledItemCount++;
            } else if (item.isUnpaid()) {
                // Count unpaid items even if commission is 0
                unpaidItemCount++;
            }

            // Sum price
            if (item.getPrice() != null) {
                totalPrice = totalPrice.add(item.getPrice());
            }

            // Collect product names (max 200 chars)
            if (productNames.length() < 200 && item.getItemName() != null) {
                if (productNames.length() > 0) {
                    productNames.append(", ");
                }
                productNames.append(item.getItemName());
            }
        }

        // Determine order status based on items
        // Skip if ALL items are unpaid (customer hasn't paid yet)
        // CANCELLED if ALL items cancelled
        // APPROVED if ALL non-cancelled items completed
        // PENDING if ANY item is pending
        boolean allItemsUnpaid = (unpaidItemCount > 0 && completedItemCount == 0 && pendingItemCount == 0 && cancelledItemCount == 0);
        boolean allItemsCancelled = (cancelledItemCount > 0 && completedItemCount == 0 && pendingItemCount == 0 && unpaidItemCount == 0);
        boolean allItemsCompleted = (pendingItemCount == 0 && completedItemCount > 0 && unpaidItemCount == 0);
        boolean isOrderCompleted = allItemsCompleted;
        boolean isOrderCancelled = allItemsCancelled;

        log.debug("Order {} aggregated: total={}, completed={} ({}items), pending={} ({}items), cancelled={} ({}items), unpaid={} ({}items)",
            orderId, totalCommission, completedCommission, completedItemCount,
            pendingCommission, pendingItemCount, cancelledCommission, cancelledItemCount,
            BigDecimal.ZERO, unpaidItemCount);

        // Skip if ALL items are unpaid (customer hasn't paid yet - no commission earned)
        if (allItemsUnpaid) {
            batch.incrementSkipped();
            log.debug("Skipping unpaid order: {} (all {} items are unpaid)", orderId, unpaidItemCount);
            return;
        }

        // Check for existing order
        AffiliateOrder existingOrder = orderRepository.findByOrderId(orderId).orElse(null);

        if (existingOrder != null) {
            // Order exists - handle based on updateMode
            if (request.getUpdateMode() == UpdateMode.SKIP) {
                batch.incrementSkipped();
                log.debug("Skipping duplicate order: {}", orderId);
                return;
            } else if (request.getUpdateMode() == UpdateMode.UPDATE) {
                // Update existing order with aggregated data (including cancellation handling)
                updateExistingOrderWithItems(existingOrder, items, totalCommission, totalPrice,
                    isOrderCompleted, isOrderCancelled, productNames.toString(), batch, platform.getId(),
                    completedCommission, pendingCommission, affectedUserIds);
                return;
            }
        }

        // For new cancelled orders, skip (no need to create order/cashback for cancelled orders)
        if (isOrderCancelled) {
            batch.incrementSkipped();
            log.debug("Skipping new cancelled order: {}", orderId);
            return;
        }

        // Extract user ID from tracking code
        Long userId = null;
        AffiliateClick click = null;
        boolean isFallbackMatch = false;

        if (firstItem.hasTrackingCode()) {
            try {
                userId = trackingCodeGenerator.extractUserId(firstItem.getTrackingCode());
                log.debug("Extracted user ID {} from tracking code {}", userId, firstItem.getTrackingCode());

                // Validate user exists
                if (!userRepository.existsById(userId)) {
                    batch.incrementSkipped();
                    log.warn("User ID {} does not exist, skipping order {}", userId, orderId);
                    errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                        .rowNumber(firstItem.getRowNumber())
                        .orderId(orderId)
                        .error(String.format("User ID %d does not exist", userId))
                        .rawData(firstItem.getRawData())
                        .build());
                    return;
                }

                // Find click if autoMatch enabled
                if (request.getAutoMatch()) {
                    click = clickRepository.findByTrackingCode(firstItem.getTrackingCode()).orElse(null);
                }
            } catch (IllegalArgumentException e) {
                batch.incrementSkipped();
                log.warn("Invalid tracking code format: {}", firstItem.getTrackingCode());
                errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                    .rowNumber(firstItem.getRowNumber())
                    .orderId(orderId)
                    .error("Invalid tracking code: " + e.getMessage())
                    .rawData(firstItem.getRawData())
                    .build());
                return;
            }
        }

        // ========== FALLBACK MATCHING ==========
        // If no tracking code, try to match by context (item + shop + time window)
        // Loop through ALL items in the order to find a match (not just firstItem)
        if (userId == null && request.getAutoMatch()) {
            log.info("[FALLBACK] Order {} has no tracking code, attempting fallback matching with {} items. autoMatch={}",
                orderId, items.size(), request.getAutoMatch());

            FallbackMatchResult fallbackResult = FallbackMatchResult.noMatch();
            ShopeeCSVParser.ShopeeOrderRecord matchedItem = null;

            // Try fallback matching with each item in the order
            for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
                log.info("[FALLBACK] Order {} - Checking item: itemId={}, shopId={}, orderTime={}",
                    orderId, item.getItemId(), item.getShopId(), item.getOrderTime());

                if (item.getItemId() == null || item.getShopId() == null) {
                    log.warn("[FALLBACK] Order {} - Skipping item with null itemId or shopId", orderId);
                    continue; // Skip items without itemId/shopId
                }

                LocalDateTime effectiveOrderTime = item.getOrderTime() != null ? item.getOrderTime() : firstItem.getOrderTime();
                log.info("[FALLBACK] Order {} - Calling attemptFallbackMatch(platformId={}, itemId={}, shopId={}, orderTime={})",
                    orderId, platform.getId(), item.getItemId(), item.getShopId(), effectiveOrderTime);

                fallbackResult = attemptFallbackMatch(
                    platform.getId(),
                    item.getItemId(),
                    item.getShopId(),
                    effectiveOrderTime
                );

                log.info("[FALLBACK] Order {} - Result: isUniqueMatch={}, hasMultipleMatches={}, matchCount={}",
                    orderId, fallbackResult.isUniqueMatch(), fallbackResult.hasMultipleMatches(),
                    fallbackResult.getMatchCount());

                if (fallbackResult.isUniqueMatch() || fallbackResult.hasMultipleMatches()) {
                    matchedItem = item;
                    log.info("[FALLBACK] Order {} - Found match using item {} from shop {}",
                        orderId, item.getItemId(), item.getShopId());
                    break; // Found a match, stop searching
                }
            }

            if (fallbackResult.isUniqueMatch()) {
                // Found exactly one matching click - auto-assign
                click = fallbackResult.getClick();
                userId = click.getUserId();
                isFallbackMatch = true;
                fallbackMatchedCount[0]++;
                log.info("[FALLBACK] SUCCESS! Order {} matched with click {} (user {}) via item {} shop {}",
                    orderId, click.getId(), userId,
                    matchedItem != null ? matchedItem.getItemId() : "unknown",
                    matchedItem != null ? matchedItem.getShopId() : "unknown");
            } else if (fallbackResult.hasMultipleMatches()) {
                // Multiple possible matches - skip and log for admin review
                multipleMatchSkippedCount[0]++;
                batch.incrementSkipped();
                log.warn("[FALLBACK] Order {} has {} possible matches from different users, skipping: {}",
                    orderId, fallbackResult.getMatchCount(), fallbackResult.getPossibleUserIds());
                errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                    .rowNumber(firstItem.getRowNumber())
                    .orderId(orderId)
                    .error(String.format("Multiple fallback matches found (%d users: %s). Manual review required.",
                        fallbackResult.getMatchCount(), fallbackResult.getPossibleUserIds()))
                    .rawData(firstItem.getRawData())
                    .build());
                return;
            } else {
                // No matches found after trying all items
                log.warn("[FALLBACK] Order {} - NO MATCH found after trying {} items",
                    orderId, items.size());
            }
        } else if (userId == null) {
            log.info("[FALLBACK] Order {} - Skipped fallback matching. userId={}, autoMatch={}",
                orderId, userId, request.getAutoMatch());
        }

        if (userId == null) {
            batch.incrementSkipped();
            log.warn("No tracking code and no fallback match for order {}", orderId);
            errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                .rowNumber(firstItem.getRowNumber())
                .orderId(orderId)
                .error("No tracking code (Sub_id1) and no fallback match found")
                .rawData(firstItem.getRawData())
                .build());
            return;
        }

        // Create AffiliateOrder with aggregated data
        AffiliateOrder order = AffiliateOrder.builder()
            .platformId(platform.getId())
            .userId(userId)
            .orderId(orderId)
            .clickId(isFallbackMatch ? null : firstItem.getTrackingCode())  // No tracking code for fallback
            .productName(truncateString(productNames.toString(), 255))
            .productPrice(totalPrice)
            .commissionAmount(totalCommission)  // TOTAL commission from all items
            .currency("VND")
            .orderTime(firstItem.getOrderTime())
            .orderStatus(isOrderCompleted ? OrderStatus.APPROVED : OrderStatus.PENDING)
            .source("IMPORT")
            .importBatchId(batchId)
            .fallbackMatch(isFallbackMatch)  // Track if this was a fallback match
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        order.validate();
        order = orderRepository.save(order);
        log.debug("Created order {} (id={}) for user {} with total commission {}",
            orderId, order.getId(), userId, totalCommission);

        // Create AffiliateOrderItem records for each item, each with its own cashback
        // Skip unpaid and cancelled items for NEW orders
        final Long savedOrderId = order.getId();
        final Long finalUserId = userId;

        // CRITICAL FIX: Deduplicate items by itemId + modelId to prevent duplicate key errors
        // Shopee CSV may contain multiple rows for the same item (e.g., different status snapshots)
        // We keep the most recent/relevant record and aggregate commission
        Map<String, ShopeeCSVParser.ShopeeOrderRecord> uniqueItems = deduplicateItems(items);
        log.debug("Deduplicated {} items to {} unique items for order {}", items.size(), uniqueItems.size(), orderId);

        int createdItemCount = 0;
        for (ShopeeCSVParser.ShopeeOrderRecord item : uniqueItems.values()) {
            // Skip unpaid items - they haven't been paid yet, no commission earned
            if (item.isUnpaid()) {
                log.debug("Skipping unpaid item {} for order {}", item.getItemId(), orderId);
                continue;
            }

            // Skip cancelled items for new orders
            if (item.isCancelled()) {
                log.debug("Skipping cancelled item {} for new order {}", item.getItemId(), orderId);
                continue;
            }

            // Determine item status
            OrderStatus itemStatus = item.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING;

            AffiliateOrderItem orderItem = AffiliateOrderItem.builder()
                .orderId(savedOrderId)
                .itemId(item.getItemId())
                .modelId(item.getModelId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                .actualAmount(item.getPrice())
                .itemCommission(item.getCommissionForCashback())
                .shopId(item.getShopId())
                .shopName(item.getShopName())
                .categoryLv1(item.getCategoryLv1())
                .categoryLv2(item.getCategoryLv2())
                .categoryLv3(item.getCategoryLv3())
                .status(itemStatus)
                .createdAt(LocalDateTime.now())
                .build();
            AffiliateOrderItem savedItem = orderItemRepository.save(orderItem);
            createdItemCount++;

            // Create cashback for this item (each item has its own cashback)
            BigDecimal itemCommission = item.getCommissionForCashback();
            if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
                Cashback cashback = calculateCashbackUseCase.executeForItem(
                    finalUserId,
                    savedOrderId,
                    savedItem.getId(),
                    platform.getId(),
                    itemCommission,
                    item.isCompleted()
                );
                log.debug("Created cashback {} for item {} (status: {})",
                    cashback.getId(), item.getItemId(), cashback.getStatus());
            }
        }
        log.debug("Created {} order items with cashbacks for order {} (skipped {} unpaid/cancelled items)",
            createdItemCount, orderId, items.size() - createdItemCount);

        // Track affected user for wallet recalculation
        affectedUserIds.add(finalUserId);

        // Match with click if found
        // Note: An order can be BOTH "new" (successCount) AND "matched" (matchedCount)
        // - successCount tracks NEW orders created
        // - matchedCount tracks orders that have a corresponding click record
        if (click != null && request.getAutoMatch()) {
            click.matchWithOrder(order.getId());
            clickRepository.save(click);
            matchedCount[0]++;
            log.debug("Matched order {} with click {}", orderId, click.getId());
        }

        // Increment success count for new order
        batch.incrementSuccess();
    }

    /**
     * Update existing order with aggregated item data.
     * Also updates/creates AffiliateOrderItem records.
     *
     * Handles partial completion:
     * - If order was PENDING and now some items completed → update cashback incrementally
     * - If order was PENDING and all items completed → move all from pending_balance to balance
     *
     * @param existingOrder Existing order
     * @param items List of item records
     * @param totalCommission Aggregated commission from ALL items
     * @param totalPrice Aggregated price
     * @param isOrderCompleted Whether ALL items are completed
     * @param productNames Concatenated product names
     * @param batch Import batch
     * @param platformId Platform ID for cashback calculation
     * @param completedCommission Commission from COMPLETED items only
     * @param pendingCommission Commission from PENDING items only
     */
    private void updateExistingOrderWithItems(AffiliateOrder existingOrder,
                                              List<ShopeeCSVParser.ShopeeOrderRecord> items,
                                              BigDecimal totalCommission,
                                              BigDecimal totalPrice,
                                              boolean isOrderCompleted,
                                              boolean isOrderCancelled,
                                              String productNames,
                                              ImportBatch batch,
                                              Long platformId,
                                              BigDecimal completedCommission,
                                              BigDecimal pendingCommission,
                                              Set<Long> affectedUserIds) {

        log.info("Updating existing order: {} (old status: {}, completed: {}, cancelled: {}, completed commission: {}, pending commission: {})",
            existingOrder.getOrderId(), existingOrder.getOrderStatus(), isOrderCompleted, isOrderCancelled,
            completedCommission, pendingCommission);

        try {
            OrderStatus oldStatus = existingOrder.getOrderStatus();
            // Determine new status: CANCELLED > APPROVED > PENDING
            OrderStatus newStatus;
            if (isOrderCancelled) {
                newStatus = OrderStatus.CANCELLED;
            } else if (isOrderCompleted) {
                newStatus = OrderStatus.APPROVED;
            } else {
                newStatus = OrderStatus.PENDING;
            }
            boolean statusChanged = oldStatus != newStatus;

            // Update order with aggregated data
            existingOrder.setOrderStatus(newStatus);
            existingOrder.setProductName(truncateString(productNames, 255));
            existingOrder.setProductPrice(totalPrice);
            existingOrder.setCommissionAmount(totalCommission);
            existingOrder.setOrderTime(items.get(0).getOrderTime());

            if (newStatus == OrderStatus.APPROVED && existingOrder.getConfirmTime() == null) {
                existingOrder.setConfirmTime(LocalDateTime.now());
            }

            existingOrder.setUpdatedAt(LocalDateTime.now());
            existingOrder.validate();
            orderRepository.save(existingOrder);

            log.info("Updated order {} (status: {} → {}, commission: {})",
                existingOrder.getOrderId(), oldStatus, newStatus, totalCommission);

            // Update order items: upsert each item by order_id + item_id
            // CRITICAL FIX: Deduplicate items by itemId + modelId to prevent duplicate processing
            Map<String, ShopeeCSVParser.ShopeeOrderRecord> uniqueItems = deduplicateItems(items);
            log.info("[DEBUG] Processing {} items for order {} (deduplicated from {})",
                uniqueItems.size(), existingOrder.getOrderId(), items.size());

            int itemIndex = 0;
            for (ShopeeCSVParser.ShopeeOrderRecord item : uniqueItems.values()) {
                itemIndex++;
                log.info("[DEBUG] Item {}/{}: itemId={}, modelId={}, cancelled={}, completed={}, unpaid={}, commission={}",
                    itemIndex, uniqueItems.size(), item.getItemId(), item.getModelId(),
                    item.isCancelled(), item.isCompleted(), item.isUnpaid(), item.getCommissionForCashback());

                // Skip unpaid items - they haven't been paid yet, no commission earned
                // When the customer pays, the order will be re-imported with a different status
                if (item.isUnpaid()) {
                    log.info("[DEBUG] SKIP unpaid item {} for order {}", item.getItemId(), existingOrder.getOrderId());
                    continue;
                }

                // Determine item status: CANCELLED > APPROVED > PENDING
                OrderStatus itemStatus;
                if (item.isCancelled()) {
                    itemStatus = OrderStatus.CANCELLED;
                } else if (item.isCompleted()) {
                    itemStatus = OrderStatus.APPROVED;
                } else {
                    itemStatus = OrderStatus.PENDING;
                }
                log.info("[DEBUG] Item {} determined status: {}", item.getItemId(), itemStatus);

                // Find existing item by order_id + item_id + model_id (unique combination)
                log.info("[DEBUG] Searching existingItem: orderId={}, itemId={}, modelId={}",
                    existingOrder.getId(), item.getItemId(), item.getModelId());
                AffiliateOrderItem existingItem = orderItemRepository
                    .findByOrderIdAndItemIdAndModelId(existingOrder.getId(), item.getItemId(), item.getModelId())
                    .orElse(null);
                log.info("[DEBUG] existingItem found: {}", existingItem != null ? existingItem.getId() : "NULL");

                AffiliateOrderItem orderItem;
                if (existingItem != null) {
                    // Update existing item
                    existingItem.setItemName(item.getItemName());
                    existingItem.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
                    existingItem.setActualAmount(item.getPrice());
                    existingItem.setItemCommission(item.getCommissionForCashback());
                    existingItem.setShopId(item.getShopId());
                    existingItem.setShopName(item.getShopName());
                    existingItem.setCategoryLv1(item.getCategoryLv1());
                    existingItem.setCategoryLv2(item.getCategoryLv2());
                    existingItem.setCategoryLv3(item.getCategoryLv3());
                    existingItem.setStatus(itemStatus);
                    orderItem = orderItemRepository.save(existingItem);
                    log.debug("Updated item {} for order {} (status: {})", item.getItemId(), existingOrder.getOrderId(), itemStatus);
                } else {
                    // Item doesn't exist in DB
                    log.info("[DEBUG] existingItem is NULL, item.isCancelled()={}", item.isCancelled());
                    if (item.isCancelled()) {
                        // CRITICAL FIX: Even if item doesn't exist by itemId, we still need to
                        // try to cancel any existing cashback for this order.
                        // This handles the case where Shopee changes itemId when order is cancelled.
                        log.info("[DEBUG] Cancelled item {} not found by itemId for order {}, will try to cancel by orderId",
                            item.getItemId(), existingOrder.getOrderId());

                        // Try to cancel cashback using orderId (will search by orderId in upsertForItemWithCancellation)
                        log.info("[DEBUG] Calling upsertForItemWithCancellation with orderItemId=NULL for cancelled item");
                        calculateCashbackUseCase.upsertForItemWithCancellation(
                            existingOrder.getUserId(),
                            existingOrder.getId(),
                            null,  // No orderItemId since item doesn't exist
                            platformId,
                            BigDecimal.ZERO,  // Amount will be taken from existing cashback
                            false,
                            true  // isCancelled = true
                        );
                        log.info("[DEBUG] upsertForItemWithCancellation completed for cancelled item without existing DB record");
                        continue;
                    }
                    orderItem = AffiliateOrderItem.builder()
                        .orderId(existingOrder.getId())
                        .itemId(item.getItemId())
                        .modelId(item.getModelId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                        .actualAmount(item.getPrice())
                        .itemCommission(item.getCommissionForCashback())
                        .shopId(item.getShopId())
                        .shopName(item.getShopName())
                        .categoryLv1(item.getCategoryLv1())
                        .categoryLv2(item.getCategoryLv2())
                        .categoryLv3(item.getCategoryLv3())
                        .status(itemStatus)
                        .createdAt(LocalDateTime.now())
                        .build();
                    orderItem = orderItemRepository.save(orderItem);
                    log.debug("Created new item {} (model: {}) for order {}", item.getItemId(), item.getModelId(), existingOrder.getOrderId());
                }

                // Upsert cashback for this item (including cancellation handling)
                BigDecimal itemCommission = item.getCommissionForCashback();
                // For cancelled items, we still need to process to cancel cashback
                // For non-cancelled items, skip if no commission
                log.info("[DEBUG] Checking if should call upsertForItemWithCancellation: isCancelled={}, itemCommission={}",
                    item.isCancelled(), itemCommission);
                if (item.isCancelled() || (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0)) {
                    log.info("[DEBUG] Calling upsertForItemWithCancellation: userId={}, orderId={}, orderItemId={}, platformId={}, commission={}, completed={}, cancelled={}",
                        existingOrder.getUserId(), existingOrder.getId(), orderItem.getId(), platformId,
                        itemCommission != null ? itemCommission : BigDecimal.ZERO, item.isCompleted(), item.isCancelled());
                    calculateCashbackUseCase.upsertForItemWithCancellation(
                        existingOrder.getUserId(),
                        existingOrder.getId(),
                        orderItem.getId(),
                        platformId,
                        itemCommission != null ? itemCommission : BigDecimal.ZERO,
                        item.isCompleted(),
                        item.isCancelled()
                    );
                    log.info("[DEBUG] upsertForItemWithCancellation completed for orderItem {}", orderItem.getId());
                } else {
                    log.info("[DEBUG] SKIP upsertForItemWithCancellation - no commission and not cancelled");
                }
            }

            batch.incrementUpdated();

            // Track affected user for wallet recalculation
            affectedUserIds.add(existingOrder.getUserId());

        } catch (Exception e) {
            batch.incrementFailed();
            log.error("Failed to update order {}: {}", existingOrder.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Truncate string to max length.
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
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
     *
     * Note:
     * - successCount = NEW orders created
     * - updatedCount = EXISTING orders updated
     * - matchedCount = Orders (new or existing) that were matched with clicks
     * - fallbackMatchedCount = Orders matched via fallback (context-based)
     * - multipleMatchSkippedCount = Orders skipped due to multiple possible matches
     */
    private String buildMessage(ImportBatch batch, int matchedCount, int fallbackMatchedCount, int multipleMatchSkippedCount) {
        StringBuilder message = new StringBuilder();

        if (batch.getStatus() == ImportStatus.COMPLETED) {
            message.append(String.format("Successfully processed %d rows. ", batch.getTotalRows()));

            int totalProcessed = batch.getSuccessCount() + batch.getUpdatedCount();
            if (totalProcessed > 0) {
                message.append(String.format("%d orders processed ", totalProcessed));

                // Break down by type
                List<String> details = new ArrayList<>();
                if (batch.getSuccessCount() > 0) {
                    details.add(String.format("%d new", batch.getSuccessCount()));
                }
                if (batch.getUpdatedCount() > 0) {
                    details.add(String.format("%d updated", batch.getUpdatedCount()));
                }

                if (!details.isEmpty()) {
                    message.append("(").append(String.join(", ", details)).append("). ");
                }
            }

            if (matchedCount > 0) {
                message.append(String.format("%d orders matched with clicks", matchedCount));
                if (fallbackMatchedCount > 0) {
                    message.append(String.format(" (%d via fallback)", fallbackMatchedCount));
                }
                message.append(". ");
            }

            if (multipleMatchSkippedCount > 0) {
                message.append(String.format("%d orders skipped (multiple matches, needs review). ", multipleMatchSkippedCount));
            }
        } else if (batch.getStatus() == ImportStatus.PARTIAL) {
            message.append(String.format("Partially processed: %d new, %d updated, %d failed, %d skipped. ",
                batch.getSuccessCount(), batch.getUpdatedCount(),
                batch.getFailedCount(), batch.getSkippedCount()));
            if (matchedCount > 0) {
                message.append(String.format("%d orders matched with clicks", matchedCount));
                if (fallbackMatchedCount > 0) {
                    message.append(String.format(" (%d via fallback)", fallbackMatchedCount));
                }
                message.append(". ");
            }
            if (multipleMatchSkippedCount > 0) {
                message.append(String.format("%d orders need manual review. ", multipleMatchSkippedCount));
            }
        } else {
            message.append(String.format("Import failed. %d orders failed to import.",
                batch.getFailedCount()));
        }

        return message.toString().trim();
    }

    /**
     * Attempt to match an order without tracking code by context.
     * SIMPLIFIED VERSION: Only matches by itemId + shopId (NO time restriction).
     *
     * Matching criteria:
     * - Same platform (Shopee)
     * - Same item ID (product that was clicked)
     * - Same shop ID (store that was clicked)
     * - Click not already matched with another order
     *
     * Logic:
     * - If exactly 1 match → auto-assign to that user
     * - If multiple matches from SAME user → use most recent click
     * - If multiple matches from DIFFERENT users → flag for admin review
     *
     * @param platformId Platform ID
     * @param itemId Item ID from order
     * @param shopId Shop ID from order
     * @param orderTime When the order was placed (for logging only)
     * @return FallbackMatchResult indicating match status
     */
    private FallbackMatchResult attemptFallbackMatch(
            Long platformId,
            String itemId,
            String shopId,
            LocalDateTime orderTime) {

        // Skip if missing required fields
        if (itemId == null || shopId == null) {
            log.warn("[FALLBACK-QUERY] Cannot attempt fallback match: itemId={}, shopId={}",
                itemId, shopId);
            return FallbackMatchResult.noMatch();
        }

        // Debug: Log exact values with length to detect hidden characters/whitespace
        log.info("[FALLBACK-QUERY] Searching clicks by itemId+shopId only (NO time restriction): platformId={}, itemId='{}' (len={}), shopId='{}' (len={})",
            platformId, itemId, itemId.length(), shopId, shopId.length());

        // Find clicks matching by itemId + shopId only (NO time restriction)
        List<AffiliateClick> possibleMatches = clickRepository.findPossibleMatchesByItemAndShop(
            platformId,
            itemId,
            shopId
        );

        log.info("[FALLBACK-QUERY] Found {} possible matches for itemId={}, shopId={}",
            possibleMatches.size(), itemId, shopId);

        if (possibleMatches.isEmpty()) {
            log.warn("[FALLBACK-QUERY] No clicks found matching criteria. Check if click exists with: " +
                "platformId={}, itemId={}, shopId={}, orderMatched=false",
                platformId, itemId, shopId);
            return FallbackMatchResult.noMatch();
        }

        if (possibleMatches.size() == 1) {
            AffiliateClick click = possibleMatches.get(0);
            log.info("[FALLBACK-QUERY] Found exactly 1 match: clickId={}, userId={}", click.getId(), click.getUserId());
            return FallbackMatchResult.uniqueMatch(click);
        }

        // Multiple matches - check if all from same user
        long distinctUsers = possibleMatches.stream()
            .map(AffiliateClick::getUserId)
            .distinct()
            .count();

        if (distinctUsers == 1) {
            // All clicks from same user - safe to match with most recent click
            AffiliateClick mostRecentClick = possibleMatches.get(0); // Already sorted by createdAt DESC
            log.info("[FALLBACK-QUERY] Multiple clicks ({}) from same user {}, using most recent click {}",
                possibleMatches.size(), mostRecentClick.getUserId(), mostRecentClick.getId());
            return FallbackMatchResult.uniqueMatch(mostRecentClick);
        }

        // Multiple users - needs admin review
        log.warn("[FALLBACK-QUERY] Multiple clicks from {} different users found, needs admin review", distinctUsers);
        return FallbackMatchResult.multipleMatches(possibleMatches);
    }

    /**
     * Deduplicate items by itemId + modelId.
     *
     * Shopee CSV may contain duplicate rows for the same item. This can happen when:
     * 1. An item appears in multiple status snapshots (e.g., pending then completed)
     * 2. Data export includes redundant records
     *
     * Strategy:
     * - Use itemId + modelId as unique key
     * - When duplicates found, prefer: completed > pending > cancelled > unpaid
     * - Keep the record with higher status priority
     *
     * @param items List of items from CSV
     * @return Map of unique items keyed by "itemId|modelId"
     */
    private Map<String, ShopeeCSVParser.ShopeeOrderRecord> deduplicateItems(
            List<ShopeeCSVParser.ShopeeOrderRecord> items) {

        Map<String, ShopeeCSVParser.ShopeeOrderRecord> uniqueItems = new HashMap<>();

        for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
            String key = item.getItemId() + "|" + item.getModelId();

            ShopeeCSVParser.ShopeeOrderRecord existing = uniqueItems.get(key);
            if (existing == null) {
                uniqueItems.put(key, item);
            } else {
                // Duplicate found - choose the better record
                // Priority: completed > pending > cancelled > unpaid
                int existingPriority = getStatusPriority(existing);
                int newPriority = getStatusPriority(item);

                if (newPriority > existingPriority) {
                    log.debug("Replacing duplicate item {}|{}: {} -> {} (priority {} -> {})",
                        item.getItemId(), item.getModelId(),
                        getStatusDescription(existing), getStatusDescription(item),
                        existingPriority, newPriority);
                    uniqueItems.put(key, item);
                } else {
                    log.debug("Keeping existing item {}|{}: {} (priority {}) over {} (priority {})",
                        item.getItemId(), item.getModelId(),
                        getStatusDescription(existing), existingPriority,
                        getStatusDescription(item), newPriority);
                }
            }
        }

        return uniqueItems;
    }

    /**
     * Get priority score for item status.
     * Higher = better (more valuable for processing)
     */
    private int getStatusPriority(ShopeeCSVParser.ShopeeOrderRecord item) {
        if (item.isCompleted()) return 4;
        if (!item.isCompleted() && !item.isCancelled() && !item.isUnpaid()) return 3; // pending
        if (item.isCancelled()) return 2;
        if (item.isUnpaid()) return 1;
        return 0;
    }

    /**
     * Get human-readable status description.
     */
    private String getStatusDescription(ShopeeCSVParser.ShopeeOrderRecord item) {
        if (item.isCompleted()) return "COMPLETED";
        if (item.isCancelled()) return "CANCELLED";
        if (item.isUnpaid()) return "UNPAID";
        return "PENDING";
    }
}
