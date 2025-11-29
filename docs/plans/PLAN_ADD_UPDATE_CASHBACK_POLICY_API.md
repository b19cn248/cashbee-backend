# Plan: Add Update Cashback Policy API

## Mục tiêu
Bổ sung API trong `CashbackPolicyController` để admin có thể **update** cashback policy cho người dùng theo từng sàn thương mại điện tử.

---

## Review Source Code Hiện Tại (Góc nhìn Senior Developer cho người mới học OOP)

### 1. Domain Model: `CashbackPolicy.java`

**Điểm tốt:**
- ✅ **Single Responsibility**: Class chỉ làm một việc - quản lý logic cashback policy
- ✅ **Encapsulation**: Dùng Lombok `@Getter/@Setter` để kiểm soát truy cập
- ✅ **Validation logic**: Method `validate()` tập trung validation tại domain
- ✅ **Business methods**: `calculateCashback()`, `isEffectiveNow()` - logic nghiệp vụ nằm trong model

**Cần cải thiện:**
- ⚠️ `@EqualsAndHashCode(of = {"id"})`: Chỉ so sánh theo ID, có thể gây bug nếu 2 policy chưa persist (id = null)

### 2. Repository Interface: `CashbackPolicyRepository.java`

**Điểm tốt:**
- ✅ **Interface Segregation**: Chỉ định nghĩa methods cần thiết
- ✅ **Hexagonal Architecture**: Đây là PORT, implementation (Adapter) nằm ở infrastructure layer
- ✅ **Optional return**: Dùng `Optional` để tránh NullPointerException

### 3. Controller: `CashbackPolicyController.java`

**Điểm tốt:**
- ✅ **RESTful design**: GET `/api/admin/cashback-policies`
- ✅ **Dependency Injection**: Dùng constructor injection qua `@RequiredArgsConstructor`
- ✅ **API documentation**: Dùng Swagger annotations

**Thiếu sót:**
- ❌ **Không có PUT/PATCH endpoint**: Không thể update policy
- ❌ **Không có POST endpoint**: Không thể tạo policy mới
- ❌ **Không có DELETE endpoint**: Không thể xóa policy

### 4. Architecture Pattern: Clean Architecture / Hexagonal

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  CashbackPolicyController (REST API)                        │
│     ↓ gọi                                                    │
├─────────────────────────────────────────────────────────────┤
│                    APPLICATION LAYER                         │
│  UpdateCashbackPolicyUseCase (Business Logic)               │
│     ↓ sử dụng                                                │
├─────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                            │
│  CashbackPolicy (Model) + CashbackPolicyRepository (Port)   │
│     ↑ implement                                              │
├─────────────────────────────────────────────────────────────┤
│                  INFRASTRUCTURE LAYER                        │
│  CashbackPolicyRepositoryAdapter (Adapter)                  │
│  CashbackPolicyJpaRepository (Spring Data JPA)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Plan Thực Hiện

### Bước 1: Tạo Request DTO
**File:** `cashbee-application/src/main/java/com/cashbee/application/dto/cashback/UpdateCashbackPolicyRequest.java`

**Mục đích:** Nhận dữ liệu từ client, có validation annotations

```java
@Data
public class UpdateCashbackPolicyRequest {
    @NotNull(message = "Cashback rate is required")
    @DecimalMin(value = "0", message = "Cashback rate cannot be negative")
    @DecimalMax(value = "100", message = "Cashback rate cannot exceed 100%")
    private BigDecimal cashbackRate;

    private BigDecimal minOrderValue;
    private BigDecimal maxCashbackPerOrder;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
```

### Bước 2: Tạo Use Case
**File:** `cashbee-application/src/main/java/com/cashbee/application/usecase/cashback/UpdateCashbackPolicyUseCase.java`

**Mục đích:** Xử lý business logic update policy

**Logic:**
1. Tìm policy theo ID
2. Validate dữ liệu mới
3. Update các fields
4. Lưu và trả về response

### Bước 3: Cập nhật Controller
**File:** `cashbee-presentation/src/main/java/com/cashbee/presentation/controller/CashbackPolicyController.java`

**Thêm endpoints:**
- `PUT /api/admin/cashback-policies/{id}` - Update policy by ID
- `GET /api/admin/cashback-policies/{id}` - Get policy by ID (for form load)
- `GET /api/admin/cashback-policies/platform/{platformId}` - Get policies by platform

---

## Chi Tiết Thay Đổi

| # | File | Thao tác | Mô tả |
|---|------|----------|-------|
| 1 | `UpdateCashbackPolicyRequest.java` | CREATE | Request DTO với validation |
| 2 | `UpdateCashbackPolicyUseCase.java` | CREATE | Business logic update |
| 3 | `CashbackPolicyController.java` | UPDATE | Thêm PUT endpoint |

---

## API Specification

### PUT /api/admin/cashback-policies/{id}

**Request:**
```json
{
  "cashbackRate": 75.00,
  "minOrderValue": 50000,
  "maxCashbackPerOrder": 100000,
  "isActive": true,
  "priority": 10,
  "effectiveFrom": "2025-01-01T00:00:00",
  "effectiveTo": "2025-12-31T23:59:59"
}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Cashback policy updated successfully",
  "data": {
    "id": 1,
    "policyName": "VIP Shopee Policy",
    "policyCode": "VIP_SHOPEE_2025",
    "platformId": 1,
    "platformName": "Shopee",
    "userLevel": "VIP",
    "cashbackRate": 75.00,
    "minOrderValue": 50000,
    "maxCashbackPerOrder": 100000,
    "isActive": true,
    "priority": 10,
    "effectiveFrom": "2025-01-01T00:00:00",
    "effectiveTo": "2025-12-31T23:59:59"
  },
  "timestamp": "2025-11-28T15:00:00"
}
```

**Response Error (404):**
```json
{
  "success": false,
  "message": "Cashback policy not found with ID: 999",
  "data": null,
  "timestamp": "2025-11-28T15:00:00"
}
```

**Response Error (400 - Validation):**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "cashbackRate": "Cashback rate cannot exceed 100%"
  },
  "timestamp": "2025-11-28T15:00:00"
}
```

---

## Ước tính công việc

| Công việc | Độ phức tạp | Số dòng code |
|-----------|-------------|--------------|
| UpdateCashbackPolicyRequest.java | Thấp | ~40 dòng |
| UpdateCashbackPolicyUseCase.java | Trung bình | ~80 dòng |
| CashbackPolicyController.java (update) | Thấp | ~50 dòng |

**Tổng:** ~170 dòng code mới

---

## Checklist trước khi implement

- [ ] User approve plan
- [ ] Đảm bảo có đầy đủ validation
- [ ] Đảm bảo có response rõ ràng cho tất cả cases
- [ ] Compile thành công
