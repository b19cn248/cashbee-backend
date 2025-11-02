# CashBee Backend - Frontend API Documentation

**Version:** 2.0
**Last Updated:** 2025-10-30
**Base URL:** `http://localhost:8080` (Development) | `https://api.cashbee.com` (Production)

---

## 📋 Table of Contents

1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Authentication & Authorization](#2-authentication--authorization)
3. [API Response Format](#3-api-response-format)
4. [User Management APIs](#4-user-management-apis)
5. [Wallet Management APIs](#5-wallet-management-apis)
6. [Transaction APIs](#6-transaction-apis)
7. [Payout Management APIs](#7-payout-management-apis)
8. [Admin Dashboard APIs](#8-admin-dashboard-apis)
9. [Data Models](#9-data-models)
10. [Enums & Constants](#10-enums--constants)
11. [Business Flows](#11-business-flows)
12. [Error Handling](#12-error-handling)
13. [Testing Examples](#13-testing-examples)

---

## 1. Tổng quan hệ thống

### 1.1. Mô tả dự án

**CashBee** là nền tảng hoàn tiền (cashback) cho người dùng mua hàng qua liên kết tiếp thị liên kết (Affiliate Link).

**Chức năng chính:**
- User đăng ký/đăng nhập qua **Keycloak** (OAuth2/OIDC)
- User xem số dư ví, lịch sử giao dịch
- User yêu cầu rút tiền (payout)
- Admin import dữ liệu đơn hàng từ Shopee
- Hệ thống tự động tính cashback và cập nhật ví

### 1.2. Kiến trúc hệ thống

```
┌──────────────┐
│   Frontend   │ (React/Vue/Angular)
│   (SPA/Web)  │
└──────┬───────┘
       │ REST API (HTTPS)
       │ Bearer Token (JWT)
       ▼
┌──────────────┐
│   Keycloak   │ (OAuth2/OIDC)
│ (Auth Server)│
└──────┬───────┘
       │ JWT Validation
       ▼
┌──────────────┐
│    Backend   │ (Spring Boot 3.4.1)
│   (REST API) │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   MySQL DB   │ (MySQL 8.0+)
└──────────────┘
```

### 1.3. Tech Stack Backend

- **Framework:** Spring Boot 3.4.1
- **Language:** Java 17
- **Architecture:** Hexagonal (Ports & Adapters)
- **Database:** MySQL 8.0+
- **Migration:** Liquibase
- **Authentication:** Keycloak (OAuth2/OIDC)
- **API Documentation:** SpringDoc OpenAPI (Swagger)

---

## 2. Authentication & Authorization

### 2.1. Keycloak Integration

Backend sử dụng **Keycloak** làm Identity Provider (IdP) cho authentication & authorization.

**Authentication Flow:**

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Keycloak
    participant Backend

    User->>Frontend: Click "Login"
    Frontend->>Keycloak: Redirect to login page
    Keycloak->>User: Show login form
    User->>Keycloak: Enter credentials
    Keycloak->>Frontend: Return JWT Access Token
    Frontend->>Backend: API call with Authorization: Bearer {token}
    Backend->>Backend: Validate JWT signature & claims
    Backend->>Backend: Extract user info (keycloakId, roles)
    Backend->>Backend: Sync user to local DB (if first login)
    Backend->>Frontend: API Response
```

### 2.2. JWT Token Format

**Header:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**JWT Claims (Example):**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "preferred_username": "john_doe",
  "email": "john@example.com",
  "given_name": "John",
  "family_name": "Doe",
  "realm_access": {
    "roles": ["USER"]
  },
  "exp": 1698765432,
  "iat": 1698765132
}
```

**Claims giải thích:**
- `sub`: Keycloak User ID (UUID) - đây là primary identifier
- `preferred_username`: Username
- `email`: Email address
- `realm_access.roles`: User roles (`USER`, `ADMIN`)

### 2.3. Roles & Permissions

| Role | Mô tả | Permissions |
|------|-------|-------------|
| `USER` | Người dùng thông thường | View own wallet, request payout, view own transactions |
| `ADMIN` | Quản trị viên | All USER permissions + manage payouts, view statistics, audit logs |

### 2.4. User Sync Process

**Khi nào user được sync?**
- Lần đầu tiên user login (tạo mới user + wallet)
- Khi gọi API `/api/users/sync` sau mỗi lần login

**Dữ liệu được sync:**
- Keycloak ID (`sub` claim)
- Username (`preferred_username`)
- Email (`email`)
- Full Name (`given_name` + `family_name`)

**Lưu ý quan trọng:**
- Backend **KHÔNG** lưu password
- Backend **KHÔNG** quản lý authentication
- Backend chỉ lưu business data và sync từ Keycloak

---

## 3. API Response Format

### 3.1. Standard Response Wrapper

**Tất cả API responses** đều được wrap trong format sau:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2025-10-30T10:30:00"
}
```

**Fields:**
- `success` (boolean): `true` nếu thành công, `false` nếu lỗi
- `message` (string): Message mô tả kết quả
- `data` (object/array/null): Dữ liệu trả về
- `timestamp` (string): Timestamp của response (ISO 8601)

### 3.2. Success Response Examples

**Single Object:**
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com"
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

**Array/List:**
```json
{
  "success": true,
  "message": "Transactions retrieved successfully",
  "data": [
    { "id": 1, "type": "CASHBACK", "amount": 50000 },
    { "id": 2, "type": "WITHDRAW", "amount": -100000 }
  ],
  "timestamp": "2025-10-30T10:30:00"
}
```

**Paginated Response:**
```json
{
  "success": true,
  "message": "Payouts retrieved successfully",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

### 3.3. Error Response Format

```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": "Invalid email format"
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

---

## 4. User Management APIs

### 4.1. Sync User from Keycloak

**Description:** Đồng bộ user từ Keycloak về local database. Gọi API này sau mỗi lần login.

**Endpoint:** `POST /api/users/sync`

**Authorization:** `Bearer {jwt_token}` (Optional - có thể gọi anonymous)

**Request Body:**
```json
{
  "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "phone": "0901234567",
  "referredBy": "CB123456"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| keycloakId | string | ✅ Yes | Keycloak User ID (UUID from JWT `sub` claim) |
| username | string | ✅ Yes | Username from Keycloak |
| email | string | ✅ Yes | Email address (must be valid email format) |
| fullName | string | ❌ No | User's full name |
| phone | string | ❌ No | Phone number (Vietnamese format: 10-11 digits) |
| referredBy | string | ❌ No | Referral code của người giới thiệu (nếu có) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User synced successfully",
  "data": {
    "id": 1,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567",
    "referralCode": "CB789012",
    "referredBy": "CB123456",
    "status": "ACTIVE",
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:00:00"
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

**Business Logic:**
1. Kiểm tra user có tồn tại trong DB không (by `keycloakId`)
2. **Nếu chưa tồn tại:**
   - Tạo mới user với thông tin từ Keycloak
   - Generate unique `referralCode` (format: `CBxxxxxx`)
   - Tạo `user_wallet` với balance = 0
   - Link với referrer nếu có `referredBy`
3. **Nếu đã tồn tại:**
   - Update `lastLoginAt` timestamp
   - Update email/username nếu thay đổi trên Keycloak
4. Return user data

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid input (email format, phone format, etc.) |
| 409 | `DUPLICATE_ENTITY` | User already exists with different keycloakId |
| 500 | `INTERNAL_ERROR` | Server error |

**Frontend Integration:**
```javascript
// After Keycloak login success
const keycloak = ... // Keycloak instance
const token = keycloak.token;
const tokenParsed = keycloak.tokenParsed;

// Call sync API
const response = await fetch('/api/users/sync', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    keycloakId: tokenParsed.sub,
    username: tokenParsed.preferred_username,
    email: tokenParsed.email,
    fullName: `${tokenParsed.given_name} ${tokenParsed.family_name}`,
    referredBy: localStorage.getItem('referralCode') // From URL param or storage
  })
});

const data = await response.json();
if (data.success) {
  // Save user info to state/store
  const user = data.data;
  console.log('User synced:', user);
}
```

---

### 4.2. Get User by Keycloak ID

**Description:** Lấy thông tin user theo Keycloak ID.

**Endpoint:** `GET /api/users/keycloak/{keycloakId}`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| keycloakId | string | Keycloak User ID (UUID) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567",
    "referralCode": "CB789012",
    "referredBy": "CB123456",
    "status": "ACTIVE",
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:00:00"
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 404 | `NOT_FOUND` | User not found with given keycloakId |
| 500 | `INTERNAL_ERROR` | Server error |

---

## 5. Wallet Management APIs

### 5.1. Get User Wallet

**Description:** Lấy thông tin ví của user theo User ID.

**Endpoint:** `GET /api/wallets/user/{userId}`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | long | User ID (from local database, not keycloakId) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Wallet retrieved successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 150000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 100000.00,
    "totalEarned": 500000.00,
    "totalWithdrawn": 200000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:30:00"
  },
  "timestamp": "2025-10-30T10:30:00"
}
```

**Response Fields Giải thích:**
| Field | Type | Description |
|-------|------|-------------|
| id | long | Wallet ID |
| userId | long | User ID (owner) |
| balance | decimal | **Số dư khả dụng** - có thể rút về |
| pendingBalance | decimal | **Số dư chờ xác nhận** - cashback chưa được confirm |
| lockedBalance | decimal | **Số dư bị khóa** - đang chờ xử lý payout request |
| totalEarned | decimal | **Tổng kiếm được** (lifetime) |
| totalWithdrawn | decimal | **Tổng đã rút** (lifetime) |

**Computed Values:**
- **Total Balance** = `balance + pendingBalance + lockedBalance`
- **Available to Withdraw** = `balance` (chỉ số dư khả dụng mới rút được)

**Business Logic:**
1. Tìm wallet theo `userId`
2. Return wallet information
3. Nếu wallet không tồn tại → return 404

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | User không có quyền xem wallet của người khác |
| 404 | `NOT_FOUND` | Wallet not found for user |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 5.2. Add Pending Balance

**Description:** Thêm số dư vào pending balance (chưa xác nhận). Dùng khi cashback được tính nhưng chưa confirm.

**Endpoint:** `POST /api/wallets/pending`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER` or `ADMIN`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 50000.00,
  "description": "Cashback from order #12345"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | long | ✅ Yes | User ID |
| amount | decimal | ✅ Yes | Amount to add (must be > 0) |
| description | string | ❌ No | Description for transaction log |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Pending balance added successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 150000.00,
    "pendingBalance": 100000.00,
    "lockedBalance": 100000.00,
    "totalEarned": 550000.00,
    "totalWithdrawn": 200000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:35:00"
  },
  "timestamp": "2025-10-30T10:35:00"
}
```

**Business Logic:**
1. Validate amount > 0
2. Tìm wallet theo userId
3. Tăng `pendingBalance` += amount
4. Tăng `totalEarned` += amount
5. Update wallet
6. Return updated wallet

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Amount must be greater than 0 |
| 404 | `NOT_FOUND` | Wallet not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 5.3. Confirm Pending Balance

**Description:** Xác nhận pending balance và chuyển sang available balance. Dùng khi cashback được confirm bởi affiliate network.

**Endpoint:** `POST /api/wallets/confirm`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 50000.00,
  "description": "Confirmed cashback from order #12345"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Pending balance confirmed successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 200000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 100000.00,
    "totalEarned": 550000.00,
    "totalWithdrawn": 200000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:40:00"
  },
  "timestamp": "2025-10-30T10:40:00"
}
```

**Business Logic:**
1. Validate amount > 0
2. Validate `pendingBalance >= amount`
3. Giảm `pendingBalance` -= amount
4. Tăng `balance` += amount (available)
5. Update wallet
6. Return updated wallet

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Amount exceeds pending balance |
| 403 | `FORBIDDEN` | Admin role required |
| 404 | `NOT_FOUND` | Wallet not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 5.4. Lock Balance

**Description:** Khóa số dư khi user request payout. Chuyển từ available balance sang locked balance.

**Endpoint:** `POST /api/wallets/lock`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 100000.00,
  "description": "Lock balance for payout request #PR20251030001"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Balance locked successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 100000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 200000.00,
    "totalEarned": 550000.00,
    "totalWithdrawn": 200000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:45:00"
  },
  "timestamp": "2025-10-30T10:45:00"
}
```

**Business Logic:**
1. Validate amount > 0
2. Validate `balance >= amount` (sufficient available balance)
3. Giảm `balance` -= amount
4. Tăng `lockedBalance` += amount
5. Update wallet
6. Return updated wallet

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `INSUFFICIENT_BALANCE` | Not enough available balance |
| 404 | `NOT_FOUND` | Wallet not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 5.5. Unlock Balance

**Description:** Mở khóa số dư khi payout bị reject hoặc cancelled. Chuyển từ locked balance về available balance.

**Endpoint:** `POST /api/wallets/unlock`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER` or `ADMIN`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 100000.00,
  "description": "Unlock balance - payout request rejected"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Balance unlocked successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 200000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 100000.00,
    "totalEarned": 550000.00,
    "totalWithdrawn": 200000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T10:50:00"
  },
  "timestamp": "2025-10-30T10:50:00"
}
```

**Business Logic:**
1. Validate amount > 0
2. Validate `lockedBalance >= amount`
3. Giảm `lockedBalance` -= amount
4. Tăng `balance` += amount (available)
5. Update wallet
6. Return updated wallet

---

### 5.6. Deduct Locked Balance

**Description:** Trừ locked balance khi payout được complete (tiền đã chuyển cho user). Locked balance sẽ bị trừ vĩnh viễn.

**Endpoint:** `POST /api/wallets/deduct`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 100000.00,
  "description": "Deduct balance - payout completed #PR20251030001"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Locked balance deducted successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 200000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 0.00,
    "totalEarned": 550000.00,
    "totalWithdrawn": 300000.00,
    "createdAt": "2025-10-30T10:00:00",
    "updatedAt": "2025-10-30T11:00:00"
  },
  "timestamp": "2025-10-30T11:00:00"
}
```

**Business Logic:**
1. Validate amount > 0
2. Validate `lockedBalance >= amount`
3. Giảm `lockedBalance` -= amount
4. Tăng `totalWithdrawn` += amount
5. Update wallet
6. Return updated wallet

---

## 6. Transaction APIs

### 6.1. Get Transaction History

**Description:** Lấy lịch sử giao dịch của user với phân trang. Sorted by `createdAt` DESC (newest first).

**Endpoint:** `GET /api/transactions/user/{userId}`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | long | User ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | ❌ No | 0 | Page number (0-indexed) |
| size | int | ❌ No | 20 | Page size (items per page) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Transactions retrieved successfully",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "walletId": 1,
      "type": "CASHBACK",
      "amount": 50000.00,
      "description": "Cashback from order #12345",
      "balanceBefore": 150000.00,
      "balanceAfter": 200000.00,
      "status": "SUCCESS",
      "createdAt": "2025-10-30T10:30:00"
    },
    {
      "id": 2,
      "userId": 1,
      "walletId": 1,
      "type": "WITHDRAW",
      "amount": -100000.00,
      "description": "Payout to Momo 0901234567",
      "balanceBefore": 200000.00,
      "balanceAfter": 100000.00,
      "status": "SUCCESS",
      "createdAt": "2025-10-30T09:00:00"
    }
  ],
  "timestamp": "2025-10-30T11:00:00"
}
```

**Transaction Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | long | Transaction ID |
| userId | long | User ID |
| walletId | long | Wallet ID |
| type | enum | Transaction type (see [Enums](#101-transaction-type)) |
| amount | decimal | Transaction amount (positive = credit, negative = debit) |
| description | string | Human-readable description |
| balanceBefore | decimal | Balance before transaction |
| balanceAfter | decimal | Balance after transaction |
| status | enum | Transaction status (see [Enums](#102-transaction-status)) |
| createdAt | datetime | When transaction was created |

**Business Logic:**
1. Tìm tất cả transactions của user (by userId)
2. Sort by createdAt DESC (newest first)
3. Apply pagination (page, size)
4. Return list of transactions

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | User can only view their own transactions |
| 404 | `NOT_FOUND` | User not found |
| 500 | `INTERNAL_ERROR` | Server error |

**Example Request:**
```bash
GET /api/transactions/user/1?page=0&size=10
```

---

## 7. Payout Management APIs

### 7.1. Create Payout Request (User)

**Description:** User tạo yêu cầu rút tiền từ wallet.

**Endpoint:** `POST /api/payouts/request`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Request Body:**
```json
{
  "userId": 1,
  "amount": 100000.00,
  "payoutMethod": "MOMO",
  "accountNumber": "0901234567",
  "accountName": "NGUYEN VAN A",
  "bankName": null
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | long | ✅ Yes | User ID requesting payout |
| amount | decimal | ✅ Yes | Amount to withdraw (min: 50,000 VND) |
| payoutMethod | enum | ✅ Yes | `MOMO`, `ZALOPAY`, or `BANK` |
| accountNumber | string | ✅ Yes | Phone number (for MOMO/ZALOPAY) or bank account number |
| accountName | string | ✅ Yes | Account holder name |
| bankName | string | ❌ No | Bank name (required if payoutMethod = BANK) |

**Response:** `201 CREATED`
```json
{
  "success": true,
  "message": "Payout request created successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "walletId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A",
    "bankName": null,
    "status": "REQUESTED",
    "requestedAt": "2025-10-30T11:00:00",
    "processedAt": null,
    "processedBy": null,
    "completedAt": null,
    "rejectionReason": null
  },
  "timestamp": "2025-10-30T11:00:00"
}
```

**Business Logic:**
1. Validate amount >= 50,000 VND (min payout)
2. Validate wallet has sufficient balance (`balance >= amount`)
3. Lock balance (call `/api/wallets/lock`)
4. Create payout request với status = `REQUESTED`
5. Return payout request

**Validation Rules:**
- Amount >= 50,000 VND
- Amount <= available balance
- For `BANK` method: bankName is required
- For `MOMO`/`ZALOPAY`: accountNumber must be valid phone (10 digits)

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid input (amount too low, missing fields, etc.) |
| 400 | `INSUFFICIENT_BALANCE` | Not enough available balance |
| 404 | `NOT_FOUND` | Wallet not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 7.2. Get User Payout Requests

**Description:** User xem danh sách các payout requests của mình.

**Endpoint:** `GET /api/payouts/user/{userId}`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | long | User ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | enum | ❌ No | null | Filter by status (REQUESTED, PROCESSING, PAID, REJECTED, CANCELLED) |
| page | int | ❌ No | 0 | Page number (0-indexed) |
| size | int | ❌ No | 20 | Page size |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payout requests retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 1,
        "walletId": 1,
        "amount": 100000.00,
        "payoutMethod": "MOMO",
        "accountNumber": "0901234567",
        "accountName": "NGUYEN VAN A",
        "bankName": null,
        "status": "REQUESTED",
        "requestedAt": "2025-10-30T11:00:00",
        "processedAt": null,
        "processedBy": null,
        "completedAt": null,
        "rejectionReason": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2025-10-30T11:30:00"
}
```

**Paginated Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| content | array | List of payout requests |
| page | int | Current page number (0-indexed) |
| size | int | Page size |
| totalElements | long | Total number of items |
| totalPages | int | Total number of pages |

**Business Logic:**
1. Find all payout requests for user
2. Apply status filter (if provided)
3. Sort by requestedAt DESC (newest first)
4. Apply pagination
5. Return paginated result

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 403 | `FORBIDDEN` | User can only view their own payout requests |
| 404 | `NOT_FOUND` | User not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 7.3. Cancel Payout Request (User)

**Description:** User hủy payout request của mình (chỉ hủy được nếu status = REQUESTED).

**Endpoint:** `PUT /api/payouts/{payoutRequestId}/cancel`

**Authorization:** `Bearer {jwt_token}` (Required - role: `USER`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| payoutRequestId | long | Payout request ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | long | ✅ Yes | User ID (for validation) |
| reason | string | ✅ Yes | Cancellation reason |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payout request cancelled successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "walletId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A",
    "bankName": null,
    "status": "CANCELLED",
    "requestedAt": "2025-10-30T11:00:00",
    "processedAt": null,
    "processedBy": null,
    "completedAt": null,
    "rejectionReason": "User changed mind"
  },
  "timestamp": "2025-10-30T11:45:00"
}
```

**Business Logic:**
1. Validate payout request belongs to user
2. Validate status = `REQUESTED` (only REQUESTED can be cancelled)
3. Update status to `CANCELLED`
4. Unlock balance (call `/api/wallets/unlock`)
5. Save rejection reason
6. Return updated payout request

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Cannot cancel (already processed) |
| 403 | `FORBIDDEN` | User can only cancel their own requests |
| 404 | `NOT_FOUND` | Payout request not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 7.4. Get All Payout Requests (Admin)

**Description:** Admin xem tất cả payout requests với filter.

**Endpoint:** `GET /api/payouts/admin/all`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | long | ❌ No | null | Filter by specific user |
| status | enum | ❌ No | null | Filter by status |
| page | int | ❌ No | 0 | Page number |
| size | int | ❌ No | 20 | Page size |

**Response:** `200 OK` (Same format as User Payout Requests - paginated)

**Business Logic:**
1. Apply filters (userId, status)
2. Sort by requestedAt DESC
3. Paginate
4. Return result

---

### 7.5. Approve Payout Request (Admin)

**Description:** Admin approve payout request. Status chuyển từ REQUESTED → PROCESSING.

**Endpoint:** `PUT /api/payouts/admin/{payoutRequestId}/approve`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| payoutRequestId | long | Payout request ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| adminId | string | ✅ Yes | Admin Keycloak ID (from JWT) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payout request approved successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "walletId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A",
    "bankName": null,
    "status": "PROCESSING",
    "requestedAt": "2025-10-30T11:00:00",
    "processedAt": "2025-10-30T12:00:00",
    "processedBy": "admin_keycloak_id",
    "completedAt": null,
    "rejectionReason": null
  },
  "timestamp": "2025-10-30T12:00:00"
}
```

**Business Logic:**
1. Validate payout request exists
2. Validate status = `REQUESTED`
3. Update status to `PROCESSING`
4. Set `processedAt` = now
5. Set `processedBy` = adminId
6. Balance remains locked (wait for completion)
7. Return updated payout request

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Cannot approve (wrong status) |
| 403 | `FORBIDDEN` | Admin role required |
| 404 | `NOT_FOUND` | Payout request not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

### 7.6. Reject Payout Request (Admin)

**Description:** Admin reject payout request. Status chuyển từ REQUESTED → REJECTED. Balance được unlock.

**Endpoint:** `PUT /api/payouts/admin/{payoutRequestId}/reject`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| payoutRequestId | long | Payout request ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| adminId | string | ✅ Yes | Admin Keycloak ID |
| reason | string | ✅ Yes | Rejection reason (required) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payout request rejected successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "walletId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A",
    "bankName": null,
    "status": "REJECTED",
    "requestedAt": "2025-10-30T11:00:00",
    "processedAt": "2025-10-30T12:00:00",
    "processedBy": "admin_keycloak_id",
    "completedAt": null,
    "rejectionReason": "Invalid bank account"
  },
  "timestamp": "2025-10-30T12:00:00"
}
```

**Business Logic:**
1. Validate status = `REQUESTED`
2. Update status to `REJECTED`
3. Set processedAt, processedBy, rejectionReason
4. **Unlock balance** (call `/api/wallets/unlock`)
5. Return updated payout request

---

### 7.7. Complete Payout Request (Admin)

**Description:** Admin complete payout sau khi đã chuyển tiền thành công. Status chuyển từ PROCESSING → PAID. Locked balance sẽ bị deduct.

**Endpoint:** `PUT /api/payouts/admin/{payoutRequestId}/complete`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| payoutRequestId | long | Payout request ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| adminId | string | ✅ Yes | Admin Keycloak ID |
| transactionReference | string | ❌ No | External transaction reference (Momo/Bank transaction ID) |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payout request completed successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "walletId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A",
    "bankName": null,
    "status": "PAID",
    "requestedAt": "2025-10-30T11:00:00",
    "processedAt": "2025-10-30T12:00:00",
    "processedBy": "admin_keycloak_id",
    "completedAt": "2025-10-30T12:30:00",
    "rejectionReason": null
  },
  "timestamp": "2025-10-30T12:30:00"
}
```

**Business Logic:**
1. Validate status = `PROCESSING`
2. Update status to `PAID`
3. Set `completedAt` = now
4. **Deduct locked balance** (call `/api/wallets/deduct`)
5. **Create transaction record** (type = WITHDRAW)
6. Return updated payout request

**Error Responses:**

| Status | Error Code | Description |
|--------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Cannot complete (wrong status) |
| 403 | `FORBIDDEN` | Admin role required |
| 404 | `NOT_FOUND` | Payout request not found |
| 500 | `INTERNAL_ERROR` | Server error |

---

## 8. Admin Dashboard APIs

### 8.1. Get System Statistics

**Description:** Lấy thống kê tổng quan toàn hệ thống cho admin dashboard.

**Endpoint:** `GET /api/admin/dashboard/statistics`

**Authorization:** `Bearer {jwt_token}` (Required - role: `ADMIN`)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "System statistics retrieved successfully",
  "data": {
    "totalUsers": 1250,
    "totalWallets": 1250,
    "totalBalance": 125000000.00,
    "totalLockedBalance": 15000000.00,
    "totalPendingBalance": 8000000.00,
    "totalEarned": 500000000.00,
    "totalWithdrawn": 350000000.00,
    "payoutsRequested": 45,
    "payoutsProcessing": 12,
    "payoutsPaid": 890,
    "payoutsRejected": 23,
    "payoutsCancelled": 15,
    "totalPayouts": 985,
    "totalTransactions": 15234
  },
  "timestamp": "2025-10-30T13:00:00"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| totalUsers | long | Tổng số users |
| totalWallets | long | Tổng số wallets |
| totalBalance | decimal | Tổng available balance toàn hệ thống |
| totalLockedBalance | decimal | Tổng locked balance toàn hệ thống |
| totalPendingBalance | decimal | Tổng pending balance toàn hệ thống |
| totalEarned | decimal | Tổng tiền đã kiếm được (lifetime) |
| totalWithdrawn | decimal | Tổng tiền đã rút (lifetime) |
| payoutsRequested | long | Số payout requests đang chờ approve |
| payoutsProcessing | long | Số payouts đang processing |
| payoutsPaid | long | Số payouts đã hoàn thành |
| payoutsRejected | long | Số payouts bị reject |
| payoutsCancelled | long | Số payouts bị cancel |
| totalPayouts | long | Tổng số payout requests |
| totalTransactions | long | Tổng số transactions |

**Business Logic:**
1. Query aggregated data from database
2. Calculate statistics from:
   - user table (count)
   - user_wallet table (sum balances)
   - payout_request table (count by status)
   - wallet_transaction table (count)
3. Return statistics

---

## 9. Data Models

### 9.1. UserResponse

**Mô tả:** Thông tin user response

```typescript
interface UserResponse {
  id: number;                    // User ID
  keycloakId: string;            // Keycloak User UUID
  username: string;              // Username
  email: string;                 // Email address
  fullName: string | null;       // Full name
  phone: string | null;          // Phone number
  referralCode: string;          // User's referral code (CBxxxxxx)
  referredBy: string | null;     // Referral code của người giới thiệu
  status: UserStatus;            // ACTIVE | BANNED | SUSPENDED
  createdAt: string;             // ISO 8601 datetime
  updatedAt: string;             // ISO 8601 datetime
}
```

---

### 9.2. WalletResponse

**Mô tả:** Thông tin ví user

```typescript
interface WalletResponse {
  id: number;                    // Wallet ID
  userId: number;                // User ID (owner)
  balance: number;               // Available balance (có thể rút)
  pendingBalance: number;        // Pending balance (chờ confirm)
  lockedBalance: number;         // Locked balance (đang payout)
  totalEarned: number;           // Total earned (lifetime)
  totalWithdrawn: number;        // Total withdrawn (lifetime)
  createdAt: string;             // ISO 8601 datetime
  updatedAt: string;             // ISO 8601 datetime
}
```

**Computed Properties:**
```typescript
// Total balance across all states
totalBalance = balance + pendingBalance + lockedBalance;

// Available to withdraw
availableToWithdraw = balance;
```

---

### 9.3. TransactionResponse

**Mô tả:** Thông tin giao dịch

```typescript
interface TransactionResponse {
  id: number;                    // Transaction ID
  userId: number;                // User ID
  walletId: number;              // Wallet ID
  type: TransactionType;         // CASHBACK | WITHDRAW | BONUS | REFERRAL | REFUND | ADJUSTMENT
  amount: number;                // Amount (positive = credit, negative = debit)
  description: string;           // Human-readable description
  balanceBefore: number;         // Balance before transaction
  balanceAfter: number;          // Balance after transaction
  status: TransactionStatus;     // SUCCESS | PENDING | FAILED | CANCELLED
  createdAt: string;             // ISO 8601 datetime
}
```

---

### 9.4. PayoutRequestResponse

**Mô tả:** Thông tin yêu cầu rút tiền

```typescript
interface PayoutRequestResponse {
  id: number;                    // Payout request ID
  userId: number;                // User ID
  walletId: number;              // Wallet ID
  amount: number;                // Amount to withdraw
  payoutMethod: PayoutMethod;    // MOMO | ZALOPAY | BANK
  accountNumber: string;         // Phone or bank account number
  accountName: string;           // Account holder name
  bankName: string | null;       // Bank name (for BANK method)
  status: PayoutStatus;          // REQUESTED | PROCESSING | PAID | REJECTED | CANCELLED
  requestedAt: string;           // ISO 8601 datetime
  processedAt: string | null;    // ISO 8601 datetime
  processedBy: string | null;    // Admin Keycloak ID
  completedAt: string | null;    // ISO 8601 datetime
  rejectionReason: string | null;// Rejection/cancellation reason
}
```

---

### 9.5. PageResponse

**Mô tả:** Paginated response wrapper

```typescript
interface PageResponse<T> {
  content: T[];                  // List of items
  page: number;                  // Current page (0-indexed)
  size: number;                  // Page size
  totalElements: number;         // Total number of items
  totalPages: number;            // Total number of pages
}
```

---

### 9.6. SystemStatisticsResponse

**Mô tả:** System statistics for admin dashboard

```typescript
interface SystemStatisticsResponse {
  // User & Wallet
  totalUsers: number;
  totalWallets: number;

  // Balance
  totalBalance: number;
  totalLockedBalance: number;
  totalPendingBalance: number;
  totalEarned: number;
  totalWithdrawn: number;

  // Payouts
  payoutsRequested: number;
  payoutsProcessing: number;
  payoutsPaid: number;
  payoutsRejected: number;
  payoutsCancelled: number;
  totalPayouts: number;

  // Transactions
  totalTransactions: number;
}
```

---

## 10. Enums & Constants

### 10.1. UserStatus

```typescript
enum UserStatus {
  ACTIVE = "ACTIVE",         // User active, có thể sử dụng hệ thống
  BANNED = "BANNED",         // User bị ban, không thể truy cập
  SUSPENDED = "SUSPENDED"    // User bị tạm khóa
}
```

---

### 10.2. PayoutStatus

```typescript
enum PayoutStatus {
  REQUESTED = "REQUESTED",     // User vừa tạo request, chờ admin review
  PROCESSING = "PROCESSING",   // Admin approved, đang xử lý chuyển tiền
  PAID = "PAID",              // Đã chuyển tiền thành công
  REJECTED = "REJECTED",      // Admin reject request
  CANCELLED = "CANCELLED"     // User hoặc admin cancel request
}
```

**Status Flow:**
```
REQUESTED → PROCESSING → PAID
    ↓           ↓
REJECTED   CANCELLED
    ↓           ↓
CANCELLED   CANCELLED
```

---

### 10.3. PayoutMethod

```typescript
enum PayoutMethod {
  MOMO = "MOMO",           // Ví Momo
  ZALOPAY = "ZALOPAY",     // Ví ZaloPay
  BANK = "BANK"            // Chuyển khoản ngân hàng
}
```

---

### 10.4. TransactionType

```typescript
enum TransactionType {
  CASHBACK = "CASHBACK",       // Cashback earned from orders
  WITHDRAW = "WITHDRAW",       // Withdrawal (payout)
  BONUS = "BONUS",            // Bonus from promotions
  REFERRAL = "REFERRAL",      // Referral rewards
  REFUND = "REFUND",          // Refund
  ADJUSTMENT = "ADJUSTMENT"   // Admin manual adjustment
}
```

---

### 10.5. TransactionStatus

```typescript
enum TransactionStatus {
  SUCCESS = "SUCCESS",     // Transaction successful
  PENDING = "PENDING",     // Transaction pending
  FAILED = "FAILED",       // Transaction failed
  CANCELLED = "CANCELLED"  // Transaction cancelled
}
```

---

### 10.6. Constants

```typescript
// Payout minimum
const MIN_PAYOUT_AMOUNT = 50000; // 50,000 VND

// Pagination defaults
const DEFAULT_PAGE_SIZE = 20;
const DEFAULT_PAGE_NUMBER = 0;

// Date format
const DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"; // ISO 8601
```

---

## 11. Business Flows

### 11.1. User Registration & Login Flow

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant KC as Keycloak
    participant BE as Backend
    participant DB as Database

    User->>FE: Click "Register/Login"
    FE->>KC: Redirect to Keycloak
    KC->>User: Show login page
    User->>KC: Enter credentials
    KC->>KC: Validate & create session
    KC->>FE: Redirect with JWT token
    FE->>FE: Store token in localStorage/sessionStorage
    FE->>BE: POST /api/users/sync (with JWT)
    BE->>BE: Validate JWT signature
    BE->>BE: Extract keycloakId, email, username
    BE->>DB: Check if user exists (by keycloakId)
    alt User NOT exists
        DB->>BE: User not found
        BE->>BE: Create new user + wallet
        BE->>BE: Generate referral code
        BE->>DB: INSERT user + user_wallet
    else User exists
        DB->>BE: User found
        BE->>BE: Update lastLoginAt, lastSyncAt
        BE->>DB: UPDATE user
    end
    BE->>FE: Return UserResponse
    FE->>FE: Save user info to state/store
    FE->>User: Show dashboard
```

**Frontend Implementation:**

```javascript
// 1. Initialize Keycloak
const keycloak = new Keycloak({
  url: 'https://keycloak.cashbee.com',
  realm: 'cashbee',
  clientId: 'cashbee-frontend'
});

// 2. Initialize Keycloak
await keycloak.init({ onLoad: 'login-required' });

// 3. Get token
const token = keycloak.token;
const tokenParsed = keycloak.tokenParsed;

// 4. Sync user to backend
const syncUser = async () => {
  const response = await fetch('/api/users/sync', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      keycloakId: tokenParsed.sub,
      username: tokenParsed.preferred_username,
      email: tokenParsed.email,
      fullName: `${tokenParsed.given_name || ''} ${tokenParsed.family_name || ''}`.trim(),
      referredBy: localStorage.getItem('referralCode') // From URL ?ref=CBxxxxxx
    })
  });

  const data = await response.json();
  if (data.success) {
    // Save user to Redux/Vuex/Context
    store.dispatch('user/setUser', data.data);
    // Fetch wallet
    fetchWallet(data.data.id);
  }
};

// 5. Fetch wallet
const fetchWallet = async (userId) => {
  const response = await fetch(`/api/wallets/user/${userId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const data = await response.json();
  if (data.success) {
    store.dispatch('wallet/setWallet', data.data);
  }
};
```

---

### 11.2. Wallet Balance Flow

```mermaid
flowchart TD
    A[User mua hàng qua affiliate link] --> B[Shopee ghi nhận conversion]
    B --> C[Admin import file Shopee]
    C --> D[BE parse file và tạo orders]
    D --> E[BE tính cashback theo policy]
    E --> F[BE thêm pending balance]
    F --> G[User xem pending balance trong ví]

    H[Shopee confirm order PAID] --> I[Admin approve order]
    I --> J[BE confirm pending balance]
    J --> K[Pending balance → Available balance]
    K --> L[User có thể request payout]
```

**API Sequence:**

1. **Admin import orders** (Not implemented yet in current backend)
2. **BE adds pending balance:**
   ```
   POST /api/wallets/pending
   {
     "userId": 1,
     "amount": 50000,
     "description": "Cashback from order #12345"
   }
   ```

3. **User checks wallet:**
   ```
   GET /api/wallets/user/1
   → pendingBalance: 50000
   ```

4. **Admin confirms order:**
   ```
   POST /api/wallets/confirm
   {
     "userId": 1,
     "amount": 50000,
     "description": "Confirmed order #12345"
   }
   ```

5. **User sees updated wallet:**
   ```
   GET /api/wallets/user/1
   → balance: 50000
   → pendingBalance: 0
   ```

---

### 11.3. Payout Request Flow

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant BE as Backend
    participant Wallet
    participant Admin

    User->>FE: Click "Request Payout"
    FE->>FE: Validate amount >= 50,000
    FE->>BE: POST /api/payouts/request
    BE->>Wallet: Check balance >= amount
    Wallet->>BE: OK
    BE->>Wallet: Lock balance
    BE->>BE: Create payout request (REQUESTED)
    BE->>FE: Return payout request
    FE->>User: Show "Request submitted"

    Admin->>BE: GET /api/payouts/admin/all
    BE->>Admin: List of REQUESTED payouts
    Admin->>BE: PUT /api/payouts/admin/{id}/approve
    BE->>BE: Update status to PROCESSING
    BE->>Admin: Approved

    Admin->>Admin: Transfer money (Momo/Bank)
    Admin->>BE: PUT /api/payouts/admin/{id}/complete
    BE->>Wallet: Deduct locked balance
    BE->>BE: Create WITHDRAW transaction
    BE->>BE: Update status to PAID
    BE->>Admin: Completed

    User->>BE: GET /api/payouts/user/{userId}
    BE->>User: Show payout status = PAID
```

**Frontend Implementation - Request Payout:**

```javascript
const requestPayout = async (userId, amount, payoutMethod, accountNumber, accountName, bankName) => {
  // 1. Validate amount
  if (amount < 50000) {
    alert('Minimum payout amount is 50,000 VND');
    return;
  }

  // 2. Check wallet balance
  const walletResponse = await fetch(`/api/wallets/user/${userId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const walletData = await walletResponse.json();

  if (walletData.data.balance < amount) {
    alert('Insufficient balance');
    return;
  }

  // 3. Create payout request
  const response = await fetch('/api/payouts/request', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      userId,
      amount,
      payoutMethod, // 'MOMO' | 'ZALOPAY' | 'BANK'
      accountNumber,
      accountName,
      bankName: payoutMethod === 'BANK' ? bankName : null
    })
  });

  const data = await response.json();
  if (data.success) {
    alert('Payout request submitted successfully!');
    // Refresh wallet & payout list
    fetchWallet(userId);
    fetchPayoutRequests(userId);
  } else {
    alert(`Error: ${data.error.details}`);
  }
};
```

---

### 11.4. Transaction History Flow

**Frontend Implementation:**

```javascript
const fetchTransactionHistory = async (userId, page = 0, size = 20) => {
  const response = await fetch(
    `/api/transactions/user/${userId}?page=${page}&size=${size}`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );

  const data = await response.json();
  if (data.success) {
    return data.data; // Array of TransactionResponse
  }
  return [];
};

// Usage in React component
const TransactionHistory = () => {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const userId = useSelector(state => state.user.id);

  useEffect(() => {
    fetchTransactionHistory(userId, page, 20).then(setTransactions);
  }, [userId, page]);

  return (
    <div>
      {transactions.map(tx => (
        <TransactionItem key={tx.id} transaction={tx} />
      ))}
      <Pagination page={page} onPageChange={setPage} />
    </div>
  );
};
```

---

## 12. Error Handling

### 12.1. Error Response Format

**Khi có lỗi, API trả về:**

```json
{
  "success": false,
  "message": "Error message for display",
  "error": {
    "code": "ERROR_CODE",
    "details": "Detailed error message"
  },
  "timestamp": "2025-10-30T13:00:00"
}
```

### 12.2. HTTP Status Codes

| Status | Meaning | When to use |
|--------|---------|-------------|
| 200 | OK | Successful GET/PUT/DELETE |
| 201 | Created | Successful POST (resource created) |
| 400 | Bad Request | Validation error, invalid input |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Valid token but insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate entity (e.g., email already exists) |
| 500 | Internal Server Error | Server error |

### 12.3. Common Error Codes

| Error Code | HTTP Status | Description | Example |
|------------|-------------|-------------|---------|
| `VALIDATION_ERROR` | 400 | Input validation failed | Invalid email format, amount < 0 |
| `UNAUTHORIZED` | 401 | Missing or invalid JWT token | Token expired, invalid signature |
| `FORBIDDEN` | 403 | Insufficient permissions | User trying to access admin API |
| `NOT_FOUND` | 404 | Resource not found | User not found, wallet not found |
| `DUPLICATE_ENTITY` | 409 | Entity already exists | Email already registered |
| `INSUFFICIENT_BALANCE` | 400 | Not enough balance | Trying to withdraw more than balance |
| `INTERNAL_ERROR` | 500 | Server error | Database error, unexpected exception |

### 12.4. Frontend Error Handling Example

```javascript
const handleApiCall = async (apiCall) => {
  try {
    const response = await apiCall();
    const data = await response.json();

    if (!response.ok || !data.success) {
      // Handle API error
      const errorCode = data.error?.code || 'UNKNOWN_ERROR';
      const errorMessage = data.message || 'An error occurred';

      switch (errorCode) {
        case 'UNAUTHORIZED':
          // Redirect to login
          keycloak.login();
          break;
        case 'FORBIDDEN':
          alert('You do not have permission to perform this action');
          break;
        case 'NOT_FOUND':
          alert('Resource not found');
          break;
        case 'VALIDATION_ERROR':
          alert(`Validation error: ${data.error.details}`);
          break;
        case 'INSUFFICIENT_BALANCE':
          alert('Insufficient balance for this operation');
          break;
        default:
          alert(errorMessage);
      }

      return null;
    }

    return data.data;
  } catch (error) {
    console.error('Network error:', error);
    alert('Network error. Please check your connection.');
    return null;
  }
};

// Usage
const user = await handleApiCall(() =>
  fetch('/api/users/sync', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ ... })
  })
);

if (user) {
  // Success
  console.log('User synced:', user);
}
```

---

## 13. Testing Examples

### 13.1. cURL Examples

**Sync User:**
```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe"
  }'
```

**Get Wallet:**
```bash
curl -X GET http://localhost:8080/api/wallets/user/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Add Pending Balance:**
```bash
curl -X POST http://localhost:8080/api/wallets/pending \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": 1,
    "amount": 50000.00,
    "description": "Cashback from order #12345"
  }'
```

**Create Payout Request:**
```bash
curl -X POST http://localhost:8080/api/payouts/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": 1,
    "amount": 100000.00,
    "payoutMethod": "MOMO",
    "accountNumber": "0901234567",
    "accountName": "NGUYEN VAN A"
  }'
```

**Get Transaction History:**
```bash
curl -X GET "http://localhost:8080/api/transactions/user/1?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Admin - Get System Statistics:**
```bash
curl -X GET http://localhost:8080/api/admin/dashboard/statistics \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

### 13.2. Postman Collection Structure

**Collection Name:** CashBee API

**Folders:**
1. **Authentication**
   - Sync User
   - Get User by Keycloak ID

2. **Wallet Management**
   - Get Wallet
   - Add Pending Balance
   - Confirm Pending Balance
   - Lock Balance
   - Unlock Balance
   - Deduct Locked Balance

3. **Transactions**
   - Get Transaction History

4. **Payouts (User)**
   - Create Payout Request
   - Get My Payouts
   - Cancel Payout Request

5. **Payouts (Admin)**
   - Get All Payouts
   - Approve Payout
   - Reject Payout
   - Complete Payout

6. **Admin Dashboard**
   - Get System Statistics

**Environment Variables:**
```json
{
  "baseUrl": "http://localhost:8080",
  "jwt_token": "YOUR_JWT_TOKEN_HERE",
  "admin_jwt_token": "ADMIN_JWT_TOKEN_HERE",
  "userId": 1,
  "keycloakId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 13.3. Test Scenarios

**Scenario 1: User Registration & First Login**
1. User login qua Keycloak → nhận JWT token
2. Frontend call `/api/users/sync` với data từ JWT
3. Backend tạo user mới + wallet
4. Frontend nhận UserResponse với `referralCode`
5. Frontend call `/api/wallets/user/{userId}` để lấy wallet
6. Frontend hiển thị dashboard với balance = 0

**Scenario 2: Add & Confirm Cashback**
1. Admin import orders (future feature)
2. Backend call `/api/wallets/pending` để thêm pending balance
3. User xem wallet → thấy `pendingBalance` tăng
4. Admin confirm order
5. Backend call `/api/wallets/confirm` để confirm balance
6. User xem wallet → thấy `balance` tăng, `pendingBalance` giảm

**Scenario 3: Request Payout**
1. User check wallet balance
2. User click "Request Payout"
3. Frontend validate amount >= 50,000
4. Frontend call `/api/payouts/request`
5. Backend lock balance
6. User xem wallet → thấy `lockedBalance` tăng, `balance` giảm
7. Admin review payout request
8. Admin approve → status = PROCESSING
9. Admin transfer money
10. Admin complete → status = PAID
11. Backend deduct locked balance
12. User xem wallet → thấy `lockedBalance` giảm, `totalWithdrawn` tăng

**Scenario 4: Cancel Payout**
1. User tạo payout request
2. User click "Cancel"
3. Frontend call `/api/payouts/{id}/cancel`
4. Backend unlock balance
5. User xem wallet → thấy balance restored

---

## 14. Swagger UI

**Access Swagger Documentation:**

```
http://localhost:8080/swagger-ui.html
```

**Features:**
- Interactive API documentation
- Try out APIs directly from browser
- View request/response schemas
- View all endpoints grouped by tags

---

## 15. Best Practices cho Frontend Development

### 15.1. Authentication

```javascript
// 1. Always include JWT token in headers
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${keycloak.token}`
};

// 2. Handle token refresh
keycloak.updateToken(30).then(refreshed => {
  if (refreshed) {
    console.log('Token refreshed');
  }
}).catch(() => {
  console.log('Failed to refresh token');
  keycloak.login();
});

// 3. Check token expiry before API calls
const ensureTokenValid = async () => {
  try {
    await keycloak.updateToken(30);
    return keycloak.token;
  } catch {
    keycloak.login();
    throw new Error('Token expired');
  }
};
```

### 15.2. Error Handling

```javascript
// Centralized error handler
const handleApiError = (error, response) => {
  if (!response.ok) {
    switch (response.status) {
      case 401:
        keycloak.login();
        break;
      case 403:
        showNotification('You do not have permission', 'error');
        break;
      case 404:
        showNotification('Resource not found', 'error');
        break;
      case 400:
        showNotification(error.message, 'error');
        break;
      default:
        showNotification('An error occurred', 'error');
    }
  }
};
```

### 15.3. State Management

```javascript
// Redux store structure
const initialState = {
  user: {
    data: null,
    loading: false,
    error: null
  },
  wallet: {
    data: null,
    loading: false,
    error: null
  },
  transactions: {
    data: [],
    loading: false,
    error: null,
    pagination: {
      page: 0,
      size: 20,
      totalPages: 0
    }
  },
  payouts: {
    data: [],
    loading: false,
    error: null,
    pagination: {
      page: 0,
      size: 20,
      totalPages: 0
    }
  }
};
```

### 15.4. Polling for Real-time Updates

```javascript
// Poll wallet balance every 30 seconds
useEffect(() => {
  const interval = setInterval(() => {
    fetchWallet(userId);
  }, 30000);

  return () => clearInterval(interval);
}, [userId]);

// Poll payout status while PROCESSING
useEffect(() => {
  if (payout.status === 'PROCESSING') {
    const interval = setInterval(() => {
      fetchPayoutStatus(payout.id);
    }, 10000);

    return () => clearInterval(interval);
  }
}, [payout.status]);
```

---

## 16. FAQ & Troubleshooting

### Q1: JWT token expired, phải làm gì?

**A:** Frontend phải handle token refresh tự động:
```javascript
keycloak.updateToken(30).catch(() => {
  keycloak.login();
});
```

### Q2: API trả về 403 Forbidden?

**A:** Kiểm tra:
- JWT token có hợp lệ không?
- User có role đúng không? (USER/ADMIN)
- Endpoint có yêu cầu admin role không?

### Q3: Làm sao để test API khi chưa có Keycloak?

**A:** Có 2 cách:
1. Setup Keycloak local (recommended)
2. Tạm thời disable security trong Spring Boot (dev only)

### Q4: Balance không update sau khi confirm?

**A:** Kiểm tra:
1. API `/api/wallets/confirm` có được gọi thành công không?
2. Check response - `balance` có tăng, `pendingBalance` có giảm không?
3. Refresh lại wallet data từ API

### Q5: Payout request không tạo được?

**A:** Kiểm tra:
1. Amount >= 50,000 VND?
2. Wallet balance >= amount?
3. PayoutMethod và accountNumber có hợp lệ không?

---

## 17. Contact & Support

**Backend Team:**
- Email: backend@cashbee.com
- Slack: #cashbee-backend

**API Issues:**
- Create issue on GitHub: https://github.com/cashbee/backend/issues

**Documentation:**
- Swagger UI: http://localhost:8080/swagger-ui.html
- This Document: FRONTEND_API_DOCUMENTATION.md

---

**🎉 Happy Coding! 🎉**

*Last Updated: 2025-10-30*
*Version: 2.0*
