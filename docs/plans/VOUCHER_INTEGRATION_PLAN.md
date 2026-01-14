# Plan: Tích Hợp Mã Giảm Giá từ PolyXGO API

## 1. PHÂN TÍCH API POLYXGO

### 1.1. Endpoint Information

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `https://data.polyxgo.com` |
| **Endpoint** | `/api/v1/datax/shopee_vouchers` |
| **Method** | GET |
| **Rate Limit** | Không có document (cần thận trọng) |
| **Update Frequency** | 30 giây - 60 phút tùy loại data |

### 1.2. Response Structure

```json
{
  "key": "shopee_vouchers",
  "value": "[{...}]",           // JSON string chứa array vouchers
  "group": "shopee",
  "date": "1675671016",         // Unix timestamp
  "datex": "2/6/2023 3:10:15 PM"
}
```

**Lưu ý quan trọng**: Field `value` là **JSON STRING**, không phải JSON object. Cần parse 2 lần.

### 1.3. Voucher Object Fields

| Field | Type | Nullable | Mô tả |
|-------|------|----------|-------|
| `voucher_code` | String | No | Mã voucher (VD: "LANB12F", "FSV-555260997419008") |
| `usage_terms` | String | No | Điều kiện sử dụng (VD: "Giảm ngay 10%...") |
| `discount_percentage` | Integer | No | % giảm giá (0-100) |
| `discount_value` | Integer | Yes | Giá trị giảm cố định (VND) |
| `min_spend` | Integer | No | Đơn tối thiểu (VND) - chia 10^8 |
| `max_value` | Integer | Yes | Giảm tối đa (VND) - chia 10^8 |
| `start_time` | Integer | No | Unix timestamp bắt đầu |
| `end_time` | Integer | No | Unix timestamp kết thúc |
| `shop_id` | Integer | No | ID shop (0 = toàn sàn) |
| `shop_name` | String | Yes | Tên shop |
| `shop_logo` | String | Yes | Hash logo shop |
| `category` | String | - | Category name (từ parent object) |
| `devices` | Array | Yes | ["iOS", "Android"] |
| `payments` | String | Yes | Hình thức thanh toán |
| `logistics` | String | Yes | Đơn vị vận chuyển |
| `usage_limit_per_user` | Int/String | Yes | Giới hạn sử dụng/user |

### 1.4. Các Endpoint Khác Có Sẵn

| Platform | Endpoint |
|----------|----------|
| Shopee | `/api/v1/datax/shopee_vouchers` |
| Lazada | `/api/v1/datax/lazada_vouchers` |
| Tiki | `/api/v1/datax/tiki_vouchers` |
| Sendo | `/api/v1/datax/sendo_vouchers` |

---

## 2. KẾ HOẠCH TÍCH HỢP

### 2.1. Tổng Quan Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                               │
│  ┌──────────────────────┐    ┌──────────────────────────────────────┐   │
│  │ VoucherController    │    │ VoucherSyncScheduler                 │   │
│  │ - GET /vouchers      │    │ - Sync mỗi 30 phút                   │   │
│  │ - GET /vouchers/{id} │    │ - Gọi SyncVouchersFromPolyXGOUseCase │   │
│  └──────────────────────┘    └──────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         APPLICATION LAYER                                │
│  ┌──────────────────────────────┐  ┌────────────────────────────────┐   │
│  │ SyncVouchersFromPolyXGOUseCase│  │ GetActiveVouchersUseCase      │   │
│  │ - Fetch from external API    │  │ - Query active vouchers       │   │
│  │ - Parse response             │  │ - Filter by platform/category │   │
│  │ - Upsert to database         │  └────────────────────────────────┘   │
│  └──────────────────────────────┘                                        │
│  ┌──────────────────────────────┐  ┌────────────────────────────────┐   │
│  │ GetVoucherByCodeUseCase      │  │ CleanExpiredVouchersUseCase   │   │
│  │ - Find voucher by code       │  │ - Delete expired vouchers     │   │
│  └──────────────────────────────┘  └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                   │
│  ┌──────────────────────────────┐  ┌────────────────────────────────┐   │
│  │ Voucher (Domain Model)       │  │ VoucherRepository (Interface)  │   │
│  │ - code, usageTerms           │  │ - save(), findByCode()        │   │
│  │ - discountPercentage/Value   │  │ - findActiveByPlatform()      │   │
│  │ - minSpend, maxValue         │  │ - deleteExpired()             │   │
│  │ - startTime, endTime         │  └────────────────────────────────┘   │
│  │ - platform, category         │                                        │
│  │ - isActive(), isExpired()    │  ┌────────────────────────────────┐   │
│  └──────────────────────────────┘  │ VoucherPlatform (Enum)         │   │
│                                     │ - SHOPEE, LAZADA, TIKI, SENDO  │   │
│                                     └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       INFRASTRUCTURE LAYER                               │
│  ┌──────────────────────────────┐  ┌────────────────────────────────┐   │
│  │ PolyXGOVoucherService        │  │ VoucherRepositoryAdapter       │   │
│  │ (External API Client)        │  │ (implements VoucherRepository) │   │
│  │ - RestTemplate calls         │  └────────────────────────────────┘   │
│  │ - JSON parsing               │                                        │
│  │ - Error handling             │  ┌────────────────────────────────┐   │
│  └──────────────────────────────┘  │ VoucherJpaEntity               │   │
│                                     │ VoucherJpaRepository           │   │
│                                     │ VoucherMapper                  │   │
│                                     └────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2. Files Cần Tạo

#### Domain Layer (`cashbee-domain`)
| File | Mô tả |
|------|-------|
| `model/Voucher.java` | Domain model - pure POJO |
| `enums/VoucherPlatform.java` | Enum: SHOPEE, LAZADA, TIKI, SENDO |
| `repository/VoucherRepository.java` | Repository interface (port) |

#### Application Layer (`cashbee-application`)
| File | Mô tả |
|------|-------|
| `dto/voucher/VoucherResponse.java` | Response DTO |
| `dto/voucher/VoucherMapper.java` | MapStruct mapper Domain ↔ DTO |
| `usecase/voucher/SyncVouchersFromPolyXGOUseCase.java` | Sync từ external API |
| `usecase/voucher/GetActiveVouchersUseCase.java` | Lấy danh sách voucher active |
| `usecase/voucher/GetVoucherByCodeUseCase.java` | Tìm voucher theo code |
| `usecase/voucher/CleanExpiredVouchersUseCase.java` | Xóa voucher hết hạn |

#### Infrastructure Layer (`cashbee-infrastructure`)
| File | Mô tả |
|------|-------|
| `external/polyxgo/PolyXGOVoucherService.java` | RestTemplate client |
| `external/polyxgo/dto/PolyXGOResponse.java` | API response DTO |
| `external/polyxgo/dto/PolyXGOVoucherDto.java` | Voucher từ API |
| `persistence/entity/VoucherJpaEntity.java` | JPA entity |
| `persistence/repository/VoucherJpaRepository.java` | JPA repository |
| `persistence/adapter/VoucherRepositoryAdapter.java` | Adapter implements domain interface |
| `persistence/mapper/VoucherEntityMapper.java` | MapStruct mapper Domain ↔ Entity |

#### Presentation Layer (`cashbee-presentation`)
| File | Mô tả |
|------|-------|
| `controller/VoucherController.java` | REST endpoints |
| `scheduler/VoucherSyncScheduler.java` | Scheduled sync job |
| `resources/db/changelog/046-create-voucher-table.xml` | Liquibase migration |

---

## 3. CHI TIẾT IMPLEMENTATION

### 3.1. Phase 1: Domain Layer

#### 3.1.1. VoucherPlatform Enum
```java
public enum VoucherPlatform {
    SHOPEE("shopee"),
    LAZADA("lazada"),
    TIKI("tiki"),
    SENDO("sendo");

    private final String apiKey;
}
```

#### 3.1.2. Voucher Domain Model
```java
@Getter
@Builder
public class Voucher {
    private Long id;
    private String code;                    // Mã voucher (unique per platform)
    private String usageTerms;              // Điều kiện sử dụng
    private VoucherPlatform platform;       // SHOPEE, LAZADA, etc.
    private String category;                // "Miễn Phí Vận Chuyển", etc.

    // Discount info
    private Integer discountPercentage;     // % giảm (0-100)
    private Long discountValue;             // Giá trị giảm cố định (VND)
    private Long minSpend;                  // Đơn tối thiểu (VND)
    private Long maxValue;                  // Giảm tối đa (VND)

    // Validity
    private Instant startTime;
    private Instant endTime;

    // Shop info (nullable for platform-wide vouchers)
    private Long shopId;
    private String shopName;
    private String shopLogo;

    // Metadata
    private Instant syncedAt;               // Lần sync cuối
    private String externalId;              // promotionid từ API

    // Business methods
    public boolean isActive() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(endTime);
    }

    public boolean isPlatformWide() {
        return shopId == null || shopId == 0;
    }
}
```

#### 3.1.3. VoucherRepository Interface
```java
public interface VoucherRepository {
    Voucher save(Voucher voucher);
    List<Voucher> saveAll(List<Voucher> vouchers);
    Optional<Voucher> findById(Long id);
    Optional<Voucher> findByCodeAndPlatform(String code, VoucherPlatform platform);
    List<Voucher> findActiveByPlatform(VoucherPlatform platform);
    List<Voucher> findActiveByPlatformAndCategory(VoucherPlatform platform, String category);
    int deleteExpiredBefore(Instant cutoff);
    boolean existsByCodeAndPlatform(String code, VoucherPlatform platform);
}
```

### 3.2. Phase 2: Infrastructure Layer

#### 3.2.1. PolyXGO API Client
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PolyXGOVoucherService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cashbee.external.polyxgo.base-url:https://data.polyxgo.com}")
    private String baseUrl;

    public List<PolyXGOVoucherDto> fetchVouchers(VoucherPlatform platform) {
        String endpoint = switch (platform) {
            case SHOPEE -> "/api/v1/datax/shopee_vouchers";
            case LAZADA -> "/api/v1/datax/lazada_vouchers";
            case TIKI -> "/api/v1/datax/tiki_vouchers";
            case SENDO -> "/api/v1/datax/sendo_vouchers";
        };

        try {
            String url = baseUrl + endpoint;
            ResponseEntity<PolyXGOResponse> response =
                restTemplate.getForEntity(url, PolyXGOResponse.class);

            if (response.getBody() != null) {
                // Parse nested JSON string in "value" field
                String valueJson = response.getBody().getValue();
                return parseVouchers(valueJson);
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch vouchers from PolyXGO: {}", platform, e);
        }
        return Collections.emptyList();
    }

    private List<PolyXGOVoucherDto> parseVouchers(String json) {
        // Parse categories → flatten vouchers
    }
}
```

#### 3.2.2. Database Schema (Liquibase)
```xml
<changeSet id="046-create-voucher-table" author="system">
    <createTable tableName="vouchers">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="code" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="platform" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="category" type="VARCHAR(100)"/>
        <column name="usage_terms" type="TEXT"/>
        <column name="discount_percentage" type="INT"/>
        <column name="discount_value" type="BIGINT"/>
        <column name="min_spend" type="BIGINT"/>
        <column name="max_value" type="BIGINT"/>
        <column name="start_time" type="DATETIME"/>
        <column name="end_time" type="DATETIME"/>
        <column name="shop_id" type="BIGINT"/>
        <column name="shop_name" type="VARCHAR(255)"/>
        <column name="shop_logo" type="VARCHAR(255)"/>
        <column name="external_id" type="VARCHAR(100)"/>
        <column name="synced_at" type="DATETIME"/>
        <column name="created_at" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP"/>
        <column name="updated_at" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>

    <addUniqueConstraint
        tableName="vouchers"
        columnNames="code, platform"
        constraintName="uk_voucher_code_platform"/>

    <createIndex tableName="vouchers" indexName="idx_voucher_platform_active">
        <column name="platform"/>
        <column name="end_time"/>
    </createIndex>
</changeSet>
```

### 3.3. Phase 3: Application Layer

#### 3.3.1. SyncVouchersFromPolyXGOUseCase
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncVouchersFromPolyXGOUseCase {
    private final PolyXGOVoucherService polyXGOService;
    private final VoucherRepository voucherRepository;

    @Transactional
    public SyncResult execute(VoucherPlatform platform) {
        log.info("[SYNC] Starting voucher sync for platform: {}", platform);

        // 1. Fetch from external API
        List<PolyXGOVoucherDto> externalVouchers = polyXGOService.fetchVouchers(platform);

        // 2. Convert to domain models
        List<Voucher> vouchers = externalVouchers.stream()
            .map(dto -> mapToDomain(dto, platform))
            .toList();

        // 3. Upsert (update if exists, insert if new)
        int created = 0, updated = 0;
        for (Voucher voucher : vouchers) {
            boolean exists = voucherRepository.existsByCodeAndPlatform(
                voucher.getCode(), platform);
            voucherRepository.save(voucher);
            if (exists) updated++; else created++;
        }

        log.info("[SYNC] Completed: {} created, {} updated", created, updated);
        return new SyncResult(created, updated, vouchers.size());
    }
}
```

### 3.4. Phase 4: Presentation Layer

#### 3.4.1. VoucherController
```java
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    @GetMapping
    public ApiResponse<List<VoucherResponse>> getActiveVouchers(
            @RequestParam(defaultValue = "SHOPEE") VoucherPlatform platform,
            @RequestParam(required = false) String category) {
        // ...
    }

    @GetMapping("/{code}")
    public ApiResponse<VoucherResponse> getVoucherByCode(
            @PathVariable String code,
            @RequestParam(defaultValue = "SHOPEE") VoucherPlatform platform) {
        // ...
    }

    @PostMapping("/sync")
    public ApiResponse<SyncResult> triggerSync(
            @RequestParam(defaultValue = "SHOPEE") VoucherPlatform platform) {
        // Manual trigger for admin
    }
}
```

#### 3.4.2. VoucherSyncScheduler
```java
@Component
@ConditionalOnProperty(
    prefix = "cashbee.voucher.sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class VoucherSyncScheduler {

    @Scheduled(cron = "${cashbee.voucher.sync.cron:0 */30 * * * *}")  // Every 30 mins
    public void syncAllPlatforms() {
        log.info("[SCHEDULER] Starting voucher sync for all platforms");

        for (VoucherPlatform platform : VoucherPlatform.values()) {
            try {
                syncUseCase.execute(platform);
            } catch (Exception e) {
                log.error("[SCHEDULER] Failed to sync {}", platform, e);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM
    public void cleanExpiredVouchers() {
        cleanUseCase.execute();
    }
}
```

---

## 4. CONFIGURATION

### 4.1. application.yml
```yaml
cashbee:
  external:
    polyxgo:
      base-url: https://data.polyxgo.com
      enabled: true

  voucher:
    sync:
      enabled: true
      cron: "0 */30 * * * *"    # Every 30 minutes
    cleanup:
      enabled: true
      retention-days: 7          # Keep expired vouchers for 7 days
```

---

## 5. THỨ TỰ TRIỂN KHAI

### Phase 1: Domain Layer (Ước tính: 3 files)
1. [ ] Tạo `VoucherPlatform.java` enum
2. [ ] Tạo `Voucher.java` domain model
3. [ ] Tạo `VoucherRepository.java` interface

### Phase 2: Infrastructure - Database (Ước tính: 5 files)
4. [ ] Tạo Liquibase migration `046-create-voucher-table.xml`
5. [ ] Cập nhật `db.changelog-master.xml`
6. [ ] Tạo `VoucherJpaEntity.java`
7. [ ] Tạo `VoucherJpaRepository.java`
8. [ ] Tạo `VoucherEntityMapper.java` (MapStruct)
9. [ ] Tạo `VoucherRepositoryAdapter.java`

### Phase 3: Infrastructure - External API (Ước tính: 3 files)
10. [ ] Tạo `PolyXGOResponse.java` DTO
11. [ ] Tạo `PolyXGOVoucherDto.java` DTO
12. [ ] Tạo `PolyXGOVoucherService.java` (RestTemplate client)

### Phase 4: Application Layer (Ước tính: 5 files)
13. [ ] Tạo `VoucherResponse.java` DTO
14. [ ] Tạo `VoucherMapper.java` (Domain ↔ Response DTO)
15. [ ] Tạo `SyncVouchersFromPolyXGOUseCase.java`
16. [ ] Tạo `GetActiveVouchersUseCase.java`
17. [ ] Tạo `CleanExpiredVouchersUseCase.java`

### Phase 5: Presentation Layer (Ước tính: 3 files)
18. [ ] Tạo `VoucherController.java`
19. [ ] Tạo `VoucherSyncScheduler.java`
20. [ ] Cập nhật `application.yml` với config mới

### Phase 6: Testing
21. [ ] Unit tests cho use cases
22. [ ] Integration test với mock API

---

## 6. API ENDPOINTS

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/vouchers` | Lấy danh sách voucher active |
| GET | `/api/vouchers?platform=SHOPEE` | Filter theo platform |
| GET | `/api/vouchers?platform=SHOPEE&category=Miễn Phí Vận Chuyển` | Filter theo category |
| GET | `/api/vouchers/{code}?platform=SHOPEE` | Tìm voucher theo code |
| POST | `/api/vouchers/sync?platform=SHOPEE` | Manual trigger sync (admin) |

---

## 7. LƯU Ý QUAN TRỌNG

### 7.1. Data Parsing
- Field `value` trong response là **JSON STRING**, cần parse 2 lần
- `min_spend` và `max_value` từ API cần **chia cho 10^8** để ra VND thực
- `start_time` và `end_time` là **Unix timestamp** (seconds)

### 7.2. Error Handling
- Nếu API fail, log error và tiếp tục (không throw exception)
- Scheduler không được crash toàn bộ nếu 1 platform fail

### 7.3. Performance
- Sử dụng batch insert/update thay vì từng record
- Index trên `(platform, end_time)` để query nhanh
- Unique constraint `(code, platform)` để tránh duplicate

### 7.4. Extensibility
- Design để dễ thêm platform mới (chỉ cần thêm enum value và endpoint)
- Có thể thêm AccessTrade API sau này với cùng architecture

---

## 8. TỔNG KẾT

| Metric | Giá trị |
|--------|---------|
| **Tổng số files mới** | ~20 files |
| **Layers affected** | All 4 layers |
| **Database changes** | 1 table mới |
| **Scheduled jobs** | 2 (sync + cleanup) |
| **REST endpoints** | 3 endpoints |
