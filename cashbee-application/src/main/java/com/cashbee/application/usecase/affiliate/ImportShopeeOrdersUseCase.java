package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.ImportOrdersRequest;
import com.cashbee.application.dto.affiliate.ImportOrdersResponse;
import com.cashbee.application.usecase.cashback.CalculateCashbackUseCase;
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
        final Set<Long> affectedUserIds = new HashSet<>();  // Track users to recalculate wallets

        try {
            // Use streaming parser to process records in batches
            csvParser.parseStreaming(request.getFileInputStream(), BATCH_SIZE, recordBatch -> {
                // Process this batch in a separate transaction
                processBatch(recordBatch, platform, batchId, request, errors, matchedCount, totalRowsProcessed, affectedUserIds);
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

        log.info("UseCase: Import completed. New orders: {}, Updated: {}, Failed: {}, Skipped: {}, Orders matched with clicks: {}",
            batch.getSuccessCount(), batch.getUpdatedCount(), batch.getFailedCount(),
            batch.getSkippedCount(), matchedCount[0]);

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

        // Step 5: Build response
        // NOTE: Wallet is now updated directly in CalculateCashbackUseCase
        // when cashback is created or status changes. No need to recalculate here.
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
     * @param totalRowsProcessed Counter for total rows processed
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected void processBatch(List<ShopeeCSVParser.ShopeeOrderRecord> recordBatch,
                                AffiliatePlatform platform,
                                Long batchId,
                                ImportOrdersRequest request,
                                List<ImportOrdersResponse.ImportErrorDetail> errors,
                                int[] matchedCount,
                                int[] totalRowsProcessed,
                                Set<Long> affectedUserIds) {

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
        int processedOrders = 0;
        for (Map.Entry<String, List<ShopeeCSVParser.ShopeeOrderRecord>> entry : orderItemsMap.entrySet()) {
            String orderId = entry.getKey();
            List<ShopeeCSVParser.ShopeeOrderRecord> items = entry.getValue();

            try {
                processOrderWithItems(orderId, items, platform, batchId, request, errors, matchedCount, batch, affectedUserIds);
                processedOrders++;

                // Flush and clear every 50 orders to prevent memory buildup
                if (processedOrders % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.debug("Flushed and cleared EntityManager after {} orders", processedOrders);
                }

            } catch (Exception e) {
                batch.incrementFailed();
                log.error("Failed to process order {}: {}", orderId, e.getMessage(), e);
                errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                    .rowNumber(items.get(0).getRowNumber())
                    .orderId(orderId)
                    .error(e.getMessage())
                    .rawData(items.get(0).getRawData())
                    .build());
            }
        }

        // Save batch after processing all records in this batch
        batch = batchRepository.save(batch);

        // Log BEFORE clear to show correct counts
        log.info("Completed batch processing: {} unique orders processed ({} new, {} updated, {} failed, {} skipped)",
            processedOrders, batch.getSuccessCount(), batch.getUpdatedCount(),
            batch.getFailedCount(), batch.getSkippedCount());

        // Final flush and clear for this batch
        // IMPORTANT: Clear AFTER logging to avoid losing batch state
        entityManager.flush();
        entityManager.clear();
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
     * @param batch Import batch
     */
    private void processOrderWithItems(String orderId,
                                       List<ShopeeCSVParser.ShopeeOrderRecord> items,
                                       AffiliatePlatform platform,
                                       Long batchId,
                                       ImportOrdersRequest request,
                                       List<ImportOrdersResponse.ImportErrorDetail> errors,
                                       int[] matchedCount,
                                       ImportBatch batch,
                                       Set<Long> affectedUserIds) {

        log.debug("Processing order {} with {} items", orderId, items.size());

        // Use first item for order-level data (tracking code, order time, etc.)
        ShopeeCSVParser.ShopeeOrderRecord firstItem = items.get(0);

        // Aggregate data from all items
        // IMPORTANT: Items can have DIFFERENT statuses within the same order!
        // We need to track commission separately for completed vs pending vs cancelled items
        BigDecimal totalCommission = BigDecimal.ZERO;           // Total commission from ALL items
        BigDecimal completedCommission = BigDecimal.ZERO;       // Commission from COMPLETED items only
        BigDecimal pendingCommission = BigDecimal.ZERO;         // Commission from PENDING items only
        BigDecimal cancelledCommission = BigDecimal.ZERO;       // Commission from CANCELLED items only
        BigDecimal totalPrice = BigDecimal.ZERO;
        int completedItemCount = 0;
        int pendingItemCount = 0;
        int cancelledItemCount = 0;
        StringBuilder productNames = new StringBuilder();

        for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
            BigDecimal itemCommission = item.getCommissionForCashback();

            // Sum commission by status
            if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
                totalCommission = totalCommission.add(itemCommission);

                if (item.isCancelled()) {
                    cancelledCommission = cancelledCommission.add(itemCommission);
                    cancelledItemCount++;
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
        // CANCELLED if ALL items cancelled
        // APPROVED if ALL non-cancelled items completed
        // PENDING if ANY item is pending
        boolean allItemsCancelled = (cancelledItemCount > 0 && completedItemCount == 0 && pendingItemCount == 0);
        boolean allItemsCompleted = (pendingItemCount == 0 && completedItemCount > 0);
        boolean isOrderCompleted = allItemsCompleted;
        boolean isOrderCancelled = allItemsCancelled;

        log.debug("Order {} aggregated: total={}, completed={} ({}items), pending={} ({}items), cancelled={} ({}items)",
            orderId, totalCommission, completedCommission, completedItemCount,
            pendingCommission, pendingItemCount, cancelledCommission, cancelledItemCount);

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

        if (userId == null) {
            batch.incrementSkipped();
            log.warn("No tracking code found for order {}", orderId);
            errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                .rowNumber(firstItem.getRowNumber())
                .orderId(orderId)
                .error("No tracking code (Sub_id1) found")
                .rawData(firstItem.getRawData())
                .build());
            return;
        }

        // Create AffiliateOrder with aggregated data
        AffiliateOrder order = AffiliateOrder.builder()
            .platformId(platform.getId())
            .userId(userId)
            .orderId(orderId)
            .clickId(firstItem.getTrackingCode())
            .productName(truncateString(productNames.toString(), 255))
            .productPrice(totalPrice)
            .commissionAmount(totalCommission)  // TOTAL commission from all items
            .currency("VND")
            .orderTime(firstItem.getOrderTime())
            .orderStatus(isOrderCompleted ? OrderStatus.APPROVED : OrderStatus.PENDING)
            .source("IMPORT")
            .importBatchId(batchId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        order.validate();
        order = orderRepository.save(order);
        log.debug("Created order {} (id={}) for user {} with total commission {}",
            orderId, order.getId(), userId, totalCommission);

        // Create AffiliateOrderItem records for each item, each with its own cashback
        final Long savedOrderId = order.getId();
        final Long finalUserId = userId;
        for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
            // Determine item status
            OrderStatus itemStatus = item.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING;

            AffiliateOrderItem orderItem = AffiliateOrderItem.builder()
                .orderId(savedOrderId)
                .itemId(item.getItemId())
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
        log.debug("Created {} order items with cashbacks for order {}", items.size(), orderId);

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
            for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
                // Determine item status: CANCELLED > APPROVED > PENDING
                OrderStatus itemStatus;
                if (item.isCancelled()) {
                    itemStatus = OrderStatus.CANCELLED;
                } else if (item.isCompleted()) {
                    itemStatus = OrderStatus.APPROVED;
                } else {
                    itemStatus = OrderStatus.PENDING;
                }

                // Find existing item or create new
                AffiliateOrderItem existingItem = orderItemRepository
                    .findByOrderIdAndItemId(existingOrder.getId(), item.getItemId())
                    .orElse(null);

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
                    if (item.isCancelled()) {
                        // CRITICAL FIX: Even if item doesn't exist by itemId, we still need to
                        // try to cancel any existing cashback for this order.
                        // This handles the case where Shopee changes itemId when order is cancelled.
                        log.info("Cancelled item {} not found by itemId for order {}, will try to cancel by orderId",
                            item.getItemId(), existingOrder.getOrderId());

                        // Try to cancel cashback using orderId (will search by orderId in upsertForItemWithCancellation)
                        calculateCashbackUseCase.upsertForItemWithCancellation(
                            existingOrder.getUserId(),
                            existingOrder.getId(),
                            null,  // No orderItemId since item doesn't exist
                            platformId,
                            BigDecimal.ZERO,  // Amount will be taken from existing cashback
                            false,
                            true  // isCancelled = true
                        );
                        continue;
                    }
                    orderItem = AffiliateOrderItem.builder()
                        .orderId(existingOrder.getId())
                        .itemId(item.getItemId())
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
                    log.debug("Created new item {} for order {}", item.getItemId(), existingOrder.getOrderId());
                }

                // Upsert cashback for this item (including cancellation handling)
                BigDecimal itemCommission = item.getCommissionForCashback();
                // For cancelled items, we still need to process to cancel cashback
                // For non-cancelled items, skip if no commission
                if (item.isCancelled() || (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0)) {
                    calculateCashbackUseCase.upsertForItemWithCancellation(
                        existingOrder.getUserId(),
                        existingOrder.getId(),
                        orderItem.getId(),
                        platformId,
                        itemCommission != null ? itemCommission : BigDecimal.ZERO,
                        item.isCompleted(),
                        item.isCancelled()
                    );
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
     */
    private String buildMessage(ImportBatch batch, int matchedCount) {
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
                message.append(String.format("%d orders matched with clicks.", matchedCount));
            }
        } else if (batch.getStatus() == ImportStatus.PARTIAL) {
            message.append(String.format("Partially processed: %d new, %d updated, %d failed, %d skipped. ",
                batch.getSuccessCount(), batch.getUpdatedCount(),
                batch.getFailedCount(), batch.getSkippedCount()));
            if (matchedCount > 0) {
                message.append(String.format("%d orders matched with clicks.", matchedCount));
            }
        } else {
            message.append(String.format("Import failed. %d orders failed to import.",
                batch.getFailedCount()));
        }

        return message.toString().trim();
    }
}
