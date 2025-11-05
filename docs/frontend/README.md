# 📦 Frontend Integration Package

**Version**: 1.0
**Date**: 2025-11-03
**Status**: ✅ Production Ready

---

## 📋 MỤC LỤC

1. [Quick Start](#quick-start)
2. [Files Included](#files-included)
3. [Installation](#installation)
4. [Usage Examples](#usage-examples)
5. [Complete Examples](#complete-examples)
6. [Testing](#testing)
7. [FAQ](#faq)

---

## 🚀 QUICK START

### **Bước 1: Copy Files**

Copy 3 files này vào project của bạn:

```
your-project/
├── src/
│   ├── api/
│   │   ├── types.ts           ← Copy từ đây
│   │   ├── client.ts          ← Copy từ đây
│   │   └── config.ts          ← Tạo mới
│   └── ...
└── ...
```

### **Bước 2: Install Dependencies**

```bash
npm install axios
# or
yarn add axios
```

### **Bước 3: Configure API Base URL**

Create `src/api/config.ts`:

```typescript
export const API_CONFIG = {
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000
};
```

### **Bước 4: Use API Client**

```typescript
import api from './api/client';

// Example: Upload CSV
const file = document.querySelector('input[type="file"]').files[0];
const result = await api.importOrders(file, adminUserId);
console.log('Import successful:', result);

// Example: Get wallet
const wallet = await api.getWallet(userId);
console.log('Balance:', wallet.balance);
```

---

## 📁 FILES INCLUDED

### **1. FE_GUIDE_CSV_IMPORT_CASHBACK.md**

**Tài liệu chính** - Đọc file này trước!

**Nội dung**:
- ✅ Tổng quan flow hoàn chỉnh
- ✅ API endpoints chi tiết
- ✅ Request/Response examples
- ✅ Error handling guide
- ✅ Frontend implementation guide
- ✅ Testing guide
- ✅ FAQ

**Khi nào đọc**: Trước khi bắt đầu code

---

### **2. types.ts**

**TypeScript type definitions**

**Nội dung**:
- ✅ Interface cho tất cả API responses
- ✅ Enum cho status values
- ✅ Helper functions (formatMoney, formatDateTime)
- ✅ Error codes

**Cách dùng**:

```typescript
import type {
  WalletResponse,
  AffiliateOrderResponse,
  ImportOrdersResponse
} from './api/types';

// Type-safe API calls
const wallet: WalletResponse = await getWallet(userId);
const orders: AffiliateOrderResponse[] = await getMyOrders(userId);
```

**Features**:
- ✅ Type safety cho tất cả API calls
- ✅ Autocomplete trong IDE
- ✅ Compile-time error checking
- ✅ Better documentation

---

### **3. api-client.ts**

**Ready-to-use API client**

**Nội dung**:
- ✅ Pre-configured axios instance
- ✅ Auto token management
- ✅ Auto error handling
- ✅ All API methods implemented

**Cách dùng**:

```typescript
import api from './api/client';

// All methods available:
await api.importOrders(file, adminUserId);
await api.getWallet(userId);
await api.getMyOrders(userId);
await api.getTransactions(userId, page, size);
```

**Features**:
- ✅ Automatic JWT token injection
- ✅ Global error handling (401, 403, 500)
- ✅ Request/response interceptors
- ✅ Progress tracking for file upload
- ✅ Type-safe methods

---

## 📦 INSTALLATION

### **Method 1: Copy Files (Recommended)**

```bash
# 1. Create directory
mkdir -p src/api

# 2. Copy files
cp types.ts src/api/
cp api-client.ts src/api/client.ts

# 3. Install axios
npm install axios
```

### **Method 2: NPM Package (Future)**

```bash
# Coming soon
npm install @cashbee/api-client
```

---

## 💻 USAGE EXAMPLES

### **Example 1: Upload CSV (Admin)**

```typescript
import React, { useState } from 'react';
import api from './api/client';
import { ImportOrdersResponse } from './api/types';

function AdminUploadPage() {
  const [result, setResult] = useState<ImportOrdersResponse | null>(null);
  const [uploading, setUploading] = useState(false);

  const handleUpload = async (file: File) => {
    setUploading(true);

    try {
      // Validate file first
      api.validateCSVFile(file);

      // Upload with progress tracking
      const result = await api.importOrdersWithProgress(
        file,
        adminUserId,
        (progress) => {
          console.log(`Progress: ${progress}%`);
        }
      );

      setResult(result);
      alert(`Successfully imported ${result.successCount} orders!`);

    } catch (error) {
      alert('Upload failed: ' + error.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <input
        type="file"
        accept=".csv"
        onChange={(e) => {
          if (e.target.files[0]) {
            handleUpload(e.target.files[0]);
          }
        }}
        disabled={uploading}
      />

      {result && (
        <div>
          <h3>Import Result</h3>
          <p>Success: {result.successCount}</p>
          <p>Failed: {result.failedCount}</p>
          <p>Duration: {result.durationSeconds}s</p>
        </div>
      )}
    </div>
  );
}
```

---

### **Example 2: Display Wallet (User)**

```typescript
import React, { useEffect, useState } from 'react';
import api from './api/client';
import { WalletResponse } from './api/types';
import { formatMoney } from './api/types';

function WalletPage({ userId }: { userId: number }) {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWallet();
  }, [userId]);

  const loadWallet = async () => {
    setLoading(true);
    try {
      const data = await api.getWallet(userId);
      setWallet(data);
    } catch (error) {
      console.error('Failed to load wallet:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (!wallet) return <div>No wallet found</div>;

  return (
    <div className="wallet-page">
      <h2>Your Wallet</h2>

      <div className="balance-card">
        <label>Available Balance</label>
        <div className="amount">{formatMoney(wallet.balance)} ₫</div>
        <small>Can withdraw now</small>
      </div>

      <div className="balance-card secondary">
        <label>Pending Balance</label>
        <div className="amount">{formatMoney(wallet.pendingBalance)} ₫</div>
        <small>Orders being processed</small>
      </div>

      <div className="stats">
        <div>Total Earned: {formatMoney(wallet.totalEarned)} ₫</div>
        <div>Total Withdrawn: {formatMoney(wallet.totalWithdrawn)} ₫</div>
      </div>
    </div>
  );
}
```

---

### **Example 3: Display Orders List (User)**

```typescript
import React, { useEffect, useState } from 'react';
import api from './api/client';
import { AffiliateOrderResponse } from './api/types';
import { formatMoney, getOrderStatusConfig, getCashbackStatusConfig } from './api/types';

function OrdersPage({ userId }: { userId: number }) {
  const [orders, setOrders] = useState<AffiliateOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadOrders();
  }, [userId]);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const data = await api.getMyOrders(userId);
      setOrders(data);
    } catch (error) {
      console.error('Failed to load orders:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading orders...</div>;

  return (
    <div className="orders-page">
      <h2>Your Orders ({orders.length})</h2>

      {orders.length === 0 ? (
        <div className="empty">No orders yet</div>
      ) : (
        <div className="orders-list">
          {orders.map(order => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      )}
    </div>
  );
}

function OrderCard({ order }: { order: AffiliateOrderResponse }) {
  const orderStatus = getOrderStatusConfig(order.orderStatus);
  const cashbackStatus = getCashbackStatusConfig(order.cashbackStatus);

  return (
    <div className="order-card">
      <div className="header">
        <span>#{order.orderId}</span>
        <span className={`badge badge-${orderStatus.color}`}>
          {orderStatus.icon} {orderStatus.label}
        </span>
      </div>

      <div className="body">
        <h3>{order.productName}</h3>
        <p>{order.platformName}</p>

        <div className="amounts">
          <div>Product: {formatMoney(order.productPrice)} ₫</div>
          <div>Commission: {formatMoney(order.commissionAmount)} ₫</div>
          <div className="highlight">
            Cashback: {formatMoney(order.cashbackAmount)} ₫
          </div>
        </div>
      </div>

      <div className="footer">
        <small>{new Date(order.orderTime).toLocaleDateString('vi-VN')}</small>
        <span className={`badge badge-${cashbackStatus.color}`}>
          {cashbackStatus.label}
        </span>
      </div>
    </div>
  );
}
```

---

### **Example 4: Transaction History (User)**

```typescript
import React, { useEffect, useState } from 'react';
import api from './api/client';
import { TransactionResponse } from './api/types';
import { formatMoney, getTransactionIcon } from './api/types';

function TransactionsPage({ userId }: { userId: number }) {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadTransactions();
  }, [page]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const data = await api.getTransactions(userId, page, 20);
      setTransactions(data);
    } catch (error) {
      console.error('Failed to load transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="transactions-page">
      <h2>Transaction History</h2>

      {loading ? (
        <div>Loading...</div>
      ) : (
        <>
          <div className="transactions-list">
            {transactions.map(tx => (
              <div key={tx.id} className="transaction-item">
                <div className="icon">
                  {getTransactionIcon(tx.type)}
                </div>

                <div className="details">
                  <div className="description">{tx.description}</div>
                  <div className="date">
                    {new Date(tx.createdAt).toLocaleString('vi-VN')}
                  </div>
                </div>

                <div className={`amount ${tx.amount > 0 ? 'positive' : 'negative'}`}>
                  {tx.amount > 0 ? '+' : ''}{formatMoney(tx.amount)} ₫
                </div>
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
```

---

## 🧪 TESTING

### **1. Testing with Mock Data**

Create `src/api/mock.ts`:

```typescript
import { WalletResponse, AffiliateOrderResponse } from './types';

export const mockWallet: WalletResponse = {
  id: 1,
  userId: 1,
  balance: 3500000,
  pendingBalance: 4600000,
  lockedBalance: 0,
  totalEarned: 8100000,
  totalWithdrawn: 0,
  createdAt: '2025-11-01T10:00:00',
  updatedAt: '2025-11-03T22:50:15'
};

export const mockOrders: AffiliateOrderResponse[] = [
  {
    id: 1,
    orderId: '251030ABC',
    userId: 1,
    platformId: 1,
    platformName: 'Shopee',
    productName: 'Áo thun nam basic',
    productPrice: 150000,
    commissionAmount: 100000,
    cashbackAmount: 70000,
    currency: 'VND',
    orderTime: '2025-10-29T23:14:00',
    orderStatus: 'APPROVED',
    cashbackStatus: 'PAID',
    source: 'IMPORT',
    createdAt: '2025-11-03T22:50:10',
    updatedAt: '2025-11-03T22:50:10'
  }
];
```

### **2. Testing with MSW (Mock Service Worker)**

```bash
npm install msw --save-dev
```

Create `src/mocks/handlers.ts`:

```typescript
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
          failedCount: 0
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
          id: 1,
          userId: Number(req.params.userId),
          balance: 3500000,
          pendingBalance: 4600000,
          totalEarned: 8100000
        }
      })
    );
  })
];
```

---

## ❓ FAQ

### **Q: Làm sao để test mà không cần Backend running?**

**A**: Sử dụng Mock Service Worker (MSW) như ví dụ ở phần Testing.

---

### **Q: API base URL nên để ở đâu?**

**A**:
```
Development: http://localhost:8080
Staging: https://api-staging.cashbee.com
Production: https://api.cashbee.com
```

Dùng environment variables:
```typescript
const BASE_URL = process.env.REACT_APP_API_BASE_URL;
```

---

### **Q: Làm sao handle authentication?**

**A**: API client tự động inject token từ localStorage:

```typescript
// After login, save token
api.setAuthToken(loginResponse.token);

// All subsequent API calls automatically include token
await api.getWallet(userId); // Token auto-injected

// On logout
api.clearAuthToken();
```

---

### **Q: Làm sao refresh token khi expired?**

**A**: Modify interceptor trong `api-client.ts`:

```typescript
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Try refresh token
      const newToken = await refreshToken();
      if (newToken) {
        api.setAuthToken(newToken);
        // Retry original request
        return instance.request(error.config);
      }
    }
    return Promise.reject(error);
  }
);
```

---

### **Q: Type definitions có được update tự động không?**

**A**: Không. Khi Backend thay đổi API, cần update `types.ts` manually.

**Best practice**: Backend team nên maintain `types.ts` và share cho Frontend.

---

## 📞 SUPPORT

**Nếu gặp vấn đề**:

1. ✅ Đọc `FE_GUIDE_CSV_IMPORT_CASHBACK.md`
2. ✅ Check FAQ trong guide
3. ✅ Check browser console logs
4. ✅ Check network tab
5. ✅ Contact Backend team

**Report bug format**:
```
Issue: [Brief description]
API: POST /api/admin/import/orders
Request: [Request payload]
Response: [Response body]
Expected: [What you expected]
Browser: Chrome 120
Environment: Staging
```

---

## 🎉 READY TO GO!

Bạn đã có đủ mọi thứ để bắt đầu:

- ✅ Complete documentation
- ✅ Type definitions
- ✅ Ready-to-use API client
- ✅ Usage examples
- ✅ Testing guide

**Next steps**:
1. Copy files to your project
2. Install dependencies
3. Configure API base URL
4. Start coding!

Good luck! 🚀
