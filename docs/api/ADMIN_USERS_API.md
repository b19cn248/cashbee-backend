# Admin Users API Documentation

> Version: 1.2.0
> Last Updated: 2025-12-18
> Base URL: `https://api.cashbee.vn` (Production) | `http://localhost:8080` (Development)

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Common Response Format](#common-response-format)
4. [Error Codes](#error-codes)
5. [Endpoints](#endpoints)
   - [Get All Users](#1-get-all-users)
   - [Get User Orders by ID](#2-get-user-orders-by-id)
6. [Data Models](#data-models)
7. [Integration Examples](#integration-examples)
8. [Changelog](#changelog)

---

## Overview

API cho phep Admin quan ly users trong he thong CashBee, bao gom:
- Lay danh sach tat ca users voi phan trang
- Loc users theo trang thai (ACTIVE, SUSPENDED, BANNED)
- Tim kiem users theo email hoac username
- **[NEW v1.2.0]** Loc users theo khoang thoi gian phat sinh don hang
- Xem thong tin chi tiet cua tung user
- Xem danh sach orders cua bat ky user nao

### Use Cases
- Admin xem danh sach tat ca nguoi dung
- Admin tim kiem user theo email de ho tro
- Admin loc users theo trang thai de quan ly
- **[NEW v1.2.0]** Admin loc users co don hang trong ngay hom nay
- **[NEW v1.2.0]** Admin loc users co don hang trong 3 ngay gan day
- **[NEW v1.2.0]** Admin loc users co don hang trong khoang thoi gian bat ky
- Customer Support tra cuu thong tin khach hang
- Admin xem orders cua mot user cu the de ho tro hoac kiem tra

### Access Control

| Role | Access |
|------|--------|
| **ADMIN** | Full access |
| **SUPPORT** | Read-only access |
| **USER** | No access (403 Forbidden) |

---

## Authentication

API yeu cau JWT token voi role ADMIN trong header:

```http
Authorization: Bearer <admin_access_token>
```

### Token Requirements

JWT token phai co:
- `realm_access.roles` chua `ADMIN` hoac `admin`
- Token chua het han
- Token duoc sign boi Keycloak server

---

## Common Response Format

### Success Response
```json
{
  "success": true,
  "message": null,
  "data": { ... },
  "errorCode": null,
  "timestamp": "2025-11-25T11:30:00.000"
}
```

### Error Response
```json
{
  "success": false,
  "message": "Access denied. Admin role required.",
  "data": null,
  "errorCode": "FORBIDDEN",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Tham so khong hop le |
| `UNAUTHORIZED` | 401 | Chua dang nhap hoac token het han |
| `FORBIDDEN` | 403 | Khong co quyen truy cap (khong phai Admin) |
| `INTERNAL_ERROR` | 500 | Loi server |

---

## Endpoints

### 1. Get All Users

Lay danh sach tat ca users trong he thong voi phan trang, loc va tim kiem.

#### Request

```http
GET /api/admin/users
Authorization: Bearer <admin_access_token>
```

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `status` | String | No | null | Loc theo trang thai: `ACTIVE`, `SUSPENDED`, `BANNED` |
| `search` | String | No | null | Tim kiem theo email hoac username |
| `orderFromDate` | String | No | null | **[NEW v1.2.0]** Loc users co don hang TU ngay nay (format: `YYYY-MM-DD`) |
| `orderToDate` | String | No | null | **[NEW v1.2.0]** Loc users co don hang DEN ngay nay (format: `YYYY-MM-DD`) |
| `page` | Integer | No | 0 | So trang (bat dau tu 0) |
| `size` | Integer | No | 20 | So users moi trang (toi da 100) |

#### Filter Priority

Khi su dung nhieu filter cung luc, he thong ap dung theo thu tu uu tien sau:

1. **Order Date Filter** (cao nhat) - Neu co `orderFromDate` hoac `orderToDate`
2. **Search Filter** - Neu co `search`
3. **Status Filter** - Neu co `status`
4. **No Filter** - Lay tat ca users

> **Luu y:** Hien tai cac filter hoat dong doc lap. Neu can ket hop (vi du: users ACTIVE co don hang hom nay), hay lien he Backend team de ho tro.

#### Example Requests

**Lay tat ca users (trang dau tien):**
```bash
curl -X GET "http://localhost:8080/api/admin/users" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay users ACTIVE:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?status=ACTIVE" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Tim kiem user theo email:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?search=john@gmail.com" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay trang thu 2 voi 50 users:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?page=1&size=50" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**[NEW v1.2.0] Lay users co don hang hom nay (18/12/2025):**
```bash
curl -X GET "http://localhost:8080/api/admin/users?orderFromDate=2025-12-18&orderToDate=2025-12-18" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**[NEW v1.2.0] Lay users co don hang trong 3 ngay gan day:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?orderFromDate=2025-12-15&orderToDate=2025-12-18" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**[NEW v1.2.0] Lay users co don hang tu ngay 1/12 den nay:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?orderFromDate=2025-12-01" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**[NEW v1.2.0] Lay users co don hang truoc ngay 15/12:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?orderToDate=2025-12-15" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**[NEW v1.2.0] Lay users co don hang thang 11/2025:**
```bash
curl -X GET "http://localhost:8080/api/admin/users?orderFromDate=2025-11-01&orderToDate=2025-11-30" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Ket hop nhieu filter (luu y filter priority):**
```bash
curl -X GET "http://localhost:8080/api/admin/users?status=ACTIVE&search=nguyen&page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Success Response

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": 1,
        "keycloakId": "550e8400-e29b-41d4-a716-446655440001",
        "username": "nguyenvana",
        "email": "nguyenvana@gmail.com",
        "fullName": "Nguyen Van A",
        "phone": "0901234567",
        "referralCode": "NVANA001",
        "referredBy": null,
        "status": "ACTIVE",
        "createdAt": "2025-10-15T08:30:00",
        "updatedAt": "2025-11-20T14:25:00",
        "accountNumber": "1234567890",
        "accountName": "NGUYEN VAN A",
        "bankCode": "VPBANK",
        "bankName": "Ngan hang TMCP Viet Nam Thinh Vuong"
      },
      {
        "id": 2,
        "keycloakId": "550e8400-e29b-41d4-a716-446655440002",
        "username": "tranthib",
        "email": "tranthib@gmail.com",
        "fullName": "Tran Thi B",
        "phone": "0912345678",
        "referralCode": "TTB002",
        "referredBy": "NVANA001",
        "status": "ACTIVE",
        "createdAt": "2025-10-20T10:15:00",
        "updatedAt": "2025-11-18T09:45:00",
        "accountNumber": null,
        "accountName": null,
        "bankCode": null,
        "bankName": null
      },
      {
        "id": 3,
        "keycloakId": "550e8400-e29b-41d4-a716-446655440003",
        "username": "levanc",
        "email": "levanc@gmail.com",
        "fullName": "Le Van C",
        "phone": "0923456789",
        "referralCode": "LVC003",
        "referredBy": "NVANA001",
        "status": "SUSPENDED",
        "createdAt": "2025-11-01T14:00:00",
        "updatedAt": "2025-11-22T16:30:00",
        "accountNumber": "9876543210",
        "accountName": "LE VAN C",
        "bankCode": "ACB",
        "bankName": "Ngan hang A Chau"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "errorCode": null,
  "timestamp": "2025-11-25T11:30:00.000"
}
```

#### Error Responses

**Unauthorized (Token missing or invalid):**
```http
HTTP/1.1 401 Unauthorized
```
```json
{
  "success": false,
  "message": "Token is missing or invalid",
  "data": null,
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Forbidden (Not Admin):**
```http
HTTP/1.1 403 Forbidden
```
```json
{
  "success": false,
  "message": "Access denied. Admin role required.",
  "data": null,
  "errorCode": "FORBIDDEN",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Invalid Status:**
```http
HTTP/1.1 400 Bad Request
```
```json
{
  "success": false,
  "message": "Invalid status value. Allowed: ACTIVE, SUSPENDED, BANNED",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Empty Result (Not an error):**
```http
HTTP/1.1 200 OK
```
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "errorCode": null,
  "timestamp": "2025-11-25T11:30:00.000"
}
```

---

### 2. Get User Orders by ID

Lay danh sach orders cua mot user cu the theo userId. Chi Admin moi duoc su dung endpoint nay.

#### Use Cases

- Admin xem orders cua mot user de ho tro khach hang
- Customer Support kiem tra tinh trang don hang cua user
- Admin theo doi hoa hong cua mot user cu the

#### Request

```http
GET /api/users/{userId}/orders
Authorization: Bearer <admin_access_token>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | Long | Yes | ID cua user can xem orders |

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `status` | String | No | null | Loc theo trang thai: `PENDING`, `APPROVED`, `PAID`, `CANCELLED` |
| `page` | Integer | No | 0 | So trang (bat dau tu 0) |
| `size` | Integer | No | 10 | So orders moi trang (toi da 50) |

#### Example Requests

**Lay tat ca orders cua user 100:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay orders PENDING cua user 100:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders?status=PENDING" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay trang thu 2 voi 20 orders:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders?page=1&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Ket hop nhieu filter:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders?status=APPROVED&page=0&size=15" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Success Response

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": 1,
        "platformId": 1,
        "platformName": "Shopee",
        "userId": 100,
        "clickId": 5,
        "orderId": "241125ABCDEF123",
        "orderStatus": "PENDING",
        "productName": "iPhone 15 Pro Max 256GB",
        "productPrice": 29990000.00,
        "commissionAmount": 299900.00,
        "currency": "VND",
        "orderTime": "2025-11-25T10:30:00",
        "confirmTime": null,
        "paidTime": null,
        "source": "IMPORT",
        "importBatchId": 15,
        "createdAt": "2025-11-25T11:00:00",
        "updatedAt": "2025-11-25T11:00:00",
        "items": [
          {
            "id": 101,
            "orderId": 1,
            "itemId": "12345678901",
            "itemName": "iPhone 15 Pro Max 256GB - Titan Tu Nhien",
            "quantity": 1,
            "actualAmount": 29990000.00,
            "itemCommission": 299900.00,
            "shopId": "123456",
            "shopName": "Apple Official Store",
            "categoryLv1": "Dien thoai & Phu kien",
            "categoryLv2": "Dien thoai",
            "categoryLv3": "Smartphone",
            "imgUrl": "https://cf.shopee.vn/file/abc123.jpg",
            "brandCommissionRate": 1.0,
            "platformCommissionRate": 0.5,
            "status": "PENDING",
            "createdAt": "2025-11-25T11:00:00"
          }
        ],
        "totalItems": 1,
        "canReceiveCashback": true
      },
      {
        "id": 2,
        "platformId": 1,
        "platformName": "Shopee",
        "userId": 100,
        "clickId": 3,
        "orderId": "241124XYZ789456",
        "orderStatus": "APPROVED",
        "productName": "Samsung Galaxy S24 Ultra",
        "productPrice": 25990000.00,
        "commissionAmount": 259900.00,
        "currency": "VND",
        "orderTime": "2025-11-24T15:20:00",
        "confirmTime": "2025-11-25T09:00:00",
        "paidTime": null,
        "source": "IMPORT",
        "importBatchId": 14,
        "createdAt": "2025-11-24T16:00:00",
        "updatedAt": "2025-11-25T09:00:00",
        "items": [
          {
            "id": 102,
            "orderId": 2,
            "itemId": "98765432109",
            "itemName": "Samsung Galaxy S24 Ultra 512GB - Titanium Gray",
            "quantity": 1,
            "actualAmount": 25990000.00,
            "itemCommission": 259900.00,
            "shopId": "654321",
            "shopName": "Samsung Official Store",
            "categoryLv1": "Dien thoai & Phu kien",
            "categoryLv2": "Dien thoai",
            "categoryLv3": "Smartphone",
            "imgUrl": "https://cf.shopee.vn/file/xyz789.jpg",
            "brandCommissionRate": 1.0,
            "platformCommissionRate": 0.5,
            "status": "APPROVED",
            "createdAt": "2025-11-24T16:00:00"
          }
        ],
        "totalItems": 1,
        "canReceiveCashback": true
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "first": true,
    "last": false
  },
  "errorCode": null,
  "timestamp": "2025-11-25T11:30:00.000"
}
```

#### Error Responses

**Invalid User ID:**
```http
HTTP/1.1 400 Bad Request
```
```json
{
  "success": false,
  "message": "User ID must be positive",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Invalid Status:**
```http
HTTP/1.1 400 Bad Request
```
```json
{
  "success": false,
  "message": "Invalid status value. Allowed: PENDING, APPROVED, PAID, CANCELLED",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Forbidden (Not Admin):**
```http
HTTP/1.1 403 Forbidden
```
```json
{
  "success": false,
  "message": "Access denied. Admin role required.",
  "data": null,
  "errorCode": "FORBIDDEN",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

**Empty Result (Not an error):**
```http
HTTP/1.1 200 OK
```
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "errorCode": null,
  "timestamp": "2025-11-25T11:30:00.000"
}
```

---

## Data Models

### UserStatus (Enum)

| Value | Description | UI Color Suggestion |
|-------|-------------|---------------------|
| `ACTIVE` | User dang hoat dong binh thuong | Green `#28A745` |
| `SUSPENDED` | User bi tam ngung (co the khoi phuc) | Orange `#FFA500` |
| `BANNED` | User bi cam vinh vien | Red `#DC3545` |

### UserResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Database ID cua user |
| `keycloakId` | String | No | UUID tu Keycloak |
| `username` | String | No | Ten dang nhap |
| `email` | String | No | Email cua user |
| `fullName` | String | Yes | Ho ten day du |
| `phone` | String | Yes | So dien thoai |
| `referralCode` | String | No | Ma gioi thieu cua user |
| `referredBy` | String | Yes | Ma gioi thieu cua nguoi da moi |
| `status` | String | No | Trang thai user |
| `createdAt` | DateTime | No | Thoi gian tao tai khoan |
| `updatedAt` | DateTime | Yes | Thoi gian cap nhat cuoi |
| `accountNumber` | String | Yes | So tai khoan ngan hang |
| `accountName` | String | Yes | Ten chu tai khoan |
| `bankCode` | String | Yes | Ma ngan hang (VPBANK, ACB, ...) |
| `bankName` | String | Yes | Ten ngan hang day du |

### PageResponse

| Field | Type | Description |
|-------|------|-------------|
| `content` | Array | Danh sach users/orders |
| `page` | Integer | So trang hien tai (bat dau tu 0) |
| `size` | Integer | So item moi trang |
| `totalElements` | Long | Tong so items |
| `totalPages` | Integer | Tong so trang |
| `first` | Boolean | La trang dau tien? |
| `last` | Boolean | La trang cuoi cung? |

### OrderStatus (Enum)

| Value | Description | UI Color Suggestion |
|-------|-------------|---------------------|
| `PENDING` | Don hang dang cho xu ly | Orange `#FFA500` |
| `APPROVED` | Don hang da duoc duyet | Blue `#007BFF` |
| `PAID` | Hoa hong da duoc thanh toan | Green `#28A745` |
| `CANCELLED` | Don hang da bi huy | Red `#DC3545` |

### AffiliateOrderResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | ID noi bo cua order |
| `platformId` | Long | No | ID platform (1 = Shopee) |
| `platformName` | String | Yes | Ten platform |
| `userId` | Long | No | ID cua user |
| `clickId` | Long | Yes | ID cua click affiliate |
| `orderId` | String | No | Ma don hang tren san |
| `orderStatus` | String | No | Trang thai order |
| `productName` | String | Yes | Ten san pham (tom tat) |
| `productPrice` | BigDecimal | Yes | Tong gia tri don hang |
| `commissionAmount` | BigDecimal | Yes | Tong hoa hong |
| `currency` | String | No | Loai tien te (VND) |
| `orderTime` | DateTime | Yes | Thoi gian dat hang |
| `confirmTime` | DateTime | Yes | Thoi gian xac nhan |
| `paidTime` | DateTime | Yes | Thoi gian thanh toan |
| `source` | String | No | Nguon (IMPORT, API, MANUAL) |
| `importBatchId` | Long | Yes | ID batch import |
| `createdAt` | DateTime | No | Thoi gian tao |
| `updatedAt` | DateTime | Yes | Thoi gian cap nhat |
| `items` | Array | No | Danh sach items |
| `totalItems` | Integer | No | Tong so items |
| `canReceiveCashback` | Boolean | No | Co the nhan cashback? |

### AffiliateOrderItemResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | ID noi bo cua item |
| `orderId` | Long | No | ID order chua item |
| `itemId` | String | No | Ma san pham tren san |
| `itemName` | String | No | Ten san pham |
| `quantity` | Integer | No | So luong |
| `actualAmount` | BigDecimal | Yes | Gia tri thuc te |
| `itemCommission` | BigDecimal | Yes | Hoa hong tu item |
| `shopId` | String | Yes | ID shop |
| `shopName` | String | Yes | Ten shop |
| `categoryLv1` | String | Yes | Danh muc cap 1 |
| `categoryLv2` | String | Yes | Danh muc cap 2 |
| `categoryLv3` | String | Yes | Danh muc cap 3 |
| `imgUrl` | String | Yes | URL anh san pham |
| `brandCommissionRate` | BigDecimal | Yes | Ty le hoa hong brand (%) |
| `platformCommissionRate` | BigDecimal | Yes | Ty le hoa hong platform (%) |
| `status` | String | No | Trang thai item |
| `createdAt` | DateTime | No | Thoi gian tao |

---

## Integration Examples

### React Admin Dashboard Example

```typescript
// types/user.ts
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  BANNED = 'BANNED'
}

export interface User {
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  fullName: string | null;
  phone: string | null;
  referralCode: string;
  referredBy: string | null;
  status: UserStatus;
  createdAt: string;
  updatedAt: string | null;
  accountNumber: string | null;
  accountName: string | null;
  bankCode: string | null;
  bankName: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
  errorCode: string | null;
  timestamp: string;
}
```

```typescript
// services/adminService.ts
import { getAdminAccessToken } from './authService';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export interface GetUsersParams {
  status?: string | null;
  search?: string | null;
  orderFromDate?: string | null;  // [NEW v1.2.0] Format: YYYY-MM-DD
  orderToDate?: string | null;    // [NEW v1.2.0] Format: YYYY-MM-DD
  page?: number;
  size?: number;
}

/**
 * [ADMIN] Get all users with pagination and filtering.
 *
 * [NEW v1.2.0] Supports orderFromDate and orderToDate to filter
 * users who have orders in the specified date range.
 */
export async function getUsers({
  status = null,
  search = null,
  orderFromDate = null,
  orderToDate = null,
  page = 0,
  size = 20
}: GetUsersParams = {}) {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString()
  });

  if (status) {
    params.append('status', status);
  }

  if (search) {
    params.append('search', search);
  }

  // [NEW v1.2.0] Order date range filter
  if (orderFromDate) {
    params.append('orderFromDate', orderFromDate);
  }

  if (orderToDate) {
    params.append('orderToDate', orderToDate);
  }

  const response = await fetch(
    `${API_BASE_URL}/api/admin/users?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${getAdminAccessToken()}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    if (response.status === 403) {
      throw new Error('Access denied. Admin role required.');
    }
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export interface GetUserOrdersParams {
  userId: number;
  status?: string | null;
  page?: number;
  size?: number;
}

/**
 * [ADMIN] Get orders for a specific user by ID.
 */
export async function getUserOrders({
  userId,
  status = null,
  page = 0,
  size = 10
}: GetUserOrdersParams) {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString()
  });

  if (status) {
    params.append('status', status);
  }

  const response = await fetch(
    `${API_BASE_URL}/api/users/${userId}/orders?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${getAdminAccessToken()}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    if (response.status === 403) {
      throw new Error('Access denied. Admin role required.');
    }
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}
```

```typescript
// hooks/useAdminUsers.ts
import { useState, useEffect, useCallback } from 'react';
import { getUsers, GetUsersParams } from '../services/adminService';
import { User, UserStatus, PageResponse, ApiResponse } from '../types/user';

interface UseAdminUsersOptions {
  initialStatus?: UserStatus | null;
  pageSize?: number;
}

// [NEW v1.2.0] Date range filter interface
interface DateRangeFilter {
  fromDate: string | null;  // Format: YYYY-MM-DD
  toDate: string | null;    // Format: YYYY-MM-DD
}

export function useAdminUsers({ initialStatus = null, pageSize = 20 }: UseAdminUsersOptions = {}) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<UserStatus | null>(initialStatus);
  const [searchTerm, setSearchTerm] = useState<string>('');

  // [NEW v1.2.0] Order date range filter
  const [orderDateFilter, setOrderDateFilter] = useState<DateRangeFilter>({
    fromDate: null,
    toDate: null
  });

  const fetchUsers = useCallback(async (pageNum: number) => {
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<PageResponse<User>> = await getUsers({
        status: statusFilter,
        search: searchTerm || null,
        orderFromDate: orderDateFilter.fromDate,  // [NEW v1.2.0]
        orderToDate: orderDateFilter.toDate,      // [NEW v1.2.0]
        page: pageNum,
        size: pageSize
      });

      if (response.success) {
        setUsers(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setPage(pageNum);
      } else {
        setError(response.message || 'Failed to load users');
      }
    } catch (err: any) {
      setError(err.message || 'Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, searchTerm, orderDateFilter, pageSize]);

  useEffect(() => {
    fetchUsers(0);
  }, [statusFilter, searchTerm, orderDateFilter]);

  const goToPage = useCallback((pageNum: number) => {
    if (pageNum >= 0 && pageNum < totalPages) {
      fetchUsers(pageNum);
    }
  }, [totalPages, fetchUsers]);

  const refresh = useCallback(() => {
    fetchUsers(page);
  }, [page, fetchUsers]);

  const changeStatusFilter = useCallback((status: UserStatus | null) => {
    setStatusFilter(status);
  }, []);

  const changeSearchTerm = useCallback((term: string) => {
    setSearchTerm(term);
  }, []);

  // [NEW v1.2.0] Change order date filter
  const changeOrderDateFilter = useCallback((filter: DateRangeFilter) => {
    setOrderDateFilter(filter);
  }, []);

  // [NEW v1.2.0] Helper: Filter users with orders today
  const filterUsersWithOrdersToday = useCallback(() => {
    const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD
    setOrderDateFilter({ fromDate: today, toDate: today });
  }, []);

  // [NEW v1.2.0] Helper: Filter users with orders in last N days
  const filterUsersWithOrdersInLastDays = useCallback((days: number) => {
    const today = new Date();
    const fromDate = new Date(today);
    fromDate.setDate(today.getDate() - days + 1);

    setOrderDateFilter({
      fromDate: fromDate.toISOString().split('T')[0],
      toDate: today.toISOString().split('T')[0]
    });
  }, []);

  // [NEW v1.2.0] Clear order date filter
  const clearOrderDateFilter = useCallback(() => {
    setOrderDateFilter({ fromDate: null, toDate: null });
  }, []);

  return {
    users,
    loading,
    error,
    page,
    totalPages,
    totalElements,
    statusFilter,
    searchTerm,
    orderDateFilter,  // [NEW v1.2.0]
    goToPage,
    refresh,
    changeStatusFilter,
    changeSearchTerm,
    changeOrderDateFilter,           // [NEW v1.2.0]
    filterUsersWithOrdersToday,      // [NEW v1.2.0]
    filterUsersWithOrdersInLastDays, // [NEW v1.2.0]
    clearOrderDateFilter             // [NEW v1.2.0]
  };
}
```

```tsx
// components/AdminUserList.tsx
import React, { useState } from 'react';
import { useAdminUsers } from '../hooks/useAdminUsers';
import { UserStatus } from '../types/user';

const STATUS_CONFIG = {
  [UserStatus.ACTIVE]: { label: 'Hoat dong', color: 'bg-green-500' },
  [UserStatus.SUSPENDED]: { label: 'Tam ngung', color: 'bg-orange-500' },
  [UserStatus.BANNED]: { label: 'Cam', color: 'bg-red-500' }
};

export function AdminUserList() {
  const {
    users,
    loading,
    error,
    page,
    totalPages,
    totalElements,
    statusFilter,
    searchTerm,
    orderDateFilter,  // [NEW v1.2.0]
    goToPage,
    refresh,
    changeStatusFilter,
    changeSearchTerm,
    changeOrderDateFilter,           // [NEW v1.2.0]
    filterUsersWithOrdersToday,      // [NEW v1.2.0]
    filterUsersWithOrdersInLastDays, // [NEW v1.2.0]
    clearOrderDateFilter             // [NEW v1.2.0]
  } = useAdminUsers({ pageSize: 20 });

  const [searchInput, setSearchInput] = useState('');

  // [NEW v1.2.0] Date input states
  const [fromDateInput, setFromDateInput] = useState('');
  const [toDateInput, setToDateInput] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    changeSearchTerm(searchInput);
  };

  // [NEW v1.2.0] Handle custom date range filter
  const handleDateFilter = () => {
    changeOrderDateFilter({
      fromDate: fromDateInput || null,
      toDate: toDateInput || null
    });
  };

  // [NEW v1.2.0] Clear all date filters
  const handleClearDateFilter = () => {
    setFromDateInput('');
    setToDateInput('');
    clearOrderDateFilter();
  };

  return (
    <div className="container mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Quan ly Users</h1>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4 mb-6">
        <div className="flex flex-wrap gap-4 items-center">
          {/* Status Filter */}
          <div className="flex gap-2">
            <button
              className={`px-3 py-1 rounded ${!statusFilter ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
              onClick={() => changeStatusFilter(null)}
            >
              Tat ca
            </button>
            {Object.entries(STATUS_CONFIG).map(([status, config]) => (
              <button
                key={status}
                className={`px-3 py-1 rounded ${statusFilter === status ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
                onClick={() => changeStatusFilter(status as UserStatus)}
              >
                {config.label}
              </button>
            ))}
          </div>

          {/* Search */}
          <form onSubmit={handleSearch} className="flex gap-2 ml-auto">
            <input
              type="text"
              placeholder="Tim theo email/username..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="px-3 py-1 border rounded w-64"
            />
            <button type="submit" className="px-4 py-1 bg-blue-500 text-white rounded">
              Tim
            </button>
          </form>
        </div>

        {/* [NEW v1.2.0] Order Date Filter */}
        <div className="mt-4 pt-4 border-t">
          <div className="flex flex-wrap gap-4 items-center">
            <span className="text-sm font-medium text-gray-700">Loc theo don hang:</span>

            {/* Quick Filters */}
            <div className="flex gap-2">
              <button
                className={`px-3 py-1 rounded text-sm ${
                  !orderDateFilter.fromDate && !orderDateFilter.toDate
                    ? 'bg-gray-200'
                    : 'bg-gray-100'
                }`}
                onClick={handleClearDateFilter}
              >
                Tat ca
              </button>
              <button
                className="px-3 py-1 rounded text-sm bg-green-100 hover:bg-green-200"
                onClick={filterUsersWithOrdersToday}
              >
                Hom nay
              </button>
              <button
                className="px-3 py-1 rounded text-sm bg-blue-100 hover:bg-blue-200"
                onClick={() => filterUsersWithOrdersInLastDays(3)}
              >
                3 ngay
              </button>
              <button
                className="px-3 py-1 rounded text-sm bg-purple-100 hover:bg-purple-200"
                onClick={() => filterUsersWithOrdersInLastDays(7)}
              >
                7 ngay
              </button>
              <button
                className="px-3 py-1 rounded text-sm bg-orange-100 hover:bg-orange-200"
                onClick={() => filterUsersWithOrdersInLastDays(30)}
              >
                30 ngay
              </button>
            </div>

            {/* Custom Date Range */}
            <div className="flex gap-2 items-center">
              <input
                type="date"
                value={fromDateInput}
                onChange={(e) => setFromDateInput(e.target.value)}
                className="px-2 py-1 border rounded text-sm"
                placeholder="Tu ngay"
              />
              <span className="text-gray-500">-</span>
              <input
                type="date"
                value={toDateInput}
                onChange={(e) => setToDateInput(e.target.value)}
                className="px-2 py-1 border rounded text-sm"
                placeholder="Den ngay"
              />
              <button
                onClick={handleDateFilter}
                className="px-3 py-1 bg-indigo-500 text-white rounded text-sm hover:bg-indigo-600"
              >
                Loc
              </button>
            </div>

            {/* Active Filter Indicator */}
            {(orderDateFilter.fromDate || orderDateFilter.toDate) && (
              <div className="flex items-center gap-2 px-3 py-1 bg-indigo-100 rounded text-sm">
                <span>
                  Dang loc: {orderDateFilter.fromDate || '...'} → {orderDateFilter.toDate || '...'}
                </span>
                <button
                  onClick={handleClearDateFilter}
                  className="text-red-500 hover:text-red-700 font-bold"
                >
                  ×
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
          <button onClick={refresh} className="ml-2 underline">Thu lai</button>
        </div>
      )}

      {/* Users Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {loading ? (
              <tr>
                <td colSpan={7} className="px-6 py-4 text-center">Dang tai...</td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-4 text-center text-gray-500">
                  Khong tim thay user nao
                </td>
              </tr>
            ) : (
              users.map(user => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div>
                      <div className="text-sm font-medium text-gray-900">
                        {user.fullName || user.username}
                      </div>
                      <div className="text-sm text-gray-500">@{user.username}</div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.email}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.phone || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full text-white ${STATUS_CONFIG[user.status as UserStatus]?.color || 'bg-gray-500'}`}>
                      {STATUS_CONFIG[user.status as UserStatus]?.label || user.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString('vi-VN')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button className="text-blue-600 hover:text-blue-900 mr-3">
                      Xem
                    </button>
                    <button className="text-orange-600 hover:text-orange-900">
                      Sua
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200">
          <div className="text-sm text-gray-700">
            Hien thi <span className="font-medium">{users.length}</span> trong tong so{' '}
            <span className="font-medium">{totalElements}</span> users
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => goToPage(page - 1)}
              disabled={page === 0}
              className="px-3 py-1 border rounded disabled:opacity-50"
            >
              Truoc
            </button>
            <span className="px-3 py-1">
              Trang {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => goToPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border rounded disabled:opacity-50"
            >
              Sau
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
```

### Vue.js Admin Dashboard Example

```typescript
// composables/useAdminUsers.ts
import { ref, reactive, watch } from 'vue';

interface User {
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  fullName: string | null;
  phone: string | null;
  referralCode: string;
  referredBy: string | null;
  status: string;
  createdAt: string;
  updatedAt: string | null;
  accountNumber: string | null;
  accountName: string | null;
  bankCode: string | null;
  bankName: string | null;
}

export function useAdminUsers() {
  const users = ref<User[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(0);
  const totalPages = ref(0);
  const totalElements = ref(0);

  const filters = reactive({
    status: null as string | null,
    search: '',
    size: 20
  });

  async function fetchUsers(pageNum: number = 0) {
    loading.value = true;
    error.value = null;

    try {
      const params = new URLSearchParams({
        page: pageNum.toString(),
        size: filters.size.toString()
      });

      if (filters.status) params.append('status', filters.status);
      if (filters.search) params.append('search', filters.search);

      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/admin/users?${params}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('admin_token')}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(response.status === 403 ? 'Access denied' : `HTTP ${response.status}`);
      }

      const result = await response.json();

      if (result.success) {
        users.value = result.data.content;
        page.value = result.data.page;
        totalPages.value = result.data.totalPages;
        totalElements.value = result.data.totalElements;
      } else {
        error.value = result.message;
      }
    } catch (err: any) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  }

  // Watch filters and refetch
  watch([() => filters.status, () => filters.search], () => {
    fetchUsers(0);
  });

  return {
    users,
    loading,
    error,
    page,
    totalPages,
    totalElements,
    filters,
    fetchUsers
  };
}
```

```vue
<!-- components/AdminUserList.vue -->
<template>
  <div class="container mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">Quan ly Users</h1>

    <!-- Filters -->
    <div class="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-center">
      <select v-model="filters.status" class="border rounded px-3 py-1">
        <option :value="null">Tat ca Status</option>
        <option value="ACTIVE">Hoat dong</option>
        <option value="SUSPENDED">Tam ngung</option>
        <option value="BANNED">Cam</option>
      </select>

      <input
        v-model="filters.search"
        type="text"
        placeholder="Tim theo email/username..."
        class="border rounded px-3 py-1 w-64 ml-auto"
        @keyup.enter="fetchUsers(0)"
      />
    </div>

    <!-- Error -->
    <div v-if="error" class="bg-red-100 text-red-700 p-4 rounded mb-4">
      {{ error }}
    </div>

    <!-- Table -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="text-center py-4">Dang tai...</td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="5" class="text-center py-4 text-gray-500">Khong co user nao</td>
          </tr>
          <tr v-else v-for="user in users" :key="user.id" class="border-t hover:bg-gray-50">
            <td class="px-6 py-4">{{ user.id }}</td>
            <td class="px-6 py-4">
              <div class="font-medium">{{ user.fullName || user.username }}</div>
              <div class="text-sm text-gray-500">@{{ user.username }}</div>
            </td>
            <td class="px-6 py-4">{{ user.email }}</td>
            <td class="px-6 py-4">
              <span :class="statusClass(user.status)" class="px-2 py-1 rounded text-white text-xs">
                {{ statusLabel(user.status) }}
              </span>
            </td>
            <td class="px-6 py-4">{{ formatDate(user.createdAt) }}</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="px-4 py-3 border-t flex justify-between items-center">
        <span class="text-sm text-gray-700">
          Tong: {{ totalElements }} users
        </span>
        <div class="flex gap-2">
          <button @click="fetchUsers(page - 1)" :disabled="page === 0" class="px-3 py-1 border rounded disabled:opacity-50">
            Truoc
          </button>
          <span class="px-3 py-1">{{ page + 1 }} / {{ totalPages }}</span>
          <button @click="fetchUsers(page + 1)" :disabled="page >= totalPages - 1" class="px-3 py-1 border rounded disabled:opacity-50">
            Sau
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAdminUsers } from '../composables/useAdminUsers';

const { users, loading, error, page, totalPages, totalElements, filters, fetchUsers } = useAdminUsers();

// Initial fetch
fetchUsers(0);

const statusClass = (status: string) => ({
  'bg-green-500': status === 'ACTIVE',
  'bg-orange-500': status === 'SUSPENDED',
  'bg-red-500': status === 'BANNED'
});

const statusLabel = (status: string) => ({
  ACTIVE: 'Hoat dong',
  SUSPENDED: 'Tam ngung',
  BANNED: 'Cam'
}[status] || status);

const formatDate = (date: string) => new Date(date).toLocaleDateString('vi-VN');
</script>
```

---

## Additional Integration: User Orders Component

### React Hook for User Orders

```typescript
// hooks/useUserOrders.ts
import { useState, useEffect, useCallback } from 'react';
import { getUserOrders } from '../services/adminService';

export enum OrderStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  PAID = 'PAID',
  CANCELLED = 'CANCELLED'
}

interface OrderItem {
  id: number;
  itemId: string;
  itemName: string;
  quantity: number;
  actualAmount: number;
  itemCommission: number;
  shopName: string;
  imgUrl: string;
  status: string;
}

interface Order {
  id: number;
  platformName: string;
  orderId: string;
  orderStatus: OrderStatus;
  productName: string;
  productPrice: number;
  commissionAmount: number;
  orderTime: string;
  items: OrderItem[];
  totalItems: number;
  canReceiveCashback: boolean;
}

interface UseUserOrdersOptions {
  userId: number;
  initialStatus?: OrderStatus | null;
  pageSize?: number;
}

export function useUserOrders({ userId, initialStatus = null, pageSize = 10 }: UseUserOrdersOptions) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | null>(initialStatus);

  const fetchOrders = useCallback(async (pageNum: number) => {
    setLoading(true);
    setError(null);

    try {
      const response = await getUserOrders({
        userId,
        status: statusFilter,
        page: pageNum,
        size: pageSize
      });

      if (response.success) {
        setOrders(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setPage(pageNum);
      } else {
        setError(response.message || 'Failed to load orders');
      }
    } catch (err: any) {
      setError(err.message || 'Network error');
    } finally {
      setLoading(false);
    }
  }, [userId, statusFilter, pageSize]);

  useEffect(() => {
    fetchOrders(0);
  }, [userId, statusFilter]);

  const goToPage = useCallback((pageNum: number) => {
    if (pageNum >= 0 && pageNum < totalPages) {
      fetchOrders(pageNum);
    }
  }, [totalPages, fetchOrders]);

  const changeStatusFilter = useCallback((status: OrderStatus | null) => {
    setStatusFilter(status);
  }, []);

  return {
    orders,
    loading,
    error,
    page,
    totalPages,
    totalElements,
    statusFilter,
    goToPage,
    changeStatusFilter,
    refresh: () => fetchOrders(page)
  };
}
```

### User Orders Component (Modal/Page)

```tsx
// components/UserOrdersModal.tsx
import React from 'react';
import { useUserOrders, OrderStatus } from '../hooks/useUserOrders';

const ORDER_STATUS_CONFIG = {
  [OrderStatus.PENDING]: { label: 'Dang cho', color: 'bg-orange-500' },
  [OrderStatus.APPROVED]: { label: 'Da duyet', color: 'bg-blue-500' },
  [OrderStatus.PAID]: { label: 'Da thanh toan', color: 'bg-green-500' },
  [OrderStatus.CANCELLED]: { label: 'Da huy', color: 'bg-red-500' }
};

interface UserOrdersModalProps {
  userId: number;
  userName: string;
  onClose: () => void;
}

export function UserOrdersModal({ userId, userName, onClose }: UserOrdersModalProps) {
  const {
    orders,
    loading,
    error,
    page,
    totalPages,
    totalElements,
    statusFilter,
    goToPage,
    changeStatusFilter
  } = useUserOrders({ userId, pageSize: 10 });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b flex justify-between items-center">
          <div>
            <h2 className="text-xl font-bold">Orders cua {userName}</h2>
            <p className="text-sm text-gray-500">User ID: {userId} | Tong: {totalElements} orders</p>
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700 text-2xl">
            &times;
          </button>
        </div>

        {/* Filters */}
        <div className="px-6 py-3 border-b flex gap-2">
          <button
            className={`px-3 py-1 rounded text-sm ${!statusFilter ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
            onClick={() => changeStatusFilter(null)}
          >
            Tat ca
          </button>
          {Object.entries(ORDER_STATUS_CONFIG).map(([status, config]) => (
            <button
              key={status}
              className={`px-3 py-1 rounded text-sm ${statusFilter === status ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
              onClick={() => changeStatusFilter(status as OrderStatus)}
            >
              {config.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="px-6 py-4 overflow-y-auto" style={{ maxHeight: 'calc(90vh - 200px)' }}>
          {error && (
            <div className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</div>
          )}

          {loading ? (
            <div className="text-center py-8">Dang tai...</div>
          ) : orders.length === 0 ? (
            <div className="text-center py-8 text-gray-500">Khong co order nao</div>
          ) : (
            <div className="space-y-4">
              {orders.map(order => (
                <div key={order.id} className="border rounded-lg p-4">
                  {/* Order Header */}
                  <div className="flex justify-between items-start mb-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">#{order.orderId}</span>
                        <span className={`px-2 py-0.5 rounded text-xs text-white ${ORDER_STATUS_CONFIG[order.orderStatus]?.color}`}>
                          {ORDER_STATUS_CONFIG[order.orderStatus]?.label}
                        </span>
                      </div>
                      <div className="text-sm text-gray-500 mt-1">
                        {order.platformName} | {new Date(order.orderTime).toLocaleString('vi-VN')}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-medium">{formatCurrency(order.productPrice)}</div>
                      <div className="text-sm text-green-600">
                        Hoa hong: {formatCurrency(order.commissionAmount)}
                      </div>
                    </div>
                  </div>

                  {/* Order Items */}
                  <div className="border-t pt-3">
                    {order.items.map(item => (
                      <div key={item.id} className="flex gap-3 mb-2">
                        {item.imgUrl && (
                          <img src={item.imgUrl} alt={item.itemName} className="w-12 h-12 object-cover rounded" />
                        )}
                        <div className="flex-1">
                          <div className="text-sm font-medium line-clamp-1">{item.itemName}</div>
                          <div className="text-xs text-gray-500">
                            {item.shopName} | SL: {item.quantity}
                          </div>
                        </div>
                        <div className="text-right text-sm">
                          <div>{formatCurrency(item.actualAmount)}</div>
                          <div className="text-green-600">{formatCurrency(item.itemCommission)}</div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Cashback Status */}
                  {order.canReceiveCashback && (
                    <div className="mt-2 text-xs text-blue-600">
                      Co the nhan cashback
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pagination */}
        <div className="px-6 py-3 border-t flex justify-between items-center">
          <span className="text-sm text-gray-500">
            Trang {page + 1} / {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => goToPage(page - 1)}
              disabled={page === 0}
              className="px-3 py-1 border rounded disabled:opacity-50"
            >
              Truoc
            </button>
            <button
              onClick={() => goToPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border rounded disabled:opacity-50"
            >
              Sau
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
```

### Usage in Admin User List

```tsx
// In AdminUserList.tsx - Add view orders functionality

import { useState } from 'react';
import { UserOrdersModal } from './UserOrdersModal';

// Inside component:
const [selectedUser, setSelectedUser] = useState<{ id: number; name: string } | null>(null);

// In the table actions column:
<button
  className="text-blue-600 hover:text-blue-900 mr-3"
  onClick={() => setSelectedUser({ id: user.id, name: user.fullName || user.username })}
>
  Xem Orders
</button>

// Add modal at the end of component:
{selectedUser && (
  <UserOrdersModal
    userId={selectedUser.id}
    userName={selectedUser.name}
    onClose={() => setSelectedUser(null)}
  />
)}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.2.0 | 2025-12-18 | Added `orderFromDate` and `orderToDate` parameters to filter users by order date range |
| 1.1.0 | 2025-11-25 | Added `GET /api/users/{userId}/orders` endpoint for admin to view user's orders |
| 1.0.0 | 2025-11-25 | Initial release with `GET /api/admin/users` endpoint |

---

## Contact

- **Backend Team**: backend@cashbee.vn
- **API Issues**: Tao issue tren Plane project CASHBEE
