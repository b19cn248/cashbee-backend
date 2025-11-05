# 📊 TỔNG KẾT: CHỨC NĂNG IMPORT CSV SHOPEE & TÍNH CASHBACK

## 🎯 MỤC TIÊU ĐÃ HOÀN THÀNH

Bổ sung chức năng import file CSV báo cáo chuyển đổi của Shopee Affiliate để **tự động tính tiền cashback** cho user dựa trên trạng thái đơn hàng.

---

## ✅ NHỮNG GÌ ĐÃ LÀM

### 1. **Cải tiến ShopeeCSVParser** ✅

**File**: `cashbee-application/src/main/java/com/cashbee/application/util/affiliate/ShopeeCSVParser.java`

**Thay đổi:**
- Parse từ **13 cột → 43 cột** để lấy đầy đủ thông tin
- Thêm các field quan trọng:
  - `checkoutId` - Group items trong cùng đơn hàng
  - `completeTime` - Thời gian hoàn thành
  - `shopName`, `shopType`, `categories` - Thông tin shop & sản phẩm
  - `totalOrderCommission` - **Hoa hồng đơn hàng** (quan trọng nhất!)
  - `netAffiliateCommission` - Hoa hồng sau phí
  - Và 30+ field khác...

**Method mới:**
```java
public BigDecimal getCommissionForCashback() {
    // Ưu tiên totalOrderCommission > totalProductCommission
}
```

**Lợi ích:**
- Tính cashback chính xác dựa trên hoa hồng thực tế
- Hỗ trợ đơn hàng có nhiều sản phẩm
- Tracking đầy đủ thông tin để audit

---

### 2. **Tạo Cashback Infrastructure** ✅

#### **Domain Layer** (Business logic)
- `Cashback.java` - Domain model thuần túy, không phụ thuộc framework
- `CashbackRepository.java` - Repository interface

#### **Infrastructure Layer** (Persistence)
- `CashbackJpaEntity.java` - JPA entity
- `CashbackJpaRepository.java` - Spring Data repository
- `CashbackRepositoryAdapter.java` - Adapter pattern
- `CashbackMapper.java` - Domain ↔ Entity mapper

**Cấu trúc bảng `cashback`:**
```sql
CREATE TABLE cashback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    commission_amount DECIMAL(12,2) NOT NULL,    -- Hoa hồng từ Shopee
    cashback_amount DECIMAL(12,2) NOT NULL,      -- Tiền trả lại cho user
    cashback_rate DECIMAL(5,2) NOT NULL,         -- Tỷ lệ (70%)
    policy_id BIGINT,                            -- Policy áp dụng
    status VARCHAR(20) NOT NULL,                 -- PENDING/CONFIRMED/PAID/CANCELLED/EXPIRED
    created_at, confirmed_at, paid_at, cancelled_at, updated_at,
    ...
)
```

**Status Flow:**
```
PENDING → CONFIRMED → PAID
   ↓
CANCELLED
```

---

### 3. **Tạo CalculateCashbackUseCase** ✅

**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/cashback/CalculateCashbackUseCase.java`

**Chức năng:**
1. Lấy `CashbackPolicy` đang active từ database
2. Tính cashback: `cashbackAmount = commission × (rate / 100)`
3. Áp dụng giới hạn min/max từ policy
4. Tạo `Cashback` với status phù hợp:
   - Đơn **"Hoàn thành"** → `CONFIRMED`
   - Đơn **"Đang chờ xử lý"** → `PENDING`

**Logic tính toán:**
```java
// Example: Commission = 10,000 VND, Rate = 70%
BigDecimal cashback = 10000 * 70 / 100 = 7,000 VND

// Apply limits from policy
if (cashback > maxCashbackPerOrder) {
    cashback = maxCashbackPerOrder;
}
```

**Fallback:** Nếu không có policy, dùng rate mặc định 70%

---

### 4. **Tạo AddCashbackToWalletUseCase** ✅

**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/cashback/AddCashbackToWalletUseCase.java`

**Methods:**

1. **addConfirmedCashback()** - Đơn hàng "Hoàn thành"
   ```
   balance += cashbackAmount
   status → PAID
   ```

2. **addPendingCashback()** - Đơn hàng "Đang chờ xử lý"
   ```
   pending_balance += cashbackAmount
   status → PENDING
   ```

3. **confirmCashbackForOrder()** - Khi đơn chuyển sang "Hoàn thành"
   ```
   pending_balance -= cashbackAmount
   balance += cashbackAmount
   status: PENDING → CONFIRMED → PAID
   ```

4. **cancelCashbackForOrder()** - Khi đơn bị hủy
   ```
   status → CANCELLED
   (Không cộng tiền)
   ```

**Transaction Safety:**
- Tất cả operations đều atomic (`@Transactional`)
- Tạo `WalletTransaction` để audit trail
- Reuse existing UseCases (`AddPendingBalanceUseCase`, `ConfirmPendingBalanceUseCase`)

---

### 5. **Hướng dẫn Integration** ✅

**File**: `docs/IMPORT_SHOPEE_CSV_INTEGRATION_GUIDE.md`

**Nội dung:**
- Step-by-step guide để integrate vào `ImportShopeeOrdersUseCase`
- Code mẫu đầy đủ (copy-paste ready)
- Helper methods
- Error handling
- Testing guide

**Flow hoàn chỉnh:**
```
CSV → Parse → Group by OrderID
  ↓
For each Order:
  ├─ Extract UserID from tracking code
  ├─ Verify user exists
  ├─ Check duplicate
  ├─ Calculate total commission
  ├─ Create AffiliateOrder
  ├─ Create AffiliateOrderItems (multiple)
  ├─ Calculate Cashback from Policy
  └─ Add to Wallet:
      ├─ "Hoàn thành" → balance += cashback
      └─ "Đang chờ" → pending_balance += cashback
```

---

### 6. **SQL Migration Script** ✅

**File**: `scripts/migration/001_create_cashback_table.sql`

**Chức năng:**
- Tạo bảng `cashback` với indexes tối ưu
- Foreign keys đến `user`, `affiliate_order`, `affiliate_platform`, `cashback_policy`
- Constraints để validate data
- Comments đầy đủ cho documentation

**Cách chạy:**
```bash
mysql -u root -p cashbee_db < scripts/migration/001_create_cashback_table.sql
```

---

## 🔄 FLOW HOẠT ĐỘNG

### **Scenario 1: Đơn hàng "Hoàn thành"**

```
1. Admin upload CSV
   ↓
2. Parse CSV → Phát hiện order "251030ABC" với status "Hoàn thành"
   ↓
3. Extract tracking code "CB123_456_..." → userId = 123
   ↓
4. Create AffiliateOrder (orderId=1, userId=123, commission=10000)
   ↓
5. Calculate Cashback:
   - Get policy: rate = 70%
   - cashbackAmount = 10000 * 0.7 = 7000 VND
   - status = CONFIRMED (vì đơn đã hoàn thành)
   ↓
6. Add to Wallet:
   - user_wallet.balance += 7000
   - wallet_transaction: type=CASHBACK, amount=7000
   - cashback.status → PAID
   ↓
7. Response:
   {
     "successCount": 1,
     "cashbackPaidCount": 1,
     "totalCashbackAmount": 7000
   }
```

### **Scenario 2: Đơn hàng "Đang chờ xử lý"**

```
1. Admin upload CSV
   ↓
2. Parse CSV → Phát hiện order "251031XYZ" với status "Đang chờ xử lý"
   ↓
3. Create AffiliateOrder (orderId=2, status=PENDING)
   ↓
4. Calculate Cashback:
   - cashbackAmount = 5000 VND
   - status = PENDING (vì đơn chưa hoàn thành)
   ↓
5. Add to Pending Balance:
   - user_wallet.pending_balance += 5000
   - cashback.status = PENDING
   ↓
6. Sau khi customer nhận hàng, admin re-import CSV:
   - Order status giờ là "Hoàn thành"
   - System update cashback: PENDING → CONFIRMED → PAID
   - pending_balance -= 5000
   - balance += 5000
```

### **Scenario 3: Đơn hàng không có tracking code**

```
1. Parse CSV → Order "251032NO_TRACK" không có Sub_id1
   ↓
2. System skip order này
   ↓
3. Add vào errors list:
   {
     "orderId": "251032NO_TRACK",
     "error": "No tracking code (Sub_id1 is empty)",
     "rawData": "..."
   }
   ↓
4. Response.skippedCount++
```

---

## 📋 CHECKLIST TRIỂN KHAI

### **ĐÃ HOÀN THÀNH** ✅

- [x] Parse đầy đủ 43 cột từ CSV
- [x] Tạo Cashback domain model
- [x] Tạo Cashback infrastructure (Entity, Repository, Mapper)
- [x] Tạo CalculateCashbackUseCase
- [x] Tạo AddCashbackToWalletUseCase
- [x] Tạo Liquibase changeset (016-create-cashback-table.xml)
- [x] Fix tất cả compile errors
- [x] Build SUCCESS toàn bộ project
- [x] Viết integration guide đầy đủ
- [x] Document flow và scenarios

### **CẦN LÀM TIẾP** 📝

- [ ] **Chạy Liquibase migration** để tạo bảng `cashback`
  ```bash
  ./mvnw liquibase:update
  # Hoặc start application để Liquibase tự động chạy
  ./mvnw spring-boot:run
  ```

- [ ] **Integrate code vào ImportShopeeOrdersUseCase**
  - Copy code từ `IMPORT_SHOPEE_CSV_INTEGRATION_GUIDE.md`
  - Thêm dependencies vào constructor
  - Thay thế loop logic để group items theo orderId
  - Thêm helper methods để calculate cashback

- [ ] **Update ImportOrdersResponse DTO**
  - Thêm fields: `cashbackCreatedCount`, `cashbackPaidCount`, `totalCashbackAmount`

- [ ] **Test với file CSV thật**
  - Upload `AffiliateCommissionReport202510300813.csv`
  - Verify database:
    - `affiliate_order` có records mới
    - `affiliate_order_item` có items
    - `cashback` có records
    - `user_wallet` balance đã tăng
    - `wallet_transaction` có transactions

- [ ] **Kiểm tra edge cases**
  - Đơn hàng duplicate
  - Tracking code invalid
  - User không tồn tại
  - Commission = 0
  - Multiple items per order

---

## 🎓 GIẢI THÍCH CHO NGƯỜI MỚI

### **Tại sao cần nhiều layer (Domain, Application, Infrastructure)?**

**Clean Architecture** giúp:
1. **Testability**: Test business logic không cần database
2. **Maintainability**: Thay đổi database không ảnh hưởng domain logic
3. **Flexibility**: Có thể switch từ MySQL → PostgreSQL dễ dàng

**Ví dụ:**
```
Domain Layer (Cashback.java):
  - Chứa business rules
  - Không biết gì về JPA, MySQL, Spring

Infrastructure Layer (CashbackJpaEntity.java):
  - Biết cách lưu vào MySQL
  - Không chứa business logic

Adapter (CashbackRepositoryAdapter):
  - Kết nối 2 layer
  - Convert Domain ↔ Entity
```

### **Tại sao có pending_balance và balance riêng biệt?**

**Lý do:**
- **pending_balance**: Tiền chưa chắc chắn (đơn hàng chưa hoàn thành, có thể bị hủy)
- **balance**: Tiền đã confirmed, user có thể rút

**Ví dụ thực tế:**
```
User A mua hàng 1,000,000 VND → Cashback 700,000 VND
- Ngày 1: Đơn "Đang giao" → pending_balance = 700,000
- Ngày 5: Đơn "Hoàn thành" → balance = 700,000, pending_balance = 0
- User rút tiền: balance -= 700,000
```

Nếu đơn hàng bị hủy:
```
- Ngày 1: pending_balance = 700,000
- Ngày 3: Đơn "Đã hủy" → pending_balance = 0 (không cộng vào balance)
```

### **Tại sao cần tracking code?**

**Tracking code** là cách duy nhất để biết đơn hàng thuộc về user nào:

1. User share link có tracking code: `CB123_456_20251103...`
2. Khách mua hàng qua link đó
3. Shopee lưu tracking code vào `Sub_id1`
4. Khi import CSV, system parse `Sub_id1` → userId = 123
5. Cộng tiền vào ví của User 123

**Không có tracking code** → Không biết cộng tiền cho ai → Skip đơn hàng!

---

## 🚀 NEXT STEPS

1. **Follow integration guide**: `docs/IMPORT_SHOPEE_CSV_INTEGRATION_GUIDE.md`
2. **Run migration**: `scripts/migration/001_create_cashback_table.sql`
3. **Test locally**
4. **Deploy to staging**
5. **Monitor logs**
6. **Deploy to production**

---

## 📞 HỖ TRỢ

Nếu gặp vấn đề:
1. Đọc lại `IMPORT_SHOPEE_CSV_INTEGRATION_GUIDE.md`
2. Check logs: `tail -f logs/application.log`
3. Verify database: `SELECT * FROM cashback LIMIT 10;`
4. Check user wallet: `SELECT * FROM user_wallet WHERE user_id = 123;`

---

## 🔧 TECHNICAL NOTES

### **Database Migration với Liquibase**
Project sử dụng **Liquibase** để quản lý database migrations thay vì plain SQL.

**File migrations:**
- `cashbee-presentation/src/main/resources/db/changelog/016-create-cashback-table.xml`
- `cashbee-presentation/src/main/resources/db/changelog/db.changelog-master.xml` (đã update)

**Cách chạy:**
```bash
# Option 1: Liquibase command
./mvnw liquibase:update

# Option 2: Start application (Liquibase auto-run)
./mvnw spring-boot:run

# Option 3: Docker compose
docker-compose up -d
```

### **Clean Architecture Pattern**
- **Domain Layer**: Pure business logic (Cashback.java, CashbackRepository interface)
- **Application Layer**: Use cases (CalculateCashbackUseCase, AddCashbackToWalletUseCase)
- **Infrastructure Layer**: JPA entities, repositories, adapters
- **Presentation Layer**: Controllers, DTOs

### **Fixed Issues**
1. ✅ Changed `getTotalCommission()` → `getCommissionForCashback()`
2. ✅ Fixed wallet UseCase calls to use Command objects
3. ✅ Fixed `findActivePolicyByPlatformId()` → `findActivePolicyFor()`
4. ✅ Added `existsById()` to UserRepository
5. ✅ Build SUCCESS - All compile errors resolved

---

**Author**: CashBee Team
**Date**: 2025-11-03
**Version**: 2.0 (Updated after implementing Liquibase)
**Status**: ✅ Build SUCCESS - Ready for Integration
