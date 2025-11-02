# ✅ Shopee Official Affiliate Link Format (From Documentation)

## 📖 Source

**Official Shopee Documentation:**
https://help.shopee.vn/portal/10/article/172955

**Title:** "Hướng dẫn tạo link rút gọn cho đối tác Shopee Affiliate"

---

## 🎯 Official Link Format

### Template Structure

```
https://s.shopee.vn/an_redir?origin_link=[ENCODED_PRODUCT_URL]&affiliate_id=[YOUR_AFFILIATE_ID]&sub_id=[TRACKING_VALUES]
```

### Parameters Explained

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| `origin_link` | ✅ Yes | **URL-encoded** destination URL | `https%3A%2F%2Fshopee.vn%2Fproduct%2F...` |
| `affiliate_id` | ✅ Yes | Your affiliate publisher ID | `cashbee_vn_123456` |
| `sub_id` | ⚠️ Optional | 5 hyphen-separated tracking values | `value1-value2-value3-value4-value5` |

**Note:** Shopee automatically populates `uls_trackid` and `utm_term` parameters.

---

## 📋 Step-by-Step Process

### Step 1: Get Product URL

**Original product URL:**
```
https://shopee.vn/product/289826815/21040225881
```

### Step 2: URL Encode the Product URL

**Encode special characters:**
- `:` → `%3A`
- `/` → `%2F`

**Encoded URL:**
```
https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881
```

### Step 3: Build Shopee Redirect URL

**Template:**
```
https://s.shopee.vn/an_redir?origin_link={encoded_url}&affiliate_id={affiliate_id}&sub_id={tracking}
```

**Example:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_1_20251102065522
```

### Step 4: (Optional) Shorten with bit.ly or similar

**Before shortening:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_1_20251102065522
```

**After shortening (example):**
```
https://bit.ly/3XyZ123
```

**Note:** For CashBee, we'll use the full URL without shortening for transparency and tracking.

---

## 🔍 URL Encoding Details

### What Needs Encoding?

**Original URL:**
```
https://shopee.vn/product/289826815/21040225881
```

**Encoding map:**
```
h → h
t → t
t → t
p → p
s → s
: → %3A  ← ENCODE
/ → %2F  ← ENCODE
/ → %2F  ← ENCODE
s → s
h → h
o → o
p → p
e → e
e → e
. → .
v → v
n → n
/ → %2F  ← ENCODE
p → p
r → r
o → o
d → d
u → u
c → c
t → t
/ → %2F  ← ENCODE
2 → 2
8 → 8
9 → 9
... → ...
```

**Result:**
```
https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881
```

### Java Implementation

```java
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

String productUrl = "https://shopee.vn/product/289826815/21040225881";
String encodedUrl = URLEncoder.encode(productUrl, StandardCharsets.UTF_8);

// Result: https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881
```

---

## 📊 Sub ID Format

### What is sub_id?

**Format:**
```
sub_id=value1-value2-value3-value4-value5
```

**5 values separated by hyphens for tracking:**

| Position | Recommended Use | Example |
|----------|----------------|---------|
| value1 | Tracking Code | `CB1_1_20251102065522` |
| value2 | User ID | `user_1` |
| value3 | Campaign ID | `campaign_nov2024` |
| value4 | Source | `web_app` |
| value5 | Custom Data | `promo_blackfriday` |

### CashBee Use Case

**Simple (Current):**
```
sub_id=CB1_1_20251102065522
```

**Advanced (Future):**
```
sub_id=CB1_1_20251102065522-user_1-campaign_nov2024-web_app-promo_blackfriday
```

### Important Notes

- ⚠️ All 5 values must be present (can be empty: `value1----`)
- ⚠️ Use hyphen `-` as separator
- ⚠️ URL encode if values contain special characters

---

## ✅ Complete Examples

### Example 1: Basic Product Link

**Input:**
```
Product URL: https://shopee.vn/product/289826815/21040225881
User ID: 1
Affiliate ID: cashbee_vn_123456
```

**Process:**
```java
// 1. Encode product URL
String encodedUrl = URLEncoder.encode(
    "https://shopee.vn/product/289826815/21040225881",
    StandardCharsets.UTF_8
);
// Result: https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881

// 2. Generate tracking code
String trackingCode = "CB1_1_20251102065522";

// 3. Build affiliate URL
String affiliateUrl = String.format(
    "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
    encodedUrl,
    "cashbee_vn_123456",
    trackingCode
);
```

**Output:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_1_20251102065522
```

**When clicked:**
- Redirects to: `https://shopee.vn/product/289826815/21040225881`
- Tracking parameters added by Shopee automatically
- Commission tracked to `cashbee_vn_123456`
- Sub ID `CB1_1_20251102065522` recorded

---

### Example 2: Shop Page Link

**Input:**
```
Shop URL: https://shopee.vn/colgate.palmolive_vietnam
Affiliate ID: cashbee_vn_123456
Tracking: CB1_5_20251102070000
```

**Process:**
```java
String encodedUrl = URLEncoder.encode(
    "https://shopee.vn/colgate.palmolive_vietnam",
    StandardCharsets.UTF_8
);
// Result: https%3A%2F%2Fshopee.vn%2Fcolgate.palmolive_vietnam

String affiliateUrl = String.format(
    "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
    encodedUrl,
    "cashbee_vn_123456",
    "CB1_5_20251102070000"
);
```

**Output:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fcolgate.palmolive_vietnam&affiliate_id=cashbee_vn_123456&sub_id=CB1_5_20251102070000
```

---

### Example 3: Short Link Format

**Input:**
```
Product URL: https://shopee.vn/-i.289826815.21040225881
```

**Process:**
```java
String encodedUrl = URLEncoder.encode(
    "https://shopee.vn/-i.289826815.21040225881",
    StandardCharsets.UTF_8
);
// Result: https%3A%2F%2Fshopee.vn%2F-i.289826815.21040225881

String affiliateUrl = String.format(
    "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
    encodedUrl,
    "cashbee_vn_123456",
    "CB1_6_20251102070100"
);
```

**Output:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2F-i.289826815.21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_6_20251102070100
```

---

### Example 4: Advanced with 5 Sub ID Values

**Input:**
```
Product URL: https://shopee.vn/product/289826815/21040225881
User ID: 1
Campaign: black_friday_2024
Source: mobile_app
```

**Process:**
```java
String trackingCode = "CB1_1_20251102065522";
String userId = "user_1";
String campaign = "black_friday_2024";
String source = "mobile_app";
String custom = "first_purchase";

String subId = String.format(
    "%s-%s-%s-%s-%s",
    trackingCode,
    userId,
    campaign,
    source,
    custom
);
// Result: CB1_1_20251102065522-user_1-black_friday_2024-mobile_app-first_purchase

String encodedUrl = URLEncoder.encode(
    "https://shopee.vn/product/289826815/21040225881",
    StandardCharsets.UTF_8
);

String affiliateUrl = String.format(
    "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
    encodedUrl,
    "cashbee_vn_123456",
    URLEncoder.encode(subId, StandardCharsets.UTF_8)
);
```

**Output:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_1_20251102065522-user_1-black_friday_2024-mobile_app-first_purchase
```

---

## 🚫 What NOT to Do (Old Wrong Approach)

### ❌ WRONG Template (What We Were Using)

```
https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Generated:**
```
https://shopee.vn/universal-link/21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Problems:**
- ❌ Wrong base domain (shopee.vn instead of s.shopee.vn)
- ❌ Wrong endpoint (universal-link instead of an_redir)
- ❌ Wrong parameters (af_siteid, pid, af_sub1)
- ❌ Missing shop_id
- ❌ Not URL encoded

**Result:**
```
Error: "Không thể tải Shop này"
```

---

### ❌ WRONG Alternative (Short Format)

```
https://shopee.vn/-i.{shop_id}.{item_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}
```

**Generated:**
```
https://shopee.vn/-i.289826815.21040225881?af_siteid=0&pid=cashbee_vn_123456&af_sub1=CB1_1_20251102065522
```

**Problems:**
- ❌ Wrong parameters (af_siteid, pid, af_sub1)
- ❌ Not using Shopee redirect service
- ❌ May not track commission properly

---

## ✅ Correct Template (Official Shopee Way)

```
https://s.shopee.vn/an_redir?origin_link={ENCODED_PRODUCT_URL}&affiliate_id={AFFILIATE_ID}&sub_id={TRACKING_CODE}
```

**Generated:**
```
https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F289826815%2F21040225881&affiliate_id=cashbee_vn_123456&sub_id=CB1_1_20251102065522
```

**Benefits:**
- ✅ Official Shopee format
- ✅ Proper tracking
- ✅ Commission guaranteed
- ✅ Works with ANY product URL
- ✅ No shop_id/item_id parsing needed

---

## 🎯 Implementation Strategy

### Current Approach (WRONG)

```java
// Build URL with template replacements
String url = template
    .replace("{product_id}", itemId)
    .replace("{affiliate_id}", affiliateId)
    .replace("{tracking_code}", trackingCode);

// Result: https://shopee.vn/universal-link/21040225881?...
```

### New Approach (CORRECT)

```java
// 1. Keep original product URL
String originalUrl = "https://shopee.vn/product/289826815/21040225881";

// 2. URL encode it
String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);

// 3. Build redirect URL
String affiliateUrl = String.format(
    "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
    encodedUrl,
    affiliateId,
    trackingCode
);

// Result: https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2F...
```

---

## 📝 Database Migration

### Old Template (Delete)

```sql
UPDATE affiliate_platform
SET link_template = 'https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}'
WHERE code = 'shopee';
```

### New Template (Use This)

**Option 1: Store as template (NOT RECOMMENDED)**
```sql
-- This won't work because we need to encode the FULL URL
-- NOT recommended
```

**Option 2: Use NULL template (RECOMMENDED)**
```sql
-- Don't use template for Shopee
-- Build URL programmatically in code
UPDATE affiliate_platform
SET link_template = NULL,
    affiliate_id = 'cashbee_vn_123456'
WHERE code = 'shopee';
```

**Why NULL?**
- Cannot use simple placeholder replacement
- Need to URL encode the ENTIRE original URL
- Need to preserve original URL format (product, shop, short, etc.)
- Better to build in code with proper encoding

---

## 🧪 Testing Checklist

### Test 1: Standard Product URL
- Input: `https://shopee.vn/product/289826815/21040225881`
- Expected: Link redirects to exact product page
- Commission: Should track

### Test 2: Shop Page URL
- Input: `https://shopee.vn/colgate.palmolive_vietnam`
- Expected: Link redirects to shop page
- Commission: Should track

### Test 3: Short Format URL
- Input: `https://shopee.vn/-i.289826815.21040225881`
- Expected: Link redirects to product page
- Commission: Should track

### Test 4: Named Product URL
- Input: `https://shopee.vn/Product-Name-i.289826815.21040225881`
- Expected: Link redirects to product page
- Commission: Should track

### Test 5: Special Characters in URL
- Input: `https://shopee.vn/Áo-Thun-i.123.456?variant=789`
- Expected: Link properly encoded and redirects
- Commission: Should track

---

## 📖 References

- **Official Guide:** https://help.shopee.vn/portal/10/article/172955
- **Shopee Affiliate Portal:** https://affiliate.shopee.vn
- **URL Encoder:** https://www.urlencoder.org/

---

**Status:** ✅ Official Format Confirmed
**Source:** Shopee Help Center (Official Documentation)
**Last Verified:** 2025-11-02
