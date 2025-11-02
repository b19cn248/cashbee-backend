# Hexagonal Architecture Implementation Guide

## 🏛️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                    │
│              (REST Controllers, DTOs)                   │
└──────────────────────┬──────────────────────────────────┘
                       │ Uses
┌──────────────────────▼──────────────────────────────────┐
│                  APPLICATION LAYER                      │
│                    (Use Cases)                          │
│  - RegisterUserUseCase                                  │
│  - TransferCashbackUseCase                              │
└──────────────────────┬──────────────────────────────────┘
                       │ Uses & Depends on
┌──────────────────────▼──────────────────────────────────┐
│                    DOMAIN LAYER                         │
│                  (Pure Business Logic)                  │
│  ┌────────────────────────────────────────────────┐    │
│  │  Models (POJOs)                                │    │
│  │  - User.java                                   │    │
│  │  - UserWallet.java                             │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │  Repository Interfaces (Ports)                 │    │
│  │  - UserRepository (interface)                  │    │
│  │  - UserWalletRepository (interface)            │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │  Domain Services                               │    │
│  │  - WalletTransferService                       │    │
│  └────────────────────────────────────────────────┘    │
└──────────────────────▲──────────────────────────────────┘
                       │ Implements (Dependency Inversion)
┌──────────────────────┴──────────────────────────────────┐
│                INFRASTRUCTURE LAYER                     │
│                     (Adapters)                          │
│  ┌────────────────────────────────────────────────┐    │
│  │  JPA Entities (Persistence Models)             │    │
│  │  - UserJpaEntity.java                          │    │
│  │  - UserWalletJpaEntity.java                    │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │  Repository Implementations (Adapters)         │    │
│  │  - UserRepositoryImpl implements UserRepository│    │
│  │  - UserWalletRepositoryImpl                    │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │  Mappers (Domain ↔ JPA)                       │    │
│  │  - UserMapper                                  │    │
│  │  - UserWalletMapper                            │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │  JPA Repositories (Spring Data)                │    │
│  │  - UserJpaRepository extends JpaRepository     │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

---

## 📦 Module Structure

### 1. **cashbee-domain** (Core - No external dependencies)
```
domain/
├── model/                    # Pure domain models (POJOs)
│   ├── User.java            # ✅ No JPA annotations
│   ├── UserWallet.java      # ✅ Pure business logic
│   └── AffiliateOrder.java
├── repository/               # Repository interfaces (Ports)
│   ├── UserRepository.java  # ✅ Interface only
│   └── UserWalletRepository.java
├── service/                  # Domain services
│   └── WalletTransferService.java
├── enums/                    # Enums
│   ├── UserStatus.java
│   └── OrderStatus.java
└── valueobject/              # Value objects (immutable)
    └── Money.java
```

**Key Principles:**
- ✅ **Zero infrastructure dependencies** (no JPA, no Spring Data)
- ✅ **Pure Java objects** with business logic
- ✅ **Interfaces define contracts** (ports)
- ✅ **Rich domain models** with validation and behavior
- ✅ **Foreign keys as primitive types** (Long userId, not User object)

### 2. **cashbee-infrastructure** (Adapters - Implements domain interfaces)
```
infrastructure/
├── persistence/
│   ├── entity/              # JPA entities (persistence models)
│   │   ├── UserJpaEntity.java       # ✅ With @Entity, @Table
│   │   └── UserWalletJpaEntity.java # ✅ With JPA annotations
│   ├── repository/          # JPA repositories
│   │   ├── UserJpaRepository.java   # extends JpaRepository
│   │   └── UserWalletJpaRepository.java
│   ├── adapter/             # Repository implementations
│   │   ├── UserRepositoryImpl.java  # implements UserRepository
│   │   └── UserWalletRepositoryImpl.java
│   └── mapper/              # Domain ↔ JPA mappers
│       ├── UserMapper.java          # Maps User ↔ UserJpaEntity
│       └── UserWalletMapper.java
├── file/                    # File processing adapters
│   └── ExcelFileParser.java
└── config/                  # Infrastructure configs
    └── JpaConfig.java
```

---

## 🔄 Mapping Between Layers

### Example: User Domain Model → JPA Entity

#### **Domain Model** (cashbee-domain/model/User.java)
```java
package com.cashbee.domain.model;

// ✅ Pure POJO - No JPA annotations
public class User {
    private Long id;
    private String keycloakId;
    private String username;
    private String email;
    private UserStatus status;

    // ✅ Business logic methods
    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isDeleted();
    }
}
```

#### **JPA Entity** (cashbee-infrastructure/persistence/entity/UserJpaEntity.java)
```java
package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;

// ✅ JPA annotations ONLY in infrastructure layer
@Entity
@Table(name = "user")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ✅ NO business logic here - just getters/setters
}
```

#### **Mapper** (cashbee-infrastructure/persistence/mapper/UserMapper.java)
```java
package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.User;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.infrastructure.persistence.entity.UserJpaEntity;

// ✅ Can use MapStruct or manual mapping
@Mapper
public class UserMapper {

    // Domain → JPA Entity
    public UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setKeycloakId(user.getKeycloakId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setStatus(user.getStatus().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        return entity;
    }

    // JPA Entity → Domain
    public User toDomain(UserJpaEntity entity) {
        return User.builder()
            .id(entity.getId())
            .keycloakId(entity.getKeycloakId())
            .username(entity.getUsername())
            .email(entity.getEmail())
            .status(UserStatus.valueOf(entity.getStatus()))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .deletedAt(entity.getDeletedAt())
            .build();
    }
}
```

#### **Repository Implementation** (cashbee-infrastructure/persistence/adapter/UserRepositoryImpl.java)
```java
package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.infrastructure.persistence.entity.UserJpaEntity;
import com.cashbee.infrastructure.persistence.repository.UserJpaRepository;
import com.cashbee.infrastructure.persistence.mapper.UserMapper;

// ✅ Adapter implements domain interface
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        // Map domain → JPA entity
        UserJpaEntity entity = mapper.toEntity(user);

        // Save using JPA
        UserJpaEntity saved = jpaRepository.save(entity);

        // Map back to domain
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return jpaRepository.findByKeycloakId(keycloakId)
            .map(mapper::toDomain);
    }
}
```

---

## 🚫 What NOT to Do

### ❌ **WRONG: Relationships in Domain Models**
```java
// ❌ DON'T DO THIS in domain layer
public class User {
    private UserWallet wallet;  // ❌ Object reference

    @OneToOne
    private UserWallet wallet;  // ❌ JPA annotation in domain
}
```

### ✅ **CORRECT: Foreign Key Only**
```java
// ✅ DO THIS in domain layer
public class UserWallet {
    private Long userId;  // ✅ Just the ID (foreign key)

    // If you need User object, fetch it via repository
    // in application layer or domain service
}
```

---

## 💡 Benefits of This Approach

1. **✅ Technology Independence**
   - Can switch from JPA → MyBatis without changing domain
   - Can switch from MySQL → PostgreSQL → MongoDB
   - Domain layer has zero infrastructure knowledge

2. **✅ Testability**
   - Domain models are pure POJOs - easy to unit test
   - No need for Spring context or database
   - Mock repositories easily

3. **✅ Business Logic in Domain**
   - All business rules in domain models
   - `wallet.lockBalance()` - clear business operation
   - Easy to understand and maintain

4. **✅ No Lazy Loading Issues**
   - No `LazyInitializationException`
   - Explicit data fetching via repositories
   - Performance control in application layer

5. **✅ Clean Separation of Concerns**
   - Domain: WHAT (business rules)
   - Infrastructure: HOW (persistence)
   - Application: WHEN (orchestration)

---

## 📋 Migration Checklist

When switching from JPA to MyBatis (example):

- [ ] Domain layer: **NO CHANGES NEEDED** ✅
- [ ] Application layer: **NO CHANGES NEEDED** ✅
- [ ] Infrastructure layer: Replace JPA implementations
  - [ ] Create MyBatis XML mappers
  - [ ] Implement repositories using MyBatis
  - [ ] Update mappers if needed

**Impact: ZERO changes to business logic!**

---

## 🎯 Summary

| Layer | Responsibility | Technology |
|-------|---------------|------------|
| **Domain** | Business logic, rules, validations | Pure Java |
| **Application** | Use cases, orchestration | Spring (optional) |
| **Infrastructure** | Persistence, external services | JPA, MyBatis, JDBC, etc. |
| **Presentation** | HTTP, REST, controllers | Spring MVC |

**Key Principle**: Domain layer depends on NOTHING. All other layers depend on Domain.

---

This is **true** Hexagonal Architecture (Ports & Adapters pattern).
