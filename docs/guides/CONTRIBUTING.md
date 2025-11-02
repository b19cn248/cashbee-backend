# Contributing to CashBee Backend

Thank you for your interest in contributing to CashBee Backend! This document provides guidelines and instructions for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Architecture Guidelines](#architecture-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)

## 🤝 Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Maintain professional communication

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Git
- IDE (IntelliJ IDEA recommended)

### Setup Development Environment

1. **Fork and clone the repository**

```bash
git clone https://github.com/your-username/cashbee-backend.git
cd cashbee-backend
```

2. **Setup database**

```bash
mysql -u root -p < scripts/setup-database.sql
```

3. **Build the project**

```bash
./mvnw clean install
```

4. **Run tests**

```bash
./mvnw test
```

5. **Start the application**

```bash
./mvnw spring-boot:run -pl cashbee-presentation
```

## 🔄 Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b bugfix/issue-description
```

**Branch naming conventions:**
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `refactor/` - Code refactoring
- `docs/` - Documentation updates

### 2. Make Your Changes

- Follow the coding standards (see below)
- Write tests for new functionality
- Update documentation as needed
- Ensure all tests pass

### 3. Commit Your Changes

```bash
git add .
git commit -m "feat: add wallet lock/unlock functionality"
```

### 4. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## 📝 Coding Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Braces**: Egyptian style (opening brace on same line)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Example

```java
public class UserWalletService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final UserWalletRepository repository;

    public UserWallet addPendingBalance(Long userId, BigDecimal amount) {
        // Implementation
    }
}
```

### Lombok Usage

✅ **Recommended:**
```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
}
```

❌ **Avoid:**
```java
@Data  // Too broad, avoid
@ToString  // Use only when needed
```

### Comments and Documentation

- **Javadoc** for all public classes and methods
- **Inline comments** for complex logic only
- **TODO comments** with issue number

```java
/**
 * Add pending balance to user wallet.
 *
 * @param userId User ID
 * @param amount Amount to add
 * @return Updated wallet
 * @throws NotFoundException if wallet not found
 */
public UserWallet addPendingBalance(Long userId, BigDecimal amount) {
    // TODO: Add rate limiting (#123)
    // ...
}
```

## 🏗️ Architecture Guidelines

### Hexagonal Architecture Rules

#### 1. **Domain Layer** (cashbee-domain)

✅ **DO:**
```java
// Pure POJO with business logic
public class UserWallet {
    private Long id;
    private Long userId;  // Foreign key as Long
    private BigDecimal balance;

    public void addPendingBalance(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.pendingBalance = this.pendingBalance.add(amount);
    }
}
```

❌ **DON'T:**
```java
// NO JPA annotations in domain!
@Entity  // ❌ WRONG
@Table(name = "user_wallet")  // ❌ WRONG
public class UserWallet {
    @Id  // ❌ WRONG
    @GeneratedValue  // ❌ WRONG
    private Long id;

    @OneToOne  // ❌ WRONG - Use Long userId instead
    private User user;
}
```

#### 2. **Application Layer** (cashbee-application)

✅ **DO:**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AddPendingBalanceUseCase {
    private final UserWalletRepository repository;
    private final WalletMapper mapper;

    public WalletResponse execute(AddPendingBalanceCommand command) {
        UserWallet wallet = repository.findByUserId(command.getUserId())
            .orElseThrow(() -> new NotFoundException(...));

        wallet.addPendingBalance(command.getAmount());
        wallet = repository.save(wallet);

        return mapper.toResponse(wallet);
    }
}
```

#### 3. **Infrastructure Layer** (cashbee-infrastructure)

✅ **DO:**
```java
// JPA entity (WITH annotations)
@Entity
@Table(name = "user_wallet")
public class UserWalletJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;  // NOT @OneToOne
}

// Adapter
@Component
public class UserWalletRepositoryAdapter implements UserWalletRepository {
    private final UserWalletJpaRepository jpaRepository;
    private final UserWalletMapper mapper;

    @Override
    public UserWallet save(UserWallet wallet) {
        UserWalletJpaEntity entity = mapper.toEntity(wallet);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }
}
```

### Module Dependencies

```
presentation → application → domain ← infrastructure
     ↓              ↓           ↑
   common       common      common
```

**Rules:**
- Domain has NO dependencies (except common)
- Infrastructure depends on domain
- Application depends on domain
- Presentation depends on application + infrastructure

## 🧪 Testing Guidelines

### Test Structure

```
src/test/java/
└── com/cashbee/
    ├── domain/
    │   └── model/
    │       ├── UserTest.java
    │       └── UserWalletTest.java
    ├── application/
    │   └── usecase/
    │       └── AddPendingBalanceUseCaseTest.java
    └── infrastructure/
        └── persistence/
            └── adapter/
                └── UserWalletRepositoryAdapterTest.java
```

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class AddPendingBalanceUseCaseTest {

    @Mock
    private UserWalletRepository repository;

    @Mock
    private WalletMapper mapper;

    @InjectMocks
    private AddPendingBalanceUseCase useCase;

    @Test
    void shouldAddPendingBalanceSuccessfully() {
        // Given
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        UserWallet wallet = UserWallet.builder()
            .userId(userId)
            .build();

        when(repository.findByUserId(userId))
            .thenReturn(Optional.of(wallet));

        // When
        AddPendingBalanceCommand command = AddPendingBalanceCommand.builder()
            .userId(userId)
            .amount(amount)
            .build();

        useCase.execute(command);

        // Then
        verify(repository).save(any(UserWallet.class));
        assertEquals(amount, wallet.getPendingBalance());
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserWalletRepositoryAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UserWalletRepository repository;

    @Test
    void shouldSaveAndRetrieveWallet() {
        // Given
        UserWallet wallet = UserWallet.builder()
            .userId(1L)
            .build();

        // When
        UserWallet saved = repository.save(wallet);
        UserWallet retrieved = repository.findById(saved.getId()).get();

        // Then
        assertNotNull(saved.getId());
        assertEquals(saved.getUserId(), retrieved.getUserId());
    }
}
```

### Coverage Requirements

- **Minimum**: 80% line coverage
- **Focus**: Domain logic and use cases
- **Run coverage**:

```bash
./mvnw clean test jacoco:report
```

## 💬 Commit Message Guidelines

Follow [Conventional Commits](https://www.conventionalcommits.org/):

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```
feat(wallet): add lock/unlock balance functionality

Implement balance locking for pending payouts.
Includes domain logic, use cases, and API endpoints.

Closes #123
```

```
fix(user): prevent duplicate referral codes

Add retry logic with max attempts to ensure unique codes.
Includes unit tests for edge cases.

Fixes #456
```

```
docs(readme): update API examples

Add examples for wallet endpoints with curl commands.
```

## 🔍 Pull Request Process

### 1. Before Creating PR

- [ ] All tests pass locally
- [ ] Code follows coding standards
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No merge conflicts with main branch

### 2. PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How was this tested?

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No new warnings
```

### 3. Review Process

- At least 1 approval required
- All CI checks must pass
- Address review comments
- Squash commits before merge (if needed)

### 4. After Merge

- Delete feature branch
- Update related issues
- Monitor for any issues

## 🎯 Areas for Contribution

### High Priority

- [ ] Keycloak integration module
- [ ] Affiliate network integration (Shopee)
- [ ] Cashback calculation engine
- [ ] Payout management
- [ ] Admin dashboard endpoints

### Medium Priority

- [ ] Integration tests
- [ ] Performance optimization
- [ ] API documentation improvements
- [ ] Error handling enhancements

### Low Priority

- [ ] Code refactoring
- [ ] Additional utility methods
- [ ] Documentation translations

## ❓ Questions?

- Create an issue for questions
- Join our developer chat (if available)
- Email: dev@cashbee.com

---

Thank you for contributing to CashBee Backend! 🎉
