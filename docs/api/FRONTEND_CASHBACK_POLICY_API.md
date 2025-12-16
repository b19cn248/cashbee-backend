# 📘 Cashback Policy API Documentation for Frontend

**Module**: Cashback Policy Management
**Created**: 2025-11-01
**Status**: ✅ Implemented & Ready
**Base URL**: `http://localhost:8080/api`

---

## 🎯 Overview

This module provides APIs for managing cashback policies - the rules that determine how much cashback users receive from affiliate commissions.

**What is a Cashback Policy?**
- Defines the percentage of commission that goes back to users as cashback
- Different policies for different user levels (NORMAL, VIP, SUPER)
- Can be platform-specific (Shopee only) or universal (all platforms)
- Has effective date ranges and priority for conflict resolution

**Example**: "VIP users on Shopee get 80% of commission as cashback"

---

## 🔐 Authentication & Authorization

**Required Role**: `ADMIN` (for management endpoints)

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

> **Note**: Authentication with Keycloak will be implemented in later phase.
> For now, endpoints are accessible without authentication for testing.

---

## 📊 Data Model

### CashbackPolicyResponse

```typescript
interface CashbackPolicyResponse {
  id: number;                           // Policy ID
  policyName: string;                   // Display name (e.g., "VIP Shopee Policy")
  policyCode: string;                   // Unique code (e.g., "SHOPEE_VIP_2025")
  platformId: number | null;            // Platform ID (null = all platforms)
  platformName?: string;                // Platform name (optional, for display)
  userLevel: "NORMAL" | "VIP" | "SUPER"; // User level
  cashbackRate: number;                 // Percentage (0-100, e.g., 70.00 = 70%)
  minOrderValue: number | null;         // Minimum order value (null = no minimum)
  maxCashbackPerOrder: number | null;   // Maximum cashback cap (null = no cap)
  isActive: boolean;                    // Whether policy is active
  priority: number;                     // Priority (higher = preferred)
  effectiveFrom: string;                // ISO 8601 date (e.g., "2025-01-01T00:00:00")
  effectiveTo: string | null;           // ISO 8601 date (null = no end date)
}
```

### Standard API Response Wrapper

All endpoints return data wrapped in this structure:

```typescript
interface ApiResponse<T> {
  success: boolean;      // true if request succeeded
  message: string;       // Success/error message
  data: T;              // Actual response data
  timestamp: string;     // ISO 8601 timestamp
}
```

---

## 📡 API Endpoints

---

### 1. Get All Cashback Policies

Retrieve list of all cashback policies (both active and inactive).

**Endpoint**: `GET /api/admin/cashback-policies`

**Method**: `GET`

**Query Parameters**: None

**Request Example**:

```bash
curl -X GET http://localhost:8080/api/admin/cashback-policies \
  -H "Content-Type: application/json"
```

**Response**:

```json
{
  "success": true,
  "message": "Retrieved 4 policies",
  "data": [
    {
      "id": 1,
      "policyName": "Shopee Normal User Policy",
      "policyCode": "SHOPEE_NORMAL_2025",
      "platformId": 1,
      "platformName": null,
      "userLevel": "NORMAL",
      "cashbackRate": 70.00,
      "minOrderValue": 0.00,
      "maxCashbackPerOrder": null,
      "isActive": true,
      "priority": 0,
      "effectiveFrom": "2025-01-01T00:00:00",
      "effectiveTo": null
    },
    {
      "id": 2,
      "policyName": "Shopee VIP User Policy",
      "policyCode": "SHOPEE_VIP_2025",
      "platformId": 1,
      "platformName": null,
      "userLevel": "VIP",
      "cashbackRate": 80.00,
      "minOrderValue": 0.00,
      "maxCashbackPerOrder": null,
      "isActive": true,
      "priority": 10,
      "effectiveFrom": "2025-01-01T00:00:00",
      "effectiveTo": null
    },
    {
      "id": 3,
      "policyName": "Shopee Super VIP Policy",
      "policyCode": "SHOPEE_SUPER_2025",
      "platformId": 1,
      "platformName": null,
      "userLevel": "SUPER",
      "cashbackRate": 90.00,
      "minOrderValue": 0.00,
      "maxCashbackPerOrder": null,
      "isActive": true,
      "priority": 20,
      "effectiveFrom": "2025-01-01T00:00:00",
      "effectiveTo": null
    },
    {
      "id": 4,
      "policyName": "Universal Normal User Policy",
      "policyCode": "UNIVERSAL_NORMAL_2025",
      "platformId": null,
      "platformName": null,
      "userLevel": "NORMAL",
      "cashbackRate": 60.00,
      "minOrderValue": 0.00,
      "maxCashbackPerOrder": null,
      "isActive": true,
      "priority": -10,
      "effectiveFrom": "2025-01-01T00:00:00",
      "effectiveTo": null
    }
  ],
  "timestamp": "2025-11-01T15:30:00"
}
```

**Status Codes**:
- `200 OK` - Success
- `500 Internal Server Error` - Server error

**Frontend Usage**:

```typescript
// React/TypeScript example
const fetchPolicies = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/admin/cashback-policies');
    const result: ApiResponse<CashbackPolicyResponse[]> = await response.json();

    if (result.success) {
      setPolicies(result.data);
    } else {
      showError(result.message);
    }
  } catch (error) {
    showError('Failed to fetch policies');
  }
};
```

**Use Cases**:
- Admin views all cashback policies
- Display policies in admin dashboard
- Show policy list for editing

---

### 2. Get Active Cashback Policy

Get the active cashback policy for a specific platform and user level. This is the **KEY endpoint** used for cashback calculation!

**Endpoint**: `GET /api/admin/cashback-policies/active`

**Method**: `GET`

**Query Parameters**:
- `platformId` (number, required) - Platform ID (e.g., 1 for Shopee)
- `userLevel` (string, required) - User level: "NORMAL", "VIP", or "SUPER"

**Request Example**:

```bash
curl -X GET "http://localhost:8080/api/admin/cashback-policies/active?platformId=1&userLevel=NORMAL" \
  -H "Content-Type: application/json"
```

**Response (Success)**:

```json
{
  "success": true,
  "message": "Active policy retrieved successfully",
  "data": {
    "id": 1,
    "policyName": "Shopee Normal User Policy",
    "policyCode": "SHOPEE_NORMAL_2025",
    "platformId": 1,
    "platformName": null,
    "userLevel": "NORMAL",
    "cashbackRate": 70.00,
    "minOrderValue": 0.00,
    "maxCashbackPerOrder": null,
    "isActive": true,
    "priority": 0,
    "effectiveFrom": "2025-01-01T00:00:00",
    "effectiveTo": null
  },
  "timestamp": "2025-11-01T15:30:00"
}
```

**Response (Not Found)**:

```json
{
  "success": false,
  "message": "No active cashback policy found for platform 2 and user level VIP",
  "data": null,
  "timestamp": "2025-11-01T15:30:00"
}
```

**Status Codes**:
- `200 OK` - Success
- `404 Not Found` - No active policy found
- `400 Bad Request` - Invalid parameters
- `500 Internal Server Error` - Server error

**Frontend Usage**:

```typescript
// React/TypeScript example
const fetchActivePolicy = async (platformId: number, userLevel: string) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/admin/cashback-policies/active?platformId=${platformId}&userLevel=${userLevel}`
    );

    if (!response.ok) {
      if (response.status === 404) {
        showError('No active policy found for this criteria');
        return null;
      }
      throw new Error('Failed to fetch active policy');
    }

    const result: ApiResponse<CashbackPolicyResponse> = await response.json();
    return result.data;
  } catch (error) {
    showError('Failed to fetch active policy');
    return null;
  }
};
```

**Use Cases**:
- **Cashback calculation** - Get policy to calculate cashback amount
- Preview cashback rate before order confirmation
- Display user's current cashback rate

---

## 🎨 Frontend Implementation Examples

### 1. Policy List Component (React)

```tsx
import React, { useEffect, useState } from 'react';

interface CashbackPolicy {
  id: number;
  policyName: string;
  policyCode: string;
  platformId: number | null;
  userLevel: string;
  cashbackRate: number;
  isActive: boolean;
  priority: number;
  effectiveFrom: string;
  effectiveTo: string | null;
}

const PolicyListComponent: React.FC = () => {
  const [policies, setPolicies] = useState<CashbackPolicy[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPolicies();
  }, []);

  const fetchPolicies = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/admin/cashback-policies');
      const result = await response.json();

      if (result.success) {
        setPolicies(result.data);
      }
    } catch (error) {
      console.error('Failed to fetch policies:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (isActive: boolean) => {
    return isActive
      ? <span className="badge bg-success">Active</span>
      : <span className="badge bg-secondary">Inactive</span>;
  };

  const getPlatformName = (platformId: number | null) => {
    if (platformId === null) return 'All Platforms';
    if (platformId === 1) return 'Shopee';
    if (platformId === 2) return 'Lazada';
    if (platformId === 3) return 'TikTok';
    return `Platform ${platformId}`;
  };

  const getUserLevelBadge = (level: string) => {
    const colors = {
      NORMAL: 'info',
      VIP: 'warning',
      SUPER: 'danger'
    };
    return <span className={`badge bg-${colors[level] || 'secondary'}`}>{level}</span>;
  };

  if (loading) return <div>Loading policies...</div>;

  return (
    <div className="policy-list">
      <h2>Cashback Policies</h2>
      <table className="table table-striped">
        <thead>
          <tr>
            <th>ID</th>
            <th>Policy Name</th>
            <th>Code</th>
            <th>Platform</th>
            <th>User Level</th>
            <th>Cashback Rate</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Effective Period</th>
          </tr>
        </thead>
        <tbody>
          {policies.map((policy) => (
            <tr key={policy.id}>
              <td>{policy.id}</td>
              <td>{policy.policyName}</td>
              <td><code>{policy.policyCode}</code></td>
              <td>{getPlatformName(policy.platformId)}</td>
              <td>{getUserLevelBadge(policy.userLevel)}</td>
              <td><strong>{policy.cashbackRate}%</strong></td>
              <td>{policy.priority}</td>
              <td>{getStatusBadge(policy.isActive)}</td>
              <td>
                {new Date(policy.effectiveFrom).toLocaleDateString()}
                {' → '}
                {policy.effectiveTo
                  ? new Date(policy.effectiveTo).toLocaleDateString()
                  : 'Permanent'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PolicyListComponent;
```

---

### 2. Cashback Calculator Component (React)

```tsx
import React, { useState } from 'react';

const CashbackCalculator: React.FC = () => {
  const [platformId, setPlatformId] = useState<number>(1);
  const [userLevel, setUserLevel] = useState<string>('NORMAL');
  const [commissionAmount, setCommissionAmount] = useState<number>(0);
  const [policy, setPolicy] = useState<any>(null);
  const [cashbackAmount, setCashbackAmount] = useState<number>(0);
  const [loading, setLoading] = useState(false);

  const calculateCashback = async () => {
    setLoading(true);
    try {
      // Fetch active policy
      const response = await fetch(
        `http://localhost:8080/api/admin/cashback-policies/active?platformId=${platformId}&userLevel=${userLevel}`
      );
      const result = await response.json();

      if (result.success) {
        setPolicy(result.data);

        // Calculate cashback: commission * (rate / 100)
        const rate = result.data.cashbackRate;
        let cashback = commissionAmount * (rate / 100);

        // Apply max cap if exists
        if (result.data.maxCashbackPerOrder && cashback > result.data.maxCashbackPerOrder) {
          cashback = result.data.maxCashbackPerOrder;
        }

        setCashbackAmount(cashback);
      } else {
        alert('No active policy found');
        setPolicy(null);
        setCashbackAmount(0);
      }
    } catch (error) {
      console.error('Error:', error);
      alert('Failed to calculate cashback');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cashback-calculator">
      <h3>Cashback Calculator</h3>

      <div className="form-group">
        <label>Platform:</label>
        <select
          value={platformId}
          onChange={(e) => setPlatformId(Number(e.target.value))}
          className="form-control"
        >
          <option value="1">Shopee</option>
          <option value="2">Lazada</option>
          <option value="3">TikTok Shop</option>
        </select>
      </div>

      <div className="form-group">
        <label>User Level:</label>
        <select
          value={userLevel}
          onChange={(e) => setUserLevel(e.target.value)}
          className="form-control"
        >
          <option value="NORMAL">Normal</option>
          <option value="VIP">VIP</option>
          <option value="SUPER">Super VIP</option>
        </select>
      </div>

      <div className="form-group">
        <label>Commission Amount:</label>
        <input
          type="number"
          value={commissionAmount}
          onChange={(e) => setCommissionAmount(Number(e.target.value))}
          className="form-control"
          placeholder="Enter commission amount"
        />
      </div>

      <button
        onClick={calculateCashback}
        disabled={loading}
        className="btn btn-primary"
      >
        {loading ? 'Calculating...' : 'Calculate Cashback'}
      </button>

      {policy && (
        <div className="result mt-3 p-3 bg-light">
          <h4>Result:</h4>
          <p><strong>Policy:</strong> {policy.policyName}</p>
          <p><strong>Cashback Rate:</strong> {policy.cashbackRate}%</p>
          <p><strong>Commission:</strong> {commissionAmount.toLocaleString()} VND</p>
          <p className="text-success">
            <strong>Cashback Amount:</strong> {cashbackAmount.toLocaleString()} VND
          </p>
          {policy.maxCashbackPerOrder && cashbackAmount === policy.maxCashbackPerOrder && (
            <p className="text-warning">
              ⚠️ Capped at maximum: {policy.maxCashbackPerOrder.toLocaleString()} VND
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export default CashbackCalculator;
```

---

### 3. API Service (Axios)

```typescript
// services/cashbackPolicyService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export interface CashbackPolicyResponse {
  id: number;
  policyName: string;
  policyCode: string;
  platformId: number | null;
  platformName?: string;
  userLevel: 'NORMAL' | 'VIP' | 'SUPER';
  cashbackRate: number;
  minOrderValue: number | null;
  maxCashbackPerOrder: number | null;
  isActive: boolean;
  priority: number;
  effectiveFrom: string;
  effectiveTo: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

class CashbackPolicyService {
  /**
   * Get all cashback policies
   */
  async getAllPolicies(): Promise<CashbackPolicyResponse[]> {
    const response = await axios.get<ApiResponse<CashbackPolicyResponse[]>>(
      `${API_BASE_URL}/admin/cashback-policies`
    );
    return response.data.data;
  }

  /**
   * Get active policy for platform and user level
   */
  async getActivePolicy(
    platformId: number,
    userLevel: 'NORMAL' | 'VIP' | 'SUPER'
  ): Promise<CashbackPolicyResponse> {
    const response = await axios.get<ApiResponse<CashbackPolicyResponse>>(
      `${API_BASE_URL}/admin/cashback-policies/active`,
      {
        params: { platformId, userLevel }
      }
    );
    return response.data.data;
  }

  /**
   * Calculate cashback amount
   */
  calculateCashback(
    policy: CashbackPolicyResponse,
    commissionAmount: number
  ): number {
    // Calculate: commission * (rate / 100)
    let cashback = commissionAmount * (policy.cashbackRate / 100);

    // Apply max cap if exists
    if (policy.maxCashbackPerOrder && cashback > policy.maxCashbackPerOrder) {
      cashback = policy.maxCashbackPerOrder;
    }

    // Round to 2 decimal places
    return Math.round(cashback * 100) / 100;
  }

  /**
   * Get policies by user level
   */
  async getPoliciesByUserLevel(
    userLevel: 'NORMAL' | 'VIP' | 'SUPER'
  ): Promise<CashbackPolicyResponse[]> {
    const allPolicies = await this.getAllPolicies();
    return allPolicies.filter(p => p.userLevel === userLevel && p.isActive);
  }
}

export default new CashbackPolicyService();
```

**Usage**:

```typescript
import cashbackPolicyService from './services/cashbackPolicyService';

// In component
const loadActivePolicy = async () => {
  try {
    const policy = await cashbackPolicyService.getActivePolicy(1, 'NORMAL');
    const cashback = cashbackPolicyService.calculateCashback(policy, 100000);

    console.log(`Cashback for 100,000 VND commission: ${cashback} VND`);
  } catch (error) {
    console.error('Error:', error);
  }
};
```

---

## 🧪 Testing with Postman

### Collection Setup

1. **Create Collection**: "CashBee - Cashback Policies"
2. **Set Base URL Variable**: `{{base_url}}` = `http://localhost:8080`

### Test Cases

#### Test 1: Get All Policies

```
GET {{base_url}}/api/admin/cashback-policies
```

**Expected**: 200 OK with array of 4 policies

**Assertions**:
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success = true", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Response contains 4 policies", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.eql(4);
});

pm.test("Shopee policies have correct rates", function () {
    var jsonData = pm.response.json();
    var shopeePolicies = jsonData.data.filter(p => p.platformId === 1);

    var normalPolicy = shopeePolicies.find(p => p.userLevel === 'NORMAL');
    var vipPolicy = shopeePolicies.find(p => p.userLevel === 'VIP');
    var superPolicy = shopeePolicies.find(p => p.userLevel === 'SUPER');

    pm.expect(normalPolicy.cashbackRate).to.eql(70.00);
    pm.expect(vipPolicy.cashbackRate).to.eql(80.00);
    pm.expect(superPolicy.cashbackRate).to.eql(90.00);
});
```

#### Test 2: Get Active Policy for Shopee NORMAL User

```
GET {{base_url}}/api/admin/cashback-policies/active?platformId=1&userLevel=NORMAL
```

**Expected**: 200 OK with Shopee NORMAL policy (70% rate)

**Assertions**:
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Policy has correct rate", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.cashbackRate).to.eql(70.00);
    pm.expect(jsonData.data.userLevel).to.eql("NORMAL");
    pm.expect(jsonData.data.platformId).to.eql(1);
});
```

#### Test 3: Get Active Policy for Shopee VIP User

```
GET {{base_url}}/api/admin/cashback-policies/active?platformId=1&userLevel=VIP
```

**Expected**: 200 OK with Shopee VIP policy (80% rate)

**Assertions**:
```javascript
pm.test("VIP gets higher rate than NORMAL", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.cashbackRate).to.eql(80.00);
    pm.expect(jsonData.data.priority).to.be.above(0);
});
```

#### Test 4: Get Active Policy for Inactive Platform

```
GET {{base_url}}/api/admin/cashback-policies/active?platformId=2&userLevel=NORMAL
```

**Expected**: 200 OK with Universal policy (60% rate, fallback)

**Assertions**:
```javascript
pm.test("Falls back to universal policy", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.platformId).to.eql(null);
    pm.expect(jsonData.data.cashbackRate).to.eql(60.00);
    pm.expect(jsonData.data.policyCode).to.include("UNIVERSAL");
});
```

#### Test 5: Invalid User Level

```
GET {{base_url}}/api/admin/cashback-policies/active?platformId=1&userLevel=INVALID
```

**Expected**: 400 Bad Request

---

## 🔍 Error Handling

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-11-01T15:30:00"
}
```

### Common Errors

| Status Code | Error Message | Cause | Solution |
|-------------|--------------|-------|----------|
| 404 | "No active cashback policy found for platform X and user level Y" | No matching policy | Check if platform/user level combination has active policy |
| 400 | "Invalid user level" | Invalid enum value | Use: NORMAL, VIP, or SUPER |
| 500 | "Internal server error" | Database/server issue | Contact backend team |

### Frontend Error Handling

```typescript
const handlePolicyError = (error: any) => {
  if (error.response) {
    const status = error.response.status;
    const message = error.response.data?.message || 'Unknown error';

    switch (status) {
      case 404:
        showNotification(
          'No active policy found. Using default cashback rate.',
          'warning'
        );
        // Use fallback rate
        break;
      case 400:
        showNotification('Invalid parameters', 'error');
        break;
      case 500:
        showNotification('Server error. Please try again later.', 'error');
        break;
      default:
        showNotification(message, 'error');
    }
  } else if (error.request) {
    showNotification('Cannot connect to server', 'error');
  } else {
    showNotification('An error occurred', 'error');
  }
};
```

---

## 💡 Business Logic

### How Policy Matching Works

When calculating cashback, the system finds the best matching policy using this logic:

```typescript
// Pseudo-code
function findActivePolicy(platformId, userLevel, now) {
  // Filter policies:
  // 1. isActive = true
  // 2. effectiveFrom <= now
  // 3. (effectiveTo >= now OR effectiveTo IS NULL)
  // 4. userLevel matches
  // 5. (platformId matches OR platformId IS NULL)

  // Sort by:
  // 1. priority DESC (higher priority wins)
  // 2. createdAt DESC (newer wins if same priority)

  // Return first match
}
```

**Example Scenarios**:

1. **Shopee NORMAL user**:
   - Matches: "Shopee Normal" (priority 0) AND "Universal Normal" (priority -10)
   - Winner: "Shopee Normal" (higher priority) → 70% cashback

2. **Lazada NORMAL user** (Lazada inactive):
   - Matches: "Universal Normal" (priority -10) only
   - Winner: "Universal Normal" → 60% cashback (fallback)

3. **Shopee VIP user**:
   - Matches: "Shopee VIP" (priority 10)
   - Winner: "Shopee VIP" → 80% cashback

---

## 📝 Database Seed Data

The system comes pre-loaded with these policies:

| ID | Policy Name | Platform | User Level | Rate | Priority | Status |
|----|-------------|----------|------------|------|----------|--------|
| 1 | Shopee Normal User Policy | Shopee (1) | NORMAL | 70% | 0 | Active |
| 2 | Shopee VIP User Policy | Shopee (1) | VIP | 80% | 10 | Active |
| 3 | Shopee Super VIP Policy | Shopee (1) | SUPER | 90% | 20 | Active |
| 4 | Universal Normal User Policy | All (null) | NORMAL | 60% | -10 | Active |

**Priority Explanation**:
- Higher priority = preferred when multiple policies match
- Platform-specific policies have higher priority than universal
- VIP tiers have higher priority than NORMAL

---

## 🎯 Frontend Checklist

Before using these APIs, ensure:

- [ ] Backend is running on `http://localhost:8080`
- [ ] Database is initialized with seed data
- [ ] CORS is enabled (if frontend on different origin)
- [ ] Error handling is implemented
- [ ] Loading states are shown during API calls
- [ ] User level enum is properly handled

---

## 🚀 Quick Start for Frontend

### 1. Check Backend Status

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

### 2. Test Policy API

```bash
curl http://localhost:8080/api/admin/cashback-policies
```

Expected: JSON array with 4 policies

### 3. Test Active Policy Endpoint

```bash
curl "http://localhost:8080/api/admin/cashback-policies/active?platformId=1&userLevel=NORMAL"
```

Expected: Shopee NORMAL policy with 70% rate

### 4. Integrate in Frontend

Copy the API service code above and start using it!

---

## 📚 Related Documentation

- **Backend API Docs (Swagger)**: http://localhost:8080/swagger-ui.html
- **Affiliate Platform API**: See `FRONTEND_AFFILIATE_PLATFORM_API.md`
- **Implementation Plan**: See `IMPLEMENTATION_PLAN.md`

---

### 3. Get Cashback Policy by ID

Retrieve a specific cashback policy by its ID.

**Endpoint**: `GET /api/admin/cashback-policies/{id}`

**Method**: `GET`

**Path Parameters**:
- `id` (number, required) - Policy ID

**Request Example**:

```bash
curl -X GET http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json"
```

**Response**:

```json
{
  "success": true,
  "message": "Policy retrieved successfully",
  "data": {
    "id": 1,
    "policyName": "Shopee Normal User Policy",
    "policyCode": "SHOPEE_NORMAL_2025",
    "platformId": 1,
    "userLevel": "NORMAL",
    "cashbackRate": 70.00,
    "minOrderValue": 0.00,
    "maxCashbackPerOrder": null,
    "isActive": true,
    "priority": 0,
    "effectiveFrom": "2025-01-01T00:00:00",
    "effectiveTo": null
  },
  "timestamp": "2025-12-16T10:30:00"
}
```

**Status Codes**:
- `200 OK` - Success
- `404 Not Found` - Policy not found
- `401 Unauthorized` - Missing or invalid JWT token

---

### 4. Update Cashback Policy ⭐ NEW

Update an existing cashback policy. **Supports partial update** - only provided fields will be updated.

**Endpoint**: `PUT /api/admin/cashback-policies/{id}`

**Method**: `PUT`

**Path Parameters**:
- `id` (number, required) - Policy ID to update

**Request Body** (all fields optional - only send fields you want to update):

```typescript
interface UpdateCashbackPolicyRequest {
  policyName?: string;           // Policy display name (max 255 chars)
  platformId?: number;           // Platform ID (1=Shopee, 2=Lazada, 3=TikTok, null=All) ⭐ NEW
  cashbackRate?: number;         // Percentage (0-100)
  minOrderValue?: number;        // Minimum order value (>= 0)
  maxCashbackPerOrder?: number;  // Maximum cashback cap (>= 0)
  isActive?: boolean;            // Enable/disable policy
  priority?: number;             // Priority (>= 0, higher = preferred)
  effectiveFrom?: string;        // Start date (ISO 8601)
  effectiveTo?: string;          // End date (ISO 8601, null = permanent)
}
```

**Request Examples**:

#### Example 1: Update cashback rate only

```bash
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "cashbackRate": 75.00
  }'
```

#### Example 2: Change platform (⭐ NEW feature)

```bash
# Move policy from Shopee (id=1) to Lazada (id=2)
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "platformId": 2
  }'
```

#### Example 3: Make policy universal (apply to ALL platforms)

```bash
# Set platformId to null to apply to all platforms
# Note: Send 0 or use special handling - see notes below
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "platformId": null
  }'
```

#### Example 4: Update multiple fields at once

```bash
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "policyName": "Shopee Premium User Policy",
    "platformId": 1,
    "cashbackRate": 85.00,
    "isActive": true,
    "priority": 15
  }'
```

#### Example 5: Deactivate policy

```bash
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "isActive": false
  }'
```

#### Example 6: Set effective date range

```bash
curl -X PUT http://localhost:8080/api/admin/cashback-policies/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "effectiveFrom": "2025-01-01T00:00:00",
    "effectiveTo": "2025-12-31T23:59:59"
  }'
```

**Response (Success)**:

```json
{
  "success": true,
  "message": "Cashback policy updated successfully",
  "data": {
    "id": 1,
    "policyName": "Shopee Premium User Policy",
    "policyCode": "SHOPEE_NORMAL_2025",
    "platformId": 1,
    "userLevel": "NORMAL",
    "cashbackRate": 85.00,
    "minOrderValue": 0.00,
    "maxCashbackPerOrder": null,
    "isActive": true,
    "priority": 15,
    "effectiveFrom": "2025-01-01T00:00:00",
    "effectiveTo": null
  },
  "timestamp": "2025-12-16T10:30:00"
}
```

**Response (Error - Policy not found)**:

```json
{
  "success": false,
  "message": "Cashback policy not found with ID: 999",
  "data": null,
  "timestamp": "2025-12-16T10:30:00"
}
```

**Response (Error - Validation failed)**:

```json
{
  "success": false,
  "message": "Cashback rate cannot exceed 100%",
  "data": null,
  "timestamp": "2025-12-16T10:30:00"
}
```

**Response (Error - No fields provided)**:

```json
{
  "success": false,
  "message": "No fields provided for update",
  "data": null,
  "timestamp": "2025-12-16T10:30:00"
}
```

**Status Codes**:
- `200 OK` - Update successful
- `400 Bad Request` - Validation error or no fields provided
- `404 Not Found` - Policy not found
- `401 Unauthorized` - Missing or invalid JWT token

**Validation Rules**:

| Field | Rule |
|-------|------|
| `policyName` | Max 255 characters |
| `platformId` | Must be valid platform ID (1, 2, 3) or null |
| `cashbackRate` | 0 ≤ value ≤ 100 |
| `minOrderValue` | ≥ 0 |
| `maxCashbackPerOrder` | ≥ 0 |
| `priority` | ≥ 0 |
| `effectiveTo` | Must be after `effectiveFrom` if both provided |

---

## 🎨 Frontend Implementation for Update Policy

### React Component Example

```tsx
import React, { useState, useEffect } from 'react';

interface UpdatePolicyFormData {
  policyName?: string;
  platformId?: number | null;
  cashbackRate?: number;
  minOrderValue?: number;
  maxCashbackPerOrder?: number;
  isActive?: boolean;
  priority?: number;
  effectiveFrom?: string;
  effectiveTo?: string;
}

interface Platform {
  id: number;
  name: string;
  code: string;
}

const UpdatePolicyForm: React.FC<{ policyId: number; onSuccess: () => void }> = ({
  policyId,
  onSuccess
}) => {
  const [formData, setFormData] = useState<UpdatePolicyFormData>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [originalPolicy, setOriginalPolicy] = useState<any>(null);

  // Available platforms
  const platforms: Platform[] = [
    { id: 1, name: 'Shopee', code: 'shopee' },
    { id: 2, name: 'Lazada', code: 'lazada' },
    { id: 3, name: 'TikTok Shop', code: 'tiktok' },
  ];

  // Load current policy data
  useEffect(() => {
    loadPolicy();
  }, [policyId]);

  const loadPolicy = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/cashback-policies/${policyId}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      const result = await response.json();
      if (result.success) {
        setOriginalPolicy(result.data);
        // Pre-fill form with current values
        setFormData({
          policyName: result.data.policyName,
          platformId: result.data.platformId,
          cashbackRate: result.data.cashbackRate,
          minOrderValue: result.data.minOrderValue,
          maxCashbackPerOrder: result.data.maxCashbackPerOrder,
          isActive: result.data.isActive,
          priority: result.data.priority,
        });
      }
    } catch (err) {
      setError('Failed to load policy');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    // Only send changed fields
    const changedFields: UpdatePolicyFormData = {};

    if (formData.policyName !== originalPolicy?.policyName) {
      changedFields.policyName = formData.policyName;
    }
    if (formData.platformId !== originalPolicy?.platformId) {
      changedFields.platformId = formData.platformId;
    }
    if (formData.cashbackRate !== originalPolicy?.cashbackRate) {
      changedFields.cashbackRate = formData.cashbackRate;
    }
    if (formData.isActive !== originalPolicy?.isActive) {
      changedFields.isActive = formData.isActive;
    }
    if (formData.priority !== originalPolicy?.priority) {
      changedFields.priority = formData.priority;
    }

    // Check if any fields changed
    if (Object.keys(changedFields).length === 0) {
      setError('No changes detected');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/cashback-policies/${policyId}`,
        {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(changedFields),
        }
      );

      const result = await response.json();

      if (result.success) {
        alert('Policy updated successfully!');
        onSuccess();
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('Failed to update policy');
    } finally {
      setLoading(false);
    }
  };

  if (!originalPolicy) {
    return <div>Loading policy...</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="update-policy-form">
      <h3>Update Policy: {originalPolicy.policyCode}</h3>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Policy Name */}
      <div className="form-group">
        <label>Policy Name</label>
        <input
          type="text"
          className="form-control"
          value={formData.policyName || ''}
          onChange={(e) => setFormData({ ...formData, policyName: e.target.value })}
          maxLength={255}
        />
      </div>

      {/* Platform Selection - ⭐ NEW */}
      <div className="form-group">
        <label>Platform</label>
        <select
          className="form-control"
          value={formData.platformId ?? 'null'}
          onChange={(e) => {
            const value = e.target.value;
            setFormData({
              ...formData,
              platformId: value === 'null' ? null : Number(value),
            });
          }}
        >
          <option value="null">All Platforms (Universal)</option>
          {platforms.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <small className="form-text text-muted">
          Select "All Platforms" to apply this policy to all platforms (fallback policy)
        </small>
      </div>

      {/* Cashback Rate */}
      <div className="form-group">
        <label>Cashback Rate (%)</label>
        <input
          type="number"
          className="form-control"
          value={formData.cashbackRate || 0}
          onChange={(e) => setFormData({ ...formData, cashbackRate: Number(e.target.value) })}
          min={0}
          max={100}
          step={0.01}
        />
        <small className="form-text text-muted">
          Percentage of commission given to user as cashback (0-100)
        </small>
      </div>

      {/* Priority */}
      <div className="form-group">
        <label>Priority</label>
        <input
          type="number"
          className="form-control"
          value={formData.priority || 0}
          onChange={(e) => setFormData({ ...formData, priority: Number(e.target.value) })}
          min={0}
        />
        <small className="form-text text-muted">
          Higher priority wins when multiple policies match (recommended: 0-100)
        </small>
      </div>

      {/* Is Active */}
      <div className="form-group form-check">
        <input
          type="checkbox"
          className="form-check-input"
          id="isActive"
          checked={formData.isActive || false}
          onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
        />
        <label className="form-check-label" htmlFor="isActive">
          Active
        </label>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        className="btn btn-primary"
        disabled={loading}
      >
        {loading ? 'Updating...' : 'Update Policy'}
      </button>
    </form>
  );
};

export default UpdatePolicyForm;
```

### API Service with Update Method

```typescript
// services/cashbackPolicyService.ts

interface UpdateCashbackPolicyRequest {
  policyName?: string;
  platformId?: number | null;
  cashbackRate?: number;
  minOrderValue?: number;
  maxCashbackPerOrder?: number;
  isActive?: boolean;
  priority?: number;
  effectiveFrom?: string;
  effectiveTo?: string;
}

class CashbackPolicyService {
  private baseUrl = 'http://localhost:8080/api';

  /**
   * Update a cashback policy (partial update)
   * Only send fields that need to be updated
   */
  async updatePolicy(
    id: number,
    updates: UpdateCashbackPolicyRequest
  ): Promise<CashbackPolicyResponse> {
    const token = localStorage.getItem('token');

    const response = await fetch(
      `${this.baseUrl}/admin/cashback-policies/${id}`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updates),
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to update policy');
    }

    const result = await response.json();
    return result.data;
  }

  /**
   * Update only the platform of a policy
   */
  async updatePlatform(id: number, platformId: number | null): Promise<CashbackPolicyResponse> {
    return this.updatePolicy(id, { platformId });
  }

  /**
   * Update only the cashback rate
   */
  async updateCashbackRate(id: number, cashbackRate: number): Promise<CashbackPolicyResponse> {
    return this.updatePolicy(id, { cashbackRate });
  }

  /**
   * Activate or deactivate a policy
   */
  async setActive(id: number, isActive: boolean): Promise<CashbackPolicyResponse> {
    return this.updatePolicy(id, { isActive });
  }

  /**
   * Get policy by ID
   */
  async getById(id: number): Promise<CashbackPolicyResponse> {
    const token = localStorage.getItem('token');

    const response = await fetch(
      `${this.baseUrl}/admin/cashback-policies/${id}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to fetch policy');
    }

    const result = await response.json();
    return result.data;
  }
}

export default new CashbackPolicyService();
```

### Usage Examples

```typescript
import cashbackPolicyService from './services/cashbackPolicyService';

// Example 1: Update cashback rate
await cashbackPolicyService.updateCashbackRate(1, 75.00);

// Example 2: Change platform from Shopee to Lazada
await cashbackPolicyService.updatePlatform(1, 2);

// Example 3: Make policy universal (all platforms)
await cashbackPolicyService.updatePlatform(1, null);

// Example 4: Deactivate policy
await cashbackPolicyService.setActive(1, false);

// Example 5: Update multiple fields
await cashbackPolicyService.updatePolicy(1, {
  policyName: 'New Policy Name',
  platformId: 1,
  cashbackRate: 80.00,
  priority: 10,
  isActive: true,
});
```

---

## 📊 Platform ID Reference

| Platform ID | Name | Code | Status |
|-------------|------|------|--------|
| 1 | Shopee | `shopee` | Active |
| 2 | Lazada | `lazada` | Inactive |
| 3 | TikTok Shop | `tiktok` | Inactive |
| `null` | All Platforms | - | Universal/Fallback |

**Note**: When `platformId` is `null`, the policy applies to ALL platforms and acts as a fallback when no platform-specific policy exists.

---

## 🔄 Upcoming Features

These endpoints will be added in future phases:

- `POST /api/admin/cashback-policies` - Create new policy
- `DELETE /api/admin/cashback-policies/{id}` - Delete policy

---

## 💡 Tips for Frontend Developers

1. **Cache Policies**: Policies change rarely, cache them in app state
2. **Fallback Rate**: If no policy found, use default 60% rate
3. **User Level Badge**: Use color coding (NORMAL=blue, VIP=gold, SUPER=red)
4. **Display Format**: Show rate as percentage with 2 decimals (e.g., "70.00%")
5. **Priority Indicator**: Show priority in admin UI for conflict resolution
6. **Date Range Display**: Show effective period clearly (e.g., "Jan 1, 2025 → Permanent")

---

## 📞 Support

If you encounter issues:
1. Check backend logs: `logs/application.log`
2. Verify database has seed data
3. Check CORS settings
4. Contact backend team

---

**Last Updated**: 2025-12-16
**Backend Version**: 1.0.0-SNAPSHOT
**Status**: ✅ Ready for Frontend Integration

---

## 📋 Changelog

### 2025-12-16
- ⭐ **NEW**: Added `platformId` field to Update Policy API
- ⭐ **NEW**: Added `GET /api/admin/cashback-policies/{id}` endpoint documentation
- ⭐ **NEW**: Added detailed Update Policy API documentation with examples
- ⭐ **NEW**: Added React component example for Update Policy form
- ⭐ **NEW**: Added API Service examples with TypeScript
- Updated Platform ID Reference table
