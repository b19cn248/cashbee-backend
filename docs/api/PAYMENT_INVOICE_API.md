# Payment Invoice API Documentation

## Overview

API endpoints for managing payment invoices (receipts for batch transfer payments).

**Base URL:** `/api/invoices`

**Authentication:** Required (Bearer Token)

---

## Endpoints

### 1. Get User's Invoices (Paginated)

Get a paginated list of payment invoices for the current authenticated user.

**Endpoint:** `GET /api/invoices/me`

**Authentication:** Required

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | int | No | 0 | Page number (0-based) |
| `size` | int | No | 10 | Page size (max 50) |

**Response:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": 1,
        "invoiceNumber": "INV-20260104-00001",
        "amount": 150000,
        "currency": "VND",
        "transferTime": "2026-01-04T10:30:00",
        "createdAt": "2026-01-04T10:30:05",
        "totalOrders": 5,
        "transferStatus": "SUCCESS"
      },
      {
        "id": 2,
        "invoiceNumber": "INV-20260103-00015",
        "amount": 85000,
        "currency": "VND",
        "transferTime": "2026-01-03T14:15:00",
        "createdAt": "2026-01-03T14:15:10",
        "totalOrders": 3,
        "transferStatus": "SUCCESS"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

**Frontend Usage Example:**

```javascript
// React Native / JavaScript
const getInvoices = async (page = 0, size = 10) => {
  const response = await fetch(`${API_BASE}/api/invoices/me?page=${page}&size=${size}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  const result = await response.json();

  if (result.success) {
    // result.data.content - array of invoice summaries
    // result.data.totalElements - total number of invoices
    // result.data.totalPages - total number of pages
    return result.data;
  }

  throw new Error(result.message);
};
```

---

### 2. Get Invoice Detail by ID

Get full details of a specific invoice including platform breakdown.

**Endpoint:** `GET /api/invoices/{invoiceId}`

**Authentication:** Required (Owner only)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoiceId` | Long | Yes | Invoice ID |

**Response:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "invoiceNumber": "INV-20260104-00001",
    "batchId": 123,
    "batchItemId": 456,
    "userId": 789,
    "amount": 150000,
    "currency": "VND",
    "bankName": "Vietcombank",
    "accountNumber": "****5678",
    "transferTime": "2026-01-04T10:30:00",
    "transferStatus": "SUCCESS",
    "totalOrders": 5,
    "shopeeAmount": 80000,
    "shopeeOrders": 3,
    "lazadaAmount": 50000,
    "lazadaOrders": 1,
    "tikiAmount": 0,
    "tikiOrders": 0,
    "tiktokAmount": 20000,
    "tiktokOrders": 1,
    "otherAmount": 0,
    "otherOrders": 0,
    "emailSent": true,
    "emailSentAt": "2026-01-04T10:31:00",
    "createdAt": "2026-01-04T10:30:05"
  }
}
```

**Frontend Usage Example:**

```javascript
const getInvoiceDetail = async (invoiceId) => {
  const response = await fetch(`${API_BASE}/api/invoices/${invoiceId}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  const result = await response.json();

  if (result.success) {
    return result.data;
  }

  throw new Error(result.message);
};
```

---

### 3. Get Invoice by Invoice Number

Alternative endpoint to get invoice by human-readable invoice number.

**Endpoint:** `GET /api/invoices/by-number/{invoiceNumber}`

**Authentication:** Required (Owner only)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoiceNumber` | String | Yes | Invoice number (e.g., "INV-20260104-00001") |

**Response:** Same as "Get Invoice Detail by ID"

**Frontend Usage Example:**

```javascript
const getInvoiceByNumber = async (invoiceNumber) => {
  const response = await fetch(`${API_BASE}/api/invoices/by-number/${invoiceNumber}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  const result = await response.json();

  if (result.success) {
    return result.data;
  }

  throw new Error(result.message);
};
```

---

## Data Models

### InvoiceSummaryResponse (List View)

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Invoice ID |
| `invoiceNumber` | String | Human-readable invoice number (e.g., "INV-20260104-00001") |
| `amount` | BigDecimal | Total transfer amount |
| `currency` | String | Currency code (default: "VND") |
| `transferTime` | LocalDateTime | When the transfer was completed |
| `createdAt` | LocalDateTime | When the invoice was created |
| `totalOrders` | Integer | Total number of orders in this payment |
| `transferStatus` | String | Transfer status (SUCCESS, FAILED, PENDING) |

### PaymentInvoiceResponse (Detail View)

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Invoice ID |
| `invoiceNumber` | String | Human-readable invoice number |
| `batchId` | Long | Batch transfer ID |
| `batchItemId` | Long | Batch item ID |
| `userId` | Long | User ID |
| `amount` | BigDecimal | Total transfer amount |
| `currency` | String | Currency code |
| `bankName` | String | Bank name |
| `accountNumber` | String | Masked bank account (e.g., "****5678") |
| `transferTime` | LocalDateTime | When the transfer was completed |
| `transferStatus` | String | Transfer status |
| `totalOrders` | Integer | Total number of orders |
| **Platform Breakdown** | | |
| `shopeeAmount` | BigDecimal | Total cashback from Shopee orders |
| `shopeeOrders` | Integer | Number of Shopee orders |
| `lazadaAmount` | BigDecimal | Total cashback from Lazada orders |
| `lazadaOrders` | Integer | Number of Lazada orders |
| `tikiAmount` | BigDecimal | Total cashback from Tiki orders |
| `tikiOrders` | Integer | Number of Tiki orders |
| `tiktokAmount` | BigDecimal | Total cashback from TikTok orders |
| `tiktokOrders` | Integer | Number of TikTok orders |
| `otherAmount` | BigDecimal | Total cashback from other platforms |
| `otherOrders` | Integer | Number of other platform orders |
| **Email Status** | | |
| `emailSent` | Boolean | Whether email was sent |
| `emailSentAt` | LocalDateTime | When email was sent (null if not sent) |
| `createdAt` | LocalDateTime | Invoice creation time |

---

## Error Responses

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```

### 403 Forbidden (Access Denied)

```json
{
  "success": false,
  "message": "Access denied to invoice: 123",
  "data": null
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Invoice not found: 123",
  "data": null
}
```

---

## UI/UX Recommendations

### Invoice List Screen

1. Display invoices in a scrollable list with:
   - Invoice number (e.g., "INV-20260104-00001")
   - Amount formatted with currency (e.g., "150,000 VND")
   - Transfer date (e.g., "04/01/2026 10:30")
   - Order count (e.g., "5 orders")

2. Support pull-to-refresh and infinite scroll pagination

3. Tap on invoice to navigate to detail screen

### Invoice Detail Screen

1. Header section:
   - Invoice number (prominent display)
   - Total amount (large, highlighted)
   - Transfer date and status

2. Bank information:
   - Bank name
   - Masked account number

3. Platform breakdown (pie chart or list):
   - Shopee: amount + order count
   - Lazada: amount + order count
   - Tiki: amount + order count
   - TikTok: amount + order count
   - Other: amount + order count

4. Actions:
   - Share invoice
   - Download as PDF (future feature)

---

## Invoice Number Format

Format: `INV-YYYYMMDD-XXXXX`

- `INV` - Prefix
- `YYYYMMDD` - Date (e.g., 20260104)
- `XXXXX` - 5-digit sequence number (e.g., 00001)

Example: `INV-20260104-00001`

---

## Notes

1. **Ownership Validation:** Users can only access their own invoices. Attempting to access another user's invoice will return 403 Forbidden.

2. **Automatic Generation:** Invoices are automatically generated when batch transfers complete successfully.

3. **Email Notifications:** If user has enabled payment invoice emails in notification preferences, they will receive an email with invoice details.

4. **Account Number Masking:** For security, bank account numbers are masked (showing only last 4 digits) in API responses.

5. **Pagination:** Maximum page size is 50 items. Requests with size > 50 will be automatically limited to 50.
