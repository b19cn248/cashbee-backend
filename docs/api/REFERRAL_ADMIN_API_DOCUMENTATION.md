# Referral Admin API Documentation

> **Module:** Referral System - Admin Management
> **Base URL:** `/api/admin/referral`
> **Version:** 2.0
> **Last Updated:** 2026-01-14

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Data Models](#data-models)
   - [MilestoneConfig](#milestoneconfig)
   - [ReferrerTierConfig](#referrertierconfig)
   - [Enums](#enums)
4. [Milestone Config APIs](#milestone-config-apis)
   - [GET /milestones](#1-get-all-milestones)
   - [GET /milestones/{id}](#2-get-milestone-by-id)
   - [POST /milestones](#3-create-milestone)
   - [PUT /milestones/{id}](#4-update-milestone)
   - [DELETE /milestones/{id}](#5-delete-milestone)
   - [PATCH /milestones/{id}/toggle](#6-toggle-milestone-active-status)
5. [Referrer Tier Config APIs](#referrer-tier-config-apis)
   - [GET /tiers](#7-get-all-tiers)
   - [GET /tiers/{id}](#8-get-tier-by-id)
   - [POST /tiers](#9-create-tier)
   - [PUT /tiers/{id}](#10-update-tier)
   - [PATCH /tiers/{id}/toggle](#11-toggle-tier-active-status)
6. [Error Handling](#error-handling)
7. [Business Rules & Validation](#business-rules--validation)
8. [UI/UX Guidelines](#uiux-guidelines)
9. [Code Examples](#code-examples)

---

## Overview

### Purpose

Tài liệu này mô tả các API dành cho **Admin Web UI** để quản lý:

1. **MilestoneConfig**: Cấu hình các mốc thưởng cho người dùng dựa trên số đơn hàng hoàn thành
2. **ReferrerTierConfig**: Cấu hình các cấp bậc cho người giới thiệu (BRONZE, SILVER, GOLD)

### Current Milestone Configuration (v2.0)

Hệ thống milestone mới được tối ưu với các thay đổi:

| Feature | Old (v1.0) | New (v2.0) |
|---------|------------|------------|
| First Order Bonus | ❌ Không có | ✅ B +5k, A +5k |
| Activation Threshold | 5 đơn | 3 đơn |
| Commission Duration | 5 tháng | 12 tháng |
| Intermediate Milestones | 5, 10, 80, 300 | 1, 3, 20, 40, 80, 150, 300 |

### Referrer Tier System

| Tier | Min Referrals | Commission Rate | Bonus/Activation |
|------|---------------|-----------------|------------------|
| BRONZE | 0 | 5% | 0 VND |
| SILVER | 5 | 7% | 5,000 VND |
| GOLD | 20 | 10% | 10,000 VND |

---

## Authentication & Authorization

### Required Role

```http
Authorization: Bearer <admin_access_token>
X-Admin-Role: ADMIN | SUPER_ADMIN
```

Tất cả các API trong module này yêu cầu:
- JWT token từ Keycloak với role `ADMIN` hoặc `SUPER_ADMIN`
- HTTP Header `X-Admin-Role` để xác định quyền

### Permission Matrix

| API | ADMIN | SUPER_ADMIN |
|-----|-------|-------------|
| GET (List/Read) | ✅ | ✅ |
| POST (Create) | ✅ | ✅ |
| PUT (Update) | ✅ | ✅ |
| DELETE (Delete) | ❌ | ✅ |
| Toggle Active | ✅ | ✅ |

---

## Data Models

### MilestoneConfig

Cấu hình mốc thưởng cho người dùng dựa trên số đơn hàng hoàn thành.

```json
{
  "id": 1,
  "milestoneType": "WITH_REFERRER",
  "ordersRequired": 3,
  "refereeBonus": 10000.00,
  "referrerBonus": 20000.00,
  "newTier": null,
  "commissionMonths": 12,
  "activatesReferral": true,
  "description": "Referral activation: B +10k, A +20k, 12 months commission",
  "isActive": true,
  "createdAt": "2026-01-14T10:00:00",
  "updatedAt": "2026-01-14T10:00:00"
}
```

#### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | Long | Auto | ID tự động sinh |
| `milestoneType` | Enum | ✅ | `WITH_REFERRER` hoặc `WITHOUT_REFERRER` |
| `ordersRequired` | Integer | ✅ | Số đơn cần hoàn thành để đạt mốc (> 0) |
| `refereeBonus` | BigDecimal | ✅ | Bonus cho người được giới thiệu (VND) |
| `referrerBonus` | BigDecimal | ✅ | Bonus cho người giới thiệu (VND) |
| `newTier` | Enum | No | Tier mới nếu có nâng cấp: `VIP`, `SUPER`, `DIAMOND` |
| `commissionMonths` | Integer | ✅ | Số tháng nhận hoa hồng (0 nếu không áp dụng) |
| `activatesReferral` | Boolean | ✅ | Mốc này có kích hoạt referral không |
| `description` | String | No | Mô tả mốc thưởng |
| `isActive` | Boolean | ✅ | Trạng thái hoạt động |
| `createdAt` | DateTime | Auto | Thời điểm tạo |
| `updatedAt` | DateTime | Auto | Thời điểm cập nhật |

### ReferrerTierConfig

Cấu hình cấp bậc cho người giới thiệu.

```json
{
  "id": 1,
  "tierName": "SILVER",
  "minReferrals": 5,
  "commissionRate": 7.00,
  "bonusPerActivation": 5000.00,
  "description": "Silver tier: 7% commission + 5k bonus per activation",
  "isActive": true,
  "createdAt": "2026-01-14T10:00:00",
  "updatedAt": "2026-01-14T10:00:00"
}
```

#### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | Long | Auto | ID tự động sinh |
| `tierName` | String | ✅ | Tên tier: `BRONZE`, `SILVER`, `GOLD` (unique, max 30 chars) |
| `minReferrals` | Integer | ✅ | Số referral tối thiểu để đạt tier (≥ 0) |
| `commissionRate` | BigDecimal | ✅ | % hoa hồng (VD: 5.00 = 5%) |
| `bonusPerActivation` | BigDecimal | ✅ | Bonus khi có referral mới kích hoạt (VND) |
| `description` | String | No | Mô tả tier |
| `isActive` | Boolean | ✅ | Trạng thái hoạt động |
| `createdAt` | DateTime | Auto | Thời điểm tạo |
| `updatedAt` | DateTime | Auto | Thời điểm cập nhật |

### Enums

#### MilestoneType

```java
public enum MilestoneType {
    WITH_REFERRER,      // User đã nhập mã giới thiệu
    WITHOUT_REFERRER    // User không nhập mã giới thiệu
}
```

| Value | Description |
|-------|-------------|
| `WITH_REFERRER` | Các mốc dành cho user đã nhập mã giới thiệu từ người khác |
| `WITHOUT_REFERRER` | Các mốc dành cho user không nhập mã giới thiệu |

#### UserLevel

```java
public enum UserLevel {
    NORMAL(80),   // 80% cashback
    VIP(83),      // 83% cashback - unlocked at 40 orders (was 80)
    SUPER(85),    // 85% cashback - unlocked at 150 orders (was 300)
    DIAMOND(100)  // 100% cashback - admin assigned only
}
```

| Value | Cashback Rate | Unlock Condition |
|-------|---------------|------------------|
| `NORMAL` | 80% | Default |
| `VIP` | 83% | 40 đơn hoàn thành (was 80) |
| `SUPER` | 85% | 150 đơn hoàn thành (was 300) |
| `DIAMOND` | 100% | Chỉ admin có thể gán |

---

## Milestone Config APIs

### 1. Get All Milestones

Lấy danh sách tất cả milestone configs.

```http
GET /api/admin/referral/milestones
```

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | No | Filter by type: `WITH_REFERRER`, `WITHOUT_REFERRER` |
| `activeOnly` | Boolean | No | Chỉ lấy các milestone đang active (default: false) |
| `sortBy` | String | No | Sort field: `ordersRequired`, `createdAt` (default: `ordersRequired`) |
| `sortDir` | String | No | Sort direction: `asc`, `desc` (default: `asc`) |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "milestones": [
      {
        "id": 1,
        "milestoneType": "WITH_REFERRER",
        "ordersRequired": 1,
        "refereeBonus": 5000.00,
        "referrerBonus": 5000.00,
        "newTier": null,
        "commissionMonths": 0,
        "activatesReferral": false,
        "description": "First Order Bonus: B +5k, A +5k",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      },
      {
        "id": 2,
        "milestoneType": "WITH_REFERRER",
        "ordersRequired": 3,
        "refereeBonus": 10000.00,
        "referrerBonus": 20000.00,
        "newTier": null,
        "commissionMonths": 12,
        "activatesReferral": true,
        "description": "Referral activation: B +10k, A +20k, 12 months commission",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      },
      {
        "id": 3,
        "milestoneType": "WITH_REFERRER",
        "ordersRequired": 20,
        "refereeBonus": 15000.00,
        "referrerBonus": 0.00,
        "newTier": null,
        "commissionMonths": 0,
        "activatesReferral": false,
        "description": "Milestone bonus: B +15k",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      }
    ],
    "summary": {
      "totalActive": 10,
      "totalInactive": 1,
      "withReferrerCount": 7,
      "withoutReferrerCount": 4
    }
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 2. Get Milestone by ID

Lấy thông tin chi tiết một milestone.

```http
GET /api/admin/referral/milestones/{id}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Milestone config ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 2,
    "milestoneType": "WITH_REFERRER",
    "ordersRequired": 3,
    "refereeBonus": 10000.00,
    "referrerBonus": 20000.00,
    "newTier": null,
    "commissionMonths": 12,
    "activatesReferral": true,
    "description": "Referral activation: B +10k, A +20k, 12 months commission",
    "isActive": true,
    "createdAt": "2026-01-14T10:00:00",
    "updatedAt": "2026-01-14T10:00:00"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

#### Error Response (404 Not Found)

```json
{
  "success": false,
  "message": "Milestone config not found",
  "error": {
    "code": "MILESTONE_NOT_FOUND",
    "details": "No milestone config found with ID: 999"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 3. Create Milestone

Tạo mới một milestone config.

```http
POST /api/admin/referral/milestones
```

#### Request Body

```json
{
  "milestoneType": "WITH_REFERRER",
  "ordersRequired": 50,
  "refereeBonus": 25000.00,
  "referrerBonus": 0.00,
  "newTier": null,
  "commissionMonths": 0,
  "activatesReferral": false,
  "description": "Milestone 50 orders: B +25k"
}
```

#### Request Validation

| Field | Validation Rules |
|-------|------------------|
| `milestoneType` | Required, must be `WITH_REFERRER` or `WITHOUT_REFERRER` |
| `ordersRequired` | Required, > 0, unique per milestoneType |
| `refereeBonus` | Required, ≥ 0 |
| `referrerBonus` | Required, ≥ 0 |
| `newTier` | Optional, if set must be `VIP`, `SUPER`, or `DIAMOND` |
| `commissionMonths` | Required, ≥ 0 |
| `activatesReferral` | Required |

#### Response (201 Created)

```json
{
  "success": true,
  "message": "Milestone config created successfully",
  "data": {
    "id": 12,
    "milestoneType": "WITH_REFERRER",
    "ordersRequired": 50,
    "refereeBonus": 25000.00,
    "referrerBonus": 0.00,
    "newTier": null,
    "commissionMonths": 0,
    "activatesReferral": false,
    "description": "Milestone 50 orders: B +25k",
    "isActive": true,
    "createdAt": "2026-01-14T10:30:00",
    "updatedAt": "2026-01-14T10:30:00"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

#### Error Response (400 Bad Request)

```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "MILESTONE_DUPLICATE",
    "details": "A milestone config with type 'WITH_REFERRER' and 50 orders already exists"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 4. Update Milestone

Cập nhật thông tin milestone config.

```http
PUT /api/admin/referral/milestones/{id}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Milestone config ID |

#### Request Body

```json
{
  "ordersRequired": 50,
  "refereeBonus": 30000.00,
  "referrerBonus": 5000.00,
  "newTier": null,
  "commissionMonths": 0,
  "activatesReferral": false,
  "description": "Updated: Milestone 50 orders: B +30k, A +5k"
}
```

> **Note:** `milestoneType` không thể thay đổi sau khi tạo.

#### Response (200 OK)

```json
{
  "success": true,
  "message": "Milestone config updated successfully",
  "data": {
    "id": 12,
    "milestoneType": "WITH_REFERRER",
    "ordersRequired": 50,
    "refereeBonus": 30000.00,
    "referrerBonus": 5000.00,
    "newTier": null,
    "commissionMonths": 0,
    "activatesReferral": false,
    "description": "Updated: Milestone 50 orders: B +30k, A +5k",
    "isActive": true,
    "createdAt": "2026-01-14T10:00:00",
    "updatedAt": "2026-01-14T11:00:00"
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

---

### 5. Delete Milestone

Xóa một milestone config.

```http
DELETE /api/admin/referral/milestones/{id}
```

> **Permission:** Chỉ `SUPER_ADMIN` mới có quyền xóa.

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Milestone config ID |

#### Response (200 OK)

```json
{
  "success": true,
  "message": "Milestone config deleted successfully",
  "data": {
    "deletedId": 12
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

#### Error Response (403 Forbidden)

```json
{
  "success": false,
  "message": "Access denied",
  "error": {
    "code": "INSUFFICIENT_PERMISSION",
    "details": "Only SUPER_ADMIN can delete milestone configs"
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

---

### 6. Toggle Milestone Active Status

Bật/tắt trạng thái active của milestone.

```http
PATCH /api/admin/referral/milestones/{id}/toggle
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Milestone config ID |

#### Response (200 OK)

```json
{
  "success": true,
  "message": "Milestone config status updated",
  "data": {
    "id": 12,
    "isActive": false,
    "previousStatus": true
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

---

## Referrer Tier Config APIs

### 7. Get All Tiers

Lấy danh sách tất cả referrer tier configs.

```http
GET /api/admin/referral/tiers
```

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `activeOnly` | Boolean | No | Chỉ lấy các tier đang active (default: false) |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "tiers": [
      {
        "id": 1,
        "tierName": "BRONZE",
        "minReferrals": 0,
        "commissionRate": 5.00,
        "bonusPerActivation": 0.00,
        "description": "Default tier: 5% commission",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      },
      {
        "id": 2,
        "tierName": "SILVER",
        "minReferrals": 5,
        "commissionRate": 7.00,
        "bonusPerActivation": 5000.00,
        "description": "Silver tier: 7% commission + 5k bonus per activation",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      },
      {
        "id": 3,
        "tierName": "GOLD",
        "minReferrals": 20,
        "commissionRate": 10.00,
        "bonusPerActivation": 10000.00,
        "description": "Gold tier: 10% commission + 10k bonus per activation",
        "isActive": true,
        "createdAt": "2026-01-14T10:00:00",
        "updatedAt": "2026-01-14T10:00:00"
      }
    ],
    "summary": {
      "totalActive": 3,
      "totalInactive": 0
    }
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 8. Get Tier by ID

Lấy thông tin chi tiết một tier.

```http
GET /api/admin/referral/tiers/{id}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Tier config ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 2,
    "tierName": "SILVER",
    "minReferrals": 5,
    "commissionRate": 7.00,
    "bonusPerActivation": 5000.00,
    "description": "Silver tier: 7% commission + 5k bonus per activation",
    "isActive": true,
    "createdAt": "2026-01-14T10:00:00",
    "updatedAt": "2026-01-14T10:00:00"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 9. Create Tier

Tạo mới một referrer tier config.

```http
POST /api/admin/referral/tiers
```

#### Request Body

```json
{
  "tierName": "PLATINUM",
  "minReferrals": 50,
  "commissionRate": 12.00,
  "bonusPerActivation": 15000.00,
  "description": "Platinum tier: 12% commission + 15k bonus per activation"
}
```

#### Request Validation

| Field | Validation Rules |
|-------|------------------|
| `tierName` | Required, unique, max 30 chars, uppercase recommended |
| `minReferrals` | Required, ≥ 0, unique |
| `commissionRate` | Required, 0-100 |
| `bonusPerActivation` | Required, ≥ 0 |

#### Response (201 Created)

```json
{
  "success": true,
  "message": "Referrer tier config created successfully",
  "data": {
    "id": 4,
    "tierName": "PLATINUM",
    "minReferrals": 50,
    "commissionRate": 12.00,
    "bonusPerActivation": 15000.00,
    "description": "Platinum tier: 12% commission + 15k bonus per activation",
    "isActive": true,
    "createdAt": "2026-01-14T10:30:00",
    "updatedAt": "2026-01-14T10:30:00"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

---

### 10. Update Tier

Cập nhật thông tin tier config.

```http
PUT /api/admin/referral/tiers/{id}
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Tier config ID |

#### Request Body

```json
{
  "minReferrals": 50,
  "commissionRate": 15.00,
  "bonusPerActivation": 20000.00,
  "description": "Updated Platinum tier: 15% commission + 20k bonus per activation"
}
```

> **Note:** `tierName` không thể thay đổi sau khi tạo.

#### Response (200 OK)

```json
{
  "success": true,
  "message": "Referrer tier config updated successfully",
  "data": {
    "id": 4,
    "tierName": "PLATINUM",
    "minReferrals": 50,
    "commissionRate": 15.00,
    "bonusPerActivation": 20000.00,
    "description": "Updated Platinum tier: 15% commission + 20k bonus per activation",
    "isActive": true,
    "createdAt": "2026-01-14T10:00:00",
    "updatedAt": "2026-01-14T11:00:00"
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

---

### 11. Toggle Tier Active Status

Bật/tắt trạng thái active của tier.

```http
PATCH /api/admin/referral/tiers/{id}/toggle
```

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Tier config ID |

#### Response (200 OK)

```json
{
  "success": true,
  "message": "Referrer tier config status updated",
  "data": {
    "id": 4,
    "tierName": "PLATINUM",
    "isActive": false,
    "previousStatus": true
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

#### Error Response (400 Bad Request)

```json
{
  "success": false,
  "message": "Cannot deactivate tier",
  "error": {
    "code": "TIER_IN_USE",
    "details": "Cannot deactivate BRONZE tier as it is the default tier"
  },
  "timestamp": "2026-01-14T11:00:00"
}
```

---

## Error Handling

### Error Response Format

```json
{
  "success": false,
  "message": "Human-readable error message",
  "error": {
    "code": "ERROR_CODE",
    "details": "Detailed error information",
    "field": "fieldName"
  },
  "timestamp": "2026-01-14T10:30:00"
}
```

### Error Codes Reference

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `MILESTONE_NOT_FOUND` | 404 | Milestone config không tồn tại |
| `MILESTONE_DUPLICATE` | 400 | Đã có milestone với type + orders tương tự |
| `TIER_NOT_FOUND` | 404 | Tier config không tồn tại |
| `TIER_DUPLICATE` | 400 | Đã có tier với tên hoặc minReferrals tương tự |
| `TIER_IN_USE` | 400 | Không thể xóa/deactivate tier đang được sử dụng |
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `INSUFFICIENT_PERMISSION` | 403 | Không đủ quyền thực hiện action |
| `UNAUTHORIZED` | 401 | Token không hợp lệ hoặc hết hạn |

---

## Business Rules & Validation

### Milestone Config Rules

1. **Unique Constraint:** Mỗi cặp (milestoneType, ordersRequired) phải là duy nhất
2. **Orders Required:** Phải > 0
3. **Bonus Values:** Không được âm (≥ 0)
4. **Commission Months:** Chỉ có ý nghĩa khi `activatesReferral = true`
5. **New Tier:** Nếu set, phải là `VIP`, `SUPER`, hoặc `DIAMOND`
6. **Type Immutable:** Không thể thay đổi `milestoneType` sau khi tạo

### Referrer Tier Rules

1. **Unique TierName:** Tên tier phải unique
2. **Unique MinReferrals:** Mỗi tier phải có `minReferrals` khác nhau
3. **Commission Rate:** Phải trong khoảng 0-100
4. **Default Tier:** Phải có ít nhất 1 tier với `minReferrals = 0` (BRONZE)
5. **Cannot Delete Default:** Không thể xóa tier BRONZE (default tier)
6. **Name Immutable:** Không thể thay đổi `tierName` sau khi tạo

### Best Practices

```
✅ Luôn test trên staging trước khi thay đổi production
✅ Backup data trước khi xóa milestone/tier
✅ Deactivate thay vì xóa để giữ lịch sử
✅ Thông báo cho team khi có thay đổi config quan trọng
✅ Cập nhật documentation khi thêm milestone/tier mới
```

---

## UI/UX Guidelines

### Milestone Config Management Screen

```
┌─────────────────────────────────────────────────────────────────┐
│         QUẢN LÝ CẤU HÌNH MILESTONE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Tab: WITH_REFERRER] [Tab: WITHOUT_REFERRER]    [+ Thêm mới]   │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  Filter: [All ▼] [Active ▼]              Search: [_________]   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ # │ Orders │ Referee    │ Referrer   │ Tier  │ Active  │    │
│  ├───┼────────┼────────────┼────────────┼───────┼─────────┤    │
│  │ 1 │ 1      │ 5,000 VND  │ 5,000 VND  │ -     │ ✅ [⋮] │    │
│  │ 2 │ 3 ⭐   │ 10,000 VND │ 20,000 VND │ -     │ ✅ [⋮] │    │
│  │ 3 │ 20     │ 15,000 VND │ -          │ -     │ ✅ [⋮] │    │
│  │ 4 │ 40     │ 20,000 VND │ -          │ VIP   │ ✅ [⋮] │    │
│  │ 5 │ 80     │ -          │ -          │ VIP   │ ❌ [⋮] │    │
│  │ 6 │ 150    │ 50,000 VND │ -          │ SUPER │ ✅ [⋮] │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ⭐ = Milestone kích hoạt referral                              │
│                                                                 │
│  [Previous] Page 1 of 1 [Next]                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Milestone Create/Edit Modal

```
┌─────────────────────────────────────────────────────────────────┐
│         THÊM MILESTONE MỚI                            [X]       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Loại milestone *                                               │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ○ Có mã giới thiệu (WITH_REFERRER)                      │    │
│  │ ○ Không có mã giới thiệu (WITHOUT_REFERRER)             │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Số đơn yêu cầu *            Kích hoạt referral                 │
│  ┌──────────────────┐        ┌────────────────────────┐         │
│  │ 50               │        │ ☐ Có                   │         │
│  └──────────────────┘        └────────────────────────┘         │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  PHẦN THƯỞNG                                                    │
│                                                                 │
│  Bonus cho Referee *         Bonus cho Referrer *               │
│  ┌──────────────────┐        ┌──────────────────┐               │
│  │ 25,000       VND │        │ 0            VND │               │
│  └──────────────────┘        └──────────────────┘               │
│                                                                 │
│  Nâng cấp tier               Số tháng hoa hồng                  │
│  ┌──────────────────┐        ┌──────────────────┐               │
│  │ Không nâng cấp ▼ │        │ 0           tháng│               │
│  └──────────────────┘        └──────────────────┘               │
│                                                                 │
│  Mô tả                                                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Milestone 50 orders: Referee +25k                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│              [Hủy]                    [Lưu]                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Referrer Tier Management Screen

```
┌─────────────────────────────────────────────────────────────────┐
│         QUẢN LÝ CẤP BẬC NGƯỜI GIỚI THIỆU                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                                                   [+ Thêm mới]  │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ # │ Tên     │ Min Refs │ Commission │ Bonus/Act │ Active│    │
│  ├───┼─────────┼──────────┼────────────┼───────────┼───────┤    │
│  │ 1 │ BRONZE  │ 0        │ 5%         │ -         │ ✅ 🔒 │    │
│  │ 2 │ SILVER  │ 5        │ 7%         │ 5,000     │ ✅ [⋮]│    │
│  │ 3 │ GOLD    │ 20       │ 10%        │ 10,000    │ ✅ [⋮]│    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  🔒 = Tier mặc định, không thể xóa/deactivate                   │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  THỐNG KÊ                                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 📊 BRONZE: 1,234 users (78%)                            │    │
│  │ 📊 SILVER: 256 users (16%)                              │    │
│  │ 📊 GOLD: 98 users (6%)                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Color Coding

| Element | Color | Hex Code |
|---------|-------|----------|
| Active Status ✅ | Green | `#4CAF50` |
| Inactive Status ❌ | Red | `#F44336` |
| Default/Locked 🔒 | Gray | `#9E9E9E` |
| Special Milestone ⭐ | Yellow | `#FFC107` |
| VIP Tier | Gold | `#FFD700` |
| SUPER Tier | Purple | `#9C27B0` |
| DIAMOND Tier | Cyan | `#00BCD4` |

---

## Code Examples

### React Admin Component

```typescript
// types/referral.ts
export interface MilestoneConfig {
  id: number;
  milestoneType: 'WITH_REFERRER' | 'WITHOUT_REFERRER';
  ordersRequired: number;
  refereeBonus: number;
  referrerBonus: number;
  newTier: 'VIP' | 'SUPER' | 'DIAMOND' | null;
  commissionMonths: number;
  activatesReferral: boolean;
  description: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReferrerTierConfig {
  id: number;
  tierName: string;
  minReferrals: number;
  commissionRate: number;
  bonusPerActivation: number;
  description: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

// api/referralAdminApi.ts
import axios from 'axios';

const API_BASE = '/api/admin/referral';

export const referralAdminApi = {
  // Milestone APIs
  getMilestones: async (params?: {
    type?: string;
    activeOnly?: boolean;
  }) => {
    const response = await axios.get(`${API_BASE}/milestones`, { params });
    return response.data.data;
  },

  getMilestone: async (id: number) => {
    const response = await axios.get(`${API_BASE}/milestones/${id}`);
    return response.data.data;
  },

  createMilestone: async (data: Omit<MilestoneConfig, 'id' | 'createdAt' | 'updatedAt' | 'isActive'>) => {
    const response = await axios.post(`${API_BASE}/milestones`, data);
    return response.data.data;
  },

  updateMilestone: async (id: number, data: Partial<MilestoneConfig>) => {
    const response = await axios.put(`${API_BASE}/milestones/${id}`, data);
    return response.data.data;
  },

  deleteMilestone: async (id: number) => {
    const response = await axios.delete(`${API_BASE}/milestones/${id}`);
    return response.data.data;
  },

  toggleMilestone: async (id: number) => {
    const response = await axios.patch(`${API_BASE}/milestones/${id}/toggle`);
    return response.data.data;
  },

  // Tier APIs
  getTiers: async (activeOnly?: boolean) => {
    const response = await axios.get(`${API_BASE}/tiers`, {
      params: { activeOnly }
    });
    return response.data.data;
  },

  getTier: async (id: number) => {
    const response = await axios.get(`${API_BASE}/tiers/${id}`);
    return response.data.data;
  },

  createTier: async (data: Omit<ReferrerTierConfig, 'id' | 'createdAt' | 'updatedAt' | 'isActive'>) => {
    const response = await axios.post(`${API_BASE}/tiers`, data);
    return response.data.data;
  },

  updateTier: async (id: number, data: Partial<ReferrerTierConfig>) => {
    const response = await axios.put(`${API_BASE}/tiers/${id}`, data);
    return response.data.data;
  },

  toggleTier: async (id: number) => {
    const response = await axios.patch(`${API_BASE}/tiers/${id}/toggle`);
    return response.data.data;
  }
};
```

### React Hook for Milestone Management

```typescript
// hooks/useMilestones.ts
import { useState, useEffect, useCallback } from 'react';
import { referralAdminApi } from '../api/referralAdminApi';
import { MilestoneConfig } from '../types/referral';

export const useMilestones = (type?: string) => {
  const [milestones, setMilestones] = useState<MilestoneConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchMilestones = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await referralAdminApi.getMilestones({ type });
      setMilestones(data.milestones);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch milestones');
    } finally {
      setLoading(false);
    }
  }, [type]);

  useEffect(() => {
    fetchMilestones();
  }, [fetchMilestones]);

  const createMilestone = async (data: Omit<MilestoneConfig, 'id' | 'createdAt' | 'updatedAt' | 'isActive'>) => {
    const result = await referralAdminApi.createMilestone(data);
    await fetchMilestones();
    return result;
  };

  const updateMilestone = async (id: number, data: Partial<MilestoneConfig>) => {
    const result = await referralAdminApi.updateMilestone(id, data);
    await fetchMilestones();
    return result;
  };

  const deleteMilestone = async (id: number) => {
    await referralAdminApi.deleteMilestone(id);
    await fetchMilestones();
  };

  const toggleMilestone = async (id: number) => {
    const result = await referralAdminApi.toggleMilestone(id);
    await fetchMilestones();
    return result;
  };

  return {
    milestones,
    loading,
    error,
    fetchMilestones,
    createMilestone,
    updateMilestone,
    deleteMilestone,
    toggleMilestone
  };
};
```

### Milestone Table Component

```tsx
// components/MilestoneTable.tsx
import React from 'react';
import { MilestoneConfig } from '../types/referral';

interface MilestoneTableProps {
  milestones: MilestoneConfig[];
  onEdit: (milestone: MilestoneConfig) => void;
  onToggle: (id: number) => void;
  onDelete: (id: number) => void;
}

export const MilestoneTable: React.FC<MilestoneTableProps> = ({
  milestones,
  onEdit,
  onToggle,
  onDelete
}) => {
  const formatCurrency = (value: number) => {
    if (value === 0) return '-';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  };

  return (
    <table className="w-full border-collapse">
      <thead>
        <tr className="bg-gray-100">
          <th className="p-2 text-left">#</th>
          <th className="p-2 text-left">Orders</th>
          <th className="p-2 text-right">Referee Bonus</th>
          <th className="p-2 text-right">Referrer Bonus</th>
          <th className="p-2 text-center">Tier</th>
          <th className="p-2 text-center">Commission</th>
          <th className="p-2 text-center">Status</th>
          <th className="p-2 text-center">Actions</th>
        </tr>
      </thead>
      <tbody>
        {milestones.map((milestone, index) => (
          <tr key={milestone.id} className="border-b hover:bg-gray-50">
            <td className="p-2">{index + 1}</td>
            <td className="p-2">
              {milestone.ordersRequired}
              {milestone.activatesReferral && (
                <span className="ml-1 text-yellow-500" title="Activates Referral">⭐</span>
              )}
            </td>
            <td className="p-2 text-right">{formatCurrency(milestone.refereeBonus)}</td>
            <td className="p-2 text-right">{formatCurrency(milestone.referrerBonus)}</td>
            <td className="p-2 text-center">
              {milestone.newTier ? (
                <span className={`px-2 py-1 rounded text-xs ${
                  milestone.newTier === 'VIP' ? 'bg-yellow-100 text-yellow-800' :
                  milestone.newTier === 'SUPER' ? 'bg-purple-100 text-purple-800' :
                  'bg-cyan-100 text-cyan-800'
                }`}>
                  {milestone.newTier}
                </span>
              ) : '-'}
            </td>
            <td className="p-2 text-center">
              {milestone.commissionMonths > 0 ? `${milestone.commissionMonths} months` : '-'}
            </td>
            <td className="p-2 text-center">
              <button
                onClick={() => onToggle(milestone.id)}
                className={`px-2 py-1 rounded text-xs ${
                  milestone.isActive
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {milestone.isActive ? '✅ Active' : '❌ Inactive'}
              </button>
            </td>
            <td className="p-2 text-center">
              <button
                onClick={() => onEdit(milestone)}
                className="mr-2 text-blue-600 hover:text-blue-800"
              >
                Edit
              </button>
              <button
                onClick={() => onDelete(milestone.id)}
                className="text-red-600 hover:text-red-800"
              >
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
```

---

## API Testing with cURL

```bash
# Get all milestones
curl -X GET "https://api.cashbee.vn/api/admin/referral/milestones" \
  -H "Authorization: Bearer <admin_token>"

# Get milestones by type
curl -X GET "https://api.cashbee.vn/api/admin/referral/milestones?type=WITH_REFERRER&activeOnly=true" \
  -H "Authorization: Bearer <admin_token>"

# Create milestone
curl -X POST "https://api.cashbee.vn/api/admin/referral/milestones" \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "milestoneType": "WITH_REFERRER",
    "ordersRequired": 50,
    "refereeBonus": 25000.00,
    "referrerBonus": 0.00,
    "commissionMonths": 0,
    "activatesReferral": false,
    "description": "Milestone 50 orders: B +25k"
  }'

# Update milestone
curl -X PUT "https://api.cashbee.vn/api/admin/referral/milestones/12" \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "refereeBonus": 30000.00,
    "description": "Updated: Milestone 50 orders: B +30k"
  }'

# Toggle milestone status
curl -X PATCH "https://api.cashbee.vn/api/admin/referral/milestones/12/toggle" \
  -H "Authorization: Bearer <admin_token>"

# Delete milestone
curl -X DELETE "https://api.cashbee.vn/api/admin/referral/milestones/12" \
  -H "Authorization: Bearer <admin_token>"

# Get all tiers
curl -X GET "https://api.cashbee.vn/api/admin/referral/tiers" \
  -H "Authorization: Bearer <admin_token>"

# Create tier
curl -X POST "https://api.cashbee.vn/api/admin/referral/tiers" \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "tierName": "PLATINUM",
    "minReferrals": 50,
    "commissionRate": 12.00,
    "bonusPerActivation": 15000.00,
    "description": "Platinum tier: 12% commission + 15k bonus"
  }'
```

---

## Database Schema Reference

### milestone_config Table

```sql
CREATE TABLE milestone_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    milestone_type ENUM('WITH_REFERRER', 'WITHOUT_REFERRER') NOT NULL,
    orders_required INT NOT NULL,
    referee_bonus DECIMAL(19,2) NOT NULL DEFAULT 0,
    referrer_bonus DECIMAL(19,2) NOT NULL DEFAULT 0,
    new_tier VARCHAR(20) NULL,
    commission_months INT NOT NULL DEFAULT 0,
    activates_referral TINYINT(1) NOT NULL DEFAULT 0,
    description VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_milestone_type_orders (milestone_type, orders_required),
    INDEX idx_milestone_type (milestone_type),
    INDEX idx_is_active (is_active)
);
```

### referrer_tier_config Table

```sql
CREATE TABLE referrer_tier_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tier_name VARCHAR(30) NOT NULL UNIQUE,
    min_referrals INT NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL,
    bonus_per_activation DECIMAL(19,2) NOT NULL DEFAULT 0,
    description VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_min_referrals (min_referrals),
    INDEX idx_min_referrals (min_referrals)
);
```

---

## Contact

Nếu có thắc mắc về API, vui lòng liên hệ:

- **Backend Team:** backend@cashbee.vn
- **Slack Channel:** #dev-api-support

---

*Document generated: 2026-01-14*
*Version: 2.0*
