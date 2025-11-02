# 🔧 Fix: Platform Affiliate ID Not Configured Error

## 📋 Vấn Đề

Khi gọi API `POST /api/affiliate/tracking/create-link`, nhận được lỗi:

```json
{
  "success": false,
  "message": "Platform affiliate ID is not configured. Please contact admin.",
  "errorCode": "BUSINESS_ERROR",
  "timestamp": "2025-11-01T20:48:09.0887736"
}
```

**HTTP Status:** `400 Bad Request`

---

## 🔍 Nguyên Nhân

Platform "Shopee" trong database chưa có `affiliate_id` được cấu hình. Code validation kiểm tra:

```java
// CreateTrackingLinkUseCase.java:63
if (platform.getAffiliateId() == null || platform.getAffiliateId().isBlank()) {
    throw new BusinessException("Platform affiliate ID is not configured. Please contact admin.");
}
```

Migration 013 chỉ thêm column và update `link_template`, nhưng **quên update `affiliate_id`**.

---

## ✅ Giải Pháp (Chọn 1 trong 3 cách)

### **Cách 1: Update Database Trực Tiếp** ⚡ (Nhanh nhất - 30 giây)

**Bước 1:** Lấy Affiliate ID từ Shopee Portal

1. Đăng nhập vào [Shopee Affiliate Portal](https://shopee.vn/affiliate)
2. Vào mục **Settings** hoặc **Account Info**
3. Copy **Publisher ID** hoặc **Affiliate ID**
   - Ví dụ: `cashbee_vn_123456` hoặc `123456`

**Bước 2:** Chạy SQL

```bash
# Kết nối MySQL
mysql -u root -p cashbee_db
```

```sql
-- Update affiliate_id cho Shopee
UPDATE affiliate_platform
SET
    affiliate_id = 'YOUR_REAL_AFFILIATE_ID_HERE',
    link_template = 'https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}',
    tracking_enabled = 1
WHERE code = 'shopee';

-- Verify kết quả
SELECT
    id,
    code,
    name,
    affiliate_id,
    link_template,
    tracking_enabled
FROM affiliate_platform
WHERE code = 'shopee';
```

**⚠️ QUAN TRỌNG:** Thay `YOUR_REAL_AFFILIATE_ID_HERE` bằng Affiliate ID thật của bạn!

**Kết quả mong đợi:**
```
+----+--------+--------+---------------------+------------------+------------------+
| id | code   | name   | affiliate_id        | link_template    | tracking_enabled |
+----+--------+--------+---------------------+------------------+------------------+
|  1 | shopee | Shopee | cashbee_vn_123456   | https://shop...  |                1 |
+----+--------+--------+---------------------+------------------+------------------+
```

**Bước 3:** Test lại API

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
    "trackingUrl": "https://shopee.vn/universal-link/21040225881?...",
    "trackingCode": "CB1_1_20251101205330",
    "estimatedCashbackRate": 5.00
  }
}
```

---

### **Cách 2: Dùng Liquibase Migration** 📦 (Recommended cho Production)

Migration 014 đã được tạo sẵn. Bạn chỉ cần:

**Bước 1:** Sửa file migration

```bash
# Mở file
nano cashbee-presentation/src/main/resources/db/changelog/014-update-shopee-affiliate-id.xml
```

**Bước 2:** Thay `cashbee_vn_123456` bằng Affiliate ID thật

```xml
<update tableName="affiliate_platform">
    <column name="affiliate_id" value="YOUR_REAL_AFFILIATE_ID_HERE"/>
    <column name="link_template" value="https://shopee.vn/universal-link/{product_id}?af_siteid=0&amp;pid={affiliate_id}&amp;af_sub1={tracking_code}"/>
    <column name="tracking_enabled" value="1"/>
    <where>code='shopee'</where>
</update>
```

**Bước 3:** Restart application

```bash
# Stop application (Ctrl+C)

# Start lại
./mvnw spring-boot:run
```

Liquibase sẽ tự động:
- Detect migration mới (014)
- Chạy SQL update
- Ghi log vào `databasechangelog` table

**Bước 4:** Verify migration đã chạy

```sql
SELECT * FROM databasechangelog
WHERE id = '014'
ORDER BY dateexecuted DESC
LIMIT 1;
```

**Expected:**
```
+-----+----------+---------------------+
| id  | author   | dateexecuted        |
+-----+----------+---------------------+
| 014 | cashbee  | 2025-11-01 20:55:00 |
+-----+----------+---------------------+
```

---

### **Cách 3: Dùng API Endpoint (Admin UI)** 🎨 (Tốt nhất cho Production)

Endpoint mới đã được tạo: `PUT /api/admin/platforms/code/{code}/config`

**Bước 1:** Gọi API để update config

```bash
curl -X PUT http://localhost:8080/api/admin/platforms/code/shopee/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -d '{
    "affiliateId": "YOUR_REAL_AFFILIATE_ID_HERE",
    "linkTemplate": "https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}",
    "trackingEnabled": true
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Platform configuration updated successfully",
  "data": {
    "id": 1,
    "name": "Shopee",
    "code": "shopee",
    "status": "ACTIVE"
  }
}
```

**Bước 2:** Tạo Admin UI Form (Frontend)

```typescript
// AdminPlatformConfigForm.tsx
import { useState } from 'react';

function AdminPlatformConfigForm({ platformCode }: { platformCode: string }) {
  const [affiliateId, setAffiliateId] = useState('');
  const [linkTemplate, setLinkTemplate] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const response = await fetch(
      `http://localhost:8080/api/admin/platforms/code/${platformCode}/config`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getAdminToken()}`
        },
        body: JSON.stringify({
          affiliateId,
          linkTemplate,
          trackingEnabled: true
        })
      }
    );

    if (response.ok) {
      alert('Cấu hình thành công!');
    } else {
      alert('Lỗi: ' + (await response.text()));
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2>Cấu Hình Affiliate ID - {platformCode}</h2>

      <div>
        <label>Affiliate ID:</label>
        <input
          type="text"
          value={affiliateId}
          onChange={(e) => setAffiliateId(e.target.value)}
          placeholder="cashbee_vn_123456"
          required
        />
        <small>Lấy từ Shopee Affiliate Portal</small>
      </div>

      <div>
        <label>Link Template:</label>
        <textarea
          value={linkTemplate}
          onChange={(e) => setLinkTemplate(e.target.value)}
          placeholder="https://shopee.vn/universal-link/{product_id}?..."
          rows={3}
          required
        />
        <small>
          Placeholders: {'{product_id}'}, {'{affiliate_id}'}, {'{tracking_code}'}
        </small>
      </div>

      <button type="submit">Lưu Cấu Hình</button>
    </form>
  );
}
```

---

## 📊 So Sánh 3 Cách

| Tiêu Chí | Cách 1: SQL | Cách 2: Migration | Cách 3: API |
|----------|-------------|-------------------|-------------|
| **Tốc độ** | ⚡⚡⚡ Nhanh nhất | ⚡⚡ Trung bình | ⚡ Chậm nhất |
| **Production Ready** | ❌ Không | ✅ Có | ✅ Có |
| **Audit Trail** | ❌ Không | ✅ Có (databasechangelog) | ✅ Có (logs) |
| **User Friendly** | ❌ Cần SQL knowledge | ❌ Cần restart | ✅ UI friendly |
| **Rollback** | ❌ Khó | ✅ Dễ | ✅ Dễ |
| **Khi nào dùng** | Dev/Testing | Deployment | Production UI |

**Khuyến nghị:**
- **Development:** Dùng Cách 1 (SQL) - nhanh nhất
- **Staging/Production:** Dùng Cách 2 (Migration) - có audit trail
- **Production với Admin UI:** Dùng Cách 3 (API) - user friendly

---

## 🧪 Kiểm Tra Sau Khi Fix

### **Test 1: Kiểm tra database**

```sql
SELECT
    code,
    affiliate_id,
    link_template,
    tracking_enabled
FROM affiliate_platform
WHERE code = 'shopee';
```

**Expected:**
- `affiliate_id` không NULL
- `link_template` có placeholders
- `tracking_enabled` = 1

### **Test 2: Gọi API create link**

```bash
curl -X POST http://localhost:8080/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
    "userId": 1,
    "platformCode": "shopee"
  }'
```

**Expected:** HTTP 201 Created với tracking URL

### **Test 3: Kiểm tra tracking URL**

URL phải có format:
```
https://shopee.vn/universal-link/{ITEM_ID}?
  af_siteid=0
  &pid={YOUR_AFFILIATE_ID}
  &af_sub1={TRACKING_CODE}
```

### **Test 4: Kiểm tra database sau tạo link**

```sql
SELECT
    id,
    user_id,
    tracking_code,
    tracking_url,
    status
FROM affiliate_click
ORDER BY created_at DESC
LIMIT 1;
```

**Expected:** Có record mới với tracking_code dạng `CB1_1_20251101205330`

---

## 🚨 Các Lỗi Khác Có Thể Gặp

### **Lỗi 1: Platform Not Found**

```json
{
  "errorCode": "PLATFORM_NOT_FOUND",
  "message": "Affiliate platform not found: shopee"
}
```

**Nguyên nhân:** Platform "shopee" chưa có trong database

**Giải pháp:** Chạy migration 006-seed-affiliate-platforms.xml

```bash
./mvnw liquibase:update
```

### **Lỗi 2: Tracking Disabled**

```json
{
  "errorCode": "TRACKING_DISABLED",
  "message": "Tracking is not enabled for platform: Shopee"
}
```

**Nguyên nhân:** `tracking_enabled = 0`

**Giải pháp:**
```sql
UPDATE affiliate_platform
SET tracking_enabled = 1
WHERE code = 'shopee';
```

### **Lỗi 3: Platform Inactive**

```json
{
  "errorCode": "PLATFORM_INACTIVE",
  "message": "Platform is not active: Shopee"
}
```

**Nguyên nhân:** `status = 'INACTIVE'`

**Giải pháp:**
```sql
UPDATE affiliate_platform
SET status = 'ACTIVE'
WHERE code = 'shopee';
```

### **Lỗi 4: Invalid URL Format**

```json
{
  "errorCode": "INVALID_URL",
  "message": "Invalid Shopee URL format: URL does not match any supported pattern"
}
```

**Nguyên nhân:** URL không phải Shopee hoặc format sai

**Supported formats:**
- `https://shopee.vn/product/{shop_id}/{item_id}`
- `https://shopee.vn/-i.{shop_id}.{item_id}`
- `https://shopee.vn/Product-Name-i.{shop_id}.{item_id}`
- `https://shopee.vn/universal-link/{item_id}`

---

## 📖 Lấy Affiliate ID từ Shopee

### **Bước 1: Đăng ký Shopee Affiliate**

1. Truy cập: https://shopee.vn/affiliate
2. Đăng nhập tài khoản Shopee
3. Đăng ký làm Publisher/Affiliate
4. Chờ duyệt (thường 1-2 ngày)

### **Bước 2: Lấy Affiliate ID**

1. Đăng nhập Shopee Affiliate Portal
2. Vào mục **Dashboard** hoặc **Settings**
3. Tìm **Publisher ID** hoặc **Affiliate ID**
4. Copy ID (ví dụ: `123456` hoặc `cashbee_vn_123456`)

### **Bước 3: Test Link Template**

1. Tạo link test trên Shopee Portal
2. Copy link được generate
3. Phân tích cấu trúc:
   ```
   https://shopee.vn/universal-link/123456?
     af_siteid=0
     &pid=YOUR_AFFILIATE_ID
     &af_sub1=CUSTOM_TRACKING
   ```
4. Dùng cấu trúc này cho `link_template`

---

## 📝 Checklist

- [ ] Lấy được Affiliate ID từ Shopee Portal
- [ ] Chọn 1 trong 3 cách fix (SQL/Migration/API)
- [ ] Update `affiliate_id` trong database
- [ ] Verify affiliate_id không NULL
- [ ] Test API create-link thành công
- [ ] Kiểm tra tracking URL đúng format
- [ ] Verify tracking_code được tạo
- [ ] Test click redirect hoạt động
- [ ] Document Affiliate ID cho team

---

## 🆘 Cần Hỗ Trợ?

- **Slack:** #cashbee-backend channel
- **Email:** backend-team@cashbee.vn
- **GitHub:** [Create Issue](https://github.com/cashbee/backend/issues)

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-01
**Status:** ✅ Ready to Fix
