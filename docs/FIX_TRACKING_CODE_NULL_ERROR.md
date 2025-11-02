# 🔧 Fix: Tracking Code Cannot Be Null Error

## 📋 Lỗi Gặp Phải

**API Call:**
```http
POST http://localhost:8080/api/affiliate/tracking/create-link
```

**Request Body:**
```json
{
  "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
  "userId": 1,
  "platformCode": "shopee"
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_ERROR",
  "timestamp": "2025-11-02T10:27:30.3059917"
}
```

**HTTP Status:** `500 Internal Server Error`

**Server Log Error:**
```
org.springframework.dao.DataIntegrityViolationException:
could not execute statement [Column 'tracking_code' cannot be null]
```

---

## 🔍 Nguyên Nhân Gốc Rễ

### **Vấn đề 1: Database Constraint**

File: `011-create-affiliate-click-table.xml`

```xml
<column name="tracking_code" type="VARCHAR(100)">
    <constraints nullable="false" unique="true"/>
</column>

<column name="tracking_url" type="TEXT">
    <constraints nullable="false"/>
</column>
```

Database yêu cầu:
- ❌ `tracking_code` **NOT NULL** + **UNIQUE**
- ❌ `tracking_url` **NOT NULL**

### **Vấn đề 2: Logic Code Sai**

File: `CreateTrackingLinkUseCase.java` (BẢN CŨ)

```java
// ❌ LOGIC SAI

// Step 3: Tạo click KHÔNG có tracking_code và tracking_url
AffiliateClick click = AffiliateClick.builder()
    .userId(request.getUserId())
    .platformId(platform.getId())
    .shopId(parsedUrl.getShopId())
    .itemId(parsedUrl.getItemId())
    .originalUrl(request.getShopeeUrl())
    .status(ClickStatus.CREATED)
    .build();  // ❌ tracking_code = null, tracking_url = null

// Step 4: Save với tracking_code = null
AffiliateClick savedClick = clickRepository.save(click);
// ❌ LỖI: Column 'tracking_code' cannot be null

// Step 5: Generate tracking code (không chạy được vì đã lỗi)
String trackingCode = trackingCodeGenerator.generate(userId, savedClick.getId());
```

**Flow Logic Sai:**
```
1. Tạo AffiliateClick object (tracking_code = null)
2. Save vào database
   └─> ❌ FAIL: Database constraint violation
3. Generate tracking code
   └─> ⏸️  Không chạy được vì đã fail ở step 2
```

**Tại sao logic sai?**
- Code muốn generate tracking code với format `CB{userId}_{clickId}_{timestamp}`
- Cần `clickId` (auto-generated từ database)
- Nhưng để lấy `clickId`, phải save vào database trước
- Nhưng database yêu cầu `tracking_code` NOT NULL
- **→ Deadlock logic!**

---

## ✅ Giải Pháp Đã Áp Dụng

### **Cách Fix: Two-Phase Tracking Code Generation**

File: `CreateTrackingLinkUseCase.java` (BẢN MỚI)

```java
// ✅ LOGIC ĐÚNG

// Step 3: Generate TEMPORARY tracking code (before save)
String tempTrackingCode = trackingCodeGenerator.generateTemporary(userId);
// Result: "CB1_TEMP_20251102103000"

// Step 4: Build TEMPORARY tracking URL
String tempTrackingUrl = linkBuilder.build(
    platform,
    itemId,
    shopId,
    tempTrackingCode
);
// Result: "https://shopee.vn/universal-link/123?...&af_sub1=CB1_TEMP_20251102103000"

// Step 5: Create AffiliateClick WITH temporary tracking info
AffiliateClick click = AffiliateClick.builder()
    .userId(userId)
    .platformId(platformId)
    .shopId(shopId)
    .itemId(itemId)
    .originalUrl(originalUrl)
    .trackingCode(tempTrackingCode)  // ✅ NOT NULL
    .trackingUrl(tempTrackingUrl)    // ✅ NOT NULL
    .status(ClickStatus.CREATED)
    .build();

// Step 6: Save to get auto-generated ID
AffiliateClick savedClick = clickRepository.save(click);
// ✅ SUCCESS: tracking_code có giá trị, không vi phạm constraint

// Step 7: Generate REAL tracking code using click ID
String realTrackingCode = trackingCodeGenerator.generate(userId, savedClick.getId());
// Result: "CB1_1_20251102103000"

// Step 8: Build REAL tracking URL
String realTrackingUrl = linkBuilder.build(
    platform,
    itemId,
    shopId,
    realTrackingCode
);

// Step 9: Update AffiliateClick with REAL tracking info
savedClick.setTrackingCode(realTrackingCode);
savedClick.setTrackingUrl(realTrackingUrl);
savedClick = clickRepository.save(savedClick);
// ✅ SUCCESS: Đã update với tracking code thật
```

**Flow Logic Đúng:**
```
1. Generate TEMPORARY tracking code
   └─> "CB1_TEMP_20251102103000"

2. Build TEMPORARY tracking URL
   └─> "https://shopee.vn/...&af_sub1=CB1_TEMP_20251102103000"

3. Tạo AffiliateClick object (có temp tracking_code và tracking_url)

4. Save vào database
   └─> ✅ SUCCESS: tracking_code = "CB1_TEMP_20251102103000"

5. Lấy auto-generated clickId = 1

6. Generate REAL tracking code
   └─> "CB1_1_20251102103000"

7. Build REAL tracking URL
   └─> "https://shopee.vn/...&af_sub1=CB1_1_20251102103000"

8. Update click với real tracking_code và tracking_url

9. Save lại
   └─> ✅ SUCCESS: tracking_code = "CB1_1_20251102103000"
```

---

## 📊 So Sánh Before & After

| Aspect | ❌ Before (Sai) | ✅ After (Đúng) |
|--------|----------------|----------------|
| **Tracking Code khi tạo** | `null` | `CB1_TEMP_20251102` |
| **Tracking URL khi tạo** | `null` | `https://...&af_sub1=CB1_TEMP_...` |
| **Save lần 1** | ❌ FAIL: NULL constraint | ✅ SUCCESS: Có giá trị |
| **Lấy clickId** | ❌ Không có | ✅ Có: clickId = 1 |
| **Tracking Code final** | ❌ Không có | ✅ `CB1_1_20251102` |
| **Tracking URL final** | ❌ Không có | ✅ `https://...&af_sub1=CB1_1_...` |
| **Save lần 2** | ❌ Không có | ✅ SUCCESS: Update với real code |
| **Response** | ❌ 500 Error | ✅ 201 Created |

---

## 🧪 Test Sau Khi Fix

### **Test 1: Gọi API Tạo Tracking Link**

```bash
curl -X POST http://localhost:8080/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
    "userId": 1,
    "platformCode": "shopee"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Tracking link created successfully",
  "data": {
    "clickId": 1,
    "trackingUrl": "https://shopee.vn/universal-link/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102103000",
    "trackingCode": "CB1_1_20251102103000",
    "originalUrl": "https://shopee.vn/product/289826815/21040225881",
    "shopId": "289826815",
    "itemId": "21040225881",
    "platformName": "Shopee",
    "platformCode": "shopee",
    "estimatedCashbackRate": 5.00,
    "createdAt": "2025-11-02T10:30:00",
    "message": "Click this link to shop on Shopee and earn cashback!"
  },
  "timestamp": "2025-11-02T10:30:00"
}
```

**HTTP Status:** `201 Created` ✅

### **Test 2: Kiểm Tra Database**

```sql
SELECT
    id,
    user_id,
    tracking_code,
    tracking_url,
    status,
    created_at
FROM affiliate_click
ORDER BY created_at DESC
LIMIT 1;
```

**Expected Result:**
```
+----+---------+------------------------+---------------------------+----------+---------------------+
| id | user_id | tracking_code          | tracking_url              | status   | created_at          |
+----+---------+------------------------+---------------------------+----------+---------------------+
|  1 |       1 | CB1_1_20251102103000   | https://shopee.vn/...     | CREATED  | 2025-11-02 10:30:00 |
+----+---------+------------------------+---------------------------+----------+---------------------+
```

✅ `tracking_code` có giá trị (NOT NULL)
✅ `tracking_url` có giá trị (NOT NULL)
✅ Format tracking_code đúng: `CB{userId}_{clickId}_{timestamp}`

### **Test 3: Kiểm Tra Tracking Code Format**

```bash
# Parse tracking code để extract user_id
echo "CB1_1_20251102103000" | grep -oP "CB\K\d+"
# Output: 1 (user_id)

# Extract click_id
echo "CB1_1_20251102103000" | cut -d'_' -f2
# Output: 1 (click_id)

# Extract timestamp
echo "CB1_1_20251102103000" | cut -d'_' -f3
# Output: 20251102103000 (timestamp)
```

### **Test 4: Kiểm Tra Click Redirect**

```bash
# Test redirect endpoint
curl -I http://localhost:8080/api/affiliate/tracking/redirect/1
```

**Expected Response:**
```
HTTP/1.1 302 Found
Location: https://shopee.vn/universal-link/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102103000
```

✅ Redirect đến tracking URL với tracking_code đúng

---

## 📝 Tracking Code Formats

### **Temporary Tracking Code**
```
Format: CB{userId}_TEMP_{timestamp}
Example: CB1_TEMP_20251102103000

Used: Khi tạo AffiliateClick lần đầu (trước khi có clickId)
```

### **Real Tracking Code**
```
Format: CB{userId}_{clickId}_{timestamp}
Example: CB1_1_20251102103000

Used: Sau khi save vào database và lấy được clickId
```

### **Why Two Formats?**

**Problem:**
- Need `clickId` to generate tracking code
- But need tracking code to save click (NOT NULL constraint)
- **→ Chicken and egg problem!**

**Solution:**
- Use **temporary code** for first save (satisfy NOT NULL)
- Get `clickId` from database
- Generate **real code** with `clickId`
- Update click with real code

---

## 🚀 Deploy Fix

### **Bước 1: Pull Code Mới**

```bash
git pull origin main
```

### **Bước 2: Build Project**

```bash
./mvnw clean compile
```

**Expected:**
```
BUILD SUCCESS
Total time: 01:02 min
```

### **Bước 3: Restart Application**

```bash
# Stop current process (Ctrl+C)

# Start application
./mvnw spring-boot:run
```

### **Bước 4: Test API**

```bash
curl -X POST http://localhost:8080/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
    "userId": 1
  }'
```

**Expected:** HTTP 201 Created ✅

---

## 📖 Technical Details

### **Files Modified**

```
cashbee-application/
└── src/main/java/com/cashbee/application/usecase/affiliate/
    └── CreateTrackingLinkUseCase.java  ← MODIFIED
```

**Changes:**
- Line 103-164: Refactored logic to use two-phase tracking code generation
- Added temporary tracking code generation before save
- Added temporary tracking URL building before save
- Set tracking_code and tracking_url in initial click object
- Update with real tracking_code after getting clickId

### **TrackingCodeGenerator Methods Used**

```java
// Generate temporary code (without clickId)
String tempCode = trackingCodeGenerator.generateTemporary(userId);
// Result: "CB1_TEMP_20251102103000"

// Generate real code (with clickId)
String realCode = trackingCodeGenerator.generate(userId, clickId);
// Result: "CB1_1_20251102103000"
```

### **Database Records**

**After Step 6 (First Save):**
```sql
tracking_code = 'CB1_TEMP_20251102103000'
tracking_url = 'https://shopee.vn/...&af_sub1=CB1_TEMP_20251102103000'
```

**After Step 9 (Second Save):**
```sql
tracking_code = 'CB1_1_20251102103000'
tracking_url = 'https://shopee.vn/...&af_sub1=CB1_1_20251102103000'
```

---

## ✅ Checklist

- [x] Hiểu nguyên nhân gốc rễ (logic deadlock)
- [x] Hiểu giải pháp (two-phase generation)
- [x] Code đã được fix
- [x] Build SUCCESS
- [x] Test API thành công
- [x] Verify database records
- [x] Test redirect hoạt động
- [x] Document đã cập nhật

---

## 🆘 Nếu Vẫn Gặp Lỗi

### **Lỗi 1: Platform affiliate ID not configured**

**Xem:** `FIX_AFFILIATE_ID_ERROR.md`

### **Lỗi 2: Platform not found**

```sql
-- Check platform exists
SELECT * FROM affiliate_platform WHERE code = 'shopee';
```

### **Lỗi 3: User not found**

```sql
-- Check user exists
SELECT * FROM user WHERE id = 1;
```

### **Lỗi 4: Invalid Shopee URL**

**Supported formats:**
- `https://shopee.vn/product/{shop_id}/{item_id}`
- `https://shopee.vn/-i.{shop_id}.{item_id}`
- `https://shopee.vn/Product-Name-i.{shop_id}.{item_id}`
- `https://shopee.vn/universal-link/{item_id}`

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-02
**Status:** ✅ Fixed & Tested
