# Code Review

Review code cho: **$ARGUMENTS**

---

## Huong dan su dung

Command nay ho tro review theo nhieu cach:

| Cach dung | Vi du | Mo ta |
|-----------|-------|-------|
| **Task ID** | `/review-code CASHB-84` | Review code changes cua task tren Plane |
| **Module/Class** | `/review-code Cashback` | Review toan bo module Cashback (UseCase, Repository, Entity, DTO, Mapper) |
| **Specific class** | `/review-code CalculateCashbackUseCase` | Review mot class cu the |
| **Package path** | `/review-code cashback` | Review theo duong dan package |
| **Multiple targets** | `/review-code Cashback,AffiliateOrder` | Review nhieu modules cung luc |

---

## EXECUTION INSTRUCTIONS (Claude phai thuc hien)

Khi nhan duoc `$ARGUMENTS`, Claude PHAI tu dong thuc hien cac buoc sau:

### Buoc 1: Xac dinh loai input

```
IF $ARGUMENTS matches pattern "CASHB-\d+" THEN
    → Day la Task ID → Thuc hien Task Review Flow
ELSE
    → Day la Module/Class name → Thuc hien Module Review Flow
```

### Buoc 2A: Task Review Flow (cho CASHB-XXX)

1. Goi `mcp__my_plane__get_issue_using_readable_identifier` voi project_identifier="CASHB"
2. Doc thong tin task va xac dinh scope
3. Dung `git diff` de xem cac files da thay doi
4. Review theo checklist

### Buoc 2B: Module Review Flow (cho Class/Module name)

**Claude PHAI thuc hien tu dong:**

#### 2B.1: Trich xuat base name tu input

```
Input: "CalculateCashbackUseCase" → Base name: "Cashback"
Input: "CashbackRepositoryAdapter" → Base name: "Cashback"
Input: "Cashback" → Base name: "Cashback"
Input: "cashback" → Base name: "Cashback" (convert to PascalCase)
```

#### 2B.2: Tim TAT CA files lien quan bang Glob tool

Voi base name = `{BaseName}`, tim cac patterns sau theo Hexagonal Architecture:

```bash
# Domain Layer (cashbee-domain)
**/domain/model/{BaseName}.java
**/domain/model/{BaseName}*.java
**/domain/repository/{BaseName}Repository.java

# Application Layer (cashbee-application)
**/usecase/**/*{BaseName}*.java
**/usecase/{baseName}/**/*.java
**/dto/{BaseName}*.java
**/dto/**/{BaseName}*.java
**/service/**/*{BaseName}*.java

# Infrastructure Layer (cashbee-infrastructure)
**/persistence/entity/{BaseName}JpaEntity.java
**/persistence/repository/{BaseName}JpaRepository.java
**/persistence/adapter/{BaseName}RepositoryAdapter.java
**/persistence/mapper/{BaseName}Mapper.java
**/adapter/{BaseName}RepositoryAdapter.java

# Presentation Layer (cashbee-presentation)
**/controller/{BaseName}Controller.java
**/controller/**/{BaseName}*.java

# Database migrations
**/db/changelog/*{baseName}*.xml
**/db/changelog/**/*{baseName}*.xml

# Test layer
**/{BaseName}*Test.java
**/{BaseName}*Tests.java
```

#### 2B.3: Doc va phan tich dependencies

Sau khi tim duoc files, Claude phai:

1. **Doc tung file** bang Read tool
2. **Phan tich imports** de tim them dependencies:
   - Neu UseCase inject `OtherRepository` → ghi nhan dependency
   - Neu Domain model co relationship voi `OtherModel` → ghi nhan relationship
3. **Tao dependency graph** de hieu data flow

#### 2B.4: Review theo thu tu layers (Hexagonal Architecture)

Review tu inside-out:
1. **Domain Model** → Kiem tra business logic, NO JPA annotations
2. **Domain Repository Interface** → Kiem tra port definition
3. **UseCase** → Kiem tra business workflow, transactions
4. **Infrastructure Adapter** → Kiem tra implementation cua ports
5. **JPA Entity** → Kiem tra JPA annotations, mapping
6. **Mapper** → Kiem tra Domain <-> JPA Entity mapping
7. **Controller** → Kiem tra endpoints, validation, security
8. **Tests** → Kiem tra coverage va quality

### Buoc 3: Output ket qua

Su dung Output Format tuong ung (Task hoac Module) o cuoi file nay.

---

## Vi du thuc thi

### Input: `/review-code CalculateCashbackUseCase`

Claude se tu dong:

```
1. Trich xuat base name: "Cashback"

2. Tim files bang Glob:
   - Glob: **/Cashback*.java
   - Glob: **/cashback/**/*.java

3. Ket qua tim duoc (vi du):
   ✓ Cashback.java (domain model)
   ✓ CashbackRepository.java (domain port)
   ✓ CalculateCashbackUseCase.java
   ✓ CashbackJpaEntity.java
   ✓ CashbackJpaRepository.java
   ✓ CashbackRepositoryAdapter.java
   ✓ CashbackMapper.java
   ✓ CashbackController.java
   ✓ CashbackRequest.java
   ✓ CashbackResponse.java
   ✓ CalculateCashbackUseCaseTest.java

4. Doc tung file va review theo checklist

5. Output bao cao review
```

---

## Review Checklist

### 1. Code Quality & Clean Code

#### Naming & Readability
- [ ] Follows project naming conventions (PascalCase, camelCase, UPPER_SNAKE_CASE)?
- [ ] Ten bien/method/class co y nghia, tu mo ta (self-documenting)?
- [ ] Khong dung magic numbers/strings - su dung constants?
- [ ] Code de doc nhu van xuoi (readable as prose)?

#### SOLID Principles
- [ ] **S**ingle Responsibility: Moi class/method chi lam mot viec?
- [ ] **O**pen/Closed: Mo cho extension, dong cho modification?
- [ ] **L**iskov Substitution: Subclass co the thay the parent class?
- [ ] **I**nterface Segregation: Interface nho, tap trung?
- [ ] **D**ependency Inversion: Phu thuoc vao abstraction, khong phai concrete?

#### Code Structure
- [ ] No code duplication (DRY - Don't Repeat Yourself)?
- [ ] Methods khong qua dai (max ~20-30 lines)?
- [ ] Max 3-4 parameters per method (dung object neu nhieu hon)?
- [ ] Cyclomatic complexity thap (tranh nested if/loop qua sau)?
- [ ] Proper use of design patterns khi can thiet?
- [ ] Khong co dead code hoac commented-out code?

### 2. Hexagonal Architecture Compliance

#### Domain Layer (cashbee-domain)
- [ ] Domain models are pure POJOs - NO JPA annotations?
- [ ] Domain models contain business logic?
- [ ] Repository interfaces (ports) defined in domain?
- [ ] Foreign keys as primitive types (Long userId, NOT User user)?
- [ ] No Spring/Infrastructure dependencies?

#### Application Layer (cashbee-application)
- [ ] Each UseCase has single `execute()` method?
- [ ] UseCases are annotated with `@Service` and `@Transactional`?
- [ ] Returns DTOs, never domain models directly?
- [ ] Orchestrates business workflows?

#### Infrastructure Layer (cashbee-infrastructure)
- [ ] JPA entities with proper annotations (@Entity, @Table)?
- [ ] Repository adapters implement domain interfaces?
- [ ] MapStruct mappers for Domain <-> JPA Entity conversion?
- [ ] Follows naming: *JpaEntity, *JpaRepository, *RepositoryAdapter, *Mapper?

#### Presentation Layer (cashbee-presentation)
- [ ] Controllers call UseCases (not repositories directly)?
- [ ] Returns ApiResponse<T> wrapper?
- [ ] Proper request validation?

### 3. Security (OWASP Top 10)

#### Injection Prevention
- [ ] No SQL injection - dung parameterized queries/JPA?
- [ ] No Command injection - validate/sanitize input truoc khi exec?
- [ ] No Log injection - sanitize data truoc khi log?

#### Authentication & Authorization
- [ ] Proper Keycloak security annotations?
- [ ] Kiem tra authorization o ca API va service layer?
- [ ] No hardcoded credentials hoac secrets?

#### Input Validation
- [ ] Input validation voi `@Valid` va custom validators?
- [ ] Whitelist validation thay vi blacklist?
- [ ] File upload validation (type, size, content)?

#### Data Protection
- [ ] No sensitive data in logs (passwords, tokens, PII)?
- [ ] Error messages khong leak sensitive information?

### 4. Performance & Optimization

#### Database Performance
- [ ] No N+1 query issues - dung `@EntityGraph` hoac `JOIN FETCH`?
- [ ] Proper use of indexes tren frequently queried columns?
- [ ] No unnecessary database calls trong loops?
- [ ] Proper pagination voi `Pageable` cho large datasets?
- [ ] `@Transactional(readOnly = true)` cho read operations?
- [ ] Batch operations cho bulk insert/update?

#### Memory & Resources
- [ ] No memory leaks - streams/resources duoc close dung cach?
- [ ] Dung `try-with-resources` cho AutoCloseable?
- [ ] Tranh load toan bo data vao memory - dung streaming?
- [ ] Proper use of caching voi `@Cacheable`?

### 5. Testing

#### Test Coverage
- [ ] Unit tests cho business logic (min 80% coverage)?
- [ ] Integration tests cho API endpoints?
- [ ] Edge cases va boundary conditions covered?
- [ ] Negative test cases (invalid input, errors)?

#### Test Quality
- [ ] Tests theo pattern Given-When-Then?
- [ ] Test naming: `should[Behavior]_when[Condition]`?
- [ ] Mocks duoc su dung dung cach?
- [ ] Tests doc lap, khong phu thuoc thu tu chay?

### 6. Documentation

#### API Documentation
- [ ] Swagger annotations complete va accurate?
- [ ] Request/Response examples trong Swagger?
- [ ] Error responses duoc document?

#### Code Documentation
- [ ] Complex business logic co comments giai thich WHY?
- [ ] Public methods co JavaDoc voi params va return?

### 7. Project-Specific Best Practices (Cashbee)

#### Response Pattern
- [ ] Dung `ApiResponse<T>` wrapper cho all endpoints?
- [ ] Proper error handling voi custom exceptions?

#### Entity & DTO
- [ ] DTOs used - khong expose domain models truc tiep?
- [ ] MapStruct mappers cho entity-DTO conversion?
- [ ] Domain models separate from JPA entities?

#### Database Migrations
- [ ] Liquibase changelog files for schema changes?
- [ ] Changelog naming: NNN-description.xml?
- [ ] No ddl-auto: create/update in production?

---

## Output Format

### Cho Task Review (CASHB-XXX)

```markdown
## Review Summary
**Task**: CASHB-XXX - [Task title]
**Overall Assessment**: [APPROVED / NEEDS CHANGES]
**Files Reviewed**: [number] files

## Issues Found
### Critical
- [file:line] [issue description + suggested fix]

### Major
- [file:line] [issue description + suggested fix]

### Minor
- [file:line] [issue description + suggested fix]

## Suggestions for Improvement
- [optional improvements]

## What's Good
- [positive feedback]
```

### Cho Module Review

```markdown
## Module Review Summary
**Module**: [Module name]
**Overall Assessment**: [APPROVED / NEEDS CHANGES]

## Files Reviewed
| Layer | File | Status |
|-------|------|--------|
| Domain Model | Xxx.java | OK/WARN/FAIL |
| Domain Repository | XxxRepository.java | OK/WARN/FAIL |
| UseCase | XxxUseCase.java | OK/WARN/FAIL |
| JPA Entity | XxxJpaEntity.java | OK/WARN/FAIL |
| JPA Repository | XxxJpaRepository.java | OK/WARN/FAIL |
| Adapter | XxxRepositoryAdapter.java | OK/WARN/FAIL |
| Mapper | XxxMapper.java | OK/WARN/FAIL |
| Controller | XxxController.java | OK/WARN/FAIL |
| Test | XxxUseCaseTest.java | OK/WARN/FAIL |

## Architecture Review
- [ ] Domain layer is pure (no infrastructure deps)?
- [ ] Dependencies flow inward (Presentation -> Application -> Domain)?
- [ ] Infrastructure implements domain ports?
- [ ] No circular dependencies?

## Issues Found

### By Layer

#### Domain Layer
- [issue + fix]

#### Application Layer (UseCases)
- [issue + fix]

#### Infrastructure Layer
- [issue + fix]

#### Presentation Layer
- [issue + fix]

### By Severity

#### Critical
- [file:line] [issue description + suggested fix]

#### Major
- [file:line] [issue description + suggested fix]

#### Minor
- [file:line] [issue description + suggested fix]

## Cross-Cutting Concerns
- **Security**: [findings]
- **Performance**: [findings]
- **Testing**: [findings]

## Suggestions for Improvement
- [optional improvements]

## What's Good
- [positive feedback]

## Action Items
- [ ] [action 1]
- [ ] [action 2]
```
