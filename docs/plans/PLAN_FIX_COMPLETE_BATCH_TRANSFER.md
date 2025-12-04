# Plan: Fix Complete Batch Transfer Logic

## 📊 Phân tích Database Schema Hiện tại

### ERD - Entity Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                           CASHBEE DATABASE SCHEMA                                     │
└──────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│      user       │         │   user_wallet   │         │ user_bank_account│
├─────────────────┤    1:1  ├─────────────────┤    1:1  ├─────────────────┤
│ id (PK)         │◄───────►│ id (PK)         │◄───────►│ id (PK)         │
│ email           │         │ user_id (FK,UK) │         │ user_id (FK,UK) │
│ phone           │         │ balance         │         │ account_number  │
│ full_name       │         │ pending_balance │         │ account_name    │
│ deleted_at      │         │ total_earned    │         │ bank_name       │
└────────┬────────┘         │ total_withdrawn │         │ bank_code       │
         │                  └─────────────────┘         └─────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ affiliate_order │    1:N  │affiliate_order_ │    1:1  │    cashback     │
├─────────────────┤────────►│     item        │◄───────►├─────────────────┤
│ id (PK)         │         ├─────────────────┤         │ id (PK)         │
│ platform_id(FK) │         │ id (PK)         │         │ user_id (FK)    │
│ user_id (FK)    │         │ order_id (FK)   │         │ order_id (FK)   │
│ order_id (UK)   │         │ item_id         │         │ order_item_id(FK)│
│ order_status    │         │ item_name       │         │ platform_id(FK) │
│ commission_amt  │         │ actual_amount   │         │ commission_amt  │
└─────────────────┘         │ item_commission │         │ cashback_amount │
                            │ status          │         │ cashback_rate   │
                            └─────────────────┘         │ status          │
                                                        │ paid_at         │
                                                        └─────────────────┘

┌─────────────────┐         ┌─────────────────┐
│batch_transfer_  │    1:N  │batch_transfer_  │
│    export       │────────►│     item        │
├─────────────────┤         ├─────────────────┤
│ id (PK)         │         │ id (PK)         │
│ batch_code (UK) │         │ batch_id (FK)   │
│ file_name       │         │ user_id (FK)    │
│ total_users     │         │ wallet_id (FK)  │
│ total_amount    │         │ amount          │    ❌ KHÔNG CÓ liên kết
│ status          │         │ account_number  │       đến cashback!
│ min_balance     │         │ account_name    │
│ remark_template │         │ bank_name       │
└─────────────────┘         │ status          │
                            └─────────────────┘

┌─────────────────┐
│  transactions   │
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │
│ wallet_id (FK)  │
│ type            │  (DEPOSIT, WITHDRAW, etc.)
│ amount          │
│ balance_before  │
│ balance_after   │
│ description     │  (chứa batch_code)
│ status          │
└─────────────────┘
```

---

## 🔍 Vấn đề hiện tại

### 1. Logic cập nhật cashback sai

**Hiện tại trong `CompleteBatchTransferUseCase.processItem()`:**
```java
// 5. Update cashback status: CONFIRMED → PAID
int updatedCashbacks = cashbackRepository.updateStatusByUserIdAndStatus(
        item.getUserId(),
        CashbackStatus.CONFIRMED,
        CashbackStatus.PAID
);
```

**Vấn đề:**
- Cập nhật **TẤT CẢ** cashback `CONFIRMED` của user → `PAID`
- Không phân biệt cashback nào thuộc batch nào
- Nếu user có thêm cashback `CONFIRMED` mới sau khi tạo batch → cũng bị đổi thành `PAID`!

### 2. Không có traceability

```
batchCode → batch_transfer_item → ??? → cashback
                                   ↑
                           MISSING LINK!
```

Từ `batchCode` **KHÔNG THỂ** truy vết được:
- Những cashback nào đã được paid trong batch đó
- Những đơn hàng nào đã được thanh toán

---

## 💡 Giải pháp đề xuất: Thêm cột `paid_batch_id` vào bảng `cashback`

### Tại sao chọn giải pháp này?

| Tiêu chí | Option 1: Bảng liên kết mới | Option 2: Thêm cột (✓) |
|----------|----------------------------|------------------------|
| Độ phức tạp | Cao (cần tạo entity, repo mới) | Thấp (chỉ thêm 1 cột) |
| Thay đổi code | Nhiều | Ít |
| Performance | Cần JOIN thêm | Truy vấn trực tiếp |
| Traceability | ✓ | ✓ |
| Đơn giản | ❌ | ✓ |

### Thiết kế mới

```
┌─────────────────┐
│    cashback     │
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │
│ order_id (FK)   │
│ order_item_id   │
│ ...             │
│ status          │  PENDING → CONFIRMED → PAID
│ paid_at         │
│ paid_batch_id   │  ← NEW! FK to batch_transfer_export
└─────────────────┘
         │
         │ N:1
         ▼
┌─────────────────┐
│batch_transfer_  │
│    export       │
├─────────────────┤
│ id (PK)         │
│ batch_code      │
│ ...             │
└─────────────────┘
```

**Ý nghĩa:**
- `paid_batch_id = NULL` → Cashback chưa được thanh toán hoặc đã được thanh toán trước khi có tính năng batch
- `paid_batch_id = 123` → Cashback được thanh toán trong batch có id = 123

---

## 📝 Chi tiết kế hoạch triển khai

### Phase 1: Database Migration

**File:** `032-add-paid-batch-id-to-cashback.xml`

```xml
<!-- 1. Thêm cột paid_batch_id -->
<addColumn tableName="cashback">
    <column name="paid_batch_id" type="BIGINT">
        <constraints nullable="true"/>
    </column>
</addColumn>

<!-- 2. Thêm foreign key -->
<addForeignKeyConstraint
    baseTableName="cashback"
    baseColumnNames="paid_batch_id"
    referencedTableName="batch_transfer_export"
    referencedColumnNames="id"/>

<!-- 3. Thêm index cho query -->
<createIndex tableName="cashback" indexName="idx_cashback_paid_batch_id">
    <column name="paid_batch_id"/>
</createIndex>
```

---

### Phase 2: Thay đổi Domain Model

**File:** `Cashback.java`

```java
// Thêm field mới
private final Long paidBatchId;
```

---

### Phase 3: Thay đổi JPA Entity

**File:** `CashbackJpaEntity.java`

```java
@Column(name = "paid_batch_id")
private Long paidBatchId;
```

---

### Phase 4: Thay đổi Repository

**File:** `CashbackJpaRepository.java`

Thay đổi query từ:
```java
@Query("UPDATE CashbackJpaEntity c
        SET c.status = :newStatus, c.paidAt = CURRENT_TIMESTAMP
        WHERE c.userId = :userId AND c.status = :oldStatus")
int updateStatusByUserIdAndStatus(...);
```

Thành:
```java
@Query("UPDATE CashbackJpaEntity c
        SET c.status = :newStatus,
            c.paidAt = CURRENT_TIMESTAMP,
            c.paidBatchId = :batchId
        WHERE c.userId = :userId
        AND c.status = :oldStatus
        AND c.paidBatchId IS NULL")  // Chỉ update những cashback chưa được paid
int updateStatusByUserIdAndStatusAndBatchId(...);
```

---

### Phase 5: Thay đổi UseCase Logic

**File:** `CompleteBatchTransferUseCase.java`

**Logic cũ:**
```java
// Cập nhật TẤT CẢ cashback CONFIRMED của user
cashbackRepository.updateStatusByUserIdAndStatus(
    item.getUserId(),
    CashbackStatus.CONFIRMED,
    CashbackStatus.PAID
);
```

**Logic mới:**
```java
// Chỉ cập nhật cashback CONFIRMED và chưa được paid (paidBatchId IS NULL)
// Đồng thời ghi nhận batchId
cashbackRepository.updateStatusByUserIdAndStatusAndBatchId(
    item.getUserId(),
    CashbackStatus.CONFIRMED,
    CashbackStatus.PAID,
    batch.getId()  // Ghi nhận batch đã thanh toán cashback này
);
```

---

### Phase 6: Thêm API để truy vết

**File:** `BatchTransferController.java` - Endpoint mới (optional)

```
GET /api/admin/batch-transfer/{batchCode}/cashbacks
```

Response:
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251119_001",
    "cashbacks": [
      {
        "cashbackId": 123,
        "userId": 456,
        "orderId": 789,
        "orderItemId": 101,
        "cashbackAmount": 50000,
        "paidAt": "2025-11-19T14:30:00"
      },
      ...
    ],
    "totalCashbacks": 50,
    "totalAmount": 2500000
  }
}
```

---

## 📁 Files cần thay đổi

| # | File | Thay đổi |
|---|------|----------|
| 1 | `032-add-paid-batch-id-to-cashback.xml` | **NEW** - Migration script |
| 2 | `db.changelog-master.xml` | Include file mới |
| 3 | `Cashback.java` (domain) | Thêm field `paidBatchId` |
| 4 | `CashbackJpaEntity.java` | Thêm column mapping |
| 5 | `CashbackMapper.java` | Thêm mapping cho field mới |
| 6 | `CashbackRepository.java` (interface) | Thêm method mới |
| 7 | `CashbackJpaRepository.java` | Thêm query mới |
| 8 | `CashbackRepositoryAdapter.java` | Implement method mới |
| 9 | `CompleteBatchTransferUseCase.java` | Sửa logic gọi repository |

---

## ✅ Checklist triển khai

- [ ] Phase 1: Tạo migration script
- [ ] Phase 2: Cập nhật Domain Model
- [ ] Phase 3: Cập nhật JPA Entity
- [ ] Phase 4: Cập nhật Repository layer
- [ ] Phase 5: Cập nhật UseCase logic
- [ ] Phase 6: (Optional) Thêm API truy vết
- [ ] Test: Unit test
- [ ] Test: Integration test
- [ ] Test: Manual test với production-like data

---

## 🎯 Kết quả mong đợi

### Trước khi fix:
```
User A có 5 cashback CONFIRMED
Tạo batch → Snapshot balance = 100,000 VND (từ 3 cashback cũ)
User A nhận thêm 2 cashback CONFIRMED mới (20,000 VND)
Complete batch → TẤT CẢ 5 cashback đều thành PAID ❌
```

### Sau khi fix:
```
User A có 5 cashback CONFIRMED
Tạo batch → Snapshot balance = 100,000 VND (từ 3 cashback cũ)
User A nhận thêm 2 cashback CONFIRMED mới (20,000 VND)
Complete batch → Chỉ 3 cashback cũ thành PAID ✓
                 2 cashback mới vẫn CONFIRMED ✓
                 3 cashback có paid_batch_id = batch.id ✓
```

---

## ⚠️ Lưu ý quan trọng

1. **Backward compatibility**: Cashback cũ đã được PAID trước đây sẽ có `paid_batch_id = NULL`, điều này hoàn toàn OK.

2. **Không cần migrate dữ liệu cũ**: Chỉ cashback mới từ giờ mới có `paid_batch_id`.

3. **Query performance**: Đã thêm index cho `paid_batch_id`.

---

## 🤔 Câu hỏi cho bạn review

1. Bạn có muốn thêm API endpoint để truy vết cashback theo batchCode không?

2. Có cần migrate dữ liệu cũ (mapping paid_batch_id cho các cashback đã PAID) không?

3. Có cần thêm validation để đảm bảo `amount` trong `batch_transfer_item` khớp với tổng `cashback_amount` của user không?
