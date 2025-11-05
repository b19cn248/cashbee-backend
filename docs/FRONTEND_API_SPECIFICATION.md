# 📱 FRONTEND API SPECIFICATION: Import CSV Shopee & Cashback

**Version**: 1.0
**Date**: 2025-11-03
**Status**: Ready for Frontend Development

---

## 📋 MỤC LỤC

1. [Tổng quan chức năng](#tổng-quan-chức-năng)
2. [User Stories](#user-stories)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Examples](#requestresponse-examples)
5. [Mock Data cho Development](#mock-data-cho-development)
6. [UI/UX Requirements](#uiux-requirements)
7. [State Management](#state-management)
8. [Error Handling](#error-handling)
9. [Testing Scenarios](#testing-scenarios)
10. [FAQs](#faqs)

---

## 🎯 TỔNG QUAN CHỨC NĂNG

### **Mô tả ngắn gọn**
Admin có thể upload file CSV từ Shopee Affiliate để import orders và tự động tính cashback cho users.

### **Actors**
- **Admin**: Upload CSV, xem kết quả import
- **User**: Nhận cashback tự động vào ví

### **Flow cơ bản**
```
1. Admin download CSV từ Shopee Affiliate Portal
2. Admin login vào CashBee Admin Panel
3. Admin navigate đến trang "Import Orders"
4. Admin select file CSV và click "Upload"
5. System xử lý file (có thể mất vài giây với file lớn)
6. System hiển thị kết quả: success, failed, skipped
7. Admin xem chi tiết errors (nếu có)
8. Users tự động nhận cashback vào ví
```

---

## 📖 USER STORIES

### **US-001: Admin Upload CSV**
```
AS AN Admin
I WANT TO upload Shopee CSV file
SO THAT orders are imported and users receive cashback automatically

ACCEPTANCE CRITERIA:
✅ Chỉ accept file .csv
✅ Hiển thị progress khi uploading
✅ Hiển thị kết quả chi tiết sau khi xong
✅ Có thể download error report
✅ Hiển thị thời gian xử lý
```

### **US-002: Admin View Import History**
```
AS AN Admin
I WANT TO view history of imports
SO THAT I can track what has been imported

ACCEPTANCE CRITERIA:
✅ List tất cả imports với status
✅ Filter by date, status, platform
✅ Search by filename
✅ Click vào item để xem detail
✅ Pagination (20 items/page)
```

### **US-003: Admin View Import Details**
```
AS AN Admin
I WANT TO view details of an import
SO THAT I can see what went wrong

ACCEPTANCE CRITERIA:
✅ Hiển thị statistics (success/failed/skipped)
✅ Hiển thị danh sách errors
✅ Export errors to CSV
✅ Retry failed orders (future)
```

### **US-004: User View Cashback**
```
AS A User
I WANT TO see my cashback in wallet
SO THAT I know how much I earned

ACCEPTANCE CRITERIA:
✅ Hiển thị balance và pending_balance
✅ Hiển thị transaction history
✅ Filter by type (CASHBACK, WITHDRAW, etc.)
✅ Show cashback source (order ID)
```

---

## 🔌 API ENDPOINTS

### **BASE URL**
```
Production: https://api.cashbee.vn/api/v1
Staging: https://staging-api.cashbee.vn/api/v1
Development: http://localhost:8080/api/v1
```

### **Authentication**
Tất cả endpoints cần authentication header:
```
Authorization: Bearer {access_token}
```

---

## 📤 1. UPLOAD CSV FILE

### **POST** `/admin/import/orders`

Upload file CSV từ Shopee Affiliate để import orders.

#### **Headers**
```http
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

#### **Request Body (Form Data)**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `file` | File | ✅ | - | CSV file từ Shopee (max 50MB) |
| `platformCode` | String | ❌ | "shopee" | Platform code (shopee, lazada, tiki) |
| `importedBy` | Long | ✅ | - | Admin user ID |
| `skipDuplicates` | Boolean | ❌ | true | Bỏ qua orders đã import trước đó |
| `autoMatch` | Boolean | ❌ | true | Tự động match với clicks |

#### **Request Example (JavaScript)**

```javascript
const formData = new FormData();
formData.append('file', selectedFile);
formData.append('platformCode', 'shopee');
formData.append('importedBy', currentUserId);
formData.append('skipDuplicates', true);
formData.append('autoMatch', true);

const response = await fetch(`${API_BASE_URL}/admin/import/orders`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
  },
  body: formData
});

const result = await response.json();
```

#### **Response 200 OK**

```json
{
  "success": true,
  "code": "IMPORT_SUCCESS",
  "message": "Successfully imported 150 orders. 145 orders matched with clicks.",
  "data": {
    "batchId": 1234,
    "platformName": "Shopee",
    "platformCode": "shopee",
    "fileName": "AffiliateCommissionReport202511030813.csv",
    "status": "COMPLETED",
    "totalRows": 200,
    "successCount": 150,
    "failedCount": 5,
    "skippedCount": 45,
    "matchedCount": 145,
    "cashbackCreatedCount": 150,
    "cashbackPaidCount": 120,
    "totalCashbackAmount": 1250000.00,
    "successRate": 75.0,
    "startedAt": "2025-11-03T10:15:30",
    "completedAt": "2025-11-03T10:16:45",
    "durationSeconds": 75,
    "importedBy": 1,
    "errors": [
      {
        "rowNumber": 15,
        "orderId": "251030XXXX",
        "error": "No tracking code (Sub_id1 is empty)",
        "rawData": "251030XXXX,Hoàn thành,..."
      },
      {
        "rowNumber": 23,
        "orderId": "251030YYYY",
        "error": "Invalid tracking code format: INVALID123",
        "rawData": "251030YYYY,Đang chờ xử lý,..."
      }
    ]
  },
  "timestamp": "2025-11-03T10:16:45"
}
```

#### **Response 400 Bad Request**

```json
{
  "success": false,
  "code": "INVALID_FILE",
  "message": "Only CSV files are supported",
  "data": null,
  "timestamp": "2025-11-03T10:15:30"
}
```

#### **Response 401 Unauthorized**

```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "data": null,
  "timestamp": "2025-11-03T10:15:30"
}
```

#### **Response 403 Forbidden**

```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "Admin role required",
  "data": null,
  "timestamp": "2025-11-03T10:15:30"
}
```

#### **Possible Status Values**

| Status | Description |
|--------|-------------|
| `PROCESSING` | Đang xử lý (có thể mất vài giây) |
| `COMPLETED` | Hoàn thành, tất cả thành công |
| `PARTIAL` | Hoàn thành nhưng có một số lỗi |
| `FAILED` | Thất bại hoàn toàn |

---

## 📥 2. GET IMPORT BATCH DETAILS

### **GET** `/admin/import/batches/{batchId}`

Lấy chi tiết của một batch import.

#### **Headers**
```http
Authorization: Bearer {token}
```

#### **Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `batchId` | Long | ✅ | ID của batch cần lấy |

#### **Request Example**

```javascript
const batchId = 1234;
const response = await fetch(
  `${API_BASE_URL}/admin/import/batches/${batchId}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const result = await response.json();
```

#### **Response 200 OK**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Batch details retrieved",
  "data": {
    "id": 1234,
    "batchCode": "BATCH_20251103_101530",
    "fileName": "AffiliateCommissionReport202511030813.csv",
    "fileSize": 1048576,
    "platformId": 1,
    "platformName": "Shopee",
    "platformCode": "shopee",
    "status": "COMPLETED",
    "totalRows": 200,
    "successCount": 150,
    "failedCount": 5,
    "skippedCount": 45,
    "matchedCount": 145,
    "cashbackCreatedCount": 150,
    "cashbackPaidCount": 120,
    "totalCashbackAmount": 1250000.00,
    "successRate": 75.0,
    "importedAt": "2025-11-03T10:15:30",
    "completedAt": "2025-11-03T10:16:45",
    "durationSeconds": 75,
    "importedBy": {
      "id": 1,
      "username": "admin01",
      "fullName": "Nguyen Van Admin"
    },
    "errors": [
      {
        "rowNumber": 15,
        "orderId": "251030XXXX",
        "error": "No tracking code (Sub_id1 is empty)"
      }
    ]
  },
  "timestamp": "2025-11-03T10:20:00"
}
```

#### **Response 404 Not Found**

```json
{
  "success": false,
  "code": "BATCH_NOT_FOUND",
  "message": "Import batch not found with ID: 1234",
  "data": null,
  "timestamp": "2025-11-03T10:20:00"
}
```

---

## 📋 3. GET IMPORT BATCHES LIST

### **GET** `/admin/import/batches`

Lấy danh sách các batch đã import (có pagination).

#### **Headers**
```http
Authorization: Bearer {token}
```

#### **Query Parameters**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | ❌ | 0 | Page number (0-indexed) |
| `size` | Integer | ❌ | 20 | Items per page |
| `sort` | String | ❌ | "importedAt,desc" | Sort field and direction |
| `status` | String | ❌ | - | Filter by status |
| `platformCode` | String | ❌ | - | Filter by platform |
| `fromDate` | String | ❌ | - | Filter from date (ISO 8601) |
| `toDate` | String | ❌ | - | Filter to date (ISO 8601) |

#### **Request Example**

```javascript
const params = new URLSearchParams({
  page: 0,
  size: 20,
  sort: 'importedAt,desc',
  status: 'COMPLETED',
  platformCode: 'shopee'
});

const response = await fetch(
  `${API_BASE_URL}/admin/import/batches?${params}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const result = await response.json();
```

#### **Response 200 OK**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Import batches retrieved",
  "data": {
    "content": [
      {
        "id": 1234,
        "fileName": "AffiliateCommissionReport202511030813.csv",
        "platformCode": "shopee",
        "platformName": "Shopee",
        "status": "COMPLETED",
        "totalRows": 200,
        "successCount": 150,
        "failedCount": 5,
        "skippedCount": 45,
        "successRate": 75.0,
        "totalCashbackAmount": 1250000.00,
        "importedAt": "2025-11-03T10:15:30",
        "completedAt": "2025-11-03T10:16:45",
        "importedBy": {
          "id": 1,
          "username": "admin01"
        }
      },
      {
        "id": 1233,
        "fileName": "AffiliateCommissionReport202511020813.csv",
        "platformCode": "shopee",
        "platformName": "Shopee",
        "status": "COMPLETED",
        "totalRows": 150,
        "successCount": 145,
        "failedCount": 2,
        "skippedCount": 3,
        "successRate": 96.67,
        "totalCashbackAmount": 980000.00,
        "importedAt": "2025-11-02T09:30:15",
        "completedAt": "2025-11-02T09:31:20",
        "importedBy": {
          "id": 1,
          "username": "admin01"
        }
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 45,
    "totalPages": 3,
    "last": false,
    "size": 20,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "numberOfElements": 20,
    "first": true,
    "empty": false
  },
  "timestamp": "2025-11-03T10:20:00"
}
```

---

## 💰 4. GET USER WALLET

### **GET** `/users/{userId}/wallet`

Lấy thông tin ví của user (balance, pending_balance, transactions).

#### **Headers**
```http
Authorization: Bearer {token}
```

#### **Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | Long | ✅ | ID của user |

#### **Request Example**

```javascript
const userId = 123;
const response = await fetch(
  `${API_BASE_URL}/users/${userId}/wallet`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const result = await response.json();
```

#### **Response 200 OK**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Wallet retrieved successfully",
  "data": {
    "id": 456,
    "userId": 123,
    "balance": 1250000.00,
    "pendingBalance": 350000.00,
    "lockedBalance": 0.00,
    "totalEarned": 5680000.00,
    "totalWithdrawn": 4030000.00,
    "currency": "VND",
    "lastUpdated": "2025-11-03T10:16:45",
    "createdAt": "2025-01-15T08:30:00"
  },
  "timestamp": "2025-11-03T10:20:00"
}
```

---

## 📊 5. GET WALLET TRANSACTIONS

### **GET** `/users/{userId}/wallet/transactions`

Lấy lịch sử giao dịch ví của user.

#### **Headers**
```http
Authorization: Bearer {token}
```

#### **Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | Long | ✅ | ID của user |

#### **Query Parameters**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | ❌ | 0 | Page number |
| `size` | Integer | ❌ | 20 | Items per page |
| `type` | String | ❌ | - | Filter by type (CASHBACK, WITHDRAW, etc.) |
| `fromDate` | String | ❌ | - | From date (ISO 8601) |
| `toDate` | String | ❌ | - | To date (ISO 8601) |

#### **Request Example**

```javascript
const userId = 123;
const params = new URLSearchParams({
  page: 0,
  size: 20,
  type: 'CASHBACK'
});

const response = await fetch(
  `${API_BASE_URL}/users/${userId}/wallet/transactions?${params}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const result = await response.json();
```

#### **Response 200 OK**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Transactions retrieved",
  "data": {
    "content": [
      {
        "id": 7890,
        "transactionCode": "TXN_20251103_101645_001",
        "type": "CASHBACK",
        "amount": 7000.00,
        "balanceBefore": 1243000.00,
        "balanceAfter": 1250000.00,
        "status": "SUCCESS",
        "description": "Cashback from order #251030ABC",
        "referenceType": "ORDER_ID",
        "referenceId": "251030ABC",
        "createdAt": "2025-11-03T10:16:45"
      },
      {
        "id": 7889,
        "transactionCode": "TXN_20251103_093520_001",
        "type": "CASHBACK",
        "amount": 5500.00,
        "balanceBefore": 1237500.00,
        "balanceAfter": 1243000.00,
        "status": "SUCCESS",
        "description": "Cashback from order #251030XYZ",
        "referenceType": "ORDER_ID",
        "referenceId": "251030XYZ",
        "createdAt": "2025-11-03T09:35:20"
      }
    ],
    "totalElements": 156,
    "totalPages": 8,
    "size": 20,
    "number": 0
  },
  "timestamp": "2025-11-03T10:20:00"
}
```

#### **Transaction Types**

| Type | Description |
|------|-------------|
| `CASHBACK` | Tiền cashback từ orders |
| `WITHDRAW` | Rút tiền |
| `BONUS` | Tiền thưởng |
| `REFERRAL` | Hoa hồng giới thiệu |
| `REFUND` | Hoàn tiền |
| `ADJUSTMENT` | Điều chỉnh (admin) |

---

## 🎨 UI/UX REQUIREMENTS

### **1. Upload Page** (`/admin/imports/upload`)

#### **Layout**
```
┌─────────────────────────────────────────┐
│  📤 Import Shopee Orders                │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  📄 Drop CSV file here            │  │
│  │     or click to browse            │  │
│  │                                   │  │
│  │  Maximum file size: 50MB          │  │
│  │  Accepted format: .csv            │  │
│  └───────────────────────────────────┘  │
│                                         │
│  Selected file: [AffiliateComm...csv]  │
│                                         │
│  ⚙️ Options:                            │
│  ☑ Skip duplicate orders               │
│  ☑ Auto-match with clicks              │
│                                         │
│  [Cancel]  [Upload & Process]          │
│                                         │
└─────────────────────────────────────────┘
```

#### **States**

**Initial State:**
- Drop zone hiển thị "Drop CSV file here"
- Upload button disabled

**File Selected:**
- Hiển thị filename và size
- Upload button enabled
- Có button [x] để remove file

**Uploading:**
- Hiển thị progress bar: "Uploading... 45%"
- Disable tất cả controls
- Có button [Cancel] để hủy

**Processing:**
- Progress bar: "Processing orders... Please wait"
- Loading spinner
- Message: "This may take a few seconds for large files"

**Success:**
- Hiển thị summary card (xem phần Result Display)
- Button [View Details] [Upload Another]

**Error:**
- Alert box màu đỏ với error message
- Button [Try Again] [Cancel]

---

### **2. Result Display** (Modal/Card sau khi upload)

```
┌─────────────────────────────────────────────────┐
│  ✅ Import Completed                            │
├─────────────────────────────────────────────────┤
│                                                 │
│  File: AffiliateCommissionReport202511030813... │
│  Processing time: 75 seconds                    │
│  Imported at: 2025-11-03 10:16:45              │
│                                                 │
│  📊 Summary                                      │
│  ┌─────────────────────────────────────────┐   │
│  │  Total Rows:        200                 │   │
│  │  ✅ Success:         150  (75%)          │   │
│  │  ❌ Failed:          5    (2.5%)         │   │
│  │  ⏭️  Skipped:        45   (22.5%)        │   │
│  │  🔗 Matched:         145                 │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  💰 Cashback Summary                            │
│  ┌─────────────────────────────────────────┐   │
│  │  Created:        150 cashbacks          │   │
│  │  Paid:           120 cashbacks          │   │
│  │  Total Amount:   1,250,000 VND          │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ⚠️ Errors (5)                                  │
│  [Show Details]  [Download Error Report]       │
│                                                 │
│  [Close]  [View Full Details]                  │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

### **3. Import History Page** (`/admin/imports/history`)

#### **Layout**
```
┌──────────────────────────────────────────────────────┐
│  📋 Import History                                   │
├──────────────────────────────────────────────────────┤
│                                                      │
│  🔍 Filters:                                         │
│  Platform: [Shopee ▼]  Status: [All ▼]             │
│  Date Range: [2025-11-01] to [2025-11-03]          │
│  [Search filename...]              [Apply Filters]  │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │ Filename           Status   Orders  Cashback   │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 📄 Affiliate...csv  ✅ COMPLETED              │ │
│  │ 2025-11-03 10:15   200/150  1,250,000 VND     │ │
│  │ by admin01         [View Details]              │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 📄 Affiliate...csv  ⚠️ PARTIAL                │ │
│  │ 2025-11-02 09:30   150/145  980,000 VND       │ │
│  │ by admin01         [View Details]              │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 📄 Affiliate...csv  ❌ FAILED                  │ │
│  │ 2025-11-01 14:22   0/0      0 VND              │ │
│  │ by admin02         [View Details]              │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  Showing 1-20 of 45    [< Prev]  [Next >]          │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

### **4. Import Details Page** (`/admin/imports/{batchId}`)

```
┌──────────────────────────────────────────────────────┐
│  [← Back to History]                                 │
│                                                      │
│  📄 Import Details #1234                             │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ℹ️ Basic Information                                │
│  Filename:    AffiliateCommissionReport...csv       │
│  Platform:    Shopee                                │
│  Status:      ✅ COMPLETED                           │
│  Imported by: admin01 (Nguyen Van Admin)            │
│  Started:     2025-11-03 10:15:30                   │
│  Completed:   2025-11-03 10:16:45                   │
│  Duration:    75 seconds                            │
│                                                      │
│  📊 Statistics                                        │
│  ┌──────────────────────────────────────────────┐   │
│  │  Total Rows      200                         │   │
│  │  Success         150 (75.0%)  ████████░░     │   │
│  │  Failed          5   (2.5%)   ░              │   │
│  │  Skipped         45  (22.5%)  ██░            │   │
│  │  Matched         145                         │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  💰 Cashback Details                                 │
│  ┌──────────────────────────────────────────────┐   │
│  │  Cashback Created    150                     │   │
│  │  Cashback Paid       120                     │   │
│  │  Total Amount        1,250,000 VND           │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  ❌ Errors (5)                                       │
│  [Download CSV]                                     │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │ Row  Order ID      Error                       │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 15   251030XXXX    No tracking code            │ │
│  │ 23   251030YYYY    Invalid tracking code       │ │
│  │ 45   251030ZZZZ    User not found              │ │
│  │ 67   251030AAAA    No tracking code            │ │
│  │ 89   251030BBBB    Order already exists        │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

### **5. User Wallet Page** (`/users/{userId}/wallet`)

```
┌──────────────────────────────────────────────────────┐
│  💰 My Wallet                                        │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ┌────────────────┐  ┌────────────────┐            │
│  │ Available      │  │ Pending        │            │
│  │ 1,250,000 VND  │  │ 350,000 VND    │            │
│  │ [Withdraw]     │  │ ⏳ Processing   │            │
│  └────────────────┘  └────────────────┘            │
│                                                      │
│  ℹ️ Total Earned: 5,680,000 VND                     │
│     Total Withdrawn: 4,030,000 VND                  │
│                                                      │
│  📊 Transaction History                              │
│  Filter: [All Types ▼]  [Last 30 days ▼]           │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │ 2025-11-03 10:16                               │ │
│  │ ✅ Cashback from order #251030ABC              │ │
│  │ +7,000 VND                                     │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 2025-11-03 09:35                               │ │
│  │ ✅ Cashback from order #251030XYZ              │ │
│  │ +5,500 VND                                     │ │
│  ├────────────────────────────────────────────────┤ │
│  │ 2025-11-02 15:20                               │ │
│  │ 💸 Withdrawal to Momo                          │ │
│  │ -500,000 VND                                   │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  [Load More]                                        │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## 🎭 MOCK DATA CHO DEVELOPMENT

### **Mock API Server (JSON Server)**

Tạo file `db.json`:

```json
{
  "imports": [
    {
      "id": 1234,
      "fileName": "AffiliateCommissionReport202511030813.csv",
      "platformCode": "shopee",
      "platformName": "Shopee",
      "status": "COMPLETED",
      "totalRows": 200,
      "successCount": 150,
      "failedCount": 5,
      "skippedCount": 45,
      "matchedCount": 145,
      "cashbackCreatedCount": 150,
      "cashbackPaidCount": 120,
      "totalCashbackAmount": 1250000,
      "successRate": 75.0,
      "importedAt": "2025-11-03T10:15:30",
      "completedAt": "2025-11-03T10:16:45",
      "durationSeconds": 75,
      "importedBy": {
        "id": 1,
        "username": "admin01",
        "fullName": "Nguyen Van Admin"
      },
      "errors": [
        {
          "rowNumber": 15,
          "orderId": "251030XXXX",
          "error": "No tracking code (Sub_id1 is empty)",
          "rawData": "..."
        },
        {
          "rowNumber": 23,
          "orderId": "251030YYYY",
          "error": "Invalid tracking code format: INVALID123",
          "rawData": "..."
        }
      ]
    }
  ],
  "wallets": [
    {
      "id": 456,
      "userId": 123,
      "balance": 1250000,
      "pendingBalance": 350000,
      "lockedBalance": 0,
      "totalEarned": 5680000,
      "totalWithdrawn": 4030000,
      "currency": "VND",
      "lastUpdated": "2025-11-03T10:16:45",
      "createdAt": "2025-01-15T08:30:00"
    }
  ],
  "transactions": [
    {
      "id": 7890,
      "transactionCode": "TXN_20251103_101645_001",
      "type": "CASHBACK",
      "amount": 7000,
      "balanceBefore": 1243000,
      "balanceAfter": 1250000,
      "status": "SUCCESS",
      "description": "Cashback from order #251030ABC",
      "referenceType": "ORDER_ID",
      "referenceId": "251030ABC",
      "createdAt": "2025-11-03T10:16:45"
    }
  ]
}
```

### **Start Mock Server**

```bash
# Install json-server
npm install -g json-server

# Start server
json-server --watch db.json --port 8080
```

### **Mock Upload Response**

Để mock upload API, tạo file `mock-upload.js`:

```javascript
// mock-upload.js - Express server để mock upload
const express = require('express');
const multer = require('multer');
const app = express();
const upload = multer();

app.post('/api/admin/import/orders', upload.single('file'), (req, res) => {
  // Simulate processing time
  setTimeout(() => {
    res.json({
      success: true,
      code: 'IMPORT_SUCCESS',
      message: 'Successfully imported 150 orders',
      data: {
        batchId: Math.floor(Math.random() * 10000),
        platformName: 'Shopee',
        platformCode: 'shopee',
        fileName: req.file.originalname,
        status: 'COMPLETED',
        totalRows: 200,
        successCount: 150,
        failedCount: 5,
        skippedCount: 45,
        matchedCount: 145,
        cashbackCreatedCount: 150,
        cashbackPaidCount: 120,
        totalCashbackAmount: 1250000,
        successRate: 75.0,
        startedAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        durationSeconds: 75,
        importedBy: 1,
        errors: [
          {
            rowNumber: 15,
            orderId: '251030XXXX',
            error: 'No tracking code (Sub_id1 is empty)'
          }
        ]
      }
    });
  }, 3000); // 3 seconds delay
});

app.listen(8080, () => console.log('Mock server on http://localhost:8080'));
```

---

## 🧪 STATE MANAGEMENT

### **Redux/Zustand Store Structure**

```typescript
interface ImportState {
  // Upload state
  uploadState: 'idle' | 'uploading' | 'processing' | 'success' | 'error';
  uploadProgress: number; // 0-100
  currentBatch: ImportBatch | null;

  // History
  batches: ImportBatch[];
  batchesLoading: boolean;
  batchesError: string | null;
  pagination: {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };

  // Filters
  filters: {
    status: string | null;
    platformCode: string | null;
    fromDate: string | null;
    toDate: string | null;
  };
}

interface WalletState {
  wallet: Wallet | null;
  transactions: Transaction[];
  transactionsLoading: boolean;
  transactionsPagination: Pagination;
}
```

### **Actions**

```typescript
// Import actions
uploadCsvFile(file: File, options: UploadOptions): Promise<ImportBatch>
fetchImportBatches(filters: ImportFilters): Promise<PaginatedResponse<ImportBatch>>
fetchImportBatchDetails(batchId: number): Promise<ImportBatch>
setUploadState(state: UploadState): void
setFilters(filters: ImportFilters): void

// Wallet actions
fetchWallet(userId: number): Promise<Wallet>
fetchTransactions(userId: number, filters: TransactionFilters): Promise<PaginatedResponse<Transaction>>
```

---

## ⚠️ ERROR HANDLING

### **Error Codes**

| Code | HTTP Status | Description | User Message |
|------|-------------|-------------|--------------|
| `INVALID_FILE` | 400 | File không phải CSV | "Vui lòng chọn file CSV" |
| `FILE_TOO_LARGE` | 400 | File > 50MB | "File quá lớn. Tối đa 50MB" |
| `EMPTY_FILE` | 400 | File rỗng | "File CSV trống" |
| `PARSE_ERROR` | 400 | Không parse được CSV | "File CSV không đúng định dạng" |
| `UNAUTHORIZED` | 401 | Token không hợp lệ | "Vui lòng đăng nhập lại" |
| `FORBIDDEN` | 403 | Không có quyền admin | "Bạn không có quyền thực hiện" |
| `BATCH_NOT_FOUND` | 404 | Batch không tồn tại | "Không tìm thấy batch" |
| `SERVER_ERROR` | 500 | Lỗi server | "Lỗi hệ thống. Vui lòng thử lại" |

### **Error Display**

```jsx
// Toast notification
toast.error('Lỗi: File quá lớn. Tối đa 50MB');

// Inline error
<Alert severity="error">
  <AlertTitle>Upload Failed</AlertTitle>
  {errorMessage}
</Alert>

// Error list
<List>
  {errors.map(error => (
    <ListItem key={error.rowNumber}>
      <ListItemText
        primary={`Row ${error.rowNumber}: ${error.orderId}`}
        secondary={error.error}
      />
    </ListItem>
  ))}
</List>
```

---

## ✅ TESTING SCENARIOS

### **Scenario 1: Upload thành công**
1. Login as admin
2. Navigate to Import page
3. Select valid CSV file
4. Click Upload
5. See progress bar
6. See success message with statistics
7. Verify data in Import History

**Expected:**
- Status: COMPLETED
- Success count > 0
- Cashback amount > 0

### **Scenario 2: Upload file invalid**
1. Login as admin
2. Select .txt file (not CSV)
3. Try to upload
4. See error: "Only CSV files are supported"

**Expected:**
- Upload button disabled or error shown
- File not uploaded

### **Scenario 3: Upload với errors**
1. Upload CSV có một số orders lỗi
2. See status: PARTIAL
3. View error details
4. Download error report

**Expected:**
- Status: PARTIAL
- Failed count > 0
- Errors list visible

### **Scenario 4: View wallet balance**
1. Login as user
2. Navigate to Wallet page
3. See balance and pending balance
4. See transaction history
5. Filter by CASHBACK type
6. See only cashback transactions

**Expected:**
- Balance displayed correctly
- Transactions filtered
- Pagination works

### **Scenario 5: Large file upload**
1. Upload file > 1000 rows
2. See processing indicator
3. Wait for completion
4. See results

**Expected:**
- Processing takes longer
- UI stays responsive
- Results displayed correctly

---

## 🔒 AUTHENTICATION & AUTHORIZATION

### **Token Management**

```typescript
// Get token from localStorage/sessionStorage
const accessToken = localStorage.getItem('access_token');

// Add to all API requests
headers: {
  'Authorization': `Bearer ${accessToken}`
}

// Handle 401 Unauthorized
if (response.status === 401) {
  // Redirect to login
  window.location.href = '/login';
}

// Handle 403 Forbidden
if (response.status === 403) {
  // Show "No permission" message
  toast.error('You do not have permission');
}
```

### **Role-based Access**

| Page | Required Role |
|------|---------------|
| Upload CSV | ADMIN |
| Import History | ADMIN |
| Import Details | ADMIN |
| My Wallet | USER (own wallet only) |
| User Wallet (admin view) | ADMIN |

---

## 📱 RESPONSIVE DESIGN

### **Breakpoints**

```css
/* Mobile */
@media (max-width: 640px) {
  /* Stack cards vertically */
  /* Hide less important columns */
  /* Collapse filters */
}

/* Tablet */
@media (min-width: 641px) and (max-width: 1024px) {
  /* 2 columns for cards */
  /* Show filters in collapsible panel */
}

/* Desktop */
@media (min-width: 1025px) {
  /* Full layout */
  /* All columns visible */
}
```

### **Mobile Considerations**

- Upload via camera (for testing)
- Swipe gestures for pagination
- Bottom sheet for filters
- Collapsible error list

---

## 🎯 PERFORMANCE OPTIMIZATION

### **Best Practices**

1. **Lazy Loading**
   - Load import history on demand
   - Virtual scrolling for large lists

2. **Caching**
   - Cache import batches list (5 minutes)
   - Cache wallet balance (1 minute)

3. **Pagination**
   - Default 20 items per page
   - Infinite scroll or Load More button

4. **Debouncing**
   - Search input: 300ms debounce
   - Filter changes: 500ms debounce

5. **Optimistic Updates**
   - Show upload success immediately
   - Refresh data in background

---

## 🔗 API CLIENT EXAMPLE

### **TypeScript/Axios Example**

```typescript
// api/import.service.ts
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL;

export class ImportService {

  async uploadCsv(
    file: File,
    options: UploadOptions
  ): Promise<ImportBatch> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('platformCode', options.platformCode || 'shopee');
    formData.append('importedBy', options.importedBy.toString());
    formData.append('skipDuplicates', options.skipDuplicates.toString());
    formData.append('autoMatch', options.autoMatch.toString());

    const response = await axios.post(
      `${API_BASE_URL}/admin/import/orders`,
      formData,
      {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          this.onProgress?.(percentCompleted);
        }
      }
    );

    return response.data.data;
  }

  async fetchBatches(
    filters: ImportFilters,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<ImportBatch>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort: 'importedAt,desc',
      ...filters
    });

    const response = await axios.get(
      `${API_BASE_URL}/admin/import/batches?${params}`,
      {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      }
    );

    return response.data.data;
  }

  async fetchBatchDetails(batchId: number): Promise<ImportBatch> {
    const response = await axios.get(
      `${API_BASE_URL}/admin/import/batches/${batchId}`,
      {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      }
    );

    return response.data.data;
  }

  private getToken(): string {
    return localStorage.getItem('access_token') || '';
  }
}
```

---

## 💡 FAQ

### **Q: File CSV cần có format như thế nào?**
**A:** File CSV từ Shopee Affiliate Portal, có 43 cột, encoding UTF-8 with BOM. FE không cần validate format, BE sẽ xử lý.

### **Q: Upload file lớn (5000+ rows) có bị timeout không?**
**A:** Không. API có timeout 10 phút. Nhưng nên show loading indicator rõ ràng.

### **Q: Có thể cancel upload đang xử lý không?**
**A:** Hiện tại chưa support. Future feature.

### **Q: Pending balance là gì?**
**A:** Là tiền cashback từ orders chưa hoàn thành. Khi order hoàn thành, sẽ chuyển sang available balance.

### **Q: User có thể xem được order nào sinh ra cashback không?**
**A:** Có, trong transaction detail có field `referenceId` chứa order ID.

### **Q: Admin có thể retry failed orders không?**
**A:** Hiện tại chưa. Admin cần fix data và re-upload file.

### **Q: Có giới hạn số lần upload/ngày không?**
**A:** Không có giới hạn. Nhưng recommend 1 lần/ngày (file report hàng ngày).

### **Q: File CSV có thể chứa orders đã import không?**
**A:** Có. Nếu `skipDuplicates=true`, system sẽ skip. Nếu `false`, sẽ update.

---

## 📚 ADDITIONAL RESOURCES

### **Figma Design** (Mock)
- [Import Upload Page](link-to-figma)
- [Import History](link-to-figma)
- [Wallet Page](link-to-figma)

### **Postman Collection**
```json
{
  "info": {
    "name": "CashBee Import API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Upload CSV",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/admin/import/orders",
        "body": {
          "mode": "formdata",
          "formdata": [
            {"key": "file", "type": "file"},
            {"key": "platformCode", "value": "shopee"},
            {"key": "importedBy", "value": "1"}
          ]
        }
      }
    }
  ]
}
```

### **Sample CSV File**
Download: [AffiliateCommissionReport_Sample.csv](link-to-sample)

---

## 📞 SUPPORT

Nếu có thắc mắc trong quá trình development:

1. **Check documentation** này trước
2. **Mock API** để test mà không cần BE
3. **Contact Backend Team** để clarify business logic
4. **Create ticket** trong Jira/GitHub Issues

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Maintained by**: Backend Team
**Frontend Contact**: [Your Name]

---

**Happy Coding! 🚀**
