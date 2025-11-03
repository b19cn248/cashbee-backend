# 🔑 Keycloak CORS Configuration Fix

## 📋 Vấn đề

**Hiện tượng:**
- Trước đây code hoạt động bình thường ✅
- Sau khi thay đổi cấu hình Keycloak → Bị lỗi CORS ❌

**Lỗi:**
```
Access to XMLHttpRequest blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

---

## 🔍 Nguyên Nhân

### Keycloak có 2 hệ thống CORS:

1. **Spring Boot CORS Filter** (trong code)
   - Cấu hình trong `SecurityConfig.java`
   - ✅ Đã được cấu hình đúng

2. **Keycloak CORS** (trong Keycloak Admin Console) ⚠️
   - Cấu hình trong **Keycloak Client Settings**
   - ❌ **ĐÂY LÀ VẤN ĐỀ!**

### Tại sao Keycloak ảnh hưởng đến CORS?

Khi bạn sử dụng Keycloak làm OAuth2 server:

```
Browser (localhost:3000) → Backend API (https://cashbee.api.nguocchieuvangle.io.vn)
                              ↓
                        Policy Enforcer Filter
                              ↓
                        Kiểm tra JWT token
                              ↓
                        Gọi Keycloak để verify permissions
                              ↓
                        🔴 KEYCLOAK CHECK WEB ORIGINS!
```

**Nếu `http://localhost:3000` không có trong Keycloak Web Origins:**
- Keycloak từ chối request
- Không trả về CORS headers
- Browser block request

---

## ✅ Giải pháp

### Bước 1: Đăng nhập Keycloak Admin Console

1. Truy cập: `https://auth.nguocchieuvangle.io.vn/`
2. Click **Administration Console**
3. Đăng nhập với admin credentials

### Bước 2: Chọn Realm và Client

1. Chọn realm: **`cashbee`** (góc trên bên trái)
2. Vào menu **Clients** (menu bên trái)
3. Tìm và click vào client: **`cashbee-backend`**

### Bước 3: Cấu hình Web Origins

Trong trang Client Settings, tìm phần **Access settings**:

#### **Web Origins** (quan trọng nhất!)

**Thêm các origins sau:**
```
http://localhost:3000
http://localhost:3007
https://cashbee.nguocchieuvangle.io.vn
+
```

**Giải thích:**
- `http://localhost:3000` - Frontend development
- `http://localhost:3007` - Nếu có port khác
- `https://cashbee.nguocchieuvangle.io.vn` - Production frontend
- `+` - Cho phép tất cả origins trong Valid Redirect URIs (không khuyến khích cho production)

**Ảnh minh họa cấu hình:**
```
┌─────────────────────────────────────────────────┐
│ Access settings                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│ Root URL                                        │
│ ┌─────────────────────────────────────────────┐│
│ │                                             ││
│ └─────────────────────────────────────────────┘│
│                                                 │
│ Valid redirect URIs                             │
│ ┌─────────────────────────────────────────────┐│
│ │ http://localhost:3000/*                     ││
│ │ https://cashbee.nguocchieuvangle.io.vn/*    ││
│ └─────────────────────────────────────────────┘│
│                                                 │
│ ⭐ Web origins                                  │
│ ┌─────────────────────────────────────────────┐│
│ │ http://localhost:3000                       ││ ← THÊM DÒNG NÀY!
│ │ http://localhost:3007                       ││ ← THÊM DÒNG NÀY!
│ │ https://cashbee.nguocchieuvangle.io.vn      ││ ← THÊM DÒNG NÀY!
│ └─────────────────────────────────────────────┘│
│                                                 │
│ Admin URL                                       │
│ ┌─────────────────────────────────────────────┐│
│ │                                             ││
│ └─────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

### Bước 4: Cấu hình Valid Redirect URIs (nếu cần)

**Valid redirect URIs** (cho authentication flow):
```
http://localhost:3000/*
http://localhost:3007/*
https://cashbee.nguocchieuvangle.io.vn/*
```

**Lưu ý:** Phải có `/*` ở cuối!

### Bước 5: Kiểm tra các cài đặt khác

Đảm bảo các setting sau được bật:

- ✅ **Client authentication**: `ON` (nếu dùng confidential client)
- ✅ **Authorization**: `ON` (nếu dùng policy enforcer)
- ✅ **Standard flow**: `ON` (Authorization Code flow)
- ✅ **Direct access grants**: `ON` (nếu cần password grant)

### Bước 6: Lưu cấu hình

Click nút **Save** ở cuối trang.

---

## 🧪 Test sau khi cấu hình

### Test 1: Verify trong Keycloak

1. Vào **Clients** → **cashbee-backend**
2. Kiểm tra lại **Web Origins** đã có `http://localhost:3000`
3. Kiểm tra **Valid Redirect URIs** đã có `http://localhost:3000/*`

### Test 2: Test từ Frontend

```javascript
// Trong browser console ở http://localhost:3000
fetch('https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log('✅ Success:', data))
.catch(error => console.error('❌ Error:', error));
```

**Kết quả mong đợi:** ✅ Không còn CORS error!

### Test 3: Kiểm tra CORS headers

```bash
curl -X OPTIONS https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v 2>&1 | grep -i "access-control"
```

**Phải thấy:**
```
< Access-Control-Allow-Origin: http://localhost:3000
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
< Access-Control-Allow-Credentials: true
```

---

## 🔍 Các trường hợp khác

### Trường hợp 1: Vẫn bị CORS sau khi cấu hình Web Origins

**Nguyên nhân:**
- Cache của browser
- Keycloak chưa reload cấu hình

**Giải pháp:**
```bash
# 1. Clear browser cache hoặc dùng Incognito mode
# 2. Restart Spring Boot backend
docker restart cashbee-backend
# OR
sudo systemctl restart cashbee-backend
```

### Trường hợp 2: CORS chỉ xảy ra với một số endpoints

**Nguyên nhân:**
- Policy Enforcer enforcement mode khác nhau cho từng path

**Kiểm tra:**
```json
// File: policy-enforcer-dev.json
{
  "paths": [
    {
      "path": "/api/admin/platforms/*",
      "enforcement-mode": "DISABLED"  // ← Nếu là DISABLED thì không check Keycloak
    }
  ]
}
```

**Giải pháp:**
- Nếu `enforcement-mode: "DISABLED"` → Không cần Keycloak auth
- Nếu `enforcement-mode: "ENFORCING"` → Cần cấu hình Web Origins

### Trường hợp 3: Production domain bị CORS

**Nguyên nhân:**
- Chưa thêm production domain vào Web Origins

**Giải pháp:**
```
Web Origins:
http://localhost:3000                      ← Development
https://cashbee.nguocchieuvangle.io.vn     ← Production frontend
https://app.yourdomain.com                 ← Nếu có domain khác
```

---

## 📊 So sánh: Spring Boot CORS vs Keycloak CORS

| Tính năng | Spring Boot CORS | Keycloak CORS |
|-----------|------------------|---------------|
| **Vị trí cấu hình** | `SecurityConfig.java` | Keycloak Admin Console |
| **Áp dụng cho** | Tất cả requests đến backend | Chỉ requests qua Policy Enforcer |
| **Khi nào cần** | Luôn luôn | Khi dùng Keycloak authentication |
| **Cách fix** | Sửa code | Sửa trong Keycloak UI |
| **Priority** | Thứ 1 (CorsFilter chạy trước) | Thứ 2 (Policy Enforcer chạy sau) |

### Flow xử lý CORS với Keycloak:

```
Request từ browser (localhost:3000)
        ↓
[1] Spring Boot CorsFilter
        ↓ (kiểm tra allowed origins)
        ✅ PASS
        ↓
[2] Spring Security Filter
        ↓
[3] BearerTokenAuthenticationFilter (validate JWT)
        ↓
[4] Policy Enforcer Filter
        ↓ (gọi Keycloak để check permissions)
        ↓
    Keycloak kiểm tra Web Origins
        ↓
        ❌ FAIL nếu origin không trong Web Origins
        ↓
    Response không có CORS headers
        ↓
Browser block request
```

**Vì vậy:**
- Phải cấu hình **CẢ HAI** Spring Boot CORS và Keycloak Web Origins
- Nếu thiếu một trong hai → CORS error!

---

## 🎯 Checklist

Sau khi thay đổi Keycloak config, kiểm tra:

### Keycloak Settings:

- [ ] Web Origins có `http://localhost:3000`
- [ ] Web Origins có `https://cashbee.nguocchieuvangle.io.vn`
- [ ] Valid Redirect URIs có `http://localhost:3000/*`
- [ ] Client authentication: ON (nếu dùng confidential client)
- [ ] Đã click **Save**

### Spring Boot Settings:

- [ ] `SecurityConfig.java` có `http://localhost:3000` trong allowedOrigins
- [ ] CORS filter bean được tạo
- [ ] Backend đã restart sau khi thay đổi

### Testing:

- [ ] curl -X OPTIONS trả về CORS headers
- [ ] Browser DevTools không có CORS error
- [ ] Frontend có thể call API thành công

---

## 🆘 Debug

### Bật Keycloak Debug Logs

**application.yml:**
```yaml
logging:
  level:
    org.keycloak: DEBUG
```

Restart backend và check logs:
```bash
tail -f /app/logs/cashbee-backend.log | grep -i "cors\|keycloak"
```

### Kiểm tra Client Configuration

```bash
# Get Keycloak access token
TOKEN=$(curl -X POST "https://auth.nguocchieuvangle.io.vn/realms/cashbee/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=YOUR_PASSWORD" \
  -d "grant_type=password" | jq -r '.access_token')

# Get client configuration
curl -X GET "https://auth.nguocchieuvangle.io.vn/admin/realms/cashbee/clients" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | select(.clientId=="cashbee-backend") | {webOrigins, redirectUris}'
```

**Output mong đợi:**
```json
{
  "webOrigins": [
    "http://localhost:3000",
    "http://localhost:3007",
    "https://cashbee.nguocchieuvangle.io.vn"
  ],
  "redirectUris": [
    "http://localhost:3000/*",
    "https://cashbee.nguocchieuvangle.io.vn/*"
  ]
}
```

---

## 📚 Tài liệu tham khảo

- [Keycloak Client Configuration](https://www.keycloak.org/docs/latest/server_admin/#_clients)
- [Keycloak CORS Documentation](https://www.keycloak.org/docs/latest/securing_apps/#_cors)
- [Spring Security with Keycloak](https://www.keycloak.org/docs/latest/securing_apps/#_spring_boot_adapter)

---

## 🎯 Tóm tắt

**Vấn đề:**
- Sau khi thay đổi Keycloak config → CORS error

**Nguyên nhân:**
- Quên cấu hình **Web Origins** trong Keycloak client

**Giải pháp:**
1. Vào Keycloak Admin Console
2. Clients → cashbee-backend
3. Thêm `http://localhost:3000` vào **Web Origins**
4. Thêm `http://localhost:3000/*` vào **Valid Redirect URIs**
5. Click **Save**
6. Test lại

**Không cần:**
- ❌ Không cần thay đổi nginx config
- ❌ Không cần thay đổi Spring Boot code
- ❌ Không cần thay đổi frontend code

Chỉ cần cấu hình lại Keycloak là xong! ✅

---

**Status:** 🔑 Keycloak CORS Fix Guide Complete
**Last Updated:** 2025-11-02
