# API Documentation: Voucher/Promotion Code Management

## Tổng quan

API để lưu trữ và quản lý mã giảm giá từ Telegram. Hệ thống tự động chuyển đổi link Shopee thành affiliate link.

**Base URL**: `https://your-domain.com/api/vouchers`

**Authentication**: Không cần - Tất cả endpoints đều public.

---

## API 1: Tạo Voucher Đơn Lẻ

### Endpoint
```
POST /api/vouchers
```

### Headers
```
Content-Type: application/json
```

### Request Body

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `code` | string | No | Mã voucher | `"AFFZOSI0"` |
| `title` | string | **Yes** | Tiêu đề voucher | `"Giảm 30k từ 99k"` |
| `description` | string | No | Mô tả chi tiết | `"Áp dụng cho đa số sản phẩm"` |
| `discountType` | string | **Yes** | Loại giảm giá | `"FIXED_AMOUNT"` hoặc `"PERCENTAGE"` |
| `discountValue` | number | No | Giá trị giảm | `30000` (đồng) hoặc `20` (%) |
| `maxDiscount` | number | No | Giảm tối đa (cho mã %) | `70000` |
| `minOrderValue` | number | No | Đơn tối thiểu | `99000` |
| `originalLink` | string | No | Link sản phẩm Shopee | `"https://s.shopee.vn/8V2XQ3Idut"` |
| `voucherSaveLink` | string | No | Link để lưu voucher | `"https://s.shopee.vn/abc123"` |
| `category` | string | No | Phân loại | Xem bảng category bên dưới |
| `platform` | string | No | Nền tảng (default: SHOPEE) | `"SHOPEE"`, `"LAZADA"`, `"TIKTOK"` |
| `validTimeSlot` | string | No | Khung giờ áp dụng | `"9H-11H"` |
| `validFrom` | string | No | Thời gian bắt đầu (ISO 8601) | `"2026-01-11T09:00:00"` |
| `validUntil` | string | No | Thời gian kết thúc (ISO 8601) | `"2026-01-11T23:59:59"` |

### Giá trị discountType:
| Value | Mô tả | Ví dụ |
|-------|-------|-------|
| `FIXED_AMOUNT` | Giảm số tiền cố định | Giảm 30.000đ |
| `PERCENTAGE` | Giảm theo phần trăm | Giảm 20% |

### Giá trị category:
| Value | Mô tả |
|-------|-------|
| `GENERAL` | Áp đa số - Mã phổ thông |
| `NEW_SHOP` | Shop mới lên sàn |
| `SHOPEE_XULY` | Shopee xử lý |
| `XTRA` | HH Xtra |
| `MXH` | Mã độc quyền MXH |
| `VIP` | Mã VIP/Thân thiết |
| `OTHER` | Khác |

### Example Request - Mã giảm tiền cố định:
```json
{
    "code": "AFFZOSI0",
    "title": "Back AFF giảm 30k từ 99k",
    "description": "Áp dụng cho đa số sản phẩm",
    "discountType": "FIXED_AMOUNT",
    "discountValue": 30000,
    "minOrderValue": 99000,
    "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
    "category": "GENERAL",
    "validTimeSlot": "9H-11H"
}
```

### Example Request - Mã giảm phần trăm:
```json
{
    "code": "SPVIP20",
    "title": "SPVip 20% tối đa 2TR",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "maxDiscount": 2000000,
    "minOrderValue": 2000000,
    "originalLink": "https://s.shopee.vn/gJgyeQ7FR",
    "category": "VIP"
}
```

### Example Request - Chỉ có link lưu mã:
```json
{
    "title": "Lưu x2 mã SPVip giảm 25% tối đa 200K",
    "discountType": "PERCENTAGE",
    "discountValue": 25,
    "maxDiscount": 200000,
    "minOrderValue": 0,
    "voucherSaveLink": "https://s.shopee.vn/2B8Gde97ng",
    "category": "VIP"
}
```

### Success Response (201 Created):
```json
{
    "success": true,
    "message": "Voucher created successfully",
    "data": {
        "id": 123,
        "code": "AFFZOSI0",
        "title": "Back AFF giảm 30k từ 99k",
        "description": "Áp dụng cho đa số sản phẩm",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 30000,
        "maxDiscount": null,
        "minOrderValue": 99000,
        "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
        "affiliateLink": "https://s.shopee.vn/an_redir?origin_link=...&affiliate_id=...&sub_id=voucher_123",
        "voucherSaveLink": null,
        "category": "GENERAL",
        "platform": "SHOPEE",
        "validTimeSlot": "9H-11H",
        "validFrom": null,
        "validUntil": null,
        "status": "ACTIVE",
        "viewCount": 0,
        "clickCount": 0,
        "displayDiscount": "30.000đ",
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

### Error Response (400 Bad Request):
```json
{
    "success": false,
    "errorCode": "VALIDATION_ERROR",
    "message": "title: Title is required; discountType: Discount type is required",
    "timestamp": "2026-01-11T10:30:00"
}
```

---

## API 2: Import Nhiều Voucher (Batch)

### Endpoint
```
POST /api/vouchers/batch
```

### Headers
```
Content-Type: application/json
```

### Request Body
Array of voucher objects (giống như API 1)

### Example Request:
```json
[
    {
        "code": "AFFZOSI0",
        "title": "Giảm 30k từ 99k",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 30000,
        "minOrderValue": 99000,
        "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
        "category": "GENERAL"
    },
    {
        "code": "AFFDEN",
        "title": "Giảm 39K từ 129K",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 39000,
        "minOrderValue": 129000,
        "originalLink": "https://s.shopee.vn/8V2Zzhxvk6",
        "category": "GENERAL"
    },
    {
        "title": "XTRA 20% tối đa 150K từ 500K",
        "discountType": "PERCENTAGE",
        "discountValue": 20,
        "maxDiscount": 150000,
        "minOrderValue": 500000,
        "voucherSaveLink": "https://s.shopee.vn/6VBgucz3hL",
        "category": "XTRA"
    }
]
```

### Success Response (200 OK):
```json
{
    "success": true,
    "message": "Batch import completed",
    "data": {
        "totalReceived": 3,
        "created": 3,
        "updated": 0,
        "failed": 0,
        "errors": []
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

### Partial Success Response (200 OK):
```json
{
    "success": true,
    "message": "Batch import completed with some errors",
    "data": {
        "totalReceived": 3,
        "created": 2,
        "updated": 0,
        "failed": 1,
        "errors": [
            {
                "index": 2,
                "message": "Invalid discount type"
            }
        ]
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

### Duplicate Handling:
- Nếu voucher có cùng `code` đã tồn tại, hệ thống sẽ **UPDATE** thông tin voucher cũ
- Response sẽ tính vào `updated` thay vì `created`

---

## API 3: Lấy Danh Sách Voucher

### Endpoint
```
GET /api/vouchers
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `status` | string | No | `ACTIVE` | Filter theo status |
| `category` | string | No | - | Filter theo category |
| `platform` | string | No | - | Filter theo platform |
| `page` | number | No | `0` | Số trang (bắt đầu từ 0) |
| `size` | number | No | `20` | Số item mỗi trang |
| `sort` | string | No | `createdAt,desc` | Sắp xếp |

### Example Requests:
```
GET /api/vouchers
GET /api/vouchers?status=ACTIVE
GET /api/vouchers?status=ACTIVE&category=GENERAL
GET /api/vouchers?status=ACTIVE&category=XTRA&page=0&size=10
GET /api/vouchers?sort=discountValue,desc
```

### Success Response (200 OK):
```json
{
    "success": true,
    "data": {
        "content": [
            {
                "id": 123,
                "code": "AFFZOSI0",
                "title": "Back AFF giảm 30k từ 99k",
                "discountType": "FIXED_AMOUNT",
                "discountValue": 30000,
                "minOrderValue": 99000,
                "affiliateLink": "https://s.shopee.vn/an_redir?...",
                "category": "GENERAL",
                "status": "ACTIVE",
                "viewCount": 150,
                "clickCount": 45,
                "displayDiscount": "30.000đ",
                "createdAt": "2026-01-11T10:30:00"
            }
        ],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 20
        },
        "totalElements": 50,
        "totalPages": 3,
        "first": true,
        "last": false
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

---

## API 4: Lấy Chi Tiết Voucher

### Endpoint
```
GET /api/vouchers/{id}
```

### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | number | ID của voucher |

### Example:
```
GET /api/vouchers/123
```

### Success Response (200 OK):
```json
{
    "success": true,
    "data": {
        "id": 123,
        "code": "AFFZOSI0",
        "title": "Back AFF giảm 30k từ 99k",
        "description": "Áp dụng cho đa số sản phẩm",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 30000,
        "maxDiscount": null,
        "minOrderValue": 99000,
        "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
        "affiliateLink": "https://s.shopee.vn/an_redir?...",
        "voucherSaveLink": null,
        "category": "GENERAL",
        "platform": "SHOPEE",
        "validTimeSlot": "9H-11H",
        "validFrom": null,
        "validUntil": null,
        "status": "ACTIVE",
        "viewCount": 150,
        "clickCount": 45,
        "displayDiscount": "30.000đ",
        "createdAt": "2026-01-11T10:30:00",
        "updatedAt": "2026-01-11T10:30:00"
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

### Error Response (404 Not Found):
```json
{
    "success": false,
    "errorCode": "NOT_FOUND",
    "message": "Voucher not found with id: 999",
    "timestamp": "2026-01-11T10:30:00"
}
```

---

## API 5: Track View (Khi User Xem Voucher)

### Endpoint
```
POST /api/vouchers/{id}/view
```

### Example:
```
POST /api/vouchers/123/view
```

### Success Response (200 OK):
```json
{
    "success": true,
    "message": "View tracked",
    "data": {
        "voucherId": 123,
        "viewCount": 151
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

---

## API 6: Track Click (Khi User Click Link)

### Endpoint
```
POST /api/vouchers/{id}/click
```

### Example:
```
POST /api/vouchers/123/click
```

### Success Response (200 OK):
```json
{
    "success": true,
    "message": "Click tracked",
    "data": {
        "voucherId": 123,
        "clickCount": 46,
        "affiliateLink": "https://s.shopee.vn/an_redir?..."
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

**Lưu ý**: Response trả về `affiliateLink` để redirect user. Fallback order:
1. `affiliateLink` (nếu có)
2. `originalLink` (nếu không có affiliate)
3. `voucherSaveLink` (nếu không có cả 2)

---

## API 7: Cập Nhật Status Voucher

### Endpoint
```
PATCH /api/vouchers/{id}/status
```

### Headers
```
Content-Type: application/json
```

### Request Body
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | **Yes** | Status mới |

### Giá trị status:
| Value | Mô tả |
|-------|-------|
| `ACTIVE` | Đang hoạt động |
| `INACTIVE` | Tạm ngừng |
| `EXPIRED` | Hết hạn |

### Example Request:
```json
{
    "status": "EXPIRED"
}
```

### Success Response (200 OK):
```json
{
    "success": true,
    "message": "Voucher status updated",
    "data": {
        "id": 123,
        "code": "AFFZOSI0",
        "title": "Back AFF giảm 30k từ 99k",
        "status": "EXPIRED",
        "updatedAt": "2026-01-11T10:30:00"
    },
    "timestamp": "2026-01-11T10:30:00"
}
```

---

## Error Codes Reference

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Dữ liệu không hợp lệ |
| `NOT_FOUND` | 404 | Không tìm thấy voucher |
| `INVALID_DISCOUNT_TYPE` | 400 | discountType không hợp lệ |
| `INVALID_CATEGORY` | 400 | category không hợp lệ |
| `INVALID_STATUS` | 400 | status không hợp lệ |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống |

---

## Lưu ý quan trọng cho Tool Telegram

### 1. Không cần Authentication
Tất cả API đều public, không cần JWT token hay API key.

### 2. Affiliate Link tự động
Chỉ cần gửi `originalLink` (link Shopee gốc), hệ thống sẽ tự động tạo `affiliateLink`. Hỗ trợ cả link rút gọn (s.shopee.vn).

### 3. Duplicate Handling
- Nếu tạo voucher có cùng `code`, hệ thống sẽ **UPDATE** voucher cũ
- Với batch import, count sẽ phản ánh số `created` và `updated`

### 4. Giá trị tiền
Đơn vị là VND, không có số thập phân:
- Đúng: `30000`, `99000`, `2000000`
- Sai: `30.000`, `30k`, `30,000`

### 5. Phần trăm
Giá trị từ 1-100:
- Đúng: `20` (cho 20%), `30` (cho 30%)
- Sai: `0.2`, `20%`

### 6. Datetime format
ISO 8601: `YYYY-MM-DDTHH:mm:ss`
- Ví dụ: `2026-01-11T09:00:00`

---

## Ví dụ Parse Tin Nhắn Telegram

### Input (mã tiền cố định):
```
◼️Back AFFZOSI0 giảm 30k từ 99k áp list: https://s.shopee.vn/8V2XQ3Idut
```

### Output JSON:
```json
{
    "code": "AFFZOSI0",
    "title": "Back AFFZOSI0 giảm 30k từ 99k",
    "discountType": "FIXED_AMOUNT",
    "discountValue": 30000,
    "minOrderValue": 99000,
    "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
    "category": "GENERAL"
}
```

### Input (mã phần trăm):
```
◼️X10 mã 30% tối đa 70K/200K áp list: https://s.shopee.vn/1qVPUVZqHy
```

### Output JSON:
```json
{
    "title": "X10 mã 30% tối đa 70K/200K",
    "discountType": "PERCENTAGE",
    "discountValue": 30,
    "maxDiscount": 70000,
    "minOrderValue": 200000,
    "originalLink": "https://s.shopee.vn/1qVPUVZqHy",
    "category": "SHOPEE_XULY"
}
```

### Input (mã lưu):
```
◼️Lưu x2 mã SPVip giảm 25% tối đa 200K: https://s.shopee.vn/2B8Gde97ng
```

### Output JSON:
```json
{
    "title": "Lưu x2 mã SPVip giảm 25% tối đa 200K",
    "discountType": "PERCENTAGE",
    "discountValue": 25,
    "maxDiscount": 200000,
    "voucherSaveLink": "https://s.shopee.vn/2B8Gde97ng",
    "category": "VIP"
}
```

---

## cURL Examples

### Tạo voucher:
```bash
curl -X POST http://localhost:8080/api/vouchers \
  -H "Content-Type: application/json" \
  -d '{
    "code": "AFFZOSI0",
    "title": "Giảm 30k từ 99k",
    "discountType": "FIXED_AMOUNT",
    "discountValue": 30000,
    "minOrderValue": 99000,
    "originalLink": "https://s.shopee.vn/8V2XQ3Idut",
    "category": "GENERAL"
  }'
```

### Batch import:
```bash
curl -X POST http://localhost:8080/api/vouchers/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"title": "Voucher 1", "discountType": "FIXED_AMOUNT", "discountValue": 30000},
    {"title": "Voucher 2", "discountType": "PERCENTAGE", "discountValue": 20}
  ]'
```

### Lấy danh sách:
```bash
curl "http://localhost:8080/api/vouchers?status=ACTIVE&category=GENERAL&page=0&size=10"
```

### Track click:
```bash
curl -X POST http://localhost:8080/api/vouchers/123/click
```

---

*Tài liệu được tạo tự động - CashBee Team*
