# 🔍 PHÂN TÍCH LỖI: DateTime Parsing Failed

**Date**: 2025-11-03
**Status**: ⚠️ **IDENTIFIED - AWAITING DECISION**

---

## 📋 LOGS

```
DEBUG: Failed to parse datetime: 10/29/2025 15:49
DEBUG: Failed to parse datetime: 10/23/2025 16:42
DEBUG: Failed to parse datetime: 10/29/2025 15:35
DEBUG: Failed to parse datetime: 10/22/2025 21:05
DEBUG: Failed to parse datetime: 10/29/2025 15:26
```

---

## 🎯 NGUYÊN NHÂN

### **ROOT CAUSE: DateTime Format Mismatch**

**Parser expects** (Line 39 trong ShopeeCSVParser.java):
```java
private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

**Format**: `yyyy-MM-dd HH:mm:ss`
**Example**: `2025-10-29 15:49:00`

---

**CSV file actually has**:
```
10/29/2025 23:14
10/29/2025 22:46
10/29/2025 22:45
10/23/2025 13:34
```

**Format**: `MM/dd/yyyy HH:mm`
**Example**: `10/29/2025 15:49`

---

### **MISMATCH TABLE**

| Aspect | Parser Expects | CSV Contains | Match? |
|--------|---------------|--------------|--------|
| **Date separator** | `-` (dash) | `/` (slash) | ❌ |
| **Date order** | `yyyy-MM-dd` | `MM/dd/yyyy` | ❌ |
| **Time format** | `HH:mm:ss` | `HH:mm` | ❌ (no seconds) |
| **Example** | `2025-10-29 15:49:00` | `10/29/2025 15:49` | ❌ |

➡️ **100% INCOMPATIBLE FORMATS**

---

## 🔍 EVIDENCE

### **1. Code Evidence**

**File**: `ShopeeCSVParser.java:246-257`

```java
private LocalDateTime parseDateTime(String value) {
    if (value == null || value.isEmpty()) {
        return null;
    }

    try {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);  // ❌ Expects "yyyy-MM-dd HH:mm:ss"
    } catch (DateTimeParseException e) {
        log.debug("Failed to parse datetime: {}", value);  // ← This is logging our errors
        return null;
    }
}
```

**Column name constants** (Line 46-48):
```java
private static final String COL_ORDER_TIME = "Thời Gian Đặt Hàng";
private static final String COL_COMPLETE_TIME = "Thời gian hoàn thành";
private static final String COL_CLICK_TIME = "Thời gian Click";
```

✅ Column names are **CORRECT** (match CSV headers exactly)

---

### **2. CSV File Evidence**

**File**: `AffiliateCommissionReport202510300813.csv`

**Headers**:
```csv
...,Thời Gian Đặt Hàng,Thời gian hoàn thành,Thời gian Click,...
```

**Sample Data** (first 3 rows):
```csv
Row 1:
  Thời Gian Đặt Hàng: 10/29/2025 23:14
  Thời gian hoàn thành: (empty)
  Thời gian Click: 10/23/2025 13:34

Row 2:
  Thời Gian Đặt Hàng: 10/29/2025 22:46
  Thời gian hoàn thành: (empty)
  Thời gian Click: 10/29/2025 12:34

Row 3:
  Thời Gian Đặt Hàng: 10/29/2025 22:45
  Thời gian hoàn thành: (empty)
  Thời gian Click: 10/25/2025 12:57
```

**Format Analysis**:
- Date separator: `/` (not `-`)
- Date order: `MM/dd/yyyy` (not `yyyy-MM-dd`)
- Time: `HH:mm` (no seconds)

---

## 💥 IMPACT

### **Current Behavior**

When parser encounters datetime values:

1. `parseDateTime("10/29/2025 15:49")` is called
2. Tries to parse with format `"yyyy-MM-dd HH:mm:ss"`
3. Throws `DateTimeParseException` because format doesn't match
4. Catches exception and logs: `DEBUG: Failed to parse datetime: 10/29/2025 15:49`
5. Returns `null`

### **Impact on Data**

**Affected fields** in `ShopeeOrderRecord`:
```java
.orderTime(parseDateTime(...))      // ❌ Always null
.completeTime(parseDateTime(...))   // ❌ Always null
.clickTime(parseDateTime(...))      // ❌ Always null
```

**Consequences**:

1. ❌ **Order time = null** → Không biết đơn hàng đặt lúc nào
2. ❌ **Complete time = null** → Không biết đơn hàng hoàn thành lúc nào
3. ❌ **Click time = null** → Không biết user click affiliate link lúc nào

**Business Impact**:
- ❌ Không thể sort orders by time
- ❌ Không thể filter orders by date range
- ❌ Không thể track conversion time (click → order → complete)
- ❌ Báo cáo thống kê thiếu dimension thời gian

---

## ✅ GIẢI PHÁP

### **Option 1: Update DateTimeFormatter (RECOMMENDED)**

**Change formatter** to match CSV format:

**File**: `ShopeeCSVParser.java:39`

**Before**:
```java
private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

**After**:
```java
private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
```

**Pros**:
- ✅ Simple fix (1 line change)
- ✅ Matches actual CSV format from Shopee
- ✅ No data transformation needed

**Cons**:
- ⚠️ If Shopee changes format later, need to update again

---

### **Option 2: Support Multiple Formats**

Try multiple formats in order:

```java
private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),      // Shopee current format
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),   // ISO-like format
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),      // European format
};

private LocalDateTime parseDateTime(String value) {
    if (value == null || value.isEmpty()) {
        return null;
    }

    for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            // Try next formatter
        }
    }

    log.debug("Failed to parse datetime with any format: {}", value);
    return null;
}
```

**Pros**:
- ✅ Robust - handles format changes
- ✅ Supports multiple CSV sources
- ✅ Future-proof

**Cons**:
- ⚠️ More complex code
- ⚠️ Slight performance overhead (tries multiple formats)

---

### **Option 3: Configurable Format**

Make format configurable via properties:

```properties
# application.yml
shopee:
  csv:
    datetime-format: "MM/dd/yyyy HH:mm"
```

```java
@Value("${shopee.csv.datetime-format}")
private String dateTimeFormatPattern;

private DateTimeFormatter getDateTimeFormatter() {
    return DateTimeFormatter.ofPattern(dateTimeFormatPattern);
}
```

**Pros**:
- ✅ Highly flexible
- ✅ No code change when format changes
- ✅ Can be different per environment

**Cons**:
- ⚠️ More complex setup
- ⚠️ Configuration management overhead

---

## 🎯 RECOMMENDED ACTION

### **Immediate Fix: Option 1**

**Why**:
- Quick to implement (1 line change)
- Matches actual Shopee CSV format
- Solves the problem completely for current data

**Change**:
```java
// Line 39 in ShopeeCSVParser.java
private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");  // ✅ Match Shopee format
```

**Test**:
```
Input: "10/29/2025 15:49"
Output: LocalDateTime.of(2025, 10, 29, 15, 49)  ✅
```

---

### **Future Enhancement: Option 2**

If Shopee changes CSV format frequently, implement Option 2 for robustness.

---

## 🧪 VERIFICATION

### **After Fix**

**Expected logs** (INFO level):
```
INFO: Parsed 182 records from CSV
INFO: Order time parsed: 2025-10-29T23:14
INFO: Click time parsed: 2025-10-23T13:34
```

**No more DEBUG logs**:
```
❌ DEBUG: Failed to parse datetime: 10/29/2025 15:49  (should be gone)
```

---

### **Database Check**

```sql
-- Check orders have non-null datetime values
SELECT
    COUNT(*) as total_orders,
    COUNT(order_time) as orders_with_time,
    COUNT(click_time) as orders_with_click_time,
    COUNT(complete_time) as orders_with_complete_time
FROM affiliate_order;

-- Expected after fix:
-- total_orders: 180+
-- orders_with_time: 180+ (not 0)
-- orders_with_click_time: 180+ (not 0)
-- orders_with_complete_time: 0-50 (many orders not completed yet)
```

---

## 📊 SUMMARY

| Aspect | Details |
|--------|---------|
| **Root Cause** | DateTime format mismatch |
| **Parser Expects** | `yyyy-MM-dd HH:mm:ss` (e.g., `2025-10-29 15:49:00`) |
| **CSV Contains** | `MM/dd/yyyy HH:mm` (e.g., `10/29/2025 15:49`) |
| **Impact** | All datetime fields = null (order_time, click_time, complete_time) |
| **Severity** | ⚠️ **MEDIUM** - Orders imported but missing time data |
| **Fix Complexity** | ✅ **SIMPLE** - 1 line change |
| **Recommended Fix** | Change formatter pattern to `"MM/dd/yyyy HH:mm"` |

---

## 🔧 TECHNICAL DETAILS

### **Java DateTimeFormatter Patterns**

| Symbol | Meaning | Example |
|--------|---------|---------|
| `yyyy` | Year (4 digits) | `2025` |
| `MM` | Month (01-12) | `10` |
| `dd` | Day (01-31) | `29` |
| `HH` | Hour (00-23) | `15` |
| `mm` | Minute (00-59) | `49` |
| `ss` | Second (00-59) | `00` |

**Current pattern**: `yyyy-MM-dd HH:mm:ss`
- Example: `2025-10-29 15:49:00`

**Required pattern**: `MM/dd/yyyy HH:mm`
- Example: `10/29/2025 15:49`

---

### **Why Format Matters**

Java's `DateTimeFormatter.parse()` is **strict**:
- It does NOT auto-detect format
- It does NOT guess missing components
- It throws exception if format doesn't match exactly

**Example**:
```java
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// ✅ Parses successfully
formatter.parse("2025-10-29 15:49:00");

// ❌ Throws DateTimeParseException
formatter.parse("10/29/2025 15:49");
```

---

## 📝 NOTES

### **About "Thời gian hoàn thành" (Complete Time)**

In the CSV sample data, most "Thời gian hoàn thành" values are **empty**.

**This is expected** because:
- Many orders are still "Đang chờ xử lý" (Pending)
- Complete time is only set when order status = "Hoàn thành" (Completed)

**Not an error** - just reflects order status.

---

### **Date Values Look Strange?**

Dates in logs show `10/29/2025`, `10/23/2025`, etc.

**Possible reasons**:
1. **Test data**: May be using future dates for testing
2. **System clock**: Computer clock may be incorrect
3. **CSV export time**: CSV exported with specific date range

**Not a parsing issue** - just the actual data in CSV.

---

## 🎉 CONCLUSION

**Problem**: Parser using wrong datetime format
**Solution**: Update format from `"yyyy-MM-dd HH:mm:ss"` to `"MM/dd/yyyy HH:mm"`
**Effort**: 1 line change
**Impact**: High - enables time-based analytics and reporting

---

**Analyzed by**: Claude Code Assistant
**Date**: 2025-11-03
**Status**: ⚠️ **AWAITING USER DECISION**

---

## 🚀 NEXT STEPS

**User to decide**:
1. ✅ Apply Option 1 (simple fix) immediately?
2. 🔄 Implement Option 2 (multiple format support) for robustness?
3. ⚙️ Go with Option 3 (configurable format) for flexibility?

**Recommended**: Start with Option 1, upgrade to Option 2 if needed later.
