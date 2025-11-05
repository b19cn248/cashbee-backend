# 📝 CẬP NHẬT TRACKING CODE CHO CSV FILE

**Date**: 2025-11-03
**Status**: ✅ **COMPLETED**

---

## 🎯 MỤC TIÊU

Cập nhật cột `Sub_id1` trong file CSV Shopee Affiliate với tracking codes để có thể test chức năng import và tính cashback.

---

## ✅ NHỮNG GÌ ĐÃ LÀM

### 1. **Phân tích File CSV Gốc**
- File: `AffiliateCommissionReport202510300813.csv`
- Tổng số dòng: 183 (bao gồm header)
- Số dòng dữ liệu: 182
- Cột `Sub_id1` (cột 42): Ban đầu trống

### 2. **Tạo Script Update Tracking Codes**
**File**: `update_tracking_codes.py`

**Chức năng**:
- Đọc CSV file với UTF-8 encoding (handle BOM)
- Tìm cột `Sub_id1`
- Update mỗi dòng với tracking code xen kẽ:
  - Dòng lẻ: `CB1_1_20251103215156`
  - Dòng chẵn: `CB1_2_20251103215300`
- Lưu vào file mới: `AffiliateCommissionReport202510300813_updated.csv`

**Code highlights**:
```python
tracking_codes = [
    "CB1_1_20251103215156",
    "CB1_2_20251103215300"
]

# Alternate between two tracking codes
tracking_code = tracking_codes[row_count % 2]
row[sub_id1_index] = tracking_code
```

### 3. **Chạy Script Update**
```bash
$ python3 update_tracking_codes.py

✅ Updated 182 rows
   - 91 rows with tracking code: CB1_1_20251103215156
   - 91 rows with tracking code: CB1_2_20251103215300
   Output saved to: AffiliateCommissionReport202510300813_updated.csv
```

### 4. **Verify Kết Quả**
**File**: `verify_tracking_codes.py`

**Kết quả**:
```bash
$ python3 verify_tracking_codes.py

✅ Found Sub_id1 at column 42

First 10 rows with tracking codes:
Row 2: CB1_1_20251103215156
Row 3: CB1_2_20251103215300
Row 4: CB1_1_20251103215156
Row 5: CB1_2_20251103215300
...

📊 Tracking code distribution:
   CB1_1_20251103215156: 91 rows
   CB1_2_20251103215300: 91 rows
```

### 5. **Backup & Replace File Gốc**
```bash
# Backup file gốc
mv AffiliateCommissionReport202510300813.csv \
   AffiliateCommissionReport202510300813_backup.csv

# Replace với file đã update
mv AffiliateCommissionReport202510300813_updated.csv \
   AffiliateCommissionReport202510300813.csv
```

---

## 📊 KẾT QUẢ

### **Files Hiện Tại:**
```
AffiliateCommissionReport202510300813.csv         (123KB) - File đã update ✅
AffiliateCommissionReport202510300813_backup.csv  (119KB) - File gốc (backup)
update_tracking_codes.py                          (2.0KB) - Script update
verify_tracking_codes.py                          (1.5KB) - Script verify
```

### **Tracking Codes Distribution:**
- **CB1_1_20251103215156**: 91 rows (50%)
- **CB1_2_20251103215300**: 91 rows (50%)
- **Tổng**: 182 rows

### **Mapping với Users:**
Theo format tracking code `CB{userId}_{clickId}_{timestamp}`:
- `CB1_1_20251103215156` → User ID = **1**, Click ID = 1
- `CB1_2_20251103215300` → User ID = **1**, Click ID = 2

➡️ **Tất cả đơn hàng sẽ được mapping về User ID = 1**

---

## 🧪 SẴN SÀNG CHO TEST

File CSV hiện tại đã sẵn sàng để test chức năng:

### **1. Import CSV**
```bash
POST /api/admin/import/orders
- File: AffiliateCommissionReport202510300813.csv
- Platform: Shopee
- Expected: 182 orders imported
```

### **2. Verify Cashback Calculation**
```sql
-- Check affiliate_order table
SELECT COUNT(*) FROM affiliate_order WHERE user_id = 1;
-- Expected: ~182 orders (some may be grouped by checkout_id)

-- Check cashback table
SELECT COUNT(*), SUM(cashback_amount)
FROM cashback WHERE user_id = 1;

-- Check user wallet
SELECT balance, pending_balance
FROM user_wallet WHERE user_id = 1;
```

### **3. Expected Results**

Với User ID = 1:
- **Đơn "Hoàn thành"**: Cashback được cộng vào `balance` ngay
- **Đơn "Đang chờ xử lý"**: Cashback được cộng vào `pending_balance`
- **Đơn "Đã hủy"**: Không tạo cashback

---

## 📝 GHI CHÚ

### **Tracking Code Format**
```
CB{userId}_{clickId}_{timestamp}
```

**Giải thích**:
- `CB`: Prefix của CashBee
- `userId`: ID của user trong database (ở đây là 1)
- `clickId`: ID của lần click (1, 2, 3,...)
- `timestamp`: YYYYMMDDHHmmss

### **Parse Tracking Code**
```java
// Trong TrackingCodeGenerator.java
public static Long extractUserId(String trackingCode) {
    // CB1_1_20251103215156 → userId = 1
    String[] parts = trackingCode.split("_");
    if (parts.length >= 2 && parts[0].startsWith("CB")) {
        return Long.parseLong(parts[0].substring(2));
    }
    return null;
}
```

### **Lưu Ý Quan Trọng**
1. **User ID = 1 phải tồn tại** trong database trước khi import
2. **Cashback policy** cho platform Shopee phải được cấu hình
3. Nếu muốn test với nhiều users, cần update tracking codes với `CB2_xxx`, `CB3_xxx`,...

---

## 🔧 SCRIPTS

### **Update Tracking Codes Mới**
Nếu cần update với user IDs khác:

```python
# update_tracking_codes.py
tracking_codes = [
    "CB2_1_20251103215156",  # User ID 2
    "CB3_1_20251103215300",  # User ID 3
    "CB4_1_20251103215400",  # User ID 4
]

# Sử dụng round-robin
tracking_code = tracking_codes[row_count % len(tracking_codes)]
```

### **Verify Script**
```bash
python3 verify_tracking_codes.py
```

---

## 🎉 HOÀN THÀNH

✅ **File CSV đã được update thành công với tracking codes**
✅ **Chia đều 182 rows cho 2 tracking codes (91 rows mỗi code)**
✅ **Tất cả đơn hàng mapping về User ID = 1**
✅ **Sẵn sàng test import và tính cashback**

---

**Created by**: Claude Code Assistant
**Date**: 2025-11-03
**Files Modified**: 1 file
**Scripts Created**: 2 files
**Total Rows Updated**: 182 rows
