# ⚡ Keycloak CORS - Quick Fix

## 🎯 Vấn đề

Sau khi thay đổi Keycloak config → Bị CORS error

**Nguyên nhân:** Quên cấu hình **Web Origins** trong Keycloak

---

## ✅ Giải pháp (5 bước đơn giản)

### 1️⃣ Đăng nhập Keycloak Admin

```
URL: https://auth.nguocchieuvangle.io.vn/
→ Click "Administration Console"
→ Đăng nhập với admin credentials
```

### 2️⃣ Chọn Realm

```
Góc trên bên trái → Chọn realm: "cashbee"
```

### 3️⃣ Vào Clients

```
Menu bên trái → "Clients"
→ Tìm và click: "cashbee-backend"
```

### 4️⃣ Cấu hình Web Origins ⭐ (QUAN TRỌNG!)

**Cuộn xuống phần "Access settings"**

**Tìm mục "Web origins" và thêm:**
```
http://localhost:3000
http://localhost:3007
https://cashbee.nguocchieuvangle.io.vn
```

**Lưu ý:**
- ✅ Không có dấu `/` ở cuối
- ✅ Không có `/*` ở cuối
- ✅ Có `http://` hoặc `https://` ở đầu

**Ví dụ ĐÚNG:**
```
✅ http://localhost:3000
✅ https://cashbee.nguocchieuvangle.io.vn
```

**Ví dụ SAI:**
```
❌ http://localhost:3000/
❌ http://localhost:3000/*
❌ localhost:3000
```

### 5️⃣ Save

```
Click nút "Save" ở cuối trang
```

---

## 🧪 Test ngay

**Trong browser console ở localhost:3000:**

```javascript
fetch('https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms')
  .then(r => r.json())
  .then(data => console.log('✅ Success!', data))
  .catch(err => console.error('❌ Still error:', err));
```

**Kết quả:** ✅ Không còn CORS error!

---

## 🔍 Vẫn không work?

### Thử các bước sau:

1. **Clear browser cache**
   - Ctrl + Shift + Delete
   - Hoặc dùng Incognito mode

2. **Restart backend**
   ```bash
   docker restart cashbee-backend
   ```

3. **Kiểm tra lại Web Origins**
   - Vào lại Keycloak
   - Clients → cashbee-backend
   - Xem Web Origins có đúng không

4. **Kiểm tra Valid Redirect URIs**
   - Phải có: `http://localhost:3000/*`
   - Lưu ý có `/*` ở cuối!

---

## 📋 Checklist

- [ ] Đã thêm `http://localhost:3000` vào Web Origins
- [ ] Đã click Save trong Keycloak
- [ ] Đã clear browser cache
- [ ] Đã test lại từ frontend

---

## 🎯 Tại sao lại như vậy?

**Keycloak có 2 nơi kiểm tra CORS:**

1. **Spring Boot** (`SecurityConfig.java`)
   - ✅ Đã cấu hình đúng rồi
   - Không cần thay đổi

2. **Keycloak Server** (Admin Console)
   - ❌ Đây là vấn đề
   - Cần thêm Web Origins

**Flow:**
```
Browser → Spring Boot CORS ✅
       → Spring Security ✅
       → Keycloak Policy Enforcer
          → Kiểm tra Web Origins ❌ (nếu không có → CORS error!)
```

---

## 📖 Chi tiết hơn

Đọc file: `docs/fixes/KEYCLOAK_CORS_FIX.md`

---

**Last Updated:** 2025-11-02
