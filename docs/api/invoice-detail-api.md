# API Hoá Đơn Chi Tiết (Invoice Detail API)

> **Version:** 1.1.0
> **Last Updated:** 2026-01-30
> **Author:** CashBee Backend Team

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Authentication](#2-authentication)
3. [API Endpoints](#3-api-endpoints)
4. [Response Schema](#4-response-schema)
5. [Ví dụ Response](#5-ví-dụ-response)
6. [Gợi ý UI/UX](#6-gợi-ý-uiux)
7. [Error Handling](#7-error-handling)

---

## 1. Tổng quan

API này cung cấp thông tin chi tiết về hoá đơn thanh toán theo kiểu **"hoá đơn siêu thị"** - hiển thị từng đơn hàng và sản phẩm đã góp phần vào tổng tiền hoàn.

### Các thành phần chính của hoá đơn:

| Thành phần | Mô tả |
|------------|-------|
| **Invoice Metadata** | Thông tin cơ bản: số hoá đơn, trạng thái, thời gian |
| **Summary** | Tổng kết: số đơn, số sản phẩm, tổng tiền, tỷ lệ hoàn |
| **Bank Info** | Thông tin tài khoản ngân hàng nhận tiền |
| **Orders** | Danh sách đơn hàng với chi tiết từng sản phẩm |
| **Bonus Breakdown** | Phân tích thưởng: mốc thưởng + hoa hồng giới thiệu |
| **Calculation** | Giải thích cách tính tiền hoàn |

---

## 2. Authentication

API yêu cầu JWT Bearer Token.

```http
Authorization: Bearer <jwt_token>
```

---

## 3. API Endpoints

### 3.1. Lấy chi tiết hoá đơn với breakdown đơn hàng

```http
GET /api/invoices/{invoiceId}/details
```

#### Request

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoiceId` | Long | ✅ | ID của hoá đơn |

#### cURL Example

```bash
curl --location 'https://api.cashbee.vn/api/invoices/58/details' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...'
```

#### Response

```json
{
    "status": "success",
    "data": { ... },  // InvoiceDetailResponse
    "message": null
}
```

---

## 4. Response Schema

### 4.1. InvoiceDetailResponse (Root Object)

```typescript
interface InvoiceDetailResponse {
    // ===== Invoice Metadata =====
    id: number;                      // ID hoá đơn
    invoiceNumber: string;           // Số hoá đơn (VD: "INV-20260130-00012")
    transferTime: string;            // Thời gian chuyển khoản (ISO 8601)
    transferStatus: string;          // Trạng thái: "COMPLETED", "PENDING", "FAILED"
    createdAt: string;               // Thời gian tạo hoá đơn (ISO 8601)

    // ===== Sections =====
    summary: InvoiceSummaryDetail;   // Tổng kết
    bankInfo: BankInfo;              // Thông tin ngân hàng
    orders: OrderDetail[];           // Danh sách đơn hàng
    bonusBreakdown: BonusBreakdown;  // Phân tích thưởng
    calculation: CalculationInfo;    // Giải thích cách tính
}
```

### 4.2. InvoiceSummaryDetail

```typescript
interface InvoiceSummaryDetail {
    totalOrders: number;           // Tổng số đơn hàng
    totalItems: number;            // Tổng số sản phẩm
    totalProductAmount: number;    // Tổng giá trị sản phẩm (VND)
    totalCommission: number;       // Tổng hoa hồng từ sàn (VND)
    cashbackRate: number;          // Tỷ lệ hoàn cho user (VD: 80.00 = 80%)
    platformFeeRate: number;       // Phí nền tảng (VD: 20.00 = 20%)
    totalCashback: number;         // Tổng tiền hoàn (VND)
    currency: string;              // Đơn vị tiền tệ (mặc định: "VND")
}
```

### 4.3. BankInfo

```typescript
interface BankInfo {
    bankName: string;       // Mã ngân hàng (VD: "VCB", "TCB")
    bankFullName: string;   // Tên đầy đủ (VD: "Vietcombank")
    accountNumber: string;  // Số tài khoản (đã che: "****1234")
    accountName: string;    // Tên chủ tài khoản
}
```

### 4.4. OrderDetail

```typescript
interface OrderDetail {
    orderCode: string;              // Mã đơn hàng (VD: "251228J18KS4X6")
    orderTime: string;              // Thời gian đặt hàng (ISO 8601)
    platformName: string;           // Tên sàn (VD: "Shopee", "Lazada")
    items: OrderItemDetail[];       // Danh sách sản phẩm
    totals: OrderTotals;            // Tổng của đơn hàng này
}
```

### 4.5. OrderItemDetail

```typescript
interface OrderItemDetail {
    itemName: string;           // Tên sản phẩm
    shopName: string;           // Tên shop/seller
    quantity: number;           // Số lượng
    productPrice: number;       // Giá sản phẩm (VND)
    commission: number;         // Hoa hồng từ sàn (VND)
    cashbackRate: number;       // Tỷ lệ hoàn (VD: 80.00)
    cashbackAmount: number;     // Tiền hoàn (VND)
    imageUrl: string | null;    // URL hình ảnh (có thể null)
    category: string | null;    // Danh mục (có thể null)
}
```

### 4.6. OrderTotals

```typescript
interface OrderTotals {
    productAmount: number;    // Tổng giá sản phẩm của đơn
    commission: number;       // Tổng hoa hồng của đơn
    cashbackAmount: number;   // Tổng tiền hoàn của đơn
}
```

### 4.7. BonusBreakdown ⭐ NEW

```typescript
interface BonusBreakdown {
    totalBonusAmount: number;           // Tổng thưởng (milestone + referrer)
    milestoneBonus: BonusDetail;        // Chi tiết thưởng mốc
    referrerCommission: BonusDetail;    // Chi tiết hoa hồng giới thiệu
}

interface BonusDetail {
    label: string;         // Nhãn hiển thị
    count: number;         // Số lượng (mốc/đơn)
    amount: number;        // Tổng tiền (VND)
    description: string;   // Mô tả chi tiết
}
```

#### Giá trị có thể của BonusDetail:

| Field | Milestone Bonus | Referrer Commission |
|-------|-----------------|---------------------|
| `label` | "Thưởng mốc" | "Hoa hồng giới thiệu" |
| `count` | Số mốc đạt được | Số đơn hàng của người được giới thiệu |
| `amount` | Tổng tiền thưởng mốc | Tổng hoa hồng 5% |
| `description` | "Đạt 2 mốc thưởng: 1 đơn, 5 đơn" | "Hoa hồng từ 3 đơn hàng của người được giới thiệu (5% hoa hồng gốc)" |

### 4.8. CalculationInfo

```typescript
interface CalculationInfo {
    description: string;   // Mô tả cách tính
    formula: string;       // Công thức
    note: string;          // Ghi chú thêm
}
```

---

## 5. Ví dụ Response

### 5.1. Response đầy đủ

```json
{
    "status": "success",
    "data": {
        "id": 58,
        "invoiceNumber": "INV-20260130-00012",
        "transferTime": "2026-01-30T14:30:00",
        "transferStatus": "COMPLETED",
        "createdAt": "2026-01-30T14:30:00",

        "summary": {
            "totalOrders": 5,
            "totalItems": 12,
            "totalProductAmount": 2500000.00,
            "totalCommission": 67500.00,
            "cashbackRate": 80.00,
            "platformFeeRate": 20.00,
            "totalCashback": 54000.00,
            "currency": "VND"
        },

        "bankInfo": {
            "bankName": "VCB",
            "bankFullName": "Vietcombank",
            "accountNumber": "****5678",
            "accountName": "NGUYEN VAN A"
        },

        "orders": [
            {
                "orderCode": "251228J18KS4X6",
                "orderTime": "2025-12-28T10:30:00",
                "platformName": "Shopee",
                "items": [
                    {
                        "itemName": "Áo thun nam cotton",
                        "shopName": "Fashion Store",
                        "quantity": 2,
                        "productPrice": 250000.00,
                        "commission": 6750.00,
                        "cashbackRate": 80.00,
                        "cashbackAmount": 5400.00,
                        "imageUrl": "https://cf.shopee.vn/file/...",
                        "category": "Thời trang nam"
                    },
                    {
                        "itemName": "Quần jean nam",
                        "shopName": "Fashion Store",
                        "quantity": 1,
                        "productPrice": 350000.00,
                        "commission": 9450.00,
                        "cashbackRate": 80.00,
                        "cashbackAmount": 7560.00,
                        "imageUrl": "https://cf.shopee.vn/file/...",
                        "category": "Thời trang nam"
                    }
                ],
                "totals": {
                    "productAmount": 600000.00,
                    "commission": 16200.00,
                    "cashbackAmount": 12960.00
                }
            }
        ],

        "bonusBreakdown": {
            "totalBonusAmount": 25795.00,
            "milestoneBonus": {
                "label": "Thưởng mốc",
                "count": 2,
                "amount": 25000.00,
                "description": "Đạt 2 mốc thưởng: 1 đơn, 5 đơn"
            },
            "referrerCommission": {
                "label": "Hoa hồng giới thiệu",
                "count": 1,
                "amount": 795.00,
                "description": "Hoa hồng từ 1 đơn hàng của người được giới thiệu (5% hoa hồng gốc)"
            }
        },

        "calculation": {
            "description": "Tiền hoàn được tính dựa trên hoa hồng từ các nền tảng thương mại điện tử",
            "formula": "Tiền hoàn = Hoa hồng × Tỷ lệ hoàn (thường là 80%)",
            "note": "Tỷ lệ hoàn có thể khác nhau tùy theo sản phẩm và chương trình khuyến mãi"
        }
    },
    "message": null
}
```

### 5.2. Response khi không có bonus

```json
{
    "bonusBreakdown": {
        "totalBonusAmount": 0.00,
        "milestoneBonus": {
            "label": "Thưởng mốc",
            "count": 0,
            "amount": 0.00,
            "description": "Chưa có thưởng mốc trong đợt thanh toán này"
        },
        "referrerCommission": {
            "label": "Hoa hồng giới thiệu",
            "count": 0,
            "amount": 0.00,
            "description": "Chưa có hoa hồng giới thiệu trong đợt thanh toán này"
        }
    }
}
```

---

## 6. Gợi ý UI/UX

### 6.1. Layout tổng thể

```
┌─────────────────────────────────────────────┐
│  📄 HOÁ ĐƠN THANH TOÁN                      │
│  INV-20260130-00012                         │
│  Ngày: 30/01/2026 14:30                     │
├─────────────────────────────────────────────┤
│  📊 TỔNG KẾT                                │
│  ┌─────────────┬─────────────┐              │
│  │ 5 đơn hàng  │ 12 sản phẩm │              │
│  └─────────────┴─────────────┘              │
│  Tổng giá trị:     2,500,000đ               │
│  Hoa hồng sàn:        67,500đ               │
│  Tỷ lệ hoàn:             80%                │
│  ─────────────────────────────              │
│  💰 TIỀN HOÀN:       54,000đ                │
├─────────────────────────────────────────────┤
│  🎁 THƯỞNG & HOA HỒNG                       │
│  ┌───────────────────────────────────────┐  │
│  │ 🏆 Thưởng mốc           25,000đ       │  │
│  │    Đạt 2 mốc: 1 đơn, 5 đơn            │  │
│  ├───────────────────────────────────────┤  │
│  │ 👥 Hoa hồng giới thiệu     795đ       │  │
│  │    1 đơn từ người được giới thiệu     │  │
│  └───────────────────────────────────────┘  │
│  Tổng thưởng:            25,795đ            │
├─────────────────────────────────────────────┤
│  🏦 CHUYỂN VÀO TÀI KHOẢN                    │
│  Vietcombank - ****5678                     │
│  NGUYEN VAN A                               │
├─────────────────────────────────────────────┤
│  📦 CHI TIẾT ĐƠN HÀNG                       │
│                                             │
│  ▼ Đơn #251228J18KS4X6 - Shopee            │
│    28/12/2025 10:30                         │
│    ┌─────────────────────────────────────┐  │
│    │ [IMG] Áo thun nam cotton            │  │
│    │       Fashion Store                 │  │
│    │       SL: 2 × 125,000đ = 250,000đ   │  │
│    │       Hoàn: 5,400đ (80%)            │  │
│    ├─────────────────────────────────────┤  │
│    │ [IMG] Quần jean nam                 │  │
│    │       Fashion Store                 │  │
│    │       SL: 1 × 350,000đ              │  │
│    │       Hoàn: 7,560đ (80%)            │  │
│    └─────────────────────────────────────┘  │
│    Tổng đơn: 12,960đ                        │
│                                             │
│  ▼ Đơn #251229ABC123 - Lazada              │
│    ...                                      │
├─────────────────────────────────────────────┤
│  ════════════════════════════════════════   │
│  💵 TỔNG THANH TOÁN                         │
│     Tiền hoàn:           54,000đ            │
│     Thưởng & hoa hồng:   25,795đ            │
│     ─────────────────────────────           │
│     TỔNG CỘNG:           79,795đ            │
│  ════════════════════════════════════════   │
├─────────────────────────────────────────────┤
│  ℹ️ CÁCH TÍNH                               │
│  Tiền hoàn = Hoa hồng × 80%                 │
│  Tỷ lệ có thể khác nhau tùy sản phẩm        │
└─────────────────────────────────────────────┘
```

### 6.2. Logic hiển thị Bonus

```typescript
// Kiểm tra có bonus không
const hasBonus = bonusBreakdown.totalBonusAmount > 0;

// Hiển thị section bonus nếu có
if (hasBonus) {
    // Hiển thị milestone nếu count > 0
    if (bonusBreakdown.milestoneBonus.count > 0) {
        // Show milestone section
    }

    // Hiển thị referrer commission nếu count > 0
    if (bonusBreakdown.referrerCommission.count > 0) {
        // Show referrer commission section
    }
}
```

### 6.3. Format số tiền

```typescript
function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(amount) + 'đ';
}

// Ví dụ: 54000 -> "54,000đ"
```

### 6.4. Format thời gian

```typescript
function formatDateTime(isoString: string): string {
    const date = new Date(isoString);
    return date.toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Ví dụ: "2026-01-30T14:30:00" -> "30/01/2026 14:30"
```

### 6.5. Màu sắc gợi ý

| Element | Color | Hex |
|---------|-------|-----|
| Tiền hoàn (positive) | Green | `#22C55E` |
| Thưởng mốc | Gold | `#F59E0B` |
| Hoa hồng giới thiệu | Blue | `#3B82F6` |
| Platform: Shopee | Orange | `#EE4D2D` |
| Platform: Lazada | Blue | `#0F146D` |
| Platform: Tiki | Blue | `#1A94FF` |
| Platform: TikTok | Black | `#000000` |

---

## 7. Error Handling

### 7.1. Error Response Format

```json
{
    "status": "error",
    "data": null,
    "message": "Error message here",
    "errorCode": "ERROR_CODE"
}
```

### 7.2. Các mã lỗi

| HTTP Code | Error Code | Message | Xử lý |
|-----------|------------|---------|-------|
| 401 | `UNAUTHORIZED` | Token không hợp lệ | Redirect đến login |
| 403 | `FORBIDDEN` | Không có quyền truy cập hoá đơn này | Hiển thị thông báo lỗi |
| 404 | `INVOICE_NOT_FOUND` | Hoá đơn không tồn tại | Hiển thị "Không tìm thấy hoá đơn" |
| 500 | `INTERNAL_ERROR` | Lỗi server | Hiển thị "Có lỗi xảy ra, vui lòng thử lại" |

### 7.3. Ví dụ xử lý lỗi (React Native)

```typescript
try {
    const response = await api.get(`/invoices/${invoiceId}/details`);
    setInvoiceDetail(response.data.data);
} catch (error) {
    if (error.response?.status === 401) {
        navigation.navigate('Login');
    } else if (error.response?.status === 404) {
        Alert.alert('Lỗi', 'Không tìm thấy hoá đơn');
    } else {
        Alert.alert('Lỗi', 'Có lỗi xảy ra, vui lòng thử lại');
    }
}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.1.0 | 2026-01-30 | Thêm `bonusBreakdown` với `milestoneBonus` và `referrerCommission` |
| 1.0.0 | 2026-01-15 | Initial release |

---

## Liên hệ

Nếu có thắc mắc về API, vui lòng liên hệ Backend Team qua:
- Slack: #cashbee-backend
- Email: backend@cashbee.vn
