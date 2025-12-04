# Plan: Tối ưu Keycloak Policy Enforcer

## Vấn đề hiện tại

Mỗi API call hiện tại gây ra 2 lần query đến Keycloak server:
```
o.k.a.authorization.PathConfigMatcher : No path provided in configuration.
o.k.a.authorization.PathConfigMatcher : Querying the server for all resources associated with this application.
```

### Nguyên nhân gốc
1. `ConfigurationResolver.resolve()` được gọi mỗi request
2. `ServletPolicyEnforcerFilter` tạo mới `PolicyEnforcer` instance mỗi lần
3. Mỗi `PolicyEnforcer` mới phải query Keycloak để lấy resources

### Tác động
- **Latency**: +50-200ms mỗi request (round-trip đến Keycloak)
- **Keycloak load**: N requests → 2N queries đến Keycloak
- **Scalability**: Keycloak trở thành bottleneck

---

## Giải pháp đề xuất

### Option A: Cache PolicyEnforcerConfig (Đã implement - Không đủ)

```java
private volatile PolicyEnforcerConfig cachedPolicyEnforcerConfig;
```

**Vấn đề**: Chỉ cache config, nhưng `ServletPolicyEnforcerFilter` vẫn tạo mới `PolicyEnforcer` mỗi request.

---

### Option B: Sử dụng lazy-load-paths (Recommended)

Thêm cấu hình trong `policy-enforcer-dev.json`:

```json
{
  "lazy-load-paths": false,
  "path-cache": {
    "max-entries": 1000,
    "lifespan": 30000
  }
}
```

**Giải thích**:
- `lazy-load-paths: false` → Load TẤT CẢ paths khi startup, không query lại
- `path-cache` → Cache kết quả authorization decisions

**Ưu điểm**:
- Cấu hình đơn giản, không cần thay đổi code
- Keycloak built-in feature, đã được test kỹ

**Nhược điểm**:
- Startup time tăng (load tất cả paths)
- Cần restart khi thay đổi permissions trong Keycloak

---

### Option C: Disable Policy Enforcer, dùng Spring Security thuần (Alternative)

Nếu không cần fine-grained authorization từ Keycloak, có thể disable Policy Enforcer và dùng Spring Security với roles từ JWT.

```java
// Thay vì PolicyEnforcer, dùng role-based authorization
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
```

**Ưu điểm**:
- Không phụ thuộc Keycloak runtime
- Performance tốt nhất (chỉ validate JWT)
- Đơn giản, dễ debug

**Nhược điểm**:
- Mất khả năng dynamic policy từ Keycloak
- Phải restart app khi thay đổi permissions

---

### Option D: Custom PolicyEnforcer với caching (Most Robust)

Tạo custom filter với caching đầy đủ:

```java
@Component
public class CachedPolicyEnforcerFilter extends OncePerRequestFilter {

    private final PolicyEnforcer policyEnforcer;  // Singleton

    public CachedPolicyEnforcerFilter(PolicyEnforcerConfig config) {
        // Tạo PolicyEnforcer MỘT LẦN khi startup
        this.policyEnforcer = PolicyEnforcer.builder()
            .authServerUrl(config.getAuthServerUrl())
            .realm(config.getRealm())
            .clientId(config.getResource())
            .credentials(config.getCredentials())
            .enforcerConfig(config)
            .build();
    }

    @Override
    protected void doFilterInternal(...) {
        // Sử dụng singleton policyEnforcer
    }
}
```

**Ưu điểm**:
- Control hoàn toàn lifecycle
- Cache cả PolicyEnforcer, không chỉ config
- Có thể thêm custom caching logic

**Nhược điểm**:
- Cần viết nhiều code hơn
- Phải tự handle edge cases

---

## Đề xuất Implementation

### Phase 1: Quick Fix (Option B) - 30 phút

1. Update `policy-enforcer-dev.json`:
```json
{
  "auth-server-url": "https://auth.nguocchieuvangle.io.vn/",
  "realm": "cashbee",
  "resource": "cashbee-backend",
  "credentials": {
    "secret": "..."
  },
  "enforcement-mode": "PERMISSIVE",
  "http-method-as-scope": true,
  "lazy-load-paths": false,
  "path-cache": {
    "max-entries": 1000,
    "lifespan": 300000
  },
  "paths": [...]
}
```

2. Giữ nguyên cache config trong `SecurityConfig.java` (đã implement)

3. Test và verify không còn query mỗi request

### Phase 2: Long-term (Option C hoặc D) - Tùy yêu cầu

Nếu không cần dynamic authorization từ Keycloak:
- Chuyển sang Spring Security thuần (Option C)
- Định nghĩa roles trong code, không phụ thuộc Keycloak runtime

---

## So sánh các giải pháp

| Tiêu chí | Option A (Current) | Option B (Recommended) | Option C | Option D |
|----------|-------------------|----------------------|----------|----------|
| Effort | Đã done | Thấp | Trung bình | Cao |
| Performance | Chưa tối ưu | Tốt | Tốt nhất | Tốt |
| Maintainability | Trung bình | Tốt | Tốt | Phức tạp |
| Dynamic policies | Có | Có (cached) | Không | Có |
| Keycloak dependency | Runtime | Runtime (startup) | Chỉ JWT | Runtime |

---

## Recommended Action

**Implement Option B** vì:
1. Ít thay đổi nhất
2. Sử dụng Keycloak built-in feature
3. Giải quyết được vấn đề performance
4. Vẫn giữ được dynamic authorization

---

## Files cần thay đổi

1. `cashbee-presentation/src/main/resources/policy-enforcer-dev.json`
   - Thêm `lazy-load-paths` và `path-cache`

2. `cashbee-presentation/src/main/java/com/cashbee/presentation/config/SecurityConfig.java`
   - Giữ nguyên cache config (đã implement)

---

## Testing Checklist

- [ ] Restart app và verify chỉ có 1-2 log "Querying server" lúc startup
- [ ] Call nhiều API và verify KHÔNG có thêm log "No path provided"
- [ ] Đo latency trước/sau để confirm improvement
- [ ] Test permission vẫn hoạt động đúng
