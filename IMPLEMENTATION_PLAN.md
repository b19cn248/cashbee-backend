# 🚀 CashBee MVP - Implementation Plan (BUSINESS FLOW FIRST)

**Created**: 2025-11-01
**Objective**: Complete the **MAIN CASHBACK BUSINESS FLOW** for MVP
**Approach**: Bottom-up implementation (Database → Domain → Infrastructure → Application → Presentation)

---

## 🎯 Goal: Complete Cashback Flow

The **Cashback Flow** (Section 5.2 of business requirements) is the CORE business value:

```
Admin uploads Shopee file
  ↓
Backend parses file & creates orders
  ↓
System calculates cashback based on policy
  ↓
User views orders & cashback status
  ↓
Order APPROVED → Cashback CONFIRMED (pending balance)
  ↓
Order PAID → Cashback PAID (available balance)
  ↓
User requests payout (✅ already implemented)
  ↓
Admin approves payout (✅ already implemented)
```

---

## 📋 Implementation Phases

---

## 🔥 PHASE 1: Affiliate Platform Foundation (Days 1-2)

**Goal**: Create the foundation for managing affiliate platforms (Shopee, Lazada, TikTok)

### Day 1: Database & Domain Layer

#### Step 1.1: Create Liquibase Migration
**File**: `cashbee-presentation/src/main/resources/db/changelog/005-create-affiliate-platform-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog>
  <changeSet id="005" author="cashbee">
    <createTable tableName="affiliate_platform">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true"/>
      </column>
      <column name="name" type="VARCHAR(50)">
        <constraints nullable="false" unique="true"/>
      </column>
      <column name="code" type="VARCHAR(20)">
        <constraints nullable="false" unique="true"/>
      </column>
      <column name="api_key" type="VARCHAR(255)"/>
      <column name="api_secret" type="VARCHAR(255)"/>
      <column name="base_url" type="VARCHAR(255)"/>
      <column name="default_commission_rate" type="DECIMAL(5,2)"/>
      <column name="status" type="VARCHAR(20)" defaultValue="ACTIVE"/>
      <column name="created_at" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP"/>
      <column name="updated_at" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
    </createTable>

    <createIndex tableName="affiliate_platform" indexName="idx_platform_code">
      <column name="code"/>
    </createIndex>
    <createIndex tableName="affiliate_platform" indexName="idx_platform_status">
      <column name="status"/>
    </createIndex>
  </changeSet>
</databaseChangeLog>
```

**Update**: `db.changelog-master.xml` - add `<include file="db/changelog/005-create-affiliate-platform-table.xml"/>`

#### Step 1.2: Create Domain Model
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/model/AffiliatePlatform.java`

```java
package com.cashbee.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePlatform {
    private Long id;
    private String name;          // "Shopee", "Lazada", "TikTok"
    private String code;          // "shopee", "lazada", "tiktok"
    private String apiKey;
    private String apiSecret;
    private String baseUrl;
    private BigDecimal defaultCommissionRate;
    private PlatformStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return this.status == PlatformStatus.ACTIVE;
    }

    public void activate() {
        this.status = PlatformStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = PlatformStatus.INACTIVE;
    }
}
```

#### Step 1.3: Create Enum
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/enums/PlatformStatus.java`

```java
package com.cashbee.domain.enums;

public enum PlatformStatus {
    ACTIVE,
    INACTIVE
}
```

#### Step 1.4: Create Repository Interface
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/repository/AffiliatePlatformRepository.java`

```java
package com.cashbee.domain.repository;

import com.cashbee.domain.model.AffiliatePlatform;
import java.util.List;
import java.util.Optional;

public interface AffiliatePlatformRepository {
    AffiliatePlatform save(AffiliatePlatform platform);
    Optional<AffiliatePlatform> findById(Long id);
    Optional<AffiliatePlatform> findByCode(String code);
    List<AffiliatePlatform> findAll();
    List<AffiliatePlatform> findAllActive();
    void deleteById(Long id);
}
```

### Day 2: Infrastructure & Application Layer

#### Step 1.5: Create JPA Entity
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/entity/AffiliatePlatformJpaEntity.java`

```java
package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.PlatformStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "affiliate_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePlatformJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 255)
    private String apiKey;

    @Column(length = 255)
    private String apiSecret;

    @Column(length = 255)
    private String baseUrl;

    @Column(precision = 5, scale = 2)
    private BigDecimal defaultCommissionRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlatformStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### Step 1.6: Create Spring Data JPA Repository
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/repository/AffiliatePlatformJpaRepository.java`

```java
package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.PlatformStatus;
import com.cashbee.infrastructure.persistence.entity.AffiliatePlatformJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliatePlatformJpaRepository extends JpaRepository<AffiliatePlatformJpaEntity, Long> {
    Optional<AffiliatePlatformJpaEntity> findByCode(String code);
    List<AffiliatePlatformJpaEntity> findByStatus(PlatformStatus status);
}
```

#### Step 1.7: Create MapStruct Mapper
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/mapper/AffiliatePlatformMapper.java`

```java
package com.cashbee.infrastructure.mapper;

import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.infrastructure.persistence.entity.AffiliatePlatformJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AffiliatePlatformMapper {
    AffiliatePlatform toDomain(AffiliatePlatformJpaEntity entity);
    AffiliatePlatformJpaEntity toEntity(AffiliatePlatform domain);
}
```

#### Step 1.8: Create Repository Adapter
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/adapter/AffiliatePlatformRepositoryAdapter.java`

```java
package com.cashbee.infrastructure.adapter;

import com.cashbee.domain.enums.PlatformStatus;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.infrastructure.mapper.AffiliatePlatformMapper;
import com.cashbee.infrastructure.persistence.repository.AffiliatePlatformJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AffiliatePlatformRepositoryAdapter implements AffiliatePlatformRepository {
    private final AffiliatePlatformJpaRepository jpaRepository;
    private final AffiliatePlatformMapper mapper;

    @Override
    public AffiliatePlatform save(AffiliatePlatform platform) {
        var entity = mapper.toEntity(platform);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AffiliatePlatform> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AffiliatePlatform> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<AffiliatePlatform> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AffiliatePlatform> findAllActive() {
        return jpaRepository.findByStatus(PlatformStatus.ACTIVE).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
```

#### Step 1.9: Create DTOs & Use Cases (Application Layer)
**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/affiliate/AffiliatePlatformResponse.java`

```java
package com.cashbee.application.dto.affiliate;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffiliatePlatformResponse {
    private Long id;
    private String name;
    private String code;
    private BigDecimal defaultCommissionRate;
    private String status;
}
```

**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/affiliate/GetAffiliatePlatformsUseCase.java`

```java
package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAffiliatePlatformsUseCase {
    private final AffiliatePlatformRepository platformRepository;

    public List<AffiliatePlatformResponse> execute() {
        return platformRepository.findAll().stream()
            .map(platform -> AffiliatePlatformResponse.builder()
                .id(platform.getId())
                .name(platform.getName())
                .code(platform.getCode())
                .defaultCommissionRate(platform.getDefaultCommissionRate())
                .status(platform.getStatus().name())
                .build())
            .collect(Collectors.toList());
    }
}
```

**File**: `cashbee-application/src/main/java/com/cashbee/application/usecase/affiliate/GetPlatformByCodeUseCase.java`

```java
package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPlatformByCodeUseCase {
    private final AffiliatePlatformRepository platformRepository;

    public AffiliatePlatformResponse execute(String code) {
        var platform = platformRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Platform not found: " + code));

        return AffiliatePlatformResponse.builder()
            .id(platform.getId())
            .name(platform.getName())
            .code(platform.getCode())
            .defaultCommissionRate(platform.getDefaultCommissionRate())
            .status(platform.getStatus().name())
            .build();
    }
}
```

#### Step 1.10: Create REST Controller (Presentation Layer)
**File**: `cashbee-presentation/src/main/java/com/cashbee/presentation/controller/AffiliatePlatformController.java`

```java
package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.application.usecase.affiliate.GetAffiliatePlatformsUseCase;
import com.cashbee.application.usecase.affiliate.GetPlatformByCodeUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/platforms")
@RequiredArgsConstructor
public class AffiliatePlatformController {
    private final GetAffiliatePlatformsUseCase getPlatformsUseCase;
    private final GetPlatformByCodeUseCase getPlatformByCodeUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AffiliatePlatformResponse>>> getAllPlatforms() {
        var platforms = getPlatformsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(platforms));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<AffiliatePlatformResponse>> getPlatformByCode(
        @PathVariable String code) {
        var platform = getPlatformByCodeUseCase.execute(code);
        return ResponseEntity.ok(ApiResponse.success(platform));
    }
}
```

#### Step 1.11: Seed Data (Optional)
Create SQL script to insert default platforms:
**File**: `cashbee-presentation/src/main/resources/db/changelog/006-seed-affiliate-platforms.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog>
  <changeSet id="006" author="cashbee">
    <insert tableName="affiliate_platform">
      <column name="name" value="Shopee"/>
      <column name="code" value="shopee"/>
      <column name="default_commission_rate" value="5.00"/>
      <column name="status" value="ACTIVE"/>
    </insert>
    <insert tableName="affiliate_platform">
      <column name="name" value="Lazada"/>
      <column name="code" value="lazada"/>
      <column name="default_commission_rate" value="4.50"/>
      <column name="status" value="INACTIVE"/>
    </insert>
    <insert tableName="affiliate_platform">
      <column name="name" value="TikTok Shop"/>
      <column name="code" value="tiktok"/>
      <column name="default_commission_rate" value="6.00"/>
      <column name="status" value="INACTIVE"/>
    </insert>
  </changeSet>
</databaseChangeLog>
```

---

## 🔥 PHASE 2: Cashback Policy Module (Days 3-4)

**Goal**: Create policy system for cashback calculation

### Follow same pattern as Phase 1:
1. Liquibase migration (007-create-cashback-policy-table.xml)
2. Domain model (CashbackPolicy.java)
3. Enum (UserLevel.java)
4. Repository interface
5. JPA entity + repository
6. Mapper
7. Repository adapter
8. DTOs + Use cases
9. REST controller
10. Seed data (default policy)

**Key Use Case**: `GetActiveCashbackPolicyUseCase` - returns policy for calculating cashback

---

## 🔥 PHASE 3: Affiliate Order Module (Days 5-7)

**Goal**: Store orders from imported files

### Database (Day 5)
- Create `affiliate_order` table (migration 008)
- Create `affiliate_order_item` table (migration 009)

### Domain Layer (Day 5)
- `AffiliateOrder.java` domain model
- `AffiliateOrderItem.java` domain model
- Repository interfaces

### Infrastructure Layer (Day 6)
- JPA entities
- JPA repositories
- Mappers
- Repository adapters

### Application Layer (Day 6)
- DTOs: `AffiliateOrderResponse`, `AffiliateOrderItemResponse`
- Use cases:
  - `CreateAffiliateOrderUseCase` - Called during import
  - `GetUserOrdersUseCase` - User views their orders
  - `UpdateOrderStatusUseCase` - Update status (triggers cashback)

### Presentation Layer (Day 7)
- `AffiliateOrderController`
- Endpoints:
  - `GET /api/orders/my` - User views orders
  - `GET /api/orders/{orderId}` - Order details
  - `PUT /api/admin/orders/{orderId}/status` - Update status

---

## 🔥 PHASE 4: Cashback Module (Days 8-10)

**Goal**: Calculate and manage cashback

### Database (Day 8)
- Create `cashback` table (migration 010)

### Domain Layer (Day 8)
- `Cashback.java` domain model
- Repository interface

### Infrastructure Layer (Day 9)
- JPA entity + repository
- Mapper
- Repository adapter

### Application Layer (Days 9-10)
- DTOs: `CashbackResponse`, `CalculateCashbackCommand`
- Use cases:
  - **`CalculateCashbackUseCase`** - Calculate cashback from order
  - **`ConfirmCashbackUseCase`** - Order APPROVED → Cashback CONFIRMED → Add pending balance
  - **`PayCashbackUseCase`** - Order PAID → Cashback PAID → Move to available balance
  - `GetUserCashbackUseCase` - User views cashback

### Presentation Layer (Day 10)
- `CashbackController`
- Endpoints:
  - `GET /api/cashback/my` - User views cashback
  - `GET /api/cashback/{id}` - Cashback details

---

## 🔥 PHASE 5: Affiliate Import Module (Days 11-14) ⭐ **MOST COMPLEX**

**Goal**: Admin uploads Shopee Excel/JSON file → System creates orders & calculates cashback

### Database (Day 11)
- Create `affiliate_import_batch` table (migration 011)

### Domain Layer (Day 11)
- `AffiliateImportBatch.java` domain model
- Repository interface

### Infrastructure Layer (Day 11)
- JPA entity + repository
- Mapper
- Repository adapter

### File Parser Service (Days 12-13) ⭐ **CRITICAL**

**File**: `cashbee-application/src/main/java/com/cashbee/application/service/ShopeeFileParser.java`

```java
package com.cashbee.application.service;

import com.cashbee.application.dto.affiliate.ShopeeOrderData;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ShopeeFileParser {
    List<ShopeeOrderData> parseFile(MultipartFile file, String fileType);
}
```

**Implementations**:
1. `ShopeeExcelParser` - Parse Excel files (Apache POI)
2. `ShopeeJsonParser` - Parse JSON files (Jackson)

**DTO**: `ShopeeOrderData.java` - Raw order data from Shopee file

### Import Use Case (Day 13-14) ⭐ **CRITICAL**

**File**: `ImportShopeeFileUseCase.java`

**Logic**:
```java
1. Validate file (type, size)
2. Create import batch record (status = PROCESSING)
3. Parse file → List<ShopeeOrderData>
4. For each order data:
   a. Match user by click_id or email
   b. Check if order exists (by order_id)
   c. If exists → update commission
   d. If new → create AffiliateOrder + AffiliateOrderItem
   e. Calculate cashback using CashbackPolicy
   f. Create Cashback record (status = PENDING)
5. Update batch (status = COMPLETED, total_orders, success_orders)
6. Return batch summary
```

### Presentation Layer (Day 14)
- `AffiliateImportController`
- Endpoints:
  - **`POST /api/admin/imports/shopee`** - Upload file (multipart/form-data)
  - `GET /api/admin/imports` - List batches
  - `GET /api/admin/imports/{batchId}` - Batch details

---

## 🔥 PHASE 6: Integration & Testing (Days 15-16)

### Day 15: Integration Testing
Test the full cashback flow:

1. Create test Shopee Excel file
2. Admin uploads file via POST /api/admin/imports/shopee
3. Verify:
   - Import batch created (COMPLETED)
   - Orders created
   - Cashback calculated (PENDING)
4. Update order status to APPROVED
5. Verify:
   - Cashback CONFIRMED
   - User wallet pending_balance increased
6. Update order status to PAID
7. Verify:
   - Cashback PAID
   - User wallet balance increased (pending → available)
8. User requests payout
9. Admin approves payout
10. Verify wallet balance decreased

### Day 16: Bug Fixes & Optimization
- Fix any bugs found during testing
- Optimize database queries
- Add error handling
- Add logging

---

## 📝 Implementation Checklist

### Phase 1: Affiliate Platform ✅
- [ ] Liquibase migration
- [ ] Domain model + enum + repository interface
- [ ] JPA entity + repository + mapper + adapter
- [ ] DTOs + use cases
- [ ] REST controller
- [ ] Seed data
- [ ] Test API

### Phase 2: Cashback Policy ✅
- [ ] Liquibase migration
- [ ] Domain model + enum + repository interface
- [ ] JPA entity + repository + mapper + adapter
- [ ] DTOs + use cases (GetActiveCashbackPolicyUseCase)
- [ ] REST controller
- [ ] Seed data (default policy)
- [ ] Test API

### Phase 3: Affiliate Order ✅
- [ ] Liquibase migrations (order + item tables)
- [ ] Domain models + repository interfaces
- [ ] JPA entities + repositories + mappers + adapters
- [ ] DTOs + use cases (Create, GetUserOrders, UpdateStatus)
- [ ] REST controller
- [ ] Test API

### Phase 4: Cashback ✅
- [ ] Liquibase migration
- [ ] Domain model + repository interface
- [ ] JPA entity + repository + mapper + adapter
- [ ] DTOs + use cases (Calculate, Confirm, Pay, Get)
- [ ] REST controller
- [ ] Test API

### Phase 5: Affiliate Import ⭐ **MOST IMPORTANT**
- [ ] Liquibase migration
- [ ] Domain model + repository interface
- [ ] JPA entity + repository + mapper + adapter
- [ ] File parser service (Excel + JSON)
- [ ] ShopeeOrderData DTO
- [ ] ImportShopeeFileUseCase (full import logic)
- [ ] REST controller (file upload endpoint)
- [ ] Test with real Shopee file

### Phase 6: Integration Testing ✅
- [ ] Create test Excel file
- [ ] Test full cashback flow end-to-end
- [ ] Fix bugs
- [ ] Optimize queries
- [ ] Add comprehensive logging
- [ ] Update documentation

---

## 🎯 Success Criteria

The implementation is successful when:

✅ **Admin can upload Shopee file**
- POST /api/admin/imports/shopee works
- File is parsed correctly
- Orders are created

✅ **Orders are created with cashback**
- affiliate_order table populated
- affiliate_order_item table populated
- cashback table populated (PENDING)

✅ **User can view orders**
- GET /api/orders/my returns user's orders
- Cashback status visible

✅ **Cashback flow works**
- Order APPROVED → Cashback CONFIRMED → pending_balance increased
- Order PAID → Cashback PAID → balance increased

✅ **Payout flow works** (already implemented)
- User can request payout
- Admin can approve
- Balance decreased

---

## 💡 Implementation Tips

### Follow Hexagonal Architecture
Every new module must follow this structure:
1. **Database**: Liquibase migration first
2. **Domain**: Pure domain model (NO JPA)
3. **Infrastructure**: JPA entity + repository + adapter
4. **Application**: DTOs + use cases
5. **Presentation**: REST controller

### Foreign Keys as Primitives
```java
// ✅ Correct
private Long userId;
private Long platformId;

// ❌ Wrong
private User user;
private AffiliatePlatform platform;
```

### Use MapStruct for Mapping
```java
@Mapper(componentModel = "spring")
public interface AffiliatePlatformMapper {
    AffiliatePlatform toDomain(AffiliatePlatformJpaEntity entity);
    AffiliatePlatformJpaEntity toEntity(AffiliatePlatform domain);
}
```

### Test After Each Phase
Don't wait until the end. Test each module immediately after implementation.

---

## 📅 Timeline Summary

| Phase | Days | Description |
|-------|------|-------------|
| **Phase 1** | 1-2 | Affiliate Platform Module |
| **Phase 2** | 3-4 | Cashback Policy Module |
| **Phase 3** | 5-7 | Affiliate Order Module |
| **Phase 4** | 8-10 | Cashback Module |
| **Phase 5** | 11-14 | Affiliate Import Module ⭐ |
| **Phase 6** | 15-16 | Integration & Testing |

**Total**: **16 working days** (~3-4 weeks)

---

## 🚀 Let's Start!

**Next Step**: Begin Phase 1 - Affiliate Platform Module

1. Create Liquibase migration 005-create-affiliate-platform-table.xml
2. Create domain model AffiliatePlatform.java
3. Continue following the plan...

---

**Last Updated**: 2025-11-01
**Status**: Ready to implement
