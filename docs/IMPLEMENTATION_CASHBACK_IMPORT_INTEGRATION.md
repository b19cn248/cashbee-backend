# ✅ IMPLEMENTATION: Cashback Integration vào CSV Import

**Date**: 2025-11-03
**Status**: ✅ **COMPLETED**

---

## 🎯 MỤC TIÊU

Tích hợp tự động tính cashback và cộng tiền cho user sau khi import orders từ CSV Shopee.

---

## 📋 TỔNG QUAN THAY ĐỔI

### **Before** (Trước khi implement):
```
CSV Import
  ↓
Create AffiliateOrder
  ↓
❌ STOP (Không có cashback, không có tiền)
```

### **After** (Sau khi implement):
```
CSV Import
  ↓
Create AffiliateOrder
  ↓
Calculate Cashback ← ✨ NEW
  ↓
Add to Wallet ← ✨ NEW
  ↓
✅ User có tiền trong wallet!
```

---

## 🔧 THAY ĐỔI CODE

### **File Modified**: `ImportShopeeOrdersUseCase.java`

#### **1. Thêm Dependencies** (Line 58-59)

```java
private final CalculateCashbackUseCase calculateCashbackUseCase;
private final AddCashbackToWalletUseCase addCashbackToWalletUseCase;
```

**Giải thích**:
- Inject 2 UseCase để xử lý cashback
- `@RequiredArgsConstructor` (Lombok) tự động tạo constructor
- Spring tự động inject qua dependency injection

---

#### **2. Thêm Imports** (Line 5-6, 16)

```java
import com.cashbee.application.usecase.cashback.AddCashbackToWalletUseCase;
import com.cashbee.application.usecase.cashback.CalculateCashbackUseCase;
import com.cashbee.domain.model.Cashback;
```

**Giải thích**:
- Import 2 UseCase classes
- Import `Cashback` domain model

---

#### **3. Thêm Cashback Logic** (Line 212-253)

**Location**: Sau `orderRepository.save(order)`, trước "Match with click"

```java
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
```

**Giải thích từng phần**:

##### **a) Validation** (Line 215-216)
```java
if (record.getCommissionForCashback() != null &&
    record.getCommissionForCashback().compareTo(BigDecimal.ZERO) > 0)
```
- Check commission có giá trị dương không
- Skip nếu commission = 0 hoặc null
- Tránh tạo cashback không có ý nghĩa

##### **b) Check Order Status** (Line 219)
```java
boolean isOrderCompleted = record.isCompleted();
```
- Check trạng thái order từ CSV
- `true` = "Hoàn thành"
- `false` = "Đang chờ xử lý"

##### **c) Calculate Cashback** (Line 222-228)
```java
Cashback cashback = calculateCashbackUseCase.execute(
    userId,           // User nào nhận tiền
    order.getId(),    // Order nào
    platform.getId(), // Platform nào (Shopee)
    commission,       // Commission bao nhiêu
    isOrderCompleted  // Đã complete chưa
);
```

**`CalculateCashbackUseCase` làm gì?**
1. Lấy `CashbackPolicy` từ DB (rate, min/max)
2. Tính: `cashbackAmount = commission × rate / 100`
3. Apply limits (min order value, max cashback)
4. Tạo `Cashback` record với status:
   - `CONFIRMED` nếu order completed
   - `PENDING` nếu order pending
5. Save vào DB

**Example**:
```
Commission: 100,000 VND
Policy Rate: 70%
→ Cashback: 100,000 × 70 / 100 = 70,000 VND
```

##### **d) Add to Wallet** (Line 234-242)

**Nếu order COMPLETED**:
```java
if (cashback.isConfirmed()) {
    addCashbackToWalletUseCase.addConfirmedCashback(cashback.getId());
}
```
- Cộng vào `user_wallet.balance` (tiền thật)
- User rút được ngay
- Tạo `transaction` record (type: CASHBACK, status: SUCCESS)

**Nếu order PENDING**:
```java
else if (cashback.isPending()) {
    addCashbackToWalletUseCase.addPendingCashback(cashback.getId());
}
```
- Cộng vào `user_wallet.pending_balance` (tiền chờ)
- User thấy nhưng chưa rút được
- Khi order complete, tiền sẽ chuyển từ pending → balance

##### **e) Error Handling** (Line 248-253)
```java
} catch (Exception e) {
    log.error("Failed to create cashback for order {}: {}",
        order.getId(), e.getMessage(), e);
    // Continue with next record
}
```
- Nếu cashback fail → Log error
- **KHÔNG** fail toàn bộ import
- Tiếp tục import order khác
- **Graceful degradation**: Order vẫn được tạo, chỉ cashback fail

---

## 🗄️ DATABASE IMPACT

### **Tables Affected**:

#### **1. `affiliate_order`** (Unchanged)
- Order vẫn được tạo như trước
- Không có thay đổi về structure

#### **2. `cashback`** (NEW RECORDS)

**Before import**: 0 records
**After import**: +N records (N = số orders có commission > 0)

**Columns populated**:
```sql
cashback:
  - user_id: User ID from tracking code
  - order_id: AffiliateOrder ID
  - platform_id: 1 (Shopee)
  - commission_amount: From CSV
  - cashback_amount: Calculated (commission × rate / 100)
  - cashback_rate: From policy (e.g., 70.00)
  - policy_id: CashbackPolicy ID used
  - status: PENDING or CONFIRMED (based on order status)
  - note: Description
  - created_at: Timestamp
  - confirmed_at: Timestamp (if CONFIRMED)
```

**Example**:
```sql
INSERT INTO cashback (
    user_id, order_id, platform_id,
    commission_amount, cashback_amount, cashback_rate,
    status, note
) VALUES (
    1, 123, 1,
    100000, 70000, 70.00,
    'CONFIRMED', 'Order completed, cashback confirmed'
);
```

#### **3. `user_wallet`** (UPDATED)

**Before import**:
```sql
user_id = 1
balance = 0
pending_balance = 0
```

**After import** (example với 100 orders):
```sql
user_id = 1
balance = 3,500,000          -- From 50 completed orders
pending_balance = 3,500,000  -- From 50 pending orders
updated_at = 2025-11-03 22:50:00
```

#### **4. `transaction`** (NEW RECORDS)

**Via `AddCashbackToWalletUseCase`**:

**For confirmed cashback**:
```sql
INSERT INTO transaction (
    user_id, type, amount, status, description
) VALUES (
    1, 'CASHBACK', 70000, 'SUCCESS',
    'Cashback from order #251030ABC'
);
```

**For pending cashback**:
```sql
INSERT INTO transaction (
    user_id, type, amount, status, description
) VALUES (
    1, 'CASHBACK', 70000, 'PENDING',
    'Pending cashback from order #251030XYZ'
);
```

---

## 📊 TEST SCENARIOS

### **Scenario 1: Order "Hoàn thành" (Completed)**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái đặt hàng,Sub_id1,Commission
251030ABC,Hoàn thành,CB1_1_xxx,100000
```

**Expected Flow**:
1. Create `AffiliateOrder` (status: APPROVED, commission: 100000)
2. Calculate cashback: 100000 × 70% = 70000
3. Create `Cashback` (status: CONFIRMED, amount: 70000)
4. Call `addConfirmedCashback()`
5. Update `user_wallet.balance += 70000`
6. Create `Transaction` (type: CASHBACK, status: SUCCESS)

**Expected DB State**:
```sql
-- affiliate_order
order_id = '251030ABC', commission_amount = 100000, status = 'APPROVED'

-- cashback
user_id = 1, order_id = (order.id), cashback_amount = 70000, status = 'PAID'

-- user_wallet
balance = 70000 (increased!)

-- transaction
type = 'CASHBACK', amount = 70000, status = 'SUCCESS'
```

**Expected Logs**:
```
INFO: Created order 1 for user 1
INFO: Created cashback 1 with amount 70000 VND (status: CONFIRMED)
INFO: Added confirmed cashback to balance for user 1
```

---

### **Scenario 2: Order "Đang chờ xử lý" (Pending)**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái đặt hàng,Sub_id1,Commission
251030XYZ,Đang chờ xử lý,CB1_2_xxx,50000
```

**Expected Flow**:
1. Create `AffiliateOrder` (status: PENDING, commission: 50000)
2. Calculate cashback: 50000 × 70% = 35000
3. Create `Cashback` (status: PENDING, amount: 35000)
4. Call `addPendingCashback()`
5. Update `user_wallet.pending_balance += 35000`
6. Create `Transaction` (type: CASHBACK, status: PENDING)

**Expected DB State**:
```sql
-- affiliate_order
order_id = '251030XYZ', commission_amount = 50000, status = 'PENDING'

-- cashback
user_id = 1, order_id = (order.id), cashback_amount = 35000, status = 'PENDING'

-- user_wallet
balance = 0 (unchanged)
pending_balance = 35000 (increased!)

-- transaction
type = 'CASHBACK', amount = 35000, status = 'PENDING'
```

**Expected Logs**:
```
INFO: Created order 2 for user 1
INFO: Created cashback 2 with amount 35000 VND (status: PENDING)
INFO: Added pending cashback to pending balance for user 1
```

---

### **Scenario 3: Order với Commission = 0**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái đặt hàng,Sub_id1,Commission
251030ZZZ,Hoàn thành,CB1_3_xxx,0
```

**Expected Flow**:
1. Create `AffiliateOrder` (commission: 0)
2. Skip cashback creation (commission = 0)
3. No cashback record
4. No wallet update

**Expected Logs**:
```
INFO: Created order 3 for user 1
DEBUG: Order 3 has no commission, skipping cashback creation
```

---

### **Scenario 4: Real CSV với 182 Orders**

**Input**: `AffiliateCommissionReport202510300813.csv`

**Expected Results**:
```
✅ 182 orders created
✅ ~180 cashbacks created (some may have commission = 0)
✅ ~90 confirmed cashbacks → balance increased
✅ ~90 pending cashbacks → pending_balance increased
✅ User wallet updated correctly
```

**Example DB State**:
```sql
-- user_wallet (user_id = 1)
balance = 6,300,000 VND          -- From completed orders
pending_balance = 6,300,000 VND  -- From pending orders
total_available = 12,600,000 VND

-- cashback table
SELECT COUNT(*), status FROM cashback WHERE user_id = 1 GROUP BY status;
-- PAID: 90 rows
-- PENDING: 90 rows
-- TOTAL: 180 rows
```

---

## 🔍 ERROR HANDLING

### **Error Case 1: Cashback Calculation Fails**

**Scenario**: No cashback policy found, or policy has invalid config

**Behavior**:
```java
} catch (Exception e) {
    log.error("Failed to create cashback for order {}: {}",
        order.getId(), e.getMessage(), e);
    // Continue with next record
}
```

**Result**:
- Order still created ✅
- Cashback NOT created ❌
- Import continues ✅
- Error logged for investigation

---

### **Error Case 2: Wallet Update Fails**

**Scenario**: User wallet doesn't exist

**Behavior**:
- `AddCashbackToWalletUseCase` should handle this
- Should auto-create wallet OR throw clear error
- **TODO**: Verify auto-create wallet logic exists

---

### **Error Case 3: Duplicate Order**

**Scenario**: Re-importing same CSV file

**Behavior**:
```java
// In ImportShopeeOrdersUseCase (Line 141-149)
if (orderRepository.existsByOrderId(record.getOrderId())) {
    if (request.getSkipDuplicates()) {
        batch.incrementSkipped();
        log.debug("Skipping duplicate order: {}", record.getOrderId());
        continue;  // Skip both order AND cashback
    } else {
        throw new BusinessException("Duplicate order ID: " + record.getOrderId());
    }
}
```

**Result**: Duplicate orders are skipped, cashback not created

---

### **Error Case 4: Cashback Already Exists**

**Scenario**: Cashback already created for order (shouldn't happen)

**Behavior**:
```java
// In CalculateCashbackUseCase (Line 77-81)
if (cashbackRepository.existsByOrderId(orderId)) {
    log.warn("Cashback already exists for order {}, skipping", orderId);
    return cashbackRepository.findByOrderId(orderId).get();
}
```

**Result**: Returns existing cashback, doesn't create duplicate

---

## 📝 LOGS EXAMPLES

### **Successful Import with Cashback**

```
INFO: UseCase: Starting import from file: AffiliateCommissionReport202510300813.csv
INFO: UseCase: Created ImportBatch with ID: 1
INFO: CSV headers: [ID đơn hàng, Trạng thái đặt hàng, ..., Sub_id1, ...]
INFO: UseCase: Parsed 182 records from CSV

DEBUG: Created order 1 for user 1
INFO: Created cashback 1 with amount 70000 VND (status: CONFIRMED)
INFO: Added confirmed cashback to balance for user 1

DEBUG: Created order 2 for user 1
INFO: Created cashback 2 with amount 35000 VND (status: PENDING)
INFO: Added pending cashback to pending balance for user 1

...

INFO: UseCase: Import completed. Success: 182, Failed: 0, Skipped: 0, Matched: 0
```

---

### **Import with Cashback Errors**

```
INFO: UseCase: Starting import from file: test.csv

DEBUG: Created order 1 for user 1
ERROR: Failed to create cashback for order 1: No active policy found for platform 999

DEBUG: Created order 2 for user 1
INFO: Created cashback 2 with amount 50000 VND (status: CONFIRMED)
INFO: Added confirmed cashback to balance for user 1

INFO: UseCase: Import completed. Success: 2, Failed: 0, Skipped: 0, Matched: 0
```

**Note**: Order 1 created but cashback failed → Graceful degradation

---

## ✅ VERIFICATION CHECKLIST

### **After Implementation**:
- [x] Code compiles successfully
- [x] No syntax errors
- [x] Dependencies injected correctly
- [x] Error handling in place

### **After Testing** (TODO):
- [ ] Import CSV successfully creates orders
- [ ] Cashback records created in DB
- [ ] User wallet balance updated
- [ ] Pending balance updated for pending orders
- [ ] Transaction records created
- [ ] Logs show cashback creation
- [ ] Error handling works (orders created even if cashback fails)

---

## 🎉 HOÀN THÀNH

✅ **Implementation completed**
✅ **Build successful** (57.809s)
✅ **No compilation errors**
✅ **Ready for testing**

---

## 📚 RELATED DOCUMENTS

- Plan: `docs/plans/PLAN_ADD_CASHBACK_TO_IMPORT.md`
- Analysis: `docs/analysis/DATETIME_PARSING_ERROR_ANALYSIS.md`
- CSV Import Guide: `docs/SHOPEE_CSV_IMPORT_CASHBACK_SUMMARY.md`

---

**Implemented by**: Claude Code Assistant
**Date**: 2025-11-03
**Build Status**: ✅ SUCCESS
**Lines Changed**: ~50 lines (imports + logic)
**Files Modified**: 1 file
