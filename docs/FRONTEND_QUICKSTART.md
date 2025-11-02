# 🚀 Frontend Quick Start Guide - Phase 5 & 6

> **Tài liệu này giúp Frontend Team phát triển độc lập mà không cần Backend**

## 📚 Tài Liệu Có Sẵn

| File | Mục đích | Khi nào dùng |
|------|----------|--------------|
| **API_PHASE5_6_TRACKING_IMPORT.md** | API Documentation đầy đủ | Khi cần xem chi tiết API specs, request/response formats |
| **MOCK_API_SERVICE.ts** | Mock API Service hoàn chỉnh | Khi phát triển frontend mà chưa có backend |
| **FRONTEND_QUICKSTART.md** | Hướng dẫn nhanh (file này) | Khi bắt đầu dự án |

---

## ⚡ Quick Start (3 phút)

### **Bước 1: Copy Mock Service**

```bash
# Copy file MOCK_API_SERVICE.ts vào project của bạn
cp MOCK_API_SERVICE.ts src/services/mock-api.ts
```

### **Bước 2: Sử dụng trong Code**

```typescript
import { mockApiService } from './services/mock-api';

// Tạo tracking link
const response = await mockApiService.createTrackingLink({
  shopeeUrl: 'https://shopee.vn/product/123/456',
  userId: 1
});

console.log(response.data.trackingUrl);
// Output: https://shopee.vn/universal-link/456?pid=cashbee_vn_123&af_sub1=CB1_100_20251101160530
```

### **Bước 3: Thay thế bằng Real API khi sẵn sàng**

```typescript
// Development: Dùng Mock
import { mockApiService as apiService } from './services/mock-api';

// Production: Dùng Real API
import { realApiService as apiService } from './services/real-api';

// Code của bạn không thay đổi!
const response = await apiService.createTrackingLink({...});
```

---

## 🎯 Use Cases Chính

### **1. User: Tạo Tracking Link**

**Mô tả:** User paste link Shopee → Hệ thống tạo affiliate link → User click để mua hàng

**API Endpoint:**
```http
POST /api/affiliate/tracking/create-link
```

**Frontend Code:**
```typescript
// Component: CreateLinkForm.tsx
import { mockApiService } from '@/services/mock-api';

async function handleCreateLink(shopeeUrl: string, userId: number) {
  try {
    const response = await mockApiService.createTrackingLink({
      shopeeUrl,
      userId,
      platformCode: 'shopee'
    });

    // Hiển thị tracking link cho user
    const { trackingUrl, estimatedCashbackRate, message } = response.data;

    alert(`Cashback: ${estimatedCashbackRate}%\n${message}`);

    // Copy vào clipboard
    navigator.clipboard.writeText(trackingUrl);

    return response.data;
  } catch (error) {
    console.error('Error:', error);
    alert('Vui lòng nhập link Shopee hợp lệ');
  }
}
```

**UI/UX Flow:**

```
┌─────────────────────────────────────────┐
│  [Nhập link Shopee vào đây...]         │
│  https://shopee.vn/product/123/456     │
│                                         │
│  [Tạo Link Kiếm Cashback] ← Button    │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  ✅ Link của bạn (Cashback: 5%)        │
│                                         │
│  https://shopee.vn/universal-link/...  │
│                                         │
│  [📋 Copy Link]  [🛒 Mua Ngay]        │
└─────────────────────────────────────────┘
```

**Mock Response:**
```json
{
  "clickId": 100,
  "trackingUrl": "https://shopee.vn/universal-link/456?...",
  "trackingCode": "CB1_100_20251101160530",
  "estimatedCashbackRate": 5.00,
  "message": "Click this link to shop on Shopee and earn cashback!"
}
```

---

### **2. User: Click Tracking Link (Redirect)**

**Mô tả:** User click vào tracking link → Redirect đến Shopee

**API Endpoint:**
```http
GET /api/affiliate/tracking/redirect/{clickId}
```

**Frontend Code:**

```typescript
// Option 1: Direct Link (Recommended - Đơn giản nhất)
<a
  href={`http://localhost:8080/api/affiliate/tracking/redirect/${clickId}`}
  target="_blank"
  rel="noopener noreferrer"
  className="btn btn-primary"
>
  🛒 Mua Ngay và Nhận {cashbackRate}% Cashback
</a>

// Option 2: Programmatic Redirect
function handleShopNow(clickId: number) {
  window.open(
    `http://localhost:8080/api/affiliate/tracking/redirect/${clickId}`,
    '_blank'
  );
}

// Option 3: Use Tracking URL Directly (Không qua redirect endpoint)
<a
  href={trackingUrl}  // URL từ createTrackingLink response
  target="_blank"
>
  Mua Ngay
</a>
```

**⚠️ Lưu Ý:**
- Option 1 (Direct Link) là tốt nhất - Browser tự động handle redirect
- Redirect endpoint trả về HTTP 302, không có JSON response
- Backend sẽ track click và redirect user đến Shopee

---

### **3. Admin: Import Orders từ CSV**

**Mô tả:** Admin upload file CSV từ Shopee → Hệ thống tạo orders → Match với clicks

**API Endpoint:**
```http
POST /api/admin/import/orders
Content-Type: multipart/form-data
```

**Frontend Code:**

```typescript
// Component: ImportOrdersForm.tsx
import { mockApiService } from '@/services/mock-api';

function ImportOrdersForm() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];

      // Validate file type
      if (!selectedFile.name.endsWith('.csv')) {
        alert('Vui lòng chọn file CSV');
        return;
      }

      setFile(selectedFile);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;

    setLoading(true);

    try {
      const response = await mockApiService.importOrders({
        file,
        platformCode: 'shopee',
        importedBy: 5, // Admin user ID
        skipDuplicates: true,
        autoMatch: true
      });

      setResult(response.data);

      // Show success notification
      alert(
        `Import thành công!\n` +
        `- Tổng: ${response.data.totalRows}\n` +
        `- Thành công: ${response.data.successCount}\n` +
        `- Matched: ${response.data.matchedCount}`
      );

    } catch (error) {
      alert('Lỗi: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Import Đơn Hàng từ Shopee</h2>

      <form onSubmit={handleSubmit}>
        <input
          type="file"
          accept=".csv"
          onChange={handleFileChange}
          disabled={loading}
        />

        <button
          type="submit"
          disabled={!file || loading}
        >
          {loading ? 'Đang import...' : 'Import Orders'}
        </button>
      </form>

      {result && (
        <ImportResultsDisplay result={result} />
      )}
    </div>
  );
}

// Component hiển thị kết quả import
function ImportResultsDisplay({ result }) {
  return (
    <div className="import-results">
      <h3>Kết Quả Import</h3>

      <div className="stats-grid">
        <StatCard
          label="Tổng Rows"
          value={result.totalRows}
          color="blue"
        />
        <StatCard
          label="Thành Công"
          value={result.successCount}
          color="green"
          percentage={result.successRate}
        />
        <StatCard
          label="Thất Bại"
          value={result.failedCount}
          color="red"
        />
        <StatCard
          label="Bỏ Qua"
          value={result.skippedCount}
          color="gray"
        />
        <StatCard
          label="Matched"
          value={result.matchedCount}
          color="purple"
        />
      </div>

      <div className="status-badge">
        Status: <span className={`status-${result.status}`}>
          {result.status}
        </span>
      </div>

      {result.errors.length > 0 && (
        <ErrorsList errors={result.errors} />
      )}
    </div>
  );
}
```

**UI/UX Flow:**

```
┌─────────────────────────────────────────┐
│  📁 Import Đơn Hàng từ Shopee           │
│                                         │
│  [Chọn File CSV...]                    │
│  📄 AffiliateCommissionReport.csv      │
│                                         │
│  [⬆️  Import Orders] ← Button          │
└─────────────────────────────────────────┘
              ↓
     (Loading 2-3 giây)
              ↓
┌─────────────────────────────────────────┐
│  ✅ Import Thành Công                   │
│                                         │
│  📊 Kết Quả:                            │
│  • Tổng: 2000 rows                     │
│  • Thành công: 1850 (92.5%)            │
│  • Thất bại: 50                        │
│  • Bỏ qua: 100                         │
│  • Matched: 1700 orders                │
│                                         │
│  ⏱️  Thời gian: 135 giây                │
│                                         │
│  [Xem Chi Tiết Lỗi] ← Collapse        │
└─────────────────────────────────────────┘
```

**Mock Response:**
```json
{
  "batchId": 42,
  "status": "COMPLETED",
  "totalRows": 2000,
  "successCount": 1850,
  "failedCount": 50,
  "skippedCount": 100,
  "matchedCount": 1700,
  "successRate": 92.5,
  "errors": [...]
}
```

---

## 🎨 UI Components Cần Thiết

### **1. CreateLinkForm Component**

**Props:**
```typescript
interface CreateLinkFormProps {
  userId: number;
  onSuccess?: (trackingLink: TrackingLinkResponse) => void;
}
```

**States:**
- `shopeeUrl: string` - URL user nhập vào
- `loading: boolean` - Đang xử lý
- `error: string | null` - Lỗi nếu có
- `result: TrackingLinkResponse | null` - Kết quả

**Features:**
- Input field với placeholder: "Paste Shopee link here..."
- Validation: Check URL có chứa "shopee.vn"
- Loading spinner khi đang tạo link
- Success message với cashback rate
- Copy to clipboard button
- "Mua Ngay" button redirect đến Shopee

### **2. TrackingLinkCard Component**

**Props:**
```typescript
interface TrackingLinkCardProps {
  trackingUrl: string;
  cashbackRate: number;
  productName?: string;
  onCopy?: () => void;
  onShopNow?: () => void;
}
```

**Features:**
- Hiển thị tracking URL (truncated)
- Hiển thị cashback rate (highlighted)
- Copy button
- Shop Now button
- Share buttons (optional)

### **3. ImportOrdersForm Component**

**Props:**
```typescript
interface ImportOrdersFormProps {
  adminUserId: number;
  onSuccess?: (result: ImportOrdersResponse) => void;
}
```

**States:**
- `file: File | null` - File CSV
- `loading: boolean` - Đang import
- `progress: number` - Progress bar (0-100)
- `result: ImportOrdersResponse | null` - Kết quả

**Features:**
- File input (accept .csv only)
- File name display
- Progress bar during import
- Result statistics display
- Error list (collapsible)
- Retry button if failed

### **4. ImportResultsCard Component**

**Props:**
```typescript
interface ImportResultsCardProps {
  result: ImportOrdersResponse;
  onViewDetails?: (batchId: number) => void;
}
```

**Features:**
- Statistics grid (total, success, failed, skipped, matched)
- Status badge (COMPLETED, PARTIAL, FAILED)
- Success rate percentage
- Duration time
- Error list with pagination
- Export errors to CSV button

---

## 🧪 Testing Scenarios

### **Test Case 1: Create Tracking Link - Happy Path**

```typescript
describe('CreateLinkForm', () => {
  it('should create tracking link successfully', async () => {
    // Arrange
    const userId = 1;
    const shopeeUrl = 'https://shopee.vn/product/123/456';

    // Act
    const result = await mockApiService.createTrackingLink({
      shopeeUrl,
      userId
    });

    // Assert
    expect(result.success).toBe(true);
    expect(result.data.trackingUrl).toContain('shopee.vn');
    expect(result.data.trackingCode).toMatch(/^CB\d+_\d+_\d+$/);
    expect(result.data.estimatedCashbackRate).toBeGreaterThan(0);
  });
});
```

### **Test Case 2: Create Tracking Link - Invalid URL**

```typescript
it('should reject invalid Shopee URL', async () => {
  const userId = 1;
  const invalidUrl = 'https://google.com';

  await expect(
    mockApiService.createTrackingLink({ shopeeUrl: invalidUrl, userId })
  ).rejects.toThrow('Invalid Shopee URL format');
});
```

### **Test Case 3: Import Orders - Success**

```typescript
it('should import orders successfully', async () => {
  const csvContent = 'ID đơn hàng,...\n251030D6GR3SET,...';
  const file = new File([csvContent], 'orders.csv', { type: 'text/csv' });

  const result = await mockApiService.importOrders({
    file,
    importedBy: 5,
    skipDuplicates: true,
    autoMatch: true
  });

  expect(result.data.status).toBe('COMPLETED');
  expect(result.data.successCount).toBeGreaterThan(0);
});
```

---

## 📱 Mobile Considerations

### **Deep Linking**

Khi user click tracking link trên mobile:

```typescript
// Detect mobile browser
function isMobileDevice(): boolean {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent
  );
}

// Handle mobile redirect
function handleMobileShopNow(trackingUrl: string) {
  if (isMobileDevice()) {
    // Try to open Shopee app first
    const appUrl = trackingUrl.replace('https://', 'shopee://');

    // Fallback to browser if app not installed
    window.location.href = appUrl;

    setTimeout(() => {
      window.location.href = trackingUrl;
    }, 1000);
  } else {
    window.open(trackingUrl, '_blank');
  }
}
```

---

## 🔐 Authentication

All API endpoints require JWT token:

```typescript
// Add token to requests
const token = localStorage.getItem('jwt_token');

// For real API (not mock)
fetch('http://localhost:8080/api/affiliate/tracking/create-link', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({...})
});
```

**Mock API Service tự động bỏ qua authentication.**

---

## 🐛 Common Issues & Solutions

### **Issue 1: CORS Error**

**Problem:** `Access-Control-Allow-Origin` error

**Solution:** Backend cần enable CORS cho frontend domain

```java
// Backend configuration (for reference)
@CrossOrigin(origins = "http://localhost:3000")
```

### **Issue 2: File Upload 413 Error**

**Problem:** "Request Entity Too Large"

**Solution:**
- Check backend max file size config
- Compress CSV before upload
- Split large files

### **Issue 3: Import Takes Too Long**

**Problem:** Import timeout after 2 minutes

**Solution:**
- Use polling để check import status
- Show progress bar
- Allow background import

```typescript
// Polling example
async function pollImportStatus(batchId: number) {
  const interval = setInterval(async () => {
    const result = await mockApiService.getImportBatch(batchId);

    if (result.data.status !== 'PROCESSING') {
      clearInterval(interval);
      // Show final result
    }
  }, 5000); // Check every 5 seconds
}
```

---

## 📊 API Response Status Codes

| Status Code | Meaning | Action |
|-------------|---------|--------|
| `200 OK` | Success | Display result |
| `201 Created` | Created | Display success message |
| `207 Multi-Status` | Partial success | Show warnings |
| `400 Bad Request` | Invalid input | Show error message |
| `401 Unauthorized` | Not logged in | Redirect to login |
| `403 Forbidden` | No permission | Show permission error |
| `404 Not Found` | Resource not found | Show not found message |
| `500 Internal Server Error` | Server error | Show retry button |

---

## 🎓 Learning Resources

1. **API Documentation:** `API_PHASE5_6_TRACKING_IMPORT.md`
   - Chi tiết đầy đủ về tất cả endpoints
   - Request/Response examples
   - Error codes
   - TypeScript types

2. **Mock Service:** `MOCK_API_SERVICE.ts`
   - Copy & paste vào project
   - Hoạt động giống real API
   - Có delay để simulate network

3. **This Guide:** `FRONTEND_QUICKSTART.md`
   - Quick start 3 phút
   - Common use cases
   - UI components
   - Testing examples

---

## 🤝 Support

Cần hỗ trợ?

- **Slack:** #cashbee-frontend channel
- **Email:** backend-team@cashbee.vn
- **GitHub Issues:** [Create Issue](https://github.com/cashbee/backend/issues)

---

## ✅ Checklist cho Frontend Team

- [ ] Copy `MOCK_API_SERVICE.ts` vào project
- [ ] Đọc qua API Documentation
- [ ] Implement CreateLinkForm component
- [ ] Implement TrackingLinkCard component
- [ ] Implement ImportOrdersForm component
- [ ] Implement ImportResultsCard component
- [ ] Test với Mock API
- [ ] Thử với các URL formats khác nhau
- [ ] Test error handling
- [ ] Test mobile responsive
- [ ] Chuẩn bị switch sang Real API

---

**Document Version:** 1.0.0
**Last Updated:** 2025-11-01
**Status:** ✅ Ready for Development

🎉 **Happy Coding!**
