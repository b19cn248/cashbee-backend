# 📖 FRONTEND GUIDE: CSV Import & Cashback System

**Date**: 2025-11-03
**Status**: ✅ **PRODUCTION READY**
**Target Audience**: Frontend Developers

---

## 🎯 MỤC ĐÍCH

Tài liệu này giúp Frontend developer hiểu và sử dụng chức năng **CSV Import và Cashback** mà không cần Backend developer hỗ trợ.

**Chức năng chính**:
1. **Admin** upload CSV từ Shopee → Tạo orders
2. **System** tự động tính cashback và cộng tiền cho user
3. **User** xem số dư trong wallet

---

## 📋 MỤC LỤC

1. [Tổng quan Flow](#tổng-quan-flow)
2. [API Endpoints](#api-endpoints)
3. [Request/Response Examples](#requestresponse-examples)
4. [Error Handling](#error-handling)
5. [Frontend Implementation Guide](#frontend-implementation-guide)
6. [Testing Guide](#testing-guide)
7. [FAQ](#faq)

---

## 🔄 TỔNG QUAN FLOW

### **Flow đầy đủ từ đầu đến cuối**:

```
Step 1: Admin Upload CSV
   ↓
Step 2: Backend Parse CSV
   ↓
Step 3: Create AffiliateOrder
   ↓
Step 4: Calculate Cashback (NEW!)
   - Lấy policy rate (70%)
   - Tính: cashback = commission × 70%
   ↓
Step 5: Add to Wallet (NEW!)
   - If order completed → balance += cashback
   - If order pending → pending_balance += cashback
   ↓
Step 6: Return Result
   - Import statistics
   - Cashback created count
   ↓
Step 7: User Check Wallet
   - GET /api/wallets/user/{userId}
   - Thấy balance hoặc pending_balance tăng
```

### **Ví dụ cụ thể**:

**Input**: Admin upload CSV với 1 order
```csv
Order ID: 251030ABC
Status: Hoàn thành
Commission: 100,000 VND
Tracking code: CB1_1_xxx
```

**Process**:
1. Create order: order_id = 251030ABC
2. Calculate cashback: 100,000 × 70% = **70,000 VND**
3. Add to wallet: balance += 70,000 VND

**Result**: User balance = **70,000 VND** 💰

---

## 🚀 API ENDPOINTS

### **1. Upload CSV (Admin Only)**

#### **Endpoint**:
```
POST /api/admin/import/orders
```

#### **Headers**:
```http
Content-Type: multipart/form-data
Authorization: Bearer {admin_jwt_token}
```

#### **Request Body** (Form Data):
```
file: (File) CSV file from Shopee
platformCode: (String) "shopee" (default)
importedBy: (Long) Admin user ID
skipDuplicates: (Boolean) true (default)
autoMatch: (Boolean) true (default)
```

#### **cURL Example**:
```bash
curl -X POST http://localhost:8080/api/admin/import/orders \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "file=@AffiliateCommissionReport.csv" \
  -F "platformCode=shopee" \
  -F "importedBy=1" \
  -F "skipDuplicates=true" \
  -F "autoMatch=true"
```

#### **JavaScript Example** (Axios):
```javascript
const uploadCSV = async (file, adminUserId) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('platformCode', 'shopee');
  formData.append('importedBy', adminUserId);
  formData.append('skipDuplicates', 'true');
  formData.append('autoMatch', 'true');

  try {
    const response = await axios.post(
      '/api/admin/import/orders',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${getAdminToken()}`
        }
      }
    );

    return response.data;
  } catch (error) {
    console.error('Upload failed:', error.response.data);
    throw error;
  }
};
```

#### **Response** (Success - Status 200):
```json
{
  "success": true,
  "data": {
    "batchId": 1,
    "platformName": "Shopee",
    "platformCode": "shopee",
    "fileName": "AffiliateCommissionReport202510300813.csv",
    "status": "COMPLETED",
    "totalRows": 182,
    "successCount": 182,
    "failedCount": 0,
    "skippedCount": 0,
    "matchedCount": 0,
    "successRate": 100.0,
    "errors": [],
    "startedAt": "2025-11-03T22:50:00",
    "completedAt": "2025-11-03T22:50:15",
    "durationSeconds": 15,
    "importedBy": 1,
    "message": "Successfully imported 182 orders. 0 orders matched with clicks."
  },
  "message": "Successfully imported 182 orders. 0 orders matched with clicks.",
  "timestamp": "2025-11-03T22:50:15"
}
```

#### **Response** (Partial Success - Status 207):
```json
{
  "success": true,
  "data": {
    "batchId": 2,
    "status": "PARTIAL",
    "totalRows": 100,
    "successCount": 95,
    "failedCount": 5,
    "skippedCount": 0,
    "errors": [
      {
        "rowNumber": 10,
        "orderId": "251030XYZ",
        "error": "Invalid commission amount",
        "rawData": "251030XYZ,Cancelled,..."
      }
    ],
    "message": "Partially imported 95 orders (5 failed, 0 skipped). 0 orders matched."
  }
}
```

#### **Response Fields Explained**:

| Field | Type | Description |
|-------|------|-------------|
| `batchId` | Long | Import batch ID (để track sau này) |
| `status` | String | `COMPLETED`, `PARTIAL`, `FAILED`, `PROCESSING` |
| `totalRows` | Integer | Tổng số dòng trong CSV |
| `successCount` | Integer | Số orders import thành công |
| `failedCount` | Integer | Số orders import thất bại |
| `skippedCount` | Integer | Số orders bị skip (duplicate, cancelled) |
| `successRate` | Double | Tỉ lệ thành công (%) |
| `errors` | Array | Chi tiết các dòng lỗi |
| `durationSeconds` | Long | Thời gian import (giây) |

---

### **2. Get User Wallet (User & Admin)**

#### **Endpoint**:
```
GET /api/wallets/user/{userId}
```

#### **Headers**:
```http
Authorization: Bearer {user_jwt_token}
```

#### **cURL Example**:
```bash
curl -X GET http://localhost:8080/api/wallets/user/1 \
  -H "Authorization: Bearer eyJhbGc..."
```

#### **JavaScript Example**:
```javascript
const getWallet = async (userId) => {
  try {
    const response = await axios.get(
      `/api/wallets/user/${userId}`,
      {
        headers: {
          'Authorization': `Bearer ${getUserToken()}`
        }
      }
    );

    return response.data.data;
  } catch (error) {
    console.error('Failed to get wallet:', error);
    throw error;
  }
};

// Usage
const wallet = await getWallet(1);
console.log('Balance:', wallet.balance);
console.log('Pending:', wallet.pendingBalance);
```

#### **Response** (Status 200):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 3500000,
    "pendingBalance": 4600000,
    "lockedBalance": 0,
    "totalEarned": 8100000,
    "totalWithdrawn": 0,
    "createdAt": "2025-11-01T10:00:00",
    "updatedAt": "2025-11-03T22:50:15"
  },
  "message": null,
  "timestamp": "2025-11-03T22:50:30"
}
```

#### **Response Fields Explained**:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `balance` | BigDecimal | **Tiền thật** - Rút được ngay | 3,500,000 VND |
| `pendingBalance` | BigDecimal | **Tiền chờ** - Chưa rút được | 4,600,000 VND |
| `lockedBalance` | BigDecimal | **Tiền khóa** - Đang xử lý payout | 0 VND |
| `totalEarned` | BigDecimal | **Tổng kiếm được** = balance + pending + locked | 8,100,000 VND |
| `totalWithdrawn` | BigDecimal | **Tổng đã rút** | 0 VND |

#### **UI Display Recommendations**:

```jsx
// React Component Example
function WalletDisplay({ wallet }) {
  return (
    <div className="wallet-card">
      <h2>Ví của bạn</h2>

      {/* Available Balance - CAN WITHDRAW */}
      <div className="balance-item primary">
        <label>Số dư khả dụng</label>
        <div className="amount">
          {formatMoney(wallet.balance)} ₫
        </div>
        <button onClick={handleWithdraw}>Rút tiền</button>
      </div>

      {/* Pending Balance - CANNOT WITHDRAW YET */}
      <div className="balance-item secondary">
        <label>Số dư chờ xử lý</label>
        <div className="amount">
          {formatMoney(wallet.pendingBalance)} ₫
        </div>
        <small>Đơn hàng đang được xử lý</small>
      </div>

      {/* Total Earned */}
      <div className="balance-item">
        <label>Tổng thu nhập</label>
        <div className="amount">
          {formatMoney(wallet.totalEarned)} ₫
        </div>
      </div>
    </div>
  );
}

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN').format(amount);
}
```

---

### **3. Get User Orders (User)**

#### **Endpoint**:
```
GET /api/orders/my?userId={userId}
```

**Note**: Trong production, `userId` sẽ được extract từ JWT token, không cần param.

#### **Headers**:
```http
Authorization: Bearer {user_jwt_token}
```

#### **JavaScript Example**:
```javascript
const getMyOrders = async (userId) => {
  try {
    const response = await axios.get(
      `/api/orders/my?userId=${userId}`,
      {
        headers: {
          'Authorization': `Bearer ${getUserToken()}`
        }
      }
    );

    return response.data.data;
  } catch (error) {
    console.error('Failed to get orders:', error);
    throw error;
  }
};

// Usage
const orders = await getMyOrders(1);
console.log(`You have ${orders.length} orders`);
```

#### **Response** (Status 200):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "orderId": "251030ABC",
      "userId": 1,
      "platformId": 1,
      "platformName": "Shopee",
      "productName": "Áo thun nam basic",
      "productPrice": 150000,
      "commissionAmount": 100000,
      "cashbackAmount": 70000,
      "currency": "VND",
      "orderTime": "2025-10-29T23:14:00",
      "orderStatus": "APPROVED",
      "cashbackStatus": "PAID",
      "source": "IMPORT",
      "createdAt": "2025-11-03T22:50:10"
    },
    {
      "id": 2,
      "orderId": "251030XYZ",
      "userId": 1,
      "platformId": 1,
      "platformName": "Shopee",
      "productName": "Giày thể thao",
      "productPrice": 500000,
      "commissionAmount": 50000,
      "cashbackAmount": 35000,
      "currency": "VND",
      "orderTime": "2025-10-29T22:46:00",
      "orderStatus": "PENDING",
      "cashbackStatus": "PENDING",
      "source": "IMPORT",
      "createdAt": "2025-11-03T22:50:10"
    }
  ],
  "timestamp": "2025-11-03T22:50:30"
}
```

#### **Order Status Explained**:

| orderStatus | cashbackStatus | Meaning | UI Display |
|------------|----------------|---------|------------|
| `APPROVED` | `PAID` | Order hoàn thành, tiền đã vào balance | ✅ "Đã hoàn thành" (Green) |
| `PENDING` | `PENDING` | Order đang chờ, tiền vào pending_balance | ⏳ "Đang xử lý" (Yellow) |
| `REJECTED` | `CANCELLED` | Order bị hủy, không có tiền | ❌ "Đã hủy" (Red) |

#### **UI Display Example**:

```jsx
// React Component
function OrdersList({ orders }) {
  return (
    <div className="orders-list">
      <h2>Đơn hàng của bạn ({orders.length})</h2>

      {orders.map(order => (
        <div key={order.id} className="order-card">
          <div className="order-header">
            <span className="order-id">{order.orderId}</span>
            <OrderStatusBadge status={order.orderStatus} />
          </div>

          <div className="order-body">
            <div className="product-info">
              <h3>{order.productName}</h3>
              <p className="platform">{order.platformName}</p>
            </div>

            <div className="order-amounts">
              <div className="amount-row">
                <label>Giá sản phẩm:</label>
                <span>{formatMoney(order.productPrice)} ₫</span>
              </div>
              <div className="amount-row">
                <label>Hoa hồng:</label>
                <span>{formatMoney(order.commissionAmount)} ₫</span>
              </div>
              <div className="amount-row highlight">
                <label>Cashback của bạn:</label>
                <span className="cashback">
                  {formatMoney(order.cashbackAmount)} ₫
                </span>
              </div>
            </div>
          </div>

          <div className="order-footer">
            <small>{formatDate(order.orderTime)}</small>
            <CashbackStatusBadge status={order.cashbackStatus} />
          </div>
        </div>
      ))}
    </div>
  );
}

function OrderStatusBadge({ status }) {
  const statusConfig = {
    APPROVED: { label: 'Đã hoàn thành', color: 'green' },
    PENDING: { label: 'Đang xử lý', color: 'yellow' },
    REJECTED: { label: 'Đã hủy', color: 'red' }
  };

  const config = statusConfig[status];

  return (
    <span className={`badge badge-${config.color}`}>
      {config.label}
    </span>
  );
}

function CashbackStatusBadge({ status }) {
  const statusConfig = {
    PAID: { label: '✅ Đã nhận tiền', color: 'green' },
    PENDING: { label: '⏳ Đang chờ', color: 'yellow' },
    CANCELLED: { label: '❌ Đã hủy', color: 'red' }
  };

  const config = statusConfig[status];

  return (
    <span className={`badge badge-${config.color}`}>
      {config.label}
    </span>
  );
}
```

---

### **4. Get Transaction History (User)**

#### **Endpoint**:
```
GET /api/transactions/user/{userId}?page={page}&size={size}
```

#### **Query Parameters**:
- `page`: Page number (0-indexed), default = 0
- `size`: Page size, default = 20

#### **JavaScript Example**:
```javascript
const getTransactions = async (userId, page = 0, size = 20) => {
  try {
    const response = await axios.get(
      `/api/transactions/user/${userId}`,
      {
        params: { page, size },
        headers: {
          'Authorization': `Bearer ${getUserToken()}`
        }
      }
    );

    return response.data.data;
  } catch (error) {
    console.error('Failed to get transactions:', error);
    throw error;
  }
};

// Usage
const transactions = await getTransactions(1, 0, 10);
console.log(`Found ${transactions.length} transactions`);
```

#### **Response** (Status 200):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "type": "CASHBACK",
      "amount": 70000,
      "balanceBefore": 0,
      "balanceAfter": 70000,
      "status": "SUCCESS",
      "description": "Cashback from order #251030ABC",
      "referenceId": "ORDER_1",
      "createdAt": "2025-11-03T22:50:10"
    },
    {
      "id": 2,
      "userId": 1,
      "type": "CASHBACK",
      "amount": 35000,
      "balanceBefore": 0,
      "balanceAfter": 0,
      "status": "PENDING",
      "description": "Pending cashback from order #251030XYZ",
      "referenceId": "ORDER_2",
      "createdAt": "2025-11-03T22:50:11"
    },
    {
      "id": 3,
      "userId": 1,
      "type": "WITHDRAWAL",
      "amount": -50000,
      "balanceBefore": 70000,
      "balanceAfter": 20000,
      "status": "SUCCESS",
      "description": "Withdrawal to bank account ***1234",
      "referenceId": "PAYOUT_1",
      "createdAt": "2025-11-04T10:30:00"
    }
  ],
  "timestamp": "2025-11-04T15:00:00"
}
```

#### **Transaction Types**:

| Type | Description | Amount Sign | Example |
|------|-------------|-------------|---------|
| `CASHBACK` | Nhận cashback từ order | Positive (+) | +70,000 ₫ |
| `WITHDRAWAL` | Rút tiền | Negative (-) | -50,000 ₫ |
| `REFUND` | Hoàn tiền (hủy order) | Negative (-) | -35,000 ₫ |
| `ADJUSTMENT` | Admin điều chỉnh | +/- | ±10,000 ₫ |

#### **UI Display Example**:

```jsx
function TransactionHistory({ userId }) {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadTransactions();
  }, [page]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const data = await getTransactions(userId, page, 20);
      setTransactions(data);
    } catch (error) {
      console.error('Failed to load transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="transaction-history">
      <h2>Lịch sử giao dịch</h2>

      {loading ? (
        <div>Đang tải...</div>
      ) : (
        <>
          <div className="transactions-list">
            {transactions.map(tx => (
              <div key={tx.id} className="transaction-item">
                <div className="tx-icon">
                  {getTransactionIcon(tx.type)}
                </div>

                <div className="tx-details">
                  <div className="tx-description">{tx.description}</div>
                  <div className="tx-date">
                    {formatDateTime(tx.createdAt)}
                  </div>
                </div>

                <div className={`tx-amount ${tx.amount > 0 ? 'positive' : 'negative'}`}>
                  {tx.amount > 0 ? '+' : ''}{formatMoney(tx.amount)} ₫
                </div>

                <TransactionStatusBadge status={tx.status} />
              </div>
            ))}
          </div>

          <div className="pagination">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </button>
            <span>Page {page + 1}</span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={transactions.length < 20}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function getTransactionIcon(type) {
  const icons = {
    CASHBACK: '💰',
    WITHDRAWAL: '🏦',
    REFUND: '↩️',
    ADJUSTMENT: '⚙️'
  };
  return icons[type] || '📄';
}

function TransactionStatusBadge({ status }) {
  const config = {
    SUCCESS: { label: 'Thành công', color: 'green' },
    PENDING: { label: 'Đang xử lý', color: 'yellow' },
    FAILED: { label: 'Thất bại', color: 'red' }
  };

  const { label, color } = config[status] || config.PENDING;

  return <span className={`badge badge-${color}`}>{label}</span>;
}
```

---

## ⚠️ ERROR HANDLING

### **Error Response Format**:

Tất cả errors đều trả về format chuẩn:

```json
{
  "success": false,
  "data": null,
  "message": "Error message here",
  "errorCode": "ERROR_CODE",
  "timestamp": "2025-11-03T22:50:00"
}
```

### **Common Error Codes**:

#### **1. File Upload Errors**:

| HTTP Status | Error Code | Message | Cause | Solution |
|------------|------------|---------|-------|----------|
| 400 | `INVALID_FILE` | "CSV file is empty" | File rỗng | Check file size > 0 |
| 400 | `INVALID_FILE_TYPE` | "Only CSV files are supported" | File không phải .csv | Check file extension |
| 400 | `FILE_READ_ERROR` | "Failed to read file: ..." | File corrupt | Upload lại file |
| 413 | `FILE_TOO_LARGE` | "File size exceeds limit" | File > 10MB | Giảm kích thước file |

**Frontend Validation**:
```javascript
const validateFile = (file) => {
  // Check file exists
  if (!file) {
    throw new Error('Please select a file');
  }

  // Check file type
  if (!file.name.toLowerCase().endsWith('.csv')) {
    throw new Error('Only CSV files are allowed');
  }

  // Check file size (max 10MB)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    throw new Error('File size must be less than 10MB');
  }

  // Check file not empty
  if (file.size === 0) {
    throw new Error('File is empty');
  }

  return true;
};

// Usage
const handleFileSelect = (event) => {
  const file = event.target.files[0];

  try {
    validateFile(file);
    setSelectedFile(file);
    setError(null);
  } catch (error) {
    setError(error.message);
    setSelectedFile(null);
  }
};
```

#### **2. Authentication Errors**:

| HTTP Status | Error Code | Message | Cause | Solution |
|------------|------------|---------|-------|----------|
| 401 | `UNAUTHORIZED` | "Authentication required" | No JWT token | Login first |
| 401 | `INVALID_TOKEN` | "Invalid or expired token" | Token hết hạn | Refresh token |
| 403 | `FORBIDDEN` | "Insufficient permissions" | User không phải admin | Check user role |

**Frontend Handling**:
```javascript
// Axios interceptor for auth errors
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Token expired, redirect to login
      localStorage.removeItem('token');
      window.location.href = '/login';
    }

    if (error.response?.status === 403) {
      // No permission
      alert('You do not have permission to perform this action');
    }

    return Promise.reject(error);
  }
);
```

#### **3. Business Logic Errors**:

| HTTP Status | Error Code | Message | Cause | Solution |
|------------|------------|---------|-------|----------|
| 404 | `NOT_FOUND` | "User not found" | User ID không tồn tại | Check user ID |
| 404 | `PLATFORM_NOT_FOUND` | "Platform not found: shopee" | Platform chưa setup | Contact admin |
| 400 | `DUPLICATE_ORDER` | "Order already exists" | Re-import CSV | Use skipDuplicates=true |
| 400 | `INSUFFICIENT_BALANCE` | "Balance is insufficient" | Rút quá số dư | Check balance first |

**Frontend Handling**:
```javascript
const handleUploadError = (error) => {
  const errorCode = error.response?.data?.errorCode;
  const message = error.response?.data?.message;

  // Map error codes to user-friendly messages
  const errorMessages = {
    'INVALID_FILE': 'File không hợp lệ. Vui lòng chọn file CSV.',
    'INVALID_FILE_TYPE': 'Chỉ chấp nhận file CSV.',
    'PLATFORM_NOT_FOUND': 'Nền tảng không tồn tại. Vui lòng liên hệ admin.',
    'DUPLICATE_ORDER': 'Có đơn hàng trùng lặp trong file.',
    'UNAUTHORIZED': 'Vui lòng đăng nhập lại.',
    'FORBIDDEN': 'Bạn không có quyền thực hiện chức năng này.'
  };

  const userMessage = errorMessages[errorCode] || message || 'Đã xảy ra lỗi';

  // Show error to user
  showToast(userMessage, 'error');

  // Log for debugging
  console.error('Upload error:', {
    errorCode,
    message,
    fullError: error.response?.data
  });
};
```

---

## 💻 FRONTEND IMPLEMENTATION GUIDE

### **Complete Admin Upload Page**:

```jsx
import React, { useState } from 'react';
import axios from 'axios';

function AdminImportPage() {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFileSelect = (event) => {
    const selectedFile = event.target.files[0];

    // Validate file
    try {
      if (!selectedFile) {
        throw new Error('Please select a file');
      }

      if (!selectedFile.name.toLowerCase().endsWith('.csv')) {
        throw new Error('Only CSV files are allowed');
      }

      if (selectedFile.size === 0) {
        throw new Error('File is empty');
      }

      if (selectedFile.size > 10 * 1024 * 1024) {
        throw new Error('File size must be less than 10MB');
      }

      setFile(selectedFile);
      setError(null);
      setResult(null);
    } catch (err) {
      setError(err.message);
      setFile(null);
    }
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file first');
      return;
    }

    setUploading(true);
    setError(null);
    setResult(null);

    try {
      // Get admin user ID from local storage or context
      const adminUserId = localStorage.getItem('userId');

      // Create form data
      const formData = new FormData();
      formData.append('file', file);
      formData.append('platformCode', 'shopee');
      formData.append('importedBy', adminUserId);
      formData.append('skipDuplicates', 'true');
      formData.append('autoMatch', 'true');

      // Upload
      const response = await axios.post(
        '/api/admin/import/orders',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          onUploadProgress: (progressEvent) => {
            const percentCompleted = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            );
            console.log(`Upload progress: ${percentCompleted}%`);
          }
        }
      );

      setResult(response.data.data);

      // Show success message
      showToast(
        `Successfully imported ${response.data.data.successCount} orders`,
        'success'
      );

      // Clear file selection
      setFile(null);

    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Upload failed';
      setError(errorMessage);

      showToast(errorMessage, 'error');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="admin-import-page">
      <h1>Import Orders from CSV</h1>

      {/* File Selection */}
      <div className="file-upload-section">
        <input
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          disabled={uploading}
          id="csv-file-input"
        />
        <label htmlFor="csv-file-input" className="file-select-button">
          {file ? file.name : 'Choose CSV File'}
        </label>

        {file && (
          <div className="file-info">
            <p>File: {file.name}</p>
            <p>Size: {formatFileSize(file.size)}</p>
          </div>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <div className="alert alert-error">
          <span className="alert-icon">❌</span>
          {error}
        </div>
      )}

      {/* Upload Button */}
      <button
        onClick={handleUpload}
        disabled={!file || uploading}
        className="btn btn-primary"
      >
        {uploading ? 'Uploading...' : 'Upload & Import'}
      </button>

      {/* Progress Indicator */}
      {uploading && (
        <div className="progress-section">
          <div className="spinner"></div>
          <p>Processing CSV file... This may take a few minutes.</p>
        </div>
      )}

      {/* Result Display */}
      {result && (
        <div className="result-section">
          <h2>Import Result</h2>

          <div className="result-summary">
            <div className="summary-item success">
              <label>Success</label>
              <div className="value">{result.successCount}</div>
            </div>
            <div className="summary-item failed">
              <label>Failed</label>
              <div className="value">{result.failedCount}</div>
            </div>
            <div className="summary-item skipped">
              <label>Skipped</label>
              <div className="value">{result.skippedCount}</div>
            </div>
          </div>

          <div className="result-details">
            <p>Batch ID: #{result.batchId}</p>
            <p>Status: <StatusBadge status={result.status} /></p>
            <p>Total Rows: {result.totalRows}</p>
            <p>Success Rate: {result.successRate}%</p>
            <p>Duration: {result.durationSeconds}s</p>
          </div>

          {/* Error Details */}
          {result.errors && result.errors.length > 0 && (
            <div className="error-details">
              <h3>Error Details</h3>
              <table>
                <thead>
                  <tr>
                    <th>Row</th>
                    <th>Order ID</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {result.errors.map((err, index) => (
                    <tr key={index}>
                      <td>{err.rowNumber}</td>
                      <td>{err.orderId}</td>
                      <td>{err.error}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function StatusBadge({ status }) {
  const config = {
    COMPLETED: { label: 'Completed', color: 'green' },
    PARTIAL: { label: 'Partial', color: 'yellow' },
    FAILED: { label: 'Failed', color: 'red' }
  };

  const { label, color } = config[status] || config.COMPLETED;

  return <span className={`badge badge-${color}`}>{label}</span>;
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

function showToast(message, type) {
  // Implement your toast notification here
  alert(`${type.toUpperCase()}: ${message}`);
}

export default AdminImportPage;
```

### **Complete User Dashboard Page**:

```jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function UserDashboard({ userId }) {
  const [wallet, setWallet] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadUserData();
  }, [userId]);

  const loadUserData = async () => {
    setLoading(true);
    setError(null);

    try {
      // Load wallet and orders in parallel
      const [walletResponse, ordersResponse] = await Promise.all([
        axios.get(`/api/wallets/user/${userId}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        }),
        axios.get(`/api/orders/my?userId=${userId}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` }
        })
      ]);

      setWallet(walletResponse.data.data);
      setOrders(ordersResponse.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (error) {
    return (
      <div className="error-page">
        <h2>Error</h2>
        <p>{error}</p>
        <button onClick={loadUserData}>Retry</button>
      </div>
    );
  }

  return (
    <div className="user-dashboard">
      {/* Wallet Section */}
      <section className="wallet-section">
        <h2>Your Wallet</h2>

        <div className="wallet-cards">
          <div className="wallet-card primary">
            <label>Available Balance</label>
            <div className="amount-large">
              {formatMoney(wallet.balance)} ₫
            </div>
            <button className="btn btn-primary">Withdraw</button>
          </div>

          <div className="wallet-card secondary">
            <label>Pending Balance</label>
            <div className="amount-large">
              {formatMoney(wallet.pendingBalance)} ₫
            </div>
            <small>Orders being processed</small>
          </div>
        </div>

        <div className="wallet-stats">
          <div className="stat-item">
            <label>Total Earned</label>
            <div className="value">{formatMoney(wallet.totalEarned)} ₫</div>
          </div>
          <div className="stat-item">
            <label>Total Withdrawn</label>
            <div className="value">{formatMoney(wallet.totalWithdrawn)} ₫</div>
          </div>
        </div>
      </section>

      {/* Orders Section */}
      <section className="orders-section">
        <h2>Your Orders ({orders.length})</h2>

        {orders.length === 0 ? (
          <div className="empty-state">
            <p>No orders yet</p>
            <p>Create your first tracking link to start earning!</p>
          </div>
        ) : (
          <div className="orders-list">
            {orders.map(order => (
              <OrderCard key={order.id} order={order} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function OrderCard({ order }) {
  return (
    <div className="order-card">
      <div className="order-header">
        <span className="order-id">#{order.orderId}</span>
        <StatusBadge status={order.orderStatus} />
      </div>

      <div className="order-body">
        <h3>{order.productName}</h3>
        <p className="platform">{order.platformName}</p>

        <div className="order-amounts">
          <div className="amount-row">
            <label>Product Price:</label>
            <span>{formatMoney(order.productPrice)} ₫</span>
          </div>
          <div className="amount-row">
            <label>Commission:</label>
            <span>{formatMoney(order.commissionAmount)} ₫</span>
          </div>
          <div className="amount-row highlight">
            <label>Your Cashback:</label>
            <span className="cashback">
              {formatMoney(order.cashbackAmount)} ₫
            </span>
          </div>
        </div>
      </div>

      <div className="order-footer">
        <small>{formatDateTime(order.orderTime)}</small>
        <CashbackBadge status={order.cashbackStatus} />
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  const config = {
    APPROVED: { label: 'Completed', color: 'green', icon: '✅' },
    PENDING: { label: 'Processing', color: 'yellow', icon: '⏳' },
    REJECTED: { label: 'Cancelled', color: 'red', icon: '❌' }
  };

  const { label, color, icon } = config[status];

  return (
    <span className={`badge badge-${color}`}>
      {icon} {label}
    </span>
  );
}

function CashbackBadge({ status }) {
  const config = {
    PAID: { label: 'Received', color: 'green' },
    PENDING: { label: 'Pending', color: 'yellow' },
    CANCELLED: { label: 'Cancelled', color: 'red' }
  };

  const { label, color } = config[status];

  return (
    <span className={`badge-small badge-${color}`}>
      {label}
    </span>
  );
}

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN').format(amount);
}

function formatDateTime(dateString) {
  return new Date(dateString).toLocaleString('vi-VN');
}

function getToken() {
  return localStorage.getItem('token');
}

export default UserDashboard;
```

---

## 🧪 TESTING GUIDE

### **1. Testing CSV Import (Postman)**:

#### **Step 1: Prepare Test Data**
Create file `test.csv`:
```csv
ID đơn hàng,Trạng thái đặt hàng,Sub_id1,Commission
251030ABC,Hoàn thành,CB1_1_20251103,100000
251030XYZ,Đang chờ xử lý,CB1_1_20251103,50000
```

#### **Step 2: Create Postman Request**
```
POST http://localhost:8080/api/admin/import/orders

Headers:
  Authorization: Bearer {your_admin_token}

Body (form-data):
  file: (Select test.csv)
  platformCode: shopee
  importedBy: 1
  skipDuplicates: true
  autoMatch: true
```

#### **Step 3: Verify Response**
Expected status: 200 OK
Expected body:
```json
{
  "success": true,
  "data": {
    "batchId": 1,
    "status": "COMPLETED",
    "totalRows": 2,
    "successCount": 2,
    "failedCount": 0
  }
}
```

#### **Step 4: Verify Database**
```sql
-- Check orders created
SELECT * FROM affiliate_order;

-- Check cashback created
SELECT * FROM cashback;

-- Check user wallet updated
SELECT * FROM user_wallet WHERE user_id = 1;

-- Check transactions created
SELECT * FROM transaction WHERE user_id = 1;
```

Expected results:
- 2 orders created
- 2 cashbacks created (1 CONFIRMED, 1 PENDING)
- User wallet balance += 100,000 (from completed order)
- User wallet pending_balance += 50,000 (from pending order)

### **2. Testing with Frontend**:

#### **Test Scenario 1: Happy Path**
```javascript
// 1. User login
const loginResponse = await login('user@example.com', 'password');
const token = loginResponse.data.token;

// 2. Get initial wallet balance
const walletBefore = await getWallet(userId);
console.log('Balance before:', walletBefore.balance);

// 3. Admin uploads CSV
const importResult = await uploadCSV(file, adminUserId);
console.log('Import result:', importResult);

// 4. Get updated wallet balance
const walletAfter = await getWallet(userId);
console.log('Balance after:', walletAfter.balance);

// Verify
expect(walletAfter.balance).toBeGreaterThan(walletBefore.balance);
```

#### **Test Scenario 2: Error Handling**
```javascript
// Test invalid file
try {
  await uploadCSV(null, adminUserId);
} catch (error) {
  expect(error.response.status).toBe(400);
  expect(error.response.data.errorCode).toBe('INVALID_FILE');
}

// Test non-CSV file
const txtFile = new File(['test'], 'test.txt', { type: 'text/plain' });
try {
  await uploadCSV(txtFile, adminUserId);
} catch (error) {
  expect(error.response.status).toBe(400);
  expect(error.response.data.errorCode).toBe('INVALID_FILE_TYPE');
}
```

---

## ❓ FAQ

### **Q1: Tại sao có 2 loại balance (balance và pendingBalance)?**

**A**:
- **balance**: Tiền từ orders đã "Hoàn thành" → User rút được ngay
- **pendingBalance**: Tiền từ orders "Đang chờ xử lý" → Chờ order complete mới chuyển sang balance

**Lý do**: Bảo vệ platform khỏi các order bị hủy sau khi đã trả tiền.

### **Q2: Làm sao biết order nào cho bao nhiêu cashback?**

**A**: Gọi API `GET /api/orders/my?userId={userId}`, mỗi order có field `cashbackAmount`.

**Example**:
```json
{
  "orderId": "251030ABC",
  "commissionAmount": 100000,
  "cashbackAmount": 70000,
  "cashbackStatus": "PAID"
}
```

### **Q3: Cashback được tính như thế nào?**

**A**:
```
Cashback = Commission × Rate / 100

Ví dụ:
- Commission: 100,000 VND
- Rate: 70% (từ CashbackPolicy)
- Cashback: 100,000 × 70 / 100 = 70,000 VND
```

### **Q4: Làm sao để test không cần admin token?**

**A**: Sử dụng mock data hoặc API mocking tool như MSW (Mock Service Worker):

```javascript
// mock-server.js
import { rest } from 'msw';

export const handlers = [
  // Mock import API
  rest.post('/api/admin/import/orders', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        data: {
          batchId: 1,
          status: 'COMPLETED',
          totalRows: 100,
          successCount: 100,
          failedCount: 0,
          skippedCount: 0
        }
      })
    );
  }),

  // Mock wallet API
  rest.get('/api/wallets/user/:userId', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        data: {
          userId: 1,
          balance: 3500000,
          pendingBalance: 4600000,
          totalEarned: 8100000
        }
      })
    );
  }),

  // Mock orders API
  rest.get('/api/orders/my', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        data: [
          {
            id: 1,
            orderId: '251030ABC',
            productName: 'Áo thun nam',
            commissionAmount: 100000,
            cashbackAmount: 70000,
            orderStatus: 'APPROVED',
            cashbackStatus: 'PAID'
          }
        ]
      })
    );
  })
];
```

### **Q5: UI nên hiển thị gì khi order đang pending?**

**A**:
```jsx
{order.cashbackStatus === 'PENDING' && (
  <div className="pending-notice">
    <span className="icon">⏳</span>
    <div>
      <strong>Cashback đang chờ xử lý</strong>
      <p>
        Số tiền {formatMoney(order.cashbackAmount)} ₫ sẽ được cộng vào
        số dư khả dụng khi đơn hàng hoàn thành.
      </p>
    </div>
  </div>
)}
```

### **Q6: Có thể import nhiều file cùng lúc không?**

**A**: Không. API chỉ nhận 1 file mỗi request. Nếu cần import nhiều file:

```javascript
const importMultipleFiles = async (files) => {
  const results = [];

  for (const file of files) {
    try {
      const result = await uploadCSV(file, adminUserId);
      results.push({ file: file.name, success: true, result });
    } catch (error) {
      results.push({ file: file.name, success: false, error });
    }
  }

  return results;
};
```

### **Q7: Làm sao track progress khi upload file lớn?**

**A**: Sử dụng `onUploadProgress`:

```javascript
const uploadWithProgress = async (file, onProgress) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('platformCode', 'shopee');
  formData.append('importedBy', adminUserId);

  return axios.post('/api/admin/import/orders', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${getToken()}`
    },
    onUploadProgress: (progressEvent) => {
      const percentCompleted = Math.round(
        (progressEvent.loaded * 100) / progressEvent.total
      );
      onProgress(percentCompleted);
    }
  });
};

// Usage
await uploadWithProgress(file, (percent) => {
  console.log(`Upload progress: ${percent}%`);
  setUploadProgress(percent);
});
```

---

## 📞 SUPPORT

**Nếu gặp vấn đề**:

1. **Check Logs**: Xem browser console và network tab
2. **Check Response**: Đọc error message trong response
3. **Check Documentation**: Tìm trong FAQ
4. **Contact Backend Team**: Provide error logs và request/response

**Example Bug Report**:
```
Issue: Import CSV returns 500 error

Request:
POST /api/admin/import/orders
File: test.csv (182 rows)

Response:
Status: 500
Body: {
  "success": false,
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "Failed to process CSV"
}

Expected: Status 200 with import results

Browser: Chrome 120
Environment: Staging
Timestamp: 2025-11-03T22:50:00
```

---

## ✅ CHECKLIST FOR FRONTEND DEVELOPERS

**Before Starting Development**:
- [ ] Đọc toàn bộ tài liệu này
- [ ] Hiểu flow từ đầu đến cuối
- [ ] Setup Postman collection để test API
- [ ] Có access token để test

**During Development**:
- [ ] Validate file trước khi upload
- [ ] Handle all error cases
- [ ] Show loading states
- [ ] Display success/error messages
- [ ] Format money correctly (VND)
- [ ] Format dates correctly (Vietnamese locale)

**Before Release**:
- [ ] Test với real CSV file
- [ ] Test error cases
- [ ] Test với file size lớn
- [ ] Verify wallet balance updates
- [ ] Verify order list updates
- [ ] Check responsive design
- [ ] Test trên nhiều browsers

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Maintained By**: Backend Team
**Contact**: backend-team@cashbee.com
