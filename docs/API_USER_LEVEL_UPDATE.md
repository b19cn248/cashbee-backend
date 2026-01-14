# API Update: User Level & Diamond Tier

> **Ngày cập nhật:** 2025-12-16
> **Version:** 1.0
> **Tác giả:** Backend Team

---

## Tổng quan

Cập nhật này bổ sung tính năng **User Level** cho hệ thống, bao gồm:
- Thêm level **DIAMOND** (100% cashback) cho khách hàng đặc biệt
- API mới để admin update user level
- Cập nhật các API hiện có để trả về/nhận field `userLevel`

---

## 1. User Level - Các cấp độ người dùng

| Level | Cashback Rate | Mô tả |
|-------|---------------|-------|
| `NORMAL` | 80% | Level mặc định cho user mới |
| `VIP` | 83% | Tự động nâng cấp khi đạt 40 đơn hàng |
| `SUPER` | 85% | Tự động nâng cấp khi đạt 150 đơn hàng |
| `DIAMOND` | 100% | **MỚI** - Chỉ admin có thể assign, dành cho khách hàng đặc biệt |

---

## 2. API Mới: Update User Level

### `PUT /api/users/{userId}/level`

Cập nhật level của user (chỉ admin).

#### Request

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Parameters:**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | Long | Yes | ID của user cần update |

**Body:**
```json
{
  "userLevel": "DIAMOND"
}
```

| Field | Type | Required | Values | Description |
|-------|------|----------|--------|-------------|
| `userLevel` | String | Yes | `NORMAL`, `VIP`, `SUPER`, `DIAMOND` | Level mới của user |

#### Response

**Success (200 OK):**
```json
{
  "success": true,
  "message": "User level updated successfully",
  "data": {
    "id": 1,
    "keycloakId": "abc-123-def",
    "username": "user1",
    "email": "user1@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0987654321",
    "referralCode": "CB4F7A9K",
    "referredBy": null,
    "status": "ACTIVE",
    "userLevel": "DIAMOND",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-12-16T14:25:00"
  }
}
```

**Error - User not found (404):**
```json
{
  "success": false,
  "message": "User not found: 999",
  "data": null
}
```

**Error - Invalid level (400):**
```json
{
  "success": false,
  "message": "User level is required",
  "data": null
}
```

#### Code Example (JavaScript/TypeScript)

```typescript
// Update user level to DIAMOND
const updateUserLevel = async (userId: number, level: string) => {
  const response = await fetch(`/api/users/${userId}/level`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ userLevel: level })
  });

  return response.json();
};

// Usage
await updateUserLevel(1, 'DIAMOND');
```

---

## 3. API Cập nhật: List Users

### `GET /api/admin/users`

**Thay đổi:** Response giờ bao gồm field `userLevel`.

#### Response mới

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "username": "user1",
        "email": "user1@example.com",
        "fullName": "Nguyen Van A",
        "phone": "0987654321",
        "referralCode": "CB4F7A9K",
        "referredBy": null,
        "status": "ACTIVE",
        "userLevel": "DIAMOND",    // <-- FIELD MỚI
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-12-16T14:25:00"
      },
      {
        "id": 2,
        "username": "user2",
        "email": "user2@example.com",
        "status": "ACTIVE",
        "userLevel": "NORMAL",     // <-- FIELD MỚI
        ...
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

#### TypeScript Interface Update

```typescript
// Cập nhật interface UserResponse
interface UserResponse {
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  fullName?: string;
  phone?: string;
  referralCode?: string;
  referredBy?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'BANNED';
  userLevel: 'NORMAL' | 'VIP' | 'SUPER' | 'DIAMOND';  // <-- FIELD MỚI
  createdAt: string;
  updatedAt: string;
  accountNumber?: string;
  accountName?: string;
  bankCode?: string;
  bankName?: string;
}
```

---

## 4. API Cập nhật: Update Cashback Policy

### `PUT /api/cashback-policies/{id}`

**Thay đổi:** Request giờ có thể nhận field `userLevel` để update.

#### Request mới

```json
{
  "cashbackRate": 85.00,
  "minOrderValue": 50000,
  "maxCashbackPerOrder": 100000,
  "isActive": true,
  "priority": 10,
  "effectiveFrom": "2025-01-01T00:00:00",
  "effectiveTo": "2025-12-31T23:59:59",
  "policyName": "Diamond Policy",
  "platformId": 1,
  "userLevel": "DIAMOND"    // <-- FIELD MỚI (optional)
}
```

| Field | Type | Required | Values | Description |
|-------|------|----------|--------|-------------|
| `userLevel` | String | No | `NORMAL`, `VIP`, `SUPER`, `DIAMOND` | User level áp dụng cho policy |

#### Code Example

```typescript
// Update policy với userLevel
const updateCashbackPolicy = async (policyId: number, data: object) => {
  const response = await fetch(`/api/cashback-policies/${policyId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  });

  return response.json();
};

// Chỉ update userLevel
await updateCashbackPolicy(1, { userLevel: 'DIAMOND' });

// Update nhiều fields
await updateCashbackPolicy(1, {
  userLevel: 'VIP',
  cashbackRate: 83.00,
  isActive: true
});
```

---

## 5. Gợi ý UI/UX cho Frontend

### 5.1. Hiển thị User Level

```tsx
// Badge component cho user level
const UserLevelBadge = ({ level }: { level: string }) => {
  const config = {
    NORMAL: { color: 'gray', label: 'Normal' },
    VIP: { color: 'blue', label: 'VIP' },
    SUPER: { color: 'purple', label: 'Super VIP' },
    DIAMOND: { color: 'gold', label: 'Diamond' }
  };

  const { color, label } = config[level] || config.NORMAL;

  return <Badge color={color}>{label}</Badge>;
};
```

### 5.2. Form Update User Level (Admin)

```tsx
// Dropdown để admin chọn user level
const UserLevelSelect = ({ value, onChange }) => (
  <Select value={value} onChange={onChange}>
    <Option value="NORMAL">Normal (80%)</Option>
    <Option value="VIP">VIP (83%)</Option>
    <Option value="SUPER">Super VIP (85%)</Option>
    <Option value="DIAMOND">Diamond (100%)</Option>
  </Select>
);
```

### 5.3. Hiển thị trong User Table

| Column | Field | Display |
|--------|-------|---------|
| Level | `userLevel` | Badge với màu tương ứng |

```tsx
// Trong columns config của table
{
  title: 'Level',
  dataIndex: 'userLevel',
  render: (level) => <UserLevelBadge level={level} />
}
```

---

## 6. Enum Values Reference

### UserLevel Enum

```typescript
enum UserLevel {
  NORMAL = 'NORMAL',   // 80% cashback
  VIP = 'VIP',         // 83% cashback
  SUPER = 'SUPER',     // 85% cashback
  DIAMOND = 'DIAMOND'  // 100% cashback (MỚI)
}
```

---

## 7. Checklist cho Frontend

- [ ] Cập nhật TypeScript interface `UserResponse` thêm field `userLevel`
- [ ] Tạo component `UserLevelBadge` để hiển thị level
- [ ] Cập nhật User List table thêm column Level
- [ ] Tạo form/modal để admin update user level
- [ ] Cập nhật Cashback Policy form thêm field userLevel
- [ ] Test API mới `PUT /api/users/{userId}/level`

---

## 8. Liên hệ

Nếu có câu hỏi hoặc cần hỗ trợ, liên hệ Backend Team.
