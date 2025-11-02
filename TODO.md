# 📋 CashBee Backend - TODO List

**Last Updated**: 2025-10-29
**Project Status**: Core foundation completed ✅
**Next Phase**: Feature implementation & Keycloak integration

---

## ✅ Đã hoàn thành (Phase 1)

- [x] Phân tích nghiệp vụ và thiết kế database
- [x] Setup project structure (Multi-module Maven)
- [x] Module 1: cashbee-common (Exceptions, Utils, Validators)
- [x] Module 2: cashbee-domain (Pure domain models - NO JPA)
- [x] Module 3: cashbee-application (Use cases, DTOs)
- [x] Module 4: cashbee-infrastructure (JPA, Adapters, Mappers)
- [x] Module 6: cashbee-presentation (REST API, Controllers)
- [x] Database schema với Liquibase migrations
- [x] API documentation (Swagger/OpenAPI)
- [x] Comprehensive documentation (README, CONTRIBUTING, TESTING_GUIDE)
- [x] Build verification: SUCCESS ✅

---

## 🚀 Công việc còn lại (Phase 2)

### 📌 **PRIORITY 1: CRITICAL** (Cần làm ngay)

#### 1. Testing & Quality Assurance ⏰ **HIGH PRIORITY**

**File location**: `*/src/test/java/`

**Tasks**:

- [ ] **1.1. Unit Tests cho Domain Layer**
  - Location: `cashbee-domain/src/test/java/com/cashbee/domain/model/`
  - Files cần test:
    - [ ] `UserTest.java` - Test business logic methods
      - Test `activate()`, `ban()`, `suspend()`
      - Test `isActive()`, `isBanned()`, `isDeleted()`
      - Test `validate()` method
      - Test `hasReferrer()` logic
    - [ ] `UserWalletTest.java` - Test wallet operations
      - Test `addPendingBalance()`
      - Test `confirmPendingBalance()`
      - Test `lockBalance()`, `unlockBalance()`
      - Test `deductBalance()`
      - Test `cancelPendingBalance()`
      - Test validation methods
      - Test edge cases (negative amounts, insufficient balance)
  - **Target**: 80%+ coverage cho domain logic

- [ ] **1.2. Unit Tests cho Application Layer**
  - Location: `cashbee-application/src/test/java/com/cashbee/application/usecase/`
  - Files cần test:
    - [ ] `SyncUserFromKeycloakUseCaseTest.java`
      - Test new user creation
      - Test existing user update
      - Test wallet auto-creation
      - Test referral code generation
      - Test referral validation
    - [ ] `AddPendingBalanceUseCaseTest.java`
      - Test successful addition
      - Test wallet not found
      - Test negative amounts
    - [ ] `ConfirmPendingBalanceUseCaseTest.java`
      - Test successful confirmation
      - Test insufficient pending balance
      - Test wallet statistics update
  - **Framework**: JUnit 5 + Mockito
  - **Mocking**: Repository interfaces

- [ ] **1.3. Integration Tests**
  - Location: `cashbee-infrastructure/src/test/java/`
  - Setup:
    - [ ] Add Testcontainers dependency
    - [ ] Configure MySQL container
    - [ ] Create test data fixtures
  - Files cần test:
    - [ ] `UserRepositoryAdapterIntegrationTest.java`
      - Test CRUD operations
      - Test query methods (findByKeycloakId, findByEmail, etc.)
      - Test soft delete
    - [ ] `UserWalletRepositoryAdapterIntegrationTest.java`
      - Test save/retrieve
      - Test aggregate queries (calculateTotalBalance, etc.)
      - Test foreign key constraints
  - **Framework**: Spring Boot Test + Testcontainers

- [ ] **1.4. API Integration Tests**
  - Location: `cashbee-presentation/src/test/java/com/cashbee/presentation/controller/`
  - Files cần test:
    - [ ] `UserControllerIntegrationTest.java`
      - Test POST /api/users/sync (success, validation errors)
      - Test GET /api/users/keycloak/{id} (success, not found)
    - [ ] `WalletControllerIntegrationTest.java`
      - Test all wallet endpoints
      - Test error scenarios
  - **Framework**: MockMvc + @SpringBootTest

**Commands**:
```bash
# Run all tests
./mvnw test

# Run tests with coverage
./mvnw clean test jacoco:report

# View coverage report
open cashbee-domain/target/site/jacoco/index.html
```

---

#### 2. Module 5: Keycloak Integration ⏰ **HIGH PRIORITY**

**Module**: `cashbee-keycloak-integration/`

**Tasks**:

- [ ] **2.1. Create module structure**
  ```bash
  mkdir -p cashbee-keycloak-integration/src/main/java/com/cashbee/keycloak/{config,client,service}
  ```

- [ ] **2.2. Setup dependencies in pom.xml**
  - [ ] Spring Boot Starter Security
  - [ ] Spring Security OAuth2 Resource Server
  - [ ] Spring Security OAuth2 Client
  - [ ] Keycloak Spring Boot Starter
  - [ ] Keycloak Admin Client

- [ ] **2.3. OAuth2/OIDC Configuration**
  - File: `KeycloakSecurityConfig.java`
  - Tasks:
    - [ ] Configure JWT token validation
    - [ ] Setup resource server
    - [ ] Configure CORS
    - [ ] Setup security filter chain
    - [ ] Disable CSRF for stateless API

- [ ] **2.4. Keycloak Admin Client**
  - File: `KeycloakAdminClientConfig.java`
  - Tasks:
    - [ ] Configure admin client
    - [ ] Setup connection pooling
    - [ ] Configure realm and credentials

- [ ] **2.5. User Sync Service**
  - File: `KeycloakUserSyncService.java`
  - Tasks:
    - [ ] Get user info from Keycloak by ID
    - [ ] Map Keycloak user to UserSyncCommand
    - [ ] Call SyncUserFromKeycloakUseCase
    - [ ] Handle sync errors

- [ ] **2.6. JWT Token Utilities**
  - File: `JwtTokenUtils.java`
  - Tasks:
    - [ ] Extract Keycloak user ID from token
    - [ ] Extract user roles
    - [ ] Validate token
    - [ ] Get current authenticated user

- [ ] **2.7. Update application.yml**
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: http://localhost:8180/realms/cashbee
            jwk-set-uri: http://localhost:8180/realms/cashbee/protocol/openid-connect/certs

  keycloak:
    auth-server-url: http://localhost:8180
    realm: cashbee
    resource: cashbee-backend
    credentials:
      secret: ${KEYCLOAK_CLIENT_SECRET}
  ```

- [ ] **2.8. Secure API endpoints**
  - Update controllers với `@PreAuthorize`
  - Add role-based access control

**Documentation needed**:
- [ ] Keycloak setup guide
- [ ] How to obtain tokens
- [ ] Role configuration

---

### 📌 **PRIORITY 2: IMPORTANT** (Cần làm trong tuần)

#### 3. Enhanced Wallet Features

**Location**: Extend existing use cases

- [ ] **3.1. Lock Balance Use Case**
  - File: `LockBalanceUseCase.java`
  - Purpose: Lock balance for pending payout
  - API: POST /api/wallets/lock

- [ ] **3.2. Unlock Balance Use Case**
  - File: `UnlockBalanceUseCase.java`
  - Purpose: Unlock balance when payout cancelled
  - API: POST /api/wallets/unlock

- [ ] **3.3. Deduct Balance Use Case**
  - File: `DeductBalanceUseCase.java`
  - Purpose: Deduct balance after successful payout
  - API: POST /api/wallets/deduct

- [ ] **3.4. Get Balance History Use Case**
  - Requires: Transaction history table
  - See section 6 below

---

#### 4. Transaction History Feature

**New domain entity needed**: `Transaction`

- [ ] **4.1. Domain Model**
  - File: `cashbee-domain/src/main/java/com/cashbee/domain/model/Transaction.java`
  - Fields:
    - id, userId, type (PENDING_ADD, CONFIRM, LOCK, UNLOCK, DEDUCT)
    - amount, description
    - balanceBefore, balanceAfter
    - createdAt

- [ ] **4.2. Repository Interface**
  - File: `TransactionRepository.java`

- [ ] **4.3. JPA Entity + Repository**
  - Files in infrastructure layer

- [ ] **4.4. Liquibase Migration**
  - File: `003-create-transaction-table.xml`

- [ ] **4.5. Use Cases**
  - `GetTransactionHistoryUseCase.java`
  - Support pagination

- [ ] **4.6. API Endpoint**
  - GET /api/transactions/user/{userId}
  - Query params: page, size, type, from, to

---

#### 5. Admin Dashboard Endpoints

**Location**: `cashbee-presentation/src/main/java/com/cashbee/presentation/controller/AdminController.java`

- [ ] **5.1. User Management**
  - [ ] GET /api/admin/users (list with pagination, search)
  - [ ] GET /api/admin/users/{id}
  - [ ] PUT /api/admin/users/{id}/status (activate/ban/suspend)
  - [ ] DELETE /api/admin/users/{id} (soft delete)

- [ ] **5.2. Wallet Management**
  - [ ] GET /api/admin/wallets (list with filters)
  - [ ] POST /api/admin/wallets/{id}/adjust (manual adjustment)
  - [ ] GET /api/admin/wallets/statistics

- [ ] **5.3. System Statistics**
  - [ ] GET /api/admin/stats/overview
    - Total users, active users
    - Total balance, pending balance
    - Total earned, total withdrawn
  - [ ] GET /api/admin/stats/referrals
    - Top referrers
    - Referral conversion rate

- [ ] **5.4. Security**
  - [ ] Add ADMIN role check với Keycloak
  - [ ] Audit logging for admin actions

---

### 📌 **PRIORITY 3: NICE TO HAVE** (Có thể làm sau)

#### 6. Affiliate Network Integration

**Module**: New module `cashbee-affiliate/` or in `cashbee-application`

- [ ] **6.1. Shopee Affiliate API Client**
  - File: `ShopeeAffiliateClient.java`
  - Tasks:
    - [ ] Get affiliate link
    - [ ] Track clicks (if needed)
    - [ ] Get order status
    - [ ] Get commission info

- [ ] **6.2. Affiliate Import Feature**
  - [ ] Excel file parser (Apache POI)
  - [ ] JSON file parser
  - [ ] Validation logic
  - [ ] Bulk import use case

- [ ] **6.3. Cashback Calculation**
  - Domain: `CashbackCalculation` model
  - Business rules:
    - [ ] Calculate commission from order value
    - [ ] Calculate user cashback from commission
    - [ ] Calculate referrer bonus (if applicable)

---

#### 7. Payout Management

**New domain entities**: `PayoutRequest`, `PayoutBatch`

- [ ] **7.1. Domain Models**
  - `PayoutRequest` - User withdrawal request
  - `PayoutBatch` - Grouped payout for processing

- [ ] **7.2. Database Schema**
  - Liquibase migrations for payout tables

- [ ] **7.3. Use Cases**
  - `CreatePayoutRequestUseCase` - User requests withdrawal
  - `ApprovePayoutRequestUseCase` - Admin approves
  - `RejectPayoutRequestUseCase` - Admin rejects
  - `ProcessPayoutBatchUseCase` - Process multiple payouts

- [ ] **7.4. API Endpoints**
  - POST /api/payouts/request
  - GET /api/payouts/user/{userId}
  - PUT /api/admin/payouts/{id}/approve
  - PUT /api/admin/payouts/{id}/reject

---

#### 8. Performance Optimization

- [ ] **8.1. Add Caching**
  - [ ] Setup Redis
  - [ ] Cache user data
  - [ ] Cache wallet data
  - [ ] Add @Cacheable annotations

- [ ] **8.2. Query Optimization**
  - [ ] Add database indexes review
  - [ ] Optimize N+1 queries
  - [ ] Add query hints if needed

- [ ] **8.3. API Rate Limiting**
  - [ ] Add rate limiting filter
  - [ ] Configure limits per endpoint

---

#### 9. Additional Documentation

- [ ] **9.1. Deployment Guide**
  - Docker compose setup
  - Kubernetes manifests
  - Environment configuration

- [ ] **9.2. Keycloak Setup Guide**
  - Realm configuration
  - Client setup
  - Role mapping

- [ ] **9.3. API Client Examples**
  - JavaScript/TypeScript examples
  - Java examples
  - Postman collection

- [ ] **9.4. Architecture Decision Records (ADRs)**
  - Why hexagonal architecture?
  - Why no JPA in domain?
  - Why primitive foreign keys?

---

## 📝 Checklist cho ngày mai (Quick Start)

### Option A: Continue with Testing (Recommended)
```bash
# 1. Create test structure
mkdir -p cashbee-domain/src/test/java/com/cashbee/domain/model

# 2. Add test dependencies to pom.xml (already included)

# 3. Start with UserTest.java
# Write tests for User domain model

# 4. Run tests
./mvnw test -pl cashbee-domain

# 5. Check coverage
./mvnw test jacoco:report -pl cashbee-domain
```

### Option B: Start Keycloak Integration
```bash
# 1. Create module
mkdir -p cashbee-keycloak-integration/src/main/java/com/cashbee/keycloak

# 2. Create pom.xml for module

# 3. Download and run Keycloak
docker run -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0.7 start-dev

# 4. Configure realm in Keycloak UI (http://localhost:8180)

# 5. Implement KeycloakSecurityConfig.java
```

### Option C: Enhanced Wallet Features
```bash
# 1. Implement LockBalanceUseCase
touch cashbee-application/src/main/java/com/cashbee/application/usecase/wallet/LockBalanceUseCase.java

# 2. Add API endpoint to WalletController

# 3. Test the endpoint

# 4. Repeat for UnlockBalanceUseCase and DeductBalanceUseCase
```

---

## 🎯 Recommended Sequence

**Week 1**: Testing & Quality
1. Day 1: Unit tests for domain (User + UserWallet)
2. Day 2: Unit tests for application use cases
3. Day 3: Integration tests for infrastructure
4. Day 4: API integration tests
5. Day 5: Fix issues, improve coverage

**Week 2**: Keycloak Integration
1. Day 1-2: Setup Keycloak, configure realm
2. Day 3-4: Implement security configuration
3. Day 5: Test authentication flow

**Week 3**: Enhanced Features
1. Day 1-2: Transaction history
2. Day 3-4: Admin dashboard
3. Day 5: Enhanced wallet operations

**Week 4**: Additional Features
1. Day 1-3: Affiliate integration
2. Day 4-5: Payout management

---

## 📊 Progress Tracking

**Current Status**: 12/20 tasks completed (60%)

**Core Foundation**: ✅ 100%
- Module structure: ✅
- Domain layer: ✅
- Infrastructure: ✅
- Application: ✅
- Presentation: ✅
- Documentation: ✅

**Testing**: ⏳ 0%
- Unit tests: ❌
- Integration tests: ❌
- API tests: ❌

**Features**: ⏳ 30%
- User sync: ✅
- Basic wallet: ✅
- Keycloak: ❌
- Transaction history: ❌
- Admin dashboard: ❌
- Affiliate: ❌
- Payout: ❌

---

## 💡 Tips & Notes

### Testing Tips
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Use `@SpringBootTest` + `@Testcontainers` for integration tests
- Aim for 80%+ coverage on domain and application layers
- Focus on testing business logic, not getters/setters

### Keycloak Tips
- Use Docker for local Keycloak instance
- Create separate realm for development
- Test with real JWT tokens
- Document the OAuth2 flow

### Git Workflow
```bash
# For each feature
git checkout -b feature/transaction-history
# ... make changes ...
git add .
git commit -m "feat(wallet): add transaction history"
git push origin feature/transaction-history
# Create PR
```

---

## 📞 References

- **Project Docs**: README.md, CONTRIBUTING.md, TESTING_GUIDE.md
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Architecture**: HEXAGONAL_ARCHITECTURE.md
- **Spring Boot**: https://docs.spring.io/spring-boot/docs/3.4.1/reference/html/
- **Keycloak**: https://www.keycloak.org/docs/latest/

---

## ✅ When a task is completed:
1. Mark it with `[x]` in this file
2. Run tests to verify
3. Update documentation if needed
4. Commit with conventional commit message
5. Move to next task

---

**Last Checkpoint**: 2025-10-29 - Core foundation completed successfully ✅
**Next Checkpoint**: After completing Priority 1 tasks

**Good luck! 🚀**
