# 📋 PLAN: Fallback Matching cho Orders Thiếu Sub_ID

**Date**: 2025-11-29
**Status**: 📝 **PLANNING** - Chờ Review

---

## 🎯 MỤC TIÊU

Khi import orders từ CSV Shopee mà **cột Sub_id1 trống**, hệ thống sẽ **tự động thử match** order với click của user dựa trên các tiêu chí context (thời gian, item, shop) thay vì bỏ qua order đó.

### **Problem Statement**:
- User click link CashBee → Cookie được set với sub_id
- User click link affiliate khác (KOL, Facebook...) → Cookie bị **ghi đè** (Last Click Wins)
- User mua hàng → Order thuộc về link cuối cùng → **sub_id của CashBee bị mất**
- Kết quả: Order có trong báo cáo Shopee nhưng **không có sub_id** → Không match được với user

### **Solution**:
Thêm cơ chế **Fallback Matching** khi sub_id trống:
1. Tìm click records của tất cả users trong 7 ngày gần đây
2. Match dựa trên `item_id` + `shop_id` + `order_time` window
3. Nếu tìm được **duy nhất 1 match** → Auto-assign cho user đó
4. Nếu tìm được **nhiều matches** → Đánh dấu "cần review" để admin xử lý

---

## 🔍 HIỆN TRẠNG

### **Flow hiện tại**:

```
CSV Row
  ↓
Extract Sub_id1 (tracking code)
  ↓
┌─────────────────────────────────┐
│ if (firstItem.hasTrackingCode())│
│   ↓                             │
│   extractUserId(trackingCode)   │
│   ↓                             │
│   findByTrackingCode()          │
│   ↓                             │
│   Create Order + Cashback       │
└─────────────────────────────────┘
  ↓
┌─────────────────────────────────┐
│ else (NO tracking code)         │
│   ↓                             │
│   ❌ batch.incrementSkipped()   │
│   ↓                             │
│   ❌ Order bị bỏ qua!           │
└─────────────────────────────────┘
```

**File**: `ImportShopeeOrdersUseCase.java` (line 506-551)

```java
// Current implementation
if (firstItem.hasTrackingCode()) {
    try {
        userId = trackingCodeGenerator.extractUserId(firstItem.getTrackingCode());
        // ... success path
    } catch (IllegalArgumentException e) {
        batch.incrementSkipped();
        // ... error handling
    }
}

if (userId == null) {
    batch.incrementSkipped();
    log.warn("No tracking code found for order {}", orderId);
    errors.add(...);
    return;  // ❌ ORDER SKIPPED!
}
```

### **Vấn đề**:

| Scenario | Hiện tại | Mong muốn |
|----------|----------|-----------|
| Có tracking code hợp lệ | ✅ Match | ✅ Match |
| Tracking code invalid | ❌ Skip | ❌ Skip (đúng) |
| **Không có tracking code** | ❌ Skip | ✨ **Fallback Match** |

---

## ✅ GIẢI PHÁP

### **Flow mới** (với Fallback Matching):

```
CSV Row
  ↓
Extract Sub_id1 (tracking code)
  ↓
┌─────────────────────────────────────┐
│ PRIMARY PATH: hasTrackingCode()     │
│   ↓                                 │
│   extractUserId() → findClick()     │
│   ↓                                 │
│   Create Order + Cashback           │
└─────────────────────────────────────┘
  ↓ (if no tracking code)
┌─────────────────────────────────────────────────────┐
│ ✨ FALLBACK PATH: No tracking code                  │
│   ↓                                                 │
│   findPossibleClicksByContext(                      │
│       itemId, shopId, orderTime, 7 days window      │
│   )                                                 │
│   ↓                                                 │
│   ┌─────────────────────────────────────────────┐   │
│   │ if (matches.size() == 1)                    │   │
│   │   → Auto-match với user                     │   │
│   │   → Create Order + Cashback                 │   │
│   │   → Log: "Fallback matched"                 │   │
│   └─────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────┐   │
│   │ if (matches.size() > 1)                     │   │
│   │   → Create Order với status NEEDS_REVIEW    │   │
│   │   → Store possible_user_ids                 │   │
│   │   → Log: "Multiple matches, needs review"   │   │
│   └─────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────┐   │
│   │ if (matches.size() == 0)                    │   │
│   │   → Skip order (như hiện tại)               │   │
│   │   → Log: "No fallback match found"          │   │
│   └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## 📝 IMPLEMENTATION STEPS

### **STEP 1: Thêm Repository Method để tìm clicks theo context**

**File**: `cashbee-domain/src/main/java/com/cashbee/domain/repository/AffiliateClickRepository.java`

**Add method**:
```java
/**
 * Find clicks that match the given item/shop within a time window.
 * Used for fallback matching when Sub_id1 is missing.
 *
 * @param platformId Platform ID (Shopee)
 * @param itemId Product item ID from order
 * @param shopId Shop ID from order
 * @param orderTime When the order was placed
 * @param windowMinutes Time window to search (e.g., 7 days = 10080 minutes)
 * @return List of possible matching clicks
 */
List<AffiliateClick> findPossibleMatchesByContext(
    Long platformId,
    String itemId,
    String shopId,
    LocalDateTime orderTime,
    int windowMinutes
);
```

**Giải thích**:
- Tìm tất cả clicks có cùng `itemId` VÀ `shopId`
- Trong khoảng thời gian: `orderTime - windowMinutes` đến `orderTime`
- Chỉ tìm clicks chưa match (`orderMatched = false`)

---

### **STEP 2: Implement JPA Query**

**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/repository/AffiliateClickJpaRepository.java`

**Add query**:
```java
/**
 * Find clicks matching context for fallback matching.
 *
 * Conditions:
 * - Same platform
 * - Same item ID (exact match)
 * - Same shop ID (exact match)
 * - Click created within window before order time
 * - Not already matched with another order
 */
@Query("""
    SELECT c FROM AffiliateClickJpaEntity c
    WHERE c.platformId = :platformId
    AND c.itemId = :itemId
    AND c.shopId = :shopId
    AND c.createdAt >= :windowStart
    AND c.createdAt <= :orderTime
    AND c.orderMatched = false
    AND c.deletedAt IS NULL
    ORDER BY c.createdAt DESC
    """)
List<AffiliateClickJpaEntity> findPossibleMatchesByContext(
    @Param("platformId") Long platformId,
    @Param("itemId") String itemId,
    @Param("shopId") String shopId,
    @Param("orderTime") LocalDateTime orderTime,
    @Param("windowStart") LocalDateTime windowStart
);
```

**Alternative - Flexible matching** (nếu item/shop có thể khác):
```java
/**
 * Find clicks by user activity within time window.
 * More flexible - matches by shop only if item not found.
 */
@Query("""
    SELECT c FROM AffiliateClickJpaEntity c
    WHERE c.platformId = :platformId
    AND (c.itemId = :itemId OR c.shopId = :shopId)
    AND c.createdAt >= :windowStart
    AND c.createdAt <= :orderTime
    AND c.orderMatched = false
    AND c.deletedAt IS NULL
    ORDER BY
        CASE WHEN c.itemId = :itemId AND c.shopId = :shopId THEN 0
             WHEN c.itemId = :itemId THEN 1
             WHEN c.shopId = :shopId THEN 2
             ELSE 3
        END,
        c.createdAt DESC
    """)
List<AffiliateClickJpaEntity> findPossibleMatchesByContextFlexible(
    @Param("platformId") Long platformId,
    @Param("itemId") String itemId,
    @Param("shopId") String shopId,
    @Param("orderTime") LocalDateTime orderTime,
    @Param("windowStart") LocalDateTime windowStart
);
```

---

### **STEP 3: Implement Repository Adapter**

**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/adapter/AffiliateClickRepositoryAdapter.java`

**Add implementation**:
```java
@Override
public List<AffiliateClick> findPossibleMatchesByContext(
        Long platformId,
        String itemId,
        String shopId,
        LocalDateTime orderTime,
        int windowMinutes) {

    LocalDateTime windowStart = orderTime.minusMinutes(windowMinutes);

    List<AffiliateClickJpaEntity> entities = jpaRepository.findPossibleMatchesByContext(
        platformId,
        itemId,
        shopId,
        orderTime,
        windowStart
    );

    return entities.stream()
        .map(clickMapper::toDomain)
        .collect(Collectors.toList());
}
```

---

### **STEP 4: Thêm Fallback Logic vào ImportShopeeOrdersUseCase**

**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/affiliate/ImportShopeeOrdersUseCase.java`

**Location**: Sau line 539, thêm fallback logic

**Add constant**:
```java
/**
 * Time window for fallback matching (7 days in minutes).
 * Matches Shopee's cookie window for app.
 */
private static final int FALLBACK_WINDOW_MINUTES = 7 * 24 * 60; // 10080 minutes
```

**Modify processOrderWithItems method** (line 506-551):

```java
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
if (userId == null && request.getAutoMatch()) {
    log.info("No tracking code for order {}, attempting fallback matching...", orderId);

    FallbackMatchResult fallbackResult = attemptFallbackMatch(
        platform.getId(),
        firstItem.getItemId(),
        firstItem.getShopId(),
        firstItem.getOrderTime()
    );

    if (fallbackResult.isUniqueMatch()) {
        // Found exactly one matching click - auto-assign
        click = fallbackResult.getClick();
        userId = click.getUserId();
        isFallbackMatch = true;
        log.info("Fallback matched order {} with click {} (user {})",
            orderId, click.getId(), userId);
    } else if (fallbackResult.hasMultipleMatches()) {
        // Multiple possible matches - log for admin review
        log.warn("Order {} has {} possible matches, skipping (needs manual review): users {}",
            orderId, fallbackResult.getMatchCount(), fallbackResult.getPossibleUserIds());
        batch.incrementSkipped();
        errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
            .rowNumber(firstItem.getRowNumber())
            .orderId(orderId)
            .error(String.format("Multiple fallback matches found (%d users: %s). Manual review required.",
                fallbackResult.getMatchCount(), fallbackResult.getPossibleUserIds()))
            .rawData(firstItem.getRawData())
            .build());
        return;
    } else {
        // No matches found
        log.info("No fallback match found for order {} (item: {}, shop: {})",
            orderId, firstItem.getItemId(), firstItem.getShopId());
    }
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

// Continue with order creation...
// Add flag to indicate fallback match
```

---

### **STEP 5: Thêm FallbackMatchResult DTO**

**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/affiliate/FallbackMatchResult.java`

**Create new file**:
```java
package com.cashbee.application.dto.affiliate;

import com.cashbee.domain.model.AffiliateClick;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of fallback matching attempt when Sub_id1 is missing.
 */
@Getter
@Builder
public class FallbackMatchResult {

    /**
     * The matched click (if unique match found).
     */
    private final AffiliateClick click;

    /**
     * All possible matching clicks (if multiple found).
     */
    private final List<AffiliateClick> possibleMatches;

    /**
     * Check if exactly one match was found (safe to auto-assign).
     */
    public boolean isUniqueMatch() {
        return possibleMatches != null && possibleMatches.size() == 1;
    }

    /**
     * Check if multiple matches were found (needs manual review).
     */
    public boolean hasMultipleMatches() {
        return possibleMatches != null && possibleMatches.size() > 1;
    }

    /**
     * Check if no matches were found.
     */
    public boolean hasNoMatch() {
        return possibleMatches == null || possibleMatches.isEmpty();
    }

    /**
     * Get count of possible matches.
     */
    public int getMatchCount() {
        return possibleMatches != null ? possibleMatches.size() : 0;
    }

    /**
     * Get list of possible user IDs (for logging/review).
     */
    public List<Long> getPossibleUserIds() {
        if (possibleMatches == null) {
            return List.of();
        }
        return possibleMatches.stream()
            .map(AffiliateClick::getUserId)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Create result for unique match.
     */
    public static FallbackMatchResult uniqueMatch(AffiliateClick click) {
        return FallbackMatchResult.builder()
            .click(click)
            .possibleMatches(List.of(click))
            .build();
    }

    /**
     * Create result for multiple matches.
     */
    public static FallbackMatchResult multipleMatches(List<AffiliateClick> clicks) {
        return FallbackMatchResult.builder()
            .click(null)
            .possibleMatches(clicks)
            .build();
    }

    /**
     * Create result for no match.
     */
    public static FallbackMatchResult noMatch() {
        return FallbackMatchResult.builder()
            .click(null)
            .possibleMatches(List.of())
            .build();
    }
}
```

---

### **STEP 6: Thêm attemptFallbackMatch method**

**File**: `ImportShopeeOrdersUseCase.java`

**Add private method**:
```java
/**
 * Attempt to match an order without tracking code by context.
 *
 * Matching criteria:
 * - Same platform (Shopee)
 * - Same item ID (product that was clicked)
 * - Same shop ID (store that was clicked)
 * - Click created within 7 days before order time
 * - Click not already matched with another order
 *
 * @param platformId Platform ID
 * @param itemId Item ID from order
 * @param shopId Shop ID from order
 * @param orderTime When the order was placed
 * @return FallbackMatchResult indicating match status
 */
private FallbackMatchResult attemptFallbackMatch(
        Long platformId,
        String itemId,
        String shopId,
        LocalDateTime orderTime) {

    // Skip if missing required fields
    if (itemId == null || shopId == null || orderTime == null) {
        log.debug("Cannot attempt fallback match: missing itemId, shopId, or orderTime");
        return FallbackMatchResult.noMatch();
    }

    // Find clicks matching this context
    List<AffiliateClick> possibleMatches = clickRepository.findPossibleMatchesByContext(
        platformId,
        itemId,
        shopId,
        orderTime,
        FALLBACK_WINDOW_MINUTES
    );

    if (possibleMatches.isEmpty()) {
        return FallbackMatchResult.noMatch();
    }

    if (possibleMatches.size() == 1) {
        return FallbackMatchResult.uniqueMatch(possibleMatches.get(0));
    }

    // Multiple matches - check if all from same user
    long distinctUsers = possibleMatches.stream()
        .map(AffiliateClick::getUserId)
        .distinct()
        .count();

    if (distinctUsers == 1) {
        // All clicks from same user - safe to match with most recent click
        AffiliateClick mostRecentClick = possibleMatches.get(0); // Already sorted by createdAt DESC
        log.info("Multiple clicks from same user {}, using most recent click {}",
            mostRecentClick.getUserId(), mostRecentClick.getId());
        return FallbackMatchResult.uniqueMatch(mostRecentClick);
    }

    // Multiple users - needs review
    return FallbackMatchResult.multipleMatches(possibleMatches);
}
```

---

### **STEP 7: Cập nhật AffiliateOrder để track fallback match**

**File**: `cashbee-domain/src/main/java/com/cashbee/domain/model/AffiliateOrder.java`

**Add field**:
```java
/**
 * Whether this order was matched using fallback (context-based) matching.
 *
 * TRUE = Order matched by context (itemId + shopId + time window) because Sub_id1 was missing
 * FALSE/NULL = Order matched by tracking code (Sub_id1) - normal flow
 */
private Boolean isFallbackMatch;
```

**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/entity/AffiliateOrderJpaEntity.java`

**Add column**:
```java
@Column(name = "is_fallback_match")
private Boolean isFallbackMatch;
```

**File**: Database migration (nếu cần)

```sql
-- Add column to track fallback matches
ALTER TABLE affiliate_order ADD COLUMN is_fallback_match BOOLEAN DEFAULT FALSE;

-- Add index for analytics
CREATE INDEX idx_order_fallback_match ON affiliate_order(is_fallback_match);
```

---

### **STEP 8: Update Order Creation để set fallback flag**

**File**: `ImportShopeeOrdersUseCase.java`

**Modify order creation** (around line 554):

```java
// Create AffiliateOrder with aggregated data
AffiliateOrder order = AffiliateOrder.builder()
    .platformId(platform.getId())
    .userId(userId)
    .orderId(orderId)
    .clickId(firstItem.getTrackingCode())  // May be null for fallback
    .productName(truncateString(productNames.toString(), 255))
    .productPrice(totalPrice)
    .commissionAmount(totalCommission)
    .currency("VND")
    .orderTime(firstItem.getOrderTime())
    .orderStatus(isOrderCompleted ? OrderStatus.APPROVED : OrderStatus.PENDING)
    .source("IMPORT")
    .importBatchId(batchId)
    .isFallbackMatch(isFallbackMatch)  // ✨ ADD THIS
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .build();
```

---

### **STEP 9: Update ImportOrdersResponse để track fallback stats**

**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/affiliate/ImportOrdersResponse.java`

**Add fields**:
```java
/**
 * Number of orders matched using fallback (context-based) matching.
 * These orders had no Sub_id1 but were matched by item/shop/time.
 */
private Integer fallbackMatchedCount;

/**
 * Number of orders skipped due to multiple possible fallback matches.
 * These need manual review.
 */
private Integer multipleMatchSkippedCount;
```

---

## 🧪 TESTING PLAN

### **Test Case 1: Order có tracking code (flow bình thường)**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái,...,Item id,Shop id,Sub_id1
ORD001,Hoàn thành,...,12345,67890,CB1_1_20251129100000
```

**Expected**:
- ✅ Match theo tracking code (như hiện tại)
- ✅ `is_fallback_match = FALSE`
- ✅ Order được tạo cho user 1

---

### **Test Case 2: Order KHÔNG có tracking code, có 1 click match**

**Setup DB**:
```sql
-- User 1 đã click vào sản phẩm này 2 ngày trước
INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (1, 1, '12345', '67890', '2025-11-27 10:00:00', FALSE);
```

**Input CSV**:
```csv
ID đơn hàng,Trạng thái,...,Item id,Shop id,Thời gian đặt hàng,Sub_id1
ORD002,Hoàn thành,...,12345,67890,2025-11-29 10:00:00,
```

**Expected**:
- ✅ Fallback match với click của user 1
- ✅ `is_fallback_match = TRUE`
- ✅ Order được tạo cho user 1
- ✅ Log: "Fallback matched order ORD002 with click X (user 1)"

---

### **Test Case 3: Order KHÔNG có tracking code, có nhiều users match**

**Setup DB**:
```sql
-- User 1 click
INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (1, 1, '12345', '67890', '2025-11-27 10:00:00', FALSE);

-- User 2 cũng click cùng sản phẩm
INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (2, 1, '12345', '67890', '2025-11-28 15:00:00', FALSE);
```

**Input CSV**:
```csv
ID đơn hàng,Trạng thái,...,Item id,Shop id,Thời gian đặt hàng,Sub_id1
ORD003,Hoàn thành,...,12345,67890,2025-11-29 10:00:00,
```

**Expected**:
- ❌ Order KHÔNG được tạo (skip)
- ✅ Log warning: "Order ORD003 has 2 possible matches, skipping (needs manual review): users [1, 2]"
- ✅ Error detail trong response

---

### **Test Case 4: Order KHÔNG có tracking code, không có click match**

**Setup DB**: Không có click nào cho item này

**Input CSV**:
```csv
ID đơn hàng,Trạng thái,...,Item id,Shop id,Thời gian đặt hàng,Sub_id1
ORD004,Hoàn thành,...,99999,88888,2025-11-29 10:00:00,
```

**Expected**:
- ❌ Order KHÔNG được tạo (skip)
- ✅ Log: "No fallback match found for order ORD004"
- ✅ Error: "No tracking code (Sub_id1) and no fallback match found"

---

### **Test Case 5: Click quá hạn (>7 ngày)**

**Setup DB**:
```sql
-- User click 10 ngày trước (quá 7 ngày window)
INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (1, 1, '12345', '67890', '2025-11-19 10:00:00', FALSE);
```

**Input CSV**:
```csv
ID đơn hàng,Trạng thái,...,Item id,Shop id,Thời gian đặt hàng,Sub_id1
ORD005,Hoàn thành,...,12345,67890,2025-11-29 10:00:00,
```

**Expected**:
- ❌ Fallback match KHÔNG tìm thấy (click quá cũ)
- ✅ Order bị skip

---

### **Test Case 6: Nhiều clicks từ cùng 1 user**

**Setup DB**:
```sql
-- User 1 click nhiều lần
INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (1, 1, '12345', '67890', '2025-11-27 10:00:00', FALSE);

INSERT INTO affiliate_click (user_id, platform_id, item_id, shop_id, created_at, order_matched)
VALUES (1, 1, '12345', '67890', '2025-11-28 15:00:00', FALSE);
```

**Expected**:
- ✅ Fallback match với click gần nhất (2025-11-28)
- ✅ Order được tạo cho user 1
- ✅ `is_fallback_match = TRUE`

---

## 📊 DATABASE IMPACT

### **Tables Affected**:

**1. `affiliate_order`** - Thêm column mới
```sql
ALTER TABLE affiliate_order
ADD COLUMN is_fallback_match BOOLEAN DEFAULT FALSE;

CREATE INDEX idx_order_fallback_match ON affiliate_order(is_fallback_match);
```

**2. `affiliate_click`** - Thêm index mới cho query
```sql
-- Composite index cho fallback matching query
CREATE INDEX idx_click_fallback_match
ON affiliate_click(platform_id, item_id, shop_id, created_at, order_matched)
WHERE deleted_at IS NULL;
```

### **Query Performance**:

**Fallback matching query**:
```sql
-- Expected query từ JPA
SELECT * FROM affiliate_click
WHERE platform_id = ?
  AND item_id = ?
  AND shop_id = ?
  AND created_at >= ? AND created_at <= ?
  AND order_matched = FALSE
  AND deleted_at IS NULL
ORDER BY created_at DESC;
```

**Estimated performance**:
- With composite index: < 10ms per query
- Without index: 50-100ms (table scan)

---

## ⚠️ ERROR HANDLING

### **Scenario 1: itemId hoặc shopId null trong CSV**

**Problem**: Một số orders có thể thiếu item_id hoặc shop_id

**Solution**:
```java
if (itemId == null || shopId == null || orderTime == null) {
    log.debug("Cannot attempt fallback match: missing itemId, shopId, or orderTime");
    return FallbackMatchResult.noMatch();
}
```

---

### **Scenario 2: Database query timeout**

**Problem**: Query chậm khi có nhiều clicks

**Solution**:
- Add composite index (Step trong DB Impact)
- Add query timeout:
```java
@QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
List<AffiliateClickJpaEntity> findPossibleMatchesByContext(...);
```

---

### **Scenario 3: Race condition - Click được match trong khi đang process**

**Problem**: 2 orders cùng match với 1 click

**Solution**:
- Click có `orderMatched` flag
- Sau khi match, update flag ngay
- Query chỉ tìm `orderMatched = false`

---

## 🎯 SUCCESS CRITERIA

✅ **Functional**:
- [ ] Orders với tracking code vẫn match như cũ
- [ ] Orders không có tracking code được fallback match khi có 1 click duy nhất
- [ ] Orders với nhiều possible matches bị skip và log warning
- [ ] Flag `is_fallback_match` được set đúng

✅ **Performance**:
- [ ] Fallback query < 50ms
- [ ] Import time tăng < 10% so với không có fallback

✅ **Observability**:
- [ ] Log rõ ràng cho mỗi loại match
- [ ] Response có `fallbackMatchedCount` và `multipleMatchSkippedCount`
- [ ] Error details cho orders bị skip

✅ **Data Integrity**:
- [ ] Không tạo duplicate orders
- [ ] Click được mark `orderMatched = true` sau khi match
- [ ] Fallback match không ghi đè tracking code match

---

## 📝 CHECKLIST

### **Domain Layer**:
- [ ] Add `findPossibleMatchesByContext()` to `AffiliateClickRepository`
- [ ] Add `isFallbackMatch` field to `AffiliateOrder`

### **Infrastructure Layer**:
- [ ] Implement JPA query in `AffiliateClickJpaRepository`
- [ ] Implement adapter method in `AffiliateClickRepositoryAdapter`
- [ ] Add column to `AffiliateOrderJpaEntity`
- [ ] Add mapper for new field
- [ ] Create database migration

### **Application Layer**:
- [ ] Create `FallbackMatchResult` DTO
- [ ] Add `FALLBACK_WINDOW_MINUTES` constant
- [ ] Add `attemptFallbackMatch()` method
- [ ] Modify `processOrderWithItems()` to use fallback
- [ ] Update `ImportOrdersResponse` with fallback stats

### **Testing**:
- [ ] Unit test: Unique match scenario
- [ ] Unit test: Multiple matches scenario
- [ ] Unit test: No match scenario
- [ ] Unit test: Expired click scenario
- [ ] Unit test: Same user multiple clicks
- [ ] Integration test: Full import flow

### **Documentation**:
- [ ] Update import flow documentation
- [ ] Add inline comments explaining fallback logic

---

## 🚀 ROLLOUT PLAN

### **Phase 1: Database Migration** (5 min)
1. Add `is_fallback_match` column
2. Add composite index for query

### **Phase 2: Implementation** (1-2 hours)
1. Domain layer changes
2. Infrastructure layer changes
3. Application layer changes

### **Phase 3: Testing** (30-45 min)
1. Unit tests
2. Integration tests
3. Manual testing with sample CSV

### **Phase 4: Deployment** (10 min)
1. Deploy to staging
2. Run test import
3. Verify logs and data

**Total Estimated Time**: 2-3 hours

---

## 🎉 EXPECTED OUTCOME

### **Before** (Current):
```
CSV Import với Sub_id1 trống:
  → Order bị skip ❌
  → User mất commission 💸
  → Business mất doanh thu 📉
```

### **After** (With Fallback):
```
CSV Import với Sub_id1 trống:
  → Tìm click theo context 🔍
  → Nếu 1 match → Auto-assign ✅
  → Nếu nhiều match → Skip + Log warning ⚠️
  → Nếu 0 match → Skip như cũ ❌
```

### **Business Impact**:

| Metric | Before | After |
|--------|--------|-------|
| Orders matched | ~70% | ~85-90% |
| Lost commission | ~30% | ~10-15% |
| Manual review needed | 0 | ~5% |

### **Ví dụ thực tế**:

**Scenario**: User A click link CashBee cho sản phẩm X, sau đó click link KOL khác, rồi mua sản phẩm X

**Before**: Order không có Sub_id1 → Skip → User mất tiền

**After**:
1. System tìm click của sản phẩm X trong 7 ngày
2. Tìm thấy click của User A
3. Auto-match → User A nhận cashback 🎉

---

**Created by**: Claude Code Assistant
**Date**: 2025-11-29
**Status**: 📝 **WAITING FOR REVIEW**
