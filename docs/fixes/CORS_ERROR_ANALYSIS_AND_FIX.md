# 🔍 CORS Error Analysis and Fix

## 📋 Problem Description

**Error Message:**
```
Access to XMLHttpRequest at 'https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms'
from origin 'http://localhost:3000' has been blocked by CORS policy:
Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

**Scenario:**
- Frontend: `http://localhost:3000`
- API Local: `http://localhost:8080` ✅ **WORKS**
- API VPS: `https://cashbee.api.nguocchieuvangle.io.vn` ❌ **FAILS**

---

## ⚠️ IMPORTANT: Check This First!

### Did you recently change Keycloak configuration?

**If YES → The problem is likely in Keycloak Web Origins configuration!**

**Quick Fix:**
1. Go to Keycloak Admin Console
2. Select realm: `cashbee`
3. Clients → `cashbee-backend`
4. Scroll to **Web Origins**
5. Add: `http://localhost:3000`
6. Click **Save**

**Detailed guide:** See `docs/fixes/KEYCLOAK_CORS_QUICK_FIX.md`

---

**If NO (or Keycloak fix didn't work) → Continue reading below for nginx configuration**

---

## 🔍 Root Cause Analysis

### 1. What is CORS?

**CORS (Cross-Origin Resource Sharing)** is a browser security mechanism that prevents a web page from making requests to a different domain than the one that served the page.

**Example:**
```
Frontend: http://localhost:3000
API:      https://cashbee.api.nguocchieuvangle.io.vn

Different domain → CORS check required!
```

### 2. How CORS Works

When making a cross-origin request, the browser sends a **preflight request** first:

```
Step 1: Browser sends OPTIONS request
┌──────────────────────────────────────────────────┐
│ OPTIONS /api/admin/platforms HTTP/1.1           │
│ Host: cashbee.api.nguocchieuvangle.io.vn        │
│ Origin: http://localhost:3000                   │
│ Access-Control-Request-Method: GET              │
│ Access-Control-Request-Headers: content-type    │
└──────────────────────────────────────────────────┘

Step 2: Server responds with CORS headers
┌──────────────────────────────────────────────────┐
│ HTTP/1.1 200 OK                                  │
│ Access-Control-Allow-Origin: http://localhost:3000│
│ Access-Control-Allow-Methods: GET, POST, PUT     │
│ Access-Control-Allow-Headers: content-type       │
│ Access-Control-Allow-Credentials: true           │
└──────────────────────────────────────────────────┘

Step 3: If preflight succeeds, browser sends actual request
┌──────────────────────────────────────────────────┐
│ GET /api/admin/platforms HTTP/1.1                │
│ Host: cashbee.api.nguocchieuvangle.io.vn        │
│ Origin: http://localhost:3000                   │
└──────────────────────────────────────────────────┘
```

### 3. Current Configuration

**Spring Boot CORS Config** (`SecurityConfig.java:136-161`):

```java
@Bean
public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();

    // ✅ Allowed origins include localhost:3000
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",           // ← Your frontend
        "http://localhost:3007",
        "https://cashbee.nguocchieuvangle.io.vn/",
        "https://video.management.v1.openlearnhub.io.vn/",
        "https://auth.cashbee.com.vn/"
    ));

    // ✅ Allowed methods include GET
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    // ✅ Allow credentials
    config.setAllowCredentials(true);

    // ✅ Allowed headers
    config.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Cache-Control",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers"
        // ... more headers
    ));

    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

**Analysis:** ✅ Spring Boot CORS configuration looks CORRECT!

### 4. Why Works Locally but Not Through Nginx?

**Local Request Flow (WORKS):**
```
Browser → http://localhost:8080 → Spring Boot
                                    ↓
                            CORS headers added
                                    ↓
                            Response returned
```

**VPS Request Flow (FAILS):**
```
Browser → https://cashbee.api.nguocchieuvangle.io.vn → Nginx → Spring Boot
                                                          ↓
                                                    ❌ PROBLEM HERE!
```

**Possible Issues with Nginx:**

1. **Nginx not forwarding OPTIONS requests** to backend
2. **Nginx stripping CORS headers** from Spring Boot response
3. **Nginx adding its own CORS headers** that conflict with Spring Boot
4. **Nginx not preserving origin header** when forwarding

---

## 🔧 Solution

### Fix 1: Update Nginx Configuration (REQUIRED)

Your nginx configuration needs to:
1. Forward all requests (including OPTIONS) to Spring Boot
2. NOT strip CORS headers from responses
3. Forward proxy headers (X-Forwarded-*, Origin, etc.)

**Recommended Nginx Configuration:**

```nginx
server {
    listen 443 ssl http2;
    server_name cashbee.api.nguocchieuvangle.io.vn;

    # SSL certificates
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;

    # Increase buffer sizes for large headers
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
    proxy_busy_buffers_size 256k;

    location / {
        # Forward ALL requests to Spring Boot (including OPTIONS)
        proxy_pass http://localhost:8080;

        # Preserve original request information
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # CRITICAL: Preserve Origin header for CORS
        proxy_set_header Origin $http_origin;

        # Don't add nginx CORS headers (let Spring Boot handle it)
        # If you have add_header directives for CORS, REMOVE THEM!

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

**IMPORTANT:**
- ❌ **Remove any `add_header Access-Control-*` directives** from nginx
- ✅ **Let Spring Boot handle all CORS headers**
- ✅ **Nginx should ONLY proxy the requests**

### Fix 2: Verify Spring Boot CORS Configuration

Your current configuration is already correct, but double-check:

**`SecurityConfig.java:136-161`**

Ensure these lines exist:
```java
// Line 139-141: Verify localhost:3000 is in allowed origins
config.setAllowedOrigins(List.of(
    "http://localhost:3000",  // ← Must be here!
    // ... other origins
));

// Line 143: Verify OPTIONS method is allowed
config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

// Line 158: Verify credentials are allowed
config.setAllowCredentials(true);
```

✅ Already correct in your code!

### Fix 3: Add More Allowed Origins (OPTIONAL)

If you want to allow requests from production frontend:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",           // Development
    "http://localhost:3007",
    "https://cashbee.nguocchieuvangle.io.vn",  // ⚠️ REMOVE trailing slash!
    "https://your-production-frontend.com",    // Add production domain
    "https://video.management.v1.openlearnhub.io.vn",
    "https://auth.cashbee.com.vn"      // ⚠️ REMOVE trailing slash!
));
```

**Note:** Trailing slashes in origins can cause issues!

---

## 🧪 Testing the Fix

### Step 1: Update Nginx Configuration

1. SSH to your VPS
2. Edit nginx config (usually at `/etc/nginx/sites-available/cashbee-api`)
3. Apply the configuration above
4. Test config: `sudo nginx -t`
5. Reload nginx: `sudo systemctl reload nginx`

### Step 2: Verify CORS Headers

**Test with curl:**

```bash
# Test preflight OPTIONS request
curl -X OPTIONS https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: content-type" \
  -v
```

**Expected response headers:**
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: Authorization, Cache-Control, Content-Type, ...
Access-Control-Allow-Credentials: true
```

### Step 3: Test from Frontend

**In browser console (localhost:3000):**

```javascript
fetch('https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('Error:', error));
```

**Should work without CORS error!**

---

## 🔍 Debugging CORS Issues

### Check 1: Nginx Logs

```bash
# Check nginx access log
sudo tail -f /var/log/nginx/access.log

# Check nginx error log
sudo tail -f /var/log/nginx/error.log
```

**Look for:**
- OPTIONS requests reaching nginx
- Any CORS-related errors

### Check 2: Spring Boot Logs

```bash
# Check application logs
sudo tail -f /app/logs/cashbee-backend.log
```

**Look for:**
- OPTIONS requests reaching Spring Boot
- CORS filter processing

### Check 3: Browser DevTools

1. Open browser DevTools (F12)
2. Go to Network tab
3. Try the request
4. Look for the OPTIONS preflight request
5. Check request headers and response headers

**What to look for:**

**Request Headers:**
```
Origin: http://localhost:3000
Access-Control-Request-Method: GET
Access-Control-Request-Headers: content-type
```

**Response Headers (should include):**
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: Authorization, Content-Type, ...
Access-Control-Allow-Credentials: true
```

---

## 📊 Common CORS Mistakes

### ❌ Mistake 1: Nginx Adding CORS Headers

**Wrong:**
```nginx
location / {
    add_header 'Access-Control-Allow-Origin' '*';  # ← DON'T DO THIS!
    add_header 'Access-Control-Allow-Methods' 'GET, POST';
    proxy_pass http://localhost:8080;
}
```

**Why wrong:** Nginx and Spring Boot both add CORS headers, causing conflicts.

**Fix:** Let Spring Boot handle CORS, nginx only proxies.

### ❌ Mistake 2: Not Forwarding OPTIONS Requests

**Wrong:**
```nginx
location / {
    if ($request_method = 'OPTIONS') {
        return 204;  # ← Blocks OPTIONS from reaching Spring Boot!
    }
    proxy_pass http://localhost:8080;
}
```

**Fix:** Remove the `if` block, let Spring Boot handle OPTIONS.

### ❌ Mistake 3: Trailing Slashes in Origins

**Wrong:**
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000/",  // ← Trailing slash!
));
```

**Fix:**
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000"   // ← No trailing slash!
));
```

### ❌ Mistake 4: Using Wildcards with Credentials

**Wrong:**
```java
config.setAllowedOrigins(List.of("*"));  // ← Can't use with credentials!
config.setAllowCredentials(true);
```

**Fix:** Specify exact origins when using credentials.

---

## 🎯 Summary

### Problem
- CORS error when calling API through nginx on VPS
- Works locally but not in production

### Root Cause
- Nginx configuration not properly forwarding OPTIONS requests
- Or nginx stripping CORS headers from Spring Boot response

### Solution
1. ✅ Update nginx config to proxy ALL requests (including OPTIONS)
2. ✅ Remove any CORS headers from nginx (let Spring Boot handle it)
3. ✅ Forward proxy headers (Origin, X-Forwarded-*, etc.)
4. ✅ Verify Spring Boot CORS config includes localhost:3000

### Next Steps
1. Update nginx configuration on VPS
2. Reload nginx
3. Test with curl
4. Test from frontend
5. Verify in browser DevTools

---

## 📚 References

- [Spring Boot CORS Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-cors)
- [MDN CORS Guide](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Nginx Proxy Configuration](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)

---

**Status:** ✅ Analysis Complete
**Solution:** Ready for implementation
**Last Updated:** 2025-11-02
