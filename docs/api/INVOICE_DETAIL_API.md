# API Chi Tiết Hóa Đơn (Invoice Detail API)

## Tổng quan

API này cung cấp thông tin chi tiết về hóa đơn thanh toán cashback, hiển thị theo kiểu "hóa đơn siêu thị" với đầy đủ thông tin từng đơn hàng và sản phẩm.

**Endpoint:** `GET /api/invoices/{invoiceId}/details`

**Authentication:** Bearer Token (JWT)

---

## Request

### HTTP Method
```
GET
```

### URL
```
/api/invoices/{invoiceId}/details
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoiceId` | Long | Yes | ID của hóa đơn cần xem chi tiết |

### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <JWT_TOKEN>` | Yes |
| `Content-Type` | `application/json` | No |

### Example Request

```bash
curl -X GET "https://api.cashbee.vn/api/invoices/1/details" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Response

### Success Response (HTTP 200)

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "invoiceNumber": "INV-20260114-00001",
    "transferTime": "2026-01-14T10:30:00",
    "transferStatus": "SUCCESS",
    "createdAt": "2026-01-14T10:30:00",
    "summary": {
      "totalOrders": 7,
      "totalItems": 7,
      "totalProductAmount": 2410200.00,
      "totalCommission": 348873.25,
      "cashbackRate": 72.00,
      "platformFeeRate": 28.00,
      "totalCashback": 251188.74,
      "currency": "VND"
    },
    "bankInfo": {
      "bankName": "Vietcombank",
      "bankFullName": "Ngân hàng TMCP Ngoại thương Việt Nam",
      "accountNumber": "****1234",
      "accountName": "NGUYEN VAN A"
    },
    "orders": [
      {
        "orderCode": "260110MY3RKCBT",
        "orderTime": "2026-01-10T12:03:27",
        "platformName": "Shopee",
        "items": [
          {
            "itemName": "Sữa lúa mạch Nestlé® MILO® thùng 12 x 110ml",
            "shopName": "Nestlé Chính hãng",
            "quantity": 3,
            "productPrice": 248000.00,
            "commission": 47430.00,
            "cashbackRate": 72.00,
            "cashbackAmount": 34149.60,
            "imageUrl": null,
            "category": "Thực phẩm và đồ uống"
          }
        ],
        "totals": {
          "productAmount": 248000.00,
          "commission": 47430.00,
          "cashbackAmount": 34149.60
        }
      }
    ],
    "calculation": {
      "description": "Tiền hoàn được tính dựa trên hoa hồng từ các nền tảng thương mại điện tử",
      "formula": "Tiền hoàn = Hoa hồng × Tỷ lệ hoàn (thường là 80%)",
      "note": "Tỷ lệ hoàn có thể khác nhau tùy theo sản phẩm và chương trình khuyến mãi"
    }
  },
  "timestamp": "2026-01-14T15:30:00"
}
```

### Error Responses

#### 401 Unauthorized
```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null,
  "timestamp": "2026-01-14T15:30:00"
}
```

#### 404 Not Found
```json
{
  "success": false,
  "message": "Invoice not found: 999",
  "data": null,
  "timestamp": "2026-01-14T15:30:00"
}
```

---

## Data Models

### InvoiceDetailResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | ID hóa đơn |
| `invoiceNumber` | String | No | Mã hóa đơn (VD: INV-20260114-00001) |
| `transferTime` | DateTime | Yes | Thời gian chuyển khoản |
| `transferStatus` | String | No | Trạng thái: `SUCCESS`, `FAILED`, `PARTIAL` |
| `createdAt` | DateTime | No | Thời gian tạo hóa đơn |
| `summary` | InvoiceSummaryDetail | No | Thông tin tổng hợp |
| `bankInfo` | BankInfo | No | Thông tin tài khoản ngân hàng |
| `orders` | Array[OrderDetail] | No | Danh sách đơn hàng |
| `calculation` | CalculationInfo | No | Giải thích cách tính |

### InvoiceSummaryDetail

| Field | Type | Description |
|-------|------|-------------|
| `totalOrders` | Integer | Tổng số đơn hàng |
| `totalItems` | Integer | Tổng số sản phẩm |
| `totalProductAmount` | BigDecimal | Tổng giá trị sản phẩm (VND) |
| `totalCommission` | BigDecimal | Tổng hoa hồng từ platform (VND) |
| `cashbackRate` | BigDecimal | Tỷ lệ hoàn tiền trung bình (%) |
| `platformFeeRate` | BigDecimal | Tỷ lệ phí nền tảng (%) |
| `totalCashback` | BigDecimal | Tổng tiền hoàn (VND) |
| `currency` | String | Đơn vị tiền tệ (mặc định: "VND") |

### BankInfo

| Field | Type | Description |
|-------|------|-------------|
| `bankName` | String | Tên ngắn ngân hàng (VD: Vietcombank) |
| `bankFullName` | String | Tên đầy đủ ngân hàng |
| `accountNumber` | String | Số tài khoản (đã ẩn: ****1234) |
| `accountName` | String | Tên chủ tài khoản |

### OrderDetail

| Field | Type | Description |
|-------|------|-------------|
| `orderCode` | String | Mã đơn hàng từ platform (VD: 260110MY3RKCBT) |
| `orderTime` | DateTime | Thời gian đặt hàng |
| `platformName` | String | Tên nền tảng (Shopee, Lazada, Tiki, TikTok) |
| `items` | Array[OrderItemDetail] | Danh sách sản phẩm trong đơn |
| `totals` | OrderTotals | Tổng cộng của đơn hàng |

### OrderItemDetail

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `itemName` | String | No | Tên sản phẩm |
| `shopName` | String | Yes | Tên shop/người bán |
| `quantity` | Integer | Yes | Số lượng mua |
| `productPrice` | BigDecimal | Yes | Giá sản phẩm (VND) |
| `commission` | BigDecimal | Yes | Hoa hồng từ sản phẩm này (VND) |
| `cashbackRate` | BigDecimal | Yes | Tỷ lệ hoàn tiền (%) |
| `cashbackAmount` | BigDecimal | Yes | Tiền hoàn cho sản phẩm này (VND) |
| `imageUrl` | String | Yes | URL hình ảnh sản phẩm |
| `category` | String | Yes | Danh mục sản phẩm |

### OrderTotals

| Field | Type | Description |
|-------|------|-------------|
| `productAmount` | BigDecimal | Tổng giá trị sản phẩm của đơn (VND) |
| `commission` | BigDecimal | Tổng hoa hồng của đơn (VND) |
| `cashbackAmount` | BigDecimal | Tổng tiền hoàn của đơn (VND) |

### CalculationInfo

| Field | Type | Description |
|-------|------|-------------|
| `description` | String | Mô tả cách tính |
| `formula` | String | Công thức tính |
| `note` | String | Ghi chú thêm |

---

## Hướng dẫn tích hợp Frontend

### 1. Gọi API

```typescript
// TypeScript/React Native example
interface InvoiceDetailResponse {
  id: number;
  invoiceNumber: string;
  transferTime: string;
  transferStatus: 'SUCCESS' | 'FAILED' | 'PARTIAL';
  createdAt: string;
  summary: InvoiceSummaryDetail;
  bankInfo: BankInfo;
  orders: OrderDetail[];
  calculation: CalculationInfo;
}

async function getInvoiceDetails(invoiceId: number): Promise<InvoiceDetailResponse> {
  const response = await fetch(`${API_BASE_URL}/api/invoices/${invoiceId}/details`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch invoice details');
  }

  const result = await response.json();
  return result.data;
}
```

### 2. Xử lý Loading State

```typescript
const [loading, setLoading] = useState(true);
const [invoice, setInvoice] = useState<InvoiceDetailResponse | null>(null);
const [error, setError] = useState<string | null>(null);

useEffect(() => {
  async function loadInvoice() {
    try {
      setLoading(true);
      const data = await getInvoiceDetails(invoiceId);
      setInvoice(data);
    } catch (err) {
      setError('Không thể tải chi tiết hóa đơn');
    } finally {
      setLoading(false);
    }
  }
  loadInvoice();
}, [invoiceId]);
```

### 3. Format số tiền

```typescript
function formatCurrency(amount: number, currency: string = 'VND'): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

// Sử dụng
formatCurrency(251188.74); // "251.189 ₫"
```

### 4. Format ngày giờ

```typescript
function formatDateTime(dateString: string): string {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

// Sử dụng
formatDateTime('2026-01-10T12:03:27'); // "10/01/2026, 12:03"
```

---

## Gợi ý thiết kế UI

### Layout màn hình

```
┌─────────────────────────────────────┐
│  ← Chi tiết hóa đơn                 │
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │ INV-20260114-00001              ││
│  │ 14/01/2026, 10:30               ││
│  │ ✓ Đã thanh toán                 ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│  TỔNG QUAN                          │
│  ┌─────────────────────────────────┐│
│  │ Tổng tiền hoàn     251.189 ₫   ││
│  │ Số đơn hàng        7           ││
│  │ Số sản phẩm        7           ││
│  │ Tỷ lệ hoàn         72%         ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│  THÔNG TIN CHUYỂN KHOẢN             │
│  ┌─────────────────────────────────┐│
│  │ 🏦 Vietcombank                  ││
│  │    ****1234                     ││
│  │    NGUYEN VAN A                 ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│  CHI TIẾT ĐƠN HÀNG (7)              │
│  ┌─────────────────────────────────┐│
│  │ 🛒 260110MY3RKCBT               ││
│  │    Shopee • 10/01/2026          ││
│  │ ┌───────────────────────────────┐│
│  │ │ 📦 Sữa MILO thùng 12x110ml   │││
│  │ │    Nestlé Chính hãng          │││
│  │ │    SL: 3 • 248.000 ₫          │││
│  │ │    Hoàn: 34.150 ₫ (72%)       │││
│  │ └───────────────────────────────┘│
│  │ Tổng đơn: 34.150 ₫              ││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │ 🛒 260110KTVR7JPC               ││
│  │    Shopee • 10/01/2026          ││
│  │ ┌───────────────────────────────┐│
│  │ │ 📦 Combo 3 Thùng Bia Saigon  │││
│  │ │    SABECO                     │││
│  │ │    SL: 1 • 900.000 ₫          │││
│  │ │    Hoàn: 22.745 ₫ (72%)       │││
│  │ └───────────────────────────────┘│
│  │ Tổng đơn: 22.745 ₫              ││
│  └─────────────────────────────────┘│
│  ... (thêm đơn hàng khác)           │
├─────────────────────────────────────┤
│  CÁCH TÍNH                          │
│  ┌─────────────────────────────────┐│
│  │ Tiền hoàn = Hoa hồng × 80%      ││
│  │                                  ││
│  │ Tỷ lệ hoàn có thể khác nhau     ││
│  │ tùy theo sản phẩm               ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Color scheme gợi ý

```typescript
const colors = {
  // Status colors
  success: '#22C55E',      // Đã thanh toán
  failed: '#EF4444',       // Thất bại
  partial: '#F59E0B',      // Một phần

  // Platform colors
  shopee: '#EE4D2D',
  lazada: '#0F146D',
  tiki: '#1A94FF',
  tiktok: '#000000',

  // Text colors
  primary: '#1F2937',
  secondary: '#6B7280',
  accent: '#3B82F6',

  // Background
  card: '#FFFFFF',
  background: '#F3F4F6',
};
```

### Component structure gợi ý

```
InvoiceDetailScreen/
├── components/
│   ├── InvoiceHeader.tsx        // Mã hóa đơn, ngày, trạng thái
│   ├── SummaryCard.tsx          // Tổng quan (tiền, số đơn, tỷ lệ)
│   ├── BankInfoCard.tsx         // Thông tin ngân hàng
│   ├── OrderList.tsx            // Danh sách đơn hàng
│   │   ├── OrderCard.tsx        // Card từng đơn
│   │   └── OrderItemRow.tsx     // Row từng sản phẩm
│   └── CalculationInfo.tsx      // Giải thích cách tính
├── hooks/
│   └── useInvoiceDetail.ts      // Custom hook gọi API
├── types/
│   └── invoice.types.ts         // TypeScript interfaces
└── InvoiceDetailScreen.tsx      // Main screen
```

---

## Xử lý các trường hợp đặc biệt

### 1. Đơn hàng không có item chi tiết

Một số đơn hàng cũ có thể không có thông tin item-level. API sẽ trả về:

```json
{
  "itemName": "Sản phẩm từ đơn hàng 260110MY3RKCBT",
  "shopName": null,
  "quantity": null,
  "productPrice": 248000.00,
  ...
}
```

**Xử lý FE:**
```typescript
const displayItemName = item.itemName || `Sản phẩm #${index + 1}`;
const displayShopName = item.shopName || 'Không có thông tin shop';
const displayQuantity = item.quantity ?? 1;
```

### 2. Hình ảnh sản phẩm null

```typescript
const productImage = item.imageUrl || DEFAULT_PRODUCT_IMAGE;
```

### 3. Tỷ lệ hoàn khác nhau giữa các sản phẩm

Mỗi sản phẩm có thể có `cashbackRate` khác nhau. Hiển thị:

```typescript
<Text>{item.cashbackRate}%</Text>
```

### 4. Empty orders array

Trường hợp hiếm khi invoice tồn tại nhưng không có cashback records:

```typescript
if (invoice.orders.length === 0) {
  return <EmptyState message="Không có chi tiết đơn hàng" />;
}
```

---

## Liên kết với các API khác

### Từ danh sách hóa đơn → Chi tiết

```typescript
// Màn hình danh sách hóa đơn
// GET /api/invoices/me?page=0&size=10

const handleInvoicePress = (invoiceId: number) => {
  navigation.navigate('InvoiceDetail', { invoiceId });
};
```

### Refresh sau khi thanh toán batch

```typescript
// Sau khi batch transfer complete, có thể:
// 1. Pull-to-refresh danh sách hóa đơn
// 2. Navigate đến hóa đơn mới tạo
```

---

## Testing

### Test cases

1. **Happy path:** Lấy chi tiết hóa đơn hợp lệ
2. **Unauthorized:** Gọi API không có token
3. **Not found:** Gọi với invoiceId không tồn tại
4. **Forbidden:** Gọi với invoiceId của user khác (trả về 404)
5. **Empty orders:** Invoice tồn tại nhưng không có cashback data

### Mock data cho development

```typescript
const mockInvoiceDetail: InvoiceDetailResponse = {
  id: 1,
  invoiceNumber: "INV-20260114-00001",
  transferTime: "2026-01-14T10:30:00",
  transferStatus: "SUCCESS",
  createdAt: "2026-01-14T10:30:00",
  summary: {
    totalOrders: 2,
    totalItems: 3,
    totalProductAmount: 1148000,
    totalCommission: 79020,
    cashbackRate: 72,
    platformFeeRate: 28,
    totalCashback: 56894.4,
    currency: "VND"
  },
  bankInfo: {
    bankName: "Vietcombank",
    bankFullName: "Ngân hàng TMCP Ngoại thương Việt Nam",
    accountNumber: "****1234",
    accountName: "NGUYEN VAN A"
  },
  orders: [
    {
      orderCode: "260110MY3RKCBT",
      orderTime: "2026-01-10T12:03:27",
      platformName: "Shopee",
      items: [
        {
          itemName: "Sữa lúa mạch Nestlé MILO thùng 12 x 110ml",
          shopName: "Nestlé Chính hãng",
          quantity: 3,
          productPrice: 248000,
          commission: 47430,
          cashbackRate: 72,
          cashbackAmount: 34149.6,
          imageUrl: null,
          category: "Thực phẩm và đồ uống"
        }
      ],
      totals: {
        productAmount: 248000,
        commission: 47430,
        cashbackAmount: 34149.6
      }
    }
  ],
  calculation: {
    description: "Tiền hoàn được tính dựa trên hoa hồng từ các nền tảng thương mại điện tử",
    formula: "Tiền hoàn = Hoa hồng × Tỷ lệ hoàn (thường là 80%)",
    note: "Tỷ lệ hoàn có thể khác nhau tùy theo sản phẩm và chương trình khuyến mãi"
  }
};
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-14 | Initial release |

---

## Liên hệ

Nếu có thắc mắc về API, vui lòng liên hệ:
- Backend Team: backend@cashbee.vn
- API Support: api-support@cashbee.vn
