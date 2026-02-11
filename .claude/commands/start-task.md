# Start Task from Plane

Bat dau lam task theo quy trinh chuan: **READ TASK → ANALYZE → PLAN → (wait for approval) → CODE**

## Task ID: $ARGUMENTS

---

## PHASE 1: READ TASK FROM PLANE

**Su dung subagent `plane-assistant`** de doc task details:

```
Task: plane-assistant
Prompt: "Doc issue $ARGUMENTS tu project Cashbee. Tra ve:
- Title, Description day du
- Priority, Labels, Assignees
- Tat ca comments history
- Acceptance criteria (neu co)"
```

**Output can thu thap**:
- Task title va full description
- Priority level
- Labels (de xac dinh task type)
- Comments tu team
- Issue UUID (de sau nay update)

---

## PHASE 2: ANALYZE & EXPLORE

### 2.1 Xac dinh Task Type
Dua vao thong tin tu Phase 1, phan loai task:
- **NEW_FEATURE**: Chuc nang hoan toan moi → Ap dung TDD
- **ENHANCEMENT**: Cai tien chuc nang co san → Viet test cho existing code truoc
- **BUG_FIX**: Sua loi → Viet failing test truoc, sau do fix
- **REFACTOR**: Tai cau truc code → Dam bao test coverage truoc khi refactor
- **MAINTENANCE**: Bao tri, update dependencies, config

### 2.2 Database Analysis

**Su dung subagent `db-explorer`** de phan tich database:

```
Task: db-explorer
Prompt: "Phan tich database schema lien quan den [mo ta task].
Su dung MCP tool `mcp__cashbee_server_mysql__mysql_query` de:
1. Tim tables lien quan: SHOW TABLES LIKE '%keyword%'
2. Xem schema: DESCRIBE table_name; SHOW CREATE TABLE table_name
3. Check indexes: SHOW INDEX FROM table_name
4. Check foreign keys va relationships
5. Sample data (LIMIT 5) de hieu business logic

Tra ve:
- Danh sach tables lien quan
- Schema chi tiet (columns, types, constraints)
- Relationships giua cac tables
- Existing indexes
- Goi y neu can them index hoac thay doi schema"
```

### 2.3 Codebase Exploration

**Su dung subagent `Explore`** de phan tich source code:

```
Task: Explore (thoroughness: medium)
Prompt: "Explore codebase lien quan den [mo ta task].
Tim va phan tich theo Hexagonal Architecture:

1. Domain Layer (cashbee-domain):
   - Domain models trong domain/model/
   - Repository interfaces (ports) trong domain/repository/

2. Application Layer (cashbee-application):
   - UseCases trong usecase/
   - DTOs trong dto/
   - Application services trong service/

3. Infrastructure Layer (cashbee-infrastructure):
   - JPA Entities trong persistence/entity/
   - JPA Repositories trong persistence/repository/
   - Repository Adapters trong persistence/adapter/
   - Mappers trong persistence/mapper/

4. Presentation Layer (cashbee-presentation):
   - Controllers trong controller/
   - Database migrations trong db/changelog/

5. Existing tests patterns

Tra ve:
- Files lien quan can modify
- Patterns dang duoc su dung
- Similar implementations de follow
- Dependencies can thiet
- Test patterns hien co"
```

---

## PHASE 3: CREATE IMPLEMENTATION PLAN

Sau khi thu thap thong tin tu cac subagents, tong hop thanh plan:

### 3.1 Task Summary
```markdown
## Task: [CASHB-XXX] [Title]
**Type**: [NEW_FEATURE | ENHANCEMENT | BUG_FIX | REFACTOR | MAINTENANCE]
**Priority**: [Urgent | High | Medium | Low]
**Approach**: [TDD | Test-First-Refactor | Bug-Reproduce-Fix]
```

### 3.2 Database Design (neu co thay doi)
```markdown
## Database Changes
- [ ] New tables: [list]
- [ ] Alter tables: [list changes]
- [ ] New indexes: [list voi reasoning]
- [ ] Liquibase migration: cashbee-presentation/src/main/resources/db/changelog/NNN-description.xml
```

### 3.3 Implementation Checklist (Hexagonal Architecture)

#### For NEW_FEATURE (TDD Approach):
```markdown
## Phase 1: Test First (RED)
- [ ] Write unit tests for UseCase (happy path)
- [ ] Write unit tests for edge cases
- [ ] Write integration tests for Repository
- [ ] Run tests - ALL SHOULD FAIL

## Phase 2: Domain Layer (GREEN - Inside Out)
- [ ] Create Domain Model (cashbee-domain/model/)
  - Pure POJO, NO JPA annotations
  - Business logic methods
  - Foreign keys as primitives (Long userId)
- [ ] Create Repository Interface/Port (cashbee-domain/repository/)

## Phase 3: Infrastructure Layer
- [ ] Create JPA Entity (cashbee-infrastructure/persistence/entity/)
  - @Entity, @Table annotations
  - Proper column mappings
- [ ] Create JPA Repository (cashbee-infrastructure/persistence/repository/)
- [ ] Create Repository Adapter (cashbee-infrastructure/persistence/adapter/)
  - Implements domain repository interface
- [ ] Create MapStruct Mapper (cashbee-infrastructure/persistence/mapper/)
  - Domain <-> JPA Entity mapping

## Phase 4: Application Layer
- [ ] Create Request/Response DTOs (cashbee-application/dto/)
  - Validation annotations
- [ ] Create UseCase (cashbee-application/usecase/)
  - @Service, @Transactional
  - Single execute() method
  - Returns DTO

## Phase 5: Presentation Layer
- [ ] Create Controller (cashbee-presentation/controller/)
  - Calls UseCase
  - Returns ApiResponse<T>
- [ ] Create Liquibase migration if needed

## Phase 6: Refactor
- [ ] Review code for clean code principles
- [ ] Optimize queries neu can
- [ ] Add proper logging
- [ ] Run all tests - ALL SHOULD PASS
```

#### For ENHANCEMENT/REFACTOR:
```markdown
## Phase 1: Secure Existing Code
- [ ] Write/verify unit tests for existing functionality
- [ ] Ensure test coverage >= 80%
- [ ] Run tests - ALL SHOULD PASS

## Phase 2: Make Changes
- [ ] Implement changes incrementally
- [ ] Follow Hexagonal Architecture
- [ ] Run tests after each change

## Phase 3: Verify
- [ ] All existing tests still pass
- [ ] New tests pass
```

#### For BUG_FIX:
```markdown
## Phase 1: Reproduce Bug
- [ ] Write failing test that reproduces the bug

## Phase 2: Fix
- [ ] Identify root cause
- [ ] Implement minimal fix
- [ ] Run test - SHOULD PASS

## Phase 3: Prevent Regression
- [ ] Add additional edge case tests
```

### 3.4 Clean Code Checklist
- [ ] Single Responsibility Principle
- [ ] Meaningful naming
- [ ] Methods max 20-30 lines
- [ ] No magic numbers
- [ ] DRY - no duplicate code
- [ ] Proper exception handling
- [ ] Meaningful logging

### 3.5 Performance Checklist
- [ ] N+1 Query Prevention (@EntityGraph / JOIN FETCH)
- [ ] Pagination for list endpoints
- [ ] Proper indexing
- [ ] LAZY fetch for relationships
- [ ] @Transactional(readOnly = true) for reads

### 3.6 Security Checklist
- [ ] Input validation (@Valid)
- [ ] Parameterized queries
- [ ] Keycloak authorization
- [ ] No sensitive data in logs

### 3.7 Files to Create/Modify
```markdown
## Files Changed (by Layer)

### Domain Layer (cashbee-domain)
| File | Action | Description |
|------|--------|-------------|
| domain/model/Xxx.java | CREATE/MODIFY | Domain model |
| domain/repository/XxxRepository.java | CREATE/MODIFY | Repository interface |

### Application Layer (cashbee-application)
| File | Action | Description |
|------|--------|-------------|
| usecase/xxx/XxxUseCase.java | CREATE/MODIFY | UseCase |
| dto/XxxRequest.java | CREATE/MODIFY | Request DTO |
| dto/XxxResponse.java | CREATE/MODIFY | Response DTO |

### Infrastructure Layer (cashbee-infrastructure)
| File | Action | Description |
|------|--------|-------------|
| persistence/entity/XxxJpaEntity.java | CREATE/MODIFY | JPA Entity |
| persistence/repository/XxxJpaRepository.java | CREATE/MODIFY | JPA Repository |
| persistence/adapter/XxxRepositoryAdapter.java | CREATE/MODIFY | Repository Adapter |
| persistence/mapper/XxxMapper.java | CREATE/MODIFY | MapStruct Mapper |

### Presentation Layer (cashbee-presentation)
| File | Action | Description |
|------|--------|-------------|
| controller/XxxController.java | CREATE/MODIFY | REST Controller |
| db/changelog/NNN-xxx.xml | CREATE | DB Migration |

## Test Files
| File | Description |
|------|-------------|
| XxxUseCaseTest.java | UseCase unit tests |
```

---

## PHASE 4: OUTPUT & WAIT FOR APPROVAL

1. **Viet plan ra file**: `docs/plans/PLAN_[TASK_ID].md`

2. **Comment len Plane issue** (su dung subagent `plane-assistant`):
```
Task: plane-assistant
Prompt: "Them comment vao issue [UUID] voi noi dung summary cua plan"
```

3. **Output format cho user**:
```markdown
# Implementation Plan: [CASHB-XXX] [Title]

## 1. Task Summary
## 2. Database Changes
## 3. Implementation Approach (Hexagonal Architecture)
## 4. Detailed Checklist by Layer
## 5. Files to Create/Modify
## 6. Estimated Complexity
## 7. Risks & Considerations

---
**Waiting for approval**
- "approved" / "go" to proceed
- "changes needed" with feedback
- Questions for clarification
```

---

## SUBAGENTS SUMMARY

| Phase | Subagent | Purpose |
|-------|----------|---------|
| 1 | `plane-assistant` | Doc task tu Plane (project Cashbee) |
| 2.2 | `db-explorer` | Phan tich database schema (mcp__cashbee_server_mysql__mysql_query) |
| 2.3 | `Explore` | Explore codebase |
| 4 | `plane-assistant` | Comment plan len Plane |

---

## IMPORTANT RULES

1. **KHONG viet code cho den khi duoc approve**
2. **Su dung subagents de toi uu context**
3. **Follow Hexagonal Architecture patterns trong codebase**
4. **Viet plan file ra `docs/plans/PLAN_[TASK_ID].md`**
5. **Neu thieu thong tin, hoi clarification questions**
6. **Domain layer MUST be pure - no JPA/Spring dependencies**
7. **UseCase = @Service + @Transactional + execute() method**
8. **Repository adapters implement domain repository interfaces**
