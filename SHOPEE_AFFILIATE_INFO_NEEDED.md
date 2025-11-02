# 📋 Thông Tin Cần Cung Cấp Từ Tài Khoản Shopee Affiliate

## 🎯 Để Fix Vấn Đề Link Generation

Tôi cần bạn làm các bước sau và cung cấp thông tin:

### Bước 1: Tạo Link Test Trên Shopee Affiliate Portal

1. **Đăng nhập vào:** https://affiliate.shopee.vn
2. **Vào mục:** "Product Offer" hoặc "Tạo Link"
3. **Nhập link sản phẩm này để test:**
   ```
   https://shopee.vn/product/289826815/21040225881
   ```
4. **Tạo link affiliate** và copy link được generate

### Bước 2: Cung Cấp Thông Tin

#### ✅ Link Được Shopee Portal Tạo Ra

**Link affiliate được tạo từ Shopee Portal:**
```
[PASTE LINK Ở ĐÂY]
```

**Ví dụ:** Link có thể có format như:
- `https://shopee.vn/universal-link/21040225881?af_siteid=X&pid=...`
- `https://shopee.vn/-i.289826815.21040225881?af_siteid=X&pid=...`
- `https://shopee.vn/product/289826815/21040225881?af_siteid=X&pid=...`
- Hoặc format khác?

#### ✅ Screenshots

**Screenshot 1:** Trang tạo link affiliate
- Chụp màn hình trang tạo link trên Shopee Affiliate Portal
- Bao gồm:
  - Input URL
  - Link được generate
  - Các tùy chọn (nếu có)

**Screenshot 2:** Link sau khi được generate
- Chụp màn hình link được tạo ra
- Copy full link

#### ✅ Thông Tin Tài Khoản

**Affiliate ID của bạn:**
```
[PASTE AFFILIATE ID]
```

Tìm ở đâu:
1. Vào Shopee Affiliate Portal
2. Mục "Account" hoặc "Settings"
3. Tìm "Publisher ID" hoặc "Affiliate ID"

**Ví dụ:**
- Format cũ: `123456`
- Format mới: `cashbee_vn_123456`
- Hoặc format khác?

#### ✅ Tracking Parameters Support

Shopee có hỗ trợ các parameter sau không:

**Sub ID Parameters:**
- `af_sub1` - ✅/❌
- `af_sub2` - ✅/❌
- `af_sub3` - ✅/❌
- `af_sub4` - ✅/❌
- `af_sub5` - ✅/❌

**Cách test:**
1. Tạo link trên Shopee Portal
2. Xem link có parameter `af_sub1`, `af_sub2` không?
3. Hoặc xem documentation trên portal

#### ✅ Link Format Examples

**Test với các loại link khác nhau:**

**Link 1:** Standard product link
```
Input:  https://shopee.vn/product/289826815/21040225881
Output: [PASTE LINK ĐƯỢC TẠO]
```

**Link 2:** Short format link (nếu có)
```
Input:  https://shopee.vn/-i.289826815.21040225881
Output: [PASTE LINK ĐƯỢC TẠO]
```

**Link 3:** Universal link format (nếu có)
```
Input:  https://shopee.vn/universal-link/21040225881
Output: [PASTE LINK ĐƯỢC TẠO]
```

### Bước 3: Documentation (Nếu Có)

**Shopee Affiliate có cung cấp documentation không?**

Nếu có, chụp screenshot hoặc copy text về:
1. Link format guidelines
2. URL structure
3. Supported parameters
4. Examples

---

## 🎯 Các Format Link Có Thể

Dựa vào kinh nghiệm với các affiliate networks khác, Shopee có thể dùng một trong các format sau:

### Format 1: Universal Link + Item ID Only ❌ (Đang dùng - KHÔNG HOẠT ĐỘNG)
```
https://shopee.vn/universal-link/{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```
**Vấn đề:** Thiếu shop_id → "Không thể tải Shop này"

### Format 2: Short Link Format (i.shop_id.item_id) 🤔
```
https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```
**Ví dụ:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102
```

### Format 3: Product URL Format 🤔
```
https://shopee.vn/product/{shop_id}/{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```
**Ví dụ:**
```
https://shopee.vn/product/289826815/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102
```

### Format 4: Shopee Redirect Service 🤔
```
https://s.shopee.vn/XXXXXX?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```
**Ví dụ:**
```
https://s.shopee.vn/ABC123?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102
```

### Format 5: Universal Link + Shop ID Parameter 🤔
```
https://shopee.vn/universal-link/{item_id}?shop_id={shop_id}&af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```
**Ví dụ:**
```
https://shopee.vn/universal-link/21040225881?shop_id=289826815&af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102
```

---

## 🔍 Cách Tìm Format Đúng

### Option 1: Tạo Link Trên Portal và Phân Tích

1. Vào Shopee Affiliate Portal
2. Tạo link cho sản phẩm: `https://shopee.vn/product/289826815/21040225881`
3. Copy link được tạo
4. Phân tích cấu trúc:
   - Base URL là gì?
   - Shop ID ở đâu?
   - Item ID ở đâu?
   - Parameters nào được thêm?

### Option 2: Check Network Traffic

1. Mở DevTools (F12)
2. Vào tab "Network"
3. Tạo link trên Shopee Affiliate Portal
4. Tìm request tạo link
5. Xem response chứa link format gì

### Option 3: Kiểm Tra Documentation

1. Vào Shopee Affiliate Portal
2. Tìm "Help", "Documentation", "API Docs"
3. Tìm phần "Link Format" hoặc "URL Structure"

---

## ⚡ Sau Khi Có Thông Tin

Khi bạn cung cấp thông tin trên, tôi sẽ:

1. ✅ Xác định format link đúng
2. ✅ Update link template trong database
3. ✅ Test với nhiều loại URL
4. ✅ Verify tracking hoạt động
5. ✅ Tạo documentation chi tiết

---

## 📝 Template Email Gửi Thông Tin

```
Format Link Từ Shopee Portal:
[PASTE LINK]

Affiliate ID:
[PASTE ID]

Screenshots:
[ATTACH FILES]

Link Test Cases:
1. Product link: https://shopee.vn/product/289826815/21040225881
   Generated: [PASTE]

2. Short link (nếu có): https://shopee.vn/-i.289826815.21040225881
   Generated: [PASTE]

Sub ID Support:
- af_sub1: Yes/No
- af_sub2: Yes/No
- af_sub3: Yes/No

Additional Notes:
[Any other information from Shopee Portal]
```

---

**Cung cấp thông tin càng nhiều càng tốt để tôi có thể fix chính xác!** 🎯
