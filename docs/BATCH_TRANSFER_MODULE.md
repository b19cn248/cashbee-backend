# Module Batch Transfer - Tài liệu Chi tiết

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc và Thiết kế](#2-kiến-trúc-và-thiết-kế)
3. [Database Schema](#3-database-schema)
4. [Domain Layer](#4-domain-layer)
5. [Infrastructure Layer](#5-infrastructure-layer)
6. [Application Layer](#6-application-layer)
7. [Presentation Layer](#7-presentation-layer)
8. [Luồng xử lý (Flow)](#8-luồng-xử-lý-flow)
9. [API Endpoints](#9-api-endpoints)
10. [Configuration](#10-configuration)
11. [Testing Guide](#11-testing-guide)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Tổng quan

### 1.1. Mục đích
Module Batch Transfer được thiết kế để xuất file Excel chứa danh sách người dùng cần chuyển tiền hoàn (cashback) theo định dạng của VPBank, giúp admin thực hiện chuyển khoản hàng loạt (batch transfer).

### 1.2. Yêu cầu nghiệp vụ
- **Điều kiện xuất**: Chỉ xuất user có số dư khả dụng >= 50,000 VND và đã có thông tin ngân hàng
- **Hai cách sử dụng**:
  1. **Manual Export**: Admin chủ động gọi API để xuất file
  2. **Scheduled Export**: Hệ thống tự động xuất file vào mỗi thứ 2 lúc 08:00 sáng
- **File Format**: Excel .xls (định dạng cũ của Excel) theo template VPBank
- **Delivery**:
  - Download trực tiếp qua API (file được tạo mới mỗi lần)
  - Gửi email tự động (scheduled) hoặc theo yêu cầu
- **Tracking**: Lưu metadata của mỗi lần export vào database để theo dõi lịch sử

### 1.3. Tính năng chính
✅ Export danh sách user đủ điều kiện ra file Excel
✅ Tạo file theo template VPBank (đảm bảo format đúng)
✅ Gửi file qua email với HTML template đẹp
✅ Scheduled job tự động chạy hàng tuần
✅ Xem lịch sử export với phân trang
✅ Tạo batch code duy nhất cho mỗi lần export

---

## 2. Kiến trúc và Thiết kế

### 2.1. Clean Architecture / Hexagonal Architecture

Module này tuân thủ nguyên tắc Clean Architecture với 4 layers riêng biệt:

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer (cashbee-presentation)              │
│  - Controllers: BatchTransferController                 │
│  - Schedulers: BatchTransferScheduler                   │
│  - DTOs: ApiResponse                                    │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Application Layer (cashbee-application)                │
│  - Use Cases: Export, Generate, GetHistory             │
│  - Services: VPBankExcelGenerator, EmailService        │
│  - DTOs: Request/Response objects                      │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Domain Layer (cashbee-domain)                          │
│  - Models: BatchTransferExport, UserBankAccount        │
│  - Enums: ExportStatus, ExportType                     │
│  - Repository Interfaces (ports)                        │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Infrastructure Layer (cashbee-infrastructure)          │
│  - JPA Entities: *JpaEntity                            │
│  - JPA Repositories: *JpaRepository                    │
│  - Adapters: *RepositoryAdapter                        │
│  - Mappers: Entity ↔ Domain                            │
└─────────────────────────────────────────────────────────┘
```

### 2.2. Dependency Rule
- **Presentation** → Application → Domain ← Infrastructure
- Domain layer không phụ thuộc vào bất kỳ layer nào (pure Java POJOs)
- Infrastructure implements interfaces từ Domain
- Application orchestrates business logic

### 2.3. Tại sao Clean Architecture?
- **Testability**: Dễ dàng test từng layer riêng biệt
- **Maintainability**: Thay đổi infrastructure (database, framework) không ảnh hưởng business logic
- **Separation of Concerns**: Mỗi layer có trách nhiệm riêng
- **Flexibility**: Dễ dàng thêm features mới hoặc thay đổi implementation

---

## 3. Database Schema

### 3.1. Bảng `user_bank_account`

**Mục đích**: Lưu thông tin tài khoản ngân hàng mặc định của user

```sql
CREATE TABLE user_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,           -- FK to users.id
    account_number VARCHAR(50) NOT NULL,      -- Số tài khoản
    account_name VARCHAR(255) NOT NULL,       -- Tên chủ tài khoản
    bank_name VARCHAR(100) NOT NULL,          -- Tên ngân hàng (VD: "VPBANK")
    bank_code VARCHAR(20),                    -- Mã ngân hàng (VD: "970432")
    branch_name VARCHAR(255),                 -- Chi nhánh
    verified BOOLEAN DEFAULT FALSE,           -- Đã xác thực chưa
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_bank_code (bank_code),
    INDEX idx_verified (verified)
);
```

**Quan trọng**:
- `user_id` là UNIQUE - mỗi user chỉ có 1 tài khoản mặc định
- Nếu user chưa có record trong bảng này → skip khi export

### 3.2. Bảng `batch_transfer_export`

**Mục đích**: Lưu metadata của mỗi lần export (KHÔNG lưu file)

```sql
CREATE TABLE batch_transfer_export (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_code VARCHAR(50) NOT NULL UNIQUE,   -- BATCH_YYYYMMDD_XXX
    file_name VARCHAR(255) NOT NULL,          -- BATCH_YYYYMMDD_XXX.xls
    total_users INT NOT NULL,                 -- Số lượng user trong batch
    total_amount DECIMAL(15,2) NOT NULL,      -- Tổng số tiền (VND)
    status VARCHAR(20) NOT NULL,              -- PENDING, COMPLETED, FAILED
    export_type VARCHAR(20) NOT NULL,         -- MANUAL, SCHEDULED
    remark_template VARCHAR(500),             -- Template nội dung CK
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_batch_code (batch_code),
    INDEX idx_export_type (export_type),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
);
```

**Batch Code Format**: `BATCH_YYYYMMDD_XXX`
- `YYYYMMDD`: Ngày tạo (VD: 20251119)
- `XXX`: Số thứ tự trong ngày (001, 002, ...)
- Ví dụ: `BATCH_20251119_001`

---

## 4. Domain Layer

### 4.1. Enums

#### ExportStatus
```java
public enum ExportStatus {
    PENDING,      // Đang chờ xử lý
    COMPLETED,    // Đã hoàn thành
    FAILED        // Thất bại
}
```

#### ExportType
```java
public enum ExportType {
    MANUAL,       // Admin export thủ công
    SCHEDULED     // Hệ thống tự động export
}
```

### 4.2. Domain Models

#### UserBankAccount
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/model/UserBankAccount.java`

```java
@Data
@Builder
public class UserBankAccount {
    private Long id;
    private Long userId;              // FK to User
    private String accountNumber;     // Bắt buộc
    private String accountName;       // Bắt buộc
    private String bankName;          // Bắt buộc
    private String bankCode;          // Optional
    private String branchName;        // Optional
    private Boolean verified;         // Default: false
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // Business logic
    public void validate() {
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("Account number is required");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalStateException("Account name is required");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalStateException("Bank name is required");
        }
    }

    public boolean isNew() {
        return id == null;
    }

    public void markAsVerified() {
        this.verified = true;
    }
}
```

**Business Rules**:
- Account number, account name, bank name là bắt buộc
- Mỗi user chỉ có 1 bank account (enforced ở database)
- Validation được gọi trước khi save

#### BatchTransferExport
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/model/BatchTransferExport.java`

```java
@Data
@Builder
public class BatchTransferExport {
    private Long id;
    private String batchCode;         // Unique identifier
    private String fileName;          // File name (.xls)
    private Integer totalUsers;       // Số user trong batch
    private BigDecimal totalAmount;   // Tổng tiền (VND)
    private ExportStatus status;      // PENDING/COMPLETED/FAILED
    private ExportType exportType;    // MANUAL/SCHEDULED
    private String remarkTemplate;    // Template nội dung CK
    private LocalDateTime createdAt;

    // Business logic
    public void validate() {
        if (batchCode == null || batchCode.isBlank()) {
            throw new IllegalStateException("Batch code is required");
        }
        if (totalUsers == null || totalUsers < 0) {
            throw new IllegalStateException("Total users must be >= 0");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Total amount must be >= 0");
        }
        if (status == null) {
            throw new IllegalStateException("Status is required");
        }
        if (exportType == null) {
            throw new IllegalStateException("Export type is required");
        }
    }

    public void markAsCompleted() {
        this.status = ExportStatus.COMPLETED;
    }

    public boolean isManual() {
        return exportType == ExportType.MANUAL;
    }

    public boolean isScheduled() {
        return exportType == ExportType.SCHEDULED;
    }
}
```

### 4.3. Repository Interfaces (Ports)

#### UserBankAccountRepository
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/repository/UserBankAccountRepository.java`

```java
public interface UserBankAccountRepository {
    Optional<UserBankAccount> findByUserId(Long userId);
    UserBankAccount save(UserBankAccount bankAccount);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
```

#### BatchTransferExportRepository
**File**: `cashbee-domain/src/main/java/com/cashbee/domain/repository/BatchTransferExportRepository.java`

```java
public interface BatchTransferExportRepository {
    Optional<BatchTransferExport> findByBatchCode(String batchCode);
    BatchTransferExport save(BatchTransferExport batchExport);
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    Page<BatchTransferExport> findAll(Pageable pageable);
    Page<BatchTransferExport> findByExportType(String exportType, Pageable pageable);
}
```

---

## 5. Infrastructure Layer

### 5.1. JPA Entities

#### UserBankAccountJpaEntity
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/entity/UserBankAccountJpaEntity.java`

```java
@Entity
@Table(name = "user_bank_account", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_bank_code", columnList = "bank_code"),
    @Index(name = "idx_verified", columnList = "verified")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBankAccountJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

#### BatchTransferExportJpaEntity
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/entity/BatchTransferExportJpaEntity.java`

```java
@Entity
@Table(name = "batch_transfer_export", indexes = {
    @Index(name = "idx_batch_code", columnList = "batch_code"),
    @Index(name = "idx_export_type", columnList = "export_type"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferExportJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "total_users", nullable = false)
    private Integer totalUsers;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 20)
    private ExportType exportType;

    @Column(name = "remark_template", length = 500)
    private String remarkTemplate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

**Lưu ý**:
- Enums được lưu dưới dạng STRING (không phải ORDINAL)
- Timestamps được auto-generate bởi Hibernate

### 5.2. Spring Data JPA Repositories

#### UserBankAccountJpaRepository
```java
@Repository
public interface UserBankAccountJpaRepository
    extends JpaRepository<UserBankAccountJpaEntity, Long> {

    Optional<UserBankAccountJpaEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
```

#### BatchTransferExportJpaRepository
```java
@Repository
public interface BatchTransferExportJpaRepository
    extends JpaRepository<BatchTransferExportJpaEntity, Long> {

    Optional<BatchTransferExportJpaEntity> findByBatchCode(String batchCode);
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    Page<BatchTransferExportJpaEntity> findByExportType(
        ExportType exportType,
        Pageable pageable
    );
}
```

### 5.3. Mappers

#### UserBankAccountMapper
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/mapper/UserBankAccountMapper.java`

```java
public class UserBankAccountMapper {

    // Entity -> Domain
    public static UserBankAccount toDomain(UserBankAccountJpaEntity entity) {
        if (entity == null) return null;

        return UserBankAccount.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .accountNumber(entity.getAccountNumber())
            .accountName(entity.getAccountName())
            .bankName(entity.getBankName())
            .bankCode(entity.getBankCode())
            .branchName(entity.getBranchName())
            .verified(entity.getVerified())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .deletedAt(entity.getDeletedAt())
            .build();
    }

    // Domain -> Entity
    public static UserBankAccountJpaEntity toEntity(UserBankAccount domain) {
        if (domain == null) return null;

        return UserBankAccountJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .accountNumber(domain.getAccountNumber())
            .accountName(domain.getAccountName())
            .bankName(domain.getBankName())
            .bankCode(domain.getBankCode())
            .branchName(domain.getBranchName())
            .verified(domain.getVerified())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .deletedAt(domain.getDeletedAt())
            .build();
    }
}
```

### 5.4. Repository Adapters

#### UserBankAccountRepositoryAdapter
**File**: `cashbee-infrastructure/src/main/java/com/cashbee/infrastructure/persistence/adapter/UserBankAccountRepositoryAdapter.java`

```java
@Component
@RequiredArgsConstructor
public class UserBankAccountRepositoryAdapter
    implements UserBankAccountRepository {

    private final UserBankAccountJpaRepository jpaRepository;

    @Override
    public Optional<UserBankAccount> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
            .map(UserBankAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public UserBankAccount save(UserBankAccount bankAccount) {
        // Validate domain model
        bankAccount.validate();

        // Convert to entity
        UserBankAccountJpaEntity entity =
            UserBankAccountMapper.toEntity(bankAccount);

        // Save to database
        UserBankAccountJpaEntity savedEntity = jpaRepository.save(entity);

        // Convert back to domain
        return UserBankAccountMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
```

**Quan trọng**:
- Adapter validates domain model trước khi save
- Tất cả operations đều convert giữa Entity ↔ Domain
- Adapter là bridge giữa Domain và Infrastructure

---

## 6. Application Layer

### 6.1. DTOs

#### ExportBatchTransferRequest
**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/batch/ExportBatchTransferRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportBatchTransferRequest {

    @Builder.Default
    private BigDecimal minBalance = new BigDecimal("50000");

    private String remarkTemplate;  // Optional, auto-generate nếu null

    private String exportType;      // Optional, default: MANUAL
}
```

**Ví dụ request**:
```json
{
  "minBalance": 50000,
  "remarkTemplate": "Hoan tien CashBee 11/2025",
  "exportType": "MANUAL"
}
```

#### ExportBatchTransferResponse
**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/batch/ExportBatchTransferResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportBatchTransferResponse {
    private String batchCode;           // BATCH_20251119_001
    private String fileName;            // BATCH_20251119_001.xls
    private Integer totalUsers;         // 25
    private BigDecimal totalAmount;     // 15500000
    private LocalDateTime exportedAt;   // 2025-11-19T14:30:00
    private String message;             // User-friendly message
}
```

**Ví dụ response**:
```json
{
  "batchCode": "BATCH_20251119_001",
  "fileName": "BATCH_20251119_001.xls",
  "totalUsers": 25,
  "totalAmount": 15500000,
  "exportedAt": "2025-11-19T14:30:00",
  "message": "Export completed. 25 users eligible for transfer (total: 15,500,000 VND)"
}
```

#### BatchTransferRow
**File**: `cashbee-application/src/main/java/com/cashbee/application/dto/batch/BatchTransferRow.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferRow {
    private Integer stt;                // Row number (1, 2, 3, ...)
    private String accountNumber;       // Số tài khoản
    private String accountName;         // Tên chủ TK
    private BigDecimal amount;          // Số tiền
    private String bankName;            // Ngân hàng
    private String remark;              // Nội dung CK
    private Long userId;                // Metadata (không xuất ra Excel)
}
```

**Mapping với Excel columns**:
| STT | DTO Field | Excel Column | Ví dụ |
|-----|-----------|--------------|-------|
| 0 | stt | # | 1 |
| 1 | accountNumber | Số Tài Khoản | 1234567890 |
| 2 | accountName | Tên Tài Khoản | NGUYEN VAN A |
| 3 | amount | Số Tiền | 50,000.00 |
| 4 | bankName | Ngân Hàng Hưởng | VPBANK |
| 5 | remark | Nội Dung | Hoan tien CashBee 11/2025 |

### 6.2. Use Cases

#### ExportBatchTransferUseCase
**File**: `cashbee-application/src/main/java/com/cashbee/application/service/usecase/ExportBatchTransferUseCase.java`

**Mục đích**: Tạo batch export metadata và lưu vào database

**Flow**:
```
1. Query eligible users
   └─> SELECT COUNT(*), SUM(balance)
       FROM users u
       JOIN user_bank_account uba ON u.id = uba.user_id
       WHERE u.balance >= minBalance

2. Generate batch code
   └─> Format: BATCH_YYYYMMDD_XXX
   └─> Count existing batches today to get sequence number

3. Generate remark template (if not provided)
   └─> Format: "Hoan tien CashBee MM/YYYY"

4. Create BatchTransferExport domain model
   └─> Set: batchCode, fileName, totalUsers, totalAmount, status, exportType

5. Validate and save to database
   └─> Call repository.save()

6. Return response DTO
```

**Code snippet**:
```java
@Transactional
public ExportBatchTransferResponse execute(ExportBatchTransferRequest request) {
    // 1. Query eligible users
    EligibleUsersResult result = queryEligibleUsers(request.getMinBalance());

    if (result.getTotalUsers() == 0) {
        throw new BusinessException("NO_ELIGIBLE_USERS", "...");
    }

    // 2. Generate batch code
    String batchCode = generateBatchCode();

    // 3. Generate remark if not provided
    String remark = request.getRemarkTemplate();
    if (remark == null || remark.isBlank()) {
        remark = generateDefaultRemark();
    }

    // 4. Determine export type
    ExportType exportType = ExportType.MANUAL;
    if ("SCHEDULED".equalsIgnoreCase(request.getExportType())) {
        exportType = ExportType.SCHEDULED;
    }

    // 5. Create and save domain model
    BatchTransferExport batchExport = BatchTransferExport.builder()
        .batchCode(batchCode)
        .fileName(batchCode + ".xls")
        .totalUsers(result.getTotalUsers())
        .totalAmount(result.getTotalAmount())
        .status(ExportStatus.COMPLETED)
        .exportType(exportType)
        .remarkTemplate(remark)
        .createdAt(LocalDateTime.now())
        .build();

    batchExport.validate();
    BatchTransferExport saved = batchTransferExportRepository.save(batchExport);

    // 6. Build response
    return ExportBatchTransferResponse.builder()
        .batchCode(saved.getBatchCode())
        .fileName(saved.getFileName())
        .totalUsers(saved.getTotalUsers())
        .totalAmount(saved.getTotalAmount())
        .exportedAt(saved.getCreatedAt())
        .message(String.format(
            "Export completed. %d users eligible for transfer (total: %,d VND)",
            saved.getTotalUsers(),
            saved.getTotalAmount().longValue()
        ))
        .build();
}
```

#### GenerateBatchTransferFileUseCase
**File**: `cashbee-application/src/main/java/com/cashbee/application/service/usecase/GenerateBatchTransferFileUseCase.java`

**Mục đích**: Tạo file Excel với dữ liệu realtime

**Flow**:
```
1. Load batch metadata (nếu có batch code)
   └─> Optional: Get remark template from metadata

2. Query eligible users với bank account details
   └─> SELECT u.id, u.balance, uba.*
       FROM users u
       JOIN user_bank_account uba ON u.id = uba.user_id
       WHERE u.balance >= minBalance
       ORDER BY u.balance DESC

3. Build list of BatchTransferRow
   └─> Auto-increment STT: 1, 2, 3, ...
   └─> Map database columns to DTO fields

4. Call VPBankExcelGenerator.generate(rows)
   └─> Returns byte[]

5. Return Excel file bytes
```

**Code snippet**:
```java
@Transactional(readOnly = true)
public byte[] generateByBatchCode(String batchCode) {
    // Load batch metadata
    BatchTransferExport batchExport = batchTransferExportRepository
        .findByBatchCode(batchCode)
        .orElseThrow(() -> new BusinessException("BATCH_NOT_FOUND", "..."));

    // Generate file with stored remark template
    return generateFile(
        new BigDecimal("50000"),
        batchExport.getRemarkTemplate()
    );
}

@Transactional(readOnly = true)
public byte[] generateFile(BigDecimal minBalance, String remarkTemplate) {
    // Generate remark if not provided
    String remark = remarkTemplate;
    if (remark == null || remark.isBlank()) {
        remark = generateDefaultRemark();
    }

    // Query eligible users
    List<BatchTransferRow> rows = queryEligibleUsersWithBankAccounts(
        minBalance,
        remark
    );

    if (rows.isEmpty()) {
        throw new BusinessException("NO_ELIGIBLE_USERS", "...");
    }

    // Generate Excel file
    byte[] excelBytes = excelGenerator.generate(rows);

    return excelBytes;
}
```

**Row Mapper**:
```java
private static class BatchTransferRowMapper implements RowMapper<BatchTransferRow> {
    private final String remarkTemplate;
    private int stt = 1;

    @Override
    public BatchTransferRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BatchTransferRow.builder()
            .stt(stt++)
            .userId(rs.getLong("user_id"))
            .accountNumber(rs.getString("account_number"))
            .accountName(rs.getString("account_name"))
            .amount(rs.getBigDecimal("balance"))
            .bankName(rs.getString("bank_name"))
            .remark(remarkTemplate)
            .build();
    }
}
```

#### GetBatchExportHistoryUseCase
**File**: `cashbee-application/src/main/java/com/cashbee/application/service/usecase/GetBatchExportHistoryUseCase.java`

**Mục đích**: Xem lịch sử export với phân trang

```java
@Transactional(readOnly = true)
public Page<ExportBatchTransferResponse> execute(Pageable pageable) {
    Page<BatchTransferExport> batchExports =
        batchTransferExportRepository.findAll(pageable);

    return batchExports.map(this::toResponse);
}

@Transactional(readOnly = true)
public Page<ExportBatchTransferResponse> executeByType(
    String exportType,
    Pageable pageable
) {
    Page<BatchTransferExport> batchExports =
        batchTransferExportRepository.findByExportType(exportType, pageable);

    return batchExports.map(this::toResponse);
}
```

### 6.3. Services

#### VPBankExcelGenerator
**File**: `cashbee-application/src/main/java/com/cashbee/application/service/excel/VPBankExcelGenerator.java`

**Mục đích**: Generate file Excel .xls theo template VPBank

**Template**: `resources/templates/vpbank_template.xls`

**Flow**:
```
1. Load template from classpath
   └─> Try to load vpbank_template.xls
   └─> Fallback: Create new HSSFWorkbook if template not found

2. Clear existing data (keep header)
   └─> Remove all rows except row 0

3. Create header if not exists
   └─> Headers: #, Số Tài Khoản, Tên Tài Khoản, Số Tiền, Ngân Hàng, Nội Dung
   └─> Bold font, center alignment

4. Add data rows
   └─> For each BatchTransferRow:
       - Cell 0: STT (center aligned)
       - Cell 1: Account Number (left aligned, text format)
       - Cell 2: Account Name (left aligned)
       - Cell 3: Amount (right aligned, number format: #,##0.00)
       - Cell 4: Bank Name (left aligned)
       - Cell 5: Remark (left aligned)

5. Auto-size columns
   └─> Auto-size all 6 columns
   └─> Add padding: +1000 units

6. Convert to byte array
   └─> ByteArrayOutputStream
   └─> workbook.write(outputStream)
   └─> Return bytes
```

**Code snippet - Key methods**:

```java
public byte[] generate(List<BatchTransferRow> rows) {
    log.info("Generating Excel file with {} rows", rows.size());

    try {
        // Load template
        HSSFWorkbook workbook = loadTemplate();
        HSSFSheet sheet = workbook.getSheetAt(0);

        // Clear existing data
        clearExistingData(sheet);

        // Create header if not exists
        if (sheet.getPhysicalNumberOfRows() == 0) {
            createHeader(sheet, workbook);
        }

        // Add data rows
        addDataRows(sheet, rows, workbook);

        // Auto-size columns
        autoSizeColumns(sheet);

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();

    } catch (Exception e) {
        log.error("Failed to generate Excel file", e);
        throw new BusinessException("EXCEL_GENERATION_FAILED", "...");
    }
}

private void createHeader(HSSFSheet sheet, HSSFWorkbook workbook) {
    Row headerRow = sheet.createRow(0);

    // Header style: bold, center
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

    String[] headers = {
        "#",
        "Số Tài Khoản (Account Number)",
        "Tên Tài Khoản (Account Name)",
        "Số Tiền (Amount)",
        "Ngân Hàng Hưởng (Bank)",
        "Nội Dung (Remark)"
    };

    for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
    }
}

private void addDataRows(HSSFSheet sheet, List<BatchTransferRow> rows,
                        HSSFWorkbook workbook) {
    CellStyle numberStyle = createNumberStyle(workbook);
    CellStyle amountStyle = createAmountStyle(workbook);
    CellStyle textStyle = createTextStyle(workbook);

    int rowNum = 1;

    for (BatchTransferRow data : rows) {
        Row row = sheet.createRow(rowNum++);

        // Column 0: STT
        Cell sttCell = row.createCell(0);
        sttCell.setCellValue(data.getStt());
        sttCell.setCellStyle(numberStyle);

        // Column 1: Account Number
        Cell accountNumberCell = row.createCell(1);
        accountNumberCell.setCellValue(data.getAccountNumber());
        accountNumberCell.setCellStyle(textStyle);

        // Column 2: Account Name
        Cell accountNameCell = row.createCell(2);
        accountNameCell.setCellValue(data.getAccountName());
        accountNameCell.setCellStyle(textStyle);

        // Column 3: Amount
        Cell amountCell = row.createCell(3);
        amountCell.setCellValue(data.getAmount().doubleValue());
        amountCell.setCellStyle(amountStyle);

        // Column 4: Bank Name
        Cell bankCell = row.createCell(4);
        bankCell.setCellValue(data.getBankName());
        bankCell.setCellStyle(textStyle);

        // Column 5: Remark
        Cell remarkCell = row.createCell(5);
        remarkCell.setCellValue(data.getRemark());
        remarkCell.setCellStyle(textStyle);
    }
}

private CellStyle createAmountStyle(HSSFWorkbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);

    // Format: #,##0.00
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,##0.00"));

    return style;
}
```

**Cell Styles**:
- **Number Style** (STT): Center aligned
- **Amount Style** (Số Tiền): Right aligned, format `#,##0.00` (thousand separator)
- **Text Style**: Left aligned

#### BatchTransferEmailService
**File**: `cashbee-application/src/main/java/com/cashbee/application/service/email/BatchTransferEmailService.java`

**Mục đích**: Gửi file Excel qua email

**Flow**:
```
1. Create MimeMessage with JavaMailSender
2. Set sender and recipient
3. Set subject: "CashBee Batch Transfer - {fileName}"
4. Build HTML email body
5. Attach Excel file as ByteArrayResource
6. Send email
```

**Code snippet**:
```java
public void sendBatchTransferFile(
    String fileName,
    byte[] fileBytes,
    Integer totalUsers,
    BigDecimal totalAmount,
    String recipientEmail
) {
    log.info("Sending batch transfer file {} to {}",
        fileName,
        recipientEmail != null ? recipientEmail : adminEmail
    );

    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set sender and recipient
        helper.setFrom(fromEmail);
        helper.setTo(recipientEmail != null ? recipientEmail : adminEmail);

        // Set subject
        helper.setSubject("CashBee Batch Transfer - " + fileName);

        // Set email body (HTML)
        String body = buildEmailBody(fileName, totalUsers, totalAmount);
        helper.setText(body, true);

        // Attach Excel file
        ByteArrayResource attachment = new ByteArrayResource(fileBytes);
        helper.addAttachment(fileName, attachment);

        // Send
        mailSender.send(message);

        log.info("Email sent successfully");

    } catch (MessagingException e) {
        log.error("Failed to send email", e);
        throw new BusinessException("EMAIL_SEND_FAILED", "...");
    }
}
```

**Email Template** (HTML):
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        .header { background-color: #4CAF50; color: white; }
        .content { background-color: #f9f9f9; padding: 20px; }
        .info-row {
            background-color: white;
            border-left: 4px solid #4CAF50;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>CashBee - Batch Transfer Export</h2>
        </div>
        <div class="content">
            <p>Xin chào,</p>
            <p>File batch transfer đã được tạo thành công...</p>

            <div class="info-row">
                <span class="label">Tên file:</span>
                <span class="value">{fileName}</span>
            </div>

            <div class="info-row">
                <span class="label">Số lượng user:</span>
                <span class="value">{totalUsers} người</span>
            </div>

            <div class="info-row">
                <span class="label">Tổng số tiền:</span>
                <span class="value">{totalAmount} VND</span>
            </div>

            <div class="info-row">
                <span class="label">Thời gian tạo:</span>
                <span class="value">{formattedDateTime}</span>
            </div>

            <p><strong>Lưu ý:</strong> File Excel đính kèm có thể được upload
            trực tiếp lên hệ thống VPBank...</p>
        </div>
    </div>
</body>
</html>
```

---

## 7. Presentation Layer

### 7.1. BatchTransferController

**File**: `cashbee-presentation/src/main/java/com/cashbee/presentation/controller/BatchTransferController.java`

**Base Path**: `/api/admin/batch-transfer`

**Security**: `@PreAuthorize("hasRole('ADMIN')")` - Chỉ ADMIN mới access được

#### Endpoints

##### 1. POST /export
**Mục đích**: Tạo batch export metadata

**Request**:
```json
{
  "minBalance": 50000,
  "remarkTemplate": "Hoan tien CashBee 11/2025",
  "exportType": "MANUAL"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251119_001",
    "fileName": "BATCH_20251119_001.xls",
    "totalUsers": 25,
    "totalAmount": 15500000,
    "exportedAt": "2025-11-19T14:30:00",
    "message": "Export completed. 25 users eligible for transfer (total: 15,500,000 VND)"
  }
}
```

**Code**:
```java
@PostMapping("/export")
public ResponseEntity<ApiResponse<ExportBatchTransferResponse>> exportBatchTransfer(
    @RequestBody(required = false) ExportBatchTransferRequest request
) {
    if (request == null) {
        request = ExportBatchTransferRequest.builder().build();
    }

    ExportBatchTransferResponse response =
        exportBatchTransferUseCase.execute(request);

    return ResponseEntity.ok(ApiResponse.success(response));
}
```

##### 2. GET /download
**Mục đích**: Download file Excel

**Parameters**:
- `batchCode` (optional): Nếu có, tạo file theo batch code. Nếu không, tạo file mới.

**Request**:
```
GET /api/admin/batch-transfer/download?batchCode=BATCH_20251119_001
```

**Response**: Excel file (.xls) as attachment

**Code**:
```java
@GetMapping("/download")
public ResponseEntity<byte[]> downloadBatchTransferFile(
    @RequestParam(required = false) String batchCode
) {
    byte[] excelBytes;
    String fileName;

    if (batchCode != null && !batchCode.isBlank()) {
        // Generate by batch code
        excelBytes = generateBatchTransferFileUseCase
            .generateByBatchCode(batchCode);
        fileName = batchCode + ".xls";
    } else {
        // Generate fresh
        excelBytes = generateBatchTransferFileUseCase.generateFile(
            BigDecimal.valueOf(50000),
            null
        );
        fileName = "BATCH_TRANSFER_" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            ) + ".xls";
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", fileName);
    headers.setContentLength(excelBytes.length);

    return ResponseEntity.ok()
        .headers(headers)
        .body(excelBytes);
}
```

##### 3. POST /send-email
**Mục đích**: Gửi file qua email

**Request**:
```json
{
  "batchCode": "BATCH_20251119_001",
  "recipientEmail": "admin@cashbee.com"
}
```

**Response**:
```json
{
  "success": true,
  "data": "Email sent successfully to admin@cashbee.com"
}
```

**Code**:
```java
@PostMapping("/send-email")
public ResponseEntity<ApiResponse<String>> sendBatchTransferEmail(
    @RequestBody SendEmailRequest request
) {
    // Generate file
    byte[] excelBytes;
    String fileName;

    if (request.getBatchCode() != null && !request.getBatchCode().isBlank()) {
        excelBytes = generateBatchTransferFileUseCase
            .generateByBatchCode(request.getBatchCode());
        fileName = request.getBatchCode() + ".xls";
    } else {
        excelBytes = generateBatchTransferFileUseCase.generateFile(
            BigDecimal.valueOf(50000),
            null
        );
        fileName = "BATCH_TRANSFER_" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            ) + ".xls";
    }

    // Send email
    emailService.sendBatchTransferFile(
        fileName,
        excelBytes,
        0, // TODO: Get from metadata
        BigDecimal.ZERO, // TODO: Get from metadata
        request.getRecipientEmail()
    );

    return ResponseEntity.ok(ApiResponse.success(
        "Email sent successfully to " +
        (request.getRecipientEmail() != null
            ? request.getRecipientEmail()
            : "admin email")
    ));
}
```

##### 4. GET /history
**Mục đích**: Xem lịch sử export

**Parameters**:
- `page` (default: 0)
- `size` (default: 10)
- `sort` (default: createdAt)

**Request**:
```
GET /api/admin/batch-transfer/history?page=0&size=10
```

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "batchCode": "BATCH_20251119_001",
        "fileName": "BATCH_20251119_001.xls",
        "totalUsers": 25,
        "totalAmount": 15500000,
        "exportedAt": "2025-11-19T14:30:00",
        "message": "MANUAL export: 25 users, 15,500,000 VND"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```

##### 5. GET /history/{exportType}
**Mục đích**: Xem lịch sử theo loại export

**Parameters**:
- `exportType`: MANUAL hoặc SCHEDULED
- `page`, `size`, `sort`

**Request**:
```
GET /api/admin/batch-transfer/history/MANUAL?page=0&size=10
```

### 7.2. BatchTransferScheduler

**File**: `cashbee-presentation/src/main/java/com/cashbee/presentation/scheduler/BatchTransferScheduler.java`

**Mục đích**: Tự động export và gửi email mỗi tuần

**Schedule**: Every Monday at 08:00 (Cron: `0 0 8 * * MON`)

**Flow**:
```
1. Create batch export metadata
   └─> exportType = SCHEDULED

2. Generate Excel file
   └─> Call generateByBatchCode()

3. Send via email
   └─> Send to admin email (from config)

4. Log success/failure
   └─> Don't throw exception (let scheduler continue)
```

**Code**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "cashbee.batch-transfer.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class BatchTransferScheduler {

    private final ExportBatchTransferUseCase exportBatchTransferUseCase;
    private final GenerateBatchTransferFileUseCase generateBatchTransferFileUseCase;
    private final BatchTransferEmailService emailService;

    @Value("${cashbee.batch-transfer.admin-email:admin@cashbee.com}")
    private String adminEmail;

    @Value("${cashbee.batch-transfer.min-balance:50000}")
    private BigDecimal minBalance;

    @Scheduled(cron = "${cashbee.batch-transfer.scheduler.cron:0 0 8 * * MON}")
    public void exportAndSendBatchTransferFile() {
        log.info("Starting scheduled batch transfer export");

        try {
            // 1. Create batch export metadata
            ExportBatchTransferRequest request = ExportBatchTransferRequest.builder()
                .minBalance(minBalance)
                .exportType("SCHEDULED")
                .build();

            ExportBatchTransferResponse response =
                exportBatchTransferUseCase.execute(request);

            log.info("Batch export created - {}", response.getBatchCode());

            // 2. Generate Excel file
            byte[] excelBytes = generateBatchTransferFileUseCase
                .generateByBatchCode(response.getBatchCode());

            log.info("Excel file generated ({} bytes)", excelBytes.length);

            // 3. Send via email
            emailService.sendBatchTransferFile(
                response.getFileName(),
                excelBytes,
                response.getTotalUsers(),
                response.getTotalAmount(),
                adminEmail
            );

            log.info("Batch transfer file sent successfully to {}", adminEmail);

        } catch (Exception e) {
            log.error("Failed to export and send batch transfer file", e);
            // Don't rethrow - let scheduler continue running
        }
    }
}
```

**Enable/Disable**:
```yaml
cashbee:
  batch-transfer:
    scheduler:
      enabled: false  # Disable scheduler
```

---

## 8. Luồng xử lý (Flow)

### 8.1. Flow: Manual Export (Admin gọi API)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Admin calls POST /api/admin/batch-transfer/export           │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. BatchTransferController                                      │
│    - Validate request                                           │
│    - Call ExportBatchTransferUseCase.execute()                  │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. ExportBatchTransferUseCase                                   │
│    - Query eligible users (JdbcTemplate)                        │
│      SELECT COUNT(*), SUM(balance)                              │
│      FROM users u                                               │
│      JOIN user_bank_account uba ON u.id = uba.user_id          │
│      WHERE u.balance >= 50000                                   │
│                                                                  │
│    - Generate batch code: BATCH_20251119_001                    │
│    - Generate remark: "Hoan tien CashBee 11/2025"               │
│    - Create BatchTransferExport domain model                    │
│    - Save to database                                           │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. BatchTransferExportRepository (Domain)                       │
│    - Call save()                                                │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. BatchTransferExportRepositoryAdapter (Infrastructure)        │
│    - Validate domain model                                      │
│    - Convert Domain → Entity (Mapper)                           │
│    - Call JpaRepository.save()                                  │
│    - Convert Entity → Domain (Mapper)                           │
│    - Return domain model                                        │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. Return Response to Admin                                     │
│    {                                                             │
│      "batchCode": "BATCH_20251119_001",                         │
│      "fileName": "BATCH_20251119_001.xls",                      │
│      "totalUsers": 25,                                          │
│      "totalAmount": 15500000,                                   │
│      "message": "Export completed..."                           │
│    }                                                             │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2. Flow: Download File Excel

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Admin calls GET /api/admin/batch-transfer/download          │
│    ?batchCode=BATCH_20251119_001                                │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. BatchTransferController                                      │
│    - Call GenerateBatchTransferFileUseCase                      │
│      .generateByBatchCode("BATCH_20251119_001")                 │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. GenerateBatchTransferFileUseCase                             │
│    - Load batch metadata from database                          │
│    - Query REALTIME user data (JdbcTemplate)                    │
│      SELECT u.id, u.balance, uba.*                              │
│      FROM users u                                               │
│      JOIN user_bank_account uba ON u.id = uba.user_id          │
│      WHERE u.balance >= 50000                                   │
│      ORDER BY u.balance DESC                                    │
│                                                                  │
│    - Build List<BatchTransferRow>                               │
│      Auto-increment STT: 1, 2, 3, ...                           │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. VPBankExcelGenerator.generate(rows)                          │
│    - Load template: vpbank_template.xls                         │
│    - Clear existing data                                        │
│    - Create header if not exists                                │
│    - Add data rows with formatting                              │
│      • STT: center aligned                                      │
│      • Account Number: text, left aligned                       │
│      • Account Name: text, left aligned                         │
│      • Amount: number, right aligned, format: #,##0.00          │
│      • Bank Name: text, left aligned                            │
│      • Remark: text, left aligned                               │
│    - Auto-size columns                                          │
│    - Convert to byte[]                                          │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Return Excel File to Browser                                 │
│    Content-Type: application/octet-stream                       │
│    Content-Disposition: attachment; filename="BATCH_...xls"     │
│    Body: byte[] (Excel file)                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 8.3. Flow: Scheduled Export (Auto chạy mỗi thứ 2)

```
┌─────────────────────────────────────────────────────────────────┐
│ Monday 08:00 - Scheduler triggers                               │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ BatchTransferScheduler.exportAndSendBatchTransferFile()         │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Create batch export metadata                            │
│    - exportType = SCHEDULED                                     │
│    - Call ExportBatchTransferUseCase.execute()                  │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Generate Excel file                                     │
│    - Call GenerateBatchTransferFileUseCase                      │
│      .generateByBatchCode(batchCode)                            │
│    - Returns: byte[] excelBytes                                 │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Send email                                              │
│    - Call BatchTransferEmailService.sendBatchTransferFile()     │
│    - To: admin@cashbee.com (from config)                        │
│    - Attachment: Excel file                                     │
│    - HTML body with summary                                     │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Log result                                              │
│    - Success: "Batch transfer file sent successfully"           │
│    - Error: Log error but don't throw (scheduler continues)     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. API Endpoints

### 9.1. Tổng quan Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/admin/batch-transfer/export` | ADMIN | Tạo batch export metadata |
| GET | `/api/admin/batch-transfer/download` | ADMIN | Download file Excel |
| POST | `/api/admin/batch-transfer/send-email` | ADMIN | Gửi file qua email |
| GET | `/api/admin/batch-transfer/history` | ADMIN | Xem lịch sử export |
| GET | `/api/admin/batch-transfer/history/{type}` | ADMIN | Xem lịch sử theo loại |

### 9.2. Chi tiết Endpoints

#### POST /api/admin/batch-transfer/export

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body** (optional):
```json
{
  "minBalance": 50000,
  "remarkTemplate": "Hoan tien CashBee 11/2025",
  "exportType": "MANUAL"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251119_001",
    "fileName": "BATCH_20251119_001.xls",
    "totalUsers": 25,
    "totalAmount": 15500000.00,
    "exportedAt": "2025-11-19T14:30:00",
    "message": "Export completed. 25 users eligible for transfer (total: 15,500,000 VND)"
  },
  "message": null,
  "timestamp": "2025-11-19T14:30:00"
}
```

**Error Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "message": "NO_ELIGIBLE_USERS: No users found with balance >= 50000 VND and valid bank account",
  "timestamp": "2025-11-19T14:30:00"
}
```

#### GET /api/admin/batch-transfer/download

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
```

**Query Parameters**:
- `batchCode` (optional): BATCH_20251119_001

**Success Response** (200 OK):
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="BATCH_20251119_001.xls"
Content-Length: 15360

[Binary Excel file data]
```

**Error Response** (404 Not Found):
```json
{
  "success": false,
  "data": null,
  "message": "BATCH_NOT_FOUND: Batch transfer export not found: BATCH_20251119_001",
  "timestamp": "2025-11-19T14:30:00"
}
```

#### POST /api/admin/batch-transfer/send-email

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body**:
```json
{
  "batchCode": "BATCH_20251119_001",
  "recipientEmail": "admin@cashbee.com"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": "Email sent successfully to admin@cashbee.com",
  "message": null,
  "timestamp": "2025-11-19T14:30:00"
}
```

#### GET /api/admin/batch-transfer/history

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
```

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 10)
- `sort` (default: createdAt,desc)

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "batchCode": "BATCH_20251119_002",
        "fileName": "BATCH_20251119_002.xls",
        "totalUsers": 30,
        "totalAmount": 18000000.00,
        "exportedAt": "2025-11-19T16:00:00",
        "message": "MANUAL export: 30 users, 18,000,000 VND"
      },
      {
        "batchCode": "BATCH_20251119_001",
        "fileName": "BATCH_20251119_001.xls",
        "totalUsers": 25,
        "totalAmount": 15500000.00,
        "exportedAt": "2025-11-19T14:30:00",
        "message": "MANUAL export: 25 users, 15,500,000 VND"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      }
    },
    "totalElements": 100,
    "totalPages": 10,
    "last": false,
    "size": 10,
    "number": 0,
    "first": true,
    "numberOfElements": 10,
    "empty": false
  },
  "message": null,
  "timestamp": "2025-11-19T14:30:00"
}
```

---

## 10. Configuration

### 10.1. Application Configuration (application.yml)

**File**: `cashbee-presentation/src/main/resources/application.yml`

```yaml
spring:
  # Email Configuration
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

cashbee:
  # Batch Transfer Configuration
  batch-transfer:
    # Minimum balance required for batch transfer (VND)
    min-balance: 50000

    # Admin email to receive scheduled batch transfer files
    admin-email: ${BATCH_TRANSFER_ADMIN_EMAIL:admin@cashbee.com}

    # Scheduler configuration
    scheduler:
      # Enable/disable scheduled batch transfer exports
      enabled: ${BATCH_TRANSFER_SCHEDULER_ENABLED:true}

      # Cron expression: Every Monday at 08:00
      # Format: second minute hour day-of-month month day-of-week
      cron: ${BATCH_TRANSFER_SCHEDULER_CRON:0 0 8 * * MON}
```

### 10.2. Environment Variables

**Production**:
```bash
# Email
MAIL_USERNAME=noreply@cashbee.com
MAIL_PASSWORD=your-app-password

# Batch Transfer
BATCH_TRANSFER_ADMIN_EMAIL=finance@cashbee.com
BATCH_TRANSFER_SCHEDULER_ENABLED=true
BATCH_TRANSFER_SCHEDULER_CRON=0 0 8 * * MON
```

**Development** (disable scheduler):
```bash
BATCH_TRANSFER_SCHEDULER_ENABLED=false
```

### 10.3. Database Migration

**File**: `cashbee-infrastructure/src/main/resources/db/changelog/020-create-batch-transfer-tables.xml`

Migration sẽ tự động chạy khi start application (Liquibase enabled).

---

## 11. Testing Guide

### 11.1. Test Manual Export

**Step 1**: Tạo test data
```sql
-- Insert user
INSERT INTO users (id, email, balance, created_at)
VALUES (1, 'test@example.com', 100000, NOW());

-- Insert bank account
INSERT INTO user_bank_account (user_id, account_number, account_name, bank_name, verified)
VALUES (1, '1234567890', 'NGUYEN VAN A', 'VPBANK', true);
```

**Step 2**: Call export API
```bash
curl -X POST http://localhost:8080/api/admin/batch-transfer/export \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "minBalance": 50000,
    "remarkTemplate": "Hoan tien CashBee Test"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "data": {
    "batchCode": "BATCH_20251119_001",
    "totalUsers": 1,
    "totalAmount": 100000
  }
}
```

**Step 3**: Download file
```bash
curl -X GET "http://localhost:8080/api/admin/batch-transfer/download?batchCode=BATCH_20251119_001" \
  -H "Authorization: Bearer {TOKEN}" \
  -o batch_transfer.xls
```

**Step 4**: Verify Excel file
- Mở file `batch_transfer.xls`
- Check header: #, Số Tài Khoản, Tên Tài Khoản, Số Tiền, Ngân Hàng, Nội Dung
- Check data row: 1, 1234567890, NGUYEN VAN A, 100,000.00, VPBANK, Hoan tien CashBee Test

### 11.2. Test Scheduled Export

**Step 1**: Enable scheduler trong application.yml
```yaml
cashbee:
  batch-transfer:
    scheduler:
      enabled: true
      cron: "0 * * * * *"  # Every minute (for testing)
```

**Step 2**: Start application và watch logs
```
2025-11-19 14:30:00 INFO  BatchTransferScheduler - Starting scheduled batch transfer export
2025-11-19 14:30:01 INFO  ExportBatchTransferUseCase - Found 25 eligible users
2025-11-19 14:30:02 INFO  BatchTransferScheduler - Batch export created - BATCH_20251119_001
2025-11-19 14:30:03 INFO  VPBankExcelGenerator - Generated Excel file (15360 bytes)
2025-11-19 14:30:04 INFO  BatchTransferEmailService - Email sent successfully to admin@cashbee.com
```

**Step 3**: Check email inbox
- Subject: "CashBee Batch Transfer - BATCH_20251119_001.xls"
- Attachment: BATCH_20251119_001.xls

### 11.3. Test Email Sending

**Step 1**: Call send-email API
```bash
curl -X POST http://localhost:8080/api/admin/batch-transfer/send-email \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "batchCode": "BATCH_20251119_001",
    "recipientEmail": "test@example.com"
  }'
```

**Step 2**: Check mailbox của test@example.com

### 11.4. Test History API

```bash
curl -X GET "http://localhost:8080/api/admin/batch-transfer/history?page=0&size=10" \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 12. Troubleshooting

### 12.1. Common Issues

#### Issue 1: "NO_ELIGIBLE_USERS"
**Nguyên nhân**: Không có user nào thỏa điều kiện
- balance >= 50,000 VND
- Có bank account trong bảng `user_bank_account`

**Giải pháp**:
```sql
-- Check users có balance >= 50000
SELECT id, email, balance
FROM users
WHERE balance >= 50000 AND deleted_at IS NULL;

-- Check users có bank account
SELECT u.id, u.email, uba.account_number
FROM users u
LEFT JOIN user_bank_account uba ON u.id = uba.user_id
WHERE u.balance >= 50000;

-- Nếu thiếu bank account, insert:
INSERT INTO user_bank_account (user_id, account_number, account_name, bank_name)
VALUES (1, '1234567890', 'NGUYEN VAN A', 'VPBANK');
```

#### Issue 2: "BATCH_NOT_FOUND"
**Nguyên nhân**: Batch code không tồn tại

**Giải pháp**:
```sql
-- Check batch tồn tại
SELECT * FROM batch_transfer_export WHERE batch_code = 'BATCH_20251119_001';

-- Hoặc gọi /export để tạo batch mới
```

#### Issue 3: "EMAIL_SEND_FAILED"
**Nguyên nhân**: Email configuration sai hoặc SMTP server lỗi

**Giải pháp**:
```yaml
# Check email config
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # App password, NOT account password
```

**For Gmail**:
- Enable 2-factor authentication
- Generate App Password: https://myaccount.google.com/apppasswords

#### Issue 4: Template file not found
**Log**: `ExcelGenerator: Template not found, creating new workbook`

**Giải pháp**:
```bash
# Verify template exists
ls cashbee-infrastructure/src/main/resources/templates/vpbank_template.xls

# If missing, copy template:
cp /path/to/template.xls cashbee-infrastructure/src/main/resources/templates/vpbank_template.xls
```

#### Issue 5: Scheduler không chạy
**Nguyên nhân**: Scheduler bị disable hoặc cron expression sai

**Giải pháp**:
```yaml
# Enable scheduler
cashbee:
  batch-transfer:
    scheduler:
      enabled: true
      cron: "0 0 8 * * MON"  # Verify cron expression
```

**Test cron expression**: https://crontab.guru/

#### Issue 6: Excel file format lỗi
**Nguyên nhân**: VPBank reject file do format không đúng

**Giải pháp**:
- Verify template file: `vpbank_template.xls` phải là format .xls (không phải .xlsx)
- Check header columns: Phải đúng thứ tự và tên
- Check data format:
  - Account number: Text format (không phải number)
  - Amount: Number format với 2 chữ số thập phân

### 12.2. Debugging Tips

#### Enable Debug Logs
```yaml
logging:
  level:
    com.cashbee.application.service.excel: DEBUG
    com.cashbee.application.service.usecase: DEBUG
    com.cashbee.application.service.email: DEBUG
```

#### Check Database
```sql
-- View recent exports
SELECT * FROM batch_transfer_export
ORDER BY created_at DESC
LIMIT 10;

-- View user bank accounts
SELECT u.email, u.balance, uba.*
FROM users u
JOIN user_bank_account uba ON u.id = uba.user_id
WHERE u.balance >= 50000;
```

#### Test Email Manually
```java
@Autowired
private JavaMailSender mailSender;

@GetMapping("/test-email")
public String testEmail() {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message);
    helper.setTo("test@example.com");
    helper.setSubject("Test");
    helper.setText("Test email");
    mailSender.send(message);
    return "Email sent";
}
```

---

## Tổng kết

Module Batch Transfer được thiết kế theo Clean Architecture với:
- **Separation of Concerns**: Mỗi layer có trách nhiệm riêng
- **Dependency Inversion**: Domain không phụ thuộc vào infrastructure
- **Testability**: Dễ dàng test từng layer
- **Maintainability**: Dễ thêm features hoặc thay đổi implementation

**Key Points**:
✅ File Excel được tạo mới mỗi lần (realtime data)
✅ Chỉ lưu metadata vào database (không lưu file)
✅ Template-based generation đảm bảo format đúng VPBank
✅ Support cả manual và scheduled export
✅ Email delivery với HTML template
✅ Tracking history với pagination

**Next Steps**:
- Thêm unit tests cho các use cases
- Thêm integration tests cho controllers
- Monitor scheduler execution với metrics
- Add retry mechanism cho email sending
