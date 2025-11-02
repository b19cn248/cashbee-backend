# CashBee API Documentation - Phase 5 & 6: Affiliate Tracking & Import

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture & Flow](#architecture--flow)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [1. Create Tracking Link](#1-create-tracking-link)
  - [2. Handle Click Redirect](#2-handle-click-redirect)
  - [3. Import Orders (Admin)](#3-import-orders-admin)
  - [4. Get Import Batch (Admin)](#4-get-import-batch-admin)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Sample Mock Data](#sample-mock-data)
- [Testing Scenarios](#testing-scenarios)

---

## Overview

Phase 5 & 6 implement the complete affiliate tracking and order import system:

### **Phase 5: Affiliate Link Tracking**
- User converts Shopee product URLs to affiliate tracking links
- System tracks clicks and redirects users to Shopee
- Each click is associated with a user for order matching

### **Phase 6: Order Import & Matching**
- Admin uploads daily CSV files from Shopee affiliate portal
- System parses CSV and creates orders
- Orders are automatically matched with user clicks via tracking codes
- Users earn cashback when orders are matched

### **Key Features**
✅ Support multiple Shopee URL formats
✅ Generate unique tracking codes per user
✅ Auto-redirect with HTTP 302
✅ Parse CSV with error handling
✅ Auto-match orders with clicks
✅ Detailed import statistics

---

## Architecture & Flow

### **User Flow: Creating Tracking Link**

```
┌──────────┐         ┌──────────┐         ┌──────────┐         ┌──────────┐
│  User    │         │ Frontend │         │ Backend  │         │ Database │
└────┬─────┘         └────┬─────┘         └────┬─────┘         └────┬─────┘
     │                    │                    │                    │
     │ 1. Copy Shopee URL │                    │                    │
     │────────────────────>                    │                    │
     │                    │                    │                    │
     │ 2. Paste into app  │                    │                    │
     │────────────────────>                    │                    │
     │                    │                    │                    │
     │                    │ 3. POST /create-link                    │
     │                    │ {shopeeUrl, userId}│                    │
     │                    │────────────────────>                    │
     │                    │                    │                    │
     │                    │                    │ 4. Parse URL       │
     │                    │                    │ Extract: shop_id,  │
     │                    │                    │         item_id    │
     │                    │                    │                    │
     │                    │                    │ 5. Save Click      │
     │                    │                    │────────────────────>
     │                    │                    │                    │
     │                    │                    │ 6. Generate code   │
     │                    │                    │ CB1_100_20251101   │
     │                    │                    │                    │
     │                    │                    │ 7. Build URL       │
     │                    │                    │ shopee.vn/...      │
     │                    │                    │ ?af_sub1=CB1_100   │
     │                    │                    │                    │
     │                    │ 8. Return Response │                    │
     │                    │ {trackingUrl, ...} │                    │
     │                    │<────────────────────                    │
     │                    │                    │                    │
     │ 9. Display link    │                    │                    │
     │<────────────────────                    │                    │
     │                    │                    │                    │
     │ 10. Click link     │                    │                    │
     │────────────────────>                    │                    │
     │                    │                    │                    │
     │                    │ 11. GET /redirect/100                   │
     │                    │────────────────────>                    │
     │                    │                    │                    │
     │                    │                    │ 12. Mark CLICKED   │
     │                    │                    │────────────────────>
     │                    │                    │                    │
     │                    │ 13. HTTP 302       │                    │
     │                    │ Location: shopee..│                    │
     │                    │<────────────────────                    │
     │                    │                    │                    │
     │ 14. Redirect       │                    │                    │
     │────────────────────────────────────────────────────>         │
     │                    │                    │        Shopee.vn   │
```

### **Admin Flow: Import Orders**

```
┌──────────┐         ┌──────────┐         ┌──────────┐         ┌──────────┐
│  Admin   │         │ Frontend │         │ Backend  │         │ Database │
└────┬─────┘         └────┬─────┘         └────┬─────┘         └────┬─────┘
     │                    │                    │                    │
     │ 1. Download CSV    │                    │                    │
     │ from Shopee Portal │                    │                    │
     │                    │                    │                    │
     │ 2. Upload CSV      │                    │                    │
     │────────────────────>                    │                    │
     │                    │                    │                    │
     │                    │ 3. POST /import/orders                  │
     │                    │ (multipart/form-data)                   │
     │                    │────────────────────>                    │
     │                    │                    │                    │
     │                    │                    │ 4. Create Batch    │
     │                    │                    │────────────────────>
     │                    │                    │                    │
     │                    │                    │ 5. Parse CSV       │
     │                    │                    │ 2000 rows          │
     │                    │                    │                    │
     │                    │                    │ FOR EACH ROW:      │
     │                    │                    │ ┌────────────────┐ │
     │                    │                    │ │ Check duplicate│ │
     │                    │                    │ │ Extract code   │ │
     │                    │                    │ │ Find user      │ │
     │                    │                    │ │ Create order   │ │
     │                    │                    │ │ Match click    │ │
     │                    │                    │ └────────────────┘ │
     │                    │                    │                    │
     │                    │                    │ 6. Save Orders     │
     │                    │                    │────────────────────>
     │                    │                    │                    │
     │                    │                    │ 7. Update Batch    │
     │                    │                    │ Success: 1850      │
     │                    │                    │ Failed: 50         │
     │                    │                    │ Skipped: 100       │
     │                    │                    │────────────────────>
     │                    │                    │                    │
     │                    │ 8. Return Stats    │                    │
     │                    │<────────────────────                    │
     │                    │                    │                    │
     │ 9. Show results    │                    │                    │
     │<────────────────────                    │                    │
```

---

## Authentication

All API endpoints require JWT authentication via Keycloak.

### **Headers Required**

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### **User Roles**

- **USER**: Can create tracking links, view own orders
- **ADMIN**: Can import CSV, view all orders, manage platforms

---

## API Endpoints

### **Base URL**

```
Development: http://localhost:8080/api
Production: https://api.cashbee.vn/api
```

---

## 1. Create Tracking Link

Convert Shopee product URL to affiliate tracking link.

### **Endpoint**

```http
POST /affiliate/tracking/create-link
```

### **Request Headers**

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### **Request Body**

```json
{
  "shopeeUrl": "https://shopee.vn/product/47305935/20317610036",
  "platformCode": "shopee",
  "userId": 1
}
```

#### **Field Descriptions**

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `shopeeUrl` | string | ✅ Yes | Original Shopee product URL | `https://shopee.vn/product/123/456` |
| `platformCode` | string | ❌ No | Platform code (default: "shopee") | `shopee`, `lazada` |
| `userId` | number | ✅ Yes | User ID from JWT token | `1` |

#### **Supported Shopee URL Formats**

```javascript
// Format 1: Standard
"https://shopee.vn/product/47305935/20317610036"

// Format 2: Short with product name
"https://shopee.vn/Dây-Nhảy-Thể-Dục-i.47305935.20317610036"

// Format 3: Short format
"https://shopee.vn/-i.47305935.20317610036"

// Format 4: Universal link
"https://shopee.vn/universal-link/20317610036"
```

### **Success Response**

**HTTP Status:** `201 Created`

```json
{
  "success": true,
  "message": "Tracking link created successfully",
  "data": {
    "clickId": 100,
    "trackingUrl": "https://shopee.vn/universal-link/20317610036?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_100_20251101160530",
    "trackingCode": "CB1_100_20251101160530",
    "originalUrl": "https://shopee.vn/product/47305935/20317610036",
    "productName": null,
    "shopId": "47305935",
    "itemId": "20317610036",
    "platformName": "Shopee",
    "platformCode": "shopee",
    "estimatedCashbackRate": 5.00,
    "createdAt": "2025-11-01T16:05:30",
    "message": "Click this link to shop on Shopee and earn cashback!"
  },
  "timestamp": "2025-11-01T16:05:30"
}
```

#### **Response Field Descriptions**

| Field | Type | Description |
|-------|------|-------------|
| `clickId` | number | Unique click ID (use for redirect) |
| `trackingUrl` | string | **Full affiliate URL to display to user** |
| `trackingCode` | string | Unique tracking code (format: CB{userId}_{clickId}_{timestamp}) |
| `originalUrl` | string | Original URL user pasted |
| `productName` | string\|null | Product name (may be null) |
| `shopId` | string | Shop ID extracted from URL |
| `itemId` | string | Item/Product ID extracted from URL |
| `platformName` | string | Platform display name |
| `platformCode` | string | Platform code |
| `estimatedCashbackRate` | number | Estimated cashback percentage (e.g., 5.00 = 5%) |
| `createdAt` | string | ISO timestamp |
| `message` | string | User-friendly message |

### **Error Responses**

#### **400 Bad Request - Invalid URL**

```json
{
  "success": false,
  "errorCode": "INVALID_URL",
  "message": "Invalid Shopee URL format: URL does not match any supported pattern",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

#### **400 Bad Request - Platform Inactive**

```json
{
  "success": false,
  "errorCode": "PLATFORM_INACTIVE",
  "message": "Platform is not active: Shopee",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

#### **400 Bad Request - Tracking Disabled**

```json
{
  "success": false,
  "errorCode": "TRACKING_DISABLED",
  "message": "Tracking is not enabled for platform: Shopee",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

#### **404 Not Found - Platform Not Found**

```json
{
  "success": false,
  "errorCode": "PLATFORM_NOT_FOUND",
  "message": "Affiliate platform not found: lazada",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

#### **401 Unauthorized**

```json
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "Invalid or expired JWT token",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

### **Frontend Implementation Example**

```typescript
// TypeScript/React Example

interface CreateTrackingLinkRequest {
  shopeeUrl: string;
  platformCode?: string;
  userId: number;
}

interface TrackingLinkResponse {
  clickId: number;
  trackingUrl: string;
  trackingCode: string;
  originalUrl: string;
  productName: string | null;
  shopId: string;
  itemId: string;
  platformName: string;
  platformCode: string;
  estimatedCashbackRate: number;
  createdAt: string;
  message: string;
}

async function createTrackingLink(
  shopeeUrl: string,
  userId: number,
  token: string
): Promise<TrackingLinkResponse> {
  const response = await fetch('http://localhost:8080/api/affiliate/tracking/create-link', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      shopeeUrl,
      platformCode: 'shopee',
      userId,
    }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  const result = await response.json();
  return result.data;
}

// Usage in React Component
function CreateLinkForm() {
  const [url, setUrl] = useState('');
  const [trackingUrl, setTrackingUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const userId = 1; // From auth context
  const token = 'your-jwt-token'; // From auth context

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await createTrackingLink(url, userId, token);
      setTrackingUrl(result.trackingUrl);

      // Show success message
      alert(`Cashback: ${result.estimatedCashbackRate}% - ${result.message}`);
    } catch (error) {
      alert('Error: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Paste Shopee URL here"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
      />
      <button type="submit" disabled={loading}>
        {loading ? 'Creating...' : 'Create Tracking Link'}
      </button>

      {trackingUrl && (
        <div>
          <p>Your tracking link:</p>
          <a href={trackingUrl} target="_blank" rel="noopener noreferrer">
            {trackingUrl}
          </a>
          <button onClick={() => navigator.clipboard.writeText(trackingUrl)}>
            Copy Link
          </button>
        </div>
      )}
    </form>
  );
}
```

---

## 2. Handle Click Redirect

Track click event and redirect user to Shopee affiliate URL.

### **Endpoint**

```http
GET /affiliate/tracking/redirect/{clickId}
```

### **Request Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `clickId` | number | ✅ Yes | Click ID from create-link response |

### **Request Headers**

```http
Authorization: Bearer <JWT_TOKEN>
```

### **Success Response**

**HTTP Status:** `302 Found` (Redirect)

**Response Headers:**
```http
Location: https://shopee.vn/universal-link/20317610036?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_100_20251101160530
```

**No body** - Browser automatically follows redirect to Shopee.

### **Alternative Endpoint (by Tracking Code)**

```http
GET /affiliate/tracking/redirect/code/{trackingCode}
```

**Example:**
```
GET /affiliate/tracking/redirect/code/CB1_100_20251101160530
```

### **Error Responses**

#### **404 Not Found - Click Not Found**

```json
{
  "success": false,
  "errorCode": "CLICK_NOT_FOUND",
  "message": "Tracking link not found or expired",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

### **Frontend Implementation Example**

```typescript
// Option 1: Direct Link (Recommended)
// User clicks on <a> tag, browser handles redirect automatically
<a
  href={`http://localhost:8080/api/affiliate/tracking/redirect/${clickId}`}
  target="_blank"
  rel="noopener noreferrer"
>
  Shop Now and Earn {cashbackRate}% Cashback
</a>

// Option 2: Programmatic Redirect
function handleShopNowClick(clickId: number) {
  // Open in new tab
  window.open(
    `http://localhost:8080/api/affiliate/tracking/redirect/${clickId}`,
    '_blank'
  );
}

// Option 3: Using trackingUrl directly (No tracking via this endpoint)
// Use the trackingUrl returned from create-link
<a
  href={trackingUrl}
  target="_blank"
  rel="noopener noreferrer"
>
  Shop Now
</a>
```

### **Testing Endpoint (Get URL without redirect)**

For testing purposes, you can get the tracking URL without redirecting:

```http
GET /affiliate/tracking/link/{clickId}
```

**Response:**
```json
{
  "success": true,
  "message": "Tracking link retrieved successfully",
  "data": "https://shopee.vn/universal-link/20317610036?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_100_20251101160530",
  "timestamp": "2025-11-01T16:05:30"
}
```

---

## 3. Import Orders (Admin)

Upload CSV file from Shopee affiliate portal to import orders.

### **Endpoint**

```http
POST /admin/import/orders
```

### **Request Headers**

```http
Authorization: Bearer <ADMIN_JWT_TOKEN>
Content-Type: multipart/form-data
```

### **Request Body (multipart/form-data)**

| Field | Type | Required | Description | Default |
|-------|------|----------|-------------|---------|
| `file` | File | ✅ Yes | CSV file from Shopee portal | - |
| `platformCode` | string | ❌ No | Platform code | `shopee` |
| `importedBy` | number | ✅ Yes | Admin user ID | - |
| `skipDuplicates` | boolean | ❌ No | Skip duplicate order IDs | `true` |
| `autoMatch` | boolean | ❌ No | Auto-match with clicks | `true` |

### **CSV File Format**

**File Name Example:** `AffiliateCommissionReport202510300813.csv`

**Headers (first row):**
```csv
ID đơn hàng,Trạng thái đặt hàng,Checkout id,Thời Gian Đặt Hàng,Thời gian hoàn thành,Thời gian Click,Tên Shop,Shop id,Loại Shop,Item id,Tên Item,ID Model,Loại sản phẩm,Promotion id,L1 Danh mục toàn cầu,L2 Danh mục toàn cầu,L3 Danh mục toàn cầu,Giá(₫),Số lượng,Loại Hoa hồng,Đối tác chiến dịchr,Giá trị đơn hàng (₫),Số tiền hoàn trả (₫),Tỷ lệ sản phẩm hoa hồng Shope,Hoa hồng Shopee trên sản phẩm(₫),Tỷ lệ sản phẩm hoa hồng người bán,Hoa hồng Xtra trên sản phẩm(₫),Tổng hoa hồng sản phẩm(₫),Hoa hồng đơn hàng từ Shopee(₫),Hoa hồng đơn hàng từ Người bán(₫),Tổng hoa hồng đơn hàng(₫),Tên MNC đã liên kết,Mã hợp đồng MCN,Mức phí quản lý MCN,Phí quản lý MCN(₫),Mức hoa hồng tiếp thị liên kết theo thỏa thuận,Hoa hồng ròng tiếp thị liên kết(₫),Trạng thái sản phẩm liên kết,Ghi chú sản phẩm,Loại thuộc tính,Trạng thái người mua,Sub_id1,Sub_id2,Sub_id3,Sub_id4,Sub_id5,Kênh
```

**Sample Data Row:**
```csv
251030D6GR3SET,Đang chờ xử lý,215453681217374,2025-10-29 23:14:53,,2025-10-23 13:34:17,GYMI - DỤNG CỤ TẬP GYM,47305935,Preferred(Non-CB),20317610036,"Dây Nhảy Thể Dục",137047019769,Normal Product,,Thể Thao & Dã Ngoại,Dụng Cụ Thể Thao & Dã Ngoại,Thiết Bị Thể Thao,39000,1,XTRA Comm,,38092,,6.00%,2285.52,6.00%,2285.52,4571.04,2285.52,2285.52,4571.04,,0,0.00%,0,100.00%,4571.04,Đang chờ xử lý,"Trạng thái của sản phẩm đang chờ xử lý",Đơn hàng từ các Shop khác nhau,Đã tồn tại,CB1_100_20251101160530,,,,,Others
```

**Key Column: `Sub_id1`** - Contains tracking code (e.g., `CB1_100_20251101160530`)

### **Success Response**

**HTTP Status:** `200 OK` (Completed) or `207 Multi-Status` (Partial)

```json
{
  "success": true,
  "message": "Successfully imported 1850 orders. 1700 orders matched with clicks.",
  "data": {
    "batchId": 42,
    "platformName": "Shopee",
    "platformCode": "shopee",
    "fileName": "AffiliateCommissionReport202510300813.csv",
    "status": "COMPLETED",
    "totalRows": 2000,
    "successCount": 1850,
    "failedCount": 50,
    "skippedCount": 100,
    "matchedCount": 1700,
    "successRate": 92.5,
    "errorMessage": null,
    "errors": [
      {
        "rowNumber": 15,
        "orderId": "251029D15M6EUC",
        "error": "Duplicate order ID",
        "rawData": "251029D15M6EUC,Đã hủy,..."
      },
      {
        "rowNumber": 127,
        "orderId": null,
        "error": "Invalid commission amount format",
        "rawData": "251029D2RWDV0C,..."
      }
    ],
    "startedAt": "2025-11-01T10:00:00",
    "completedAt": "2025-11-01T10:02:15",
    "durationSeconds": 135,
    "importedBy": 5
  },
  "timestamp": "2025-11-01T10:02:15"
}
```

#### **Response Field Descriptions**

| Field | Type | Description |
|-------|------|-------------|
| `batchId` | number | Import batch ID (for tracking) |
| `platformName` | string | Platform name |
| `platformCode` | string | Platform code |
| `fileName` | string | Original file name |
| `status` | string | Import status: `PROCESSING`, `COMPLETED`, `PARTIAL`, `FAILED` |
| `totalRows` | number | Total rows in CSV (excluding header) |
| `successCount` | number | Successfully imported orders |
| `failedCount` | number | Failed to import |
| `skippedCount` | number | Skipped (duplicates/cancelled) |
| `matchedCount` | number | Orders matched with clicks |
| `successRate` | number | Success percentage (0-100) |
| `errorMessage` | string\|null | Overall error message (if failed) |
| `errors` | array | List of error details per row |
| `errors[].rowNumber` | number | Row number in CSV (1-based) |
| `errors[].orderId` | string\|null | Order ID (if available) |
| `errors[].error` | string | Error message |
| `errors[].rawData` | string | Raw CSV row data (truncated) |
| `startedAt` | string | Import start timestamp |
| `completedAt` | string | Import completion timestamp |
| `durationSeconds` | number | Duration in seconds |
| `importedBy` | number | Admin user ID |

### **Import Status Values**

| Status | Description | HTTP Status |
|--------|-------------|-------------|
| `PROCESSING` | Import in progress | `202 Accepted` |
| `COMPLETED` | All rows imported successfully | `200 OK` |
| `PARTIAL` | Some rows succeeded, some failed | `207 Multi-Status` |
| `FAILED` | Import failed completely | `400 Bad Request` |

### **Error Responses**

#### **400 Bad Request - Empty File**

```json
{
  "success": false,
  "errorCode": "INVALID_FILE",
  "message": "CSV file is empty",
  "data": null,
  "timestamp": "2025-11-01T10:00:00"
}
```

#### **400 Bad Request - Invalid File Type**

```json
{
  "success": false,
  "errorCode": "INVALID_FILE_TYPE",
  "message": "Only CSV files are supported",
  "data": null,
  "timestamp": "2025-11-01T10:00:00"
}
```

#### **400 Bad Request - CSV Parse Error**

```json
{
  "success": false,
  "errorCode": "CSV_PARSE_ERROR",
  "message": "Import failed: Failed to parse CSV file: Invalid UTF-8 encoding",
  "data": {
    "batchId": 43,
    "status": "FAILED",
    "totalRows": 0,
    "successCount": 0,
    "failedCount": 0,
    "errorMessage": "Failed to parse CSV file: Invalid UTF-8 encoding"
  },
  "timestamp": "2025-11-01T10:00:05"
}
```

#### **403 Forbidden - Not Admin**

```json
{
  "success": false,
  "errorCode": "FORBIDDEN",
  "message": "Admin role required to import orders",
  "data": null,
  "timestamp": "2025-11-01T10:00:00"
}
```

### **Frontend Implementation Example**

```typescript
// TypeScript/React Example

interface ImportOrdersRequest {
  file: File;
  platformCode?: string;
  importedBy: number;
  skipDuplicates?: boolean;
  autoMatch?: boolean;
}

interface ImportOrdersResponse {
  batchId: number;
  platformName: string;
  platformCode: string;
  fileName: string;
  status: 'PROCESSING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';
  totalRows: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  matchedCount: number;
  successRate: number;
  errorMessage: string | null;
  errors: Array<{
    rowNumber: number;
    orderId: string | null;
    error: string;
    rawData: string;
  }>;
  startedAt: string;
  completedAt: string;
  durationSeconds: number;
  importedBy: number;
}

async function importOrders(
  file: File,
  adminUserId: number,
  token: string
): Promise<ImportOrdersResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('platformCode', 'shopee');
  formData.append('importedBy', adminUserId.toString());
  formData.append('skipDuplicates', 'true');
  formData.append('autoMatch', 'true');

  const response = await fetch('http://localhost:8080/api/admin/import/orders', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      // Note: Do NOT set Content-Type for multipart/form-data
      // Browser will set it automatically with boundary
    },
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  const result = await response.json();
  return result.data;
}

// Usage in React Component
function ImportOrdersForm() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportOrdersResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const adminUserId = 5; // From auth context
  const token = 'admin-jwt-token'; // From auth context

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;

    setLoading(true);
    setResult(null);

    try {
      const result = await importOrders(file, adminUserId, token);
      setResult(result);

      // Show success message
      alert(`Import completed! ${result.successCount} orders imported.`);
    } catch (error) {
      alert('Error: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        <input
          type="file"
          accept=".csv"
          onChange={handleFileChange}
        />
        <button type="submit" disabled={!file || loading}>
          {loading ? 'Importing...' : 'Import Orders'}
        </button>
      </form>

      {result && (
        <div className="import-results">
          <h3>Import Results</h3>
          <div className="stats">
            <p>Batch ID: {result.batchId}</p>
            <p>Status: <span className={`status-${result.status}`}>{result.status}</span></p>
            <p>Total Rows: {result.totalRows}</p>
            <p>Success: {result.successCount} ({result.successRate.toFixed(2)}%)</p>
            <p>Failed: {result.failedCount}</p>
            <p>Skipped: {result.skippedCount}</p>
            <p>Matched: {result.matchedCount}</p>
            <p>Duration: {result.durationSeconds}s</p>
          </div>

          {result.errors.length > 0 && (
            <div className="errors">
              <h4>Errors ({result.errors.length})</h4>
              <table>
                <thead>
                  <tr>
                    <th>Row</th>
                    <th>Order ID</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {result.errors.slice(0, 10).map((error, index) => (
                    <tr key={index}>
                      <td>{error.rowNumber}</td>
                      <td>{error.orderId || 'N/A'}</td>
                      <td>{error.error}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {result.errors.length > 10 && (
                <p>... and {result.errors.length - 10} more errors</p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
```

---

## 4. Get Import Batch (Admin)

Get details of a specific import batch.

### **Endpoint**

```http
GET /admin/import/batches/{batchId}
```

### **Request Headers**

```http
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### **Request Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `batchId` | number | ✅ Yes | Import batch ID |

### **Success Response**

**HTTP Status:** `200 OK`

```json
{
  "success": true,
  "message": "Batch details retrieved",
  "data": "Import batch 42",
  "timestamp": "2025-11-01T10:30:00"
}
```

> **Note:** Full implementation coming soon. Currently returns placeholder.

---

## Data Models

### **TrackingLinkResponse**

```typescript
interface TrackingLinkResponse {
  clickId: number;
  trackingUrl: string;
  trackingCode: string;
  originalUrl: string;
  productName: string | null;
  shopId: string;
  itemId: string;
  platformName: string;
  platformCode: string;
  estimatedCashbackRate: number;
  createdAt: string;
  message: string;
}
```

### **ImportOrdersResponse**

```typescript
interface ImportOrdersResponse {
  batchId: number;
  platformName: string;
  platformCode: string;
  fileName: string;
  status: ImportStatus;
  totalRows: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  matchedCount: number;
  successRate: number;
  errorMessage: string | null;
  errors: ImportErrorDetail[];
  startedAt: string;
  completedAt: string;
  durationSeconds: number;
  importedBy: number;
}

type ImportStatus = 'PROCESSING' | 'COMPLETED' | 'PARTIAL' | 'FAILED';

interface ImportErrorDetail {
  rowNumber: number;
  orderId: string | null;
  error: string;
  rawData: string;
}
```

### **AffiliateClick** (Database Model)

```typescript
interface AffiliateClick {
  id: number;
  userId: number;
  platformId: number;
  shopId: string | null;
  itemId: string;
  productName: string | null;
  trackingCode: string;
  trackingUrl: string;
  originalUrl: string;
  createdAt: string;
  clickedAt: string | null;
  orderMatched: boolean;
  matchedOrderId: number | null;
  status: ClickStatus;
}

type ClickStatus = 'CREATED' | 'CLICKED' | 'MATCHED' | 'EXPIRED';
```

### **AffiliateOrder** (Database Model)

```typescript
interface AffiliateOrder {
  id: number;
  platformId: number;
  userId: number;
  clickId: string | null;
  orderId: string;
  orderStatus: OrderStatus;
  productName: string | null;
  productPrice: number;
  commissionAmount: number;
  currency: string;
  orderTime: string;
  confirmTime: string | null;
  paidTime: string | null;
  source: 'IMPORT' | 'API';
  importBatchId: number | null;
  createdAt: string;
  updatedAt: string;
}

type OrderStatus = 'PENDING' | 'APPROVED' | 'PAID' | 'CANCELLED' | 'REJECTED';
```

### **ImportBatch** (Database Model)

```typescript
interface ImportBatch {
  id: number;
  platformId: number;
  fileName: string;
  totalRows: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  status: ImportStatus;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  importedBy: number;
}
```

---

## Error Handling

### **Standard Error Response Format**

```json
{
  "success": false,
  "errorCode": "ERROR_CODE",
  "message": "Human-readable error message",
  "data": null,
  "timestamp": "2025-11-01T16:05:30"
}
```

### **Common Error Codes**

| Error Code | HTTP Status | Description | Action |
|------------|-------------|-------------|--------|
| `UNAUTHORIZED` | 401 | Invalid or expired JWT token | Refresh token or re-login |
| `FORBIDDEN` | 403 | Insufficient permissions | Check user role |
| `NOT_FOUND` | 404 | Resource not found | Verify ID or code |
| `INVALID_URL` | 400 | Invalid Shopee URL format | Show format examples |
| `PLATFORM_NOT_FOUND` | 404 | Platform doesn't exist | Check platform code |
| `PLATFORM_INACTIVE` | 400 | Platform is not active | Contact admin |
| `TRACKING_DISABLED` | 400 | Tracking not enabled | Contact admin |
| `CLICK_NOT_FOUND` | 404 | Click ID doesn't exist | Verify click ID |
| `INVALID_FILE` | 400 | File is empty or invalid | Check file |
| `INVALID_FILE_TYPE` | 400 | Not a CSV file | Upload .csv only |
| `FILE_READ_ERROR` | 400 | Cannot read file | Check file permissions |
| `CSV_PARSE_ERROR` | 400 | CSV format error | Check CSV format |
| `DUPLICATE_ORDER` | 400 | Order ID already exists | Skip or update |
| `VALIDATION_ERROR` | 400 | Request validation failed | Check request body |
| `INTERNAL_ERROR` | 500 | Server error | Retry or contact support |

### **Frontend Error Handling Example**

```typescript
async function handleApiCall<T>(
  apiCall: () => Promise<Response>
): Promise<T> {
  try {
    const response = await apiCall();

    // Handle non-2xx responses
    if (!response.ok) {
      const error = await response.json();

      switch (error.errorCode) {
        case 'UNAUTHORIZED':
          // Redirect to login
          window.location.href = '/login';
          break;

        case 'INVALID_URL':
          throw new Error('Please enter a valid Shopee product URL');

        case 'PLATFORM_INACTIVE':
          throw new Error('This platform is temporarily unavailable');

        case 'TRACKING_DISABLED':
          throw new Error('Tracking is not available for this platform');

        default:
          throw new Error(error.message || 'An error occurred');
      }
    }

    const result = await response.json();
    return result.data;

  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Network error. Please check your connection.');
  }
}
```

---

## Sample Mock Data

For frontend development without backend, use these mock responses:

### **Mock: Create Tracking Link**

```javascript
// mock-api.ts
export const mockCreateTrackingLink = {
  success: true,
  message: "Tracking link created successfully",
  data: {
    clickId: 100,
    trackingUrl: "https://shopee.vn/universal-link/20317610036?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_100_20251101160530",
    trackingCode: "CB1_100_20251101160530",
    originalUrl: "https://shopee.vn/product/47305935/20317610036",
    productName: "Dây Nhảy Thể Dục, Dây Nhảy Thể Lực Tập Thể Dục",
    shopId: "47305935",
    itemId: "20317610036",
    platformName: "Shopee",
    platformCode: "shopee",
    estimatedCashbackRate: 5.00,
    createdAt: new Date().toISOString(),
    message: "Click this link to shop on Shopee and earn cashback!"
  },
  timestamp: new Date().toISOString()
};
```

### **Mock: Import Orders (Success)**

```javascript
export const mockImportOrdersSuccess = {
  success: true,
  message: "Successfully imported 1850 orders. 1700 orders matched with clicks.",
  data: {
    batchId: 42,
    platformName: "Shopee",
    platformCode: "shopee",
    fileName: "AffiliateCommissionReport202510300813.csv",
    status: "COMPLETED",
    totalRows: 2000,
    successCount: 1850,
    failedCount: 50,
    skippedCount: 100,
    matchedCount: 1700,
    successRate: 92.5,
    errorMessage: null,
    errors: [
      {
        rowNumber: 15,
        orderId: "251029D15M6EUC",
        error: "Duplicate order ID",
        rawData: "251029D15M6EUC,Đã hủy,..."
      },
      {
        rowNumber: 127,
        orderId: "251029D2RWDV0C",
        error: "Invalid commission amount format",
        rawData: "251029D2RWDV0C,..."
      }
    ],
    startedAt: new Date(Date.now() - 135000).toISOString(),
    completedAt: new Date().toISOString(),
    durationSeconds: 135,
    importedBy: 5
  },
  timestamp: new Date().toISOString()
};
```

### **Mock: Import Orders (Partial)**

```javascript
export const mockImportOrdersPartial = {
  success: true,
  message: "Partially imported 1200 orders (500 failed, 300 skipped). 1000 orders matched.",
  data: {
    batchId: 43,
    platformName: "Shopee",
    platformCode: "shopee",
    fileName: "AffiliateCommissionReport202510290813.csv",
    status: "PARTIAL",
    totalRows: 2000,
    successCount: 1200,
    failedCount: 500,
    skippedCount: 300,
    matchedCount: 1000,
    successRate: 60.0,
    errorMessage: null,
    errors: [
      {
        rowNumber: 10,
        orderId: null,
        error: "Missing order ID",
        rawData: ",Đang chờ xử lý,..."
      },
      {
        rowNumber: 25,
        orderId: "251029ABC123",
        error: "No tracking code in Sub_id1",
        rawData: "251029ABC123,Đang chờ xử lý,..."
      },
      // ... more errors
    ],
    startedAt: new Date(Date.now() - 180000).toISOString(),
    completedAt: new Date().toISOString(),
    durationSeconds: 180,
    importedBy: 5
  },
  timestamp: new Date().toISOString()
};
```

### **Mock Service Example**

```typescript
// mock-api-service.ts
const MOCK_DELAY = 1000; // 1 second delay to simulate API

class MockApiService {
  async createTrackingLink(request: CreateTrackingLinkRequest): Promise<TrackingLinkResponse> {
    await this.delay(MOCK_DELAY);

    // Simulate error for invalid URL
    if (!request.shopeeUrl.includes('shopee.vn')) {
      throw new Error('Invalid Shopee URL format');
    }

    // Generate mock response
    const clickId = Math.floor(Math.random() * 10000) + 1;
    const timestamp = new Date().toISOString().replace(/[-:]/g, '').slice(0, 14);

    return {
      clickId,
      trackingUrl: `https://shopee.vn/universal-link/123456?pid=cashbee_vn_123&af_sub1=CB${request.userId}_${clickId}_${timestamp}`,
      trackingCode: `CB${request.userId}_${clickId}_${timestamp}`,
      originalUrl: request.shopeeUrl,
      productName: "Sample Product",
      shopId: "123456",
      itemId: "789012",
      platformName: "Shopee",
      platformCode: "shopee",
      estimatedCashbackRate: 5.00,
      createdAt: new Date().toISOString(),
      message: "Click this link to shop on Shopee and earn cashback!"
    };
  }

  async importOrders(file: File, adminUserId: number): Promise<ImportOrdersResponse> {
    await this.delay(3000); // Longer delay for import

    // Simulate random success rate
    const totalRows = 2000;
    const successRate = 0.85 + Math.random() * 0.15; // 85-100%
    const successCount = Math.floor(totalRows * successRate);
    const failedCount = Math.floor((totalRows - successCount) * 0.3);
    const skippedCount = totalRows - successCount - failedCount;
    const matchedCount = Math.floor(successCount * 0.9);

    return {
      batchId: Math.floor(Math.random() * 1000) + 1,
      platformName: "Shopee",
      platformCode: "shopee",
      fileName: file.name,
      status: successRate > 0.95 ? "COMPLETED" : "PARTIAL",
      totalRows,
      successCount,
      failedCount,
      skippedCount,
      matchedCount,
      successRate: successRate * 100,
      errorMessage: null,
      errors: this.generateMockErrors(failedCount),
      startedAt: new Date(Date.now() - 150000).toISOString(),
      completedAt: new Date().toISOString(),
      durationSeconds: 150,
      importedBy: adminUserId
    };
  }

  private generateMockErrors(count: number): ImportErrorDetail[] {
    const errors: ImportErrorDetail[] = [];
    const errorTypes = [
      "Duplicate order ID",
      "Invalid commission amount",
      "Missing order ID",
      "No tracking code",
      "Invalid date format"
    ];

    for (let i = 0; i < Math.min(count, 10); i++) {
      errors.push({
        rowNumber: Math.floor(Math.random() * 2000) + 1,
        orderId: Math.random() > 0.5 ? `25102${Math.random().toString(36).substr(2, 9).toUpperCase()}` : null,
        error: errorTypes[Math.floor(Math.random() * errorTypes.length)],
        rawData: "CSV data truncated..."
      });
    }

    return errors;
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

export const mockApiService = new MockApiService();
```

---

## Testing Scenarios

### **Scenario 1: Happy Path - Create Tracking Link**

1. User logs in and navigates to "Create Link" page
2. User pastes valid Shopee URL: `https://shopee.vn/product/47305935/20317610036`
3. System displays loading spinner
4. System returns tracking link with 5% cashback rate
5. User copies tracking link
6. User clicks tracking link → redirects to Shopee

**Expected Results:**
- Link created with clickId
- Tracking URL contains user's tracking code
- Click tracked in database
- Redirect to Shopee with affiliate parameters

### **Scenario 2: Error Handling - Invalid URL**

1. User pastes invalid URL: `https://google.com`
2. System validates URL
3. System shows error: "Please enter a valid Shopee product URL"
4. User corrects URL and retries

**Expected Results:**
- Error message displayed
- No API call made (client-side validation)
- User can retry

### **Scenario 3: Happy Path - Import Orders (Admin)**

1. Admin downloads CSV from Shopee portal
2. Admin navigates to "Import Orders" page
3. Admin selects CSV file (2000 rows)
4. Admin clicks "Import"
5. System shows progress spinner
6. Import completes after ~2 minutes
7. System shows statistics:
   - Success: 1850 (92.5%)
   - Failed: 50
   - Skipped: 100
   - Matched: 1700

**Expected Results:**
- All orders imported
- Orders matched with clicks
- Users receive cashback notifications
- Import batch saved for audit

### **Scenario 4: Edge Case - Duplicate Orders**

1. Admin uploads same CSV twice
2. First import: 1850 successful
3. Second import: 1850 skipped (duplicates)
4. System shows "1850 orders skipped"

**Expected Results:**
- No duplicate orders created
- Skip count equals total rows
- No errors thrown

### **Scenario 5: Error Handling - Malformed CSV**

1. Admin uploads corrupted CSV
2. System attempts to parse
3. Parse fails with error
4. System shows error: "Failed to parse CSV: Invalid format"

**Expected Results:**
- Import batch marked as FAILED
- Error message returned
- No partial data imported

---

## 📞 Support

For questions or issues:

- **Technical Issues**: Open GitHub issue
- **API Questions**: Contact backend team
- **Documentation Updates**: Submit pull request

---

## 📝 Changelog

### Version 1.0.0 (2025-11-01)
- Initial release
- Create tracking link endpoint
- Click redirect endpoint
- Import orders endpoint
- Get import batch endpoint (placeholder)

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-01
**Author:** CashBee Backend Team
**Status:** ✅ Ready for Frontend Development
