# Import Reporting Logic

## Overview

Tài liệu này giải thích logic reporting khi import orders từ CSV file, làm rõ các metrics và cách chúng được tính toán.

## Các Metrics Quan Trọng

### 1. **successCount** - New Orders Created
- **Ý nghĩa**: Số lượng orders MỚI được tạo
- **Khi nào tăng**: Khi tạo mới 1 order chưa tồn tại trong DB
- **Code**: `batch.incrementSuccess()` tại `ImportShopeeOrdersUseCase.java:553`

### 2. **updatedCount** - Existing Orders Updated
- **Ý nghĩa**: Số lượng orders CŨ được cập nhật
- **Khi nào tăng**: Khi order đã tồn tại và `updateMode = UPDATE`
- **Code**: `batch.incrementUpdated()` tại `ImportShopeeOrdersUseCase.java:668`

### 3. **matchedCount** - Orders Matched with Clicks
- **Ý nghĩa**: Số lượng orders (mới HOẶC cũ) được match với click record
- **Khi nào tăng**: Khi tìm thấy click record tương ứng với tracking code
- **Code**: `matchedCount[0]++` tại `ImportShopeeOrdersUseCase.java:548`

### 4. **failedCount** - Failed Orders
- **Ý nghĩa**: Số lượng orders không thể import được
- **Khi nào tăng**: Khi có lỗi trong quá trình xử lý
- **Code**: `batch.incrementFailed()`

### 5. **skippedCount** - Skipped Orders
- **Ý nghĩa**: Số lượng orders bị bỏ qua
- **Khi nào tăng**:
  - Order đã tồn tại và `updateMode = SKIP`
  - Order bị cancelled
  - Không có tracking code
  - User không tồn tại
- **Code**: `batch.incrementSkipped()`

## Quan Trọng: Metrics KHÔNG Loại Trừ Lẫn Nhau

⚠️ **1 Order có thể ĐỒNG THỜI là "new" VÀ "matched"**

### Ví dụ từ logs:

```
Order: 2511196R4HVFCJ
- ✅ NEW order (successCount = 1) - Vì order chưa tồn tại trong DB
- ✅ MATCHED (matchedCount = 1) - Vì tìm thấy click với tracking code CB2_2_20251119060655
```

### Flow chi tiết:

```java
// Step 1: Tạo order mới
AffiliateOrder order = orderRepository.save(newOrder);
// → successCount++ (order mới được tạo)

// Step 2: Tìm click
AffiliateClick click = clickRepository.findByTrackingCode(trackingCode);

// Step 3: Match order với click
if (click != null) {
    click.matchWithOrder(order.getId());
    clickRepository.save(click);
    // → matchedCount++ (order được match với click)
}

batch.incrementSuccess(); // Kết quả: successCount=1, matchedCount=1
```

## Công Thức Tính

### Total Processed Orders
```
totalProcessed = successCount + updatedCount
```

### Total Rows in CSV
```
totalRows = successCount + updatedCount + failedCount + skippedCount
```

### Success Rate
```
successRate = (successCount / totalRows) * 100
```

## Log Messages

### Before (Confusing ❌)
```
UseCase: Import completed. Success: 0, Updated: 0, Failed: 0, Skipped: 0, Matched: 1
```
👎 Gây nhầm lẫn vì "Success: 0" nhưng thực ra đã tạo order thành công

### After (Clear ✅)
```
UseCase: Import completed. New orders: 1, Updated: 0, Failed: 0, Skipped: 0, Orders matched with clicks: 1
```
👍 Rõ ràng: 1 order mới được tạo VÀ match với click

## Response Message Examples

### Case 1: All New Orders
```json
{
  "totalRows": 10,
  "successCount": 10,      // 10 orders mới
  "updatedCount": 0,
  "matchedCount": 8,       // 8 trong 10 orders match với clicks
  "message": "Successfully processed 10 rows. 10 orders processed (10 new). 8 orders matched with clicks."
}
```

### Case 2: Mix of New and Updated
```json
{
  "totalRows": 10,
  "successCount": 6,       // 6 orders mới
  "updatedCount": 4,       // 4 orders cũ được update
  "matchedCount": 9,       // 9 orders match với clicks (có thể là new hoặc updated)
  "message": "Successfully processed 10 rows. 10 orders processed (6 new, 4 updated). 9 orders matched with clicks."
}
```

### Case 3: Update Only
```json
{
  "totalRows": 5,
  "successCount": 0,       // Không có order mới
  "updatedCount": 5,       // 5 orders cũ được update
  "matchedCount": 5,       // Tất cả đều match
  "message": "Successfully processed 5 rows. 5 orders processed (5 updated). 5 orders matched with clicks."
}
```

## Database Schema

### Import Batch Table
```sql
CREATE TABLE import_batch (
    id BIGINT PRIMARY KEY,
    total_rows INT,        -- Tổng số rows trong CSV
    success_count INT,     -- Số orders MỚI được tạo
    updated_count INT,     -- Số orders CŨ được update
    failed_count INT,      -- Số orders failed
    skipped_count INT,     -- Số orders bị skip
    -- matched_count KHÔNG lưu trong DB, chỉ đếm runtime
    ...
);
```

**Lưu ý**: `matchedCount` không được lưu trong DB vì:
1. Nó có thể tính lại từ `affiliate_click.order_matched = true`
2. Giảm redundancy
3. Tránh inconsistency

## Testing Scenarios

### Scenario 1: Import File Lần Đầu
```
Input: 1 order (chưa tồn tại), có tracking code hợp lệ
Expected:
- successCount = 1 (order mới)
- matchedCount = 1 (match với click)
- Total processed = 1
```

### Scenario 2: Re-import Same File (UPDATE mode)
```
Input: 1 order (đã tồn tại), updateMode=UPDATE
Expected:
- successCount = 0 (không tạo mới)
- updatedCount = 1 (update order cũ)
- matchedCount = 1 (vẫn match)
- Total processed = 1
```

### Scenario 3: Re-import Same File (SKIP mode)
```
Input: 1 order (đã tồn tại), updateMode=SKIP
Expected:
- successCount = 0
- updatedCount = 0
- skippedCount = 1 (bỏ qua)
- matchedCount = 0 (không xử lý)
- Total processed = 0
```

## Related Files

- `ImportShopeeOrdersUseCase.java:169-171` - Log import completed
- `ImportShopeeOrdersUseCase.java:321-323` - Log batch processing
- `ImportShopeeOrdersUseCase.java:541-553` - Match và increment logic
- `ImportShopeeOrdersUseCase.java:714-762` - Build user message
- `AffiliateImportController.java:121-128` - Controller log
- `ImportBatch.java:129-152` - Increment methods

## Changelog

### 2025-11-22: Improved Reporting Clarity
- ✅ Updated log messages to use "New orders" instead of "Success"
- ✅ Updated log messages to use "Orders matched with clicks" instead of "Matched"
- ✅ Added comments explaining that orders can be BOTH new AND matched
- ✅ Improved `buildMessage()` to show breakdown of new vs updated
- ✅ Updated Controller logs for consistency

### Previous Issues
- ❌ Log showed "Success: 0, Matched: 1" - confusing because order WAS created successfully
- ❌ Không rõ sự khác biệt giữa "success" và "matched"
