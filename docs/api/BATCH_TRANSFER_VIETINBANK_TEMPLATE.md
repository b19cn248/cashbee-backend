# API Documentation: Batch Transfer Download với VietinBank Template

## Tổng quan

API download batch transfer đã được cập nhật để hỗ trợ 2 loại template ngân hàng:
1. **VPBANK** (mặc định): File Excel định dạng `.xls`
2. **VIETINBANK**: File Excel định dạng `.xlsx`

---

## API Endpoint

### Download Batch Transfer File

```
GET /api/admin/batch-transfer/download
```

#### Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `batchCode` | string | No | - | Mã batch để download. Nếu không truyền, hệ thống sẽ generate file mới với dữ liệu realtime |
| `bankTemplate` | string | No | `VPBANK` | Loại template ngân hàng. Giá trị hợp lệ: `VPBANK`, `VIETINBANK` |

#### Ví dụ Request

```bash
# Download với template VPBank (mặc định)
GET /api/admin/batch-transfer/download?batchCode=BATCH_20251128_001

# Download với template VietinBank
GET /api/admin/batch-transfer/download?batchCode=BATCH_20251128_001&bankTemplate=VIETINBANK

# Download realtime data với template VietinBank
GET /api/admin/batch-transfer/download?bankTemplate=VIETINBANK
```

#### Response

**Success Response:**
- **Content-Type:**
  - VPBANK: `application/octet-stream`
  - VIETINBANK: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition:** `attachment; filename="BATCH_20251128_001.xls"` hoặc `attachment; filename="BATCH_20251128_001.xlsx"`
- **Body:** Binary file content

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "BATCH_NOT_FOUND",
    "message": "Batch transfer export not found: BATCH_20251128_001"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "NO_ELIGIBLE_USERS",
    "message": "No users found with balance >= 50000 VND and valid bank account"
  }
}
```

---

## So sánh 2 Template

### 1. VPBank Template (`.xls`)

| Cột | Tên cột | Mô tả | Ví dụ |
|-----|---------|-------|-------|
| A | STT | Số thứ tự | 1, 2, 3... |
| B | Số Tài Khoản | Số tài khoản ngân hàng | 0123456789 |
| C | Tên Tài Khoản | Tên chủ tài khoản (giữ nguyên) | Nguyễn Văn A |
| D | Số Tiền | Số tiền chuyển (VND) | 150000 |
| E | Ngân Hàng Hưởng | Tên ngân hàng | VPBANK, VCB, ACB |
| F | Nội Dung | Nội dung chuyển khoản | Hoan tien CashBee 11/2025 |

### 2. VietinBank Template (`.xlsx`)

| Cột | Tên cột | Mô tả | Ví dụ |
|-----|---------|-------|-------|
| A | STT | Số thứ tự | 1, 2, 3... |
| B | So Tai Khoan (*) | Số tài khoản ngân hàng | 0123456789 |
| C | Ten Nguoi Huong (*) | Tên chủ tài khoản (IN HOA, không dấu) | NGUYEN VAN A |
| D | So Tien (*) | Số tiền chuyển (VND) | 150000 |
| E | Ngan Hang Thu Huong (*) | Mã ngân hàng VietinBank (8 chữ số) | 01309001 |
| F | Noi Dung | Nội dung chuyển khoản (không dấu) | Hoan tien CashBee 11/2025 |

---

## Đặc điểm VietinBank Template

### 1. Tên chủ tài khoản (Cột C)
VietinBank yêu cầu tên chủ tài khoản phải:
- **IN HOA** toàn bộ
- **Không có dấu** tiếng Việt
- **Không có ký tự "&"** (được thay bằng khoảng trắng)

**Ví dụ chuyển đổi:**
| Input | Output |
|-------|--------|
| Nguyễn Văn A | NGUYEN VAN A |
| Trần Thị B & C | TRAN THI B C |
| Lê Đức Anh | LE DUC ANH |

### 2. Mã ngân hàng VietinBank (Cột E)
Thay vì tên ngân hàng, VietinBank sử dụng mã 8 chữ số:

| Ngân hàng | Mã VietinBank |
|-----------|---------------|
| VietinBank (nội bộ) | VietinBank |
| VPBank | 01309001 |
| BIDV | 01202001 |
| Vietcombank | 01203001 |
| Techcombank | 01310001 |
| MB Bank | 01311001 |
| ACB | 01307001 |
| Sacombank | 01303001 |
| TPBank | 01320011 |
| VIB | 01314001 |
| HDBank | 01321001 |
| OCB | 01319001 |
| SHB | 01322001 |
| Eximbank | 01305001 |
| MSB | 01302001 |
| SeABank | 01317001 |
| Nam A Bank | 01316001 |
| Bac A Bank | 01701001 |
| PVcomBank | 01313001 |
| LienVietPostBank | 01323001 |
| Agribank | 01204001 |
| VietABank | 01355001 |
| Kienlongbank | 01353001 |
| Woori Bank | 01351001 |
| Public Bank | 01352001 |
| HSBC | 01401001 |
| Standard Chartered | 01402001 |
| Shinhan Bank | 01403001 |
| CIMB Bank | 01501001 |
| UOB | 01502001 |

### 3. Nội dung chuyển khoản (Cột F)
- Được chuyển đổi **không dấu** để đảm bảo tương thích

---

## Frontend Implementation Guide

### 1. Cập nhật UI Download

Thêm dropdown/radio button để chọn template:

```jsx
// React example
const [bankTemplate, setBankTemplate] = useState('VPBANK');

const handleDownload = async () => {
  const url = `/api/admin/batch-transfer/download?batchCode=${batchCode}&bankTemplate=${bankTemplate}`;

  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (response.ok) {
    const blob = await response.blob();
    const extension = bankTemplate === 'VIETINBANK' ? '.xlsx' : '.xls';
    const filename = `${batchCode}${extension}`;

    // Download file
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
  }
};

// UI
<Select value={bankTemplate} onChange={setBankTemplate}>
  <Option value="VPBANK">VPBank Template (.xls)</Option>
  <Option value="VIETINBANK">VietinBank Template (.xlsx)</Option>
</Select>

<Button onClick={handleDownload}>Download</Button>
```

### 2. Xử lý File Extension

```javascript
const getFileExtension = (template) => {
  return template === 'VIETINBANK' ? '.xlsx' : '.xls';
};

const getContentType = (template) => {
  return template === 'VIETINBANK'
    ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    : 'application/octet-stream';
};
```

### 3. Validation

```javascript
const VALID_TEMPLATES = ['VPBANK', 'VIETINBANK'];

const validateTemplate = (template) => {
  if (!VALID_TEMPLATES.includes(template.toUpperCase())) {
    console.warn(`Invalid template: ${template}, defaulting to VPBANK`);
    return 'VPBANK';
  }
  return template.toUpperCase();
};
```

---

## Wireframe UI Suggestion

```
┌─────────────────────────────────────────────────────────────┐
│                    Batch Transfer Export                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Batch Code: [BATCH_20251128_001    ] [Search]              │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Thông tin Batch                                      │   │
│  │ - Tổng số user: 25                                   │   │
│  │ - Tổng tiền: 15,500,000 VND                         │   │
│  │ - Ngày tạo: 28/11/2025 10:30:00                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Download Template:                                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ○ VPBank Template (.xls)                            │   │
│  │   - Tên tài khoản giữ nguyên                        │   │
│  │   - Cột ngân hàng: Tên ngân hàng                    │   │
│  │                                                      │   │
│  │ ○ VietinBank Template (.xlsx)                       │   │
│  │   - Tên tài khoản: IN HOA, không dấu               │   │
│  │   - Cột ngân hàng: Mã 8 chữ số                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [Download File]  [Send Email]                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Error Handling

| Error Code | HTTP Status | Mô tả | Xử lý Frontend |
|------------|-------------|-------|----------------|
| `BATCH_NOT_FOUND` | 404 | Không tìm thấy batch | Hiển thị thông báo "Không tìm thấy batch" |
| `NO_ELIGIBLE_USERS` | 400 | Không có user đủ điều kiện | Hiển thị thông báo "Không có user đủ điều kiện" |
| `EXCEL_GENERATION_FAILED` | 500 | Lỗi tạo file Excel | Hiển thị thông báo lỗi và retry |
| `INVALID_BANK_TEMPLATE` | 400 | Template không hợp lệ | Sử dụng template mặc định (VPBANK) |

---

## Testing Checklist

- [ ] Download với template VPBANK (mặc định)
- [ ] Download với template VIETINBANK
- [ ] Kiểm tra file .xls mở được trong Excel
- [ ] Kiểm tra file .xlsx mở được trong Excel
- [ ] Kiểm tra tên tài khoản trong file VietinBank đã IN HOA và không dấu
- [ ] Kiểm tra mã ngân hàng trong file VietinBank là 8 chữ số
- [ ] Kiểm tra xử lý ký tự "&" trong tên (VietinBank)
- [ ] Kiểm tra error handling khi batch không tồn tại
- [ ] Kiểm tra error handling khi không có user đủ điều kiện

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 28/11/2025 | Initial release - Thêm hỗ trợ VietinBank template |

---

## Contact

Nếu có thắc mắc, vui lòng liên hệ Backend Team.
