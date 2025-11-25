# User Orders API Documentation

> Version: 1.1.0
> Last Updated: 2025-11-25
> Base URL: `https://api.cashbee.vn` (Production) | `http://localhost:8080` (Development)

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Common Response Format](#common-response-format)
4. [Error Codes](#error-codes)
5. [Endpoints](#endpoints)
   - [Get My Orders (Recommended)](#1-get-my-orders-recommended)
   - [Get User Orders by ID (Admin)](#2-get-user-orders-by-id-admin)
6. [Data Models](#data-models)
7. [Status Workflow](#status-workflow)
8. [Mock Data for Development](#mock-data-for-development)
9. [Integration Examples](#integration-examples)
10. [Changelog](#changelog)

---

## Overview

API cho phep user xem danh sach don hang affiliate cua minh, bao gom:
- Danh sach don hang voi phan trang
- Loc theo trang thai (PENDING, APPROVED, PAID, CANCELLED)
- Chi tiet tung san pham trong don hang
- Thong tin hoa hong tu moi don hang

### Use Cases
- User xem lich su don hang da dat qua link affiliate
- User theo doi trang thai don hang (dang cho duyet, da duyet, da thanh toan, da huy)
- User xem chi tiet hoa hong tu moi don hang
- Admin xem don hang cua bat ky user nao

### API Comparison

| Feature | `/api/orders/me` | `/api/users/{userId}/orders` |
|---------|------------------|------------------------------|
| **Target** | Mobile/Web App | Admin Dashboard |
| **Authentication** | JWT Token (auto extract userId) | JWT Token + userId in URL |
| **Security** | User chi xem duoc don hang cua minh | Admin co the xem don hang cua bat ky ai |
| **Recommended** | **YES** - Su dung cho end users | Chi dung cho Admin |

---

## Authentication

API yeu cau JWT token trong header:

```http
Authorization: Bearer <access_token>
```

### How JWT Authentication Works

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Mobile/   │      │   CashBee   │      │  Keycloak   │
│   Web App   │      │   Backend   │      │    (Auth)   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                    │
       │  1. Login          │                    │
       │ ─────────────────────────────────────> │
       │                    │                    │
       │  2. JWT Token      │                    │
       │ <───────────────────────────────────── │
       │                    │                    │
       │  3. GET /api/orders/me                 │
       │     Authorization: Bearer <token>      │
       │ ─────────────────> │                    │
       │                    │                    │
       │                    │  4. Validate Token │
       │                    │ ─────────────────> │
       │                    │                    │
       │                    │  5. Token Valid    │
       │                    │ <───────────────── │
       │                    │                    │
       │  6. Orders Response│                    │
       │ <───────────────── │                    │
       │                    │                    │
```

---

## Common Response Format

Tat ca response deu co cau truc chung:

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
  "message": "User ID must be positive",
  "data": null,
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Tham so khong hop le |
| `UNAUTHORIZED` | 401 | Chua dang nhap hoac token het han |
| `FORBIDDEN` | 403 | Khong co quyen truy cap |
| `NOT_FOUND` | 404 | Khong tim thay tai nguyen |
| `INTERNAL_ERROR` | 500 | Loi server |

---

## Endpoints

### 1. Get My Orders (Recommended)

> **RECOMMENDED cho Mobile/Web App**

Lay danh sach don hang cua user hien tai. User ID duoc tu dong lay tu JWT token.

#### Why Use This Endpoint?

- **An toan**: User chi co the xem don hang cua chinh minh
- **Don gian**: Khong can truyen userId trong URL
- **Bao mat**: Khong the truy cap don hang cua nguoi khac

#### Request

```http
GET /api/orders/me
Authorization: Bearer <access_token>
```

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `status` | String | No | null | Loc theo trang thai: `PENDING`, `APPROVED`, `PAID`, `CANCELLED` |
| `page` | Integer | No | 0 | So trang (bat dau tu 0) |
| `size` | Integer | No | 10 | So don hang moi trang (toi da 50) |

#### Example Requests

**Lay tat ca don hang cua toi:**
```bash
curl -X GET "http://localhost:8080/api/orders/me" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay don hang PENDING cua toi:**
```bash
curl -X GET "http://localhost:8080/api/orders/me?status=PENDING" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay trang thu 2 voi 20 don hang:**
```bash
curl -X GET "http://localhost:8080/api/orders/me?page=1&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Ket hop nhieu filter:**
```bash
curl -X GET "http://localhost:8080/api/orders/me?status=APPROVED&page=0&size=15" \
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

**User Not Found:**
```http
HTTP/1.1 404 Not Found
```
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errorCode": "NOT_FOUND",
  "timestamp": "2025-11-25T11:30:00.000"
}
```

---

### 2. Get User Orders by ID (Admin)

> **Chi danh cho Admin Dashboard**

Lay danh sach don hang cua bat ky user nao theo userId. Chi Admin moi duoc su dung endpoint nay.

#### When to Use This Endpoint?

- Admin can xem don hang cua mot user cu the
- Customer Support can kiem tra don hang cua khach hang
- Backend services can truy cap don hang theo userId

#### Request

```http
GET /api/users/{userId}/orders
Authorization: Bearer <admin_access_token>
```

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | Long | Yes | ID cua user can xem don hang |

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `status` | String | No | null | Loc theo trang thai: `PENDING`, `APPROVED`, `PAID`, `CANCELLED` |
| `page` | Integer | No | 0 | So trang (bat dau tu 0) |
| `size` | Integer | No | 10 | So don hang moi trang (toi da 50) |

#### Example Requests

**Lay tat ca don hang cua user 100:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay don hang PENDING cua user 100:**
```bash
curl -X GET "http://localhost:8080/api/users/100/orders?status=PENDING" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lay trang thu 2 voi 20 don hang:**
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

### OrderStatus (Enum)

| Value | Description | UI Color Suggestion |
|-------|-------------|---------------------|
| `PENDING` | Don hang dang cho xu ly | Orange/Yellow `#FFA500` |
| `APPROVED` | Don hang da duoc duyet | Blue `#007BFF` |
| `PAID` | Hoa hong da duoc thanh toan | Green `#28A745` |
| `CANCELLED` | Don hang da bi huy | Red `#DC3545` |

### AffiliateOrderResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | ID noi bo cua don hang |
| `platformId` | Long | No | ID cua san thuong mai (1 = Shopee) |
| `platformName` | String | Yes | Ten san thuong mai |
| `userId` | Long | No | ID cua user |
| `clickId` | Long | Yes | ID cua click affiliate |
| `orderId` | String | No | Ma don hang tren san |
| `orderStatus` | String | No | Trang thai don hang |
| `productName` | String | Yes | Ten san pham (tom tat) |
| `productPrice` | BigDecimal | Yes | Tong gia tri don hang |
| `commissionAmount` | BigDecimal | Yes | Tong hoa hong |
| `currency` | String | No | Loai tien te (mac dinh: VND) |
| `orderTime` | DateTime | Yes | Thoi gian dat hang |
| `confirmTime` | DateTime | Yes | Thoi gian xac nhan |
| `paidTime` | DateTime | Yes | Thoi gian thanh toan hoa hong |
| `source` | String | No | Nguon don hang (IMPORT, API, MANUAL) |
| `importBatchId` | Long | Yes | ID batch import |
| `createdAt` | DateTime | No | Thoi gian tao record |
| `updatedAt` | DateTime | Yes | Thoi gian cap nhat |
| `items` | Array | No | Danh sach san pham |
| `totalItems` | Integer | No | Tong so san pham |
| `canReceiveCashback` | Boolean | No | Co the nhan cashback khong |

### AffiliateOrderItemResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | ID noi bo cua item |
| `orderId` | Long | No | ID don hang chua item |
| `itemId` | String | No | Ma san pham tren san |
| `itemName` | String | No | Ten san pham |
| `quantity` | Integer | No | So luong |
| `actualAmount` | BigDecimal | Yes | Gia tri thuc te |
| `itemCommission` | BigDecimal | Yes | Hoa hong tu item nay |
| `shopId` | String | Yes | ID cua shop |
| `shopName` | String | Yes | Ten shop |
| `categoryLv1` | String | Yes | Danh muc cap 1 |
| `categoryLv2` | String | Yes | Danh muc cap 2 |
| `categoryLv3` | String | Yes | Danh muc cap 3 |
| `imgUrl` | String | Yes | URL anh san pham |
| `brandCommissionRate` | BigDecimal | Yes | Ty le hoa hong brand (%) |
| `platformCommissionRate` | BigDecimal | Yes | Ty le hoa hong platform (%) |
| `status` | String | No | Trang thai item |
| `createdAt` | DateTime | No | Thoi gian tao |

### PageResponse

| Field | Type | Description |
|-------|------|-------------|
| `content` | Array | Danh sach don hang |
| `page` | Integer | So trang hien tai (bat dau tu 0) |
| `size` | Integer | So item moi trang |
| `totalElements` | Long | Tong so don hang |
| `totalPages` | Integer | Tong so trang |
| `first` | Boolean | La trang dau tien? |
| `last` | Boolean | La trang cuoi cung? |

---

## Status Workflow

```
   +----------+     Approved      +----------+     Payment      +--------+
   | PENDING  | ----------------> | APPROVED | --------------> |  PAID  |
   +----------+                   +----------+                  +--------+
        |                              |
        |         Cancelled            |         Cancelled
        +----------------------------->+------------------------+
                                                                |
                                                                v
                                                          +-----------+
                                                          | CANCELLED |
                                                          +-----------+
```

### Status Descriptions

| Status | Description | User Action |
|--------|-------------|-------------|
| **PENDING** | Don hang moi import, chua duoc duyet | Cho doi |
| **APPROVED** | Don hang da duoc duyet, hoa hong se duoc cong vao pending_balance | Cho thanh toan |
| **PAID** | Hoa hong da duoc thanh toan vao vi | Da hoan tat |
| **CANCELLED** | Don hang bi huy (tra hang, hoan tien, gian lan) | Khong nhan duoc hoa hong |

---

## Mock Data for Development

### Mock API Response Generator

Frontend/Mobile team co the su dung mock data sau de phat trien doc lap:

```javascript
// mock-orders.js

const mockOrders = {
  success: true,
  data: {
    content: [
      {
        id: 1,
        platformId: 1,
        platformName: "Shopee",
        userId: 100,
        clickId: 5,
        orderId: "241125MOCK001",
        orderStatus: "PENDING",
        productName: "Ao thun nam basic",
        productPrice: 199000.00,
        commissionAmount: 1990.00,
        currency: "VND",
        orderTime: "2025-11-25T10:30:00",
        confirmTime: null,
        paidTime: null,
        source: "IMPORT",
        importBatchId: 15,
        createdAt: "2025-11-25T11:00:00",
        updatedAt: "2025-11-25T11:00:00",
        items: [
          {
            id: 101,
            orderId: 1,
            itemId: "ITEM001",
            itemName: "Ao thun nam basic - Trang - Size L",
            quantity: 2,
            actualAmount: 199000.00,
            itemCommission: 1990.00,
            shopId: "SHOP001",
            shopName: "Fashion Store VN",
            categoryLv1: "Thoi trang Nam",
            categoryLv2: "Ao",
            categoryLv3: "Ao thun",
            imgUrl: "https://via.placeholder.com/200x200?text=T-Shirt",
            brandCommissionRate: 1.0,
            platformCommissionRate: 0.5,
            status: "PENDING",
            createdAt: "2025-11-25T11:00:00"
          }
        ],
        totalItems: 1,
        canReceiveCashback: true
      },
      {
        id: 2,
        platformId: 1,
        platformName: "Shopee",
        userId: 100,
        clickId: 3,
        orderId: "241124MOCK002",
        orderStatus: "APPROVED",
        productName: "Tai nghe Bluetooth",
        productPrice: 350000.00,
        commissionAmount: 17500.00,
        currency: "VND",
        orderTime: "2025-11-24T15:20:00",
        confirmTime: "2025-11-25T09:00:00",
        paidTime: null,
        source: "IMPORT",
        importBatchId: 14,
        createdAt: "2025-11-24T16:00:00",
        updatedAt: "2025-11-25T09:00:00",
        items: [
          {
            id: 102,
            orderId: 2,
            itemId: "ITEM002",
            itemName: "Tai nghe Bluetooth TWS - Den",
            quantity: 1,
            actualAmount: 350000.00,
            itemCommission: 17500.00,
            shopId: "SHOP002",
            shopName: "Tech Zone",
            categoryLv1: "Thiet bi dien tu",
            categoryLv2: "Phu kien dien thoai",
            categoryLv3: "Tai nghe",
            imgUrl: "https://via.placeholder.com/200x200?text=Earbuds",
            brandCommissionRate: 5.0,
            platformCommissionRate: 2.5,
            status: "APPROVED",
            createdAt: "2025-11-24T16:00:00"
          }
        ],
        totalItems: 1,
        canReceiveCashback: true
      },
      {
        id: 3,
        platformId: 1,
        platformName: "Shopee",
        userId: 100,
        clickId: 2,
        orderId: "241123MOCK003",
        orderStatus: "PAID",
        productName: "Sach - Dac Nhan Tam",
        productPrice: 88000.00,
        commissionAmount: 4400.00,
        currency: "VND",
        orderTime: "2025-11-23T08:15:00",
        confirmTime: "2025-11-24T10:00:00",
        paidTime: "2025-11-25T08:00:00",
        source: "IMPORT",
        importBatchId: 13,
        createdAt: "2025-11-23T09:00:00",
        updatedAt: "2025-11-25T08:00:00",
        items: [
          {
            id: 103,
            orderId: 3,
            itemId: "ITEM003",
            itemName: "Dac Nhan Tam - Dale Carnegie",
            quantity: 1,
            actualAmount: 88000.00,
            itemCommission: 4400.00,
            shopId: "SHOP003",
            shopName: "Nha Sach Fahasa",
            categoryLv1: "Sach",
            categoryLv2: "Sach ky nang song",
            categoryLv3: "Ky nang giao tiep",
            imgUrl: "https://via.placeholder.com/200x200?text=Book",
            brandCommissionRate: 5.0,
            platformCommissionRate: 3.0,
            status: "PAID",
            createdAt: "2025-11-23T09:00:00"
          }
        ],
        totalItems: 1,
        canReceiveCashback: false
      },
      {
        id: 4,
        platformId: 1,
        platformName: "Shopee",
        userId: 100,
        clickId: 1,
        orderId: "241122MOCK004",
        orderStatus: "CANCELLED",
        productName: "Giay the thao nam",
        productPrice: 599000.00,
        commissionAmount: 0.00,
        currency: "VND",
        orderTime: "2025-11-22T14:00:00",
        confirmTime: null,
        paidTime: null,
        source: "IMPORT",
        importBatchId: 12,
        createdAt: "2025-11-22T15:00:00",
        updatedAt: "2025-11-23T10:00:00",
        items: [
          {
            id: 104,
            orderId: 4,
            itemId: "ITEM004",
            itemName: "Giay the thao nam - Trang - Size 42",
            quantity: 1,
            actualAmount: 599000.00,
            itemCommission: 0.00,
            shopId: "SHOP004",
            shopName: "Sport World",
            categoryLv1: "Giay dep Nam",
            categoryLv2: "Giay the thao",
            categoryLv3: "Giay chay bo",
            imgUrl: "https://via.placeholder.com/200x200?text=Shoes",
            brandCommissionRate: 3.0,
            platformCommissionRate: 1.5,
            status: "CANCELLED",
            createdAt: "2025-11-22T15:00:00"
          }
        ],
        totalItems: 1,
        canReceiveCashback: false
      }
    ],
    page: 0,
    size: 10,
    totalElements: 4,
    totalPages: 1,
    first: true,
    last: true
  },
  errorCode: null,
  timestamp: new Date().toISOString()
};

// Filter function
function filterByStatus(orders, status) {
  if (!status) return orders;
  return {
    ...orders,
    data: {
      ...orders.data,
      content: orders.data.content.filter(o => o.orderStatus === status),
      totalElements: orders.data.content.filter(o => o.orderStatus === status).length
    }
  };
}

// Export for use
module.exports = { mockOrders, filterByStatus };
```

---

## Integration Examples

### React Native Example (Using /api/orders/me)

```javascript
// services/orderService.js
import { getAccessToken } from './authService';

const API_BASE_URL = 'http://localhost:8080';

/**
 * Get current user's orders using JWT token.
 * User ID is automatically extracted from token on backend.
 */
export async function getMyOrders(status = null, page = 0, size = 10) {
  const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
  if (status) params.append('status', status);

  const response = await fetch(
    `${API_BASE_URL}/api/orders/me?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${await getAccessToken()}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}
```

```javascript
// screens/OrdersScreen.js
import React, { useState, useEffect } from 'react';
import { View, FlatList, Text, TouchableOpacity, ActivityIndicator, StyleSheet } from 'react-native';
import { getMyOrders } from '../services/orderService';

const STATUS_COLORS = {
  PENDING: '#FFA500',
  APPROVED: '#007BFF',
  PAID: '#28A745',
  CANCELLED: '#DC3545'
};

const STATUS_LABELS = {
  PENDING: 'Dang cho',
  APPROVED: 'Da duyet',
  PAID: 'Da thanh toan',
  CANCELLED: 'Da huy'
};

export default function OrdersScreen() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [statusFilter, setStatusFilter] = useState(null);

  useEffect(() => {
    loadOrders(0, true);
  }, [statusFilter]);

  const loadOrders = async (pageNum, reset = false) => {
    if (loading) return;

    setLoading(true);
    try {
      // No need to pass userId - extracted from JWT automatically!
      const response = await getMyOrders(statusFilter, pageNum, 10);

      if (response.success) {
        const newOrders = response.data.content;
        setOrders(reset ? newOrders : [...orders, ...newOrders]);
        setHasMore(!response.data.last);
        setPage(pageNum);
      }
    } catch (error) {
      console.error('Failed to load orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadMore = () => {
    if (hasMore && !loading) {
      loadOrders(page + 1);
    }
  };

  const renderOrderItem = ({ item }) => (
    <TouchableOpacity style={styles.orderCard}>
      <View style={styles.orderHeader}>
        <Text style={styles.orderId}>#{item.orderId}</Text>
        <View style={[styles.statusBadge, { backgroundColor: STATUS_COLORS[item.orderStatus] }]}>
          <Text style={styles.statusText}>{STATUS_LABELS[item.orderStatus]}</Text>
        </View>
      </View>

      <Text style={styles.productName}>{item.productName}</Text>
      <Text style={styles.shopName}>{item.items[0]?.shopName}</Text>

      <View style={styles.orderFooter}>
        <Text style={styles.price}>
          {new Intl.NumberFormat('vi-VN').format(item.productPrice)} VND
        </Text>
        <Text style={styles.commission}>
          Hoa hong: {new Intl.NumberFormat('vi-VN').format(item.commissionAmount)} VND
        </Text>
      </View>

      <Text style={styles.orderTime}>
        {new Date(item.orderTime).toLocaleDateString('vi-VN')}
      </Text>
    </TouchableOpacity>
  );

  const renderStatusFilters = () => (
    <View style={styles.filterContainer}>
      <TouchableOpacity
        style={[styles.filterButton, !statusFilter && styles.filterActive]}
        onPress={() => setStatusFilter(null)}
      >
        <Text>Tat ca</Text>
      </TouchableOpacity>
      {Object.keys(STATUS_LABELS).map(status => (
        <TouchableOpacity
          key={status}
          style={[styles.filterButton, statusFilter === status && styles.filterActive]}
          onPress={() => setStatusFilter(status)}
        >
          <Text>{STATUS_LABELS[status]}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );

  return (
    <View style={styles.container}>
      {renderStatusFilters()}

      <FlatList
        data={orders}
        renderItem={renderOrderItem}
        keyExtractor={item => item.id.toString()}
        onEndReached={loadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={loading ? <ActivityIndicator /> : null}
        ListEmptyComponent={
          !loading && <Text style={styles.emptyText}>Khong co don hang nao</Text>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  filterContainer: { flexDirection: 'row', padding: 10, gap: 8 },
  filterButton: { paddingHorizontal: 12, paddingVertical: 6, backgroundColor: '#e0e0e0', borderRadius: 16 },
  filterActive: { backgroundColor: '#007BFF' },
  orderCard: { backgroundColor: '#fff', margin: 10, padding: 15, borderRadius: 8 },
  orderHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  orderId: { color: '#666', fontSize: 12 },
  statusBadge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: 4 },
  statusText: { color: '#fff', fontSize: 10, fontWeight: 'bold' },
  productName: { fontSize: 16, fontWeight: '500', marginBottom: 4 },
  shopName: { color: '#666', fontSize: 14 },
  orderFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  price: { fontSize: 14 },
  commission: { color: '#28A745', fontSize: 14 },
  orderTime: { color: '#999', fontSize: 12, marginTop: 8 },
  emptyText: { textAlign: 'center', padding: 20, color: '#999' }
});
```

### Web (React + TypeScript) Example

```typescript
// services/orderService.ts
import { getAccessToken } from './authService';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

/**
 * Get current user's orders.
 * Uses /api/orders/me endpoint - userId extracted from JWT automatically.
 */
export async function getMyOrders(
  status: string | null = null,
  page: number = 0,
  size: number = 10
) {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString()
  });

  if (status) {
    params.append('status', status);
  }

  const response = await fetch(
    `${API_BASE_URL}/api/orders/me?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${getAccessToken()}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

/**
 * [ADMIN] Get orders for a specific user by ID.
 * Uses /api/users/{userId}/orders endpoint.
 */
export async function getUserOrdersById(
  userId: number,
  status: string | null = null,
  page: number = 0,
  size: number = 10
) {
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
        'Authorization': `Bearer ${getAccessToken()}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}
```

```typescript
// hooks/useMyOrders.ts
import { useState, useEffect, useCallback } from 'react';
import { getMyOrders } from '../services/orderService';
import { Order, OrderStatus, PageResponse, ApiResponse } from '../types/order';

interface UseMyOrdersOptions {
  initialStatus?: OrderStatus | null;
  pageSize?: number;
}

/**
 * Hook to fetch current user's orders.
 * No need to pass userId - extracted from JWT automatically!
 */
export function useMyOrders({ initialStatus = null, pageSize = 10 }: UseMyOrdersOptions = {}) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | null>(initialStatus);

  const fetchOrders = useCallback(async (pageNum: number, reset: boolean = false) => {
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<PageResponse<Order>> = await getMyOrders(
        statusFilter,
        pageNum,
        pageSize
      );

      if (response.success) {
        const newOrders = response.data.content;
        setOrders(prev => reset ? newOrders : [...prev, ...newOrders]);
        setTotalPages(response.data.totalPages);
        setPage(pageNum);
      } else {
        setError(response.message || 'Failed to load orders');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, pageSize]);

  useEffect(() => {
    fetchOrders(0, true);
  }, [statusFilter, fetchOrders]);

  const loadMore = useCallback(() => {
    if (page < totalPages - 1 && !loading) {
      fetchOrders(page + 1);
    }
  }, [page, totalPages, loading, fetchOrders]);

  const refresh = useCallback(() => {
    fetchOrders(0, true);
  }, [fetchOrders]);

  const changeStatusFilter = useCallback((status: OrderStatus | null) => {
    setStatusFilter(status);
    setPage(0);
  }, []);

  return {
    orders,
    loading,
    error,
    page,
    totalPages,
    statusFilter,
    hasMore: page < totalPages - 1,
    loadMore,
    refresh,
    changeStatusFilter
  };
}
```

```tsx
// components/MyOrderList.tsx
import React from 'react';
import { useMyOrders } from '../hooks/useMyOrders';
import { OrderStatus } from '../types/order';

const STATUS_CONFIG = {
  [OrderStatus.PENDING]: { label: 'Dang cho', color: 'bg-orange-500' },
  [OrderStatus.APPROVED]: { label: 'Da duyet', color: 'bg-blue-500' },
  [OrderStatus.PAID]: { label: 'Da thanh toan', color: 'bg-green-500' },
  [OrderStatus.CANCELLED]: { label: 'Da huy', color: 'bg-red-500' }
};

/**
 * Component to display current user's orders.
 * Uses /api/orders/me endpoint automatically.
 */
export function MyOrderList() {
  const {
    orders,
    loading,
    error,
    statusFilter,
    hasMore,
    loadMore,
    refresh,
    changeStatusFilter
  } = useMyOrders({ pageSize: 10 });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  return (
    <div className="container mx-auto p-4">
      {/* Status Filters */}
      <div className="flex gap-2 mb-4 overflow-x-auto">
        <button
          className={`px-4 py-2 rounded ${!statusFilter ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
          onClick={() => changeStatusFilter(null)}
        >
          Tat ca
        </button>
        {Object.entries(STATUS_CONFIG).map(([status, config]) => (
          <button
            key={status}
            className={`px-4 py-2 rounded whitespace-nowrap ${
              statusFilter === status ? 'bg-blue-500 text-white' : 'bg-gray-200'
            }`}
            onClick={() => changeStatusFilter(status as OrderStatus)}
          >
            {config.label}
          </button>
        ))}
      </div>

      {/* Error State */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
          <button onClick={refresh} className="ml-2 underline">Thu lai</button>
        </div>
      )}

      {/* Orders List */}
      <div className="space-y-4">
        {orders.map(order => (
          <div key={order.id} className="bg-white rounded-lg shadow p-4">
            {/* Header */}
            <div className="flex justify-between items-center mb-2">
              <span className="text-gray-500 text-sm">#{order.orderId}</span>
              <span className={`px-2 py-1 rounded text-white text-xs ${STATUS_CONFIG[order.orderStatus].color}`}>
                {STATUS_CONFIG[order.orderStatus].label}
              </span>
            </div>

            {/* Product Info */}
            <div className="flex gap-4">
              {order.items[0]?.imgUrl && (
                <img
                  src={order.items[0].imgUrl}
                  alt={order.productName}
                  className="w-20 h-20 object-cover rounded"
                />
              )}
              <div className="flex-1">
                <h3 className="font-medium">{order.productName}</h3>
                <p className="text-sm text-gray-500">{order.items[0]?.shopName}</p>
                <p className="text-sm text-gray-400">
                  {new Date(order.orderTime).toLocaleDateString('vi-VN')}
                </p>
              </div>
            </div>

            {/* Footer */}
            <div className="flex justify-between items-center mt-3 pt-3 border-t">
              <div>
                <span className="text-gray-500 text-sm">Gia tri: </span>
                <span className="font-medium">{formatCurrency(order.productPrice)}</span>
              </div>
              <div>
                <span className="text-gray-500 text-sm">Hoa hong: </span>
                <span className="font-medium text-green-600">
                  {formatCurrency(order.commissionAmount)}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Load More */}
      {hasMore && (
        <button
          onClick={loadMore}
          disabled={loading}
          className="w-full mt-4 py-2 bg-gray-100 rounded text-gray-600 hover:bg-gray-200"
        >
          {loading ? 'Dang tai...' : 'Xem them'}
        </button>
      )}

      {/* Empty State */}
      {!loading && orders.length === 0 && (
        <div className="text-center py-8 text-gray-500">
          Khong co don hang nao
        </div>
      )}
    </div>
  );
}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.1.0 | 2025-11-25 | Added `GET /api/orders/me` endpoint for user's own orders using JWT |
| 1.0.0 | 2025-11-25 | Initial release with `GET /api/users/{userId}/orders` |

---

## Contact

- **Backend Team**: backend@cashbee.vn
- **API Issues**: Tao issue tren Plane project CASHBEE
