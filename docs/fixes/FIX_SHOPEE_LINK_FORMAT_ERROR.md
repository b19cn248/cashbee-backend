# 🔧 Fix: Shopee Link "Không thể tải Shop này"

## 📋 Vấn Đề

### Lỗi Gặp Phải

**Link được tạo:**
```
https://shopee.vn/universal-link/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Khi click vào link:**
- ❌ Hiển thị: "Không thể tải Shop này. Vui lòng chạm và thử lại."
- ❌ Không redirect đến sản phẩm đúng

**URL gốc:**
```
https://shopee.vn/product/289826815/21040225881
```
- shop_id: `289826815`
- item_id: `21040225881`

---

## 🔍 Nguyên Nhân Gốc Rễ

### 1. Link Template Sai

**Template hiện tại (WRONG):**
```
https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Vấn đề:**
- ❌ Chỉ sử dụng `item_id` (21040225881)
- ❌ **THIẾU `shop_id`** (289826815)
- ❌ Shopee cần **CẢ shop_id VÀ item_id** để xác định sản phẩm

### 2. Tại Sao Thiếu Shop ID?

**Shopee Product Identification:**

Shopee sử dụng **composite key** để identify sản phẩm:
```
product = (shop_id, item_id)
```

**Ví dụ:**
- Shop A bán sản phẩm có item_id = 123 (áo phông)
- Shop B bán sản phẩm có item_id = 123 (quần jean)

→ Cùng item_id nhưng **khác sản phẩm** vì khác shop!

**Chỉ có item_id = 21040225881 là không đủ!**

Shopee cần biết:
- item_id = **21040225881** (sản phẩm nào?)
- shop_id = **289826815** (của shop nào?)

→ Khi thiếu shop_id → "Không thể tải Shop này"

---

## ✅ Giải Pháp

### Option 1: Short Link Format ⭐ (RECOMMENDED)

**Template mới:**
```
https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Link được tạo:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Ưu điểm:**
- ✅ Có đủ shop_id và item_id
- ✅ Format ngắn gọn
- ✅ Được Shopee affiliate support
- ✅ Hoạt động với mọi loại URL

**Migration:**
```sql
UPDATE affiliate_platform
SET link_template = 'https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
WHERE code = 'shopee';
```

---

### Option 2: Product URL Format 🤔

**Template:**
```
https://shopee.vn/product/{shop_id}/{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Link được tạo:**
```
https://shopee.vn/product/289826815/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Ưu điểm:**
- ✅ Có đủ shop_id và item_id
- ✅ Format rõ ràng, dễ đọc
- ✅ Giống URL gốc

**Nhược điểm:**
- ⚠️ Link dài hơn Option 1
- ⚠️ Cần verify với Shopee affiliate

**Migration:**
```sql
UPDATE affiliate_platform
SET link_template = 'https://shopee.vn/product/{shop_id}/{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
WHERE code = 'shopee';
```

---

### Option 3: Universal Link + Shop Parameter 🤔

**Template:**
```
https://shopee.vn/universal-link/{item_id}?shop_id={shop_id}&af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Link được tạo:**
```
https://shopee.vn/universal-link/21040225881?shop_id=289826815&af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Ưu điểm:**
- ✅ Có đủ shop_id (qua parameter)
- ✅ Vẫn dùng universal-link

**Nhược điểm:**
- ⚠️ Cần verify Shopee có support `shop_id` parameter không

**Migration:**
```sql
UPDATE affiliate_platform
SET link_template = 'https://shopee.vn/universal-link/{item_id}?shop_id={shop_id}&af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
WHERE code = 'shopee';
```

---

## 🎯 Recommendation: OPTION 1 (Short Link)

### Tại Sao Chọn Option 1?

**1. Industry Standard**
- Được sử dụng rộng rãi bởi các affiliate marketers
- Format `-i.{shop_id}.{item_id}` là standard của Shopee
- Đã được test và verify bởi community

**2. Reliable**
- Format ổn định, không thay đổi
- Support đầy đủ tracking parameters
- Hoạt động với mọi loại product URL

**3. Clean & Short**
- Link ngắn gọn hơn Product URL format
- Dễ share, dễ copy
- Professional appearance

**4. Code Compatibility**
- Code hiện tại đã support `{shop_id}` placeholder
- Chỉ cần update template trong database
- Không cần sửa code Java

---

## 🔧 Implementation

### Bước 1: Verify Current Template

```sql
SELECT
    id,
    code,
    name,
    affiliate_id,
    link_template
FROM affiliate_platform
WHERE code = 'shopee';
```

**Expected current value:**
```
link_template = 'https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
```

### Bước 2: Update to Short Link Format

```sql
UPDATE affiliate_platform
SET link_template = 'https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
WHERE code = 'shopee';
```

### Bước 3: Verify Update

```sql
SELECT
    id,
    code,
    link_template
FROM affiliate_platform
WHERE code = 'shopee';
```

**Expected new value:**
```
link_template = 'https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
```

### Bước 4: Test API

```bash
curl -X POST http://localhost:8080/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
    "userId": 1
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "trackingUrl": "https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522",
    "trackingCode": "CB1_1_20251102065522",
    "shopId": "289826815",
    "itemId": "21040225881"
  }
}
```

### Bước 5: Click Test

1. Copy `trackingUrl` từ response
2. Paste vào browser
3. Link phải redirect đến sản phẩm đúng
4. Verify tracking được ghi nhận trên Shopee Affiliate Dashboard

---

## 🧪 Test Cases

### Test Case 1: Standard Product URL

**Input:**
```
https://shopee.vn/product/289826815/21040225881
```

**Expected Output:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Verification:**
- ✅ Click link → Redirect đến sản phẩm đúng
- ✅ Shop name hiển thị đúng
- ✅ Product details đúng

---

### Test Case 2: Short Format URL

**Input:**
```
https://shopee.vn/-i.289826815.21040225881
```

**Parsed:**
- shop_id: 289826815
- item_id: 21040225881

**Expected Output:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_2_20251102065530
```

**Verification:**
- ✅ Click link → Redirect đến sản phẩm đúng

---

### Test Case 3: Named Product URL

**Input:**
```
https://shopee.vn/Product-Name-i.289826815.21040225881
```

**Parsed:**
- shop_id: 289826815
- item_id: 21040225881

**Expected Output:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_3_20251102065540
```

**Verification:**
- ✅ Click link → Redirect đến sản phẩm đúng

---

### Test Case 4: Universal Link Format

**Input:**
```
https://shopee.vn/universal-link/21040225881
```

**Problem:**
- ❌ Thiếu shop_id trong URL

**Solution:**
- ⚠️ Cần parse metadata từ Shopee API
- ⚠️ Hoặc reject URL này
- ⚠️ Recommend user dùng full product URL

**Expected Output:**
```
ERROR: Cannot extract shop_id from universal-link format. Please use full product URL.
```

---

## 📊 Comparison: Before vs After

### Before (WRONG)

**Template:**
```
https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Generated Link:**
```
https://shopee.vn/universal-link/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Result:**
- ❌ Thiếu shop_id
- ❌ "Không thể tải Shop này"
- ❌ User không thể mua hàng
- ❌ Không có commission

---

### After (CORRECT)

**Template:**
```
https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Generated Link:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Result:**
- ✅ Đủ shop_id và item_id
- ✅ Redirect đến sản phẩm đúng
- ✅ User có thể mua hàng
- ✅ Commission được track

---

## 🚨 Important Notes

### 1. Tracking Code Encoding

**Tracking code phải được URL encode:**
```java
// Already handled in AffiliateLinkBuilder.java
replacements.put("{tracking_code}", urlEncode(trackingCode));
```

**Example:**
```
CB1_1_20251102065522 → CB1_1_20251102065522 (no special chars, no encoding needed)
CB1_1_2025-11-02     → CB1_1_2025-11-02    (has dash, might need encoding)
```

### 2. Affiliate ID Format

**Verify affiliate ID format:**
```sql
SELECT affiliate_id FROM affiliate_platform WHERE code = 'shopee';
```

**Should be:**
- ✅ `cashbee_vn_123456` (alphanumeric + underscore)
- ❌ NOT empty
- ❌ NOT null

### 3. af_siteid Parameter

**What is af_siteid?**
- AppsFlyer Site ID
- Usually `0` for default
- Check with Shopee Affiliate Dashboard if different

### 4. Sub ID Parameters

**Current support:**
- ✅ `af_sub1` - Primary tracking code

**Future support (if needed):**
- `af_sub2` - User ID
- `af_sub3` - Campaign ID
- `af_sub4` - Source
- `af_sub5` - Custom parameter

**Template with all sub IDs:**
```
https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}&af_sub2={user_id}&af_sub3={campaign_id}
```

---

## ⚠️ Potential Issues

### Issue 1: Universal Link Format in Database

**If user provides:**
```
https://shopee.vn/universal-link/21040225881
```

**Parser cannot extract shop_id!**

**Solution:**
Update validation in `CreateTrackingLinkUseCase`:
```java
if (parsedUrl.getShopId() == null || parsedUrl.getShopId().isBlank()) {
    throw new BusinessException(
        "Cannot extract shop ID from URL. " +
        "Please use full product URL format: " +
        "https://shopee.vn/product/{shop_id}/{item_id}"
    );
}
```

### Issue 2: Parser Compatibility

**Verify parser supports all formats:**

```java
// ShopeeUrlParser should parse:
// 1. https://shopee.vn/product/289826815/21040225881
// 2. https://shopee.vn/-i.289826815.21040225881
// 3. https://shopee.vn/Product-Name-i.289826815.21040225881

// Should reject:
// 4. https://shopee.vn/universal-link/21040225881 (no shop_id)
```

---

## 📝 Checklist

### Pre-Update
- [ ] Backup database
- [ ] Check current link_template
- [ ] Verify affiliate_id is configured

### Update
- [ ] Run SQL update
- [ ] Verify update successful
- [ ] Restart application (if needed)

### Post-Update Testing
- [ ] Test API with standard product URL
- [ ] Test API with short format URL
- [ ] Test API with named product URL
- [ ] Click generated links
- [ ] Verify redirect works
- [ ] Check Shopee Affiliate Dashboard for tracking

### Monitoring
- [ ] Monitor error logs
- [ ] Check API success rate
- [ ] Verify commission tracking
- [ ] Collect user feedback

---

## 🎯 Next Steps

1. **Update Database** - Run SQL migration
2. **Test Immediately** - Call API and click link
3. **Verify Tracking** - Check Shopee Affiliate Dashboard
4. **Monitor** - Watch for errors in next 24 hours
5. **Document** - Update API documentation with new link format

---

**Status:** ⚠️ Awaiting Verification from Shopee Affiliate Account
**Priority:** 🔴 HIGH - Users cannot purchase products
**Impact:** 💰 No commission being generated

---

**Last Updated:** 2025-11-02
**Next Action:** Verify link format with actual Shopee Affiliate account
