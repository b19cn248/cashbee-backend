# ShopeeFood Integration Guide for Frontend

> **Version:** 1.0
> **Date:** 2025-12-20
> **Author:** CashBee Backend Team

## 1. Tổng quan

Backend đã được cập nhật để hỗ trợ tạo affiliate tracking link cho **ShopeeFood** bên cạnh Shopee Mall hiện tại. Tính năng này cho phép user paste link từ ShopeeFood app/website và nhận được tracking link để kiếm cashback.

### Điểm quan trọng

| Đặc điểm | Trước đây | Sau cập nhật |
|----------|-----------|--------------|
| Platform hỗ trợ | Shopee Mall only | Shopee Mall + ShopeeFood |
| Auto-detect platform | Không | Có (từ URL) |
| Request body | Không đổi | Không đổi |

---

## 2. API Endpoint (Không thay đổi)

```
POST /api/affiliate/tracking/create-link
```

### Headers
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### Request Body (Không thay đổi)
```json
{
  "shopeeUrl": "https://..."
}
```

> **LƯU Ý:** Field `shopeeUrl` giờ chấp nhận cả URL từ Shopee Mall và ShopeeFood. Tên field giữ nguyên để backward compatible.

---

## 3. Các định dạng URL được hỗ trợ

### 3.1 Shopee Mall (như cũ)

| Format | Ví dụ |
|--------|-------|
| Standard | `https://shopee.vn/product/123456/789012` |
| Short | `https://shopee.vn/Product-Name-i.123456.789012` |
| Shortened | `https://s.shopee.vn/5Al0npFYE8` |
| Share link | `https://vn.shp.ee/S5hDghe` |

### 3.2 ShopeeFood (MỚI)

| Format | Ví dụ | Nguồn |
|--------|-------|-------|
| Shortened link | `https://shopeefood.shopee.vn/u/je8DiJT` | Share từ app |
| Detail URL | `https://shopeefood.vn/now-food/cheap-meal/detail?itemId=278729114&restaurantId=1158256` | Copy từ browser |
| Restaurant page | `https://shopeefood.vn/ha-noi/link-food-trung-van` | Copy từ browser |

---

## 4. Response Format

### 4.1 Response cho Shopee Mall

```json
{
  "success": true,
  "message": "Tracking link created successfully",
  "data": {
    "clickId": 12345,
    "trackingUrl": "https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2F...&affiliate_id=14354840000&sub_id=CB1_100_20251220",
    "trackingCode": "CB1_100_20251220",
    "originalUrl": "https://shopee.vn/product/123/456",
    "productName": null,
    "shopId": "123",
    "itemId": "456",
    "platformName": "Shopee",
    "platformCode": "shopee",
    "estimatedCashbackRate": 0.05,
    "createdAt": "2025-12-20T10:00:00",
    "message": "Click this link to shop on Shopee and earn cashback!"
  }
}
```

### 4.2 Response cho ShopeeFood (MỚI)

```json
{
  "success": true,
  "message": "Tracking link created successfully",
  "data": {
    "clickId": 12346,
    "trackingUrl": "https://shopeefood.vn/an_redir?origin_link=https%3A%2F%2Fshopeefood.vn%2F...&affiliate_id=14354840000&sub_id=CB1_101_20251220",
    "trackingCode": "CB1_101_20251220",
    "originalUrl": "https://shopeefood.shopee.vn/u/je8DiJT",
    "productName": null,
    "shopId": null,
    "itemId": "278729114",
    "platformName": "ShopeeFood",
    "platformCode": "shopeefood",
    "estimatedCashbackRate": 0.03,
    "createdAt": "2025-12-20T10:00:00",
    "message": "Click this link to shop on ShopeeFood and earn cashback!"
  }
}
```

### Điểm khác biệt trong Response

| Field | Shopee Mall | ShopeeFood |
|-------|-------------|------------|
| `platformCode` | `"shopee"` | `"shopeefood"` |
| `platformName` | `"Shopee"` | `"ShopeeFood"` |
| `shopId` | Có giá trị | `null` |
| `itemId` | Product ID | Dish/Item ID |
| `trackingUrl` base | `s.shopee.vn/an_redir` | `shopeefood.vn/an_redir` |

---

## 5. Hướng dẫn cho Frontend

### 5.1 Luồng xử lý (Flow)

```
┌─────────────────────────────────────────────────────────────┐
│                        USER FLOW                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. User mở app Shopee/ShopeeFood                          │
│              │                                              │
│              ▼                                              │
│  2. User tìm sản phẩm/món ăn muốn mua                      │
│              │                                              │
│              ▼                                              │
│  3. User nhấn "Share" và copy link                         │
│              │                                              │
│              ▼                                              │
│  4. User mở CashBee app/web                                │
│              │                                              │
│              ▼                                              │
│  5. User paste link vào ô input                            │
│              │                                              │
│              ▼                                              │
│  6. FE gọi API: POST /api/affiliate/tracking/create-link   │
│              │                                              │
│              ▼                                              │
│  7. BE tự động detect platform (shopee/shopeefood)         │
│              │                                              │
│              ▼                                              │
│  8. BE trả về tracking link                                │
│              │                                              │
│              ▼                                              │
│  9. FE hiển thị kết quả + button "Mở link"                 │
│              │                                              │
│              ▼                                              │
│  10. User nhấn → Redirect đến Shopee/ShopeeFood với        │
│      tracking để mua hàng và nhận cashback                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Xử lý UI dựa trên platformCode

```javascript
// Ví dụ xử lý response
const response = await createTrackingLink(url);
const { platformCode, platformName, trackingUrl } = response.data;

// Hiển thị UI phù hợp với platform
if (platformCode === 'shopeefood') {
  // Hiển thị icon ShopeeFood
  // Hiển thị màu cam đặc trưng của ShopeeFood
  // Text: "Đặt món trên ShopeeFood"
} else if (platformCode === 'shopee') {
  // Hiển thị icon Shopee
  // Hiển thị màu cam Shopee
  // Text: "Mua sắm trên Shopee"
}
```

### 5.3 Validate URL ở Frontend (Optional)

FE có thể validate URL trước khi gọi API để UX tốt hơn:

```javascript
const SUPPORTED_URL_PATTERNS = [
  // Shopee Mall
  /^https?:\/\/shopee\.vn\//,
  /^https?:\/\/s\.shopee\.vn\//,
  /^https?:\/\/[a-z]{2}\.shp\.ee\//,
  /^https?:\/\/shope\.ee\//,

  // ShopeeFood (MỚI)
  /^https?:\/\/shopeefood\.vn\//,
  /^https?:\/\/shopeefood\.shopee\.vn\//,
];

function isValidUrl(url) {
  return SUPPORTED_URL_PATTERNS.some(pattern => pattern.test(url));
}

// Sử dụng
if (!isValidUrl(userInput)) {
  showError("Vui lòng nhập link từ Shopee hoặc ShopeeFood");
  return;
}
```

### 5.4 Xử lý Deep Link cho Mobile App

Khi user nhấn vào tracking link, cần mở đúng app:

```javascript
// React Native / Mobile
const openTrackingLink = async (trackingUrl, platformCode) => {
  if (platformCode === 'shopeefood') {
    // Thử mở ShopeeFood app trước
    const shopeeFoodAppUrl = trackingUrl.replace(
      'https://shopeefood.vn',
      'shopeefood://shopeefood.vn'
    );

    const canOpen = await Linking.canOpenURL(shopeeFoodAppUrl);
    if (canOpen) {
      await Linking.openURL(shopeeFoodAppUrl);
    } else {
      // Fallback: mở browser
      await Linking.openURL(trackingUrl);
    }
  } else {
    // Shopee Mall
    const shopeeAppUrl = trackingUrl.replace(
      'https://s.shopee.vn',
      'shopee://s.shopee.vn'
    );

    const canOpen = await Linking.canOpenURL(shopeeAppUrl);
    if (canOpen) {
      await Linking.openURL(shopeeAppUrl);
    } else {
      await Linking.openURL(trackingUrl);
    }
  }
};
```

---

## 6. UI/UX Recommendations

### 6.1 Input Field

```
┌─────────────────────────────────────────────────────────────┐
│  Dán link sản phẩm Shopee hoặc ShopeeFood                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ https://shopeefood.shopee.vn/u/je8DiJT              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Hỗ trợ: Shopee, ShopeeFood                                │
│                                              [Tạo link]     │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Result Card - Shopee Mall

```
┌─────────────────────────────────────────────────────────────┐
│  🛒 Shopee                                                  │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  Link tracking đã sẵn sàng!                                │
│                                                             │
│  Cashback ước tính: 5%                                      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            [🛍️ Mua sắm trên Shopee]                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [📋 Copy link]                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 Result Card - ShopeeFood

```
┌─────────────────────────────────────────────────────────────┐
│  🍔 ShopeeFood                                              │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  Link tracking đã sẵn sàng!                                │
│                                                             │
│  Cashback ước tính: 3%                                      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            [🍕 Đặt món trên ShopeeFood]            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [📋 Copy link]                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.4 Icon/Color Scheme

| Platform | Icon | Primary Color | Background |
|----------|------|---------------|------------|
| Shopee | 🛒 hoặc Shopee logo | `#EE4D2D` | `#FFF5F2` |
| ShopeeFood | 🍔 hoặc ShopeeFood logo | `#FF6633` | `#FFF5EE` |

---

## 7. Error Handling

### 7.1 Các lỗi có thể xảy ra

| Error Message | Nguyên nhân | Xử lý FE |
|---------------|-------------|----------|
| `Invalid URL format` | URL không hợp lệ | Hiển thị: "Link không hợp lệ. Vui lòng kiểm tra lại." |
| `Platform is not active` | Platform bị tắt | Hiển thị: "Nền tảng này hiện không hỗ trợ. Vui lòng thử lại sau." |
| `Failed to expand shortened link` | Không expand được short URL | Hiển thị: "Không thể xử lý link. Vui lòng thử copy link đầy đủ." |
| `Affiliate platform not found: shopeefood` | Chưa config platform trong DB | Liên hệ admin để thêm config |

### 7.2 Example Error Response

```json
{
  "success": false,
  "message": "Invalid URL format: Invalid ShopeeFood URL format: https://invalid.url",
  "data": null,
  "error": {
    "code": "BUSINESS_ERROR",
    "details": null
  }
}
```

---

## 8. Testing Checklist

### 8.1 Test Cases cho Shopee Mall

- [ ] Link sản phẩm standard: `https://shopee.vn/product/123/456`
- [ ] Link sản phẩm có tên: `https://shopee.vn/Product-Name-i.123.456`
- [ ] Link rút gọn: `https://s.shopee.vn/xxx`
- [ ] Link share từ app: `https://vn.shp.ee/xxx`

### 8.2 Test Cases cho ShopeeFood (MỚI)

- [ ] Link rút gọn từ app: `https://shopeefood.shopee.vn/u/je8DiJT`
- [ ] Link chi tiết món ăn: `https://shopeefood.vn/now-food/cheap-meal/detail?itemId=xxx&restaurantId=xxx`
- [ ] Link trang nhà hàng: `https://shopeefood.vn/ha-noi/restaurant-name`

### 8.3 Edge Cases

- [ ] URL có query params phức tạp
- [ ] URL có ký tự Unicode (tiếng Việt)
- [ ] URL không hợp lệ → hiển thị lỗi phù hợp
- [ ] Network error → hiển thị thông báo retry

---

## 9. Migration Notes

### Không cần thay đổi breaking change

- API endpoint giữ nguyên
- Request format giữ nguyên
- Thêm các URL patterns mới được hỗ trợ

### Recommended UI Updates

1. **Input placeholder**: Thay đổi text từ "Dán link Shopee" → "Dán link Shopee hoặc ShopeeFood"
2. **Supported platforms**: Thêm logo/text ShopeeFood vào phần hiển thị platforms hỗ trợ
3. **Result card**: Hiển thị UI khác nhau dựa trên `platformCode`
4. **Icon/màu sắc**: Dùng icon và màu sắc phù hợp với từng platform

---

## 10. FAQ

### Q1: Có cần gửi thêm field nào trong request không?

**A:** Không. Backend tự động detect platform từ URL. FE chỉ cần gửi `shopeeUrl` như trước.

### Q2: Làm sao biết link thuộc platform nào?

**A:** Kiểm tra field `platformCode` trong response:
- `"shopee"` → Shopee Mall
- `"shopeefood"` → ShopeeFood

### Q3: Tỷ lệ cashback có khác nhau không?

**A:** Có. Mỗi platform có tỷ lệ khác nhau, được trả về trong field `estimatedCashbackRate`.

### Q4: Tracking link mở app hay browser?

**A:** Mặc định mở browser. FE cần xử lý deep link nếu muốn mở app trực tiếp (xem section 5.4).

---

## 11. Contact

Nếu có thắc mắc, liên hệ:
- Backend Team: [backend@cashbee.vn]
- Slack: #cashbee-backend

---

*Document generated on 2025-12-20*
