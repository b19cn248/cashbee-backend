# 👤 CashBee User API Documentation

**Version**: 1.0
**Date**: 2025-11-04
**Status**: ✅ Production Ready

---

## 📋 TABLE OF CONTENTS

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Integration Guide](#integration-guide)
5. [Frontend Examples](#frontend-examples)
6. [Error Handling](#error-handling)

---

## 🎯 OVERVIEW

User API cho phép Frontend:
- ✅ Lấy thông tin user hiện tại từ JWT (RECOMMENDED)
- ✅ Đồng bộ user từ Keycloak vào database
- ✅ Lấy thông tin user theo Keycloak ID
- ✅ Quản lý referral code

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

### **Keycloak Configuration:**

```javascript
const keycloak = new Keycloak({
  url: 'https://auth.cashbee.com.vn/',
  realm: 'cashbee',
  clientId: 'cashbee-frontend'
});

await keycloak.init({ onLoad: 'login-required' });
const token = keycloak.token;
```

---

## 🔌 API ENDPOINTS

### **✅ GET /api/users/me** (RECOMMENDED)

**Mô tả**: Lấy thông tin user hiện tại (tự động từ JWT token)

**Method**: `GET`

**URL**: `/api/users/me`

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Example**:

```bash
curl -X GET "http://localhost:8080/api/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

```javascript
// Frontend
const response = await fetch('/api/users/me', {
  headers: { 'Authorization': `Bearer ${keycloak.token}` }
});
const user = await response.json();
```

**Response Success (200 OK)**:

```json
{
  "success": true,
  "data": {
    "id": 123,
    "keycloakId": "uuid-from-keycloak",
    "username": "user123",
    "email": "user@example.com",
    "fullName": "John Doe",
    "phone": "0123456789",
    "referralCode": "CB4F7A9K",
    "referredBy": "CB1A2B3C",
    "status": "ACTIVE",
    "createdAt": "2025-11-01T10:00:00",
    "updatedAt": "2025-11-04T07:20:00"
  },
  "message": null,
  "timestamp": "2025-11-04T07:20:00"
}
```

**Ưu điểm**:
- ✅ Không cần lưu userId
- ✅ Tự động lấy từ JWT (an toàn)
- ✅ Code đơn giản

---

### **POST /api/users/sync**

**Mô tả**: Đồng bộ user từ Keycloak vào database (gọi sau khi login)

**Method**: `POST`

**URL**: `/api/users/sync`

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body**:

```json
{
  "keycloakId": "uuid-from-keycloak",
  "username": "user123",
  "email": "user@example.com",
  "fullName": "John Doe",
  "phone": "0123456789",
  "referredBy": "CB4F7A9K"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `keycloakId` | String | ✅ Yes | Keycloak User UUID (from JWT.sub) |
| `username` | String | ✅ Yes | Username |
| `email` | String | ✅ Yes | Email address |
| `fullName` | String | ❌ No | Full name |
| `phone` | String | ❌ No | Phone number |
| `referredBy` | String | ❌ No | Referral code of referrer |

**Request Example**:

```bash
curl -X POST "http://localhost:8080/api/users/sync" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "uuid-from-keycloak",
    "username": "user123",
    "email": "user@example.com",
    "fullName": "John Doe"
  }'
```

```javascript
// Frontend
const userInfo = keycloak.tokenParsed;

const response = await fetch('/api/users/sync', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${keycloak.token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    keycloakId: userInfo.sub,
    username: userInfo.preferred_username,
    email: userInfo.email,
    fullName: userInfo.name
  })
});
```

**Response Success (200 OK)**:

```json
{
  "success": true,
  "data": {
    "id": 123,
    "keycloakId": "uuid-from-keycloak",
    "username": "user123",
    "email": "user@example.com",
    "fullName": "John Doe",
    "phone": null,
    "referralCode": "CB4F7A9K",
    "referredBy": null,
    "status": "ACTIVE",
    "createdAt": "2025-11-04T07:20:00",
    "updatedAt": "2025-11-04T07:20:00"
  },
  "message": "User synced successfully",
  "timestamp": "2025-11-04T07:20:00"
}
```

**Khi nào gọi API này?**
1. **First-time login**: Tạo mới user + wallet
2. **Subsequent logins**: Cập nhật thông tin từ Keycloak (nếu có thay đổi)

**Backend xử lý gì?**
1. Kiểm tra user đã tồn tại (by keycloakId)
2. **Nếu chưa tồn tại**: Tạo mới User + Wallet + ReferralCode
3. **Nếu đã tồn tại**: Cập nhật thông tin (email, fullName, etc.)
4. Xử lý referral (nếu có `referredBy`)

---

### **GET /api/users/keycloak/{keycloakId}**

**Mô tả**: Lấy user theo Keycloak ID (backend internal use)

**Method**: `GET`

**URL**: `/api/users/keycloak/{keycloakId}`

**Path Parameters**:

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| keycloakId | String | ✅ Yes | Keycloak User UUID |

**Response**: Same as GET /api/users/me

---

## 🛠️ INTEGRATION GUIDE

### **Step 1: Initialize Keycloak**

```typescript
// src/auth/keycloak.ts
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'https://auth.cashbee.com.vn/',
  realm: 'cashbee',
  clientId: 'cashbee-frontend'
});

export async function initAuth() {
  const authenticated = await keycloak.init({
    onLoad: 'login-required',
    checkLoginIframe: false
  });

  if (authenticated) {
    console.log('User authenticated:', keycloak.tokenParsed);
  }

  return keycloak;
}

export default keycloak;
```

---

### **Step 2: Sync User After Login**

```typescript
// src/auth/userSync.ts
import keycloak from './keycloak';
import api from '../api/client';

export async function syncUserAfterLogin() {
  if (!keycloak.authenticated) {
    throw new Error('User not authenticated');
  }

  const userInfo = keycloak.tokenParsed!;

  // Sync user with backend
  const response = await api.syncUser({
    keycloakId: userInfo.sub!,
    username: userInfo.preferred_username!,
    email: userInfo.email!,
    fullName: userInfo.name || '',
    phone: userInfo.phone_number || null,
    referredBy: null // Set if user has referral code
  });

  console.log('User synced:', response);
  return response;
}
```

---

### **Step 3: Add API Client Methods**

```typescript
// src/api/client.ts
import axios from 'axios';
import keycloak from '../auth/keycloak';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add JWT token to all requests
apiClient.interceptors.request.use((config) => {
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export interface UserResponse {
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  fullName?: string;
  phone?: string;
  referralCode: string;
  referredBy?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserSyncRequest {
  keycloakId: string;
  username: string;
  email: string;
  fullName?: string;
  phone?: string;
  referredBy?: string;
}

/**
 * Get current user info (RECOMMENDED)
 */
export async function getMyUser(): Promise<UserResponse> {
  const response = await apiClient.get<ApiResponse<UserResponse>>(
    '/api/users/me'
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to get user');
  }

  return response.data.data;
}

/**
 * Sync user from Keycloak to database
 */
export async function syncUser(data: UserSyncRequest): Promise<UserResponse> {
  const response = await apiClient.post<ApiResponse<UserResponse>>(
    '/api/users/sync',
    data
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to sync user');
  }

  return response.data.data;
}

export default {
  getMyUser,
  syncUser
};
```

---

### **Step 4: Create Auth Hook**

```typescript
// src/hooks/useAuth.ts
import { useState, useEffect } from 'react';
import { initAuth } from '../auth/keycloak';
import { syncUserAfterLogin } from '../auth/userSync';
import { UserResponse } from '../api/client';

interface UseAuthResult {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: UserResponse | null;
  token: string | null;
  error: string | null;
}

export function useAuth(): UseAuthResult {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function init() {
      try {
        const keycloak = await initAuth();

        setIsAuthenticated(keycloak.authenticated || false);
        setToken(keycloak.token || null);

        if (keycloak.authenticated) {
          // Sync user with backend
          const userData = await syncUserAfterLogin();
          setUser(userData);
        }
      } catch (err: any) {
        console.error('Auth initialization failed:', err);
        setError(err.message || 'Authentication failed');
      } finally {
        setIsLoading(false);
      }
    }

    init();
  }, []);

  return {
    isAuthenticated,
    isLoading,
    user,
    token,
    error
  };
}
```

---

## 💻 FRONTEND EXAMPLES

### **Example 1: Complete Login Flow**

```typescript
// src/App.tsx
import React from 'react';
import { useAuth } from './hooks/useAuth';
import Dashboard from './pages/Dashboard';

function App() {
  const { isAuthenticated, isLoading, user, error } = useAuth();

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Authentication Error</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>
          Retry
        </button>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="login-container">
        <p>Redirecting to login...</p>
      </div>
    );
  }

  return (
    <div className="app">
      <header>
        <h1>Welcome, {user?.fullName || user?.username}!</h1>
        <p>Referral Code: {user?.referralCode}</p>
      </header>
      <Dashboard />
    </div>
  );
}

export default App;
```

---

### **Example 2: User Profile Component**

```typescript
// src/components/UserProfile.tsx
import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { UserResponse } from '../api/client';

function UserProfile() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    try {
      const data = await api.getMyUser();
      setUser(data);
    } catch (error) {
      console.error('Failed to load user:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading profile...</div>;
  if (!user) return <div>No user found</div>;

  return (
    <div className="user-profile">
      <h2>Profile Information</h2>

      <div className="profile-field">
        <label>Username:</label>
        <span>{user.username}</span>
      </div>

      <div className="profile-field">
        <label>Email:</label>
        <span>{user.email}</span>
      </div>

      <div className="profile-field">
        <label>Full Name:</label>
        <span>{user.fullName || 'Not set'}</span>
      </div>

      <div className="profile-field">
        <label>Phone:</label>
        <span>{user.phone || 'Not set'}</span>
      </div>

      <div className="profile-field highlight">
        <label>Your Referral Code:</label>
        <span className="referral-code">{user.referralCode}</span>
        <button onClick={() => copyToClipboard(user.referralCode)}>
          Copy
        </button>
      </div>

      {user.referredBy && (
        <div className="profile-field">
          <label>Referred By:</label>
          <span>{user.referredBy}</span>
        </div>
      )}

      <div className="profile-field">
        <label>Status:</label>
        <span className={`status ${user.status.toLowerCase()}`}>
          {user.status}
        </span>
      </div>

      <div className="profile-field">
        <label>Member Since:</label>
        <span>{new Date(user.createdAt).toLocaleDateString('vi-VN')}</span>
      </div>
    </div>
  );
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text);
  alert('Referral code copied!');
}

export default UserProfile;
```

---

### **Example 3: Referral Code Display**

```typescript
// src/components/ReferralCard.tsx
import React from 'react';
import { UserResponse } from '../api/client';

interface ReferralCardProps {
  user: UserResponse;
}

function ReferralCard({ user }: ReferralCardProps) {
  const referralLink = `https://cashbee.com?ref=${user.referralCode}`;

  const copyReferralCode = () => {
    navigator.clipboard.writeText(user.referralCode);
    alert('Referral code copied!');
  };

  const shareReferralLink = () => {
    navigator.clipboard.writeText(referralLink);
    alert('Referral link copied!');
  };

  return (
    <div className="referral-card">
      <h3>📢 Invite Friends & Earn</h3>

      <div className="referral-code-section">
        <label>Your Referral Code</label>
        <div className="code-display">
          <span className="code">{user.referralCode}</span>
          <button onClick={copyReferralCode}>Copy</button>
        </div>
      </div>

      <div className="referral-link-section">
        <label>Referral Link</label>
        <div className="link-display">
          <input
            type="text"
            value={referralLink}
            readOnly
          />
          <button onClick={shareReferralLink}>Share</button>
        </div>
      </div>

      <div className="referral-info">
        <p>
          Share your code with friends and earn 5% commission
          on their cashback earnings!
        </p>
      </div>
    </div>
  );
}

export default ReferralCard;
```

---

## ⚠️ ERROR HANDLING

### **Common Errors**

| Error Code | HTTP Status | Description | Solution |
|------------|-------------|-------------|----------|
| `USER_NOT_FOUND` | 404 | User not found in database | Call POST /api/users/sync first |
| `UNAUTHORIZED` | 401 | No authentication token | Login via Keycloak |
| `FORBIDDEN` | 403 | User account not active | Contact support |
| `VALIDATION_ERROR` | 400 | Invalid request data | Check required fields |

### **Error Handling Example**

```typescript
async function loadUserSafely() {
  try {
    const user = await api.getMyUser();
    return { success: true, data: user, error: null };

  } catch (error: any) {
    if (error.response?.status === 404) {
      // User not synced yet - sync now
      try {
        const synced = await syncUserAfterLogin();
        return { success: true, data: synced, error: null };
      } catch (syncError: any) {
        return {
          success: false,
          data: null,
          error: 'Failed to sync user'
        };
      }
    }

    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login';
      return {
        success: false,
        data: null,
        error: 'Please login'
      };
    }

    return {
      success: false,
      data: null,
      error: error.message || 'Failed to load user'
    };
  }
}
```

---

## 🎉 SUMMARY

### **Key Points**:

1. ✅ **GET /api/users/me** - Lấy user hiện tại (no userId needed!)
2. ✅ **POST /api/users/sync** - Sync user sau khi login
3. ✅ **Complete auth flow** - Keycloak + Backend integration
4. ✅ **Referral system** - Automatic referral code generation
5. ✅ **Type-safe** - Full TypeScript support

### **Integration Workflow**:

```
1. User logs in via Keycloak
   ↓
2. Frontend calls POST /api/users/sync
   ↓
3. Backend creates/updates User + Wallet
   ↓
4. Frontend can now use GET /api/users/me
   ↓
5. Use user.id for other APIs (or just use /me endpoints!)
```

### **Best Practices**:

- ✅ Always sync user after login
- ✅ Use `GET /api/users/me` instead of storing userId
- ✅ Handle 404 errors (user not synced yet)
- ✅ Refresh token before expiration
- ✅ Display referral code prominently

---

## 📚 RELATED DOCUMENTATION

- Wallet API: `API_WALLET_DOCUMENTATION.md`
- Main API Guide: `FE_GUIDE_CSV_IMPORT_CASHBACK.md`
- Keycloak Setup: (link to Keycloak docs)

---

**Last Updated**: 2025-11-04
**Version**: 1.0
**Status**: ✅ Production Ready
