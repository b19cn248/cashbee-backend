# TikTok Shop Integration Guide

> **Document Version:** 1.0
> **Created:** 2025-12-26
> **Author:** CashBee Team

## Muc luc

1. [Tong Quan](#1-tong-quan)
2. [TikTok Shop Affiliate API](#2-tiktok-shop-affiliate-api)
3. [So Sanh Voi Shopee](#3-so-sanh-voi-shopee)
4. [Kien Truc Tich Hop](#4-kien-truc-tich-hop)
5. [Cac Buoc Tich Hop Chi Tiet](#5-cac-buoc-tich-hop-chi-tiet)
6. [Luong Di (Flow Diagrams)](#6-luong-di-flow-diagrams)
7. [Database Schema](#7-database-schema)
8. [API Endpoints](#8-api-endpoints)
9. [Code Implementation](#9-code-implementation)
10. [Testing](#10-testing)
11. [Checklist Trien Khai](#11-checklist-trien-khai)

---

## 1. Tong Quan

### 1.1 TikTok Shop la gi?

TikTok Shop la nen tang thuong mai dien tu tich hop truc tiep trong ung dung TikTok, cho phep nguoi dung mua sam san pham truc tiep tu video va livestream.

### 1.2 TikTok Shop Affiliate Program

- **Creators/Publishers** quang ba san pham thong qua video, livestream
- **Tracking** duoc thuc hien qua affiliate links
- **Commission** duoc tra sau khi don hang hoan thanh (thong thuong 14-30 ngay)

### 1.3 Tai sao can tich hop?

| Loi ich | Mo ta |
|---------|-------|
| Mo rong nguon thu | TikTok co 1.4 ty nguoi dung toan cau |
| Da dang hoa platform | Khong phu thuoc hoan toan vao Shopee |
| Xu huong Social Commerce | TikTok Shop dang tang truong manh |

---

## 2. TikTok Shop Affiliate API

### 2.1 Cac Loai API

Theo tai lieu chinh thuc tu [TikTok for Developers](https://developers.tiktok.com/blog/2024-tiktok-shop-affiliate-apis-launch-developer-opportunity):

| API Category | Chuc nang |
|--------------|-----------|
| **Collaboration APIs** | Tao va quan ly affiliate campaigns |
| **Search & Discovery APIs** | Tim kiem creators, san pham |
| **Performance Tracking APIs** | Tao affiliate links, lay thong tin don hang |
| **Order APIs** | Truy xuat thong tin don hang affiliate |

### 2.2 Dang ky Developer

1. Truy cap [TikTok Shop Partner Center](https://partner.tiktokshop.com)
2. Dang ky tai khoan Seller/Partner
3. Tao App trong Developer Console
4. Nhan App Key, App Secret, Service ID

### 2.3 Authentication

TikTok Shop su dung OAuth 2.0:

```
Authorization: Bearer {access_token}
```

**Token Flow:**
```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   CashBee    │───>│  TikTok Auth │───>│ Access Token │
│   Backend    │<───│   Server     │<───│   + Refresh  │
└──────────────┘    └──────────────┘    └──────────────┘
```

### 2.4 Affiliate Link Format (Du kien)

Dua tren nghien cuu tu [TikTok Ads Manager](https://ads.tiktok.com/help/article/list-of-supported-deeplink-formats):

```
# Deeplink format
https://www.tiktok.com/view/product/{product_id}?affiliate_id={affiliate_id}&sub_id={tracking_code}

# hoac Universal Link
https://vm.tiktok.com/{short_code}?affiliate_id={affiliate_id}&sub_id={tracking_code}
```

---

## 3. So Sanh Voi Shopee

### 3.1 Feature Comparison

| Feature | Shopee | TikTok Shop |
|---------|--------|-------------|
| Affiliate Link Format | `s.shopee.vn/an_redir?origin_link=...` | `vm.tiktok.com/...` hoac deeplink |
| Tracking Parameter | `sub_id` | `sub_id` hoac `tracking_id` |
| Authentication | Khong can (public affiliate) | OAuth 2.0 (API access) |
| Order Import | CSV manual | API + Webhook |
| Commission Tracking | CSV import | API real-time |
| Order Confirmation | 7-14 ngay | 14-30 ngay |

### 3.2 Code Reuse Analysis

| Component | Reusable? | Notes |
|-----------|-----------|-------|
| `AffiliatePlatform` model | Co | Them TikTok platform |
| `AffiliateClick` model | Co | Khong can thay doi |
| `AffiliateOrder` model | Co | Them tiktok-specific fields |
| `CreateTrackingLinkUseCase` | Co | Them TikTok builder |
| URL Parser | Khong | Can tao `TikTokUrlParser` moi |
| Link Builder | Khong | Can tao `TikTokAffiliateLinkBuilder` |
| CSV Parser | Khong | Can tao parser moi hoac dung API |

---

## 4. Kien Truc Tich Hop

### 4.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
├─────────────────────────────────────────────────────────────────────┤
│  AffiliateTrackingController (existing)                             │
│  └── POST /api/affiliate/tracking/create-link                       │
│  └── Supports: platformCode = "tiktok"                              │
│                                                                     │
│  TikTokWebhookController (NEW)                                      │
│  └── POST /api/webhooks/tiktok/order                                │
│  └── Receives order notifications from TikTok                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        APPLICATION LAYER                            │
├─────────────────────────────────────────────────────────────────────┤
│  CreateTrackingLinkUseCase (existing - add TikTok support)          │
│  │                                                                  │
│  └──> TikTokAffiliateLinkBuilder (NEW)                              │
│  └──> TikTokUrlParser (NEW)                                         │
│                                                                     │
│  ImportTikTokOrdersUseCase (NEW)                                    │
│  │                                                                  │
│  └──> TikTokOrderService (NEW)                                      │
│                                                                     │
│  SyncTikTokOrdersUseCase (NEW - scheduled job)                      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  AffiliatePlatform (existing)                                       │
│  └── code = "tiktok"                                                │
│  └── affiliateId = "{tiktok_affiliate_id}"                          │
│                                                                     │
│  AffiliateOrder (existing - extend)                                 │
│  └── platformOrderId (for TikTok order ID)                          │
│                                                                     │
│  TikTokOrderService (NEW - interface)                               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      INFRASTRUCTURE LAYER                           │
├─────────────────────────────────────────────────────────────────────┤
│  TikTokApiClient (NEW)                                              │
│  └── Authentication handling                                        │
│  └── API calls to TikTok Shop                                       │
│                                                                     │
│  TikTokOrderServiceAdapter (NEW)                                    │
│  └── Implements domain interface                                    │
│  └── Uses TikTokApiClient                                           │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Package Structure

```
cashbee-backend/
├── cashbee-application/
│   └── src/main/java/com/cashbee/application/
│       ├── usecase/
│       │   └── affiliate/
│       │       ├── CreateTrackingLinkUseCase.java      (MODIFY)
│       │       ├── ImportTikTokOrdersUseCase.java      (NEW)
│       │       └── SyncTikTokOrdersUseCase.java        (NEW)
│       ├── util/
│       │   └── affiliate/
│       │       ├── TikTokUrlParser.java                (NEW)
│       │       └── TikTokAffiliateLinkBuilder.java     (NEW)
│       └── dto/
│           └── tiktok/
│               ├── TikTokOrderResponse.java            (NEW)
│               └── TikTokWebhookPayload.java           (NEW)
│
├── cashbee-domain/
│   └── src/main/java/com/cashbee/domain/
│       └── service/
│           └── TikTokOrderService.java                 (NEW - interface)
│
├── cashbee-infrastructure/
│   └── src/main/java/com/cashbee/infrastructure/
│       └── external/
│           └── tiktok/
│               ├── TikTokApiClient.java                (NEW)
│               ├── TikTokApiConfig.java                (NEW)
│               ├── TikTokOrderServiceAdapter.java      (NEW)
│               └── dto/
│                   ├── TikTokApiResponse.java          (NEW)
│                   └── TikTokOrderDto.java             (NEW)
│
└── cashbee-presentation/
    └── src/main/java/com/cashbee/presentation/
        └── controller/
            └── TikTokWebhookController.java            (NEW)
```

---

## 5. Cac Buoc Tich Hop Chi Tiet

### Phase 1: Setup & Configuration (1-2 days)

#### Step 1.1: Dang ky TikTok Shop Partner

1. Truy cap https://partner.tiktokshop.com
2. Dang ky tai khoan Seller/Partner cho Viet Nam
3. Tao Application trong Developer Console
4. Luu lai:
   - `App Key` (tuong duong Client ID)
   - `App Secret` (tuong duong Client Secret)
   - `Service ID`

#### Step 1.2: Them Platform vao Database

```sql
-- Liquibase changelog: XXX-add-tiktok-platform.xml
INSERT INTO affiliate_platform (
    name,
    code,
    affiliate_id,
    api_key,
    api_secret,
    base_url,
    link_template,
    tracking_enabled,
    default_commission_rate,
    status,
    created_at
) VALUES (
    'TikTok Shop',
    'tiktok',
    '{YOUR_TIKTOK_AFFILIATE_ID}',
    '{APP_KEY}',
    '{APP_SECRET}',
    'https://open-api.tiktokglobalshop.com',
    NULL,  -- TikTok uses custom builder, not template
    TRUE,
    5.00,  -- Default 5% commission
    'ACTIVE',
    NOW()
);
```

#### Step 1.3: Application Configuration

```yaml
# application.yml
tiktok:
  shop:
    enabled: true
    app-key: ${TIKTOK_APP_KEY}
    app-secret: ${TIKTOK_APP_SECRET}
    service-id: ${TIKTOK_SERVICE_ID}
    base-url: https://open-api.tiktokglobalshop.com
    affiliate-id: ${TIKTOK_AFFILIATE_ID}
    webhook-secret: ${TIKTOK_WEBHOOK_SECRET}
```

---

### Phase 2: URL Parser & Link Builder (2-3 days)

#### Step 2.1: TikTok URL Parser

```java
// TikTokUrlParser.java
@Component
@Slf4j
public class TikTokUrlParser {

    // Pattern 1: Product page
    // https://www.tiktok.com/@shop/product/{product_id}
    // https://www.tiktok.com/view/product/{product_id}
    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
        "https?://(?:www\\.)?tiktok\\.com/(?:@[^/]+/product|view/product)/(\\d+)"
    );

    // Pattern 2: Short link
    // https://vm.tiktok.com/{short_code}
    // https://vt.tiktok.com/{short_code}
    private static final Pattern SHORT_LINK_PATTERN = Pattern.compile(
        "https?://(?:vm|vt)\\.tiktok\\.com/([A-Za-z0-9]+)"
    );

    // Pattern 3: TikTok Shop direct link
    // https://shop.tiktok.com/view/product/{product_id}
    private static final Pattern SHOP_PATTERN = Pattern.compile(
        "https?://shop\\.tiktok\\.com/view/product/(\\d+)"
    );

    public ParsedTikTokUrl parse(String url) {
        // Implementation...
    }

    @Data
    @Builder
    public static class ParsedTikTokUrl {
        private String originalUrl;
        private String expandedUrl;
        private String productId;
        private String shopId;
        private String format;  // "product", "short", "shop"
    }
}
```

#### Step 2.2: TikTok Affiliate Link Builder

```java
// TikTokAffiliateLinkBuilder.java
@Component
@Slf4j
public class TikTokAffiliateLinkBuilder {

    // TikTok affiliate redirect base (vi du - can xac nhan tu TikTok docs)
    private static final String TIKTOK_AFFILIATE_BASE = "https://affiliate.tiktok.com/r";

    /**
     * Build TikTok affiliate link.
     *
     * Format du kien:
     * https://affiliate.tiktok.com/r?product_id={id}&affiliate_id={id}&sub_id={tracking}
     *
     * hoac deeplink:
     * https://vm.tiktok.com/{code}?affiliate_id={id}&sub_id={tracking}
     */
    public String build(String originalUrl, String affiliateId, String trackingCode) {
        validateParameters(originalUrl, affiliateId, trackingCode);

        // URL encode
        String encodedUrl = urlEncode(originalUrl);

        return String.format(
            "%s?url=%s&affiliate_id=%s&sub_id=%s",
            TIKTOK_AFFILIATE_BASE,
            encodedUrl,
            affiliateId,
            trackingCode
        );
    }

    /**
     * Build with advanced tracking (5 sub_id values).
     */
    public String buildWithAdvancedTracking(
        String originalUrl,
        String affiliateId,
        String subId1,  // Tracking code
        String subId2,  // User ID
        String subId3,  // Campaign
        String subId4,  // Source
        String subId5   // Custom
    ) {
        // Similar to Shopee format
        String subId = String.format("%s-%s-%s-%s-%s",
            subId1, subId2, subId3, subId4, subId5);

        return build(originalUrl, affiliateId, subId);
    }
}
```

---

### Phase 3: Modify CreateTrackingLinkUseCase (1 day)

#### Step 3.1: Update Use Case

```java
// CreateTrackingLinkUseCase.java - MODIFIED
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTrackingLinkUseCase {

    // Existing dependencies...
    private final ShopeeAffiliateLinkBuilder shopeeAffiliateLinkBuilder;
    private final ShopeeFoodAffiliateLinkBuilder shopeeFoodAffiliateLinkBuilder;

    // NEW dependencies
    private final TikTokAffiliateLinkBuilder tikTokAffiliateLinkBuilder;
    private final TikTokUrlParser tikTokUrlParser;

    @Transactional
    public TrackingLinkResponse execute(CreateTrackingLinkRequest request, Long userId) {
        String platformCode = detectPlatform(request.getUrl());

        if ("tiktok".equalsIgnoreCase(platformCode)) {
            return createTikTokTrackingLink(request, userId);
        } else {
            return createShopeeTrackingLink(request, userId);
        }
    }

    private String detectPlatform(String url) {
        if (url.contains("tiktok.com") || url.contains("vm.tiktok.com")) {
            return "tiktok";
        } else if (url.contains("shopeefood")) {
            return "shopeefood";
        } else {
            return "shopee";
        }
    }

    private TrackingLinkResponse createTikTokTrackingLink(
        CreateTrackingLinkRequest request,
        Long userId
    ) {
        // 1. Parse TikTok URL
        TikTokUrlParser.ParsedTikTokUrl parsedUrl = tikTokUrlParser.parse(request.getUrl());

        // 2. Get platform config
        AffiliatePlatform platform = platformRepository.findByCode("tiktok")
            .orElseThrow(() -> new NotFoundException("TikTok platform not configured"));

        // 3. Generate tracking code (reuse existing generator)
        String trackingCode = trackingCodeGenerator.generate(userId, clickId);

        // 4. Build affiliate link
        String trackingUrl = tikTokAffiliateLinkBuilder.build(
            parsedUrl.getOriginalUrl(),
            platform.getAffiliateId(),
            trackingCode
        );

        // 5. Save and return (same as Shopee flow)
        // ...
    }
}
```

---

### Phase 4: TikTok API Client (3-4 days)

#### Step 4.1: API Client Implementation

```java
// TikTokApiClient.java
@Component
@Slf4j
public class TikTokApiClient {

    private final RestTemplate restTemplate;
    private final TikTokApiConfig config;

    // Token cache
    private String accessToken;
    private LocalDateTime tokenExpiry;

    /**
     * Get affiliate orders from TikTok.
     */
    public List<TikTokOrderDto> getAffiliateOrders(
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        ensureValidToken();

        String url = config.getBaseUrl() + "/api/affiliate/orders/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("x-tts-access-token", accessToken);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("start_time", startDate.toEpochSecond(ZoneOffset.UTC));
        body.put("end_time", endDate.toEpochSecond(ZoneOffset.UTC));
        body.put("page_size", 100);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TikTokApiResponse<List<TikTokOrderDto>>> response =
            restTemplate.exchange(url, HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {});

        return response.getBody().getData();
    }

    /**
     * Refresh access token.
     */
    private void refreshToken() {
        String url = "https://auth.tiktok-shops.com/api/v2/token/get";

        Map<String, String> body = new HashMap<>();
        body.put("app_key", config.getAppKey());
        body.put("app_secret", config.getAppSecret());
        body.put("grant_type", "authorized_code");

        // Call token endpoint...
        // Parse response and save token
    }
}
```

#### Step 4.2: Scheduled Order Sync

```java
// SyncTikTokOrdersUseCase.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncTikTokOrdersUseCase {

    private final TikTokApiClient tikTokApiClient;
    private final AffiliateOrderRepository orderRepository;
    private final AffiliateClickRepository clickRepository;
    private final TrackingCodeGenerator trackingCodeGenerator;

    /**
     * Sync orders from TikTok API.
     * Scheduled to run every hour.
     */
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    @Transactional
    public void syncOrders() {
        log.info("Starting TikTok order sync...");

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);  // Last 7 days

        List<TikTokOrderDto> orders = tikTokApiClient.getAffiliateOrders(startDate, endDate);

        for (TikTokOrderDto order : orders) {
            processOrder(order);
        }

        log.info("TikTok order sync completed. Processed {} orders", orders.size());
    }

    private void processOrder(TikTokOrderDto order) {
        // 1. Check if order already exists
        if (orderRepository.existsByPlatformOrderId(order.getOrderId())) {
            return;
        }

        // 2. Extract tracking code from sub_id
        String trackingCode = order.getSubId();

        // 3. Find matching click
        AffiliateClick click = clickRepository.findByTrackingCode(trackingCode)
            .orElse(null);

        // 4. Create order record
        AffiliateOrder affiliateOrder = AffiliateOrder.builder()
            .platformOrderId(order.getOrderId())
            .userId(click != null ? click.getUserId() : null)
            .clickId(click != null ? click.getId() : null)
            .platformId(getPlatformId("tiktok"))
            .orderAmount(order.getTotalAmount())
            .commissionAmount(order.getCommission())
            .status(mapOrderStatus(order.getStatus()))
            .orderDate(order.getCreateTime())
            .build();

        orderRepository.save(affiliateOrder);
    }
}
```

---

### Phase 5: Webhook Integration (2 days)

#### Step 5.1: Webhook Controller

```java
// TikTokWebhookController.java
@RestController
@RequestMapping("/api/webhooks/tiktok")
@RequiredArgsConstructor
@Slf4j
public class TikTokWebhookController {

    private final ImportTikTokOrdersUseCase importOrdersUseCase;
    private final TikTokWebhookValidator webhookValidator;

    /**
     * Receive order notifications from TikTok.
     *
     * TikTok sends webhooks when:
     * - Order created
     * - Order paid
     * - Order shipped
     * - Order completed
     * - Order cancelled
     */
    @PostMapping("/order")
    public ResponseEntity<String> handleOrderWebhook(
        @RequestBody String payload,
        @RequestHeader("x-tts-signature") String signature,
        @RequestHeader("x-tts-timestamp") String timestamp
    ) {
        // 1. Validate webhook signature
        if (!webhookValidator.isValid(payload, signature, timestamp)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 2. Parse payload
        TikTokWebhookPayload webhookPayload = parsePayload(payload);

        // 3. Process based on event type
        switch (webhookPayload.getEventType()) {
            case "ORDER_COMPLETED":
                importOrdersUseCase.processCompletedOrder(webhookPayload);
                break;
            case "ORDER_CANCELLED":
                importOrdersUseCase.processCancelledOrder(webhookPayload);
                break;
            default:
                log.info("Ignoring event type: {}", webhookPayload.getEventType());
        }

        // 4. Return success (TikTok expects 200 OK)
        return ResponseEntity.ok("OK");
    }
}
```

---

## 6. Luong Di (Flow Diagrams)

### 6.1 Tao Tracking Link Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USER TAO TRACKING LINK                              │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │    User     │
                              └──────┬──────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
        ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
        │  Copy Shopee  │  │  Copy TikTok  │  │ Copy Lazada   │
        │  Product URL  │  │  Product URL  │  │ Product URL   │
        └───────┬───────┘  └───────┬───────┘  └───────────────┘
                │                  │
                │                  │
                ▼                  ▼
        ┌──────────────────────────────────────────────┐
        │         POST /api/affiliate/tracking/         │
        │              create-link                      │
        │  Body: { "url": "https://..." }              │
        │  Header: Authorization: Bearer {JWT}          │
        └──────────────────┬───────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────────────┐
        │        CreateTrackingLinkUseCase             │
        │  1. Detect platform from URL                 │
        │  2. Route to appropriate handler             │
        └──────────────────┬───────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
           ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│   SHOPEE HANDLER    │         │   TIKTOK HANDLER    │
├─────────────────────┤         ├─────────────────────┤
│ 1. ShopeeUrlParser  │         │ 1. TikTokUrlParser  │
│    parse(url)       │         │    parse(url)       │
│                     │         │                     │
│ 2. Get platform     │         │ 2. Get platform     │
│    config (shopee)  │         │    config (tiktok)  │
│                     │         │                     │
│ 3. Generate         │         │ 3. Generate         │
│    tracking code    │         │    tracking code    │
│    CB{user}_{id}_ts │         │    CB{user}_{id}_ts │
│                     │         │                     │
│ 4. ShopeeAffiliate  │         │ 4. TikTokAffiliate  │
│    LinkBuilder      │         │    LinkBuilder      │
│    .build()         │         │    .build()         │
│                     │         │                     │
│ 5. Save AffiliateClick        │ 5. Save AffiliateClick
└─────────┬───────────┘         └─────────┬───────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────────────────────────────────┐
│                 RESPONSE                             │
├─────────────────────────────────────────────────────┤
│ {                                                   │
│   "trackingUrl": "https://s.shopee.vn/an_redir...", │
│   "trackingCode": "CB1_100_20251226...",            │
│   "platformCode": "shopee",                         │
│   ...                                               │
│ }                                                   │
│                        OR                           │
│ {                                                   │
│   "trackingUrl": "https://affiliate.tiktok.com/...",│
│   "trackingCode": "CB1_101_20251226...",            │
│   "platformCode": "tiktok",                         │
│   ...                                               │
│ }                                                   │
└─────────────────────────────────────────────────────┘
```

### 6.2 Order Tracking Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DON HANG DUOC TAO & TRACKING                        │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────┐   Click link    ┌────────────┐   Redirect    ┌────────────┐
│    User    │ ───────────────>│  CashBee   │ ────────────> │  Platform  │
│            │                 │  Backend   │               │ (TikTok/   │
│            │                 │            │               │  Shopee)   │
└────────────┘                 └────────────┘               └─────┬──────┘
                                     │                            │
                                     │ Record click               │ User mua hang
                                     │ (status: CLICKED)          │
                                     ▼                            ▼
                              ┌────────────┐               ┌────────────┐
                              │ affiliate_ │               │  Platform  │
                              │   click    │               │   Order    │
                              │   table    │               │   System   │
                              └────────────┘               └─────┬──────┘
                                                                 │
                          ┌──────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│      SHOPEE         │         │      TIKTOK         │
├─────────────────────┤         ├─────────────────────┤
│ Manual CSV Import   │         │ Option A: API Sync  │
│                     │         │ - Scheduled job     │
│ Admin uploads CSV   │         │ - Every hour        │
│ from Shopee partner │         │                     │
│ center              │         │ Option B: Webhook   │
│                     │         │ - Real-time         │
│ CSV contains:       │         │ - ORDER_COMPLETED   │
│ - order_id          │         │   event             │
│ - sub_id (tracking) │         │                     │
│ - commission        │         │ Payload contains:   │
│ - status            │         │ - order_id          │
└─────────┬───────────┘         │ - sub_id (tracking) │
          │                     │ - commission        │
          │                     └─────────┬───────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────────────────────────────────┐
│              MATCHING LOGIC                          │
├─────────────────────────────────────────────────────┤
│ 1. Extract tracking_code from sub_id                │
│    sub_id = "CB1_100_20251226143000"                │
│                                                     │
│ 2. Parse tracking code:                             │
│    - userId = 1                                     │
│    - clickId = 100                                  │
│                                                     │
│ 3. Find AffiliateClick by tracking_code             │
│                                                     │
│ 4. Create/Update AffiliateOrder                     │
│    - Link to userId                                 │
│    - Link to clickId                                │
│    - Store commission amount                        │
│                                                     │
│ 5. Update user's pending balance                    │
│    wallet.addPendingBalance(commission * rate)      │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│              CASHBACK CONFIRMATION                   │
├─────────────────────────────────────────────────────┤
│ After confirmation period (14-30 days):             │
│                                                     │
│ 1. Order status → CONFIRMED                         │
│                                                     │
│ 2. Move pending → available balance                 │
│    wallet.confirmPendingBalance(amount)             │
│                                                     │
│ 3. Notify user                                      │
│    "Ban da nhan duoc {amount} cashback!"            │
└─────────────────────────────────────────────────────┘
```

### 6.3 Complete Integration Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE TIKTOK INTEGRATION FLOW                         │
└─────────────────────────────────────────────────────────────────────────────┘

   ┌──────────┐                                               ┌──────────────┐
   │   User   │                                               │ TikTok Shop  │
   └────┬─────┘                                               │    Server    │
        │                                                     └───────┬──────┘
        │  1. Copy TikTok product URL                                 │
        │     https://www.tiktok.com/@shop/product/123               │
        ▼                                                             │
   ┌────────────────────────────────────────┐                         │
   │         CashBee Mobile App             │                         │
   │  ┌──────────────────────────────────┐  │                         │
   │  │  Paste URL here:                 │  │                         │
   │  │  [https://www.tiktok.com/...]    │  │                         │
   │  │                                  │  │                         │
   │  │  [Tao Link Cashback]             │  │                         │
   │  └──────────────────────────────────┘  │                         │
   └────────────────┬───────────────────────┘                         │
                    │                                                 │
                    │  2. POST /api/affiliate/tracking/create-link    │
                    ▼                                                 │
   ┌──────────────────────────────────────────────────────────────┐   │
   │                    CashBee Backend                            │   │
   │  ┌────────────────────────────────────────────────────────┐  │   │
   │  │  CreateTrackingLinkUseCase                             │  │   │
   │  │                                                        │  │   │
   │  │  1. Detect: URL contains "tiktok.com"                  │  │   │
   │  │     → platformCode = "tiktok"                          │  │   │
   │  │                                                        │  │   │
   │  │  2. TikTokUrlParser.parse(url)                         │  │   │
   │  │     → productId = "123"                                │  │   │
   │  │                                                        │  │   │
   │  │  3. Load AffiliatePlatform where code = "tiktok"       │  │   │
   │  │     → affiliateId = "CASHBEE_VN_001"                   │  │   │
   │  │                                                        │  │   │
   │  │  4. TrackingCodeGenerator.generate(userId=1, clickId=50) │  │   │
   │  │     → trackingCode = "CB1_50_20251226150000"           │  │   │
   │  │                                                        │  │   │
   │  │  5. TikTokAffiliateLinkBuilder.build(...)              │  │   │
   │  │     → trackingUrl = "https://affiliate.tiktok.com/r?   │  │   │
   │  │        url=...&affiliate_id=CASHBEE_VN_001&            │  │   │
   │  │        sub_id=CB1_50_20251226150000"                   │  │   │
   │  │                                                        │  │   │
   │  │  6. Save to affiliate_click table                      │  │   │
   │  │     - id: 50                                           │  │   │
   │  │     - user_id: 1                                       │  │   │
   │  │     - platform_id: 3 (tiktok)                          │  │   │
   │  │     - tracking_code: "CB1_50_20251226150000"           │  │   │
   │  │     - status: CREATED                                  │  │   │
   │  └────────────────────────────────────────────────────────┘  │   │
   └──────────────────────────┬───────────────────────────────────┘   │
                              │                                       │
                              │  3. Return tracking URL               │
                              ▼                                       │
   ┌────────────────────────────────────────┐                         │
   │         CashBee Mobile App             │                         │
   │  ┌──────────────────────────────────┐  │                         │
   │  │  Link da tao thanh cong!         │  │                         │
   │  │                                  │  │                         │
   │  │  Cashback uoc tinh: 5%           │  │                         │
   │  │                                  │  │                         │
   │  │  [Copy Link]  [Chia se]  [Mua]   │  │                         │
   │  └──────────────────────────────────┘  │                         │
   └────────────────┬───────────────────────┘                         │
                    │                                                 │
                    │  4. User clicks "Mua" → Opens tracking URL      │
                    ▼                                                 │
   ┌──────────────────────────────────────────────────────────────┐   │
   │                    CashBee Backend                            │   │
   │  ┌────────────────────────────────────────────────────────┐  │   │
   │  │  HandleClickRedirectUseCase                            │  │   │
   │  │                                                        │  │   │
   │  │  1. Find click by ID = 50                              │  │   │
   │  │  2. Update status = CLICKED                            │  │   │
   │  │  3. Update clicked_at = NOW()                          │  │   │
   │  │  4. Return 302 Redirect to TikTok affiliate URL        │  │   │
   │  └────────────────────────────────────────────────────────┘  │   │
   └──────────────────────────┬───────────────────────────────────┘   │
                              │                                       │
                              │  5. HTTP 302 Redirect                 │
                              ▼                                       │
   ┌──────────────────────────────────────────────────────────────────┤
   │              TikTok Shop (in-app or browser)                     │
   │  ┌────────────────────────────────────────────────────────────┐  │
   │  │                                                            │  │
   │  │   Product page with affiliate tracking                     │  │
   │  │   sub_id = "CB1_50_20251226150000" tracked by TikTok      │  │
   │  │                                                            │  │
   │  │   User adds to cart and completes purchase                 │  │
   │  │                                                            │  │
   │  └────────────────────────────────────────────────────────────┘  │
   └──────────────────────────┬───────────────────────────────────────┘
                              │
                              │  6. Order completed on TikTok
                              │
   ┌──────────────────────────┴───────────────────────────────────────┐
   │                    TikTok Server                                  │
   │  ┌────────────────────────────────────────────────────────────┐  │
   │  │  Order Event:                                              │  │
   │  │  - order_id: "TT123456789"                                 │  │
   │  │  - sub_id: "CB1_50_20251226150000"                         │  │
   │  │  - total_amount: 500,000 VND                               │  │
   │  │  - commission: 25,000 VND (5%)                             │  │
   │  │  - status: COMPLETED                                       │  │
   │  └────────────────────────────────────────────────────────────┘  │
   └──────────────────────────┬───────────────────────────────────────┘
                              │
                              │  7. Webhook notification OR API sync
                              ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                    CashBee Backend                                │
   │  ┌────────────────────────────────────────────────────────────┐  │
   │  │  ImportTikTokOrdersUseCase                                 │  │
   │  │                                                            │  │
   │  │  1. Receive order data                                     │  │
   │  │     - sub_id: "CB1_50_20251226150000"                      │  │
   │  │                                                            │  │
   │  │  2. Parse tracking code:                                   │  │
   │  │     - userId: 1                                            │  │
   │  │     - clickId: 50                                          │  │
   │  │                                                            │  │
   │  │  3. Find AffiliateClick by trackingCode                    │  │
   │  │     → Found click #50 for user #1                          │  │
   │  │                                                            │  │
   │  │  4. Create AffiliateOrder                                  │  │
   │  │     - platform_order_id: "TT123456789"                     │  │
   │  │     - user_id: 1                                           │  │
   │  │     - click_id: 50                                         │  │
   │  │     - order_amount: 500,000                                │  │
   │  │     - commission: 25,000                                   │  │
   │  │     - status: PENDING                                      │  │
   │  │                                                            │  │
   │  │  5. Update AffiliateClick                                  │  │
   │  │     - status: ORDER_MATCHED                                │  │
   │  │     - order_matched: true                                  │  │
   │  │                                                            │  │
   │  │  6. Update User Wallet                                     │  │
   │  │     - Add to pending_balance: 20,000 VND                   │  │
   │  │       (25,000 * 80% = user's share)                        │  │
   │  │                                                            │  │
   │  │  7. Send notification to user                              │  │
   │  │     "Don hang 500k da duoc ghi nhan!                       │  │
   │  │      Cashback du kien: 20,000 VND"                         │  │
   │  └────────────────────────────────────────────────────────────┘  │
   └──────────────────────────────────────────────────────────────────┘
                              │
                              │  8. After 14-30 days confirmation
                              ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                    CashBee Backend (Scheduled Job)                │
   │  ┌────────────────────────────────────────────────────────────┐  │
   │  │  ConfirmPendingOrdersUseCase                               │  │
   │  │                                                            │  │
   │  │  1. Find orders with status = PENDING                      │  │
   │  │     and order_date < NOW() - 14 days                       │  │
   │  │                                                            │  │
   │  │  2. Update status = CONFIRMED                              │  │
   │  │                                                            │  │
   │  │  3. Move from pending to available balance                 │  │
   │  │     - pending_balance -= 20,000                            │  │
   │  │     - available_balance += 20,000                          │  │
   │  │                                                            │  │
   │  │  4. Notify user:                                           │  │
   │  │     "20,000 VND da duoc chuyen vao so du kha dung!"        │  │
   │  └────────────────────────────────────────────────────────────┘  │
   └──────────────────────────────────────────────────────────────────┘
```

---

## 7. Database Schema

### 7.1 Liquibase Changelog

```xml
<!-- XXX-add-tiktok-platform.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">

    <changeSet id="add-tiktok-platform" author="cashbee">
        <insert tableName="affiliate_platform">
            <column name="name" value="TikTok Shop"/>
            <column name="code" value="tiktok"/>
            <column name="affiliate_id" value="${TIKTOK_AFFILIATE_ID}"/>
            <column name="api_key" value="${TIKTOK_APP_KEY}"/>
            <column name="api_secret" value="${TIKTOK_APP_SECRET}"/>
            <column name="base_url" value="https://open-api.tiktokglobalshop.com"/>
            <column name="tracking_enabled" valueBoolean="true"/>
            <column name="default_commission_rate" valueNumeric="5.00"/>
            <column name="status" value="ACTIVE"/>
            <column name="created_at" valueDate="CURRENT_TIMESTAMP"/>
        </insert>
    </changeSet>

</databaseChangeLog>
```

---

## 8. API Endpoints

### 8.1 Existing Endpoints (Support TikTok)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/affiliate/tracking/create-link` | POST | Tao tracking link (ho tro TikTok URL) |
| `/api/affiliate/tracking/redirect/{clickId}` | GET | Redirect den TikTok |
| `/api/affiliate/platforms` | GET | List platforms (bao gom TikTok) |

### 8.2 New Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/webhooks/tiktok/order` | POST | Nhan webhook tu TikTok |
| `/api/admin/tiktok/sync-orders` | POST | Manual trigger sync orders |
| `/api/admin/tiktok/status` | GET | Check TikTok integration status |

---

## 9. Code Implementation

### 9.1 Files Can Tao

| File | Module | Priority |
|------|--------|----------|
| `TikTokUrlParser.java` | application | HIGH |
| `TikTokAffiliateLinkBuilder.java` | application | HIGH |
| `TikTokApiClient.java` | infrastructure | HIGH |
| `TikTokApiConfig.java` | infrastructure | HIGH |
| `TikTokOrderDto.java` | infrastructure | MEDIUM |
| `TikTokWebhookController.java` | presentation | MEDIUM |
| `SyncTikTokOrdersUseCase.java` | application | MEDIUM |
| `TikTokWebhookPayload.java` | application | MEDIUM |

### 9.2 Files Can Sua

| File | Changes |
|------|---------|
| `CreateTrackingLinkUseCase.java` | Them TikTok handler |
| `application.yml` | Them TikTok config |
| `db.changelog-master.xml` | Include TikTok changelog |

---

## 10. Testing

### 10.1 Unit Tests

```java
// TikTokUrlParserTest.java
@Test
void shouldParseProductUrl() {
    String url = "https://www.tiktok.com/@shop/product/123456";
    ParsedTikTokUrl result = parser.parse(url);

    assertEquals("123456", result.getProductId());
    assertEquals("product", result.getFormat());
}

@Test
void shouldParseShortLink() {
    String url = "https://vm.tiktok.com/ABC123";
    // Need to mock URL expansion
}
```

### 10.2 Integration Tests

```java
// TikTokIntegrationTest.java
@SpringBootTest
@Testcontainers
class TikTokIntegrationTest {

    @Test
    void shouldCreateTikTokTrackingLink() {
        CreateTrackingLinkRequest request = new CreateTrackingLinkRequest();
        request.setUrl("https://www.tiktok.com/@shop/product/123");

        TrackingLinkResponse response = useCase.execute(request, 1L);

        assertThat(response.getPlatformCode()).isEqualTo("tiktok");
        assertThat(response.getTrackingUrl()).contains("affiliate.tiktok.com");
    }
}
```

---

## 11. Checklist Trien Khai

### Phase 1: Setup (Day 1-2)
- [ ] Dang ky TikTok Shop Partner account
- [ ] Tao App trong Developer Console
- [ ] Luu App Key, App Secret, Service ID
- [ ] Them config vao application.yml
- [ ] Tao Liquibase changelog cho TikTok platform

### Phase 2: Core Implementation (Day 3-5)
- [ ] Implement TikTokUrlParser
- [ ] Implement TikTokAffiliateLinkBuilder
- [ ] Modify CreateTrackingLinkUseCase
- [ ] Unit tests cho parser va builder

### Phase 3: API Integration (Day 6-8)
- [ ] Implement TikTokApiClient
- [ ] Implement TikTokApiConfig
- [ ] Test API authentication
- [ ] Test get orders API

### Phase 4: Order Sync (Day 9-10)
- [ ] Implement SyncTikTokOrdersUseCase
- [ ] Setup scheduled job
- [ ] Implement webhook controller
- [ ] Test order matching logic

### Phase 5: Testing & QA (Day 11-12)
- [ ] Integration tests
- [ ] End-to-end testing
- [ ] Performance testing
- [ ] Security review

### Phase 6: Deployment (Day 13-14)
- [ ] Deploy to staging
- [ ] UAT testing
- [ ] Deploy to production
- [ ] Monitor and fix issues

---

## Nguon Tham Khao

- [TikTok for Developers](https://developers.tiktok.com/)
- [TikTok Shop Partner Center](https://partner.tiktokshop.com)
- [TikTok Shop Affiliate APIs Launch](https://developers.tiktok.com/blog/2024-tiktok-shop-affiliate-apis-launch-developer-opportunity)
- [TikTok Affiliate Seller API Overview](https://partner.tiktokshop.com/docv2/page/affiliate-seller-api-overview)
- [TikTok Deeplink Formats](https://ads.tiktok.com/help/article/list-of-supported-deeplink-formats)
- [TikTok Shop Vietnam](https://seller-vn.tiktok.com/university/essay?knowledge_id=6837838528808705)

---

## Lien He & Ho Tro

- TikTok Developer Discord: [Join Developer Hub](https://developers.tiktok.com/)
- TikTok Shop Support: partner.tiktokshop.com
- CashBee Team: dev@cashbee.vn
