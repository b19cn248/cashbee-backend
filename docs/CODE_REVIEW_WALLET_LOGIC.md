# Code Review: Wallet Update Logic Analysis

## 📋 Overview

**Date**: 2025-11-22
**Reviewer**: AI Assistant
**Component**: Cashback & Wallet Management
**Severity**: Medium - Potential Logic Conflicts

## 🎯 Executive Summary

Sau khi review toàn bộ flow import orders và wallet update logic, tôi phát hiện:

✅ **KHÔNG CÓ BUG** - Logic hiện tại hoạt động ĐÚNG
⚠️ **CÓ CONCERNS** - Có một số điểm cần cải thiện về code quality và safety

## 🔍 Chi Tiết Phân Tích

### 1. Flow Import Order với Status Change

#### Scenario: Import lần 1 (PENDING) → Import lần 2 (APPROVED)

**Import Lần 1** (Order PENDING):
```
1. Create AffiliateOrder (status: PENDING)
2. Create AffiliateOrderItem (status: PENDING)
3. executeForItem() → Create Cashback (status: PENDING)
4. updateWalletForNewCashback(amount, PENDING)
   → pending_balance += 977.55
   → total_earned KHÔNG thay đổi ✅
```

**Import Lần 2** (Order APPROVED):
```
1. Update AffiliateOrder (status: PENDING → APPROVED)
2. Update AffiliateOrderItem (status: PENDING → APPROVED)
3. upsertForItem() → Find existing Cashback
4. cashback.getStatus() = PENDING, newStatus = CONFIRMED
5. updateWalletForStatusChange(amount, PENDING → CONFIRMED)
   → pending_balance -= 977.55
   → balance += 977.55
   → total_earned += 977.55  ✅
```

**Kết Quả**:
- ✅ pending_balance: 977.55 → 0
- ✅ balance: 0 → 977.55
- ✅ total_earned: 0 → 977.55

**ĐÚNG!** Logic hoạt động chính xác như thiết kế.

---

### 2. Concerns về Code Quality

#### Concern #1: Không sử dụng Domain Methods

**Hiện tại** (`CalculateCashbackUseCase.java:378-382`):
```java
// Directly set fields - KHÔNG có validation
wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
wallet.setBalance(wallet.getBalance().add(amount));
wallet.setTotalEarned(wallet.getTotalEarned().add(amount));
```

**Domain Model có sẵn** (`UserWallet.java:116-123`):
```java
public void confirmPendingBalance(BigDecimal amount) {
    validatePositiveAmount(amount);  // ✅ Validation
    if (MoneyUtils.isGreaterThan(amount, this.pendingBalance)) {
        throw InsufficientBalanceException.of(this.pendingBalance, amount);  // ✅ Guard
    }
    this.pendingBalance = MoneyUtils.subtract(this.pendingBalance, amount);
    this.balance = MoneyUtils.add(this.balance, amount);
    this.totalEarned = MoneyUtils.add(this.totalEarned, amount);
}
```

**Vấn Đề**:
- ❌ UseCase KHÔNG dùng business methods → Vi phạm DDD principles
- ❌ Bỏ qua validations → Có thể dẫn đến NEGATIVE balance
- ❌ Code duplication → Logic confirmation tồn tại ở 2 nơi

**Risk**:
- Nếu `pending_balance < amount` → wallet sẽ có **NEGATIVE pending_balance**!
- Hiện tại chưa xảy ra vì logic import đúng, nhưng là **time bomb**

#### Concern #2: Inconsistent Money Operations

**Nơi 1**: `CalculateCashbackUseCase` dùng `BigDecimal.add/subtract` trực tiếp
**Nơi 2**: `UserWallet` domain methods dùng `MoneyUtils.add/subtract`

**Vấn Đề**:
- Inconsistent → Khó maintain
- MoneyUtils có handling cho scale/rounding → An toàn hơn

---

### 3. Logic Flows Analysis

#### Flow 1: Create New PENDING Cashback

```
executeForItem(isCompleted=false)
└─> Create Cashback (status: PENDING)
└─> updateWalletForNewCashback(amount, PENDING)
    └─> pending_balance += amount
    └─> total_earned KHÔNG thay đổi ✅
```

**Đúng!** Pending cashback không tính vào total_earned.

#### Flow 2: Create New CONFIRMED Cashback

```
executeForItem(isCompleted=true)
└─> Create Cashback (status: CONFIRMED)
└─> updateWalletForNewCashback(amount, CONFIRMED)
    └─> balance += amount
    └─> total_earned += amount ✅
```

**Đúng!** Confirmed cashback tính luôn vào total_earned.

#### Flow 3: PENDING → CONFIRMED

```
upsertForItem(isCompleted=true) on existing PENDING cashback
└─> Find existing Cashback (status: PENDING)
└─> newStatus = CONFIRMED
└─> updateWalletForStatusChange(amount, PENDING → CONFIRMED)
    └─> pending_balance -= amount
    └─> balance += amount
    └─> total_earned += amount ✅
```

**Đúng!** Move từ pending sang confirmed, tăng total_earned.

#### Flow 4: CONFIRMED → PENDING (Rare)

```
upsertForItem(isCompleted=false) on existing CONFIRMED cashback
└─> Find existing Cashback (status: CONFIRMED)
└─> newStatus = PENDING
└─> updateWalletForStatusChange(amount, CONFIRMED → PENDING)
    └─> balance -= amount
    └─> total_earned -= amount
    └─> pending_balance += amount ✅
```

**Đúng!** Reverse flow, giảm total_earned.

---

### 4. Edge Cases Analysis

#### Case 1: Import COMPLETED order lần đầu

**Input**: Order đã COMPLETED từ Shopee (chưa có trong DB)

**Flow**:
```
processOrderWithItems()
└─> existingOrder = null
└─> Create new order (status: APPROVED)
└─> Create order item (status: APPROVED)
└─> executeForItem(isCompleted=true)
    └─> Create Cashback (status: CONFIRMED)
    └─> updateWalletForNewCashback(amount, CONFIRMED)
        └─> balance += amount
        └─> total_earned += amount
```

**Kết Quả**:
- ✅ Cashback: CONFIRMED
- ✅ Wallet: balance tăng, total_earned tăng
- ✅ pending_balance KHÔNG thay đổi

**ĐÚNG!** Hoàn toàn chính xác.

#### Case 2: Import PENDING, sau đó CANCELLED

**Hiện tại CHƯA HANDLE!** Không có logic cancel cashback.

**Cần**:
```java
// Nếu order bị cancelled
if (newStatus == OrderStatus.CANCELLED) {
    cancelCashback(orderItemId);
    // Gọi wallet.cancelPendingBalance() hoặc wallet.reverseConfirmedCashback()
}
```

**Risk**: ⚠️ Nếu order bị cancel, cashback vẫn còn trong wallet!

#### Case 3: Re-import cùng file nhiều lần

**Input**: Import file 2 lần (duplicate)

**Flow lần 1**:
```
Order chưa tồn tại → Create → pending_balance += 977.55
```

**Flow lần 2** (với updateMode=SKIP):
```
Order đã tồn tại → Skip → KHÔNG update wallet
```

**Flow lần 2** (với updateMode=UPDATE):
```
Order đã tồn tại → Update
└─> upsertForItem() tìm existing cashback
└─> Status KHÔNG đổi (PENDING → PENDING)
└─> updateWalletForStatusChange() bị skip (line 369-371)
    └─> KHÔNG update wallet ✅
```

**ĐÚNG!** Không update wallet nếu status không đổi.

---

### 5. Transaction Boundaries Analysis

#### Transaction Flow:

```
ImportShopeeOrdersUseCase.execute() @Transactional(REQUIRED)
└─> processBatch() @Transactional(MANDATORY) - cùng transaction
    └─> processOrderWithItems()
        └─> updateExistingOrderWithItems()
            └─> upsertForItem() @Transactional - NEW transaction!
                └─> updateWalletForStatusChange()
                    └─> walletRepository.save()
```

**Vấn Đề**:
- `upsertForItem()` có `@Transactional` → Mặc định là REQUIRED
- Nếu nested transaction → Có thể tạo nested transaction (depends on JPA provider)
- Nếu `upsertForItem()` throw exception → Rollback tất cả!

**Hiện Tại**: Logic đúng vì chạy trong cùng transaction.

**Risk**: Nếu sau này ai đó thêm logic throw exception KHÔNG đúng chỗ → Toàn bộ import rollback!

---

## 📊 Summary of Findings

| Item | Status | Severity | Notes |
|------|--------|----------|-------|
| Wallet update logic | ✅ Correct | - | Hoạt động đúng như thiết kế |
| PENDING → CONFIRMED flow | ✅ Correct | - | Balance transitions correctly |
| total_earned tracking | ✅ Correct | - | Only increases on CONFIRMED |
| Code uses domain methods | ❌ No | Medium | Should use `UserWallet.confirmPendingBalance()` |
| Validation on wallet updates | ❌ Missing | Medium | No check for negative balance |
| Cancel cashback logic | ❌ Missing | Low | No handling for cancelled orders |
| Transaction boundaries | ⚠️ Concern | Low | Multiple `@Transactional` may cause issues |
| Money operations | ⚠️ Inconsistent | Low | Mix of BigDecimal vs MoneyUtils |

---

## 🛠️ Recommendations

### Priority 1: HIGH - Use Domain Methods

**Problem**: `CalculateCashbackUseCase` directly sets wallet fields, bypassing validations.

**Solution**: Sử dụng domain methods của `UserWallet`:

```java
// BEFORE (CalculateCashbackUseCase.java:378-382)
wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
wallet.setBalance(wallet.getBalance().add(amount));
wallet.setTotalEarned(wallet.getTotalEarned().add(amount));

// AFTER - Use domain method
wallet.confirmPendingBalance(amount);  // Has validation!
```

**Benefits**:
- ✅ Automatic validation
- ✅ Guards against negative balance
- ✅ Single source of truth
- ✅ DDD best practices

### Priority 2: MEDIUM - Add Cancel Cashback Logic

**Problem**: No handling for cancelled orders.

**Solution**: Add method to handle order cancellation:

```java
@Transactional
public void cancelCashback(Long orderItemId) {
    Cashback cashback = cashbackRepository.findByOrderItemId(orderItemId)
        .orElseThrow(() -> new NotFoundException("Cashback not found"));

    if (cashback.getStatus() == CashbackStatus.PENDING) {
        UserWallet wallet = getOrCreateWallet(cashback.getUserId());
        wallet.cancelPendingBalance(cashback.getCashbackAmount());  // Domain method
        walletRepository.save(wallet);
    } else if (cashback.getStatus() == CashbackStatus.CONFIRMED) {
        UserWallet wallet = getOrCreateWallet(cashback.getUserId());
        wallet.reverseConfirmedCashback(cashback.getCashbackAmount());  // Domain method
        walletRepository.save(wallet);
    }

    Cashback cancelled = cashback.withStatus(CashbackStatus.CANCELLED, "Order cancelled");
    cashbackRepository.save(cancelled);
}
```

### Priority 3: LOW - Consistent Money Operations

**Problem**: Inconsistent use of `BigDecimal` vs `MoneyUtils`.

**Solution**: Always use `MoneyUtils` for money operations:

```java
// BEFORE
wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));

// AFTER
wallet.setPendingBalance(MoneyUtils.subtract(wallet.getPendingBalance(), amount));
```

**Better**: Just use domain methods (see Priority 1).

### Priority 4: LOW - Review Transaction Boundaries

**Problem**: Multiple `@Transactional` annotations may cause confusion.

**Solution**: Document transaction boundaries clearly:

```java
/**
 * Upsert cashback for item.
 *
 * TRANSACTION: Joins parent transaction (REQUIRED).
 * If called from import flow, runs in same transaction as import.
 *
 * @Transactional(propagation = Propagation.REQUIRED)
 */
public Cashback upsertForItem(...) { ... }
```

---

## ✅ Conclusion

**WALLET UPDATE LOGIC LÀ CHÍNH XÁC!**

Từ logs bạn cung cấp:
```
10:28:01.977 - BEFORE: pending=977.55, balance=0.00
10:28:01.977 - AFTER: pending=0.00, balance=977.55
10:28:01.978 - Wallet saved: balance=977.55
10:28:02.761 - SQL UPDATE wallet executed
```

**pending_balance ĐÃ BỊ TRỪ CHÍNH XÁC!**

Nếu bạn vẫn thấy `pending_balance` không bị trừ sau import:
1. Check lại **USER_ID** (có phải user 2 không?)
2. Check **SAU KHI** import hoàn tất (không check trong quá trình)
3. Check có import **lần 3** hay thao tác nào khác sau logs này không?

---

## 📝 Files Reviewed

1. `ImportShopeeOrdersUseCase.java` - Import flow
2. `CalculateCashbackUseCase.java` - Cashback calculation & wallet updates
3. `UserWallet.java` - Domain model với business methods
4. `AffiliateOrder.java` - Order model
5. `Cashback.java` - Cashback model

---

## 🔗 Related Documents

- [IMPORT_REPORTING_LOGIC.md](./IMPORT_REPORTING_LOGIC.md)
- [FIX_IMPORT_BATCH_REPORTING_BUG.md](./fixes/FIX_IMPORT_BATCH_REPORTING_BUG.md)
