# ⚡ Quick Fix: CORS Error

## 🎯 Problem

```
Access to XMLHttpRequest blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present
```

Works on **localhost:8080** ✅ but NOT on **VPS through nginx** ❌

---

## 🔍 Root Cause

Nginx is either:
1. Not forwarding OPTIONS requests to Spring Boot
2. Stripping CORS headers from Spring Boot responses
3. Adding its own CORS headers that conflict

---

## ✅ Solution

### 1. Update Nginx Config

**Edit:** `/etc/nginx/sites-available/cashbee-api`

**Key changes:**

```nginx
location / {
    proxy_pass http://localhost:8080;

    # CRITICAL: Add these headers
    proxy_set_header Host $host;
    proxy_set_header Origin $http_origin;  # ← Preserves origin for CORS
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # DON'T add any add_header 'Access-Control-*' directives!
    # Let Spring Boot handle CORS!
}
```

**Remove any lines like:**
```nginx
add_header 'Access-Control-Allow-Origin' '*';  # ❌ DELETE THIS
add_header 'Access-Control-Allow-Methods' ...;  # ❌ DELETE THIS
```

### 2. Apply Changes

```bash
# Test config
sudo nginx -t

# Reload nginx
sudo systemctl reload nginx
```

### 3. Verify

```bash
# Test CORS headers
curl -X OPTIONS https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms \
  -H "Origin: http://localhost:3000" \
  -v 2>&1 | grep "Access-Control"
```

**Should see:**
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Credentials: true
```

---

## 📝 Files to Use

1. **Nginx config template:** `docker/nginx/cashbee-api.conf`
2. **Detailed guide:** `docker/nginx/NGINX_DEPLOYMENT_GUIDE.md`
3. **Full analysis:** `docs/fixes/CORS_ERROR_ANALYSIS_AND_FIX.md`

---

## 🆘 Still Not Working?

1. Check nginx logs: `sudo tail -f /var/log/nginx/error.log`
2. Check Spring Boot logs: `sudo tail -f /app/logs/cashbee-backend.log`
3. Verify Spring Boot is running: `netstat -tlnp | grep 8080`
4. Check browser DevTools → Network → OPTIONS request

---

**Last Updated:** 2025-11-02
