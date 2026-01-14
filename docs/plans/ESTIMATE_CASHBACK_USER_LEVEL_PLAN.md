# Plan: Update EstimateCashback API with User Level Support (v2)

## Mục tiêu
- API `estimateCashback` **yêu cầu đăng nhập**
- Tự động lấy `userLevel` từ user đang đăng nhập (không cần FE truyền)
- Tính cashback theo công thức mới dựa trên user level

## Công thức mới
```
cashback = (commission / 60) * userPercentage
```

| User Level | Rate | Ví dụ (commission = 17,009đ) |
|------------|------|------------------------------|
| NORMAL | 80% | 17,009 / 60 * 80 = **22,679đ** |
| VIP | 83% | 17,009 / 60 * 83 = **23,515đ** |
| SUPER | 85% | 17,009 / 60 * 85 = **24,074đ** |

---

## Kế hoạch triển khai

### Task 1: Thêm cashbackRate vào UserLevel enum
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/enums/UserLevel.java`

**Trước:**
```java
public enum UserLevel {
    NORMAL,
    VIP,
    SUPER
}
```

**Sau:**
```java
public enum UserLevel {
    NORMAL(80),
    VIP(83),
    SUPER(85);

    private final int cashbackRate;

    UserLevel(int cashbackRate) {
        this.cashbackRate = cashbackRate;
    }

    public int getCashbackRate() {
        return cashbackRate;
    }

    public BigDecimal getCashbackRateDecimal() {
        return BigDecimal.valueOf(cashbackRate);
    }
}
```

**Giải thích:**
- Mỗi enum value lưu trữ cashback rate của nó
- NORMAL = 80%, VIP = 83%, SUPER = 85%
- Method helper để dùng trong tính toán BigDecimal

---

### Task 2: Update AffiliateTrackingController
**File**: `cashbee-presentation/src/main/java/com/cashbee/presentation/controller/AffiliateTrackingController.java`

**Trước:**
```java
@PostMapping("/estimate-cashback")
public ResponseEntity<ApiResponse<EstimateCashbackResponse>> estimateCashback(
    @Valid @RequestBody EstimateCashbackRequest request) {

    EstimateCashbackResponse response = estimateCashbackUseCase.execute(request);
    // ...
}
```

**Sau:**
```java
@PostMapping("/estimate-cashback")
public ResponseEntity<ApiResponse<EstimateCashbackResponse>> estimateCashback(
    @Valid @RequestBody EstimateCashbackRequest request,
    @AuthenticationPrincipal Jwt jwt) {

    // Lấy user từ JWT token
    User currentUser = securityUtils.getCurrentUser(jwt);

    // Truyền userLevel vào UseCase
    EstimateCashbackResponse response = estimateCashbackUseCase.execute(
        request, currentUser.getUserLevel());
    // ...
}
```

**Giải thích:**
- Thêm `@AuthenticationPrincipal Jwt jwt` → API yêu cầu login
- Dùng `securityUtils.getCurrentUser(jwt)` để lấy User object
- Truyền `userLevel` vào UseCase

---

### Task 3: Update EstimateCashbackUseCase
**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/affiliate/EstimateCashbackUseCase.java`

**Thay đổi chính:**

1. **Đổi method signature:**
```java
// OLD
public EstimateCashbackResponse execute(EstimateCashbackRequest request)

// NEW
public EstimateCashbackResponse execute(EstimateCashbackRequest request, UserLevel userLevel)
```

2. **Đổi constant:**
```java
// OLD
private static final BigDecimal TUI3GANG_SHARE_RATE = new BigDecimal("0.6");
private static final BigDecimal CASHBEE_SHARE_RATE = new BigDecimal("0.8");

// NEW
private static final BigDecimal T3_COMMISSION_BASE = new BigDecimal("60");
```

3. **Update công thức:**
```java
// OLD
BigDecimal estimatedCashback = externalCommission
    .multiply(CASHBEE_SHARE_RATE)
    .divide(TUI3GANG_SHARE_RATE, 0, RoundingMode.HALF_UP);

// NEW
BigDecimal estimatedCashback = externalCommission
    .multiply(userLevel.getCashbackRateDecimal())
    .divide(T3_COMMISSION_BASE, 0, RoundingMode.HALF_UP);
```

**Giải thích công thức:**
```
T3 API trả về: commission = 17,009đ (đã là 60% của full commission)

Tính ngược full commission:
  fullCommission = 17,009 / 0.6 = 28,348đ

CashBee trả user X% của full commission:
  NORMAL (80%): 28,348 * 0.8 = 22,679đ
  VIP (83%):    28,348 * 0.83 = 23,529đ
  SUPER (85%):  28,348 * 0.85 = 24,096đ

Rút gọn:
  cashback = commission / 60 * userPercentage
           = 17,009 / 60 * 80 = 22,679đ
```

---

### Task 4: Update EstimateCashbackResponse
**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/affiliate/EstimateCashbackResponse.java`

**Thêm fields:**
```java
/**
 * User level of the current user.
 */
private String userLevel;

/**
 * Cashback rate percentage applied (based on user level).
 * NORMAL=80, VIP=83, SUPER=85
 */
private Integer appliedCashbackRate;
```

**Giải thích:**
- FE biết user đang ở level nào
- FE biết rate nào được áp dụng

---

### Task 5: Build và Test
```bash
./mvnw compile
```

---

## API Request/Response

### Request
```bash
curl -X POST "http://localhost:8080/api/affiliate/tracking/estimate-cashback" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"shopeeUrl": "https://shopee.vn/product/123/456"}'
```

**Lưu ý:** Không cần truyền `userLevel` - server tự lấy từ user đang login.

### Response
```json
{
  "success": true,
  "data": {
    "productName": "iPhone 15 Pro Max",
    "price": 28000000,
    "chietKhauCommission": 17009,
    "estimatedCashback": 22679,
    "cashbackRate": 0.08,
    "userLevel": "NORMAL",
    "appliedCashbackRate": 80,
    "message": "Bạn sẽ nhận được ~22.679đ khi mua sản phẩm này qua CashBee!"
  }
}
```

---

## Files cần thay đổi

| # | File | Thay đổi |
|---|------|----------|
| 1 | `UserLevel.java` | Thêm `cashbackRate` field, constructor, getter |
| 2 | `AffiliateTrackingController.java` | Thêm JWT param, lấy userLevel từ user |
| 3 | `EstimateCashbackUseCase.java` | Nhận userLevel, update công thức |
| 4 | `EstimateCashbackResponse.java` | Thêm `userLevel`, `appliedCashbackRate` |

---

## Breaking Change

⚠️ **API giờ yêu cầu authentication**

FE cần gửi JWT token trong header:
```
Authorization: Bearer <JWT_TOKEN>
```

Nếu không có token → 401 Unauthorized
