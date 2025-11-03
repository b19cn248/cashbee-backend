# 🚀 Nginx Configuration Deployment Guide

This guide will help you fix the CORS error by updating your nginx configuration on the VPS.

---

## 📋 Prerequisites

- SSH access to your VPS
- Nginx installed and running
- SSL certificates configured
- Spring Boot backend running on port 8080

---

## 🔧 Step-by-Step Deployment

### Step 1: Backup Current Configuration

```bash
# SSH to your VPS
ssh your-user@your-vps-ip

# Backup current nginx configuration
sudo cp /etc/nginx/sites-available/cashbee-api /etc/nginx/sites-available/cashbee-api.backup.$(date +%Y%m%d_%H%M%S)

# Verify backup created
ls -la /etc/nginx/sites-available/ | grep cashbee-api
```

### Step 2: Update Nginx Configuration

**Option A: Edit existing file**

```bash
# Edit nginx config
sudo nano /etc/nginx/sites-available/cashbee-api
```

**Option B: Replace with new template**

```bash
# Upload the new configuration file from your local machine
# From your local machine (in project directory):
scp docker/nginx/cashbee-api.conf your-user@your-vps-ip:/tmp/

# On VPS, move to nginx directory
sudo mv /tmp/cashbee-api.conf /etc/nginx/sites-available/cashbee-api
```

**IMPORTANT: Update SSL certificate paths in the config file!**

```bash
# Edit the file to update SSL paths
sudo nano /etc/nginx/sites-available/cashbee-api
```

Find and update these lines:
```nginx
ssl_certificate /path/to/your/certificate.crt;       # ← UPDATE THIS
ssl_certificate_key /path/to/your/private.key;       # ← UPDATE THIS
```

**Example:**
```nginx
ssl_certificate /etc/letsencrypt/live/cashbee.api.nguocchieuvangle.io.vn/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/cashbee.api.nguocchieuvangle.io.vn/privkey.pem;
```

### Step 3: Verify Configuration

**Key points to verify:**

```nginx
location / {
    proxy_pass http://localhost:8080;  # ← Backend address

    # These headers MUST be present:
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Origin $http_origin;  # ← CRITICAL for CORS!

    # NO CORS headers here! Remove any add_header 'Access-Control-*' directives
}
```

**Check for and REMOVE any of these lines:**
```nginx
# ❌ REMOVE these if present:
add_header 'Access-Control-Allow-Origin' '*';
add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS';
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type';
```

### Step 4: Test Configuration

```bash
# Test nginx configuration syntax
sudo nginx -t
```

**Expected output:**
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

**If you see errors:**
- Check for typos
- Verify SSL certificate paths exist
- Check that proxy_pass URL is correct

### Step 5: Reload Nginx

```bash
# Reload nginx (doesn't drop connections)
sudo systemctl reload nginx

# OR restart nginx (drops connections)
sudo systemctl restart nginx

# Verify nginx is running
sudo systemctl status nginx
```

### Step 6: Verify Spring Boot is Running

```bash
# Check if Spring Boot is running on port 8080
sudo netstat -tlnp | grep 8080
# OR
sudo ss -tlnp | grep 8080
```

**Expected output:**
```
tcp6       0      0 :::8080                 :::*                    LISTEN      12345/java
```

**If Spring Boot is not running:**
```bash
# Check Spring Boot logs
sudo journalctl -u cashbee-backend -n 50 --no-pager

# OR if running with Docker
docker logs cashbee-backend
```

---

## 🧪 Testing the Fix

### Test 1: Verify Nginx Proxying

```bash
# From VPS, test direct connection to Spring Boot
curl -I http://localhost:8080/api/admin/platforms

# Should return 200 OK or 401 Unauthorized (if auth required)
```

```bash
# Test through nginx (from VPS)
curl -I https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms

# Should also return 200 OK or 401 Unauthorized
```

### Test 2: Verify CORS Headers

```bash
# Test preflight OPTIONS request
curl -X OPTIONS https://cashbee.api.nguocchieuvangle.io.vn/api/admin/platforms \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: content-type" \
  -v 2>&1 | grep -i "access-control"
```

**Expected output (should include):**
```
< Access-Control-Allow-Origin: http://localhost:3000
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
< Access-Control-Allow-Headers: Authorization, Cache-Control, Content-Type, ...
< Access-Control-Allow-Credentials: true
```

### Test 3: Test from Browser

**From your frontend (localhost:3000):**

```javascript
// Open browser console and run:
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

**Should work without CORS error!**

---

## 🔍 Troubleshooting

### Issue 1: Still Getting CORS Error

**Check:**

1. **Verify nginx forwarded the request:**
```bash
# Check nginx access log
sudo tail -f /var/log/nginx/cashbee-api-access.log

# Look for OPTIONS request
```

2. **Verify Spring Boot received the request:**
```bash
# Check Spring Boot logs
sudo tail -f /app/logs/cashbee-backend.log

# Look for OPTIONS request and CORS filter processing
```

3. **Check response headers in browser:**
- Open DevTools (F12) → Network tab
- Look for OPTIONS request
- Check Response Headers

**If no Access-Control-* headers:**
- Spring Boot CORS filter not working
- Check SecurityConfig.java

**If wrong Access-Control-Allow-Origin:**
- Origin not in allowed list
- Update SecurityConfig.java line 139-141

### Issue 2: 502 Bad Gateway

**Cause:** Nginx can't connect to Spring Boot

**Fix:**
```bash
# Check if Spring Boot is running
sudo systemctl status cashbee-backend
# OR
docker ps | grep cashbee

# Check if port 8080 is listening
sudo netstat -tlnp | grep 8080

# If not running, start it
sudo systemctl start cashbee-backend
# OR
docker start cashbee-backend
```

### Issue 3: SSL Certificate Errors

**Cause:** Certificate paths incorrect in nginx config

**Fix:**
```bash
# Find your SSL certificates
sudo ls -la /etc/letsencrypt/live/cashbee.api.nguocchieuvangle.io.vn/

# Update nginx config with correct paths
sudo nano /etc/nginx/sites-available/cashbee-api

# Test and reload
sudo nginx -t && sudo systemctl reload nginx
```

### Issue 4: Nginx Syntax Errors

**Common errors:**

```
nginx: [emerg] unexpected "}" in /etc/nginx/sites-available/cashbee-api:45
```

**Fix:**
- Check for missing semicolons `;`
- Check for unclosed braces `{}`
- Check for typos in directives

### Issue 5: Port Already in Use

```
nginx: [emerg] bind() to 0.0.0.0:443 failed (98: Address already in use)
```

**Fix:**
```bash
# Find what's using port 443
sudo netstat -tlnp | grep :443

# If another nginx instance, stop it
sudo systemctl stop nginx
sudo systemctl start nginx
```

---

## 🔧 Advanced Configuration

### Enable Request Logging (for debugging)

Add to the `location /` block:

```nginx
location / {
    # ... existing config ...

    # Log request details
    access_log /var/log/nginx/cashbee-api-detailed.log combined;

    # Log request body (careful - can be large!)
    # echo_read_request_body;
    # echo $request_body;
}
```

### Add Rate Limiting (optional)

```nginx
# Add before server block
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

server {
    # ... ssl config ...

    location /api/ {
        # Rate limiting
        limit_req zone=api_limit burst=20 nodelay;

        # ... proxy config ...
    }
}
```

### Configure Caching (optional)

```nginx
# Add before server block
proxy_cache_path /var/cache/nginx/cashbee levels=1:2 keys_zone=cashbee_cache:10m max_size=100m inactive=60m;

server {
    location /api/admin/platforms {
        # Cache GET requests for 5 minutes
        proxy_cache cashbee_cache;
        proxy_cache_valid 200 5m;
        proxy_cache_methods GET;
        proxy_cache_key "$scheme$request_method$host$request_uri";

        # ... proxy config ...
    }
}
```

---

## 📊 Monitoring

### Check Nginx Logs

```bash
# Real-time access log
sudo tail -f /var/log/nginx/cashbee-api-access.log

# Real-time error log
sudo tail -f /var/log/nginx/cashbee-api-error.log

# Search for CORS errors
sudo grep -i "cors\|access-control" /var/log/nginx/cashbee-api-error.log
```

### Check Spring Boot Logs

```bash
# Real-time logs
sudo tail -f /app/logs/cashbee-backend.log

# Search for CORS-related logs
sudo grep -i "cors" /app/logs/cashbee-backend.log
```

### Monitor Nginx Status

```bash
# Check nginx is running
sudo systemctl status nginx

# Check nginx error log for issues
sudo tail -20 /var/log/nginx/error.log

# Check configuration
sudo nginx -T
```

---

## ✅ Verification Checklist

After deployment, verify:

- [ ] Nginx configuration test passes: `sudo nginx -t`
- [ ] Nginx reloaded successfully: `sudo systemctl reload nginx`
- [ ] Spring Boot is running on port 8080: `netstat -tlnp | grep 8080`
- [ ] OPTIONS request returns CORS headers: `curl -X OPTIONS ...`
- [ ] Frontend can call API without CORS error
- [ ] Browser DevTools shows correct CORS headers
- [ ] No errors in nginx logs: `tail /var/log/nginx/error.log`
- [ ] No errors in Spring Boot logs: `tail /app/logs/cashbee-backend.log`

---

## 🆘 Need Help?

### Check Documentation

- Nginx Configuration: `/etc/nginx/sites-available/cashbee-api`
- Nginx Logs: `/var/log/nginx/cashbee-api-*.log`
- Spring Boot Config: `SecurityConfig.java`
- CORS Analysis: `docs/fixes/CORS_ERROR_ANALYSIS_AND_FIX.md`

### Common Commands

```bash
# Reload nginx
sudo systemctl reload nginx

# Restart nginx
sudo systemctl restart nginx

# Test nginx config
sudo nginx -t

# View nginx error log
sudo tail -f /var/log/nginx/error.log

# View Spring Boot log
sudo tail -f /app/logs/cashbee-backend.log

# Check ports
sudo netstat -tlnp | grep -E "80|443|8080"
```

---

**Status:** 📝 Deployment Guide Ready
**Last Updated:** 2025-11-02
