# 📘 Affiliate Platform API Documentation for Frontend

**Module**: Affiliate Platform Management
**Created**: 2025-11-01
**Status**: ✅ Implemented & Ready
**Base URL**: `http://localhost:8080/api`

---

## 🎯 Overview

This module provides APIs for managing affiliate platforms (Shopee, Lazada, TikTok Shop).
These platforms are the source of affiliate orders and commissions.

**Use Cases**:
- Admin views list of all available platforms
- System retrieves platform details by code during file import
- Frontend displays platform information in dropdowns/selects

---

## 🔐 Authentication & Authorization

**Required Role**: `ADMIN` (for all endpoints)

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

> **Note**: Authentication with Keycloak will be implemented in later phase.
> For now, endpoints are accessible without authentication for testing.

---

## 📊 Data Model

### AffiliatePlatformResponse

```typescript
interface AffiliatePlatformResponse {
  id: number;                          // Platform ID
  name: string;                        // Platform name (e.g., "Shopee")
  code: string;                        // Platform code (e.g., "shopee")
  defaultCommissionRate: number | null; // Default commission % (e.g., 5.00)
  status: "ACTIVE" | "INACTIVE";       // Platform status
  hasApiCredentials: boolean;          // Whether API credentials are configured
  baseUrl: string | null;              // API base URL (if configured)
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

### 1. Get All Affiliate Platforms

Retrieve list of all affiliate platforms (both active and inactive).

**Endpoint**: `GET /api/admin/platforms`

**Method**: `GET`

**Query Parameters**: None

**Request Example**:

```bash
curl -X GET http://localhost:8080/api/admin/platforms \
  -H "Content-Type: application/json"
```

**Response**:

```json
{
  "success": true,
  "message": "Retrieved 3 platforms",
  "data": [
    {
      "id": 1,
      "name": "Shopee",
      "code": "shopee",
      "defaultCommissionRate": 5.00,
      "status": "ACTIVE",
      "hasApiCredentials": false,
      "baseUrl": null
    },
    {
      "id": 2,
      "name": "Lazada",
      "code": "lazada",
      "defaultCommissionRate": 4.50,
      "status": "INACTIVE",
      "hasApiCredentials": false,
      "baseUrl": null
    },
    {
      "id": 3,
      "name": "TikTok Shop",
      "code": "tiktok",
      "defaultCommissionRate": 6.00,
      "status": "INACTIVE",
      "hasApiCredentials": false,
      "baseUrl": null
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
const fetchPlatforms = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/admin/platforms');
    const result: ApiResponse<AffiliatePlatformResponse[]> = await response.json();

    if (result.success) {
      setPlatforms(result.data);
    } else {
      showError(result.message);
    }
  } catch (error) {
    showError('Failed to fetch platforms');
  }
};
```

**Use Cases**:
- Display platforms in admin dashboard
- Show platform selector in import form
- Filter orders by platform

---

### 2. Get Platform by Code

Retrieve specific platform details by code.

**Endpoint**: `GET /api/admin/platforms/code/{code}`

**Method**: `GET`

**Path Parameters**:
- `code` (string, required) - Platform code (e.g., "shopee", "lazada", "tiktok")

**Request Example**:

```bash
curl -X GET http://localhost:8080/api/admin/platforms/code/shopee \
  -H "Content-Type: application/json"
```

**Response (Success)**:

```json
{
  "success": true,
  "message": "Platform retrieved successfully",
  "data": {
    "id": 1,
    "name": "Shopee",
    "code": "shopee",
    "defaultCommissionRate": 5.00,
    "status": "ACTIVE",
    "hasApiCredentials": false,
    "baseUrl": null
  },
  "timestamp": "2025-11-01T15:30:00"
}
```

**Response (Not Found)**:

```json
{
  "success": false,
  "message": "Affiliate platform not found: invalid_code",
  "data": null,
  "timestamp": "2025-11-01T15:30:00"
}
```

**Status Codes**:
- `200 OK` - Success
- `404 Not Found` - Platform with given code not found
- `500 Internal Server Error` - Server error

**Frontend Usage**:

```typescript
// React/TypeScript example
const fetchPlatformByCode = async (code: string) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/admin/platforms/code/${code}`
    );

    if (!response.ok) {
      if (response.status === 404) {
        showError('Platform not found');
        return null;
      }
      throw new Error('Failed to fetch platform');
    }

    const result: ApiResponse<AffiliatePlatformResponse> = await response.json();
    return result.data;
  } catch (error) {
    showError('Failed to fetch platform');
    return null;
  }
};
```

**Use Cases**:
- Validate platform code during file import
- Display platform details
- Get commission rate for calculations

---

## 🎨 Frontend Implementation Examples

### 1. Platform Selector Component (React)

```tsx
import React, { useEffect, useState } from 'react';

interface Platform {
  id: number;
  name: string;
  code: string;
  status: string;
}

const PlatformSelector: React.FC = () => {
  const [platforms, setPlatforms] = useState<Platform[]>([]);
  const [selectedPlatform, setSelectedPlatform] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPlatforms();
  }, []);

  const fetchPlatforms = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/admin/platforms');
      const result = await response.json();

      if (result.success) {
        // Filter only active platforms
        const activePlatforms = result.data.filter(
          (p: Platform) => p.status === 'ACTIVE'
        );
        setPlatforms(activePlatforms);
      }
    } catch (error) {
      console.error('Failed to fetch platforms:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading platforms...</div>;

  return (
    <div className="platform-selector">
      <label htmlFor="platform">Select Platform:</label>
      <select
        id="platform"
        value={selectedPlatform}
        onChange={(e) => setSelectedPlatform(e.target.value)}
        className="form-control"
      >
        <option value="">-- Choose Platform --</option>
        {platforms.map((platform) => (
          <option key={platform.id} value={platform.code}>
            {platform.name} ({platform.code})
          </option>
        ))}
      </select>
    </div>
  );
};

export default PlatformSelector;
```

---

### 2. Platform List Component (React)

```tsx
import React, { useEffect, useState } from 'react';

interface Platform {
  id: number;
  name: string;
  code: string;
  defaultCommissionRate: number | null;
  status: 'ACTIVE' | 'INACTIVE';
  hasApiCredentials: boolean;
}

const PlatformList: React.FC = () => {
  const [platforms, setPlatforms] = useState<Platform[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPlatforms();
  }, []);

  const fetchPlatforms = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/admin/platforms');
      const result = await response.json();

      if (result.success) {
        setPlatforms(result.data);
      }
    } catch (error) {
      console.error('Failed to fetch platforms:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    return status === 'ACTIVE'
      ? <span className="badge bg-success">Active</span>
      : <span className="badge bg-secondary">Inactive</span>;
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="platform-list">
      <h2>Affiliate Platforms</h2>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Code</th>
            <th>Commission Rate</th>
            <th>Status</th>
            <th>API Configured</th>
          </tr>
        </thead>
        <tbody>
          {platforms.map((platform) => (
            <tr key={platform.id}>
              <td>{platform.id}</td>
              <td>{platform.name}</td>
              <td><code>{platform.code}</code></td>
              <td>
                {platform.defaultCommissionRate
                  ? `${platform.defaultCommissionRate}%`
                  : 'N/A'}
              </td>
              <td>{getStatusBadge(platform.status)}</td>
              <td>
                {platform.hasApiCredentials
                  ? <span className="text-success">✓</span>
                  : <span className="text-muted">✗</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PlatformList;
```

---

### 3. API Service (Axios)

```typescript
// services/platformService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export interface AffiliatePlatformResponse {
  id: number;
  name: string;
  code: string;
  defaultCommissionRate: number | null;
  status: 'ACTIVE' | 'INACTIVE';
  hasApiCredentials: boolean;
  baseUrl: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

class PlatformService {
  /**
   * Get all affiliate platforms
   */
  async getAllPlatforms(): Promise<AffiliatePlatformResponse[]> {
    const response = await axios.get<ApiResponse<AffiliatePlatformResponse[]>>(
      `${API_BASE_URL}/admin/platforms`
    );
    return response.data.data;
  }

  /**
   * Get platform by code
   */
  async getPlatformByCode(code: string): Promise<AffiliatePlatformResponse> {
    const response = await axios.get<ApiResponse<AffiliatePlatformResponse>>(
      `${API_BASE_URL}/admin/platforms/code/${code}`
    );
    return response.data.data;
  }

  /**
   * Get only active platforms
   */
  async getActivePlatforms(): Promise<AffiliatePlatformResponse[]> {
    const platforms = await this.getAllPlatforms();
    return platforms.filter(p => p.status === 'ACTIVE');
  }
}

export default new PlatformService();
```

**Usage**:

```typescript
import platformService from './services/platformService';

// In component
const loadPlatforms = async () => {
  try {
    const platforms = await platformService.getActivePlatforms();
    setPlatforms(platforms);
  } catch (error) {
    console.error('Error:', error);
  }
};
```

---

## 🧪 Testing with Postman

### Collection Setup

1. **Create Collection**: "CashBee - Affiliate Platforms"
2. **Set Base URL Variable**: `{{base_url}}` = `http://localhost:8080`

### Test Cases

#### Test 1: Get All Platforms

```
GET {{base_url}}/api/admin/platforms
```

**Expected**: 200 OK with array of 3 platforms

**Assertions**:
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success = true", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Response contains 3 platforms", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.eql(3);
});

pm.test("First platform is Shopee", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data[0].code).to.eql("shopee");
    pm.expect(jsonData.data[0].status).to.eql("ACTIVE");
});
```

#### Test 2: Get Platform by Code (Success)

```
GET {{base_url}}/api/admin/platforms/code/shopee
```

**Expected**: 200 OK with Shopee platform details

**Assertions**:
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Platform code is shopee", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.code).to.eql("shopee");
    pm.expect(jsonData.data.name).to.eql("Shopee");
});
```

#### Test 3: Get Platform by Code (Not Found)

```
GET {{base_url}}/api/admin/platforms/code/invalid_platform
```

**Expected**: 404 Not Found

**Assertions**:
```javascript
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

pm.test("Response has success = false", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(false);
});
```

---

## 🔍 Error Handling

### Error Response Format

All errors follow this structure:

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
| 404 | "Affiliate platform not found: {code}" | Invalid platform code | Check platform code exists |
| 500 | "Internal server error" | Database/server issue | Contact backend team |
| 400 | "Bad request" | Invalid request format | Check request body/params |

### Frontend Error Handling

```typescript
const handleApiError = (error: any) => {
  if (error.response) {
    // Server responded with error
    const status = error.response.status;
    const message = error.response.data?.message || 'Unknown error';

    switch (status) {
      case 404:
        showNotification('Platform not found', 'error');
        break;
      case 500:
        showNotification('Server error. Please try again later.', 'error');
        break;
      default:
        showNotification(message, 'error');
    }
  } else if (error.request) {
    // Request made but no response
    showNotification('Cannot connect to server', 'error');
  } else {
    // Other errors
    showNotification('An error occurred', 'error');
  }
};
```

---

## 📝 Database Seed Data

The system comes pre-loaded with these platforms:

| ID | Name | Code | Commission Rate | Status |
|----|------|------|----------------|--------|
| 1 | Shopee | shopee | 5.00% | ACTIVE |
| 2 | Lazada | lazada | 4.50% | INACTIVE |
| 3 | TikTok Shop | tiktok | 6.00% | INACTIVE |

> **Note**: Only **Shopee** is ACTIVE in MVP. Other platforms are for future expansion.

---

## 🎯 Frontend Checklist

Before using these APIs, ensure:

- [ ] Backend is running on `http://localhost:8080`
- [ ] Database is initialized with seed data
- [ ] CORS is enabled (if frontend on different origin)
- [ ] Error handling is implemented
- [ ] Loading states are shown during API calls
- [ ] Success/error notifications are displayed

---

## 🚀 Quick Start for Frontend

### 1. Check Backend Status

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

### 2. Test Platform API

```bash
curl http://localhost:8080/api/admin/platforms
```

Expected: JSON array with 3 platforms

### 3. Integrate in Frontend

Copy the API service code above and start using it!

---

## 📚 Related Documentation

- **Backend API Docs (Swagger)**: http://localhost:8080/swagger-ui.html
- **Implementation Plan**: See `IMPLEMENTATION_PLAN.md`
- **Project README**: See `README.md`

---

## 🔄 Upcoming Features

These endpoints will be added in future phases:

- `POST /api/admin/platforms` - Create new platform (Phase 6+)
- `PUT /api/admin/platforms/{id}` - Update platform (Phase 6+)
- `DELETE /api/admin/platforms/{id}` - Delete platform (Phase 6+)
- `PUT /api/admin/platforms/{id}/status` - Activate/Deactivate (Phase 6+)

---

## 💡 Tips for Frontend Developers

1. **Cache Platform Data**: Platforms rarely change, so cache them in app state
2. **Filter Active Platforms**: For user-facing features, only show ACTIVE platforms
3. **Use Platform Code**: Use `code` field as identifier, not `id` (more stable)
4. **Handle Loading States**: Always show loading indicators during API calls
5. **Validate Before Submit**: Check platform exists before submitting forms

---

## 📞 Support

If you encounter issues:
1. Check backend logs: `logs/application.log`
2. Verify database connection
3. Check CORS settings
4. Contact backend team

---

**Last Updated**: 2025-11-01
**Backend Version**: 1.0.0-SNAPSHOT
**Status**: ✅ Ready for Frontend Integration
