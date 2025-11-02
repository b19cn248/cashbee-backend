# CashBee Backend

## 📖 Giới thiệu

CashBee Backend là hệ thống backend cho nền tảng cashback CashBee, được xây dựng theo kiến trúc Hexagonal (Ports & Adapters) với Spring Boot 3.4.1.

### Tính năng chính

- 🔐 **Tích hợp Keycloak**: Xác thực và phân quyền hoàn toàn qua Keycloak (OAuth2/OIDC)
- 💰 **Quản lý ví điện tử**: Theo dõi số dư available, pending, và locked
- 🎁 **Hệ thống cashback**: Tính toán và quản lý hoàn tiền từ affiliate networks
- 👥 **Hệ thống giới thiệu**: Mã giới thiệu và tracking người giới thiệu
- 🏗️ **Kiến trúc Hexagonal**: Tách biệt hoàn toàn domain logic khỏi infrastructure
- 📊 **API Documentation**: Swagger/OpenAPI tự động
- 🗄️ **Database Migration**: Liquibase để quản lý schema

## 🏗️ Kiến trúc

### Multi-Module Structure

```
cashbee-backend/
├── cashbee-common/          # Exceptions, Constants, Utilities
├── cashbee-domain/          # Pure domain models (NO infrastructure)
├── cashbee-infrastructure/  # JPA, Repositories, Adapters
├── cashbee-application/     # Use cases, Application services
└── cashbee-presentation/    # REST Controllers, Main application
```

### Hexagonal Architecture Layers

```
┌─────────────────────────────────────────┐
│  Presentation (REST API)                │
│  Controllers, Exception Handlers        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Application (Use Cases)                │
│  Business workflows orchestration       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Domain (Business Logic) - PURE         │
│  Models, Repositories (interfaces)      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Infrastructure (Adapters)              │
│  JPA, MySQL, External services          │
└─────────────────────────────────────────┘
```

**Key Principles:**
- Domain layer không có dependencies vào infrastructure
- Domain defines interfaces (Ports), Infrastructure implements (Adapters)
- Foreign keys là primitive types (Long), KHÔNG dùng @OneToOne, @ManyToOne
- Mappers convert giữa Domain models và JPA entities

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Keycloak (optional - for authentication)

### 1. Database Setup

Tạo database và user:

```sql
CREATE DATABASE cashbee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cashbee_user'@'localhost' IDENTIFIED BY 'cashbee_password';
GRANT ALL PRIVILEGES ON cashbee.* TO 'cashbee_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configuration

Cập nhật file `cashbee-presentation/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cashbee
    username: cashbee_user
    password: cashbee_password
```

### 3. Build & Run

```bash
# Build tất cả modules
./mvnw clean install

# Run application
./mvnw spring-boot:run -pl cashbee-presentation

# Hoặc run từ JAR
java -jar cashbee-presentation/target/cashbee-presentation-1.0.0-SNAPSHOT.jar
```

### 4. Verify

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health
- API Docs: http://localhost:8080/v3/api-docs

## 📚 API Endpoints

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/sync` | Sync user from Keycloak |
| GET | `/api/users/keycloak/{keycloakId}` | Get user by Keycloak ID |

**Example - Sync User:**

```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567",
    "referredBy": "CB4F7A9K"
  }'
```

**Response:**

```json
{
  "success": true,
  "message": "User synced successfully",
  "data": {
    "id": 1,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567",
    "referralCode": "CBXYZ123",
    "status": "ACTIVE",
    "createdAt": "2025-10-29T22:00:00"
  },
  "timestamp": "2025-10-29T22:00:01"
}
```

### Wallet Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/wallets/user/{userId}` | Get wallet by user ID |
| POST | `/api/wallets/pending` | Add pending balance |
| POST | `/api/wallets/confirm` | Confirm pending balance |

**Example - Get Wallet:**

```bash
curl -X GET http://localhost:8080/api/wallets/user/1
```

**Response:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 150000.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 0.00,
    "totalEarned": 200000.00,
    "totalWithdrawn": 0.00,
    "createdAt": "2025-10-29T22:00:00",
    "updatedAt": "2025-10-29T22:30:00"
  },
  "timestamp": "2025-10-29T22:30:01"
}
```

**Example - Add Pending Balance:**

```bash
curl -X POST http://localhost:8080/api/wallets/pending \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 25000.00,
    "description": "Cashback from Shopee order #12345"
  }'
```

**Example - Confirm Pending Balance:**

```bash
curl -X POST http://localhost:8080/api/wallets/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 25000.00,
    "description": "Order confirmed by Shopee"
  }'
```

## 🗄️ Database Schema

### User Table

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| keycloak_id | VARCHAR(255) | Keycloak UUID (unique) |
| username | VARCHAR(100) | Username (unique) |
| email | VARCHAR(255) | Email (unique) |
| full_name | VARCHAR(255) | Full name |
| phone | VARCHAR(20) | Phone number |
| referral_code | VARCHAR(20) | Unique referral code (CBxxxxxx) |
| referred_by | VARCHAR(20) | Referrer's code |
| status | VARCHAR(20) | ACTIVE/INACTIVE/BANNED/SUSPENDED |
| created_at | DATETIME | Creation timestamp |
| deleted_at | DATETIME | Soft delete timestamp |

### User Wallet Table

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| user_id | BIGINT | Foreign key to user (unique) |
| balance | DECIMAL(12,2) | Available balance |
| pending_balance | DECIMAL(12,2) | Pending confirmation |
| locked_balance | DECIMAL(12,2) | Reserved for payout |
| total_earned | DECIMAL(12,2) | Lifetime earnings |
| total_withdrawn | DECIMAL(12,2) | Lifetime withdrawals |
| created_at | DATETIME | Creation timestamp |
| updated_at | DATETIME | Last update timestamp |

## 🔧 Technology Stack

### Core Frameworks
- **Spring Boot 3.4.1** - Application framework
- **Spring Data JPA** - Data access
- **Spring Web** - REST API
- **Spring Validation** - Bean validation

### Database
- **MySQL 8.3.0** - Primary database
- **Liquibase 4.31.0** - Database migration
- **HikariCP** - Connection pooling

### Authentication
- **Keycloak 26.0.7** - Identity & Access Management
- **OAuth2/OIDC** - Authentication protocol

### Utilities
- **Lombok 1.18.36** - Reduce boilerplate
- **MapStruct 1.6.3** - Object mapping
- **Apache Commons Lang3 3.17.0** - Utilities

### Documentation & Monitoring
- **SpringDoc OpenAPI 2.7.0** - API documentation
- **Spring Boot Actuator** - Health checks & metrics

### Testing
- **JUnit 5.11.4** - Testing framework
- **Mockito 5.14.2** - Mocking
- **Testcontainers 1.20.4** - Integration testing

## 📁 Project Structure

```
cashbee-backend/
│
├── cashbee-common/
│   └── src/main/java/com/cashbee/common/
│       ├── exception/       # Custom exceptions
│       ├── constant/        # Constants, Error codes
│       ├── validation/      # Custom validators
│       └── util/           # Utility classes
│
├── cashbee-domain/
│   └── src/main/java/com/cashbee/domain/
│       ├── model/          # Domain models (User, UserWallet)
│       ├── repository/     # Repository interfaces (Ports)
│       └── enums/          # Domain enums
│
├── cashbee-infrastructure/
│   └── src/main/java/com/cashbee/infrastructure/
│       └── persistence/
│           ├── entity/     # JPA entities
│           ├── repository/ # Spring Data repositories
│           ├── mapper/     # Domain ↔ JPA mappers
│           └── adapter/    # Repository implementations
│
├── cashbee-application/
│   └── src/main/java/com/cashbee/application/
│       ├── dto/            # Request/Response DTOs
│       ├── port/           # Domain ↔ DTO mappers
│       └── usecase/        # Use case implementations
│
└── cashbee-presentation/
    ├── src/main/java/com/cashbee/presentation/
    │   ├── controller/     # REST controllers
    │   ├── handler/        # Exception handlers
    │   ├── config/         # Configuration classes
    │   └── CashbeeApplication.java
    └── src/main/resources/
        ├── application.yml
        └── db/changelog/   # Liquibase migrations
```

## 🔐 Security Notes

### Keycloak Integration

Keycloak quản lý toàn bộ authentication và authorization:

1. **User Authentication**: Users login qua Keycloak, nhận JWT token
2. **User Sync**: Khi user login, backend sync thông tin từ Keycloak
3. **Authorization**: Sử dụng PEP (Policy Enforcement Point) để kiểm tra quyền

### Password Management

- ❌ Backend **KHÔNG LƯU** password
- ✅ Keycloak quản lý hoàn toàn password & credentials
- ✅ Backend chỉ lưu `keycloak_id` để link với Keycloak user

## 🧪 Testing

### Run Tests

```bash
# Run all tests
./mvnw test

# Run tests with coverage
./mvnw clean test jacoco:report

# View coverage report
open cashbee-common/target/site/jacoco/index.html
```

### Coverage Target

- **Minimum**: 80% line coverage
- **Focus**: Business logic trong domain và application layers

## 📝 Development Guidelines

### Domain Layer Rules

✅ **DO:**
- Pure POJOs, no infrastructure dependencies
- Rich domain models with business logic
- Use primitive types for foreign keys (Long userId)
- Define repository interfaces (Ports)

❌ **DON'T:**
- Use JPA annotations (@Entity, @Table)
- Use relationships (@OneToOne, @ManyToOne)
- Depend on Spring, Jackson, or any framework

### Use Case Pattern

Mỗi use case nên:
1. Có một trách nhiệm rõ ràng
2. Annotate với `@Service` và `@Transactional`
3. Inject repositories qua constructor
4. Return DTOs (không return domain models directly)

### API Response Format

Tất cả endpoints return `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "errorCode": null,
  "timestamp": "2025-10-29T22:00:00"
}
```

## 🐛 Common Issues & Solutions

### Issue: Liquibase fails to run

**Solution**: Kiểm tra database connection và đảm bảo user có đủ quyền:

```sql
GRANT ALL PRIVILEGES ON cashbee.* TO 'cashbee_user'@'localhost';
```

### Issue: MapStruct mappers not generated

**Solution**: Run clean install để trigger annotation processing:

```bash
./mvnw clean install
```

### Issue: Cannot find symbol errors

**Solution**: Đảm bảo modules được build theo đúng thứ tự (Maven reactor tự động xử lý)

## 📞 Support

- **Email**: dev@cashbee.com
- **Documentation**: [API Docs](http://localhost:8080/swagger-ui.html)
- **Issues**: Create issue trên repository

## 📄 License

Proprietary - CashBee Team

---

**Built with ❤️ by CashBee Development Team**

*Last Updated: 2025-10-29*
