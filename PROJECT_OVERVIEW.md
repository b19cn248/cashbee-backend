# CashBee Backend - Project Overview

## 1. Project Description

**CashBee** is a **Cashback Platform** that allows users to earn cashback rewards when shopping through affiliate links. The platform integrates with e-commerce partners (like Shopee) to track purchases and automatically calculate cashback for users.

### Key Features
- **Cashback Rewards**: Earn cashback on purchases through affiliate tracking
- **Wallet System**: Manage pending/confirmed/locked balances
- **Payout System**: Withdraw earnings to bank accounts
- **Referral Program**: Earn commissions by referring new users
- **Milestone Rewards**: Bonus rewards for reaching referral milestones
- **Voucher System**: Promotional vouchers and flash sales
- **Multi-tier Referrer System**: Different commission rates based on referrer tier

---

## 2. Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.1 |
| **Cloud** | Spring Cloud | 2024.0.0 |
| **Database** | MySQL | 8.3.0 |
| **Migrations** | Liquibase | 4.31.0 |
| **Authentication** | Keycloak | 26.0.7 |
| **Object Mapping** | MapStruct | 1.6.3 |
| **Boilerplate Reduction** | Lombok | 1.18.36 |
| **API Documentation** | SpringDoc OpenAPI | 2.7.0 |
| **Excel Processing** | Apache POI | 5.3.0 |
| **JWT** | JJWT | 0.12.6 |
| **Testing** | JUnit 5 | 5.11.4 |
| **Mocking** | Mockito | 5.14.2 |
| **Integration Testing** | Testcontainers | 1.20.4 |

---

## 3. Architecture

This project follows **Hexagonal Architecture (Ports & Adapters)** pattern, which separates business logic from infrastructure concerns.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │   REST Controllers  │  Security Config  │  Liquibase Migrations │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ calls
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         APPLICATION LAYER                                │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │     Use Cases (Business Workflows)     │        DTOs            │   │
│  │   - Each use case = one class          │   - Request/Response   │   │
│  │   - Single execute() method            │   - Command objects    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ uses
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │   Domain Models (Pure POJOs)   │   Repository Interfaces (Ports) │   │
│  │   - No JPA annotations         │   - Define contracts            │   │
│  │   - Business logic here        │   - No implementation           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────▲──────────────────────────────────────┘
                                   │ implements
                                   │
┌─────────────────────────────────────────────────────────────────────────┐
│                       INFRASTRUCTURE LAYER                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  JPA Entities  │  Repository Adapters  │  MapStruct Mappers     │   │
│  │  (@Entity)     │  (impl interfaces)    │  (Domain <-> Entity)   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │   MySQL Database │
                          └─────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                          COMMON MODULE                                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │   Exceptions   │   Constants   │   Utilities   │   Shared Code   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Architecture Rules

| Layer | Rules |
|-------|-------|
| **Domain** | Pure POJOs, NO JPA annotations, NO Spring dependencies, business logic lives here |
| **Application** | Use cases orchestrate workflows, annotated with `@Service` and `@Transactional` |
| **Infrastructure** | JPA entities, Repository adapters implement domain interfaces |
| **Presentation** | REST Controllers call use cases, return `ApiResponse<T>` wrapper |

---

## 4. Module Structure

```
cashbee-backend/
├── cashbee-presentation/     # REST Controllers, Main App, Migrations
│   └── src/main/
│       ├── java/.../controller/
│       └── resources/
│           └── db/changelog/  # Liquibase migrations
│
├── cashbee-application/      # Use Cases, DTOs, Application Services
│   └── src/main/java/
│       ├── usecase/          # Business use cases
│       ├── dto/              # Data Transfer Objects
│       └── service/          # Application services
│
├── cashbee-domain/           # Pure domain models, Repository interfaces
│   └── src/main/java/
│       ├── model/            # Domain entities (POJOs)
│       └── repository/       # Repository interfaces (Ports)
│
├── cashbee-infrastructure/   # JPA Entities, Repository Adapters
│   └── src/main/java/
│       └── persistence/
│           ├── entity/       # JPA Entities
│           ├── repository/   # JPA Repositories
│           ├── adapter/      # Repository Adapters
│           └── mapper/       # MapStruct Mappers
│
└── cashbee-common/           # Shared utilities, exceptions, constants
```

### Module Dependencies

```
cashbee-presentation
         │
         ▼
cashbee-application
         │
         ▼
cashbee-domain ◄──── cashbee-infrastructure
         │                    │
         └────────┬───────────┘
                  ▼
           cashbee-common
```

---

## 5. Domain Models (24 Entities)

### Core Business Entities

| Entity | Description |
|--------|-------------|
| `User` | User account information, linked to Keycloak |
| `UserWallet` | User's wallet with pending/confirmed/locked balances |
| `Transaction` | All financial transactions (cashback, payout, referral) |

### Affiliate & Cashback

| Entity | Description |
|--------|-------------|
| `AffiliatePlatform` | E-commerce platforms (Shopee, Lazada, etc.) |
| `AffiliateOrder` | Orders from affiliate platforms |
| `AffiliateOrderItem` | Individual items in an order |
| `AffiliateClick` | Click tracking for affiliate links |
| `Cashback` | Cashback records per order item |
| `CashbackPolicy` | Cashback rate configuration per platform/category |

### Payout & Banking

| Entity | Description |
|--------|-------------|
| `PayoutRequest` | User withdrawal requests |
| `Bank` | Supported banks list |
| `UserBankAccount` | User's saved bank accounts |
| `BatchTransferExport` | Batch payout exports for bank processing |
| `BatchTransferItem` | Individual items in batch transfer |
| `BatchCashbackSnapshot` | Snapshot of cashbacks included in batch |
| `PaymentInvoice` | Payment invoices/receipts |

### Referral System

| Entity | Description |
|--------|-------------|
| `ReferralReward` | Rewards for referring new users |
| `ReferrerTierConfig` | Tier configuration (Bronze, Silver, Gold, etc.) |
| `ReferrerCommission` | Commission records for referrers |
| `MilestoneConfig` | Milestone bonus configuration |

### Others

| Entity | Description |
|--------|-------------|
| `ImportBatch` | Tracking for order import batches |
| `OtpVerification` | OTP verification records |
| `PromotionVoucher` | Promotional vouchers |
| `UserNotificationPreference` | User notification settings |

---

## 6. REST API Controllers (22 Controllers)

### User & Authentication
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `AuthController` | `/api/auth/*` | User registration, OTP verification |
| `UserController` | `/api/users/*` | User profile management |

### Wallet & Transactions
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `WalletController` | `/api/wallet/*` | Wallet balance operations |
| `TransactionController` | `/api/transactions/*` | Transaction history |

### Affiliate & Orders
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `AffiliateOrderController` | `/api/orders/*` | Order management |
| `AffiliateTrackingController` | `/api/tracking/*` | Click tracking, redirect handling |
| `AffiliateImportController` | `/api/import/*` | Order import from Excel |
| `AffiliatePlatformController` | `/api/platforms/*` | Platform configuration |
| `UserOrderController` | `/api/user/orders/*` | User's order history |
| `CashbackPolicyController` | `/api/cashback-policies/*` | Cashback rate configuration |

### Payout & Banking
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `PayoutController` | `/api/payouts/*` | Payout request management |
| `BankController` | `/api/banks/*` | Bank list |
| `BatchTransferController` | `/api/batch-transfers/*` | Batch payout processing |
| `InvoiceController` | `/api/invoices/*` | Payment invoices |

### Referral System
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `ReferralController` | `/api/referrals/*` | Referral management |
| `ReferrerTierAdminController` | `/api/admin/tiers/*` | Tier configuration (Admin) |
| `MilestoneAdminController` | `/api/admin/milestones/*` | Milestone configuration (Admin) |

### Promotions
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `FlashSaleController` | `/api/flash-sales/*` | Flash sale products |
| `VoucherController` | `/api/vouchers/*` | Promotional vouchers |

### Admin
| Controller | Endpoints | Description |
|------------|-----------|-------------|
| `AdminController` | `/api/admin/*` | System statistics, admin operations |
| `NotificationPreferenceController` | `/api/notifications/*` | Notification settings |
| `KeycloakSetupController` | `/api/keycloak/*` | Keycloak integration setup |

---

## 7. Use Cases (90+ Use Cases)

### Wallet Operations
- `GetUserWalletUseCase` - Get user's wallet balance
- `AddPendingBalanceUseCase` - Add pending cashback
- `ConfirmPendingBalanceUseCase` - Confirm pending to available
- `LockBalanceUseCase` - Lock balance for payout
- `UnlockBalanceUseCase` - Unlock balance (cancel payout)
- `DeductBalanceUseCase` - Deduct from wallet
- `RecalculateWalletUseCase` - Recalculate wallet totals

### Payout Operations
- `CreatePayoutRequestUseCase` - Create withdrawal request
- `ApprovePayoutRequestUseCase` - Admin approves payout
- `RejectPayoutRequestUseCase` - Admin rejects payout
- `CompletePayoutRequestUseCase` - Mark payout as completed
- `CancelPayoutRequestUseCase` - User cancels payout
- `GetPayoutRequestsUseCase` - List payout requests

### Affiliate & Order
- `CreateAffiliateOrderUseCase` - Create new order
- `UpdateOrderStatusUseCase` - Update order status
- `GetOrderByIdUseCase` - Get order details
- `ImportShopeeOrdersUseCase` - Import orders from Shopee Excel
- `HandleClickRedirectUseCase` - Handle affiliate click redirect
- `CreateTrackingLinkUseCase` - Generate affiliate link
- `EstimateCashbackUseCase` - Estimate cashback before purchase

### Cashback
- `CalculateCashbackUseCase` - Calculate cashback amount
- `AddCashbackToWalletUseCase` - Add cashback to wallet
- `UpdateCashbackOnOrderStatusChangeUseCase` - Update on status change
- `GetActiveCashbackPolicyUseCase` - Get active policies
- `UpdateCashbackPolicyUseCase` - Update policy rates

### Referral System
- `ValidateReferralCodeUseCase` - Validate referral code
- `SetReferralCodeUseCase` - Set user's referral code
- `GetMyReferralsUseCase` - Get user's referrals
- `GetReferralStatsUseCase` - Get referral statistics
- `ProcessReferralOnOrderCompletedUseCase` - Process referral reward
- `CalculateReferrerCommissionUseCase` - Calculate commission
- `PayReferrerCommissionUseCase` - Pay commission to referrer
- `ProcessReferralMilestoneUseCase` - Process milestone bonus
- `UpdateReferrerTierUseCase` - Update referrer tier
- `SyncAllUsersCompletedOrdersUseCase` - Sync completed orders

### Referral Admin
- `GetAllTiersUseCase` / `CreateTierUseCase` / `UpdateTierUseCase` / `DeleteTierUseCase`
- `GetAllMilestonesUseCase` / `CreateMilestoneUseCase` / `UpdateMilestoneUseCase` / `DeleteMilestoneUseCase`
- `GrantMissingMilestoneRewardsUseCase` - Grant missing rewards
- `GrantMissingReferrerCommissionsUseCase` - Grant missing commissions

### User Management
- `SyncUserFromKeycloakUseCase` - Sync user from Keycloak
- `GetUserByKeycloakIdUseCase` - Get user by Keycloak ID
- `UpdateUserUseCase` / `UpdateUserProfileUseCase` - Update user
- `UpdateUserLevelUseCase` - Update user level
- `CheckFirstLoginUseCase` - Check if first login
- `UpdatePhoneAndReferralUseCase` - Update phone and referral

### Voucher & Promotions
- `CreateVoucherUseCase` - Create voucher
- `GetVouchersUseCase` - List vouchers
- `TrackVoucherViewUseCase` / `TrackVoucherClickUseCase` - Track analytics
- `BatchImportVouchersUseCase` - Bulk import vouchers
- `GetFlashSaleProductsUseCase` / `GetFlashSaleTimeSlotsUseCase` - Flash sales

### Invoice & Notification
- `GeneratePaymentInvoiceUseCase` - Generate invoice
- `GetUserInvoicesUseCase` / `GetInvoiceDetailUseCase` - Get invoices
- `SendInvoiceEmailUseCase` - Send invoice via email
- `GetNotificationPreferenceUseCase` / `UpdateNotificationPreferenceUseCase`

### Batch Transfer
- `ExportBatchTransferUseCase` - Export batch for bank
- `GenerateBatchTransferFileUseCase` - Generate Excel file
- `CompleteBatchTransferUseCase` - Mark batch as completed
- `GetBatchExportHistoryUseCase` - Get export history

### Admin
- `GetSystemStatisticsUseCase` - Get system statistics

---

## 8. Build & Run Commands

```bash
# Build all modules
./mvnw clean install

# Run application (default port 8080)
./mvnw spring-boot:run -pl cashbee-presentation

# Run tests
./mvnw test

# Run single test class
./mvnw test -pl cashbee-application -Dtest=AddPendingBalanceUseCaseTest

# Run tests with coverage report
./mvnw clean test jacoco:report

# Compile only (faster for checking compilation errors)
./mvnw compile

# Docker (includes MySQL + Backend)
docker-compose up -d
```

---

## 9. Database Management

### Liquibase Migrations
- **Location**: `cashbee-presentation/src/main/resources/db/changelog/`
- **Master file**: `db.changelog-master.xml`
- **Naming convention**: `NNN-description.xml` (e.g., `024-refactor-cashback-per-item.xml`)

### Key Tables
```
users                    - User accounts
user_wallets             - Wallet balances
transactions             - All financial transactions
affiliate_platforms      - E-commerce platforms
affiliate_orders         - Orders from platforms
affiliate_order_items    - Order items
cashbacks                - Cashback records
payout_requests          - Withdrawal requests
banks                    - Bank list
user_bank_accounts       - User bank accounts
referral_rewards         - Referral rewards
referrer_tier_configs    - Tier configuration
referrer_commissions     - Commission records
milestone_configs        - Milestone bonuses
promotion_vouchers       - Promotional vouchers
```

---

## 10. Authentication & Security

### Keycloak Integration
- **OAuth2/OIDC** authentication via Keycloak
- Backend validates JWT tokens from Keycloak
- Users are synced from Keycloak to local database
- Backend does NOT store passwords - Keycloak handles all auth

### Configuration
```yaml
# application.yml
keycloak:
  realm: cashbee
  auth-server-url: https://keycloak.example.com
  resource: cashbee-backend

spring.security.oauth2.resourceserver:
  jwt:
    issuer-uri: ${keycloak.auth-server-url}/realms/${keycloak.realm}
```

---

## 11. Code Patterns

### Use Case Pattern
```java
@Service
@RequiredArgsConstructor
@Transactional
public class SomeActionUseCase {
    private final SomeRepository repository;

    public ResponseDto execute(CommandDto command) {
        // 1. Find domain entity
        // 2. Call domain business method
        // 3. Save via repository
        // 4. Map to response DTO
    }
}
```

### Repository Adapter Pattern
```java
@Component
@RequiredArgsConstructor
public class SomeRepositoryAdapter implements SomeRepository {
    private final SomeJpaRepository jpaRepository;
    private final SomeMapper mapper;

    @Override
    public DomainModel save(DomainModel model) {
        JpaEntity entity = mapper.toEntity(model);
        JpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

---

## 12. Contact & Support

- **Project**: CashBee Cashback Platform
- **Tech Stack**: Java 21, Spring Boot 3.4.1, MySQL, Keycloak
- **Architecture**: Hexagonal (Ports & Adapters)

---

*Last updated: January 2026*
