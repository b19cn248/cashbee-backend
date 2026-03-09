# CashBee Backend

Java 21 / Spring Boot 3.4.1 backend for the CashBee cashback/affiliate marketing platform.

## Development Commands

```bash
# Build & compile
./mvnw clean compile

# Run all tests
./mvnw test

# Build skipping tests
./mvnw clean install -DskipTests

# Run the application
./mvnw spring-boot:run -pl cashbee-presentation

# Run specific test
./mvnw test -pl cashbee-application -Dtest=WalletUseCaseTest

# Code coverage
./mvnw clean test jacoco:report
```

## Architecture: Hexagonal (Ports & Adapters)

Multi-module Maven project with strict layer separation:

```
cashbee-common/         → Exceptions, constants, utilities
cashbee-domain/         → Pure domain models, repository interfaces (Ports), enums
cashbee-infrastructure/ → JPA entities, repository adapters, mappers, Keycloak
cashbee-application/    → Use cases, DTOs, port interfaces
cashbee-presentation/   → REST controllers, exception handlers, security config
```

### Module Dependencies
```
presentation → application, common
application → domain, common
infrastructure → domain, common (implements domain interfaces)
domain → common
common → (none)
```

### Critical Architecture Rules

1. **Domain layer is PURE** — zero framework dependencies, no JPA annotations, no Spring imports
2. **Domain models use primitive foreign keys** — `Long userId`, NOT `User user`
3. **Infrastructure JPA entities are separate** from domain models — mappers convert between them
4. **Use cases** are `@Service @Transactional` classes with constructor injection
5. **Controllers** use `@RestController @RequestMapping`
6. **Authorization** is handled by Keycloak Policy Enforcer (no `@PreAuthorize`)

## Package Structure

```
com.cashbee.common.
  exception/        → BusinessException, NotFoundException, BadRequestException, etc.
  constant/         → AppConstants, ErrorCode
  util/             → MoneyUtils, DateTimeUtils, StringUtils
  validation/       → Custom validators

com.cashbee.domain.
  model/            → User, UserWallet, Transaction, PayoutRequest, AffiliateOrder, etc.
  repository/       → Repository interfaces (Ports)
  enums/            → UserStatus, OrderStatus, PayoutStatus, etc.

com.cashbee.infrastructure.
  entity/           → JPA entities (@Entity, @Table)
  persistence/
    entity/         → Legacy JPA entities
    repository/     → Spring Data JpaRepository interfaces
    adapter/        → Repository implementations (Adapters)
    mapper/         → Domain ↔ JPA entity mappers
  keycloak/         → Keycloak admin API integration
  config/           → Infrastructure configuration

com.cashbee.application.
  usecase/{feature}/ → Use case classes (@Service, @Transactional)
  dto/              → Request/Response DTOs (prefer records)
  port/             → Domain ↔ DTO mapper interfaces (MapStruct)
  util/             → Application utilities

com.cashbee.presentation.
  controller/       → REST controllers
  handler/          → Global exception handlers
  config/           → SecurityConfig, OpenAPI config
  dto/              → ApiResponse wrapper
```

## API Response Format

All endpoints return `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "errorCode": null,
  "timestamp": "2025-01-01T00:00:00"
}
```

## Database

- **MySQL 8.3.0** via Spring Data JPA
- **Liquibase 4.31.0** for schema migrations
- Migration files: `cashbee-presentation/src/main/resources/db/changelog/`
- Master file: `db.changelog-master.xml`
- Naming convention: `{NNN}-{description}.xml`
- `spring.jpa.ddl-auto=validate` (Liquibase manages schema, JPA validates)

## Authentication & Security

- **Keycloak 26.0.7** — OAuth2/OIDC identity provider
- JWT tokens validated via JWK Set URI
- **Keycloak Policy Enforcer** for fine-grained authorization
- Resources and scopes defined in Keycloak Admin Console
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- CORS: localhost:3000, localhost:3007, cashbee.nguocchieuvangle.io.vn

## Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.4.1 | Framework |
| Spring Data JPA | (managed) | Data access |
| Keycloak | 26.0.7 | Auth |
| Liquibase | 4.31.0 | DB migrations |
| MapStruct | 1.6.3 | Object mapping |
| SpringDoc OpenAPI | 2.7.0 | Swagger/API docs |
| Apache POI | 5.3.0 | Excel processing |
| Testcontainers | 1.20.4 | Integration tests |
| JaCoCo | (managed) | Code coverage (80% min) |

## Conventions

- Use `record` for DTOs
- Constructor injection only (no @Autowired on fields)
- `@Transactional` on use case methods, not repository methods
- Money operations: `MoneyUtils` (BigDecimal with proper scale)
- Dates: `java.time.LocalDateTime`
- Exception hierarchy: `BusinessException` base → `NotFoundException`, `BadRequestException`, `ValidationException`, `InsufficientBalanceException`, `DuplicateEntityException`, `ForbiddenException`, `UnauthorizedException`
- Domain entity scanning: both `infrastructure.entity` and `infrastructure.persistence.entity`
