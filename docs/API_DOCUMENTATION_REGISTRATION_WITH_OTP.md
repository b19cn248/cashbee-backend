# Tài liệu API - Luồng Đăng Ký Với OTP Email Verification

## 📋 Mục lục
1. [Tổng quan thay đổi](#tổng-quan-thay-đổi)
2. [So sánh luồng cũ vs mới](#so-sánh-luồng-cũ-vs-mới)
3. [Chi tiết API Endpoints](#chi-tiết-api-endpoints)
4. [Request/Response Models](#requestresponse-models)
5. [Error Handling](#error-handling)
6. [State Management cho Mobile](#state-management-cho-mobile)
7. [UI/UX Flow](#uiux-flow)
8. [Mock Data để Test](#mock-data-để-test)
9. [Security Considerations](#security-considerations)
10. [FAQ](#faq)

---

## 🔄 Tổng quan thay đổi

### Thay đổi chính
**QUAN TRỌNG:** Luồng đăng ký giờ đây là **2 bước (two-step process)** thay vì 1 bước như trước.

**Trước đây (Old Flow):**
```
User điền form → POST /api/auth/register → 201 Created → User đã đăng ký xong
```

**Bây giờ (New Flow):**
```
User điền form → POST /api/auth/register → 200 OK (cần verify OTP)
              → User nhập OTP → POST /api/auth/verify-otp → 201 Created → Đăng ký thành công
```

### Lợi ích
- ✅ Xác thực email người dùng thực sự tồn tại
- ✅ Ngăn chặn spam và fake accounts
- ✅ Tăng bảo mật cho hệ thống
- ✅ Tuân thủ best practices trong ngành

---

## 📊 So sánh luồng cũ vs mới

### Luồng Cũ (Legacy - KHÔNG còn hoạt động)

| Bước | Action | Endpoint | Response | Screen |
|------|--------|----------|----------|--------|
| 1 | User điền form đăng ký | - | - | Register Screen |
| 2 | Submit form | `POST /api/auth/register` | `201 Created` với user data | - |
| 3 | Tự động đăng nhập | - | - | Home Screen |

**Response cũ:**
```json
{
  "status": "success",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0123456789",
    "referralCode": "JOHN2024ABC",
    "wallet": {
      "id": "wallet-123",
      "balance": 0,
      "currency": "VND"
    }
  },
  "message": "Đăng ký thành công"
}
```

### Luồng Mới (Current - Bắt buộc từ nay)

| Bước | Action | Endpoint | Response | Screen | Thời gian |
|------|--------|----------|----------|--------|-----------|
| 1 | User điền form đăng ký | - | - | Register Screen | - |
| 2 | Submit form | `POST /api/auth/register` | `200 OK` với `requiresOtp=true` | - | - |
| 3 | Hiển thị OTP screen | - | - | OTP Verification Screen | 5 phút |
| 4 | User nhận OTP qua email | - | - | Email App | - |
| 5 | User nhập OTP | `POST /api/auth/verify-otp` | `201 Created` với user data | - | - |
| 6 | Đăng ký thành công | - | - | Welcome/Home Screen | - |

**Response mới (Step 2):**
```json
{
  "status": "success",
  "data": {
    "email": "john@example.com",
    "maskedEmail": "j***@ex***ple.com",
    "requiresOtp": true,
    "expiresIn": 300,
    "message": "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư và nhập mã OTP để hoàn tất đăng ký."
  }
}
```

**Response mới (Step 5):**
```json
{
  "status": "success",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0123456789",
    "referralCode": "JOHN2024ABC",
    "wallet": {
      "id": "wallet-123",
      "balance": 0,
      "currency": "VND"
    }
  },
  "message": "Xác thực email thành công! Chào mừng bạn đến với CashBee"
}
```

---

## 🔌 Chi tiết API Endpoints

### 1. POST /api/auth/register (UPDATED)

**Mục đích:** Bước 1 - Khởi tạo đăng ký và gửi OTP

**URL:** `https://cashbee.nguocchieuvangle.io.vn/api/auth/register`

**Method:** `POST`

**Authentication:** Không cần (Public endpoint)

**Content-Type:** `application/json`

#### Request Body
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "fullName": "John Doe",
  "phone": "0123456789",
  "referredBy": "FRIEND2024XYZ"
}
```

#### Request Fields

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `username` | string | ✅ Yes | 3-50 chars, alphanumeric + underscore | Tên đăng nhập duy nhất |
| `email` | string | ✅ Yes | Valid email format | Email để nhận OTP |
| `password` | string | ✅ Yes | Min 8 chars, 1 uppercase, 1 number, 1 special | Mật khẩu |
| `fullName` | string | ✅ Yes | 2-100 chars | Họ và tên đầy đủ |
| `phone` | string | ✅ Yes | 10-11 digits, Vietnamese format | Số điện thoại |
| `referredBy` | string | ❌ No | Valid referral code | Mã giới thiệu (nếu có) |

#### Success Response (200 OK)

**⚠️ CHÚ Ý:** Response code là `200 OK` KHÔNG phải `201 Created` như trước

```json
{
  "status": "success",
  "data": {
    "email": "john@example.com",
    "maskedEmail": "j***@ex***ple.com",
    "requiresOtp": true,
    "expiresIn": 300
  },
  "message": "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư và nhập mã OTP để hoàn tất đăng ký."
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `email` | string | Email đầy đủ (để gửi lại OTP) |
| `maskedEmail` | string | Email đã che (để hiển thị UI: "j\*\*\*@ex\*\*\*ple.com") |
| `requiresOtp` | boolean | Luôn là `true` - yêu cầu verify OTP |
| `expiresIn` | integer | Số giây OTP còn hiệu lực (300 = 5 phút) |

#### Error Responses

**400 Bad Request - Email đã tồn tại**
```json
{
  "status": "error",
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."
  }
}
```

**400 Bad Request - Username đã tồn tại**
```json
{
  "status": "error",
  "error": {
    "code": "USERNAME_ALREADY_EXISTS",
    "message": "Tên đăng nhập này đã được sử dụng. Vui lòng chọn tên khác."
  }
}
```

**400 Bad Request - Mã giới thiệu không hợp lệ**
```json
{
  "status": "error",
  "error": {
    "code": "INVALID_REFERRAL_CODE",
    "message": "Mã giới thiệu không hợp lệ hoặc đã hết hạn."
  }
}
```

**400 Bad Request - Validation errors**
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": {
      "email": "Email không đúng định dạng",
      "password": "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, số và ký tự đặc biệt"
    }
  }
}
```

---

### 2. POST /api/auth/verify-otp (NEW)

**Mục đích:** Bước 2 - Xác thực OTP và hoàn tất đăng ký

**URL:** `https://cashbee.nguocchieuvangle.io.vn/api/auth/verify-otp`

**Method:** `POST`

**Authentication:** Không cần (Public endpoint)

**Content-Type:** `application/json`

#### Request Body
```json
{
  "email": "john@example.com",
  "otpCode": "123456"
}
```

#### Request Fields

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `email` | string | ✅ Yes | Valid email | Email đã dùng để đăng ký |
| `otpCode` | string | ✅ Yes | Exactly 6 digits | Mã OTP 6 số nhận được qua email |

#### Success Response (201 Created)

```json
{
  "status": "success",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0123456789",
    "referralCode": "JOHN2024ABC",
    "createdAt": "2024-01-15T10:30:00Z",
    "wallet": {
      "id": "wallet-123",
      "balance": 0,
      "currency": "VND",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  },
  "message": "Xác thực email thành công! Chào mừng bạn đến với CashBee"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string (UUID) | ID người dùng |
| `username` | string | Tên đăng nhập |
| `email` | string | Email đã xác thực |
| `fullName` | string | Họ và tên đầy đủ |
| `phone` | string | Số điện thoại |
| `referralCode` | string | Mã giới thiệu của user (để chia sẻ) |
| `createdAt` | string (ISO 8601) | Thời gian tạo tài khoản |
| `wallet` | object | Thông tin ví điện tử |
| `wallet.id` | string | ID ví |
| `wallet.balance` | number | Số dư (luôn là 0 khi mới tạo) |
| `wallet.currency` | string | Đơn vị tiền tệ (VND) |

#### Error Responses

**400 Bad Request - OTP không tồn tại**
```json
{
  "status": "error",
  "error": {
    "code": "OTP_NOT_FOUND",
    "message": "Không tìm thấy mã OTP cho email này. Vui lòng đăng ký lại."
  }
}
```

**400 Bad Request - OTP đã hết hạn**
```json
{
  "status": "error",
  "error": {
    "code": "OTP_EXPIRED",
    "message": "Mã OTP đã hết hạn. Vui lòng nhấn 'Gửi lại mã' để nhận mã mới.",
    "canResend": true
  }
}
```

**400 Bad Request - OTP sai**
```json
{
  "status": "error",
  "error": {
    "code": "INVALID_OTP",
    "message": "Mã OTP không chính xác. Bạn còn 2 lần thử.",
    "attemptsRemaining": 2
  }
}
```

**400 Bad Request - Quá số lần thử**
```json
{
  "status": "error",
  "error": {
    "code": "MAX_ATTEMPTS_EXCEEDED",
    "message": "Bạn đã nhập sai OTP quá 3 lần. Vui lòng nhấn 'Gửi lại mã' để nhận mã mới.",
    "canResend": true
  }
}
```

**400 Bad Request - OTP đã được sử dụng**
```json
{
  "status": "error",
  "error": {
    "code": "OTP_ALREADY_VERIFIED",
    "message": "Mã OTP này đã được sử dụng. Tài khoản của bạn đã được kích hoạt."
  }
}
```

---

### 3. POST /api/auth/resend-otp (NEW)

**Mục đích:** Gửi lại mã OTP mới

**URL:** `https://cashbee.nguocchieuvangle.io.vn/api/auth/resend-otp`

**Method:** `POST`

**Authentication:** Không cần (Public endpoint)

**Content-Type:** `application/json`

#### Request Body
```json
{
  "email": "john@example.com"
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | ✅ Yes | Email đã dùng để đăng ký |

#### Success Response (200 OK)

```json
{
  "status": "success",
  "data": {
    "email": "john@example.com",
    "maskedEmail": "j***@ex***ple.com",
    "expiresIn": 300,
    "canResendAt": "2024-01-15T10:31:00Z"
  },
  "message": "Mã OTP mới đã được gửi đến email của bạn."
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `email` | string | Email đầy đủ |
| `maskedEmail` | string | Email đã che |
| `expiresIn` | integer | Số giây OTP mới còn hiệu lực (300 = 5 phút) |
| `canResendAt` | string (ISO 8601) | Thời điểm có thể gửi lại lần tiếp theo |

#### Error Responses

**400 Bad Request - OTP không tồn tại**
```json
{
  "status": "error",
  "error": {
    "code": "OTP_NOT_FOUND",
    "message": "Không tìm thấy yêu cầu OTP cho email này. Vui lòng đăng ký lại."
  }
}
```

**429 Too Many Requests - Cooldown chưa hết**
```json
{
  "status": "error",
  "error": {
    "code": "RESEND_COOLDOWN_ACTIVE",
    "message": "Vui lòng đợi 25 giây trước khi gửi lại mã OTP.",
    "retryAfterSeconds": 25,
    "canResendAt": "2024-01-15T10:31:00Z"
  }
}
```

**429 Too Many Requests - Quá số lần gửi trong ngày**
```json
{
  "status": "error",
  "error": {
    "code": "DAILY_LIMIT_EXCEEDED",
    "message": "Bạn đã vượt quá giới hạn 5 lần gửi OTP trong ngày. Vui lòng thử lại sau 24 giờ.",
    "retryAfterHours": 18
  }
}
```

**400 Bad Request - OTP đã được xác thực**
```json
{
  "status": "error",
  "error": {
    "code": "OTP_ALREADY_VERIFIED",
    "message": "Email này đã được xác thực. Vui lòng đăng nhập."
  }
}
```

---

## 📦 Request/Response Models

### TypeScript/JavaScript Models

```typescript
// ==================== REGISTER ====================

interface RegisterRequest {
  username: string;        // 3-50 chars
  email: string;          // Valid email
  password: string;       // Min 8 chars, complex
  fullName: string;       // 2-100 chars
  phone: string;          // 10-11 digits
  referredBy?: string;    // Optional referral code
}

interface RegisterResponse {
  email: string;          // Full email
  maskedEmail: string;    // "j***@ex***ple.com"
  requiresOtp: boolean;   // Always true
  expiresIn: number;      // 300 seconds (5 minutes)
}

// ==================== VERIFY OTP ====================

interface VerifyOtpRequest {
  email: string;          // Same email from register
  otpCode: string;        // 6 digits
}

interface VerifyOtpResponse {
  id: string;             // UUID
  username: string;
  email: string;
  fullName: string;
  phone: string;
  referralCode: string;   // User's own referral code
  createdAt: string;      // ISO 8601
  wallet: Wallet;
}

interface Wallet {
  id: string;
  balance: number;        // Always 0 for new users
  currency: string;       // "VND"
  createdAt: string;      // ISO 8601
}

// ==================== RESEND OTP ====================

interface ResendOtpRequest {
  email: string;
}

interface ResendOtpResponse {
  email: string;
  maskedEmail: string;
  expiresIn: number;      // 300 seconds
  canResendAt: string;    // ISO 8601
}

// ==================== ERROR ====================

interface ErrorResponse {
  status: "error";
  error: {
    code: string;         // Error code (see Error Codes section)
    message: string;      // User-friendly message in Vietnamese
    details?: any;        // Optional additional details
    attemptsRemaining?: number;
    retryAfterSeconds?: number;
    retryAfterHours?: number;
    canResend?: boolean;
  };
}

// ==================== COMMON ====================

interface ApiResponse<T> {
  status: "success" | "error";
  data?: T;
  error?: ErrorResponse["error"];
  message?: string;
}
```

### Kotlin/Java Models (Android)

```kotlin
// ==================== REGISTER ====================

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String,
    val referredBy: String? = null
)

data class RegisterResponse(
    val email: String,
    val maskedEmail: String,
    val requiresOtp: Boolean,
    val expiresIn: Int
)

// ==================== VERIFY OTP ====================

data class VerifyOtpRequest(
    val email: String,
    val otpCode: String
)

data class VerifyOtpResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val referralCode: String,
    val createdAt: String,
    val wallet: Wallet
)

data class Wallet(
    val id: String,
    val balance: Double,
    val currency: String,
    val createdAt: String
)

// ==================== RESEND OTP ====================

data class ResendOtpRequest(
    val email: String
)

data class ResendOtpResponse(
    val email: String,
    val maskedEmail: String,
    val expiresIn: Int,
    val canResendAt: String
)

// ==================== ERROR ====================

data class ErrorResponse(
    val status: String = "error",
    val error: ErrorDetail
)

data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
    val attemptsRemaining: Int? = null,
    val retryAfterSeconds: Int? = null,
    val retryAfterHours: Int? = null,
    val canResend: Boolean? = null
)

// ==================== COMMON ====================

data class ApiResponse<T>(
    val status: String,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val message: String? = null
)
```

### Swift Models (iOS)

```swift
// ==================== REGISTER ====================

struct RegisterRequest: Codable {
    let username: String
    let email: String
    let password: String
    let fullName: String
    let phone: String
    let referredBy: String?
}

struct RegisterResponse: Codable {
    let email: String
    let maskedEmail: String
    let requiresOtp: Bool
    let expiresIn: Int
}

// ==================== VERIFY OTP ====================

struct VerifyOtpRequest: Codable {
    let email: String
    let otpCode: String
}

struct VerifyOtpResponse: Codable {
    let id: String
    let username: String
    let email: String
    let fullName: String
    let phone: String
    let referralCode: String
    let createdAt: String
    let wallet: Wallet
}

struct Wallet: Codable {
    let id: String
    let balance: Double
    let currency: String
    let createdAt: String
}

// ==================== RESEND OTP ====================

struct ResendOtpRequest: Codable {
    let email: String
}

struct ResendOtpResponse: Codable {
    let email: String
    let maskedEmail: String
    let expiresIn: Int
    let canResendAt: String
}

// ==================== ERROR ====================

struct ErrorResponse: Codable {
    let status: String
    let error: ErrorDetail
}

struct ErrorDetail: Codable {
    let code: String
    let message: String
    let details: [String: String]?
    let attemptsRemaining: Int?
    let retryAfterSeconds: Int?
    let retryAfterHours: Int?
    let canResend: Bool?
}

// ==================== COMMON ====================

struct ApiResponse<T: Codable>: Codable {
    let status: String
    let data: T?
    let error: ErrorDetail?
    let message: String?
}
```

---

## ⚠️ Error Handling

### Error Codes Reference

| Error Code | HTTP Status | Meaning | Action Required |
|------------|-------------|---------|-----------------|
| `EMAIL_ALREADY_EXISTS` | 400 | Email đã được đăng ký | Hiển thị lỗi, suggest đăng nhập |
| `USERNAME_ALREADY_EXISTS` | 400 | Username đã tồn tại | Hiển thị lỗi, suggest username khác |
| `INVALID_REFERRAL_CODE` | 400 | Mã giới thiệu không hợp lệ | Hiển thị lỗi, cho phép bỏ qua |
| `VALIDATION_ERROR` | 400 | Dữ liệu không hợp lệ | Hiển thị lỗi từng field |
| `OTP_NOT_FOUND` | 400 | Không tìm thấy OTP | Quay về màn đăng ký |
| `OTP_EXPIRED` | 400 | OTP hết hạn | Hiển thị nút "Gửi lại mã" |
| `INVALID_OTP` | 400 | OTP sai | Hiển thị số lần thử còn lại |
| `MAX_ATTEMPTS_EXCEEDED` | 400 | Quá 3 lần thử | Hiển thị nút "Gửi lại mã" |
| `OTP_ALREADY_VERIFIED` | 400 | OTP đã dùng | Chuyển đến màn đăng nhập |
| `RESEND_COOLDOWN_ACTIVE` | 429 | Chưa đủ 30 giây | Hiển thị countdown timer |
| `DAILY_LIMIT_EXCEEDED` | 429 | Quá 5 lần/ngày | Hiển thị thông báo, đợi 24h |

### Error Handling Strategy

#### 1. Network Errors
```typescript
try {
  const response = await api.register(data);
} catch (error) {
  if (error.networkError) {
    // No internet connection
    showError("Không có kết nối internet. Vui lòng kiểm tra và thử lại.");
  } else if (error.timeout) {
    // Request timeout
    showError("Kết nối quá chậm. Vui lòng thử lại.");
  }
}
```

#### 2. Validation Errors (400 - VALIDATION_ERROR)
```typescript
if (error.code === "VALIDATION_ERROR") {
  // Display field-specific errors
  const details = error.details;
  if (details.email) {
    emailField.setError(details.email);
  }
  if (details.password) {
    passwordField.setError(details.password);
  }
}
```

#### 3. OTP Errors
```typescript
// OTP expired
if (error.code === "OTP_EXPIRED") {
  showResendButton();
  showMessage("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
}

// Invalid OTP
if (error.code === "INVALID_OTP") {
  const remaining = error.attemptsRemaining;
  showMessage(`Mã OTP không chính xác. Bạn còn ${remaining} lần thử.`);
  clearOtpInput();
}

// Max attempts exceeded
if (error.code === "MAX_ATTEMPTS_EXCEEDED") {
  showResendButton();
  disableOtpInput();
  showMessage("Bạn đã nhập sai quá 3 lần. Vui lòng gửi lại mã mới.");
}
```

#### 4. Rate Limiting (429)
```typescript
// Resend cooldown
if (error.code === "RESEND_COOLDOWN_ACTIVE") {
  const seconds = error.retryAfterSeconds;
  startCountdownTimer(seconds);
  showMessage(`Vui lòng đợi ${seconds} giây trước khi gửi lại.`);
}

// Daily limit
if (error.code === "DAILY_LIMIT_EXCEEDED") {
  hideResendButton();
  showMessage("Bạn đã vượt quá giới hạn 5 lần gửi OTP trong ngày. Vui lòng thử lại sau 24 giờ.");
}
```

---

## 📱 State Management cho Mobile

### Registration State Machine

```typescript
type RegistrationState =
  | "IDLE"                    // Chưa bắt đầu
  | "SUBMITTING"              // Đang gửi form đăng ký
  | "OTP_SENT"                // OTP đã gửi, chờ nhập
  | "VERIFYING_OTP"           // Đang verify OTP
  | "RESENDING_OTP"           // Đang gửi lại OTP
  | "SUCCESS"                 // Đăng ký thành công
  | "ERROR";                  // Có lỗi xảy ra

interface RegistrationStore {
  state: RegistrationState;
  email: string | null;
  maskedEmail: string | null;
  otpExpiresAt: Date | null;
  otpAttemptsRemaining: number;
  canResendAt: Date | null;
  error: ErrorDetail | null;
  userData: VerifyOtpResponse | null;
}
```

### State Transitions

```
IDLE
  ↓ (submit registration)
SUBMITTING
  ↓ (success)
OTP_SENT ← (resend OTP) → RESENDING_OTP
  ↓ (submit OTP)
VERIFYING_OTP
  ↓ (success)
SUCCESS

Any state → ERROR (on error)
ERROR → IDLE (reset) or OTP_SENT (retry OTP)
```

### Example State Management (Redux/MobX)

```typescript
class RegistrationStore {
  @observable state: RegistrationState = "IDLE";
  @observable email: string | null = null;
  @observable maskedEmail: string | null = null;
  @observable otpExpiresAt: Date | null = null;
  @observable otpAttemptsRemaining: number = 3;
  @observable canResendAt: Date | null = null;
  @observable error: ErrorDetail | null = null;
  @observable userData: VerifyOtpResponse | null = null;

  @action
  async register(data: RegisterRequest) {
    this.state = "SUBMITTING";
    this.error = null;

    try {
      const response = await api.register(data);

      this.state = "OTP_SENT";
      this.email = response.data.email;
      this.maskedEmail = response.data.maskedEmail;
      this.otpExpiresAt = new Date(Date.now() + response.data.expiresIn * 1000);
      this.otpAttemptsRemaining = 3;

    } catch (error) {
      this.state = "ERROR";
      this.error = error.response.data.error;
    }
  }

  @action
  async verifyOtp(otpCode: string) {
    this.state = "VERIFYING_OTP";
    this.error = null;

    try {
      const response = await api.verifyOtp({
        email: this.email!,
        otpCode
      });

      this.state = "SUCCESS";
      this.userData = response.data;

    } catch (error) {
      const errorData = error.response.data.error;

      if (errorData.code === "INVALID_OTP") {
        this.state = "OTP_SENT"; // Stay on OTP screen
        this.otpAttemptsRemaining = errorData.attemptsRemaining || 0;
      } else {
        this.state = "ERROR";
      }

      this.error = errorData;
    }
  }

  @action
  async resendOtp() {
    this.state = "RESENDING_OTP";
    this.error = null;

    try {
      const response = await api.resendOtp({
        email: this.email!
      });

      this.state = "OTP_SENT";
      this.maskedEmail = response.data.maskedEmail;
      this.otpExpiresAt = new Date(Date.now() + response.data.expiresIn * 1000);
      this.otpAttemptsRemaining = 3; // Reset attempts
      this.canResendAt = new Date(response.data.canResendAt);

    } catch (error) {
      this.state = "OTP_SENT"; // Stay on OTP screen
      this.error = error.response.data.error;
    }
  }

  @action
  reset() {
    this.state = "IDLE";
    this.email = null;
    this.maskedEmail = null;
    this.otpExpiresAt = null;
    this.otpAttemptsRemaining = 3;
    this.canResendAt = null;
    this.error = null;
    this.userData = null;
  }

  @computed
  get isOtpExpired(): boolean {
    return this.otpExpiresAt ? new Date() > this.otpExpiresAt : false;
  }

  @computed
  get canResendNow(): boolean {
    return this.canResendAt ? new Date() >= this.canResendAt : true;
  }

  @computed
  get secondsUntilExpiry(): number {
    if (!this.otpExpiresAt) return 0;
    const diff = this.otpExpiresAt.getTime() - Date.now();
    return Math.max(0, Math.floor(diff / 1000));
  }

  @computed
  get secondsUntilCanResend(): number {
    if (!this.canResendAt) return 0;
    const diff = this.canResendAt.getTime() - Date.now();
    return Math.max(0, Math.floor(diff / 1000));
  }
}
```

---

## 🎨 UI/UX Flow

### Screen 1: Registration Form

**Layout:**
```
┌─────────────────────────────────┐
│     CashBee Logo                │
│                                 │
│  Đăng ký tài khoản              │
│                                 │
│  [Username Input]               │
│  [Email Input]                  │
│  [Password Input]               │
│  [Full Name Input]              │
│  [Phone Input]                  │
│  [Referral Code Input] (opt)    │
│                                 │
│  [  Đăng ký  ]                  │
│                                 │
│  Đã có tài khoản? Đăng nhập     │
└─────────────────────────────────┘
```

**Behavior:**
- Click "Đăng ký" → Call `POST /api/auth/register`
- Success (200 OK) → Navigate to OTP Screen
- Error → Display inline errors

**Validation:**
- Username: 3-50 chars, no spaces
- Email: Valid email format
- Password: Min 8 chars, 1 uppercase, 1 number, 1 special
- Full Name: 2-100 chars
- Phone: 10-11 digits, Vietnamese format

---

### Screen 2: OTP Verification (NEW)

**Layout:**
```
┌─────────────────────────────────┐
│     [< Back]                    │
│                                 │
│  Xác thực email                 │
│                                 │
│  Chúng tôi đã gửi mã OTP đến    │
│  j***@ex***ple.com              │
│                                 │
│  ┌───┬───┬───┬───┬───┬───┐     │
│  │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │     │
│  └───┴───┴───┴───┴───┴───┘     │
│                                 │
│  Mã OTP hết hạn sau: 04:35      │
│  Còn 3 lần thử                  │
│                                 │
│  [  Xác nhận  ]                 │
│                                 │
│  Không nhận được mã?            │
│  Gửi lại (30s)                  │
│                                 │
│  ⓘ Vui lòng kiểm tra cả         │
│    thư mục Spam/Junk            │
└─────────────────────────────────┘
```

**Behavior:**
- Auto-focus on first OTP input
- Auto-advance to next input on type
- Auto-submit when 6 digits entered
- Timer countdown from 5:00 to 0:00
- "Gửi lại" button:
  - Disabled for 30 seconds after sent
  - Show countdown "Gửi lại (25s)"
  - Enabled after 30 seconds → "Gửi lại"
- Success → Navigate to Welcome/Home Screen
- Error → Show error message, clear inputs

**Error States:**
1. **OTP sai:**
   - Show: "Mã OTP không chính xác. Bạn còn 2 lần thử."
   - Clear inputs
   - Keep focus on first input

2. **OTP hết hạn:**
   - Show: "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới."
   - Enable "Gửi lại" button immediately
   - Disable OTP inputs

3. **Quá số lần thử:**
   - Show: "Bạn đã nhập sai quá 3 lần. Vui lòng gửi lại mã mới."
   - Enable "Gửi lại" button
   - Disable OTP inputs

**Timer Logic:**
```typescript
function startOtpTimer(expiresIn: number) {
  let remaining = expiresIn; // 300 seconds

  const interval = setInterval(() => {
    remaining--;

    const minutes = Math.floor(remaining / 60);
    const seconds = remaining % 60;
    updateTimerDisplay(`${minutes}:${seconds.toString().padStart(2, '0')}`);

    if (remaining <= 0) {
      clearInterval(interval);
      onOtpExpired();
    }
  }, 1000);
}

function onOtpExpired() {
  disableOtpInputs();
  enableResendButton();
  showMessage("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
}
```

**Resend Cooldown Logic:**
```typescript
function startResendCooldown(seconds: number) {
  let remaining = seconds; // 30 seconds
  disableResendButton();

  const interval = setInterval(() => {
    remaining--;
    updateResendButtonText(`Gửi lại (${remaining}s)`);

    if (remaining <= 0) {
      clearInterval(interval);
      enableResendButton();
      updateResendButtonText("Gửi lại");
    }
  }, 1000);
}
```

---

### Screen 3: Welcome/Success Screen (Optional)

**Layout:**
```
┌─────────────────────────────────┐
│                                 │
│        ✓                        │
│   Đăng ký thành công!           │
│                                 │
│  Chào mừng bạn đến với CashBee  │
│                                 │
│  Mã giới thiệu của bạn:         │
│  ┌─────────────────────────┐   │
│  │   JOHN2024ABC    [Copy] │   │
│  └─────────────────────────┘   │
│                                 │
│  Chia sẻ mã này để nhận thưởng! │
│                                 │
│  [  Khám phá ngay  ]            │
│                                 │
└─────────────────────────────────┘
```

**Behavior:**
- Auto-navigate sau 3 giây hoặc click "Khám phá ngay"
- Navigate to Home/Dashboard Screen

---

## 🧪 Mock Data để Test

### Mock API Responses (cho development không cần Backend)

```typescript
// Mock successful registration
const mockRegisterSuccess: ApiResponse<RegisterResponse> = {
  status: "success",
  data: {
    email: "test@example.com",
    maskedEmail: "t***@ex***ple.com",
    requiresOtp: true,
    expiresIn: 300
  },
  message: "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư và nhập mã OTP để hoàn tất đăng ký."
};

// Mock email already exists error
const mockEmailExistsError: ErrorResponse = {
  status: "error",
  error: {
    code: "EMAIL_ALREADY_EXISTS",
    message: "Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."
  }
};

// Mock successful OTP verification
const mockVerifyOtpSuccess: ApiResponse<VerifyOtpResponse> = {
  status: "success",
  data: {
    id: "123e4567-e89b-12d3-a456-426614174000",
    username: "testuser",
    email: "test@example.com",
    fullName: "Test User",
    phone: "0123456789",
    referralCode: "TEST2024ABC",
    createdAt: new Date().toISOString(),
    wallet: {
      id: "wallet-123",
      balance: 0,
      currency: "VND",
      createdAt: new Date().toISOString()
    }
  },
  message: "Xác thực email thành công! Chào mừng bạn đến với CashBee"
};

// Mock invalid OTP error
const mockInvalidOtpError: ErrorResponse = {
  status: "error",
  error: {
    code: "INVALID_OTP",
    message: "Mã OTP không chính xác. Bạn còn 2 lần thử.",
    attemptsRemaining: 2
  }
};

// Mock OTP expired error
const mockOtpExpiredError: ErrorResponse = {
  status: "error",
  error: {
    code: "OTP_EXPIRED",
    message: "Mã OTP đã hết hạn. Vui lòng nhấn 'Gửi lại mã' để nhận mã mới.",
    canResend: true
  }
};

// Mock resend cooldown error
const mockResendCooldownError: ErrorResponse = {
  status: "error",
  error: {
    code: "RESEND_COOLDOWN_ACTIVE",
    message: "Vui lòng đợi 25 giây trước khi gửi lại mã OTP.",
    retryAfterSeconds: 25,
    canResendAt: new Date(Date.now() + 25000).toISOString()
  }
};

// Mock successful resend
const mockResendSuccess: ApiResponse<ResendOtpResponse> = {
  status: "success",
  data: {
    email: "test@example.com",
    maskedEmail: "t***@ex***ple.com",
    expiresIn: 300,
    canResendAt: new Date(Date.now() + 30000).toISOString()
  },
  message: "Mã OTP mới đã được gửi đến email của bạn."
};
```

### Mock Service Implementation

```typescript
class MockRegistrationService {
  private mockOtpCode = "123456"; // For testing
  private attemptCount = 0;

  async register(data: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
    // Simulate network delay
    await this.delay(1000);

    // Simulate email exists error
    if (data.email === "existing@example.com") {
      throw {
        response: { data: mockEmailExistsError }
      };
    }

    // Reset attempt count for new registration
    this.attemptCount = 0;

    return mockRegisterSuccess;
  }

  async verifyOtp(data: VerifyOtpRequest): Promise<ApiResponse<VerifyOtpResponse>> {
    await this.delay(800);

    this.attemptCount++;

    // Simulate invalid OTP
    if (data.otpCode !== this.mockOtpCode) {
      const remaining = 3 - this.attemptCount;

      if (remaining <= 0) {
        // Max attempts exceeded
        throw {
          response: {
            data: {
              status: "error",
              error: {
                code: "MAX_ATTEMPTS_EXCEEDED",
                message: "Bạn đã nhập sai OTP quá 3 lần. Vui lòng nhấn 'Gửi lại mã' để nhận mã mới.",
                canResend: true
              }
            }
          }
        };
      }

      // Invalid OTP
      throw {
        response: {
          data: {
            status: "error",
            error: {
              code: "INVALID_OTP",
              message: `Mã OTP không chính xác. Bạn còn ${remaining} lần thử.`,
              attemptsRemaining: remaining
            }
          }
        }
      };
    }

    // Success
    return mockVerifyOtpSuccess;
  }

  async resendOtp(data: ResendOtpRequest): Promise<ApiResponse<ResendOtpResponse>> {
    await this.delay(600);

    // Reset attempt count
    this.attemptCount = 0;

    // Generate new mock OTP (for console logging)
    this.mockOtpCode = Math.floor(100000 + Math.random() * 900000).toString();
    console.log("New OTP Code:", this.mockOtpCode);

    return mockResendSuccess;
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Usage
const mockApi = new MockRegistrationService();

// Test registration
mockApi.register({
  username: "testuser",
  email: "test@example.com",
  password: "Test@123",
  fullName: "Test User",
  phone: "0123456789"
}).then(console.log);

// Test OTP verification (use "123456" as OTP)
mockApi.verifyOtp({
  email: "test@example.com",
  otpCode: "123456"
}).then(console.log);
```

### Test Scenarios

```typescript
// Scenario 1: Happy Path
async function testHappyPath() {
  // 1. Register
  const registerRes = await mockApi.register({
    username: "john",
    email: "john@example.com",
    password: "Secure@123",
    fullName: "John Doe",
    phone: "0123456789"
  });
  console.log("✓ Registration successful:", registerRes.data.maskedEmail);

  // 2. Verify OTP
  const verifyRes = await mockApi.verifyOtp({
    email: "john@example.com",
    otpCode: "123456"
  });
  console.log("✓ OTP verified:", verifyRes.data.username);
}

// Scenario 2: Invalid OTP (3 attempts)
async function testInvalidOtp() {
  await mockApi.register({ /* ... */ });

  try {
    await mockApi.verifyOtp({ email: "test@example.com", otpCode: "111111" });
  } catch (e) {
    console.log("✓ Attempt 1 failed:", e.response.data.error.attemptsRemaining);
  }

  try {
    await mockApi.verifyOtp({ email: "test@example.com", otpCode: "222222" });
  } catch (e) {
    console.log("✓ Attempt 2 failed:", e.response.data.error.attemptsRemaining);
  }

  try {
    await mockApi.verifyOtp({ email: "test@example.com", otpCode: "333333" });
  } catch (e) {
    console.log("✓ Max attempts exceeded:", e.response.data.error.code);
  }
}

// Scenario 3: Resend OTP
async function testResendOtp() {
  await mockApi.register({ /* ... */ });

  // First OTP
  console.log("First OTP:", mockApi.mockOtpCode);

  // Resend
  const resendRes = await mockApi.resendOtp({ email: "test@example.com" });
  console.log("✓ OTP resent:", resendRes.data.maskedEmail);
  console.log("New OTP:", mockApi.mockOtpCode);
}

// Scenario 4: Email already exists
async function testEmailExists() {
  try {
    await mockApi.register({
      username: "john",
      email: "existing@example.com",
      password: "Secure@123",
      fullName: "John Doe",
      phone: "0123456789"
    });
  } catch (e) {
    console.log("✓ Email exists error:", e.response.data.error.code);
  }
}
```

---

## 🔒 Security Considerations

### 1. Password Security
- ✅ Password được gửi trực tiếp đến Keycloak
- ✅ Password KHÔNG BAO GIỜ được lưu trong application database
- ✅ Chỉ có Keycloak quản lý password (hashed với bcrypt)

### 2. OTP Security
- ✅ OTP 6 chữ số ngẫu nhiên (100,000 - 999,999)
- ✅ Hết hạn sau 5 phút
- ✅ Tối đa 3 lần thử
- ✅ Cooldown 30 giây giữa các lần gửi
- ✅ Giới hạn 5 lần gửi/ngày
- ✅ Tự động cleanup sau 24 giờ

### 3. Email Masking
- ✅ Email được che khi hiển thị: `j***@ex***ple.com`
- ✅ Tránh lộ thông tin email đầy đủ

### 4. Rate Limiting
- ✅ 30 giây cooldown giữa các lần resend
- ✅ 5 lần resend tối đa mỗi ngày
- ✅ 3 lần thử OTP sai

### 5. HTTPS Only
- ⚠️ Chỉ sử dụng HTTPS trong production
- ⚠️ Không bao giờ gửi OTP qua HTTP

### 6. Input Validation
- ✅ Validate tất cả inputs trên cả client và server
- ✅ Sanitize user inputs
- ✅ Check email format
- ✅ Check password strength

---

## ❓ FAQ

### Q1: Tại sao phải thay đổi luồng đăng ký?
**A:** Để tăng bảo mật và xác thực email người dùng thực sự tồn tại. Ngăn chặn spam và fake accounts.

### Q2: Có thể bỏ qua OTP verification không?
**A:** Không. Tất cả users PHẢI verify OTP. Không có backward compatibility với luồng cũ.

### Q3: OTP có hiệu lực bao lâu?
**A:** 5 phút (300 giây). Sau đó phải gửi lại mã mới.

### Q4: Có thể gửi lại OTP bao nhiêu lần?
**A:** Tối đa 5 lần mỗi ngày. Mỗi lần phải đợi 30 giây.

### Q5: Nếu user nhập sai OTP quá 3 lần?
**A:** OTP hiện tại sẽ không còn hiệu lực. User phải gửi lại OTP mới.

### Q6: Email có được lưu sau khi đăng ký không?
**A:** Có, nhưng tài khoản ở trạng thái DISABLED cho đến khi verify OTP thành công.

### Q7: Nếu user không nhận được email OTP?
**A:**
- Kiểm tra thư mục Spam/Junk
- Click "Gửi lại" để nhận OTP mới
- Kiểm tra email có đúng không
- Contact support nếu vẫn không nhận được

### Q8: OTP có được gửi qua SMS không?
**A:** Không. Hiện tại chỉ gửi qua email. SMS có thể được thêm trong tương lai.

### Q9: Password có được lưu trong database không?
**A:** KHÔNG. Password chỉ được lưu trong Keycloak (hashed). Application database không bao giờ chứa password.

### Q10: Có thể test OTP flow mà không cần backend?
**A:** Có. Sử dụng Mock Service trong section [Mock Data để Test](#mock-data-để-test).

### Q11: Làm sao để debug OTP trong development?
**A:**
- Check console logs trên backend
- Backend sẽ log OTP code trong development mode
- Hoặc check email inbox (nếu đã config SMTP)

### Q12: Response code của /register endpoint thay đổi?
**A:** Có. Trước đây là `201 Created`, bây giờ là `200 OK` với `requiresOtp: true`.

### Q13: Có cần update login flow không?
**A:** Không. Login flow không thay đổi. Chỉ có registration flow mới có OTP.

### Q14: Nếu user đóng app trong lúc chờ OTP?
**A:** OTP vẫn còn hiệu lực 5 phút. User có thể mở app và nhập OTP (cần lưu state).

### Q15: Có thể thay đổi thời gian hết hạn OTP không?
**A:** Có thể, nhưng cần thay đổi ở backend. Mặc định là 5 phút.

---

## 📞 Support & Contact

Nếu có thắc mắc hoặc gặp vấn đề khi implement, vui lòng liên hệ:

**Backend Team:**
- Email: backend@cashbee.com
- Slack: #backend-support

**API Documentation:**
- Swagger UI: https://cashbee.nguocchieuvangle.io.vn/swagger-ui.html
- Postman Collection: [Link to Postman]

**Version:** 1.0.0
**Last Updated:** 2024-01-15
**Authors:** CashBee Backend Team
