# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

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

## Architecture Overview

This is a **Hexagonal Architecture (Ports & Adapters)** multi-module Maven project for a cashback platform.

### Module Structure & Dependencies

```
cashbee-presentation  (REST Controllers, Main App, Liquibase migrations)
         |
         v
cashbee-application   (Use Cases, DTOs, Application Services)
         |
         v
cashbee-domain        (Pure domain models, Repository interfaces - NO infrastructure deps)
         ^
         |
cashbee-infrastructure (JPA Entities, Repository Adapters, Mappers)
         |
    cashbee-common    (Exceptions, Constants, Utilities - shared by all)
```

### Key Architecture Rules

**Domain Layer (cashbee-domain):**
- Pure POJOs with NO JPA annotations, NO Spring dependencies
- Repository interfaces define ports (contracts)
- Foreign keys as primitive types (`Long userId`, NOT `User user`)
- Business logic lives here (e.g., `wallet.addPendingBalance()`)

**Infrastructure Layer (cashbee-infrastructure):**
- JPA entities with `@Entity`, `@Table` annotations
- Repository adapters implement domain interfaces
- MapStruct mappers convert Domain <-> JPA Entity
- Pattern: `*JpaEntity.java`, `*JpaRepository.java`, `*RepositoryAdapter.java`, `*Mapper.java`

**Application Layer (cashbee-application):**
- Use cases orchestrate business workflows
- Each use case = one class with `execute()` method
- Annotated with `@Service` and `@Transactional`
- Returns DTOs, never domain models directly

**Presentation Layer (cashbee-presentation):**
- REST Controllers call use cases
- All endpoints return `ApiResponse<T>` wrapper
- Database migrations in `src/main/resources/db/changelog/`

## Technology Stack

- Java 21, Spring Boot 3.4.1
- MySQL 8.3.0 with Liquibase migrations
- Keycloak for OAuth2/OIDC authentication
- MapStruct 1.6.3 + Lombok 1.18.36 for mapping/boilerplate
- JUnit 5 + Mockito + Testcontainers for testing

## Common Patterns

### Use Case Pattern
```java
@Service
@RequiredArgsConstructor
@Transactional
public class SomeActionUseCase {
    private final SomeRepository repository;
    private final SomeMapper mapper;

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

## Database

- Schema managed by Liquibase (never use `ddl-auto: create/update`)
- Changelog files: `cashbee-presentation/src/main/resources/db/changelog/`
- Main changelog: `db.changelog-master.xml`
- Naming: `NNN-description.xml` (e.g., `024-refactor-cashback-per-item.xml`)

## Keycloak Integration

- Backend validates JWT tokens from Keycloak
- Users sync from Keycloak to local database via `/api/users/sync`
- Backend does NOT store passwords - Keycloak handles all auth
- Config in `application.yml` under `keycloak:` and `spring.security.oauth2.resourceserver`
