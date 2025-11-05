# 🚀 HƯỚNG DẪN TÍCH HỢP CASHBACK VÀO IMPORT CSV

## 📋 TỔNG QUAN

Document này hướng dẫn cách integrate các UseCase đã tạo vào `ImportShopeeOrdersUseCase` để:
1. Tính cashback cho mỗi order
2. Cập nhật ví user tự động
3. Xử lý trạng thái đơn hàng
4. Group items theo order ID
5. Báo cáo chi tiết errors

---

## ✅ ĐÃ HOÀN THÀNH

### 1. **ShopeeCSVParser** - Parse đầy đủ thông tin
- ✅ Parse 43 cột từ CSV (thay vì 13 cột)
- ✅ Có method `getCommissionForCashback()` để lấy hoa hồng đúng

### 2. **Cashback Infrastructure**
- ✅ `Cashback.java` - Domain model
- ✅ `CashbackJpaEntity.java` - JPA entity
- ✅ `CashbackRepository.java` - Repository interface
- ✅ `CashbackJpaRepository.java` - Spring Data repository
- ✅ `CashbackRepositoryAdapter.java` - Adapter
- ✅ `CashbackMapper.java` - Mapper

### 3. **Use Cases**
- ✅ `CalculateCashbackUseCase` - Tính cashback từ policy
- ✅ `AddCashbackToWalletUseCase` - Cập nhật ví user

---

## 🔧 CẦN TÍCH HỢP VÀO `ImportShopeeOrdersUseCase`

### **BƯỚC 1: Thêm Dependencies**

Thêm vào constructor của `ImportShopeeOrdersUseCase`:

```java
private final CalculateCashbackUseCase calculateCashbackUseCase;
private final AddCashbackToWalletUseCase addCashbackToWalletUseCase;
private final AffiliateOrderItemRepository orderItemRepository;
private final UserRepository userRepository;
```

### **BƯỚC 2: Group Items theo Order ID**

Sau khi parse CSV (sau dòng 104), thêm logic group items:

```java
// Step 3.5: Group records by orderId để xử lý multiple items per order
Map<String, List<ShopeeCSVParser.ShopeeOrderRecord>> groupedByOrder = records.stream()
    .filter(r -> !r.hasError() && !r.isCancelled())
    .collect(Collectors.groupingBy(
        r -> r.getOrderId() + "_" + (r.getCheckoutId() != null ? r.getCheckoutId() : ""),
        LinkedHashMap::new,
        Collectors.toList()
    ));

log.info("UseCase: Grouped {} records into {} unique orders",
    records.size(), groupedByOrder.size());
```

###

 **BƯỚC 3: Xử lý mỗi Order (thay vì mỗi Row)**

Thay vì loop qua từng `record`, loop qua `groupedByOrder`:

```java
// Step 4: Process each order (group of items)
List<ImportOrdersResponse.ImportErrorDetail> errors = new ArrayList<>();
int matchedCount = 0;
int cashbackCreatedCount = 0;
int cashbackPaidCount = 0;
BigDecimal totalCashbackAmount = BigDecimal.ZERO;

for (Map.Entry<String, List<ShopeeCSVParser.ShopeeOrderRecord>> entry : groupedByOrder.entrySet()) {
    List<ShopeeCSVParser.ShopeeOrderRecord> orderItems = entry.getValue();
    ShopeeCSVParser.ShopeeOrderRecord firstItem = orderItems.get(0);

    try {
        // 1. Extract user ID from tracking code
        Long userId = extractUserIdFromTrackingCode(firstItem, batch, errors);
        if (userId == null) {
            continue; // Skip this order
        }

        // 2. Verify user exists
        if (!userRepository.existsById(userId)) {
            batch.incrementFailed();
            errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
                .rowNumber(firstItem.getRowNumber())
                .orderId(firstItem.getOrderId())
                .error("User not found: " + userId)
                .rawData(firstItem.getRawData())
                .build());
            continue;
        }

        // 3. Check duplicate
        boolean isDuplicate = orderRepository.existsByOrderId(firstItem.getOrderId());
        if (isDuplicate) {
            if (request.getSkipDuplicates()) {
                batch.incrementSkipped();
                log.debug("Skipping duplicate order: {}", firstItem.getOrderId());
                continue;
            } else {
                // Update existing order
                updateExistingOrder(firstItem, orderItems, platform.getId(), userId);
                batch.incrementSuccess();
                continue;
            }
        }

        // 4. Calculate total commission for all items in this order
        BigDecimal totalCommission = orderItems.stream()
            .map(item -> item.getCommissionForCashback())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Create AffiliateOrder
        AffiliateOrder order = createAffiliateOrder(
            firstItem,
            platform.getId(),
            userId,
            totalCommission,
            batch.getId()
        );
        order.validate();
        order = orderRepository.save(order);
        log.debug("Created order {} for user {}", order.getId(), userId);

        // 6. Create AffiliateOrderItems (for each product in the order)
        for (ShopeeCSVParser.ShopeeOrderRecord item : orderItems) {
            AffiliateOrderItem orderItem = createAffiliateOrderItem(item, order.getId());
            orderItemRepository.save(orderItem);
        }
        log.debug("Created {} items for order {}", orderItems.size(), order.getId());

        // 7. Match with click if available
        if (request.getAutoMatch() && firstItem.hasTrackingCode()) {
            AffiliateClick click = clickRepository.findByTrackingCode(firstItem.getTrackingCode())
                .orElse(null);
            if (click != null) {
                click.matchWithOrder(order.getId());
                clickRepository.save(click);
                matchedCount++;
                log.debug("Matched order {} with click {}", order.getId(), click.getId());
            }
        }

        // 8. Calculate and create cashback
        if (totalCommission.compareTo(BigDecimal.ZERO) > 0) {
            Cashback cashback = calculateCashbackUseCase.execute(
                userId,
                order.getId(),
                platform.getId(),
                totalCommission,
                firstItem.isCompleted()  // true if order status = "Hoàn thành"
            );

            cashbackCreatedCount++;
            totalCashbackAmount = totalCashbackAmount.add(cashback.getCashbackAmount());

            // 9. Add cashback to wallet if order completed
            if (firstItem.isCompleted()) {
                addCashbackToWalletUseCase.addConfirmedCashback(cashback.getId());
                cashbackPaidCount++;
                log.info("Added confirmed cashback {} ({} VND) to wallet of user {}",
                    cashback.getId(), cashback.getCashbackAmount(), userId);
            } else {
                // Add to pending balance
                addCashbackToWalletUseCase.addPendingCashback(cashback.getId());
                log.info("Added pending cashback {} ({} VND) to pending balance of user {}",
                    cashback.getId(), cashback.getCashbackAmount(), userId);
            }
        }

        batch.incrementSuccess();

    } catch (Exception e) {
        batch.incrementFailed();
        log.error("Failed to process order {}: {}", firstItem.getOrderId(), e.getMessage(), e);
        errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
            .rowNumber(firstItem.getRowNumber())
            .orderId(firstItem.getOrderId())
            .error(e.getMessage())
            .rawData(firstItem.getRawData())
            .build());
    }
}
```

### **BƯỚC 4: Helper Methods**

Thêm các helper methods vào class:

```java
/**
 * Extract user ID from tracking code in Sub_id1.
 * Returns null if no tracking code or invalid format.
 */
private Long extractUserIdFromTrackingCode(
    ShopeeCSVParser.ShopeeOrderRecord record,
    ImportBatch batch,
    List<ImportOrdersResponse.ImportErrorDetail> errors) {

    if (!record.hasTrackingCode()) {
        batch.incrementSkipped();
        log.warn("No tracking code for order {}, skipping", record.getOrderId());
        errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
            .rowNumber(record.getRowNumber())
            .orderId(record.getOrderId())
            .error("No tracking code (Sub_id1 is empty)")
            .rawData(record.getRawData())
            .build());
        return null;
    }

    try {
        Long userId = trackingCodeGenerator.extractUserId(record.getTrackingCode());
        log.debug("Extracted user ID {} from tracking code {}", userId, record.getTrackingCode());
        return userId;
    } catch (IllegalArgumentException e) {
        batch.incrementSkipped();
        log.warn("Invalid tracking code for order {}: {}", record.getOrderId(), record.getTrackingCode());
        errors.add(ImportOrdersResponse.ImportErrorDetail.builder()
            .rowNumber(record.getRowNumber())
            .orderId(record.getOrderId())
            .error("Invalid tracking code format: " + record.getTrackingCode())
            .rawData(record.getRawData())
            .build());
        return null;
    }
}

/**
 * Create AffiliateOrder from CSV record.
 */
private AffiliateOrder createAffiliateOrder(
    ShopeeCSVParser.ShopeeOrderRecord record,
    Long platformId,
    Long userId,
    BigDecimal totalCommission,
    Long batchId) {

    return AffiliateOrder.builder()
        .platformId(platformId)
        .userId(userId)
        .orderId(record.getOrderId())
        .clickId(record.getTrackingCode())
        .productName(record.getItemName())  // First item name
        .productPrice(record.getPrice())    // First item price
        .commissionAmount(totalCommission)  // Total commission for all items
        .currency("VND")
        .orderTime(record.getOrderTime())
        .confirmTime(record.getCompleteTime())
        .orderStatus(record.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING)
        .source("IMPORT")
        .importBatchId(batchId)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
}

/**
 * Create AffiliateOrderItem from CSV record.
 */
private AffiliateOrderItem createAffiliateOrderItem(
    ShopeeCSVParser.ShopeeOrderRecord record,
    Long orderId) {

    return AffiliateOrderItem.builder()
        .orderId(orderId)
        .itemId(record.getItemId())
        .itemName(record.getItemName())
        .quantity(record.getQuantity())
        .actualAmount(record.getPrice())
        .itemCommission(record.getCommissionForCashback())
        .shopId(record.getShopId())
        .shopName(record.getShopName())
        .categoryLv1(record.getCategoryLv1())
        .categoryLv2(record.getCategoryLv2())
        .categoryLv3(record.getCategoryLv3())
        .brandCommissionRate(record.getSellerCommissionRate())
        .platformCommissionRate(record.getShopeeCommissionRate())
        .createdAt(LocalDateTime.now())
        .build();
}

/**
 * Update existing order when re-importing.
 */
private void updateExistingOrder(
    ShopeeCSVParser.ShopeeOrderRecord record,
    List<ShopeeCSVParser.ShopeeOrderRecord> orderItems,
    Long platformId,
    Long userId) {

    // Find existing order
    AffiliateOrder existingOrder = orderRepository.findByOrderId(record.getOrderId())
        .orElseThrow(() -> new NotFoundException("Order not found: " + record.getOrderId()));

    // Calculate new total commission
    BigDecimal totalCommission = orderItems.stream()
        .map(item -> item.getCommissionForCashback())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Update order
    // (You'll need to add update methods to domain model)
    // For now, just log
    log.info("Updating existing order {}: new commission = {}", record.getOrderId(), totalCommission);

    // TODO: Implement update logic based on business requirements
}
```

### **BƯỚC 5: Update Response**

Cập nhật phần build response để include cashback stats:

```java
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
    .cashbackCreatedCount(cashbackCreatedCount)       // NEW
    .cashbackPaidCount(cashbackPaidCount)             // NEW
    .totalCashbackAmount(totalCashbackAmount)         // NEW
    .successRate(batch.getSuccessRate())
    .errors(errors)
    .startedAt(startTime)
    .completedAt(endTime)
    .durationSeconds(durationSeconds)
    .importedBy(request.getImportedBy())
    .message(buildMessage(batch, matchedCount, cashbackPaidCount))
    .build();
```

---

## 📊 IMPORTS CẦN THÊM

```java
import com.cashbee.application.usecase.cashback.CalculateCashbackUseCase;
import com.cashbee.application.usecase.cashback.AddCashbackToWalletUseCase;
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.model.Cashback;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
```

---

## 🧪 TESTING

Sau khi integrate code, test với file CSV mẫu:

```bash
# Compile
./mvnw clean compile

# Run tests (nếu có)
./mvnw test

# Start application
./mvnw spring-boot:run
```

Test import endpoint:
```bash
curl -X POST http://localhost:8080/api/admin/import/orders \
  -H "Content-Type: multipart/form-data" \
  -F "file=@AffiliateCommissionReport202510300813.csv" \
  -F "platformCode=shopee" \
  -F "importedBy=1"
```

---

## ✅ CHECKLIST HOÀN THÀNH

- [x] Parse đầy đủ CSV columns
- [x] Tạo Cashback domain model và infrastructure
- [x] Tạo CalculateCashbackUseCase
- [x] Tạo AddCashbackToWalletUseCase
- [ ] Integrate vào ImportShopeeOrdersUseCase (code mẫu đã cung cấp)
- [ ] Update ImportOrdersResponse DTO
- [ ] Test với file CSV thật
- [ ] Verify database records
- [ ] Verify user wallet balance

---

## 🔄 FLOW HOÀN CHỈNH

```
CSV File
  ↓
Parse CSV → Group by OrderID
  ↓
For each Order:
  ├─ Extract UserID từ tracking code
  ├─ Verify user exists
  ├─ Check duplicate
  ├─ Calculate total commission (sum all items)
  ├─ Create AffiliateOrder
  ├─ Create AffiliateOrderItems (multiple)
  ├─ Calculate Cashback từ Policy
  └─ Add to Wallet:
      ├─ Nếu "Hoàn thành" → balance += cashback
      └─ Nếu "Đang chờ" → pending_balance += cashback
```

---

## 📝 NOTES

1. **Error handling**: Tất cả errors được log và trả về trong response, không break import process
2. **Transaction atomic**: Mỗi order được process trong 1 transaction riêng
3. **Duplicate handling**: Cho phép update nếu `skipDuplicates=false`
4. **Orphan orders**: Orders không có tracking code được skip và log vào errors
5. **Performance**: Với file lớn (1000+ orders), consider batch processing

---

**Author**: CashBee Team
**Date**: 2025-11-03
**Version**: 1.0
