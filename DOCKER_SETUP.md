# 🐳 Docker Setup Guide

## 📋 Quick Start

Chỉ cần chạy một lệnh duy nhất để start cả MySQL và Backend:

```bash
docker-compose up -d
```

## 🎯 Ports

| Service | Container Port | Host Port | Access URL |
|---------|---------------|-----------|------------|
| **CashBee Backend** | 8080 | 8911 | http://localhost:8911 |
| **MySQL** | 3306 | 33067 | localhost:33067 |

## 📚 Các Lệnh Docker Compose

### Start services
```bash
docker-compose up -d
```

### Xem logs
```bash
# Tất cả services
docker-compose logs -f

# Chỉ backend
docker-compose logs -f cashbee-backend

# Chỉ MySQL
docker-compose logs -f mysql
```

### Stop services
```bash
docker-compose stop
```

### Restart services
```bash
docker-compose restart
```

### Stop và xóa containers (data vẫn giữ)
```bash
docker-compose down
```

### Stop, xóa containers VÀ xóa volumes (mất hết data!)
```bash
docker-compose down -v
```

### Rebuild image (sau khi thay đổi code)
```bash
docker-compose up -d --build
```

### Xem trạng thái
```bash
docker-compose ps
```

## 🔍 Health Checks

### Backend Health
```bash
curl http://localhost:8911/actuator/health
```

**Expected:**
```json
{
  "status": "UP"
}
```

### MySQL Health
```bash
docker exec cashbee-mysql mysqladmin ping -h localhost -uroot -prootpassword
```

**Expected:** `mysqld is alive`

## 🌐 Access URLs

- **API Documentation (Swagger):** http://localhost:8911/swagger-ui.html
- **API Docs (JSON):** http://localhost:8911/v3/api-docs
- **Health Check:** http://localhost:8911/actuator/health

## 🗄️ Database Access

### Connect từ host machine
```bash
mysql -h 127.0.0.1 -P 33067 -u root -p
# Password: rootpassword
```

### Connect từ bên trong container
```bash
docker exec -it cashbee-mysql mysql -u root -p
# Password: rootpassword
```

### Connection String
```
jdbc:mysql://localhost:33067/cashbee?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
```

## 📊 Database Credentials

| Parameter | Value |
|-----------|-------|
| Host | localhost (từ host) / mysql (từ container) |
| Port | 33067 (từ host) / 3306 (từ container) |
| Database | cashbee |
| Root Password | rootpassword |
| User | cashbee_user |
| User Password | cashbee_password |

## 🔧 Troubleshooting

### Backend không start được

**1. Kiểm tra logs:**
```bash
docker-compose logs -f cashbee-backend
```

**2. MySQL chưa sẵn sàng:**
```bash
docker-compose restart cashbee-backend
```

**3. Kiểm tra health:**
```bash
docker exec cashbee-backend wget --spider http://localhost:8080/actuator/health
```

### MySQL không kết nối được

**1. Kiểm tra container running:**
```bash
docker-compose ps mysql
```

**2. Kiểm tra logs:**
```bash
docker-compose logs -f mysql
```

**3. Test connection:**
```bash
docker exec cashbee-mysql mysqladmin ping -h localhost -uroot -prootpassword
```

### Port đã bị sử dụng

**Error:** `Bind for 0.0.0.0:8911 failed: port is already allocated`

**Fix:**
```bash
# Tìm process đang dùng port
# Windows:
netstat -ano | findstr :8911

# Linux/Mac:
lsof -i :8911

# Hoặc thay đổi port trong docker-compose.yml
ports:
  - "8912:8080"  # Đổi 8911 thành 8912
```

### Rebuild sau khi thay đổi code

```bash
# Stop containers
docker-compose down

# Rebuild và start lại
docker-compose up -d --build
```

### Reset toàn bộ (xóa data)

```bash
# Stop và xóa containers + volumes
docker-compose down -v

# Xóa image cũ
docker rmi cashbee-backend_cashbee-backend

# Start lại từ đầu
docker-compose up -d
```

## 🚀 Deployment Tips

### Production Build

Thay đổi `JAVA_OPTS` trong docker-compose.yml:
```yaml
environment:
  JAVA_OPTS: -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Resource Limits

Thêm vào docker-compose.yml:
```yaml
services:
  cashbee-backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

## 📝 Environment Variables

Thay đổi trong docker-compose.yml:

```yaml
environment:
  # Database
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cashbee
  SPRING_DATASOURCE_USERNAME: root
  SPRING_DATASOURCE_PASSWORD: rootpassword

  # Keycloak (thay bằng Keycloak của bạn)
  KEYCLOAK_SERVER_URL: https://your-keycloak.com
  KEYCLOAK_REALM: cashbee
  KEYCLOAK_CLIENT_ID: cashbee-backend
  KEYCLOAK_CLIENT_SECRET: your-secret
```

## 🧪 Test API

### Create Tracking Link
```bash
curl -X POST http://localhost:8911/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/289826815/21040225881",
    "userId": 1
  }'
```

### Get Platforms
```bash
curl http://localhost:8911/api/admin/platforms
```

## 📦 Docker Images

**Backend Image Size:** ~350MB (optimized with Alpine Linux + JRE only)

**Layers:**
- Base: eclipse-temurin:21-jre-alpine (~170MB)
- Application JAR: (~50MB)
- Dependencies: (~130MB)

## 🔐 Security Notes

**⚠️ QUAN TRỌNG cho Production:**

1. **Đổi mật khẩu MySQL:**
   ```yaml
   MYSQL_ROOT_PASSWORD: your-strong-password
   ```

2. **Sử dụng .env file:**
   ```bash
   # Tạo file .env
   MYSQL_ROOT_PASSWORD=your-password
   KEYCLOAK_CLIENT_SECRET=your-secret
   ```

   ```yaml
   # docker-compose.yml
   environment:
     MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
   ```

3. **Không expose MySQL port ra ngoài:**
   ```yaml
   # Xóa ports nếu chỉ backend cần access
   # ports:
   #   - "33067:3306"
   ```

---

**Version:** 1.0.0
**Last Updated:** 2025-11-02
**Status:** ✅ Ready to Use
