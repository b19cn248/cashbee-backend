# Phân Tích Logic Hiển Thị Hoa Hồng Chính Xác Từ Shopee

## Tổng Quan

Tài liệu này phân tích chi tiết cách hệ thống CashBee hiển thị hoa hồng từ Shopee một cách chính xác, từ lúc import CSV cho đến khi hiển thị cho người dùng.

---

## 1. Nguồn Dữ Liệu: File CSV Từ Shopee

### 1.1 Format CSV Shopee

File CSV từ Shopee chứa thông tin chi tiết về hoa hồng với các cột quan trọng:

```
ShopeeCSVParser.java:68-79
```

**Các cột hoa hồng chính:**

| Cột CSV | Tên Tiếng Việt | Ý Nghĩa | Sử Dụng |
|---------|----------------|---------|---------|
| `Hoa hồng Shopee trên sản phẩm(₫)` | shopeeProductCommission | Hoa hồng từ Shopee cho sản phẩm | Thành phần của tổng |
| `Hoa hồng Xtra trên sản phẩm(₫)` | xtraProductCommission | Hoa hồng Xtra cho sản phẩm | Thành phần của tổng |
| `Tổng hoa hồng sản phẩm(₫)` | totalProductCommission | **Tổng hoa hồng sản phẩm** | ✅ Ưu tiên 2 |
| `Hoa hồng đơn hàng từ Shopee(₫)` | shopeeOrderCommission | Hoa hồng order từ Shopee | Thành phần của tổng |
| `Hoa hồng đơn hàng từ Người bán(₫)` | sellerOrderCommission | Hoa hồng order từ seller | Thành phần của tổng |
| `Tổng hoa hồng đơn hàng(₫)` | totalOrderCommission | **Tổng hoa hồng order** | ✅ Ưu tiên 1 |
| `Hoa hồng ròng tiếp thị liên kết(₫)` | netAffiliateCommission | Hoa hồng ròng sau phí | Tham khảo |

### 1.2 Logic Chọn Hoa Hồng Để Tính Cashback

**File:** `ShopeeCSVParser.java:647-659`

```java
/**
 * Get the commission amount to use for cashback calculation.
 * Priority: totalOrderCommission > totalProductCommission > 0
 */
public BigDecimal getCommissionForCashback() {
    // Ưu tiên 1: Tổng hoa hồng đơn hàng
    if (totalOrderCommission != null && totalOrderCommission.compareTo(BigDecimal.ZERO) > 0) {
        return totalOrderCommission;
    }
    // Ưu tiên 2: Tổng hoa hồng sản phẩm
    if (totalProductCommission != null && totalProductCommission.compareTo(BigDecimal.ZERO) > 0) {
        return totalProductCommission;
    }
    // Default: 0 nếu không có hoa hồng
    return BigDecimal.ZERO;
}
```

**Độ ưu tiên:**
1. **totalOrderCommission** - Hoa hồng tổng của đơn hàng (nếu có)
2. **totalProductCommission** - Hoa hồng tổng của sản phẩm
3. **BigDecimal.ZERO** - Không có hoa hồng

**Lý do thiết kế:**
- `totalOrderCommission` thường bao gồm cả hoa hồng sản phẩm + hoa hồng đơn hàng
- Nếu không có order commission, dùng product commission
- Đảm bảo không bao giờ null

---

## 2. Parsing CSV: Từ File → Domain Model

### 2.1 Parse Line By Line

**File:** `ShopeeCSVParser.java:273-313`

```java
private ShopeeOrderRecord parseLine(CSVRecord csvRecord, int rowNumber) {
    return ShopeeOrderRecord.builder()
        .rowNumber(rowNumber)
        .rawData(csvRecord.toString())
        .orderId(getColumnByName(csvRecord, COL_ORDER_ID))
        .orderStatus(getColumnByName(csvRecord, COL_ORDER_STATUS))
        // ... các trường khác ...
        .totalProductCommission(parseBigDecimal(getColumnByName(csvRecord, COL_TOTAL_PRODUCT_COMMISSION)))
        .totalOrderCommission(parseBigDecimal(getColumnByName(csvRecord, COL_TOTAL_ORDER_COMMISSION)))
        .netAffiliateCommission(parseBigDecimal(getColumnByName(csvRecord, COL_NET_AFFILIATE_COMMISSION)))
        .subId1(getColumnByName(csvRecord, COL_SUB_ID1))  // Tracking code
        .build();
}
```

### 2.2 Parse BigDecimal An Toàn

**File:** `ShopeeCSVParser.java:369-391`

```java
private BigDecimal parseBigDecimal(String value) {
    if (value == null || value.isEmpty()) {
        return null;
    }

    try {
        // Loại bỏ ký tự tiền tệ, dấu phẩy, dấu %
        String cleaned = value.replace("₫", "")
            .replace(",", "")
            .replace("%", "")
            .replace(" ", "")
            .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return new BigDecimal(cleaned);
    } catch (NumberFormatException e) {
        log.debug("Failed to parse BigDecimal: {}", value);
        return null;
    }
}
```

**Ví dụ:**
- Input: `"1,234,567₫"` → Output: `BigDecimal(1234567)`
- Input: `"10.5%"` → Output: `BigDecimal(10.5)`
- Input: `""` → Output: `null`

---

## 3. Import Process: Từ CSV → Database

### 3.1 Grouping Items By Order

**Quan trọng:** Mỗi dòng CSV là 1 **ITEM**, không phải 1 order!

**File:** `ImportShopeeOrdersUseCase.java:251-285`

```java
// Step 1: Group records by orderId
Map<String, List<ShopeeOrderRecord>> orderItemsMap = new HashMap<>();

for (ShopeeCSVParser.ShopeeOrderRecord record : recordBatch) {
    // Skip cancelled items
    if (record.isCancelled()) {
        batch.incrementSkipped();
        continue;
    }

    // Group by orderId
    String orderId = record.getOrderId();
    if (orderId != null && !orderId.isBlank()) {
        orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(record);
    }
}
```

**Kết quả:**
```
CSV Rows:
Row 1: Order ABC, Item 1, Commission 10,000đ
Row 2: Order ABC, Item 2, Commission 15,000đ
Row 3: Order XYZ, Item 1, Commission 20,000đ

→ Grouped Map:
Order ABC: [Item 1, Item 2]  // Total: 25,000đ
Order XYZ: [Item 1]           // Total: 20,000đ
```

### 3.2 Tính Tổng Hoa Hồng Cho Order

**File:** `ImportShopeeOrdersUseCase.java:362-410`

```java
// Aggregate data from all items
BigDecimal totalCommission = BigDecimal.ZERO;           // Tổng hoa hồng TẤT CẢ items
BigDecimal completedCommission = BigDecimal.ZERO;       // Hoa hồng items HOÀN THÀNH
BigDecimal pendingCommission = BigDecimal.ZERO;         // Hoa hồng items CHỜ XỬ LÝ
int completedItemCount = 0;
int pendingItemCount = 0;

for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
    BigDecimal itemCommission = item.getCommissionForCashback();  // Lấy commission từ item

    // Tính tổng theo status
    if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
        totalCommission = totalCommission.add(itemCommission);

        if (item.isCompleted()) {
            completedCommission = completedCommission.add(itemCommission);
            completedItemCount++;
        } else {
            pendingCommission = pendingCommission.add(itemCommission);
            pendingItemCount++;
        }
    }
}

// Determine order status
boolean allItemsCompleted = (pendingItemCount == 0 && completedItemCount > 0);
boolean isOrderCompleted = allItemsCompleted;
```

**Ví dụ thực tế:**

```
Order ABC có 3 items:
- Item 1: Hoàn thành, Commission: 10,000đ
- Item 2: Hoàn thành, Commission: 15,000đ
- Item 3: Chờ xử lý, Commission: 5,000đ

→ Kết quả:
totalCommission = 30,000đ
completedCommission = 25,000đ
pendingCommission = 5,000đ
isOrderCompleted = false (vì có 1 item chưa hoàn thành)
```

### 3.3 Tạo AffiliateOrder

**File:** `ImportShopeeOrdersUseCase.java:481-503`

```java
// Create AffiliateOrder with aggregated data
AffiliateOrder order = AffiliateOrder.builder()
    .platformId(platform.getId())
    .userId(userId)
    .orderId(orderId)
    .clickId(firstItem.getTrackingCode())
    .productName(truncateString(productNames.toString(), 255))
    .productPrice(totalPrice)
    .commissionAmount(totalCommission)  // ⭐ TOTAL commission từ ALL items
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
```

**Điểm quan trọng:**
- `commissionAmount` = **TỔNG** hoa hồng của TẤT CẢ items trong order
- `orderStatus` = `APPROVED` nếu TẤT CẢ items hoàn thành, ngược lại = `PENDING`

---

## 4. Tạo Cashback: Từ Commission → Cashback Amount

### 4.1 Tạo Cashback Cho Từng Item

**File:** `ImportShopeeOrdersUseCase.java:507-542`

```java
// Create AffiliateOrderItem records for each item, each with its own cashback
for (ShopeeCSVParser.ShopeeOrderRecord item : items) {
    // Determine item status
    OrderStatus itemStatus = item.isCompleted() ? OrderStatus.APPROVED : OrderStatus.PENDING;

    // Create order item
    AffiliateOrderItem orderItem = AffiliateOrderItem.builder()
        .orderId(savedOrderId)
        .itemId(item.getItemId())
        .itemName(item.getItemName())
        .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
        .actualAmount(item.getPrice())
        .itemCommission(item.getCommissionForCashback())  // ⭐ Hoa hồng của item này
        .status(itemStatus)
        .build();
    AffiliateOrderItem savedItem = orderItemRepository.save(orderItem);

    // Create cashback for this item
    BigDecimal itemCommission = item.getCommissionForCashback();
    if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
        Cashback cashback = calculateCashbackUseCase.executeForItem(
            finalUserId,
            savedOrderId,
            savedItem.getId(),
            platform.getId(),
            itemCommission,        // ⭐ Commission từ Shopee
            item.isCompleted()     // ⭐ Status của item
        );
    }
}
```

**Điểm quan trọng:**
- Mỗi **item** có 1 **cashback** riêng
- Cashback được tính dựa trên `itemCommission` của từng item
- Status của cashback phụ thuộc vào status của item

### 4.2 Tính Cashback Amount

**File:** `CalculateCashbackUseCase.java:176-228`

```java
public Cashback executeForItem(Long userId, Long orderId, Long orderItemId, Long platformId,
                               BigDecimal commissionAmount, boolean isItemCompleted) {

    // Get cashback rate from policy
    BigDecimal cashbackRate = getDefaultCashbackRate(platformId);  // Default: 70%

    // Calculate cashback amount
    BigDecimal cashbackAmount = commissionAmount
        .multiply(cashbackRate)
        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

    // Determine status
    CashbackStatus status = isItemCompleted ? CashbackStatus.CONFIRMED : CashbackStatus.PENDING;

    // Create cashback record
    Cashback cashback = Cashback.builder()
        .userId(userId)
        .orderId(orderId)
        .orderItemId(orderItemId)
        .platformId(platformId)
        .commissionAmount(commissionAmount)  // ⭐ Hoa hồng gốc từ Shopee
        .cashbackAmount(cashbackAmount)      // ⭐ Số tiền hoàn lại cho user (70%)
        .cashbackRate(cashbackRate)          // ⭐ Tỷ lệ (70%)
        .status(status)
        .build();

    cashback.validate();
    Cashback savedCashback = cashbackRepository.save(cashback);

    // Update wallet
    updateWalletForNewCashback(userId, cashbackAmount, status);

    return savedCashback;
}
```

**Công thức tính:**

```
commissionAmount = Hoa hồng từ Shopee (từ CSV)
cashbackRate = 70% (từ CashbackPolicy)
cashbackAmount = commissionAmount × (cashbackRate / 100)

Ví dụ:
commissionAmount = 100,000đ
cashbackRate = 70%
→ cashbackAmount = 100,000 × 0.7 = 70,000đ
```

---

## 5. Lưu Vào Wallet: Balance vs Pending Balance

### 5.1 Logic Cập Nhật Wallet

**File:** `CalculateCashbackUseCase.java:322-349`

```java
private void updateWalletForNewCashback(Long userId, BigDecimal amount, CashbackStatus status) {
    UserWallet wallet = getOrCreateWallet(userId);

    if (status == CashbackStatus.PENDING) {
        // New PENDING cashback → add to pending_balance
        wallet.addPendingBalance(amount);
        log.info("Added {} to pending_balance for user {}", amount, userId);
    }
    else if (status == CashbackStatus.CONFIRMED) {
        // New CONFIRMED cashback → add to balance and total_earned
        wallet.addConfirmedCashbackDirectly(amount);
        log.info("Added {} to balance for user {}", amount, userId);
    }

    wallet.scaleBalances();
    walletRepository.save(wallet);
}
```

**Flow:**

```
Item Status: PENDING (Chờ xử lý)
→ Cashback Status: PENDING
→ Wallet: pending_balance += cashbackAmount

Item Status: APPROVED (Hoàn thành)
→ Cashback Status: CONFIRMED
→ Wallet: balance += cashbackAmount
         total_earned += cashbackAmount
```

### 5.2 Khi Item Status Thay Đổi

**File:** `CalculateCashbackUseCase.java:363-400`

```java
private void updateWalletForStatusChange(Long userId, BigDecimal amount,
                                          CashbackStatus oldStatus, CashbackStatus newStatus) {
    UserWallet wallet = getOrCreateWallet(userId);

    if (oldStatus == CashbackStatus.PENDING && newStatus == CashbackStatus.CONFIRMED) {
        // PENDING → CONFIRMED: move from pending_balance to balance
        wallet.confirmPendingBalance(amount);  // pending_balance -= amount
                                               // balance += amount
                                               // total_earned += amount
    }
    else if (oldStatus == CashbackStatus.CONFIRMED && newStatus == CashbackStatus.PENDING) {
        // CONFIRMED → PENDING: move from balance back to pending_balance
        wallet.reverseConfirmedCashback(amount);  // balance -= amount
                                                  // total_earned -= amount
        wallet.addPendingBalance(amount);         // pending_balance += amount
    }

    wallet.scaleBalances();
    walletRepository.save(wallet);
}
```

**Ví dụ:**

```
Ban đầu:
- Item status: PENDING
- Cashback: 70,000đ (PENDING)
- Wallet: pending_balance = 70,000đ, balance = 0đ

Sau khi item hoàn thành:
- Item status: APPROVED
- Cashback: 70,000đ (CONFIRMED)
- Wallet: pending_balance = 0đ, balance = 70,000đ, total_earned = 70,000đ
```

---

## 6. Hiển Thị Cho User: API Response

### 6.1 Wallet API

Khi user gọi API lấy thông tin ví:

```json
GET /api/user/wallet

Response:
{
  "userId": 123,
  "balance": 350000,           // ⭐ Số tiền khả dụng (từ items HOÀN THÀNH)
  "pendingBalance": 120000,    // ⭐ Số tiền chờ xử lý (từ items PENDING)
  "totalEarned": 470000,       // ⭐ Tổng đã kiếm được
  "totalWithdrawn": 0
}
```

**Giải thích:**
- `balance`: Tiền từ items đã HOÀN THÀNH, có thể rút
- `pendingBalance`: Tiền từ items ĐANG CHỜ XỬ LÝ, chưa rút được
- `totalEarned`: Tổng tiền đã kiếm = balance + total_withdrawn

### 6.2 Cashback History API

```json
GET /api/user/cashback/history

Response:
{
  "cashbacks": [
    {
      "id": 1,
      "orderId": "ABC123",
      "orderItemId": 101,
      "itemName": "Áo thun nam",
      "commissionAmount": 100000,    // ⭐ Hoa hồng từ Shopee (gốc)
      "cashbackRate": 70.0,          // ⭐ Tỷ lệ hoàn
      "cashbackAmount": 70000,       // ⭐ Số tiền user nhận = 70% × 100,000
      "status": "CONFIRMED",
      "createdAt": "2025-11-24T10:00:00",
      "confirmedAt": "2025-11-24T12:00:00"
    }
  ]
}
```

---

## 7. Độ Chính Xác Của Hệ Thống

### 7.1 Đảm Bảo Chính Xác

1. **Parse CSV chính xác:**
   - Sử dụng Apache Commons CSV (robust)
   - Parse BigDecimal an toàn (loại bỏ ký tự đặc biệt)
   - Validate headers

2. **Tính tổng commission chính xác:**
   - Dùng BigDecimal (không bị lỗi floating point)
   - Aggregate từng item riêng biệt
   - Phân biệt completed vs pending

3. **Tính cashback chính xác:**
   - Dùng RoundingMode.HALF_UP (làm tròn chuẩn)
   - Scale 2 chữ số thập phân
   - Apply policy limits

4. **Cập nhật wallet chính xác:**
   - Transaction isolation
   - Domain method validation
   - Scale balances trước khi save

### 7.2 Ví Dụ End-to-End

**CSV Input:**
```csv
ID đơn hàng,Tên Item,Tổng hoa hồng đơn hàng(₫),Trạng thái đặt hàng,Sub_id1
ABC123,Áo thun,100000,Hoàn thành,CB2_123_20251124
ABC123,Quần jean,50000,Hoàn thành,CB2_123_20251124
```

**Processing:**
```
1. Parse CSV:
   - Item 1: commission = 100,000đ, status = APPROVED
   - Item 2: commission = 50,000đ, status = APPROVED

2. Group by Order:
   - Order ABC123: 2 items, totalCommission = 150,000đ

3. Create Order:
   - AffiliateOrder.commissionAmount = 150,000đ
   - AffiliateOrder.orderStatus = APPROVED

4. Create Cashback for each item:
   - Item 1 Cashback:
     * commissionAmount = 100,000đ
     * cashbackRate = 70%
     * cashbackAmount = 70,000đ
     * status = CONFIRMED

   - Item 2 Cashback:
     * commissionAmount = 50,000đ
     * cashbackRate = 70%
     * cashbackAmount = 35,000đ
     * status = CONFIRMED

5. Update Wallet:
   - balance += 70,000 + 35,000 = 105,000đ
   - total_earned += 105,000đ
```

**User Sees:**
```
Ví của tôi: 105,000đ
Lịch sử cashback:
- Áo thun: +70,000đ (từ 100,000đ hoa hồng)
- Quần jean: +35,000đ (từ 50,000đ hoa hồng)
```

---

## 8. Edge Cases

### 8.1 Order Có Items Mixed Status

```
Order ABC123:
- Item 1: Hoàn thành, 100,000đ → Cashback: 70,000đ CONFIRMED
- Item 2: Chờ xử lý, 50,000đ → Cashback: 35,000đ PENDING

Wallet:
- balance = 70,000đ (từ item 1)
- pending_balance = 35,000đ (từ item 2)
```

### 8.2 Item Cancelled Sau Khi Import

Khi item bị hủy, logic update:
```java
// CalculateCashbackUseCase.upsertForItem()
// Nếu item chuyển từ APPROVED → CANCELLED
// → Wallet sẽ giảm balance tương ứng
```

### 8.3 Commission = 0

```java
// ShopeeCSVParser.getCommissionForCashback()
if (totalOrderCommission == null || totalOrderCommission == 0) {
    return BigDecimal.ZERO;
}

// ImportShopeeOrdersUseCase.processOrderWithItems()
if (itemCommission != null && itemCommission.compareTo(BigDecimal.ZERO) > 0) {
    // Only create cashback if commission > 0
}
```

---

## 9. Kết Luận

### 9.1 Điểm Mạnh

✅ **Chính xác:**
- Parse CSV robust với Apache Commons CSV
- BigDecimal cho tính toán tài chính
- RoundingMode.HALF_UP chuẩn

✅ **Minh bạch:**
- Lưu cả `commissionAmount` (gốc) và `cashbackAmount` (sau policy)
- Track status của từng item riêng biệt
- Separate `balance` và `pending_balance`

✅ **Có thể trace:**
- Mỗi cashback link đến order_item_id
- Logs chi tiết ở mỗi bước
- Import batch tracking

### 9.2 Flow Hoàn Chỉnh

```
CSV File (Shopee)
    ↓
Parse CSV (ShopeeCSVParser)
    ↓
Extract Commission (totalOrderCommission / totalProductCommission)
    ↓
Group Items by Order (ImportShopeeOrdersUseCase)
    ↓
Calculate Total Commission per Order
    ↓
Create AffiliateOrder (commissionAmount = total)
    ↓
Create AffiliateOrderItem for each item
    ↓
Create Cashback for each item (CalculateCashbackUseCase)
    ↓
Apply Policy Rate (70%)
    ↓
Calculate cashbackAmount = commission × 0.7
    ↓
Update Wallet (balance or pending_balance)
    ↓
Display to User
```

### 9.3 Truy Vết Hoa Hồng

Để kiểm tra hoa hồng có chính xác không:

1. **Check CSV gốc:**
   ```
   Tổng hoa hồng đơn hàng(₫) = X
   ```

2. **Check AffiliateOrderItem:**
   ```sql
   SELECT item_commission FROM affiliate_order_item WHERE item_id = 'Y';
   → Should be X
   ```

3. **Check Cashback:**
   ```sql
   SELECT commission_amount, cashback_rate, cashback_amount
   FROM cashback WHERE order_item_id = Z;
   → commission_amount = X
   → cashback_amount = X × 0.7
   ```

4. **Check Wallet:**
   ```sql
   SELECT balance, pending_balance FROM user_wallet WHERE user_id = 123;
   → Sum should match total cashbacks
   ```

---

## Related Files

- `ShopeeCSVParser.java` - Parse CSV và extract commission
- `ImportShopeeOrdersUseCase.java` - Import flow và aggregate commission
- `CalculateCashbackUseCase.java` - Tính cashback từ commission
- `Cashback.java` - Domain model cho cashback
- `UserWallet.java` - Domain model cho ví user

## Changelog

**2025-11-24:** Created comprehensive analysis document
