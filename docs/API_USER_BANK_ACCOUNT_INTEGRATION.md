# API Documentation: User Bank Account Integration

**Version:** 1.1.0
**Last Updated:** 2025-11-20
**Author:** CashBee Backend Team

**Changelog:**
- **v1.1.0** (2025-11-20): Added `shortName` field to Bank API for better UX
- **v1.0.0** (2025-11-20): Initial release - User bank account integration

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [API Endpoints](#api-endpoints)
3. [Data Models](#data-models)
4. [Frontend Implementation Guide](#frontend-implementation-guide)
5. [Mock Data for Development](#mock-data-for-development)
6. [Validation Rules](#validation-rules)
7. [UI Best Practices](#ui-best-practices)
8. [Error Handling](#error-handling)
9. [Testing Checklist](#testing-checklist)

---

## 🎯 Overview

Tính năng này cho phép user cập nhật thông tin tài khoản ngân hàng của mình, bao gồm:

- **Số tài khoản ngân hàng** (Account Number)
- **Tên chủ tài khoản** (Account Name)
- **Ngân hàng** (Bank) - chọn từ danh sách 60+ ngân hàng

### User Flow:

```
1. User vào màn hình "Thông tin cá nhân" / "Profile"
2. User nhấn "Cập nhật thông tin ngân hàng"
3. Hiển thị form với các trường:
   - Họ tên (optional)
   - Số điện thoại (optional)
   - Số tài khoản ngân hàng (required if updating bank)
   - Tên chủ tài khoản (optional - mặc định là họ tên)
   - Ngân hàng (required if updating bank) - Dropdown
4. User điền thông tin và submit
5. Backend validate và lưu vào database + Keycloak
6. Trả về thông tin user đã update (bao gồm bank info)
```

---

## 🔌 API Endpoints

### 1. GET /api/banks - Lấy danh sách ngân hàng

**Mục đích:** Lấy danh sách tất cả ngân hàng active để hiển thị trong dropdown.

**Method:** `GET`

**URL:** `/api/banks`

**Headers:**
```json
{
  "Authorization": "Bearer <JWT_TOKEN>"
}
```

**Request Body:** Không có

**Response Success (200 OK):**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "bankCode": "VPBANK",
      "bankName": "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)",
      "shortName": "VPBank",
      "isActive": true
    },
    {
      "id": 2,
      "bankCode": "ACB",
      "bankName": "Ngân hàng TMCP Á Châu (ACB)",
      "shortName": "ACB",
      "isActive": true
    },
    {
      "id": 3,
      "bankCode": "TCB",
      "bankName": "Ngân hàng TMCP Kỹ Thương Việt Nam (Techcombank)",
      "shortName": "Techcombank",
      "isActive": true
    }
    // ... 57 more banks
  ],
  "message": null,
  "timestamp": "2025-11-20T10:30:00"
}
```

**Response Error (401 Unauthorized):**

```json
{
  "success": false,
  "data": null,
  "message": "Unauthorized - Invalid or expired token",
  "timestamp": "2025-11-20T10:30:00"
}
```

**Cách sử dụng Frontend:**

```javascript
// Fetch banks khi component mount
useEffect(() => {
  const fetchBanks = async () => {
    try {
      const response = await fetch('/api/banks', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const result = await response.json();

      if (result.success) {
        setBanks(result.data);
      }
    } catch (error) {
      console.error('Failed to fetch banks:', error);
    }
  };

  fetchBanks();
}, []);
```

---

### 2. GET /api/users/me - Lấy thông tin user hiện tại

**Mục đích:** Lấy thông tin user đang đăng nhập, bao gồm thông tin ngân hàng (nếu đã setup).

**Method:** `GET`

**URL:** `/api/users/me`

**Headers:**
```json
{
  "Authorization": "Bearer <JWT_TOKEN>"
}
```

**Request Body:** Không có

**Response Success (200 OK):**

```json
{
  "success": true,
  "data": {
    "id": 123,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nguyenvana",
    "email": "nguyenvana@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0987654321",
    "referralCode": "CB4F7A9K",
    "referredBy": null,
    "status": "ACTIVE",
    "createdAt": "2025-01-15T08:30:00",
    "updatedAt": "2025-11-20T10:30:00",

    // ===== BANK ACCOUNT INFO (NEW) =====
    "accountNumber": "1234567890",
    "accountName": "NGUYEN VAN A",
    "bankCode": "VPBANK",
    "bankName": "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)"
  },
  "message": null,
  "timestamp": "2025-11-20T10:30:00"
}
```

**Response khi user chưa setup bank account:**

```json
{
  "success": true,
  "data": {
    "id": 123,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nguyenvana",
    "email": "nguyenvana@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0987654321",
    "referralCode": "CB4F7A9K",
    "referredBy": null,
    "status": "ACTIVE",
    "createdAt": "2025-01-15T08:30:00",
    "updatedAt": "2025-11-20T10:30:00",

    // Bank account fields are null
    "accountNumber": null,
    "accountName": null,
    "bankCode": null,
    "bankName": null
  },
  "message": null,
  "timestamp": "2025-11-20T10:30:00"
}
```

**Cách sử dụng Frontend:**

```javascript
// Load user info khi vào màn hình profile
const loadUserProfile = async () => {
  try {
    const response = await fetch('/api/users/me', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const result = await response.json();

    if (result.success) {
      setUser(result.data);

      // Pre-fill form nếu đã có bank account
      if (result.data.accountNumber) {
        setFormData({
          fullName: result.data.fullName,
          phone: result.data.phone,
          accountNumber: result.data.accountNumber,
          accountName: result.data.accountName,
          bankCode: result.data.bankCode
        });
      }
    }
  } catch (error) {
    console.error('Failed to load user:', error);
  }
};
```

---

### 3. PUT /api/users/me - Cập nhật thông tin user

**Mục đích:** Cập nhật thông tin user bao gồm basic info và bank account.

**Method:** `PUT`

**URL:** `/api/users/me`

**Headers:**
```json
{
  "Authorization": "Bearer <JWT_TOKEN>",
  "Content-Type": "application/json"
}
```

**Request Body:**

**Tất cả fields đều OPTIONAL** - chỉ gửi field nào muốn update.

```json
{
  "fullName": "Nguyễn Văn A",           // Optional
  "phone": "0987654321",                 // Optional
  "accountNumber": "1234567890",         // Required nếu muốn update bank
  "accountName": "NGUYEN VAN A",         // Optional - default là fullName
  "bankCode": "VPBANK"                   // Required nếu muốn update bank
}
```

**Validation Rules:**

- `fullName`: 2-100 ký tự
- `phone`: Định dạng Việt Nam (10 số, bắt đầu bằng 0)
- `accountNumber`: 6-20 ký tự
- `accountName`: 2-100 ký tự
- `bankCode`: Phải tồn tại trong danh sách banks
- **IMPORTANT:** Nếu gửi `accountNumber` thì PHẢI gửi `bankCode` và ngược lại

**Response Success (200 OK):**

```json
{
  "success": true,
  "data": {
    "id": 123,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nguyenvana",
    "email": "nguyenvana@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0987654321",
    "referralCode": "CB4F7A9K",
    "referredBy": null,
    "status": "ACTIVE",
    "createdAt": "2025-01-15T08:30:00",
    "updatedAt": "2025-11-20T10:35:00",

    // Updated bank account info
    "accountNumber": "1234567890",
    "accountName": "NGUYEN VAN A",
    "bankCode": "VPBANK",
    "bankName": "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)"
  },
  "message": "User updated successfully",
  "timestamp": "2025-11-20T10:35:00"
}
```

**Response Error (400 Bad Request) - Validation Failed:**

```json
{
  "success": false,
  "data": null,
  "message": "Validation failed: Bank code is required when account number is provided",
  "timestamp": "2025-11-20T10:35:00"
}
```

**Response Error (400 Bad Request) - Invalid Bank Code:**

```json
{
  "success": false,
  "data": null,
  "message": "Invalid bank code: INVALID_BANK",
  "timestamp": "2025-11-20T10:35:00"
}
```

**Response Error (400 Bad Request) - Invalid Phone Format:**

```json
{
  "success": false,
  "data": null,
  "message": "Phone must be 10 digits starting with 0",
  "timestamp": "2025-11-20T10:35:00"
}
```

**Cách sử dụng Frontend:**

```javascript
const updateUserProfile = async (formData) => {
  try {
    const response = await fetch('/api/users/me', {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fullName: formData.fullName,
        phone: formData.phone,
        accountNumber: formData.accountNumber,
        accountName: formData.accountName, // Optional
        bankCode: formData.bankCode
      })
    });

    const result = await response.json();

    if (result.success) {
      // Update thành công
      setUser(result.data);
      showSuccessMessage('Cập nhật thông tin thành công!');
    } else {
      // Validation error
      showErrorMessage(result.message);
    }
  } catch (error) {
    console.error('Failed to update user:', error);
    showErrorMessage('Có lỗi xảy ra, vui lòng thử lại!');
  }
};
```

---

## 📊 Data Models

### BankResponse

```typescript
interface BankResponse {
  id: number;              // Bank ID
  bankCode: string;        // Mã ngân hàng (e.g., "VPBANK", "ACB")
  bankName: string;        // Tên đầy đủ (e.g., "Ngân hàng TMCP...")
  shortName: string;       // Tên viết tắt (e.g., "VPBank", "ACB", "Techcombank")
  isActive: boolean;       // Ngân hàng có active không
}
```

### UserResponse

```typescript
interface UserResponse {
  // Basic Info
  id: number;
  keycloakId: string;
  username: string;
  email: string;
  fullName: string | null;
  phone: string | null;
  referralCode: string;
  referredBy: string | null;
  status: string;           // "ACTIVE" | "INACTIVE" | "BANNED" | "SUSPENDED"

  // Timestamps
  createdAt: string;        // ISO 8601 format
  updatedAt: string;        // ISO 8601 format

  // Bank Account Info (NEW)
  accountNumber: string | null;    // Số tài khoản
  accountName: string | null;      // Tên chủ tài khoản
  bankCode: string | null;         // Mã ngân hàng
  bankName: string | null;         // Tên ngân hàng đầy đủ
}
```

### UpdateUserCommand

```typescript
interface UpdateUserCommand {
  fullName?: string;         // Optional: 2-100 chars
  phone?: string;            // Optional: 10 digits, starts with 0
  accountNumber?: string;    // Optional: 6-20 chars (required if bankCode)
  accountName?: string;      // Optional: 2-100 chars
  bankCode?: string;         // Optional: max 50 chars (required if accountNumber)
}
```

### ApiResponse<T>

```typescript
interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  timestamp: string;         // ISO 8601 format
}
```

---

## 🎨 Frontend Implementation Guide

### Step 1: Create TypeScript Types

```typescript
// types/bank.types.ts

export interface Bank {
  id: number;
  bankCode: string;
  bankName: string;
  shortName: string;
  isActive: boolean;
}

export interface UserProfile {
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
  updatedAt: string;

  // Bank account
  accountNumber: string | null;
  accountName: string | null;
  bankCode: string | null;
  bankName: string | null;
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string;
  accountNumber?: string;
  accountName?: string;
  bankCode?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  timestamp: string;
}
```

---

### Step 2: Create API Service

```typescript
// services/user.service.ts

import { Bank, UserProfile, UpdateUserRequest, ApiResponse } from '@/types/bank.types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

class UserService {
  private getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('access_token'); // hoặc từ context/redux
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }

  /**
   * Lấy danh sách tất cả ngân hàng
   */
  async getBanks(): Promise<Bank[]> {
    const response = await fetch(`${API_BASE_URL}/api/banks`, {
      method: 'GET',
      headers: this.getAuthHeaders()
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch banks: ${response.statusText}`);
    }

    const result: ApiResponse<Bank[]> = await response.json();

    if (!result.success || !result.data) {
      throw new Error(result.message || 'Failed to fetch banks');
    }

    return result.data;
  }

  /**
   * Lấy thông tin user hiện tại
   */
  async getCurrentUser(): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/api/users/me`, {
      method: 'GET',
      headers: this.getAuthHeaders()
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch user: ${response.statusText}`);
    }

    const result: ApiResponse<UserProfile> = await response.json();

    if (!result.success || !result.data) {
      throw new Error(result.message || 'Failed to fetch user');
    }

    return result.data;
  }

  /**
   * Cập nhật thông tin user
   */
  async updateUser(data: UpdateUserRequest): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/api/users/me`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(data)
    });

    const result: ApiResponse<UserProfile> = await response.json();

    if (!result.success || !result.data) {
      throw new Error(result.message || 'Failed to update user');
    }

    return result.data;
  }
}

export const userService = new UserService();
```

---

### Step 3: Create React Component (Example)

```typescript
// components/UserBankAccountForm.tsx

import React, { useState, useEffect } from 'react';
import { userService } from '@/services/user.service';
import { Bank, UserProfile, UpdateUserRequest } from '@/types/bank.types';

export const UserBankAccountForm: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [user, setUser] = useState<UserProfile | null>(null);

  const [formData, setFormData] = useState<UpdateUserRequest>({
    fullName: '',
    phone: '',
    accountNumber: '',
    accountName: '',
    bankCode: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  // Load banks và user info khi component mount
  useEffect(() => {
    const init = async () => {
      try {
        setLoading(true);

        // Load banks và user info song song
        const [banksData, userData] = await Promise.all([
          userService.getBanks(),
          userService.getCurrentUser()
        ]);

        setBanks(banksData);
        setUser(userData);

        // Pre-fill form nếu user đã có bank account
        setFormData({
          fullName: userData.fullName || '',
          phone: userData.phone || '',
          accountNumber: userData.accountNumber || '',
          accountName: userData.accountName || '',
          bankCode: userData.bankCode || ''
        });

      } catch (error) {
        console.error('Failed to initialize:', error);
        alert('Không thể tải dữ liệu. Vui lòng thử lại!');
      } finally {
        setLoading(false);
      }
    };

    init();
  }, []);

  // Validate form
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Phone validation
    if (formData.phone && !/^0\d{9}$/.test(formData.phone)) {
      newErrors.phone = 'Số điện thoại phải có 10 số và bắt đầu bằng 0';
    }

    // Bank account validation
    const hasAccountNumber = formData.accountNumber && formData.accountNumber.trim() !== '';
    const hasBankCode = formData.bankCode && formData.bankCode.trim() !== '';

    if (hasAccountNumber && !hasBankCode) {
      newErrors.bankCode = 'Vui lòng chọn ngân hàng';
    }

    if (hasBankCode && !hasAccountNumber) {
      newErrors.accountNumber = 'Vui lòng nhập số tài khoản';
    }

    if (hasAccountNumber && (formData.accountNumber!.length < 6 || formData.accountNumber!.length > 20)) {
      newErrors.accountNumber = 'Số tài khoản phải từ 6-20 ký tự';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);

      // Chỉ gửi fields có giá trị
      const updateData: UpdateUserRequest = {};

      if (formData.fullName) updateData.fullName = formData.fullName;
      if (formData.phone) updateData.phone = formData.phone;
      if (formData.accountNumber) updateData.accountNumber = formData.accountNumber;
      if (formData.accountName) updateData.accountName = formData.accountName;
      if (formData.bankCode) updateData.bankCode = formData.bankCode;

      const updatedUser = await userService.updateUser(updateData);

      setUser(updatedUser);
      alert('Cập nhật thông tin thành công!');

    } catch (error: any) {
      console.error('Failed to update user:', error);
      alert(error.message || 'Có lỗi xảy ra. Vui lòng thử lại!');
    } finally {
      setLoading(false);
    }
  };

  if (loading && !user) {
    return <div>Đang tải...</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h2 className="text-2xl font-bold">Thông tin cá nhân</h2>

      {/* Full Name */}
      <div>
        <label className="block text-sm font-medium mb-1">Họ và tên</label>
        <input
          type="text"
          value={formData.fullName}
          onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
          className="w-full px-3 py-2 border rounded-md"
          placeholder="Nguyễn Văn A"
        />
      </div>

      {/* Phone */}
      <div>
        <label className="block text-sm font-medium mb-1">Số điện thoại</label>
        <input
          type="tel"
          value={formData.phone}
          onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          className="w-full px-3 py-2 border rounded-md"
          placeholder="0987654321"
        />
        {errors.phone && <p className="text-red-500 text-sm mt-1">{errors.phone}</p>}
      </div>

      <hr className="my-6" />

      <h3 className="text-xl font-bold">Thông tin ngân hàng</h3>

      {/* Account Number */}
      <div>
        <label className="block text-sm font-medium mb-1">Số tài khoản</label>
        <input
          type="text"
          value={formData.accountNumber}
          onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
          className="w-full px-3 py-2 border rounded-md"
          placeholder="1234567890"
        />
        {errors.accountNumber && <p className="text-red-500 text-sm mt-1">{errors.accountNumber}</p>}
      </div>

      {/* Account Name */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Tên chủ tài khoản (để trống nếu trùng với họ tên)
        </label>
        <input
          type="text"
          value={formData.accountName}
          onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
          className="w-full px-3 py-2 border rounded-md"
          placeholder="NGUYEN VAN A"
        />
      </div>

      {/* Bank Dropdown */}
      <div>
        <label className="block text-sm font-medium mb-1">Ngân hàng</label>
        <select
          value={formData.bankCode}
          onChange={(e) => setFormData({ ...formData, bankCode: e.target.value })}
          className="w-full px-3 py-2 border rounded-md"
        >
          <option value="">-- Chọn ngân hàng --</option>
          {banks.map((bank) => (
            <option key={bank.id} value={bank.bankCode}>
              {bank.shortName}
            </option>
          ))}
        </select>
        {errors.bankCode && <p className="text-red-500 text-sm mt-1">{errors.bankCode}</p>}
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
      >
        {loading ? 'Đang cập nhật...' : 'Cập nhật thông tin'}
      </button>

      {/* Display current bank info */}
      {user?.accountNumber && (
        <div className="mt-6 p-4 bg-gray-100 rounded-md">
          <h4 className="font-bold mb-2">Thông tin ngân hàng hiện tại:</h4>
          <p><strong>Số TK:</strong> {user.accountNumber}</p>
          <p><strong>Chủ TK:</strong> {user.accountName}</p>
          <p><strong>Ngân hàng:</strong> {user.bankName}</p>
        </div>
      )}
    </form>
  );
};
```

---

## 🧪 Mock Data for Development

### Mock Banks Data

```typescript
// mocks/banks.mock.ts

export const MOCK_BANKS = [
  {
    "id": 1,
    "bankCode": "VPBANK",
    "bankName": "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)",
    "shortName": "VPBank",
    "isActive": true
  },
  {
    "id": 2,
    "bankCode": "ACB",
    "bankName": "Ngân hàng TMCP Á Châu (ACB)",
    "shortName": "ACB",
    "isActive": true
  },
  {
    "id": 3,
    "bankCode": "TCB",
    "bankName": "Ngân hàng TMCP Kỹ Thương Việt Nam (Techcombank)",
    "shortName": "Techcombank",
    "isActive": true
  },
  {
    "id": 4,
    "bankCode": "VIETCOMBANK",
    "bankName": "Ngân hàng TMCP Ngoại Thương Việt Nam (Vietcombank)",
    "shortName": "Vietcombank",
    "isActive": true
  },
  {
    "id": 5,
    "bankCode": "BIDV",
    "bankName": "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (BIDV)",
    "shortName": "BIDV",
    "isActive": true
  },
  {
    "id": 6,
    "bankCode": "VIETINBANK",
    "bankName": "Ngân hàng TMCP Công Thương Việt Nam (VietinBank)",
    "shortName": "VietinBank",
    "isActive": true
  },
  {
    "id": 7,
    "bankCode": "AGRIBANK",
    "bankName": "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam (Agribank)",
    "shortName": "Agribank",
    "isActive": true
  },
  {
    "id": 8,
    "bankCode": "MB",
    "bankName": "Ngân hàng TMCP Quân Đội (MB Bank)",
    "shortName": "MB Bank",
    "isActive": true
  },
  {
    "id": 9,
    "bankCode": "SACOMBANK",
    "bankName": "Ngân hàng TMCP Sài Gòn Thương Tín (Sacombank)",
    "shortName": "Sacombank",
    "isActive": true
  },
  {
    "id": 10,
    "bankCode": "HDBANK",
    "bankName": "Ngân hàng TMCP Phát triển TPHCM (HDBank)",
    "shortName": "HDBank",
    "isActive": true
  }
  // ... thêm 50+ banks nữa
];
```

### Mock User Data

```typescript
// mocks/user.mock.ts

export const MOCK_USER_WITHOUT_BANK = {
  "id": 123,
  "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "nguyenvana",
  "email": "nguyenvana@example.com",
  "fullName": "Nguyễn Văn A",
  "phone": "0987654321",
  "referralCode": "CB4F7A9K",
  "referredBy": null,
  "status": "ACTIVE",
  "createdAt": "2025-01-15T08:30:00",
  "updatedAt": "2025-11-20T10:30:00",
  "accountNumber": null,
  "accountName": null,
  "bankCode": null,
  "bankName": null
};

export const MOCK_USER_WITH_BANK = {
  "id": 123,
  "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "nguyenvana",
  "email": "nguyenvana@example.com",
  "fullName": "Nguyễn Văn A",
  "phone": "0987654321",
  "referralCode": "CB4F7A9K",
  "referredBy": null,
  "status": "ACTIVE",
  "createdAt": "2025-01-15T08:30:00",
  "updatedAt": "2025-11-20T10:30:00",
  "accountNumber": "1234567890",
  "accountName": "NGUYEN VAN A",
  "bankCode": "VPBANK",
  "bankName": "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)"
};
```

### Mock Service (Development Mode)

```typescript
// services/user.service.mock.ts

import { MOCK_BANKS, MOCK_USER_WITHOUT_BANK } from '@/mocks';
import { Bank, UserProfile, UpdateUserRequest } from '@/types/bank.types';

class MockUserService {
  private currentUser: UserProfile = { ...MOCK_USER_WITHOUT_BANK };

  async getBanks(): Promise<Bank[]> {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    return MOCK_BANKS;
  }

  async getCurrentUser(): Promise<UserProfile> {
    await new Promise(resolve => setTimeout(resolve, 300));
    return { ...this.currentUser };
  }

  async updateUser(data: UpdateUserRequest): Promise<UserProfile> {
    await new Promise(resolve => setTimeout(resolve, 800));

    // Update mock user
    if (data.fullName) this.currentUser.fullName = data.fullName;
    if (data.phone) this.currentUser.phone = data.phone;

    if (data.accountNumber && data.bankCode) {
      this.currentUser.accountNumber = data.accountNumber;
      this.currentUser.accountName = data.accountName || data.fullName || this.currentUser.fullName;
      this.currentUser.bankCode = data.bankCode;

      // Find bank name from mock data
      const bank = MOCK_BANKS.find(b => b.bankCode === data.bankCode);
      this.currentUser.bankName = bank?.bankName || null;
    }

    this.currentUser.updatedAt = new Date().toISOString();

    return { ...this.currentUser };
  }
}

export const mockUserService = new MockUserService();
```

### Sử dụng Mock Service

```typescript
// services/user.service.ts

import { mockUserService } from './user.service.mock';

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === 'true';

export const userService = USE_MOCK ? mockUserService : realUserService;
```

Trong `.env.local`:
```
NEXT_PUBLIC_USE_MOCK=true
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## ✅ Validation Rules

### Client-side Validation (Frontend)

```typescript
const validationRules = {
  fullName: {
    minLength: 2,
    maxLength: 100,
    pattern: /^[\p{L}\s]+$/u,  // Chỉ chữ cái và khoảng trắng
    message: 'Họ tên phải từ 2-100 ký tự và chỉ chứa chữ cái'
  },

  phone: {
    pattern: /^0\d{9}$/,
    message: 'Số điện thoại phải có 10 số và bắt đầu bằng 0'
  },

  accountNumber: {
    minLength: 6,
    maxLength: 20,
    pattern: /^\d+$/,  // Chỉ số
    message: 'Số tài khoản phải từ 6-20 chữ số'
  },

  accountName: {
    minLength: 2,
    maxLength: 100,
    pattern: /^[A-Z\s]+$/,  // Chỉ chữ IN HOA và khoảng trắng
    message: 'Tên chủ TK phải từ 2-100 ký tự viết hoa không dấu'
  },

  bankCode: {
    required: true,
    message: 'Vui lòng chọn ngân hàng'
  }
};
```

### Validation Function Example

```typescript
function validateUserForm(data: UpdateUserRequest): Record<string, string> {
  const errors: Record<string, string> = {};

  // Validate fullName
  if (data.fullName) {
    if (data.fullName.length < 2 || data.fullName.length > 100) {
      errors.fullName = 'Họ tên phải từ 2-100 ký tự';
    }
  }

  // Validate phone
  if (data.phone) {
    if (!/^0\d{9}$/.test(data.phone)) {
      errors.phone = 'Số điện thoại phải có 10 số và bắt đầu bằng 0';
    }
  }

  // Validate accountNumber
  if (data.accountNumber) {
    if (data.accountNumber.length < 6 || data.accountNumber.length > 20) {
      errors.accountNumber = 'Số tài khoản phải từ 6-20 ký tự';
    }
    if (!/^\d+$/.test(data.accountNumber)) {
      errors.accountNumber = 'Số tài khoản chỉ chứa chữ số';
    }
  }

  // Validate accountNumber + bankCode dependency
  const hasAccountNumber = data.accountNumber && data.accountNumber.trim() !== '';
  const hasBankCode = data.bankCode && data.bankCode.trim() !== '';

  if (hasAccountNumber && !hasBankCode) {
    errors.bankCode = 'Vui lòng chọn ngân hàng';
  }
  if (hasBankCode && !hasAccountNumber) {
    errors.accountNumber = 'Vui lòng nhập số tài khoản';
  }

  return errors;
}
```

---

## 💡 UI Best Practices

### Using `shortName` for Better UX

API trả về cả `bankName` (tên đầy đủ) và `shortName` (tên viết tắt). **Khuyến nghị sử dụng `shortName` trong UI** để hiển thị ngắn gọn hơn:

```typescript
// ✅ GOOD: Sử dụng shortName trong dropdown
<select>
  <option value="">-- Chọn ngân hàng --</option>
  {banks.map((bank) => (
    <option key={bank.id} value={bank.bankCode}>
      {bank.shortName}  {/* "VPBank", "ACB" - ngắn gọn, dễ đọc */}
    </option>
  ))}
</select>

// ❌ BAD: Sử dụng bankName (quá dài)
<select>
  {banks.map((bank) => (
    <option key={bank.id} value={bank.bankCode}>
      {bank.bankName}  {/* "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)" - quá dài */}
    </option>
  ))}
</select>
```

### When to Use Each Field

| Field | Use Case | Example |
|-------|----------|---------|
| `shortName` | Dropdown options, Cards, Lists, Mobile UI | "VPBank", "ACB" |
| `bankName` | Tooltips, Detail pages, Print documents | "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)" |
| `bankCode` | API calls, Database queries | "VPBANK", "ACB" |

### Example: Bank Dropdown with shortName

```typescript
// Bank selection with shortName
<div className="space-y-2">
  <label className="block text-sm font-medium">Ngân hàng</label>
  <select
    value={formData.bankCode}
    onChange={(e) => handleBankChange(e.target.value)}
    className="w-full px-3 py-2 border rounded-md"
  >
    <option value="">-- Chọn ngân hàng --</option>
    {banks.map((bank) => (
      <option
        key={bank.id}
        value={bank.bankCode}
        title={bank.bankName}  // Show full name on hover
      >
        {bank.shortName}  // Display short name in dropdown
      </option>
    ))}
  </select>
</div>

// Display selected bank with full name
{user?.bankCode && (
  <div className="mt-4 p-3 bg-gray-50 rounded">
    <p className="text-sm text-gray-600">Ngân hàng đã chọn:</p>
    <p className="font-medium">{user.bankName}</p>  // Show full name here
  </div>
)}
```

---

## ⚠️ Error Handling

### Common Error Codes

| HTTP Status | Error Code | Message | Frontend Action |
|-------------|------------|---------|-----------------|
| 400 | VALIDATION_ERROR | "Validation failed: ..." | Hiển thị lỗi validation dưới field |
| 400 | INVALID_BANK_CODE | "Invalid bank code: ..." | Hiển thị lỗi: "Ngân hàng không hợp lệ" |
| 401 | UNAUTHORIZED | "Unauthorized - Invalid token" | Redirect to login page |
| 403 | FORBIDDEN | "Access denied" | Hiển thị thông báo: "Bạn không có quyền" |
| 404 | USER_NOT_FOUND | "User not found: ..." | Hiển thị: "Không tìm thấy thông tin user" |
| 500 | INTERNAL_ERROR | "Internal server error" | Hiển thị: "Lỗi hệ thống, vui lòng thử lại" |

### Error Handling Best Practices

```typescript
async function handleApiCall<T>(
  apiCall: () => Promise<T>,
  options?: {
    onSuccess?: (data: T) => void;
    onError?: (error: Error) => void;
    onFinally?: () => void;
  }
): Promise<T | null> {
  try {
    const result = await apiCall();
    options?.onSuccess?.(result);
    return result;
  } catch (error: any) {
    console.error('API Error:', error);

    // Handle specific errors
    if (error.message.includes('Unauthorized')) {
      // Redirect to login
      window.location.href = '/login';
    } else if (error.message.includes('Validation failed')) {
      // Show validation error
      options?.onError?.(error);
    } else {
      // Generic error
      alert('Có lỗi xảy ra. Vui lòng thử lại!');
      options?.onError?.(error);
    }

    return null;
  } finally {
    options?.onFinally?.();
  }
}

// Usage
const updateUser = async () => {
  await handleApiCall(
    () => userService.updateUser(formData),
    {
      onSuccess: (user) => {
        setUser(user);
        showToast('Cập nhật thành công!', 'success');
      },
      onError: (error) => {
        showToast(error.message, 'error');
      },
      onFinally: () => {
        setLoading(false);
      }
    }
  );
};
```

---

## 🧪 Testing Checklist

### Functional Testing

- [ ] Lấy danh sách banks thành công
- [ ] API trả về đầy đủ fields: id, bankCode, bankName, shortName, isActive
- [ ] Hiển thị dropdown banks với shortName (ngắn gọn)
- [ ] Hiển thị full bankName khi hover (tooltip)
- [ ] Load user info thành công
- [ ] Pre-fill form khi user đã có bank account
- [ ] Validate form trước khi submit
- [ ] Update chỉ fullName + phone (không update bank)
- [ ] Update cả fullName + phone + bank account
- [ ] Update chỉ bank account (không update fullName/phone)
- [ ] Hiển thị error message khi validation fail
- [ ] Hiển thị error message khi API fail
- [ ] Hiển thị success message khi update thành công
- [ ] Auto-fill accountName = fullName nếu không nhập

### Validation Testing

- [ ] Phone: Phải 10 số, bắt đầu bằng 0
- [ ] Phone: Reject nếu không đúng format
- [ ] AccountNumber: Phải 6-20 ký tự
- [ ] AccountNumber: Reject nếu < 6 hoặc > 20
- [ ] BankCode: Required khi có accountNumber
- [ ] AccountNumber: Required khi có bankCode
- [ ] Hiển thị error ngay dưới field có lỗi

### Edge Cases

- [ ] User chưa có bank account (all bank fields = null)
- [ ] User đã có bank account
- [ ] Update bank account từ null → có giá trị
- [ ] Update bank account từ có giá trị → giá trị mới
- [ ] Submit form trống (không update gì)
- [ ] Submit form với 1 field thay đổi
- [ ] Submit form với all fields thay đổi
- [ ] Network error (timeout)
- [ ] Unauthorized (token expired) → redirect login
- [ ] 500 error → hiển thị generic error

### UI/UX Testing

- [ ] Loading state khi fetch data
- [ ] Loading state khi submit form
- [ ] Disable submit button khi đang loading
- [ ] Clear error message khi user sửa field
- [ ] Hiển thị current bank info sau khi update
- [ ] Responsive trên mobile
- [ ] Accessibility (keyboard navigation, screen reader)

---

## 📝 Example Full Implementation (React + TypeScript)

```typescript
// pages/profile/bank-account.tsx

import React, { useState, useEffect } from 'react';
import { userService } from '@/services/user.service';
import type { Bank, UserProfile, UpdateUserRequest } from '@/types/bank.types';

export default function BankAccountPage() {
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [user, setUser] = useState<UserProfile | null>(null);

  const [formData, setFormData] = useState({
    fullName: '',
    phone: '',
    accountNumber: '',
    accountName: '',
    bankCode: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState('');

  // Initialize: Load banks và user
  useEffect(() => {
    const init = async () => {
      try {
        const [banksData, userData] = await Promise.all([
          userService.getBanks(),
          userService.getCurrentUser()
        ]);

        setBanks(banksData);
        setUser(userData);

        // Pre-fill form
        setFormData({
          fullName: userData.fullName || '',
          phone: userData.phone || '',
          accountNumber: userData.accountNumber || '',
          accountName: userData.accountName || '',
          bankCode: userData.bankCode || ''
        });

      } catch (error: any) {
        console.error('Init error:', error);
        alert('Không thể tải dữ liệu. Vui lòng thử lại!');
      } finally {
        setInitialLoading(false);
      }
    };

    init();
  }, []);

  // Validate form
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Phone
    if (formData.phone && !/^0\d{9}$/.test(formData.phone)) {
      newErrors.phone = 'Số điện thoại phải có 10 số và bắt đầu bằng 0';
    }

    // Account number
    const hasAccount = formData.accountNumber.trim() !== '';
    const hasBank = formData.bankCode.trim() !== '';

    if (hasAccount && !hasBank) {
      newErrors.bankCode = 'Vui lòng chọn ngân hàng';
    }
    if (hasBank && !hasAccount) {
      newErrors.accountNumber = 'Vui lòng nhập số tài khoản';
    }

    if (hasAccount && (formData.accountNumber.length < 6 || formData.accountNumber.length > 20)) {
      newErrors.accountNumber = 'Số tài khoản phải từ 6-20 ký tự';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMessage('');

    if (!validate()) return;

    try {
      setLoading(true);

      // Build update request (chỉ gửi fields có thay đổi)
      const updateData: UpdateUserRequest = {};

      if (formData.fullName !== user?.fullName) {
        updateData.fullName = formData.fullName;
      }
      if (formData.phone !== user?.phone) {
        updateData.phone = formData.phone;
      }
      if (formData.accountNumber !== user?.accountNumber) {
        updateData.accountNumber = formData.accountNumber;
      }
      if (formData.accountName && formData.accountName !== user?.accountName) {
        updateData.accountName = formData.accountName;
      }
      if (formData.bankCode !== user?.bankCode) {
        updateData.bankCode = formData.bankCode;
      }

      // Nếu không có gì thay đổi
      if (Object.keys(updateData).length === 0) {
        setSuccessMessage('Không có thay đổi nào!');
        return;
      }

      const updatedUser = await userService.updateUser(updateData);

      setUser(updatedUser);
      setSuccessMessage('Cập nhật thông tin thành công!');

      // Clear success message sau 3s
      setTimeout(() => setSuccessMessage(''), 3000);

    } catch (error: any) {
      console.error('Update error:', error);
      alert(error.message || 'Có lỗi xảy ra. Vui lòng thử lại!');
    } finally {
      setLoading(false);
    }
  };

  // Clear error khi user type
  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value });
    if (errors[field]) {
      setErrors({ ...errors, [field]: '' });
    }
  };

  if (initialLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Đang tải...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Thông tin tài khoản ngân hàng</h1>

      {successMessage && (
        <div className="mb-4 p-4 bg-green-100 text-green-700 rounded-md">
          {successMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic Info Section */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Thông tin cá nhân</h2>

          <div className="space-y-4">
            {/* Full Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Họ và tên
              </label>
              <input
                type="text"
                value={formData.fullName}
                onChange={(e) => handleFieldChange('fullName', e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Nguyễn Văn A"
              />
            </div>

            {/* Phone */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Số điện thoại
              </label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => handleFieldChange('phone', e.target.value)}
                className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                  errors.phone ? 'border-red-500' : 'border-gray-300'
                }`}
                placeholder="0987654321"
              />
              {errors.phone && (
                <p className="text-red-500 text-sm mt-1">{errors.phone}</p>
              )}
            </div>
          </div>
        </div>

        {/* Bank Account Section */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Thông tin ngân hàng</h2>

          <div className="space-y-4">
            {/* Account Number */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Số tài khoản <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.accountNumber}
                onChange={(e) => handleFieldChange('accountNumber', e.target.value)}
                className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                  errors.accountNumber ? 'border-red-500' : 'border-gray-300'
                }`}
                placeholder="1234567890"
              />
              {errors.accountNumber && (
                <p className="text-red-500 text-sm mt-1">{errors.accountNumber}</p>
              )}
            </div>

            {/* Account Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Tên chủ tài khoản
                <span className="text-gray-500 text-xs ml-2">
                  (Để trống nếu trùng với họ tên)
                </span>
              </label>
              <input
                type="text"
                value={formData.accountName}
                onChange={(e) => handleFieldChange('accountName', e.target.value.toUpperCase())}
                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="NGUYEN VAN A"
              />
              <p className="text-gray-500 text-xs mt-1">
                Vui lòng viết HOA, không dấu
              </p>
            </div>

            {/* Bank Dropdown */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Ngân hàng <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.bankCode}
                onChange={(e) => handleFieldChange('bankCode', e.target.value)}
                className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                  errors.bankCode ? 'border-red-500' : 'border-gray-300'
                }`}
              >
                <option value="">-- Chọn ngân hàng --</option>
                {banks.map((bank) => (
                  <option key={bank.id} value={bank.bankCode}>
                    {bank.bankName}
                  </option>
                ))}
              </select>
              {errors.bankCode && (
                <p className="text-red-500 text-sm mt-1">{errors.bankCode}</p>
              )}
            </div>
          </div>

          {/* Current Bank Info Display */}
          {user?.accountNumber && (
            <div className="mt-6 p-4 bg-gray-50 rounded-md border border-gray-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-2">
                Thông tin ngân hàng hiện tại:
              </h3>
              <div className="space-y-1 text-sm text-gray-600">
                <p><strong>Số TK:</strong> {user.accountNumber}</p>
                <p><strong>Chủ TK:</strong> {user.accountName}</p>
                <p><strong>Ngân hàng:</strong> {user.bankName}</p>
              </div>
            </div>
          )}
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={loading}
          className={`w-full py-3 px-4 rounded-md text-white font-medium transition-colors ${
            loading
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          {loading ? (
            <span className="flex items-center justify-center">
              <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Đang cập nhật...
            </span>
          ) : (
            'Cập nhật thông tin'
          )}
        </button>
      </form>
    </div>
  );
}
```

---

## 🎓 Summary

### Key Points cho Frontend Team:

1. **3 API endpoints chính:**
   - `GET /api/banks` - Lấy danh sách ngân hàng
   - `GET /api/users/me` - Lấy thông tin user (có bank info)
   - `PUT /api/users/me` - Update user info + bank account

2. **Validation rules quan trọng:**
   - Phone: 10 số, bắt đầu bằng 0
   - Account number: 6-20 ký tự
   - **accountNumber và bankCode phải đi cùng nhau**

3. **Mock data sẵn có:**
   - 60+ banks trong file mock
   - Mock service để develop không cần backend

4. **Error handling:**
   - 400: Validation errors
   - 401: Token expired → redirect login
   - 500: Generic server error

5. **Best practices:**
   - Always validate client-side trước khi submit
   - Show loading states
   - Clear errors khi user type
   - Pre-fill form với data hiện tại

---

## 📞 Support

Nếu có câu hỏi hoặc cần support, liên hệ:

- **Backend Team Lead:** [Your Name]
- **Email:** backend@cashbee.com
- **Slack:** #backend-support

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-20
**Status:** ✅ Ready for Frontend Development
