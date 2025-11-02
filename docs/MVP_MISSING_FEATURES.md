# 📋 CashBee MVP - Missing Features Tracking Document

**Created**: 2025-11-01
**Purpose**: Track missing features to complete MVP based on business requirements
**Priority**: Focus on **BUSINESS FLOW (Cashback Flow)** first

---

## 📊 Current Status Overview

| Category | Status | Completion |
|----------|--------|------------|
| **Core Foundation** | ✅ Complete | 100% |
| **User & Wallet** | ✅ Complete | 100% |
| **Transaction & Payout** | ✅ Complete | 100% |
| **User Profile** | ❌ Missing | 0% |
| **Affiliate Module** | ❌ Missing | 0% |
| **Cashback Module** | ❌ Missing | 0% |
| **Admin Audit Log** | ❌ Missing | 0% |
| **System Config** | ❌ Missing | 0% |
| **Keycloak Integration** | ❌ Missing | 0% |

**Overall MVP Completion**: **40%**

---

## ✅ What We Have (Completed Modules)

### 1. User Module ✅
- ✅ Domain: `User.java`
- ✅ Database: `user` table (Liquibase migration 001)
- ✅ Use Cases:
  - `SyncUserFromKeycloakUseCase` - Sync user from Keycloak
  - `GetUserByKeycloakIdUseCase` - Get user by Keycloak ID
- ✅ API Endpoints:
  - `POST /api/users/sync` - Sync user
  - `GET /api/users/keycloak/{keycloakId}` - Get user

### 2. Wallet Module ✅
- ✅ Domain: `UserWallet.java`
- ✅ Database: `user_wallet` table (Liquibase migration 002)
- ✅ Use Cases:
  - `GetUserWalletUseCase`
  - `AddPendingBalanceUseCase`
  - `ConfirmPendingBalanceUseCase`
  - `LockBalanceUseCase`
  - `UnlockBalanceUseCase`
  - `DeductBalanceUseCase`
- ✅ API Endpoints:
  - `GET /api/wallets/user/{userId}` - Get wallet
  - `POST /api/wallets/pending` - Add pending balance
  - `POST /api/wallets/confirm` - Confirm pending balance

### 3. Transaction Module ✅
- ✅ Domain: `Transaction.java`
- ✅ Database: `transaction` table (Liquibase migration 003)
- ✅ Use Cases:
  - `CreateTransactionUseCase`
  - `GetTransactionHistoryUseCase`
- ✅ API: `TransactionController`

### 4. Payout Module ✅
- ✅ Domain: `PayoutRequest.java`
- ✅ Database: `payout_request` table (Liquibase migration 004)
- ✅ Use Cases:
  - `CreatePayoutRequestUseCase`
  - `GetPayoutRequestsUseCase`
  - `ApprovePayoutRequestUseCase`
  - `RejectPayoutRequestUseCase`
  - `CancelPayoutRequestUseCase`
  - `CompletePayoutRequestUseCase`
- ✅ API: `PayoutController`

### 5. Admin Statistics ✅
- ✅ Use Case: `GetSystemStatisticsUseCase`
- ✅ API: `AdminController`

---

## ❌ Missing Features for MVP (Prioritized)

---

## 🔥 PRIORITY 1: CRITICAL - BUSINESS FLOW (Cashback Flow)

These features are **ESSENTIAL** for the main cashback business flow to work.

### 1. Affiliate Platform Module ⏰ **CRITICAL**

**Purpose**: Manage affiliate platforms (Shopee, Lazada, TikTok)

**Missing Components**:

#### 1.1. Database
- ❌ Liquibase migration: `005-create-affiliate-platform-table.xml`
- **Table**: `affiliate_platform`
- **Fields**:
  - id, name, code
  - api_key, api_secret, base_url
  - default_commission_rate
  - status (ACTIVE, INACTIVE)
  - created_at, updated_at

#### 1.2. Domain Layer
- ❌ Domain Model: `AffiliatePlatform.java` in `cashbee-domain`
- ❌ Repository Interface: `AffiliatePlatformRepository.java`
- ❌ Enum: `PlatformStatus.java` (ACTIVE, INACTIVE)

#### 1.3. Infrastructure Layer
- ❌ JPA Entity: `AffiliatePlatformJpaEntity.java`
- ❌ JPA Repository: `AffiliatePlatformJpaRepository.java`
- ❌ Mapper: `AffiliatePlatformMapper.java` (Domain ↔ JPA)
- ❌ Adapter: `AffiliatePlatformRepositoryAdapter.java`

#### 1.4. Application Layer
- ❌ DTOs:
  - `AffiliatePlatformResponse.java`
  - `CreatePlatformCommand.java`
  - `UpdatePlatformCommand.java`
- ❌ Use Cases:
  - `GetAffiliatePlatformsUseCase.java`
  - `GetPlatformByCodeUseCase.java`

#### 1.5. Presentation Layer
- ❌ Controller: `AffiliatePlatformController.java`
- ❌ Endpoints:
  - `GET /api/admin/platforms` - List all platforms
  - `GET /api/admin/platforms/{id}` - Get platform details
  - `POST /api/admin/platforms` - Create platform
  - `PUT /api/admin/platforms/{id}` - Update platform

---

### 2. Affiliate Import Batch Module ⏰ **CRITICAL**

**Purpose**: Track file import batches from Shopee/Lazada

**Missing Components**:

#### 2.1. Database
- ❌ Liquibase migration: `006-create-affiliate-import-batch-table.xml`
- **Table**: `affiliate_import_batch`
- **Fields**:
  - id, batch_code, file_name, file_type (EXCEL, JSON)
  - file_size, platform_id (FK to affiliate_platform)
  - total_orders, success_orders, failed_orders
  - status (PROCESSING, COMPLETED, FAILED)
  - error_message, imported_by (FK to user), imported_at, completed_at

#### 2.2. Domain Layer
- ❌ Domain Model: `AffiliateImportBatch.java`
- ❌ Repository Interface: `AffiliateImportBatchRepository.java`
- ❌ Enum: `ImportBatchStatus.java` (PROCESSING, COMPLETED, FAILED) - **ALREADY EXISTS** ✅

#### 2.3. Infrastructure Layer
- ❌ JPA Entity: `AffiliateImportBatchJpaEntity.java`
- ❌ JPA Repository: `AffiliateImportBatchJpaRepository.java`
- ❌ Mapper: `AffiliateImportBatchMapper.java`
- ❌ Adapter: `AffiliateImportBatchRepositoryAdapter.java`

#### 2.4. Application Layer
- ❌ DTOs:
  - `ImportBatchResponse.java`
  - `ImportFileCommand.java`
  - `GetImportBatchesQuery.java`
- ❌ Use Cases:
  - `ImportShopeeFileUseCase.java` - **CRITICAL**
  - `GetImportBatchesUseCase.java`
  - `GetImportBatchDetailsUseCase.java`

#### 2.5. File Parser (New Component)
- ❌ Service: `ShopeeExcelParser.java` - Parse Excel files (Apache POI)
- ❌ Service: `ShopeeJsonParser.java` - Parse JSON files
- ❌ DTO: `ShopeeOrderData.java` - Represents raw order data from Shopee

#### 2.6. Presentation Layer
- ❌ Controller: `AffiliateImportController.java`
- ❌ Endpoints:
  - `POST /api/admin/imports/shopee` - **Upload Shopee file (Excel/JSON)**
  - `GET /api/admin/imports` - List import batches
  - `GET /api/admin/imports/{batchId}` - Get batch details
  - `GET /api/admin/imports/{batchId}/orders` - Get orders in batch

---

### 3. Affiliate Order Module ⏰ **CRITICAL**

**Purpose**: Store affiliate orders and items from imported files

**Missing Components**:

#### 3.1. Database
- ❌ Liquibase migration: `007-create-affiliate-order-table.xml`
- **Table**: `affiliate_order`
- **Fields**:
  - id, platform_id (FK), user_id (FK)
  - click_id, order_id (unique, platform order ID)
  - order_status (PENDING, APPROVED, CANCELLED, REJECTED, PAID)
  - product_name, product_price
  - commission_amount, currency
  - order_time, confirm_time, paid_time
  - source (IMPORT, API)
  - import_batch_id (FK)
  - created_at, updated_at, deleted_at

- ❌ Liquibase migration: `008-create-affiliate-order-item-table.xml`
- **Table**: `affiliate_order_item`
- **Fields**:
  - id, order_id (FK)
  - item_id, item_name, quantity
  - actual_amount, item_commission
  - shop_id, shop_name
  - category_lv1, category_lv2, category_lv3
  - img_url
  - brand_commission_rate, platform_commission_rate
  - created_at

#### 3.2. Domain Layer
- ❌ Domain Model: `AffiliateOrder.java`
- ❌ Domain Model: `AffiliateOrderItem.java`
- ❌ Repository Interface: `AffiliateOrderRepository.java`
- ❌ Repository Interface: `AffiliateOrderItemRepository.java`
- ❌ Enum: `OrderStatus.java` - **ALREADY EXISTS** ✅

#### 3.3. Infrastructure Layer
- ❌ JPA Entity: `AffiliateOrderJpaEntity.java`
- ❌ JPA Entity: `AffiliateOrderItemJpaEntity.java`
- ❌ JPA Repository: `AffiliateOrderJpaRepository.java`
- ❌ JPA Repository: `AffiliateOrderItemJpaRepository.java`
- ❌ Mappers: `AffiliateOrderMapper.java`, `AffiliateOrderItemMapper.java`
- ❌ Adapters: `AffiliateOrderRepositoryAdapter.java`, `AffiliateOrderItemRepositoryAdapter.java`

#### 3.4. Application Layer
- ❌ DTOs:
  - `AffiliateOrderResponse.java`
  - `AffiliateOrderItemResponse.java`
  - `GetUserOrdersQuery.java`
  - `CreateAffiliateOrderCommand.java`
  - `UpdateOrderStatusCommand.java`
- ❌ Use Cases:
  - `GetUserOrdersUseCase.java` - **User views their orders**
  - `GetOrderDetailsUseCase.java`
  - `CreateAffiliateOrderUseCase.java` - Called during import
  - `UpdateOrderStatusUseCase.java` - Update order status (triggers cashback)

#### 3.5. Presentation Layer
- ❌ Controller: `AffiliateOrderController.java`
- ❌ Endpoints:
  - `GET /api/orders/my` - **User views their orders**
  - `GET /api/orders/{orderId}` - Get order details
  - `GET /api/admin/orders` - Admin views all orders
  - `PUT /api/admin/orders/{orderId}/status` - Update order status

---

### 4. Cashback Module ⏰ **CRITICAL**

**Purpose**: Calculate and manage cashback for users

**Missing Components**:

#### 4.1. Database
- ❌ Liquibase migration: `009-create-cashback-table.xml`
- **Table**: `cashback`
- **Fields**:
  - id, user_id (FK), order_id (FK), platform_id (FK)
  - commission_amount (from order)
  - cashback_amount (calculated)
  - cashback_rate (policy rate used)
  - policy_id (FK to cashback_policy)
  - status (PENDING, CONFIRMED, PAID, CANCELLED, EXPIRED)
  - note
  - created_at, confirmed_at, paid_at, cancelled_at, updated_at

#### 4.2. Domain Layer
- ❌ Domain Model: `Cashback.java`
- ❌ Repository Interface: `CashbackRepository.java`
- ❌ Enum: `CashbackStatus.java` - **ALREADY EXISTS** ✅

#### 4.3. Infrastructure Layer
- ❌ JPA Entity: `CashbackJpaEntity.java`
- ❌ JPA Repository: `CashbackJpaRepository.java`
- ❌ Mapper: `CashbackMapper.java`
- ❌ Adapter: `CashbackRepositoryAdapter.java`

#### 4.4. Application Layer
- ❌ DTOs:
  - `CashbackResponse.java`
  - `CalculateCashbackCommand.java`
  - `ConfirmCashbackCommand.java`
  - `GetUserCashbackQuery.java`
- ❌ Use Cases:
  - `CalculateCashbackUseCase.java` - **Calculate cashback from order**
  - `ConfirmCashbackUseCase.java` - **Order APPROVED → Cashback CONFIRMED**
  - `PayCashbackUseCase.java` - **Order PAID → Move to wallet balance**
  - `GetUserCashbackUseCase.java`
  - `CancelCashbackUseCase.java`

#### 4.5. Presentation Layer
- ❌ Controller: `CashbackController.java`
- ❌ Endpoints:
  - `GET /api/cashback/my` - User views their cashback
  - `GET /api/cashback/{cashbackId}` - Get cashback details
  - `GET /api/admin/cashback` - Admin views all cashback

---

### 5. Cashback Policy Module ⏰ **HIGH**

**Purpose**: Manage cashback calculation policies

**Missing Components**:

#### 5.1. Database
- ❌ Liquibase migration: `010-create-cashback-policy-table.xml`
- **Table**: `cashback_policy`
- **Fields**:
  - id, policy_name, policy_code
  - platform_id (FK, nullable - null = all platforms)
  - user_level (NORMAL, VIP, SUPER)
  - cashback_rate (percentage of commission)
  - min_order_value, max_cashback_per_order
  - is_active, priority
  - effective_from, effective_to
  - created_at, updated_at

#### 5.2. Domain Layer
- ❌ Domain Model: `CashbackPolicy.java`
- ❌ Repository Interface: `CashbackPolicyRepository.java`
- ❌ Enum: `UserLevel.java` (NORMAL, VIP, SUPER)

#### 5.3. Infrastructure Layer
- ❌ JPA Entity: `CashbackPolicyJpaEntity.java`
- ❌ JPA Repository: `CashbackPolicyJpaRepository.java`
- ❌ Mapper: `CashbackPolicyMapper.java`
- ❌ Adapter: `CashbackPolicyRepositoryAdapter.java`

#### 5.4. Application Layer
- ❌ DTOs:
  - `CashbackPolicyResponse.java`
  - `CreatePolicyCommand.java`
  - `UpdatePolicyCommand.java`
- ❌ Use Cases:
  - `GetActiveCashbackPolicyUseCase.java` - **Get policy for calculation**
  - `GetCashbackPoliciesUseCase.java`
  - `CreateCashbackPolicyUseCase.java`
  - `UpdateCashbackPolicyUseCase.java`

#### 5.5. Presentation Layer
- ❌ Controller: `CashbackPolicyController.java`
- ❌ Endpoints:
  - `GET /api/admin/policies` - List policies
  - `GET /api/admin/policies/{id}` - Get policy details
  - `POST /api/admin/policies` - Create policy
  - `PUT /api/admin/policies/{id}` - Update policy

---

## 🔧 PRIORITY 2: IMPORTANT - Supporting Features

### 6. User Profile Module ⏰ **HIGH**

**Purpose**: Store extended user information (payment accounts, preferences)

**Missing Components**:

#### 6.1. Database
- ❌ Liquibase migration: `011-create-user-profile-table.xml`
- **Table**: `user_profile`
- **Fields**:
  - id, user_id (FK, unique)
  - avatar_url, date_of_birth, gender, address
  - city, province, country, postal_code
  - Payment info: momo_number, momo_name, zalopay_number, zalopay_name
  - bank_account_number, bank_account_name, bank_name, bank_branch
  - preferred_payout_method (MOMO, ZALOPAY, BANK)
  - notification_enabled, email_notification_enabled
  - created_at, updated_at

#### 6.2. Domain Layer
- ❌ Domain Model: `UserProfile.java`
- ❌ Repository Interface: `UserProfileRepository.java`
- ❌ Enum: `Gender.java` (MALE, FEMALE, OTHER)

#### 6.3. Infrastructure Layer
- ❌ JPA Entity: `UserProfileJpaEntity.java`
- ❌ JPA Repository: `UserProfileJpaRepository.java`
- ❌ Mapper: `UserProfileMapper.java`
- ❌ Adapter: `UserProfileRepositoryAdapter.java`

#### 6.4. Application Layer
- ❌ DTOs:
  - `UserProfileResponse.java`
  - `UpdateProfileCommand.java`
  - `UpdatePaymentInfoCommand.java`
- ❌ Use Cases:
  - `GetUserProfileUseCase.java`
  - `UpdateUserProfileUseCase.java` - **User updates profile**
  - `UpdatePaymentInfoUseCase.java`

#### 6.5. Presentation Layer
- ❌ Update `UserController.java` or create `ProfileController.java`
- ❌ Endpoints:
  - `GET /api/v1/profile` - **Get current user profile**
  - `PUT /api/v1/profile` - **Update profile**
  - `PUT /api/v1/profile/payment` - Update payment info

---

### 7. Admin Audit Log Module ⏰ **MEDIUM**

**Purpose**: Track all admin actions for security and compliance

**Missing Components**:

#### 7.1. Database
- ❌ Liquibase migration: `012-create-admin-audit-log-table.xml`
- **Table**: `admin_audit_log`
- **Fields**:
  - id, user_id (FK - admin user)
  - action (IMPORT_FILE, APPROVE_PAYOUT, UPDATE_CONFIG, etc.)
  - entity_type, entity_id
  - changes (JSON - before/after)
  - ip_address, user_agent, request_id
  - created_at

#### 7.2. Domain Layer
- ❌ Domain Model: `AdminAuditLog.java`
- ❌ Repository Interface: `AdminAuditLogRepository.java`
- ❌ Enum: `AdminAction.java`

#### 7.3. Infrastructure Layer
- ❌ JPA Entity: `AdminAuditLogJpaEntity.java`
- ❌ JPA Repository: `AdminAuditLogJpaRepository.java`
- ❌ Mapper: `AdminAuditLogMapper.java`
- ❌ Adapter: `AdminAuditLogRepositoryAdapter.java`

#### 7.4. Application Layer
- ❌ DTOs:
  - `AuditLogResponse.java`
  - `LogAdminActionCommand.java`
  - `GetAuditLogsQuery.java`
- ❌ Use Cases:
  - `LogAdminActionUseCase.java` - **Log admin actions**
  - `GetAuditLogsUseCase.java`

#### 7.5. Presentation Layer
- ❌ Update `AdminController.java`
- ❌ Endpoints:
  - `GET /api/v1/admin/audit-logs` - **View audit logs**

---

### 8. System Configuration Module ⏰ **MEDIUM**

**Purpose**: Manage system-wide configuration (min_payout_amount, etc.)

**Missing Components**:

#### 8.1. Database
- ❌ Liquibase migration: `013-create-system-config-table.xml`
- **Table**: `system_config`
- **Fields**:
  - id, config_key (unique), config_value
  - data_type (STRING, INTEGER, DECIMAL, BOOLEAN, JSON)
  - category (PAYOUT, CASHBACK, SYSTEM, NOTIFICATION)
  - description, is_editable
  - updated_by (FK to user)
  - created_at, updated_at

#### 8.2. Domain Layer
- ❌ Domain Model: `SystemConfig.java`
- ❌ Repository Interface: `SystemConfigRepository.java`
- ❌ Enum: `ConfigDataType.java`, `ConfigCategory.java`

#### 8.3. Infrastructure Layer
- ❌ JPA Entity: `SystemConfigJpaEntity.java`
- ❌ JPA Repository: `SystemConfigJpaRepository.java`
- ❌ Mapper: `SystemConfigMapper.java`
- ❌ Adapter: `SystemConfigRepositoryAdapter.java`

#### 8.4. Application Layer
- ❌ DTOs:
  - `SystemConfigResponse.java`
  - `UpdateConfigCommand.java`
- ❌ Use Cases:
  - `GetSystemConfigUseCase.java`
  - `UpdateSystemConfigUseCase.java`
  - `GetConfigByKeyUseCase.java`

#### 8.5. Presentation Layer
- ❌ Update `AdminController.java`
- ❌ Endpoints:
  - `GET /api/admin/config` - List all configs
  - `GET /api/admin/config/{key}` - Get config by key
  - `PUT /api/admin/config/{key}` - Update config

---

## 🔒 PRIORITY 3: SECURITY - Authentication & Authorization

### 9. Keycloak Integration Module ⏰ **HIGH**

**Purpose**: OAuth2/OIDC authentication and role-based authorization

**Missing Components**:

#### 9.1. New Module
- ❌ Create `cashbee-keycloak-integration` module
- ❌ pom.xml with dependencies:
  - Spring Boot Starter Security
  - Spring Security OAuth2 Resource Server
  - Spring Security OAuth2 Client
  - Keycloak Spring Boot Starter
  - Keycloak Admin Client

#### 9.2. Security Configuration
- ❌ `KeycloakSecurityConfig.java` - JWT validation, CORS, security filter chain
- ❌ `KeycloakAdminClientConfig.java` - Admin client setup
- ❌ `JwtTokenUtils.java` - Extract user info from JWT

#### 9.3. User Sync Service
- ❌ `KeycloakUserSyncService.java` - Auto-sync user on login

#### 9.4. Update application.yml
- ❌ Add OAuth2 resource server config
- ❌ Add Keycloak realm/client config

#### 9.5. Secure Endpoints
- ❌ Add `@PreAuthorize("hasRole('USER')")` to user endpoints
- ❌ Add `@PreAuthorize("hasRole('ADMIN')")` to admin endpoints

---

## 📈 Implementation Roadmap

### Phase 1: Affiliate & Cashback Flow (Weeks 1-3) ⏰ **CRITICAL**

**Goal**: Complete the main business flow - Cashback

**Week 1**: Affiliate Foundation
- Day 1-2: Affiliate Platform Module (database, domain, API)
- Day 3-5: Affiliate Import Batch Module (database, domain, file parser)

**Week 2**: Orders & Cashback
- Day 1-3: Affiliate Order Module (database, domain, API)
- Day 4-5: Cashback Module (database, domain, calculation logic)

**Week 3**: Cashback Policy & Integration
- Day 1-2: Cashback Policy Module
- Day 3-5: Integration testing (full cashback flow)

### Phase 2: User Profile & Admin Features (Week 4) ⏰ **HIGH**

**Week 4**: Supporting Features
- Day 1-2: User Profile Module
- Day 3-4: Admin Audit Log Module
- Day 5: System Configuration Module

### Phase 3: Security & Testing (Weeks 5-6) ⏰ **HIGH**

**Week 5**: Keycloak Integration
- Day 1-2: Setup Keycloak module, security config
- Day 3-4: JWT validation, user sync automation
- Day 5: Secure all endpoints with @PreAuthorize

**Week 6**: Testing & Bug Fixes
- Day 1-3: Integration testing (full flows)
- Day 4-5: Bug fixes, performance tuning

---

## 🎯 Success Criteria for MVP

MVP is considered complete when:

✅ **Main Business Flow Works**:
1. Admin can upload Shopee Excel/JSON file
2. System parses file and creates orders
3. System calculates cashback based on policy
4. User can view their orders and cashback
5. Cashback is confirmed when order is approved
6. Cashback is paid to wallet when order is paid
7. User can request payout
8. Admin can approve/reject payout

✅ **Security**:
- All endpoints protected with Keycloak OAuth2
- Role-based access control (USER, ADMIN)

✅ **Data Integrity**:
- All database tables created with proper constraints
- Foreign keys enforced
- Indexes optimized

✅ **Documentation**:
- API documentation (Swagger)
- Setup guide for Keycloak
- Testing guide

---

## 📝 Notes

- **Hexagonal Architecture**: All new modules must follow the same pattern (domain → infrastructure → application → presentation)
- **NO JPA in Domain**: Keep domain models pure POJOs
- **Foreign Keys as Primitives**: Use `Long userId` instead of `User user`
- **MapStruct for Mapping**: Auto-generate mappers
- **Liquibase for DB**: All schema changes through migrations
- **Testing**: Aim for 80%+ coverage on domain and application layers

---

**Last Updated**: 2025-11-01
**Next Review**: After completing Phase 1 (Affiliate & Cashback Flow)
