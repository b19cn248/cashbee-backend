# 📋 PLAN: Tích hợp Cashback vào CSV Import

**Date**: 2025-11-03
**Status**: 📝 **PLANNING**

---

## 🎯 MỤC TIÊU

Sau khi import orders từ CSV Shopee, **tự động tính cashback và cộng tiền cho user**.

---

## 🔍 HIỆN TRẠNG

### **Flow hiện tại** (CHƯA ĐẦY ĐỦ):

```
CSV File
  ↓
ImportShopeeOrdersUseCase
  ↓
Parse CSV → Create AffiliateOrder → Save to DB
  ↓
❌ STOP (KHÔNG TẠO CASHBACK!)
```

### **UseCase có sẵn** (đã implement):

✅ **CalculateCashbackUseCase** - Tính cashback từ commission
- Input: userId, orderId, platformId, commissionAmount, isOrderCompleted
- Output: Cashback record (status: PENDING or CONFIRMED)

✅ **AddCashbackToWalletUseCase** - Cộng cashback vào wallet
- `addConfirmedCashback()` - Cộng vào balance (order completed)
- `addPendingCashback()` - Cộng vào pending_balance (order pending)

### **Vấn đề**:

❌ `ImportShopeeOrdersUseCase` **KHÔNG GỌI** 2 UseCase trên
❌ Sau import, orders có commission nhưng **KHÔNG CÓ cashback**
❌ User **KHÔNG NHẬN** được tiền

---

## ✅ GIẢI PHÁP

### **Flow mới** (CẦN IMPLEMENT):

```
CSV File
  ↓
ImportShopeeOrdersUseCase
  ↓
1. Parse CSV
  ↓
2. Create AffiliateOrder → Save to DB
  ↓
3. CalculateCashbackUseCase  ← ✨ THÊM BƯỚC NÀY
    - Calculate cashback from commission
    - Create Cashback record (PENDING or CONFIRMED)
  ↓
4. AddCashbackToWalletUseCase  ← ✨ THÊM BƯỚC NÀY
    - If CONFIRMED → Add to balance
    - If PENDING → Add to pending_balance
  ↓
✅ DONE (User có tiền trong wallet!)
```

---

## 📝 IMPLEMENTATION STEPS

### **STEP 1: Inject Dependencies**

**File**: `ImportShopeeOrdersUseCase.java`

**Add**:
```java
private final CalculateCashbackUseCase calculateCashbackUseCase;
private final AddCashbackToWalletUseCase addCashbackToWalletUseCase;
```

**Giải thích**:
- Cần inject 2 UseCase để gọi trong import flow
- Spring sẽ tự động inject qua constructor (có `@RequiredArgsConstructor`)

---

### **STEP 2: Add Cashback Logic After Creating Order**

**File**: `ImportShopeeOrdersUseCase.java`
**Location**: Sau dòng 205 (sau `orderRepository.save(order)`)

**Add logic**:

```java
// Step: Calculate and add cashback
try {
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

    // Add cashback to wallet
    if (cashback.isConfirmed()) {
        // Order completed → Add to balance directly
        addCashbackToWalletUseCase.addConfirmedCashback(cashback.getId());
        log.info("Added confirmed cashback to wallet for user {}", userId);
    } else if (cashback.isPending()) {
        // Order pending → Add to pending_balance
        addCashbackToWalletUseCase.addPendingCashback(cashback.getId());
        log.info("Added pending cashback to pending balance for user {}", userId);
    }

} catch (Exception e) {
    // Don't fail entire import if cashback fails
    log.error("Failed to create cashback for order {}: {}",
        order.getId(), e.getMessage(), e);
    // Continue with next record
}
```

**Giải thích từng bước**:

1. **`isOrderCompleted = record.isCompleted()`**
   - Check xem order đã "Hoàn thành" chưa
   - `ShopeeOrderRecord.isCompleted()` check status từ CSV

2. **`calculateCashbackUseCase.execute(...)`**
   - Tính cashback amount
   - Lấy policy rate (ví dụ 70%)
   - Formula: `cashbackAmount = commission * rate / 100`
   - Tạo `Cashback` record với status:
     - `CONFIRMED` nếu order completed
     - `PENDING` nếu order pending

3. **`if (cashback.isConfirmed())`**
   - Nếu order đã completed → Cộng vào `balance` ngay
   - User nhận tiền luôn

4. **`else if (cashback.isPending())`**
   - Nếu order đang pending → Cộng vào `pending_balance`
   - User thấy tiền chờ, chưa rút được

5. **`try-catch`**
   - Nếu cashback fail → Log error nhưng KHÔNG FAIL toàn bộ import
   - Import tiếp order khác

---

### **STEP 3: Update Response to Include Cashback Stats**

**File**: `ImportOrdersResponse.java`

**Add fields** (optional):
```java
private Integer cashbackCreatedCount;      // Số cashback đã tạo
private BigDecimal totalCashbackAmount;    // Tổng tiền cashback
private Integer confirmedCashbackCount;    // Số cashback CONFIRMED
private Integer pendingCashbackCount;      // Số cashback PENDING
```

**Update logic trong `ImportShopeeOrdersUseCase`**:
- Track counters khi tạo cashback
- Return trong response

---

### **STEP 4: Add Configuration (Optional)**

**File**: `application.yml`

**Add config**:
```yaml
cashbee:
  import:
    auto-create-cashback: true  # Enable/disable auto cashback
    cashback-on-error: skip     # skip | fail | log
```

**Giải thích**:
- `auto-create-cashback`: Bật/tắt tự động tạo cashback
- `cashback-on-error`:
  - `skip`: Bỏ qua order nếu cashback fail
  - `fail`: Fail toàn bộ import
  - `log`: Log error và tiếp tục

---

## 🧪 TESTING PLAN

### **Test Case 1: Import với Order "Hoàn thành"**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái đặt hàng,...,Sub_id1,Commission
251030ABC,Hoàn thành,...,CB1_1_xxx,100000
```

**Expected**:
```sql
-- affiliate_order table
order_id = '251030ABC'
user_id = 1
commission_amount = 100000
status = 'APPROVED'

-- cashback table
order_id = (order.id)
user_id = 1
commission_amount = 100000
cashback_amount = 70000  -- (70% of 100000)
status = 'PAID'

-- user_wallet table
user_id = 1
balance += 70000  -- Increased!
pending_balance = 0
```

**Logs**:
```
INFO: Created order 1 for user 1
INFO: Created cashback 1 with amount 70000 VND (status: CONFIRMED)
INFO: Added confirmed cashback to wallet for user 1
```

---

### **Test Case 2: Import với Order "Đang chờ xử lý"**

**Input CSV**:
```csv
ID đơn hàng,Trạng thái đặt hàng,...,Sub_id1,Commission
251030XYZ,Đang chờ xử lý,...,CB1_2_xxx,50000
```

**Expected**:
```sql
-- affiliate_order table
order_id = '251030XYZ'
status = 'PENDING'
commission_amount = 50000

-- cashback table
cashback_amount = 35000  -- (70% of 50000)
status = 'PENDING'

-- user_wallet table
balance = (unchanged)
pending_balance += 35000  -- Added to pending!
```

**Logs**:
```
INFO: Created order 2 for user 1
INFO: Created cashback 2 with amount 35000 VND (status: PENDING)
INFO: Added pending cashback to pending balance for user 1
```

---

### **Test Case 3: Import Multiple Orders**

**Input CSV**: 182 orders (mix of completed and pending)

**Expected**:
```
✅ 182 orders created
✅ 182 cashbacks created
✅ ~50 confirmed cashbacks → added to balance
✅ ~132 pending cashbacks → added to pending_balance
✅ User wallet updated correctly
```

---

### **Test Case 4: Cashback Calculation với Policy**

**Setup**: CashbackPolicy trong DB
```sql
platform_id = 1 (Shopee)
user_level = 'NORMAL'
cashback_rate = 70.00
min_order_value = 10000
max_cashback_per_order = 500000
```

**Test cases**:

| Commission | Rate | Calculated | After Min/Max | Expected |
|-----------|------|-----------|---------------|----------|
| 100,000 | 70% | 70,000 | 70,000 | ✅ 70,000 |
| 5,000 | 70% | 3,500 | 0 | ✅ 0 (below min) |
| 1,000,000 | 70% | 700,000 | 500,000 | ✅ 500,000 (capped) |

---

## 📊 DATABASE IMPACT

### **Tables Affected**:

**1. `affiliate_order`** (unchanged)
- Already created by import

**2. `cashback`** (NEW RECORDS)
```sql
-- Before: 0 records
-- After: +182 records (one per order)
```

**3. `user_wallet`** (UPDATED)
```sql
-- Before
user_id = 1
balance = 0
pending_balance = 0

-- After
user_id = 1
balance = 3,500,000  -- From completed orders
pending_balance = 4,600,000  -- From pending orders
```

**4. `transaction`** (NEW RECORDS - via wallet UseCase)
```sql
-- One transaction per cashback added to wallet
type = 'CASHBACK'
status = 'SUCCESS' (for confirmed) or 'PENDING'
amount = cashback_amount
```

---

## ⚠️ ERROR HANDLING

### **Scenario 1: Commission Amount = 0 or NULL**

**Problem**: Some orders might have 0 commission

**Solution**: Skip cashback creation
```java
if (record.getCommissionForCashback() == null ||
    record.getCommissionForCashback().compareTo(BigDecimal.ZERO) <= 0) {
    log.debug("Order {} has no commission, skipping cashback", order.getId());
    continue;  // Skip cashback but order is still created
}
```

---

### **Scenario 2: No Cashback Policy Found**

**Problem**: No active policy for platform

**Solution**: `CalculateCashbackUseCase` uses default rate (70%)
```java
// In CalculateCashbackUseCase (line 97-100)
if (policy == null) {
    cashbackRate = new BigDecimal("70.00");  // Default
    log.warn("No active policy found, using default rate 70%");
}
```

---

### **Scenario 3: User Wallet Not Found**

**Problem**: User doesn't have wallet yet

**Solution**: Auto-create wallet
```java
// In AddPendingBalanceUseCase / ConfirmPendingBalanceUseCase
// Should handle wallet creation if not exists
```

**Check if this is implemented**:
- Need to verify `AddPendingBalanceUseCase` implementation
- If not, add auto-create wallet logic

---

### **Scenario 4: Cashback Already Exists for Order**

**Problem**: Re-importing same order

**Solution**: Already handled in `CalculateCashbackUseCase` (line 77-81)
```java
if (cashbackRepository.existsByOrderId(orderId)) {
    log.warn("Cashback already exists for order {}, skipping", orderId);
    return cashbackRepository.findByOrderId(orderId).get();
}
```

---

## 🎯 SUCCESS CRITERIA

✅ **Functional**:
- [ ] After CSV import, `cashback` table has records
- [ ] User wallet `balance` increased for completed orders
- [ ] User wallet `pending_balance` increased for pending orders
- [ ] Transaction records created for audit

✅ **Performance**:
- [ ] Import time increases < 20% (cashback logic is fast)
- [ ] No N+1 query issues

✅ **Reliability**:
- [ ] Import doesn't fail if cashback fails (graceful degradation)
- [ ] Duplicate orders don't create duplicate cashbacks
- [ ] Cancelled orders don't create cashbacks

✅ **Logging**:
- [ ] Clear logs showing cashback creation
- [ ] Errors logged with context (orderId, userId)
- [ ] Summary stats in response

---

## 📝 CHECKLIST

### **Code Changes**:
- [ ] Update `ImportShopeeOrdersUseCase.java`:
  - [ ] Inject `CalculateCashbackUseCase`
  - [ ] Inject `AddCashbackToWalletUseCase`
  - [ ] Add cashback logic after order creation
  - [ ] Add try-catch for error handling
  - [ ] Add logging

- [ ] Update `ImportOrdersResponse.java` (optional):
  - [ ] Add cashback statistics fields
  - [ ] Update builder to include stats

- [ ] Verify `AddPendingBalanceUseCase`:
  - [ ] Check if it auto-creates wallet
  - [ ] If not, add auto-create logic

### **Testing**:
- [ ] Unit test: Import with completed orders
- [ ] Unit test: Import with pending orders
- [ ] Unit test: Import with mixed orders
- [ ] Unit test: Import with 0 commission
- [ ] Integration test: Full flow with DB
- [ ] Manual test: Real CSV file

### **Documentation**:
- [ ] Update `SHOPEE_CSV_IMPORT_CASHBACK_SUMMARY.md`
- [ ] Add comment trong code explaining flow
- [ ] Update API docs if response changed

---

## 🚀 ROLLOUT PLAN

### **Phase 1: Implementation** (30-45 min)
1. Update `ImportShopeeOrdersUseCase` with cashback logic
2. Add error handling
3. Add logging

### **Phase 2: Testing** (15-30 min)
1. Run unit tests
2. Test with sample CSV file
3. Verify database records

### **Phase 3: Verification** (10-15 min)
1. Check user wallet balance
2. Verify transaction history
3. Confirm cashback records

### **Phase 4: Documentation** (10 min)
1. Update documentation
2. Add inline comments

**Total Estimated Time**: 1-2 hours

---

## 🎉 EXPECTED OUTCOME

### **Before** (Current):
```
CSV Import → Orders created → ❌ No cashback, no money
```

### **After** (Fixed):
```
CSV Import → Orders created → ✅ Cashback calculated → ✅ Money added to wallet
```

### **User Experience**:

**User's perspective**:
1. Admin imports CSV file
2. User checks wallet → **Sees balance increased!** 💰
3. User checks orders → Sees cashback for each order
4. User can withdraw money

**Impact**:
- ✅ Fully automated cashback flow
- ✅ No manual cashback creation needed
- ✅ Users receive money immediately (for completed orders)
- ✅ Transparent tracking (all records in DB)

---

**Created by**: Claude Code Assistant
**Date**: 2025-11-03
**Status**: 📝 **READY FOR IMPLEMENTATION**
