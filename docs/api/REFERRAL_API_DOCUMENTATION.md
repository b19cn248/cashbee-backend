# Referral API Documentation

> **Module:** Referral System
> **Base URL:** `/api/referral`
> **Version:** 1.0
> **Last Updated:** 2025-12-30

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
   - [POST /set-code](#1-set-referral-code)
   - [GET /validate/{code}](#2-validate-referral-code)
   - [GET /stats](#3-get-referral-statistics)
   - [GET /my-code](#4-get-my-referral-code)
   - [GET /my-referrals](#5-get-my-referrals) ⭐ NEW
4. [Data Models](#data-models)
5. [Error Handling](#error-handling)
6. [Business Rules](#business-rules)
7. [UI/UX Guidelines](#uiux-guidelines)
8. [Code Examples](#code-examples)

---

## Overview

### What is the Referral System?

Hệ thống giới thiệu cho phép users giới thiệu bạn bè và nhận thưởng. Có 2 vai trò:

| Vai trò | Mô tả | Lợi ích |
|---------|-------|---------|
| **Referrer** (Người giới thiệu) | User share mã giới thiệu cho bạn bè | Nhận hoa hồng từ đơn hàng của referee |
| **Referee** (Người được giới thiệu) | User nhập mã giới thiệu từ người khác | Nhận bonus khi đạt milestone |

### Referral Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           REFERRAL FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────┐     Share Code      ┌──────────┐                              │
│  │ REFERRER │ ─────────────────── │ REFEREE  │                              │
│  │          │     "ABC123XY"      │          │                              │
│  └────┬─────┘                     └────┬─────┘                              │
│       │                                │                                    │
│       │                                ▼                                    │
│       │                      ┌─────────────────┐                            │
│       │                      │ Enter Code      │                            │
│       │                      │ (set-code API)  │                            │
│       │                      └────────┬────────┘                            │
│       │                               │                                     │
│       │                               ▼                                     │
│       │                      ┌─────────────────┐                            │
│       │                      │ Complete Orders │                            │
│       │                      │ 5 → 10 → 80 →   │                            │
│       │                      │ 300 orders      │                            │
│       │                      └────────┬────────┘                            │
│       │                               │                                     │
│       ▼                               ▼                                     │
│  ┌─────────────┐             ┌─────────────────┐                            │
│  │ Earn        │             │ Earn Milestone  │                            │
│  │ Commission  │             │ Rewards         │                            │
│  │ (5 months)  │             │ (Bonus + Tier)  │                            │
│  └─────────────┘             └─────────────────┘                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Milestone Rewards (For Referee - WITH_REFERRER)

> ⚠️ **KNOWN ISSUE:** API `/api/referral/stats` hiện đang trả về milestone values cũ (3, 10, 40, 150) do hardcode trong `GetReferralStatsUseCase.java`. Database config thực tế là (5, 10, 80, 300). Business logic xử lý theo database config. Cần fix code để đồng bộ.

| Milestone | Orders | Referee Reward | Referrer Reward | Note |
|-----------|--------|----------------|-----------------|------|
| 1 | 5 | 10,000 VND bonus | 20,000 VND bonus | Kích hoạt referral + Commission 5 tháng |
| 2 | 10 | 20,000 VND bonus | - | |
| 3 | 80 | Nâng cấp VIP tier (83% cashback) | - | |
| 4 | 300 | Nâng cấp SUPER tier (85% cashback) | - | |

### Milestone Rewards (For User WITHOUT_REFERRER)

| Milestone | Orders | Reward |
|-----------|--------|--------|
| 1 | 80 | Nâng cấp VIP tier (83% cashback) |
| 2 | 300 | Nâng cấp SUPER tier (85% cashback) |

---

## Authentication

Tất cả các API đều yêu cầu **Bearer Token** (JWT từ Keycloak):

```http
Authorization: Bearer <access_token>
```

### Get Token Example

```javascript
// Login to get token
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});
const { accessToken } = await loginResponse.json();
```

---

## API Endpoints

### 1. Set Referral Code

Đặt mã giới thiệu cho user (trở thành referee).

#### Endpoint

```http
POST /api/referral/set-code
```

#### When to Use

- Khi user đăng ký và nhập mã giới thiệu
- Khi user thêm tài khoản ngân hàng và nhập mã giới thiệu
- Trong màn hình "Nhập mã giới thiệu" riêng biệt

#### Request

**Headers:**
```http
Authorization: Bearer <token>
Content-Type: application/json
```

**Body:**
```json
{
  "referralCode": "ABC123XY"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `referralCode` | string | Yes | 6-12 ký tự, chỉ alphanumeric (A-Z, a-z, 0-9) |

#### Response

**Success (200 OK):**
```json
{
  "success": true,
  "message": "Referral code applied successfully",
  "data": {
    "referralCode": "ABC123XY",
    "referrerId": 12345,
    "referrerName": "Ng********",
    "message": "Referral code applied successfully! Complete 5 orders to activate referral benefits.",
    "success": true
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

**Error Responses:**

| HTTP Code | Error Code | Message | Khi nào xảy ra |
|-----------|------------|---------|----------------|
| 400 | `REFERRAL_CODE_ALREADY_SET` | User already has a referral code | User đã nhập mã trước đó |
| 400 | `SELF_REFERRAL` | Cannot use your own referral code | User nhập mã của chính mình |
| 404 | `REFERRAL_CODE_NOT_FOUND` | Referral code not found | Mã không tồn tại |
| 400 | `REFERRER_INACTIVE` | Referral code belongs to inactive user | User sở hữu mã đã bị deactivate |
| 400 | `VALIDATION_ERROR` | Validation failed | Input không hợp lệ |

**Error Response Format:**
```json
{
  "success": false,
  "message": "User already has a referral code",
  "error": {
    "code": "REFERRAL_CODE_ALREADY_SET",
    "details": "You have already set referral code: XYZ789AB"
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

#### Mobile Implementation Notes

```
✅ Gọi validate API trước để check realtime
✅ Hiển thị tên người giới thiệu (đã mask) khi validate thành công
✅ Chỉ gọi set-code khi user confirm
✅ Disable input sau khi set thành công (không thể thay đổi)
✅ Lưu trạng thái đã nhập mã vào local storage
```

---

### 2. Validate Referral Code

Kiểm tra mã giới thiệu có hợp lệ không (real-time validation).

#### Endpoint

```http
GET /api/referral/validate/{code}
```

#### When to Use

- Khi user đang nhập mã giới thiệu (debounce 300-500ms)
- Trước khi submit form đăng ký
- Để hiển thị tên người giới thiệu

#### Request

**Headers:**
```http
Authorization: Bearer <token>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `code` | string | Mã giới thiệu cần validate |

#### Response

**Valid Code (200 OK):**
```json
{
  "success": true,
  "data": {
    "referralCode": "ABC123XY",
    "valid": true,
    "referrerName": "Ng********",
    "message": "Referral code is valid!"
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

**Invalid Code (200 OK):**
```json
{
  "success": true,
  "data": {
    "referralCode": "INVALID123",
    "valid": false,
    "referrerName": null,
    "message": "Referral code not found"
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `referralCode` | string | Mã đã nhập (normalized to uppercase) |
| `valid` | boolean | `true` = hợp lệ, `false` = không hợp lệ |
| `referrerName` | string \| null | Tên người giới thiệu (masked), null nếu invalid |
| `message` | string | Message để hiển thị cho user |

#### Invalid Reasons

| Message | Meaning |
|---------|---------|
| `Referral code is required` | Mã trống |
| `Referral code not found` | Mã không tồn tại |
| `Referral code belongs to an inactive user` | User sở hữu mã đã deactivate |

#### Mobile Implementation Notes

```
✅ Debounce input (300-500ms) trước khi gọi API
✅ Hiển thị loading indicator khi đang validate
✅ Show checkmark + tên khi valid = true
✅ Show error message khi valid = false
✅ Clear validation khi user thay đổi input
```

---

### 3. Get Referral Statistics

Lấy thống kê chi tiết về referral của user.

#### Endpoint

```http
GET /api/referral/stats
```

#### When to Use

- Màn hình "Referral Dashboard"
- Màn hình "Thống kê giới thiệu"
- Widget hiển thị tiến độ milestone
- Pull-to-refresh để cập nhật stats

#### Request

**Headers:**
```http
Authorization: Bearer <token>
```

#### Response

**Success (200 OK):**
```json
{
  "success": true,
  "data": {
    "myReferralCode": "HIEU123X",
    "referredByCode": "ABC123XY",
    "referredByName": "Ng********",
    "currentTier": "NORMAL",
    "totalCompletedOrders": 7,
    "nextMilestone": 10,
    "ordersToNextMilestone": 3,
    "nextMilestoneReward": "20,000 VND bonus",
    "totalReferrals": 5,
    "activeReferrals": 3,
    "totalCommissionEarned": 150000.00,
    "pendingCommission": 25000.00,
    "confirmedCommission": 75000.00,
    "paidCommission": 50000.00,
    "milestones": [
      {
        "milestone": 5,
        "rewardDescription": "10,000 VND bonus + Referral activation (Referrer gets 20,000 VND)",
        "achieved": true,
        "achievedAt": "2025-11-15T14:30:00"
      },
      {
        "milestone": 10,
        "rewardDescription": "20,000 VND bonus",
        "achieved": false,
        "achievedAt": null
      },
      {
        "milestone": 80,
        "rewardDescription": "VIP tier upgrade (83% cashback)",
        "achieved": false,
        "achievedAt": null
      },
      {
        "milestone": 300,
        "rewardDescription": "SUPER tier upgrade (85% cashback)",
        "achieved": false,
        "achievedAt": null
      }
    ],
    "recentRewards": [
      {
        "type": "MILESTONE_BONUS",
        "description": "Milestone 5 bonus: 10,000 VND",
        "amount": 10000.00,
        "newTier": null,
        "grantedAt": "2025-11-15T14:30:00"
      }
    ]
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

#### Response Fields Detail

**User Info (As Referee):**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `myReferralCode` | string | No | Mã giới thiệu của user (để share) |
| `referredByCode` | string | Yes | Mã đã nhập (null nếu chưa nhập) |
| `referredByName` | string | Yes | Tên người giới thiệu (masked) |
| `currentTier` | string | No | Tier hiện tại: `NORMAL`, `VIP`, `SUPER` |
| `totalCompletedOrders` | integer | No | Tổng số đơn đã hoàn thành |
| `nextMilestone` | integer | No | Số đơn cần đạt tiếp theo (0 nếu đã max) |
| `ordersToNextMilestone` | integer | No | Số đơn còn thiếu |
| `nextMilestoneReward` | string | No | Mô tả phần thưởng tiếp theo |

**Earnings Info (As Referrer):**

| Field | Type | Description |
|-------|------|-------------|
| `totalReferrals` | integer | Tổng số người đã giới thiệu |
| `activeReferrals` | integer | Số referral đang active (trong 5 tháng) |
| `totalCommissionEarned` | decimal | Tổng hoa hồng đã kiếm (all time) |
| `pendingCommission` | decimal | Hoa hồng đang chờ xử lý |
| `confirmedCommission` | decimal | Hoa hồng đã xác nhận (sẵn sàng rút) |
| `paidCommission` | decimal | Hoa hồng đã rút |

**Milestone Progress:**

| Field | Type | Description |
|-------|------|-------------|
| `milestone` | integer | Số đơn cần đạt |
| `rewardDescription` | string | Mô tả phần thưởng |
| `achieved` | boolean | Đã đạt chưa |
| `achievedAt` | datetime | Thời điểm đạt (null nếu chưa) |

**Recent Rewards:**

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | `MILESTONE_BONUS` hoặc `TIER_UPGRADE` |
| `description` | string | Mô tả chi tiết |
| `amount` | decimal | Số tiền bonus (null nếu tier upgrade) |
| `newTier` | string | Tier mới (null nếu bonus) |
| `grantedAt` | datetime | Thời điểm nhận thưởng |

#### Mobile Implementation Notes

```
✅ Cache response và refresh khi pull-to-refresh
✅ Hiển thị progress bar cho milestone
✅ Format số tiền theo locale (VD: 150.000 VND)
✅ Hiển thị share button cho myReferralCode
✅ Tính % tiến độ: (totalCompletedOrders / nextMilestone) * 100
```

---

### 4. Get My Referral Code

Lấy mã giới thiệu của user (simplified API).

#### Endpoint

```http
GET /api/referral/my-code
```

#### When to Use

- Widget "Share referral code"
- Copy to clipboard feature
- Generate QR code

#### Request

**Headers:**
```http
Authorization: Bearer <token>
```

#### Response

**Success (200 OK):**
```json
{
  "success": true,
  "data": "HIEU123X",
  "timestamp": "2025-12-18T10:30:00"
}
```

---

### 5. Get My Referrals ⭐ NEW

Lấy danh sách những người đã nhập mã giới thiệu của user hiện tại.

#### Endpoint

```http
GET /api/referral/my-referrals
```

#### When to Use

- Màn hình "Danh sách bạn bè đã giới thiệu"
- Tab "Bạn bè" trong Referral Dashboard
- Kiểm tra trạng thái từng người được giới thiệu
- Theo dõi tiến độ kích hoạt referral

#### Request

**Headers:**
```http
Authorization: Bearer <token>
```

#### Response

**Success (200 OK):**
```json
{
  "success": true,
  "data": {
    "myReferralCode": "HIEU123X",
    "totalReferrals": 5,
    "activeReferrals": 3,
    "referrals": [
      {
        "name": "Ng******",
        "email": "ng***@gmail.com",
        "status": "ACTIVE",
        "joinedAt": "2025-11-09T10:30:00",
        "completedOrders": 12,
        "referralActivated": true,
        "referralActivatedAt": "2025-11-20T14:25:00",
        "referralExpiresAt": "2026-02-20T14:25:00",
        "withinCommissionPeriod": true
      },
      {
        "name": "Tr******",
        "email": "tr***@yahoo.com",
        "status": "ACTIVE",
        "joinedAt": "2025-12-01T09:15:00",
        "completedOrders": 2,
        "referralActivated": false,
        "referralActivatedAt": null,
        "referralExpiresAt": null,
        "withinCommissionPeriod": false
      },
      {
        "name": "Le******",
        "email": "le***@hotmail.com",
        "status": "ACTIVE",
        "joinedAt": "2025-08-15T16:45:00",
        "completedOrders": 25,
        "referralActivated": true,
        "referralActivatedAt": "2025-08-25T10:00:00",
        "referralExpiresAt": "2025-11-25T10:00:00",
        "withinCommissionPeriod": false
      }
    ]
  },
  "timestamp": "2025-12-24T10:30:00"
}
```

#### Response Fields Detail

**Summary Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `myReferralCode` | string | Mã giới thiệu của user (để share) |
| `totalReferrals` | integer | Tổng số người đã nhập mã của bạn |
| `activeReferrals` | integer | Số người đang trong thời hạn nhận commission (5 tháng) |
| `referrals` | array | Danh sách chi tiết từng người |

**Referred User Info (trong array `referrals`):**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `name` | string | No | Tên người được giới thiệu (đã mask) |
| `email` | string | No | Email người được giới thiệu (đã mask) |
| `status` | string | No | Trạng thái tài khoản: `ACTIVE`, `SUSPENDED`, `BANNED` |
| `joinedAt` | datetime | Yes | Thời điểm đăng ký |
| `completedOrders` | integer | No | Số đơn hàng đã hoàn thành |
| `referralActivated` | boolean | No | Đã kích hoạt referral chưa (đạt 5 đơn) |
| `referralActivatedAt` | datetime | Yes | Thời điểm kích hoạt (null nếu chưa) |
| `referralExpiresAt` | datetime | Yes | Thời điểm hết hạn nhận commission (null nếu chưa kích hoạt) |
| `withinCommissionPeriod` | boolean | No | Còn trong thời hạn nhận commission không |

#### Referral Status Explained

| Status | `referralActivated` | `withinCommissionPeriod` | Icon | Meaning |
|--------|---------------------|--------------------------|------|---------|
| **Pending** | `false` | `false` | 🔄 | Chưa đủ 5 đơn, chưa kích hoạt |
| **Active** | `true` | `true` | ✅ | Đã kích hoạt, đang nhận commission |
| **Expired** | `true` | `false` | ⏰ | Đã kích hoạt nhưng hết hạn 5 tháng |

#### Privacy Masking Rules

Thông tin cá nhân được ẩn bớt để bảo vệ quyền riêng tư:

**Name Masking:**
- Hiển thị 2 ký tự đầu, ẩn phần còn lại
- VD: "Nguyen Van A" → "Ng******"
- VD: "Tran Thi B" → "Tr******"
- Nếu không có tên, sử dụng username

**Email Masking:**
- Hiển thị 2 ký tự đầu của local part, ẩn giữa, giữ domain
- VD: "nguyen@gmail.com" → "ng***@gmail.com"
- VD: "tranb@yahoo.com" → "tr***@yahoo.com"

#### UI/UX Guidelines

**Referrals List Screen:**

```
┌─────────────────────────────────────────┐
│     DANH SÁCH BẠN BÈ ĐÃ GIỚI THIỆU      │
├─────────────────────────────────────────┤
│                                         │
│  Mã của bạn: HIEU123X     [Copy] [Share]│
│                                         │
│  ═══════════════════════════════════    │
│                                         │
│  Tổng: 5 người │ Active: 3 người        │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ ✅ Ng******                     │    │
│  │    ng***@gmail.com              │    │
│  │    12 đơn • Kích hoạt: 20/11    │    │
│  │    Hết hạn: 20/02/2026          │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ 🔄 Tr******                     │    │
│  │    tr***@yahoo.com              │    │
│  │    3 đơn • Cần 2 đơn nữa        │    │
│  │    Chưa kích hoạt               │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ ⏰ Le******                     │    │
│  │    le***@hotmail.com            │    │
│  │    25 đơn • Hết hạn commission  │    │
│  │    Hết hạn: 25/11/2025          │    │
│  └─────────────────────────────────┘    │
│                                         │
└─────────────────────────────────────────┘
```

**Status Badge Colors:**

| Status | Color | Background |
|--------|-------|------------|
| Active (✅) | Green | `#E8F5E9` |
| Pending (🔄) | Orange | `#FFF3E0` |
| Expired (⏰) | Gray | `#F5F5F5` |

#### Mobile Implementation Notes

```
✅ Pull-to-refresh để cập nhật danh sách
✅ Empty state khi chưa có ai giới thiệu
✅ Hiển thị progress bar cho người pending (VD: 3/5 đơn)
✅ Sort theo trạng thái: Active > Pending > Expired
✅ Filter tabs: All / Active / Pending / Expired
✅ Cache response và refresh khi quay lại màn hình
```

#### Error Responses

| HTTP Code | Error Code | Message | Khi nào xảy ra |
|-----------|------------|---------|----------------|
| 401 | `UNAUTHORIZED` | Unauthorized | Token không hợp lệ/hết hạn |
| 404 | `USER_NOT_FOUND` | User not found | User không tồn tại |
| 500 | `INTERNAL_ERROR` | Internal server error | Lỗi server |

---

## Data Models

### Tier Levels

| Tier | Cashback Rate | How to Achieve |
|------|---------------|----------------|
| `NORMAL` | 80% | Default |
| `VIP` | 83% | Complete 80 orders |
| `SUPER` | 85% | Complete 300 orders |
| `DIAMOND` | 100% | Manually assigned by admin (special customers) |

### Commission Status

| Status | Description |
|--------|-------------|
| `PENDING` | Đơn hàng đang chờ xác nhận |
| `CONFIRMED` | Đơn hàng đã xác nhận, hoa hồng sẵn sàng |
| `PAID` | Hoa hồng đã được thanh toán |
| `CANCELLED` | Đơn hàng bị hủy, không có hoa hồng |

### Referral Code Format

- **Length:** 8 characters (default)
- **Characters:** Alphanumeric (A-Z, 0-9)
- **Case:** Stored as UPPERCASE
- **Example:** `HIEU123X`, `ABC456YZ`

---

## Error Handling

### Error Response Format

```json
{
  "success": false,
  "message": "Human-readable error message",
  "error": {
    "code": "ERROR_CODE",
    "details": "Detailed error information"
  },
  "timestamp": "2025-12-18T10:30:00"
}
```

### Error Codes Reference

| Code | HTTP Status | Description | User Message (Vietnamese) |
|------|-------------|-------------|---------------------------|
| `REFERRAL_CODE_ALREADY_SET` | 400 | User đã có mã giới thiệu | "Bạn đã nhập mã giới thiệu trước đó" |
| `SELF_REFERRAL` | 400 | Tự giới thiệu | "Không thể sử dụng mã giới thiệu của chính bạn" |
| `REFERRAL_CODE_NOT_FOUND` | 404 | Mã không tồn tại | "Mã giới thiệu không tồn tại" |
| `REFERRER_INACTIVE` | 400 | Người giới thiệu inactive | "Mã giới thiệu không còn hoạt động" |
| `VALIDATION_ERROR` | 400 | Input validation failed | "Dữ liệu không hợp lệ" |
| `UNAUTHORIZED` | 401 | Token invalid/expired | "Phiên đăng nhập hết hạn" |

### Mobile Error Handling

```javascript
const handleReferralError = (error) => {
  const errorMessages = {
    'REFERRAL_CODE_ALREADY_SET': 'Bạn đã nhập mã giới thiệu trước đó',
    'SELF_REFERRAL': 'Không thể sử dụng mã giới thiệu của chính bạn',
    'REFERRAL_CODE_NOT_FOUND': 'Mã giới thiệu không tồn tại',
    'REFERRER_INACTIVE': 'Mã giới thiệu không còn hoạt động',
    'VALIDATION_ERROR': 'Vui lòng kiểm tra lại mã giới thiệu'
  };

  return errorMessages[error.code] || 'Đã có lỗi xảy ra';
};
```

---

## Business Rules

### Setting Referral Code

1. **One-time only:** User chỉ được set referral code 1 lần duy nhất
2. **No self-referral:** Không thể dùng mã của chính mình
3. **Active referrer:** Người giới thiệu phải đang active
4. **Case-insensitive:** `abc123` = `ABC123` = `AbC123`
5. **Timing:** Có thể nhập bất kỳ lúc nào (đăng ký, thêm bank, sau đó)

### Commission Rules

1. **Activation:** Referral được kích hoạt khi referee hoàn thành **5 đơn hàng**
2. **Duration:** Referrer nhận hoa hồng trong **5 tháng** kể từ khi referral được kích hoạt
3. **Rate:** 5% commission từ đơn hàng của referee
4. **Order completion:** Chỉ tính khi đơn hàng hoàn thành (confirmed)

### Milestone Rules

1. **Sequential:** Phải đạt milestone 1 trước khi đạt milestone 2
2. **Permanent:** Một khi đạt được, không thể mất
3. **Auto-grant:** Hệ thống tự động cấp thưởng khi đạt milestone
4. **Order counting:** Chỉ đếm đơn hàng hoàn thành (completed)

---

## UI/UX Guidelines

### Referral Input Screen

```
┌─────────────────────────────────────────┐
│         NHẬP MÃ GIỚI THIỆU              │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  ABC123XY                    ✓  │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ✓ Mã hợp lệ - Người giới thiệu: Ng*** │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │        XÁC NHẬN                 │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Hoàn thành 5 đơn hàng để kích hoạt     │
│  quyền lợi giới thiệu!                  │
│                                         │
└─────────────────────────────────────────┘
```

### Referral Dashboard Screen

```
┌─────────────────────────────────────────┐
│         MÃ GIỚI THIỆU CỦA TÔI           │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  HIEU123X          [Copy] [Share]│   │
│  └─────────────────────────────────┘    │
│                                         │
│  ═══════════════════════════════════    │
│                                         │
│  TIẾN ĐỘ CỦA TÔI                        │
│  ┌─────────────────────────────────┐    │
│  │ ████████░░░░░░░░░ 7/10 đơn      │    │
│  │ Còn 3 đơn nữa để nhận 20,000đ   │    │
│  └─────────────────────────────────┘    │
│                                         │
│  MILESTONE                              │
│  ✓ 5 đơn  - 10,000đ + Kích hoạt        │
│  ○ 10 đơn - 20,000đ                     │
│  ○ 80 đơn - Nâng VIP (83%)              │
│  ○ 300 đơn - Nâng SUPER (85%)           │
│                                         │
│  ═══════════════════════════════════    │
│                                         │
│  HOA HỒNG TỪ BẠN BÈ                     │
│  Tổng: 150,000đ                         │
│  ├─ Đang chờ:    25,000đ               │
│  ├─ Sẵn sàng:    75,000đ               │
│  └─ Đã rút:      50,000đ               │
│                                         │
│  Bạn bè đã giới thiệu: 5 (3 active)    │
│                                         │
└─────────────────────────────────────────┘
```

### Share Referral Code

```javascript
const shareReferralCode = async (code) => {
  const message = `Đăng ký CashBee với mã giới thiệu ${code} để nhận ưu đãi! 🎁`;

  if (navigator.share) {
    await navigator.share({
      title: 'CashBee - Mã giới thiệu',
      text: message,
      url: `https://cashbee.vn/register?ref=${code}`
    });
  }
};
```

---

## Code Examples

### React Native / JavaScript

```javascript
// api/referralApi.js
import axios from 'axios';

const API_BASE = '/api/referral';

export const referralApi = {
  // Validate referral code (debounced)
  validateCode: async (code) => {
    const response = await axios.get(`${API_BASE}/validate/${code}`);
    return response.data.data;
  },

  // Set referral code
  setCode: async (referralCode) => {
    const response = await axios.post(`${API_BASE}/set-code`, { referralCode });
    return response.data.data;
  },

  // Get referral statistics
  getStats: async () => {
    const response = await axios.get(`${API_BASE}/stats`);
    return response.data.data;
  },

  // Get my referral code
  getMyCode: async () => {
    const response = await axios.get(`${API_BASE}/my-code`);
    return response.data.data;
  },

  // Get list of users I referred ⭐ NEW
  getMyReferrals: async () => {
    const response = await axios.get(`${API_BASE}/my-referrals`);
    return response.data.data;
  }
};
```

### React Native Hook

```javascript
// hooks/useReferral.js
import { useState, useCallback } from 'react';
import { debounce } from 'lodash';
import { referralApi } from '../api/referralApi';

export const useReferralValidation = () => {
  const [validationResult, setValidationResult] = useState(null);
  const [isValidating, setIsValidating] = useState(false);

  const validate = useCallback(
    debounce(async (code) => {
      if (!code || code.length < 6) {
        setValidationResult(null);
        return;
      }

      setIsValidating(true);
      try {
        const result = await referralApi.validateCode(code);
        setValidationResult(result);
      } catch (error) {
        setValidationResult({
          valid: false,
          message: 'Không thể kiểm tra mã giới thiệu'
        });
      } finally {
        setIsValidating(false);
      }
    }, 300),
    []
  );

  return { validationResult, isValidating, validate };
};

export const useReferralStats = () => {
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchStats = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await referralApi.getStats();
      setStats(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Đã có lỗi xảy ra');
    } finally {
      setIsLoading(false);
    }
  };

  return { stats, isLoading, error, fetchStats };
};

// ⭐ NEW: Hook for getting referrals list
export const useMyReferrals = () => {
  const [referralsData, setReferralsData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchReferrals = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await referralApi.getMyReferrals();
      setReferralsData(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Đã có lỗi xảy ra');
    } finally {
      setIsLoading(false);
    }
  };

  // Helper to get referrals by status
  const getReferralsByStatus = useCallback((status) => {
    if (!referralsData?.referrals) return [];

    switch (status) {
      case 'active':
        return referralsData.referrals.filter(r => r.referralActivated && r.withinCommissionPeriod);
      case 'pending':
        return referralsData.referrals.filter(r => !r.referralActivated);
      case 'expired':
        return referralsData.referrals.filter(r => r.referralActivated && !r.withinCommissionPeriod);
      default:
        return referralsData.referrals;
    }
  }, [referralsData]);

  return { referralsData, isLoading, error, fetchReferrals, getReferralsByStatus };
};
```

### Flutter / Dart

```dart
// lib/services/referral_service.dart
import 'package:dio/dio.dart';

class ReferralService {
  final Dio _dio;
  static const String _basePath = '/api/referral';

  ReferralService(this._dio);

  Future<ValidateReferralResponse> validateCode(String code) async {
    final response = await _dio.get('$_basePath/validate/$code');
    return ValidateReferralResponse.fromJson(response.data['data']);
  }

  Future<SetReferralResponse> setCode(String referralCode) async {
    final response = await _dio.post(
      '$_basePath/set-code',
      data: {'referralCode': referralCode},
    );
    return SetReferralResponse.fromJson(response.data['data']);
  }

  Future<ReferralStats> getStats() async {
    final response = await _dio.get('$_basePath/stats');
    return ReferralStats.fromJson(response.data['data']);
  }

  Future<String> getMyCode() async {
    final response = await _dio.get('$_basePath/my-code');
    return response.data['data'] as String;
  }

  // ⭐ NEW: Get list of users I referred
  Future<MyReferralsResponse> getMyReferrals() async {
    final response = await _dio.get('$_basePath/my-referrals');
    return MyReferralsResponse.fromJson(response.data['data']);
  }
}

// lib/models/referral_stats.dart
class ReferralStats {
  final String myReferralCode;
  final String? referredByCode;
  final String? referredByName;
  final String currentTier;
  final int totalCompletedOrders;
  final int nextMilestone;
  final int ordersToNextMilestone;
  final String nextMilestoneReward;
  final int totalReferrals;
  final int activeReferrals;
  final double totalCommissionEarned;
  final double pendingCommission;
  final double confirmedCommission;
  final double paidCommission;
  final List<MilestoneProgress> milestones;
  final List<RewardInfo> recentRewards;

  ReferralStats.fromJson(Map<String, dynamic> json)
      : myReferralCode = json['myReferralCode'],
        referredByCode = json['referredByCode'],
        referredByName = json['referredByName'],
        currentTier = json['currentTier'],
        totalCompletedOrders = json['totalCompletedOrders'] ?? 0,
        nextMilestone = json['nextMilestone'] ?? 0,
        ordersToNextMilestone = json['ordersToNextMilestone'] ?? 0,
        nextMilestoneReward = json['nextMilestoneReward'] ?? '',
        totalReferrals = json['totalReferrals'] ?? 0,
        activeReferrals = json['activeReferrals'] ?? 0,
        totalCommissionEarned = (json['totalCommissionEarned'] ?? 0).toDouble(),
        pendingCommission = (json['pendingCommission'] ?? 0).toDouble(),
        confirmedCommission = (json['confirmedCommission'] ?? 0).toDouble(),
        paidCommission = (json['paidCommission'] ?? 0).toDouble(),
        milestones = (json['milestones'] as List?)
            ?.map((e) => MilestoneProgress.fromJson(e))
            .toList() ?? [],
        recentRewards = (json['recentRewards'] as List?)
            ?.map((e) => RewardInfo.fromJson(e))
            .toList() ?? [];

  // Calculate progress percentage
  double get progressPercentage {
    if (nextMilestone == 0) return 100.0;
    return (totalCompletedOrders / nextMilestone) * 100;
  }
}

// ⭐ NEW: lib/models/my_referrals_response.dart
class MyReferralsResponse {
  final String myReferralCode;
  final int totalReferrals;
  final int activeReferrals;
  final List<ReferredUserInfo> referrals;

  MyReferralsResponse.fromJson(Map<String, dynamic> json)
      : myReferralCode = json['myReferralCode'],
        totalReferrals = json['totalReferrals'] ?? 0,
        activeReferrals = json['activeReferrals'] ?? 0,
        referrals = (json['referrals'] as List?)
            ?.map((e) => ReferredUserInfo.fromJson(e))
            .toList() ?? [];

  List<ReferredUserInfo> get activeReferralsList =>
      referrals.where((r) => r.referralActivated && r.withinCommissionPeriod).toList();

  List<ReferredUserInfo> get pendingReferralsList =>
      referrals.where((r) => !r.referralActivated).toList();

  List<ReferredUserInfo> get expiredReferralsList =>
      referrals.where((r) => r.referralActivated && !r.withinCommissionPeriod).toList();
}

class ReferredUserInfo {
  final String name;
  final String email;
  final String status;
  final DateTime? joinedAt;
  final int completedOrders;
  final bool referralActivated;
  final DateTime? referralActivatedAt;
  final DateTime? referralExpiresAt;
  final bool withinCommissionPeriod;

  ReferredUserInfo.fromJson(Map<String, dynamic> json)
      : name = json['name'] ?? '***',
        email = json['email'] ?? '***@***',
        status = json['status'] ?? 'UNKNOWN',
        joinedAt = json['joinedAt'] != null ? DateTime.parse(json['joinedAt']) : null,
        completedOrders = json['completedOrders'] ?? 0,
        referralActivated = json['referralActivated'] ?? false,
        referralActivatedAt = json['referralActivatedAt'] != null
            ? DateTime.parse(json['referralActivatedAt']) : null,
        referralExpiresAt = json['referralExpiresAt'] != null
            ? DateTime.parse(json['referralExpiresAt']) : null,
        withinCommissionPeriod = json['withinCommissionPeriod'] ?? false;

  // Helper to get status icon
  String get statusIcon {
    if (referralActivated && withinCommissionPeriod) return '✅';
    if (!referralActivated) return '🔄';
    return '⏰';
  }

  // Helper to get orders needed for activation
  int get ordersToActivate => referralActivated ? 0 : (5 - completedOrders).clamp(0, 5);
}
```

### Kotlin / Android

```kotlin
// ReferralRepository.kt
interface ReferralApi {
    @GET("/api/referral/validate/{code}")
    suspend fun validateCode(@Path("code") code: String): ApiResponse<ValidateReferralResponse>

    @POST("/api/referral/set-code")
    suspend fun setCode(@Body command: SetReferralCodeCommand): ApiResponse<SetReferralResponse>

    @GET("/api/referral/stats")
    suspend fun getStats(): ApiResponse<ReferralStats>

    @GET("/api/referral/my-code")
    suspend fun getMyCode(): ApiResponse<String>

    // ⭐ NEW
    @GET("/api/referral/my-referrals")
    suspend fun getMyReferrals(): ApiResponse<MyReferralsResponse>
}

// MyReferralsResponse.kt ⭐ NEW
data class MyReferralsResponse(
    val myReferralCode: String,
    val totalReferrals: Int,
    val activeReferrals: Int,
    val referrals: List<ReferredUserInfo>
)

data class ReferredUserInfo(
    val name: String,
    val email: String,
    val status: String,
    val joinedAt: String?,
    val completedOrders: Int,
    val referralActivated: Boolean,
    val referralActivatedAt: String?,
    val referralExpiresAt: String?,
    val withinCommissionPeriod: Boolean
) {
    val statusIcon: String
        get() = when {
            referralActivated && withinCommissionPeriod -> "✅"
            !referralActivated -> "🔄"
            else -> "⏰"
        }

    val ordersToActivate: Int
        get() = if (referralActivated) 0 else (5 - completedOrders).coerceIn(0, 5)
}

// ReferralViewModel.kt
class ReferralViewModel(
    private val referralApi: ReferralApi
) : ViewModel() {

    private val _stats = MutableStateFlow<ReferralStats?>(null)
    val stats: StateFlow<ReferralStats?> = _stats.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidateReferralResponse?>(null)
    val validationResult: StateFlow<ValidateReferralResponse?> = _validationResult.asStateFlow()

    fun validateCode(code: String) {
        viewModelScope.launch {
            if (code.length >= 6) {
                delay(300) // Debounce
                try {
                    val response = referralApi.validateCode(code)
                    _validationResult.value = response.data
                } catch (e: Exception) {
                    _validationResult.value = ValidateReferralResponse(
                        valid = false,
                        message = "Không thể kiểm tra mã"
                    )
                }
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val response = referralApi.getStats()
                _stats.value = response.data
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // ⭐ NEW: Load referrals list
    private val _referrals = MutableStateFlow<MyReferralsResponse?>(null)
    val referrals: StateFlow<MyReferralsResponse?> = _referrals.asStateFlow()

    fun loadReferrals() {
        viewModelScope.launch {
            try {
                val response = referralApi.getMyReferrals()
                _referrals.value = response.data
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Filter referrals by status
    fun getFilteredReferrals(filter: ReferralFilter): List<ReferredUserInfo> {
        val data = _referrals.value?.referrals ?: return emptyList()
        return when (filter) {
            ReferralFilter.ALL -> data
            ReferralFilter.ACTIVE -> data.filter { it.referralActivated && it.withinCommissionPeriod }
            ReferralFilter.PENDING -> data.filter { !it.referralActivated }
            ReferralFilter.EXPIRED -> data.filter { it.referralActivated && !it.withinCommissionPeriod }
        }
    }
}

enum class ReferralFilter { ALL, ACTIVE, PENDING, EXPIRED }
```

---

## Appendix

### API Testing with cURL

```bash
# 1. Validate referral code
curl -X GET "https://api.cashbee.vn/api/referral/validate/ABC123XY" \
  -H "Authorization: Bearer <token>"

# 2. Set referral code
curl -X POST "https://api.cashbee.vn/api/referral/set-code" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"referralCode": "ABC123XY"}'

# 3. Get referral stats
curl -X GET "https://api.cashbee.vn/api/referral/stats" \
  -H "Authorization: Bearer <token>"

# 4. Get my referral code
curl -X GET "https://api.cashbee.vn/api/referral/my-code" \
  -H "Authorization: Bearer <token>"

# 5. Get my referrals list ⭐ NEW
curl -X GET "https://api.cashbee.vn/api/referral/my-referrals" \
  -H "Authorization: Bearer <token>"
```

### Postman Collection

Import file `CashBee-Referral.postman_collection.json` để có đầy đủ các request mẫu.

---

## Contact

Nếu có thắc mắc về API, vui lòng liên hệ:

- **Backend Team:** backend@cashbee.vn
- **Slack Channel:** #dev-api-support

---

*Document generated: 2025-12-24*
