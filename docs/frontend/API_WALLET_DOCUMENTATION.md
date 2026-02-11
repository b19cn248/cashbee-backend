# 💰 CashBee Wallet API Documentation

**Version**: 2.0
**Date**: 2025-11-04
**Status**: ✅ Production Ready

---

## 📋 MỤC LỤC

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Wallet Balance Types](#wallet-balance-types)
4. [API Endpoints](#api-endpoints)
5. [Integration Guide](#integration-guide)
6. [Frontend Examples](#frontend-examples)
7. [Error Handling](#error-handling)

---

## 🎯 OVERVIEW

API Wallet cho phép Frontend:
- ✅ Lấy thông tin ví của user hiện tại (tự động từ JWT - **RECOMMENDED**)
- ✅ Lấy thông tin ví theo userId (cho admin)
- ✅ Hiển thị số dư available, pending, locked
- ✅ Hiển thị tổng tiền đã kiếm được
- ✅ Hiển thị tổng tiền đã rút
- ✅ Tính toán tổng số dư tất cả

**Base URL**:
- Development: `http://localhost:8080`
- Production: `https://api.cashbee.com`

---

## 🔐 AUTHENTICATION

### **Authentication Method**: OAuth2 + JWT (Keycloak)

Tất cả API endpoints đều yêu cầu JWT token trong header:

```http
Authorization: Bearer {JWT_TOKEN}
```

### **How Frontend Gets JWT Token:**

```javascript
// 1. Initialize Keycloak
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'https://auth.cashbee.com.vn/',
  realm: 'cashbee',
  clientId: 'cashbee-frontend'
});

// 2. Login
await keycloak.init({ onLoad: 'login-required' });

// 3. Get token
const token = keycloak.token;

// 4. Use token in API calls
const response = await fetch('/api/wallets/me', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### **Token Contains:**
- `sub` (subject): Keycloak User ID
- `email`: User email
- `preferred_username`: Username
- Backend automatically extracts userId from token → No need to send userId!

---

## 💵 WALLET BALANCE TYPES

User wallet có **3 loại số dư**:

### **1. Available Balance (`balance`)**

**Ý nghĩa**: Số tiền khả dụng, user có thể rút ngay

**Khi nào có tiền vào?**
- ✅ Order đã hoàn thành → Cashback được xác nhận
- ✅ Pending balance được confirm (order từ "Đang chờ" → "Hoàn thành")

**Ví dụ**:
```
User có 1 order "Hoàn thành" với cashback 70,000 VND
→ balance = 70,000 VND (có thể rút ngay)
```

---

### **2. Pending Balance (`pendingBalance`)**

**Ý nghĩa**: Số tiền đang chờ xác nhận, user thấy nhưng chưa rút được

**Khi nào có tiền vào?**
- ✅ Order "Đang chờ xử lý" → Cashback được tạo nhưng chưa confirm
- ✅ Đợi order được hoàn thành → Sẽ chuyển sang `balance`

**Ví dụ**:
```
User có 1 order "Đang chờ xử lý" với cashback 50,000 VND
→ pendingBalance = 50,000 VND (chưa thể rút)
```

---

### **3. Locked Balance (`lockedBalance`)**

**Ý nghĩa**: Số tiền đang bị khóa cho yêu cầu rút tiền

**Khi nào có tiền vào?**
- ✅ User yêu cầu rút tiền → Tiền chuyển từ `balance` → `lockedBalance`
- ✅ Đang xử lý chuyển khoản → Tránh user chi tiêu trùng

**Ví dụ**:
```
User yêu cầu rút 100,000 VND
→ balance giảm 100,000
→ lockedBalance tăng 100,000
```

**Khi rút tiền thành công**: `lockedBalance` giảm, `totalWithdrawn` tăng

---

### **4. Total Earned (`totalEarned`)**

**Ý nghĩa**: Tổng số tiền đã kiếm được (lifetime)

**Công thức**: `totalEarned = balance + pendingBalance + lockedBalance + totalWithdrawn`

---

### **5. Total Withdrawn (`totalWithdrawn`)**

**Ý nghĩa**: Tổng số tiền đã rút thành công (lifetime)

---

## 🔌 API ENDPOINTS

### **✅ GET /api/wallets/me** (RECOMMENDED)

**Mô tả**: Lấy thông tin ví của user hiện tại (tự động từ JWT token)

**Method**: `GET`

**URL**: `/api/wallets/me`

**Path Parameters**: NONE (userId tự động trích xuất từ JWT)

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Example**:

```bash
# ✅ Đơn giản - không cần userId
curl -X GET "http://localhost:8080/api/wallets/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

```javascript
// ✅ Frontend - Đơn giản nhất
const response = await fetch('/api/wallets/me', {
  headers: { 'Authorization': `Bearer ${keycloak.token}` }
});
const wallet = await response.json();
```

**Response Success (200 OK)**:

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
  "timestamp": "2025-11-04T07:20:00"
}
```

**Ưu điểm**:
- ✅ Không cần lưu userId trong localStorage
- ✅ Không thể xem ví của người khác (tự động từ JWT)
- ✅ Code đơn giản hơn
- ✅ An toàn hơn (JWT không thể giả mạo)

---

### **GET /api/wallets/user/{userId}** (Admin use)

**Mô tả**: Lấy thông tin ví theo userId (dành cho admin hoặc khi cần query user khác)

**Method**: `GET`

**URL**: `/api/wallets/user/{userId}`

**Path Parameters**:
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| userId | Long | ✅ Yes | User ID |

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Example**:

```bash
curl -X GET "http://localhost:8080/api/wallets/user/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response Success (200 OK)**:

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
  "timestamp": "2025-11-04T07:20:00"
}
```

**Response Fields**:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Long | Wallet ID | `1` |
| `userId` | Long | User ID | `1` |
| `balance` | Number | Available balance (VND) | `3500000` (3.5M VND) |
| `pendingBalance` | Number | Pending balance (VND) | `4600000` (4.6M VND) |
| `lockedBalance` | Number | Locked balance (VND) | `0` |
| `totalEarned` | Number | Total earned (VND) | `8100000` (8.1M VND) |
| `totalWithdrawn` | Number | Total withdrawn (VND) | `0` |
| `createdAt` | String (ISO 8601) | Wallet creation time | `2025-11-01T10:00:00` |
| `updatedAt` | String (ISO 8601) | Last update time | `2025-11-03T22:50:15` |

**Response Error (404 Not Found)**:

```json
{
  "success": false,
  "data": null,
  "message": "Wallet not found for user ID: 999",
  "errorCode": "WALLET_NOT_FOUND",
  "timestamp": "2025-11-04T07:20:00"
}
```

**Response Error (401 Unauthorized)**:

```json
{
  "success": false,
  "data": null,
  "message": "Authentication required",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2025-11-04T07:20:00"
}
```

---

### **Comparison: /me vs /user/{userId}**

| Feature | GET /api/wallets/me | GET /api/wallets/user/{userId} |
|---------|---------------------|--------------------------------|
| **Target** | Current logged-in user | Any user by ID |
| **userId required** | ❌ No (from JWT) | ✅ Yes (path param) |
| **Security** | ✅ High (can't forge) | ⚠️ Need authorization check |
| **Use case** | User viewing own wallet | Admin viewing any wallet |
| **Recommended for** | **Frontend (users)** | Backend/Admin |
| **Code complexity** | ✅ Simple | ⚠️ Need userId management |

---

## 🛠️ INTEGRATION GUIDE

### **Step 1: Add to API Client**

Update `src/api/client.ts`:

```typescript
import type { WalletResponse } from './types';

/**
 * Get current user's wallet (RECOMMENDED)
 *
 * Automatically extracts userId from JWT token.
 * No need to pass userId parameter!
 *
 * @returns Current user's wallet information
 *
 * @example
 * const wallet = await getMyWallet();
 * console.log('Balance:', wallet.balance);
 * console.log('Pending:', wallet.pendingBalance);
 */
export async function getMyWallet(): Promise<WalletResponse> {
  const response = await apiClient.get<ApiResponse<WalletResponse>>(
    '/api/wallets/me'  // ✅ No userId needed!
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to get wallet');
  }

  return response.data.data;
}

/**
 * Get wallet by user ID (for admin use)
 *
 * @param userId - User ID to query
 * @returns Wallet information for specified user
 *
 * @example
 * const wallet = await getWalletByUserId(123);
 */
export async function getWalletByUserId(userId: number): Promise<WalletResponse> {
  const response = await apiClient.get<ApiResponse<WalletResponse>>(
    `/api/wallets/user/${userId}`
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to get wallet');
  }

  return response.data.data;
}
```

---

### **Step 2: Add Types**

Already defined in `src/api/types.ts`:

```typescript
export interface WalletResponse {
  id: number;
  userId: number;
  balance: number;
  pendingBalance: number;
  lockedBalance: number;
  totalEarned: number;
  totalWithdrawn: number;
  createdAt: string;
  updatedAt: string;
}
```

---

### **Step 3: Add Helper Functions**

Update `src/api/types.ts`:

```typescript
/**
 * Calculate total balance (all states)
 */
export function calculateTotalBalance(wallet: WalletResponse): number {
  return wallet.balance + wallet.pendingBalance + wallet.lockedBalance;
}

/**
 * Check if can withdraw amount
 */
export function canWithdraw(wallet: WalletResponse, amount: number): boolean {
  return wallet.balance >= amount;
}

/**
 * Format balance with currency
 */
export function formatBalance(amount: number): string {
  return `${formatMoney(amount)} ₫`;
}
```

---

## 💻 FRONTEND EXAMPLES

### **Example 1: Wallet Balance Card (Simple) - NEW API**

```typescript
import React, { useEffect, useState } from 'react';
import api from './api/client';
import { WalletResponse } from './api/types';
import { formatBalance } from './api/types';

// ✅ NO userId prop needed!
function WalletBalanceCard() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWallet();
  }, []);

  const loadWallet = async () => {
    setLoading(true);
    try {
      // ✅ NEW: Use getMyWallet() - no userId needed!
      const data = await api.getMyWallet();
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
    <div className="wallet-card">
      <h2>Your Wallet</h2>

      {/* Available Balance */}
      <div className="balance-section primary">
        <label>Available Balance</label>
        <div className="amount">{formatBalance(wallet.balance)}</div>
        <small>Can withdraw now</small>
      </div>

      {/* Pending Balance */}
      <div className="balance-section secondary">
        <label>Pending Balance</label>
        <div className="amount">{formatBalance(wallet.pendingBalance)}</div>
        <small>Waiting for order completion</small>
      </div>

      {/* Statistics */}
      <div className="stats">
        <div className="stat-item">
          <span>Total Earned:</span>
          <span>{formatBalance(wallet.totalEarned)}</span>
        </div>
        <div className="stat-item">
          <span>Total Withdrawn:</span>
          <span>{formatBalance(wallet.totalWithdrawn)}</span>
        </div>
      </div>
    </div>
  );
}

export default WalletBalanceCard;
```

**CSS Example**:

```css
.wallet-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.balance-section {
  margin: 16px 0;
  padding: 20px;
  border-radius: 8px;
}

.balance-section.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.balance-section.secondary {
  background: #f7fafc;
  border: 2px solid #e2e8f0;
}

.balance-section label {
  display: block;
  font-size: 14px;
  margin-bottom: 8px;
  opacity: 0.8;
}

.balance-section .amount {
  font-size: 32px;
  font-weight: bold;
  margin: 8px 0;
}

.balance-section small {
  font-size: 12px;
  opacity: 0.7;
}

.stats {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
  font-size: 14px;
}
```

---

### **Example 2: Complete Wallet Dashboard**

```typescript
import React, { useEffect, useState } from 'react';
import api from './api/client';
import { WalletResponse } from './api/types';
import { formatBalance, calculateTotalBalance } from './api/types';

interface WalletDashboardProps {
  userId: number;
}

function WalletDashboard({ userId }: WalletDashboardProps) {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadWallet();

    // Auto refresh every 30 seconds
    const interval = setInterval(loadWallet, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  const loadWallet = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.getWallet(userId);
      setWallet(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load wallet');
      console.error('Wallet load error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !wallet) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading your wallet...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <div className="error-icon">⚠️</div>
        <p>{error}</p>
        <button onClick={loadWallet}>Retry</button>
      </div>
    );
  }

  if (!wallet) {
    return (
      <div className="empty-container">
        <p>No wallet found</p>
      </div>
    );
  }

  const totalBalance = calculateTotalBalance(wallet);

  return (
    <div className="wallet-dashboard">
      {/* Header */}
      <div className="dashboard-header">
        <h1>💰 Your Wallet</h1>
        <button onClick={loadWallet} disabled={loading}>
          {loading ? '⏳ Refreshing...' : '🔄 Refresh'}
        </button>
      </div>

      {/* Main Balance Display */}
      <div className="main-balance-card">
        <div className="total-balance">
          <label>Total Balance</label>
          <div className="amount">{formatBalance(totalBalance)}</div>
          <small>
            Available + Pending + Locked
          </small>
        </div>
      </div>

      {/* Balance Breakdown */}
      <div className="balance-grid">
        {/* Available Balance */}
        <div className="balance-card available">
          <div className="card-icon">💵</div>
          <div className="card-content">
            <label>Available Balance</label>
            <div className="amount">{formatBalance(wallet.balance)}</div>
            <small>Can withdraw now</small>
          </div>
          {wallet.balance > 0 && (
            <button className="withdraw-btn">Withdraw</button>
          )}
        </div>

        {/* Pending Balance */}
        <div className="balance-card pending">
          <div className="card-icon">⏳</div>
          <div className="card-content">
            <label>Pending Balance</label>
            <div className="amount">{formatBalance(wallet.pendingBalance)}</div>
            <small>
              {wallet.pendingBalance > 0
                ? 'Waiting for order completion'
                : 'No pending cashback'}
            </small>
          </div>
        </div>

        {/* Locked Balance */}
        <div className="balance-card locked">
          <div className="card-icon">🔒</div>
          <div className="card-content">
            <label>Locked Balance</label>
            <div className="amount">{formatBalance(wallet.lockedBalance)}</div>
            <small>
              {wallet.lockedBalance > 0
                ? 'Withdrawal in progress'
                : 'No locked funds'}
            </small>
          </div>
        </div>
      </div>

      {/* Statistics */}
      <div className="statistics-section">
        <h3>📊 Lifetime Statistics</h3>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">💰</div>
            <div className="stat-content">
              <label>Total Earned</label>
              <div className="stat-value">{formatBalance(wallet.totalEarned)}</div>
              <small>All time cashback</small>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">🏦</div>
            <div className="stat-content">
              <label>Total Withdrawn</label>
              <div className="stat-value">{formatBalance(wallet.totalWithdrawn)}</div>
              <small>Successfully withdrawn</small>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">📈</div>
            <div className="stat-content">
              <label>Net Remaining</label>
              <div className="stat-value">
                {formatBalance(wallet.totalEarned - wallet.totalWithdrawn)}
              </div>
              <small>Available + Pending + Locked</small>
            </div>
          </div>
        </div>
      </div>

      {/* Last Updated */}
      <div className="footer-info">
        <small>
          Last updated: {new Date(wallet.updatedAt).toLocaleString('vi-VN')}
        </small>
      </div>
    </div>
  );
}

export default WalletDashboard;
```

---

### **Example 3: Wallet Balance Hook (Reusable) - NEW API**

```typescript
// src/hooks/useWallet.ts
import { useState, useEffect, useCallback } from 'react';
import api from '../api/client';
import { WalletResponse } from '../api/types';

interface UseWalletResult {
  wallet: WalletResponse | null;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

/**
 * Custom hook to manage wallet data for current user
 *
 * ✅ NEW: No userId needed - automatically uses current user from JWT
 *
 * @param autoRefresh - Auto refresh interval in ms (0 = disabled)
 * @returns Wallet data and control functions
 *
 * @example
 * const { wallet, loading, error, refresh } = useWallet(30000);
 */
export function useWallet(autoRefresh: number = 0): UseWalletResult {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadWallet = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // ✅ NEW: Use getMyWallet() - no userId needed!
      const data = await api.getMyWallet();
      setWallet(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load wallet');
      console.error('Wallet load error:', err);
    } finally {
      setLoading(false);
    }
  }, []); // ✅ No dependencies - always loads current user

  useEffect(() => {
    loadWallet();

    if (autoRefresh > 0) {
      const interval = setInterval(loadWallet, autoRefresh);
      return () => clearInterval(interval);
    }
  }, [loadWallet, autoRefresh]);

  return {
    wallet,
    loading,
    error,
    refresh: loadWallet
  };
}

// Usage in component - MUCH SIMPLER!
function MyComponent() {
  // ✅ No userId prop needed!
  const { wallet, loading, error, refresh } = useWallet(30000);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!wallet) return <div>No wallet</div>;

  return (
    <div>
      <h2>Balance: {wallet.balance}</h2>
      <button onClick={refresh}>Refresh</button>
    </div>
  );
}
```

---

### **Example 4: Wallet Summary Widget (Compact)**

```typescript
import React from 'react';
import { WalletResponse } from './api/types';
import { formatMoney } from './api/types';

interface WalletSummaryProps {
  wallet: WalletResponse;
  onViewDetails?: () => void;
}

function WalletSummary({ wallet, onViewDetails }: WalletSummaryProps) {
  return (
    <div className="wallet-summary">
      {/* Quick Balance Display */}
      <div className="quick-balance">
        <div className="balance-item">
          <span className="label">Available:</span>
          <span className="value">{formatMoney(wallet.balance)} ₫</span>
        </div>
        <div className="balance-item">
          <span className="label">Pending:</span>
          <span className="value pending">{formatMoney(wallet.pendingBalance)} ₫</span>
        </div>
      </div>

      {/* Action Button */}
      {onViewDetails && (
        <button onClick={onViewDetails} className="details-btn">
          View Details →
        </button>
      )}
    </div>
  );
}

export default WalletSummary;
```

---

## ⚠️ ERROR HANDLING

### **Common Errors**

| Error Code | HTTP Status | Description | Solution |
|------------|-------------|-------------|----------|
| `WALLET_NOT_FOUND` | 404 | Wallet not found for user | Create wallet first or check userId |
| `UNAUTHORIZED` | 401 | No authentication token | Login and get JWT token |
| `FORBIDDEN` | 403 | User can't access this wallet | Check userId matches logged-in user |
| `INTERNAL_SERVER_ERROR` | 500 | Server error | Retry or contact support |

### **Error Handling Example**

```typescript
async function loadWalletSafely(userId: number) {
  try {
    const wallet = await api.getWallet(userId);
    return { success: true, data: wallet, error: null };

  } catch (error: any) {
    // Handle specific error codes
    if (error.response?.status === 404) {
      return {
        success: false,
        data: null,
        error: 'Wallet not found. Please create a wallet first.'
      };
    }

    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login';
      return {
        success: false,
        data: null,
        error: 'Please login to view wallet'
      };
    }

    // Generic error
    return {
      success: false,
      data: null,
      error: error.message || 'Failed to load wallet'
    };
  }
}

// Usage
const result = await loadWalletSafely(userId);
if (result.success) {
  console.log('Wallet:', result.data);
} else {
  console.error('Error:', result.error);
}
```

---

## 🧪 TESTING

### **Test with cURL**

```bash
# ✅ NEW: Get current user's wallet (RECOMMENDED)
curl -X GET "http://localhost:8080/api/wallets/me" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# Get wallet by userId (admin use)
curl -X GET "http://localhost:8080/api/wallets/user/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### **Test with Postman**

#### **Test 1: GET /api/wallets/me (RECOMMENDED)**

1. **Request**:
   - Method: `GET`
   - URL: `http://localhost:8080/api/wallets/me`
   - Headers:
     - `Authorization`: `Bearer YOUR_JWT_TOKEN`
     - `Content-Type`: `application/json`

2. **Expected Response**:
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
       "updatedAt": "2025-11-04T07:20:00"
     },
     "message": null,
     "timestamp": "2025-11-04T07:20:00"
   }
   ```

#### **Test 2: GET /api/wallets/user/{userId} (Admin)**

1. **Request**:
   - Method: `GET`
   - URL: `http://localhost:8080/api/wallets/user/1`
   - Headers:
     - `Authorization`: `Bearer YOUR_JWT_TOKEN`
     - `Content-Type`: `application/json`

2. **Expected Response**: Same as above

### **Mock Data for Testing**

```typescript
// src/mocks/walletMock.ts
export const mockWallet: WalletResponse = {
  id: 1,
  userId: 1,
  balance: 3500000,      // 3.5M VND available
  pendingBalance: 4600000, // 4.6M VND pending
  lockedBalance: 0,
  totalEarned: 8100000,   // 8.1M VND earned
  totalWithdrawn: 0,
  createdAt: '2025-11-01T10:00:00',
  updatedAt: '2025-11-03T22:50:15'
};

// Use in tests
import { mockWallet } from './mocks/walletMock';

test('renders wallet balance', () => {
  render(<WalletBalanceCard wallet={mockWallet} />);
  expect(screen.getByText('3.500.000 ₫')).toBeInTheDocument();
});
```

---

## 🎨 UI/UX RECOMMENDATIONS

### **Balance Display Best Practices**

1. **Use Color Coding**:
   - 🟢 Green: Available balance (can withdraw)
   - 🟡 Yellow: Pending balance (waiting)
   - 🔵 Blue: Locked balance (processing)

2. **Show Status Icons**:
   - 💵 Available
   - ⏳ Pending
   - 🔒 Locked

3. **Provide Context**:
   - Always show what each balance type means
   - Add tooltips for explanation
   - Show estimated time for pending → available

4. **Format Numbers**:
   - Use Vietnamese number format: `1.500.000 ₫`
   - Add thousand separators
   - Show currency symbol

5. **Auto Refresh**:
   - Refresh wallet every 30-60 seconds
   - Show "last updated" timestamp
   - Add manual refresh button

---

## 📱 RESPONSIVE DESIGN

### **Mobile Layout Example**

```css
/* Mobile styles */
@media (max-width: 768px) {
  .balance-grid {
    display: block;
  }

  .balance-card {
    margin-bottom: 16px;
  }

  .main-balance-card .amount {
    font-size: 28px;
  }
}

/* Desktop styles */
@media (min-width: 769px) {
  .balance-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
}
```

---

## ❓ FAQ

### **Q: Tại sao có 3 loại balance?**

**A**: Để quản lý tiền rõ ràng:
- `balance`: Tiền thật, rút được ngay
- `pendingBalance`: Tiền chờ (order chưa hoàn thành)
- `lockedBalance`: Tiền đang rút (tránh chi tiêu trùng)

---

### **Q: Khi nào pending balance chuyển thành available balance?**

**A**: Khi order từ "Đang chờ xử lý" → "Hoàn thành"

Backend sẽ gọi:
```
POST /api/wallets/confirm
{
  "userId": 1,
  "amount": 50000,
  "description": "Order #123 completed"
}
```

→ `pendingBalance -= 50000`, `balance += 50000`

---

### **Q: Làm sao biết wallet đã được cập nhật?**

**A**: Check field `updatedAt`:
```typescript
const lastUpdate = new Date(wallet.updatedAt);
const now = new Date();
const secondsAgo = (now.getTime() - lastUpdate.getTime()) / 1000;

if (secondsAgo < 60) {
  console.log('Updated recently!');
}
```

---

### **Q: Có API để lấy lịch sử giao dịch không?**

**A**: Có! Xem tài liệu: `GET /api/transactions/user/{userId}`

---

### **Q: Làm sao test mà không cần Backend?**

**A**: Sử dụng Mock Service Worker (MSW):

```typescript
// src/mocks/handlers.ts
import { rest } from 'msw';

export const handlers = [
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
          lockedBalance: 0,
          totalEarned: 8100000,
          totalWithdrawn: 0,
          createdAt: '2025-11-01T10:00:00',
          updatedAt: '2025-11-03T22:50:15'
        }
      })
    );
  })
];
```

---

## 🎉 SUMMARY

### **Key Points**:

1. ✅ **NEW API Endpoint**: `GET /api/wallets/me` (RECOMMENDED - no userId needed!)
2. ✅ **Legacy Endpoint**: `GET /api/wallets/user/{userId}` (for admin use)
3. ✅ **3 Balance Types**: Available, Pending, Locked
4. ✅ **TypeScript Support**: Full type definitions
5. ✅ **Ready-to-use Examples**: React components, hooks
6. ✅ **Error Handling**: Comprehensive error codes
7. ✅ **Testing Support**: Mock data, MSW setup
8. ✅ **Simplified Integration**: No userId management needed

### **What's New in v2.0:**

- 🆕 **GET /api/wallets/me** - Automatically uses current user from JWT
- 🆕 **Authentication section** - Complete Keycloak integration guide
- 🆕 **Simplified examples** - No userId props/parameters needed
- 🆕 **Updated hooks** - `useWallet()` no longer requires userId
- 🆕 **Better security** - Backend validates user from JWT token

### **Migration from v1.0:**

```typescript
// ❌ OLD (v1.0)
const wallet = await api.getWallet(userId);

// ✅ NEW (v2.0)
const wallet = await api.getMyWallet();  // No userId needed!
```

```typescript
// ❌ OLD (v1.0)
const { wallet } = useWallet(userId, 30000);

// ✅ NEW (v2.0)
const { wallet } = useWallet(30000);  // No userId needed!
```

### **Integration Steps**:

1. ✅ Copy `WalletResponse` type to your project
2. ✅ Add `getMyWallet()` to API client (NEW)
3. ✅ Use updated `useWallet()` hook (no userId param)
4. ✅ Update components to use `/me` endpoint
5. ✅ Remove userId from localStorage (no longer needed!)
6. ✅ Style with provided CSS
7. ✅ Test with mock data

**Bạn đã có đủ tất cả để hiển thị số dư ví cho user - ĐƠN GIẢN HƠN BAO GIỜ HẾT!** 🚀

---

## 📚 RELATED DOCUMENTATION

- User API Documentation: `API_USER_DOCUMENTATION.md`
- Main API Guide: `FE_GUIDE_CSV_IMPORT_CASHBACK.md`
- TypeScript Types: `types.ts`
- API Client: `api-client.ts`
- Authentication Guide: See [Authentication](#authentication) section above

---

**Last Updated**: 2025-11-04
**Version**: 2.0
**Status**: ✅ Production Ready

### **Changelog:**

**v2.0 (2025-11-04)**:
- Added `GET /api/wallets/me` endpoint (automatically uses JWT)
- Added Authentication section with Keycloak integration
- Updated all examples to use new `/me` endpoint
- Simplified integration (no userId management needed)
- Updated hooks to remove userId parameter
- Added migration guide from v1.0

**v1.0 (2025-11-03)**:
- Initial release
- `GET /api/wallets/user/{userId}` endpoint
- Basic wallet integration examples
