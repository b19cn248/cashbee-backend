# Batch Transfer Improvements - Frontend Integration Guide

> **Version**: 1.0
> **Date**: 2025-12-23
> **Author**: CashBee Backend Team

## Tổng quan

Tài liệu này mô tả các cải tiến đã được thực hiện cho tính năng **Complete Batch Transfer**. Frontend cần cập nhật UI để phản ánh các thay đổi về trạng thái, thông tin audit trail, và xử lý lỗi chi tiết hơn.

---

## 1. Thay đổi API Response

### 1.1. Endpoint: Complete Batch Transfer

```
POST /api/admin/batch-transfer/{batchCode}/complete
```

#### Response trước đây:
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251223_001",
    "status": "COMPLETED",
    "message": "Batch completed. 25 success, 0 failed."
  }
}
```

#### Response mới (đã cập nhật):
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251223_001",
    "status": "COMPLETED",
    "successCount": 25,
    "failedCount": 0,
    "message": "Batch completed. 25 success, 0 failed."
  }
}
```

#### Các fields mới:
| Field | Type | Mô tả |
|-------|------|-------|
| `successCount` | Integer | Số lượng user được chuyển tiền thành công |
| `failedCount` | Integer | Số lượng user chuyển tiền thất bại |

---

## 2. Batch Status - State Machine mới

### 2.1. Các trạng thái của Batch Export

```
┌─────────────────────────────────────────────────────────────────┐
│                      BATCH STATUS FLOW                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│    ┌─────────┐                                                  │
│    │ PENDING │  ← Batch vừa được tạo, chờ admin complete        │
│    └────┬────┘                                                  │
│         │                                                       │
│         │ Admin gọi POST /{batchCode}/complete                  │
│         ▼                                                       │
│    ┌────────────┐                                               │
│    │ PROCESSING │  ← Đang xử lý (ngăn double-click)             │
│    └─────┬──────┘                                               │
│          │                                                      │
│          ├──────────────────┬────────────────────┐              │
│          ▼                  ▼                    ▼              │
│    ┌───────────┐    ┌───────────────┐    ┌────────┐             │
│    │ COMPLETED │    │PARTIAL_FAILED │    │ FAILED │             │
│    └───────────┘    └───────────────┘    └────────┘             │
│    100% thành công   Một số thất bại     100% thất bại          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2. Chi tiết các trạng thái

| Status | Mô tả | Màu đề xuất | Icon đề xuất |
|--------|-------|-------------|--------------|
| `PENDING` | Batch đã tạo, chờ admin hoàn tất chuyển tiền qua ngân hàng rồi confirm | 🟡 Yellow | ⏳ Clock |
| `PROCESSING` | Đang xử lý, không cho phép thao tác | 🔵 Blue | ⚙️ Spinner |
| `COMPLETED` | Tất cả user đã được chuyển tiền thành công | 🟢 Green | ✅ Check |
| `PARTIAL_FAILED` | Một số user thất bại, một số thành công | 🟠 Orange | ⚠️ Warning |
| `FAILED` | Tất cả user đều thất bại | 🔴 Red | ❌ Error |

---

## 3. Batch Item Status - Trạng thái từng User

### 3.1. Các trạng thái của Batch Item

```
┌─────────────────────────────────────────────────────────────────┐
│                    BATCH ITEM STATUS FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│    ┌─────────┐                                                  │
│    │ PENDING │  ← User trong batch, chờ xử lý                   │
│    └────┬────┘                                                  │
│         │                                                       │
│         │ Batch bắt đầu processing                              │
│         ▼                                                       │
│    ┌────────────┐                                               │
│    │ PROCESSING │  ← Đang trừ tiền cho user này                 │
│    └─────┬──────┘                                               │
│          │                                                      │
│          ├────────────────────────┐                             │
│          ▼                        ▼                             │
│    ┌───────────┐           ┌────────┐                           │
│    │ COMPLETED │           │ FAILED │                           │
│    └───────────┘           └────────┘                           │
│    Trừ tiền OK             Có lỗi xảy ra                        │
│                            (xem errorMessage)                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2. Chi tiết fields mới trong Batch Item

| Field | Type | Mô tả |
|-------|------|-------|
| `status` | String | PENDING / PROCESSING / COMPLETED / FAILED |
| `errorMessage` | String (nullable) | Lý do thất bại (nếu status = FAILED) |
| `actualAmountDeducted` | BigDecimal (nullable) | Số tiền thực tế đã trừ (có thể khác `amount` nếu balance đã thay đổi) |
| `completedAt` | DateTime (nullable) | Thời điểm hoàn thành xử lý item này |

---

## 4. Batch Export - Fields mới cho Audit Trail

### 4.1. Các fields mới trong Batch Export Response

Khi gọi API lấy chi tiết batch hoặc history, response sẽ có thêm các fields:

```json
{
  "batchCode": "BATCH_20251223_001",
  "fileName": "BATCH_20251223_001.xls",
  "totalUsers": 25,
  "totalAmount": 15500000,
  "status": "COMPLETED",
  "exportType": "MANUAL",
  "exportedBy": 1,
  "exportedAt": "2025-12-23T10:00:00",

  // === FIELDS MỚI ===
  "completedBy": 1,
  "completedAt": "2025-12-23T14:30:00",
  "successCount": 25,
  "failedCount": 0
}
```

### 4.2. Chi tiết fields mới

| Field | Type | Mô tả |
|-------|------|-------|
| `completedBy` | Long (nullable) | ID của admin đã complete batch |
| `completedAt` | DateTime (nullable) | Thời điểm complete batch |
| `successCount` | Integer | Số user chuyển tiền thành công |
| `failedCount` | Integer | Số user chuyển tiền thất bại |

---

## 5. Error Handling - Xử lý lỗi

### 5.1. Các lỗi có thể xảy ra khi Complete Batch

| Error Code | HTTP Status | Mô tả | UI Action |
|------------|-------------|-------|-----------|
| `BATCH_NOT_FOUND` | 404 | Batch không tồn tại | Hiển thị thông báo lỗi, reload danh sách |
| `BATCH_PROCESSING` | 400 | Batch đang được xử lý bởi request khác | Disable nút, hiển thị "Đang xử lý..." |
| `BATCH_ALREADY_COMPLETED` | 400 | Batch đã hoàn thành trước đó | Hiển thị thông báo, reload để xem kết quả |
| `NO_PENDING_ITEMS` | 400 | Không có item nào cần xử lý | Hiển thị thông báo |

### 5.2. Ví dụ Error Response

```json
{
  "success": false,
  "error": {
    "code": "BATCH_PROCESSING",
    "message": "Batch BATCH_20251223_001 is currently being processed by another request"
  }
}
```

---

## 6. UI/UX Recommendations

### 6.1. Màn hình Batch History (Danh sách)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         BATCH TRANSFER HISTORY                              │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Batch Code          │ Users │ Amount      │ Status         │ Action  │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │ BATCH_20251223_003  │ 30    │ 20,000,000đ │ 🟡 PENDING     │[Complete]│ │
│  │ BATCH_20251223_002  │ 25    │ 15,500,000đ │ 🔵 PROCESSING  │ Loading │  │
│  │ BATCH_20251223_001  │ 20    │ 12,000,000đ │ 🟢 COMPLETED   │ [View]  │  │
│  │ BATCH_20251222_005  │ 15    │  8,500,000đ │ 🟠 PARTIAL     │ [View]  │  │
│  │ BATCH_20251222_004  │ 10    │  5,000,000đ │ 🔴 FAILED      │ [Retry] │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 6.2. Màn hình Chi tiết Batch (sau khi Complete)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     BATCH DETAILS: BATCH_20251223_001                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         BATCH INFORMATION                           │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Status:        🟢 COMPLETED                                        │   │
│  │  Created:       2025-12-23 10:00:00 by Admin (ID: 1)                │   │
│  │  Completed:     2025-12-23 14:30:00 by Admin (ID: 1)    ← MỚI       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          STATISTICS                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │   │
│  │    │    TOTAL    │   │   SUCCESS   │   │   FAILED    │              │   │
│  │    │     25      │   │  🟢  25     │   │  🔴   0     │   ← MỚI      │   │
│  │    │    users    │   │   100%      │   │    0%       │              │   │
│  │    └─────────────┘   └─────────────┘   └─────────────┘              │   │
│  │                                                                     │   │
│  │    Total Amount: 15,500,000 VND                                     │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 6.3. Màn hình khi có Partial Failed

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     BATCH DETAILS: BATCH_20251223_002                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Status: 🟠 PARTIAL_FAILED                                                 │
│  Success: 23/25 (92%)   |   Failed: 2/25 (8%)                              │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       FAILED ITEMS                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  User ID  │ Amount     │ Error Message                              │   │
│  │  ─────────┼────────────┼──────────────────────────────────────────  │   │
│  │  12345    │ 500,000đ   │ Wallet not found: 999                      │   │
│  │  67890    │ 300,000đ   │ Insufficient balance                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
│  [Download Failed List]   [Retry Failed Items]                             │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 6.4. Xử lý Double-Click Prevention

```typescript
// Pseudo-code cho FE

const handleCompleteBatch = async (batchCode: string) => {
  // 1. Disable button ngay lập tức
  setIsProcessing(true);
  setButtonText("Đang xử lý...");

  try {
    const response = await api.post(`/batch-transfer/${batchCode}/complete`);

    if (response.data.success) {
      const { status, successCount, failedCount } = response.data.data;

      // 2. Hiển thị kết quả phù hợp
      if (status === 'COMPLETED') {
        showSuccess(`Hoàn tất! ${successCount} user đã được chuyển tiền.`);
      } else if (status === 'PARTIAL_FAILED') {
        showWarning(`${successCount} thành công, ${failedCount} thất bại.`);
      } else if (status === 'FAILED') {
        showError(`Thất bại! ${failedCount} user không thể chuyển tiền.`);
      }

      // 3. Reload danh sách
      refreshBatchList();
    }
  } catch (error) {
    // 4. Xử lý lỗi cụ thể
    if (error.code === 'BATCH_PROCESSING') {
      showInfo("Batch đang được xử lý, vui lòng chờ...");
    } else if (error.code === 'BATCH_ALREADY_COMPLETED') {
      showInfo("Batch đã hoàn thành trước đó.");
      refreshBatchList();
    } else {
      showError(error.message);
    }
  } finally {
    setIsProcessing(false);
    setButtonText("Complete");
  }
};
```

---

## 7. Flow Diagram - Luồng hoàn chỉnh

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE BATCH TRANSFER FLOW                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   ADMIN     │     │   FRONTEND  │     │   BACKEND   │     │  DATABASE   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │  1. Click         │                   │                   │
       │  "Complete"       │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │                   │  2. POST          │                   │
       │                   │  /complete        │                   │
       │                   │──────────────────>│                   │
       │                   │                   │                   │
       │                   │                   │  3. Lock batch    │
       │                   │                   │  (PESSIMISTIC)    │
       │                   │                   │──────────────────>│
       │                   │                   │                   │
       │                   │                   │  4. Check status  │
       │                   │                   │<──────────────────│
       │                   │                   │                   │
       │                   │                   │  5. Update status │
       │                   │                   │  → PROCESSING     │
       │                   │                   │──────────────────>│
       │                   │                   │                   │
       │                   │                   │  6. Process       │
       │                   │                   │  each item...     │
       │                   │                   │<────────────────>│
       │                   │                   │                   │
       │                   │                   │  7. Update status │
       │                   │                   │  → COMPLETED/     │
       │                   │                   │    PARTIAL_FAILED/│
       │                   │                   │    FAILED         │
       │                   │                   │──────────────────>│
       │                   │                   │                   │
       │                   │  8. Response      │                   │
       │                   │  {status,         │                   │
       │                   │   successCount,   │                   │
       │                   │   failedCount}    │                   │
       │                   │<──────────────────│                   │
       │                   │                   │                   │
       │  9. Show result   │                   │                   │
       │  based on status  │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │
```

---

## 8. API Endpoints Summary

| Method | Endpoint | Mô tả | Response có thay đổi? |
|--------|----------|-------|----------------------|
| POST | `/api/admin/batch-transfer/export` | Tạo batch mới | ❌ Không |
| GET | `/api/admin/batch-transfer/download` | Tải file Excel | ❌ Không |
| POST | `/api/admin/batch-transfer/{batchCode}/complete` | Complete batch | ✅ **CÓ** (thêm successCount, failedCount) |
| GET | `/api/admin/batch-transfer/history` | Lịch sử batch | ✅ **CÓ** (thêm completedBy, completedAt, successCount, failedCount) |
| GET | `/api/admin/batch-transfer/{batchCode}/cashbacks` | Danh sách cashbacks | ❌ Không |

---

## 9. Testing Checklist

Frontend cần test các scenario sau:

- [ ] **Happy Path**: Complete batch thành công (100% success)
- [ ] **Partial Failed**: Complete batch với một số items thất bại
- [ ] **All Failed**: Complete batch với tất cả items thất bại
- [ ] **Double Click**: Click nút Complete 2 lần liên tiếp
- [ ] **Concurrent Request**: 2 admin cùng complete 1 batch
- [ ] **Already Completed**: Complete batch đã hoàn thành trước đó
- [ ] **Invalid Batch**: Complete batch không tồn tại
- [ ] **Display Success/Failed Count**: Hiển thị đúng số lượng
- [ ] **Display Audit Trail**: Hiển thị completedBy, completedAt
- [ ] **Error Message Display**: Hiển thị errorMessage cho failed items

---

## 10. Migration Notes

### 10.1. Backward Compatibility

- API response vẫn giữ các fields cũ
- Chỉ thêm fields mới (`successCount`, `failedCount`, `completedBy`, `completedAt`)
- Frontend cũ vẫn hoạt động, nhưng không hiển thị thông tin mới

### 10.2. Database Migration

Backend đã thêm migration `037-add-batch-transfer-improvements.xml`:
- `batch_transfer_export`: Thêm `completed_by`, `completed_at`, `success_count`, `failed_count`
- `batch_transfer_item`: Thêm `error_message`, `actual_amount_deducted`

---

## 11. Liên hệ

Nếu có câu hỏi hoặc cần hỗ trợ, vui lòng liên hệ Backend team.
