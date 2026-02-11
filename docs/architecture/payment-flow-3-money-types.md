# CashBee Payment Flow: 3 Loại Tiền

> **Version:** 1.0.0
> **Last Updated:** 2026-01-30
> **Author:** CashBee Backend Team
> **Audience:** Backend Developers, New Team Members

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [3 Loại Tiền trong CashBee](#2-3-loại-tiền-trong-cashbee)
3. [Database Schema](#3-database-schema)
4. [Flow Chi Tiết](#4-flow-chi-tiết)
5. [Code Implementation](#5-code-implementation)
6. [Ví Dụ Thực Tế](#6-ví-dụ-thực-tế)
7. [API Endpoints](#7-api-endpoints)
8. [Troubleshooting](#8-troubleshooting)
9. [Glossary](#9-glossary)

---

## 1. Tổng quan

CashBee là nền tảng hoàn tiền (cashback) khi người dùng mua hàng qua các sàn thương mại điện tử (Shopee, Lazada, Tiki, TikTok Shop).

### Business Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   User mua hàng    →    Sàn trả hoa hồng    →    CashBee chia      │
│   qua link CashBee      cho CashBee              tiền cho User      │
│                                                                     │
│   VD: Mua 1,000,000đ    Shopee trả 2.7%         User nhận 80%      │
│                         = 27,000đ                = 21,600đ          │
│                                                  CashBee giữ 20%    │
│                                                  = 5,400đ           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3 Nguồn Thu Nhập của User

| # | Loại | Nguồn | Tỷ lệ |
|---|------|-------|-------|
| 1 | **Cashback** | Từ đơn hàng của chính mình | 80% hoa hồng sàn |
| 2 | **Bonus** | Thưởng khi đạt mốc đơn hàng | Cố định theo mốc |
| 3 | **Commission** | Từ đơn hàng của người được giới thiệu | 5% hoa hồng sàn |

---

## 2. 3 Loại Tiền trong CashBee

### 2.1. Cashback (Tiền hoàn)

**Định nghĩa:** Tiền user nhận được khi mua hàng qua link affiliate của CashBee.

**Công thức:**
```
Cashback = Hoa hồng sàn × Tỷ lệ hoàn (thường 80%)
```

**Ví dụ:**
```
Đơn hàng: 500,000đ
Hoa hồng Shopee (2.7%): 13,500đ
Cashback cho user (80%): 10,800đ
CashBee giữ lại (20%): 2,700đ
```

**Trạng thái Cashback:**

| Status | Ý nghĩa |
|--------|---------|
| `PENDING` | Đơn hàng đang xử lý, chưa xác nhận |
| `CONFIRMED` | Đơn hoàn thành, chờ thanh toán |
| `PAID` | Đã chuyển tiền cho user |
| `CANCELLED` | Đơn bị huỷ/hoàn |

---

### 2.2. Bonus (Thưởng mốc)

**Định nghĩa:** Tiền thưởng khi user đạt các mốc số đơn hàng nhất định.

**Bảng mốc thưởng (WITH_REFERRER - có người giới thiệu):**

| Mốc | User nhận (B) | Người giới thiệu nhận (A) | Ghi chú |
|-----|---------------|---------------------------|---------|
| 1 đơn | 10,000đ | 20,000đ | Kích hoạt referral |
| 5 đơn | 15,000đ | 10,000đ | |
| 20 đơn | 20,000đ | - | Pre-VIP |
| 50 đơn | - | - | Lên VIP (83% cashback) |
| 150 đơn | 30,000đ | - | Lên SUPER (85% cashback) |

**Bảng mốc thưởng (WITHOUT_REFERRER - không có người giới thiệu):**

| Mốc | User nhận | Ghi chú |
|-----|-----------|---------|
| 1 đơn | 5,000đ | Welcome bonus |
| 20 đơn | 10,000đ | |
| 50 đơn | - | Lên VIP |
| 150 đơn | 20,000đ | Lên SUPER |

**Điều kiện:**
- Chỉ đếm đơn hàng có giá trị > 100,000đ (chống abuse)
- Mỗi mốc chỉ nhận 1 lần

---

### 2.3. Commission (Hoa hồng giới thiệu)

**Định nghĩa:** Tiền người giới thiệu (referrer) nhận được từ mỗi đơn hàng của người được giới thiệu (referee).

**Công thức:**
```
Commission = Hoa hồng sàn gốc × 5%
```

**Lưu ý quan trọng:**
- Commission được **CashBee trả**, KHÔNG trừ từ cashback của referee
- Thời hạn: 12 tháng kể từ khi referee kích hoạt referral
- Không giới hạn số tiền

**Ví dụ:**
```
Referee (B) mua hàng 1,000,000đ
Hoa hồng Shopee: 27,000đ

→ B nhận cashback: 27,000 × 80% = 21,600đ
→ A nhận commission: 27,000 × 5% = 1,350đ (CashBee trả)
```

---

## 3. Database Schema

### 3.1. Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│      user       │       │ affiliate_order │       │    cashback     │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id              │◄──┐   │ id              │◄──────│ order_id        │
│ username        │   │   │ order_code      │       │ user_id ────────┼──┐
│ referred_by ────┼───┼──►│ product_price   │       │ cashback_amount │  │
│ referral_code   │   │   │ commission      │       │ status          │  │
└─────────────────┘   │   └─────────────────┘       │ paid_batch_id ──┼──┼──┐
                      │                             └─────────────────┘  │  │
                      │                                                  │  │
┌─────────────────┐   │   ┌─────────────────┐       ┌─────────────────┐  │  │
│ referral_reward │   │   │referrer_commission│     │payment_invoice  │  │  │
├─────────────────┤   │   ├─────────────────┤       ├─────────────────┤  │  │
│ id              │   │   │ id              │       │ id              │  │  │
│ user_id ────────┼───┘   │ referrer_id ────┼───────│ user_id ────────┼──┘  │
│ reward_type     │       │ referee_id      │       │ batch_id ───────┼─────┘
│ milestone       │       │ commission_amt  │       │ amount          │
│ amount          │       │ status          │       │ bonus_amount    │
│ paid_batch_id ──┼───────│ paid_batch_id ──┼───────│ referrer_comm   │
│ paid_at         │       │ paid_at         │       └─────────────────┘
└─────────────────┘       └─────────────────┘

                      ┌─────────────────┐
                      │batch_transfer_  │
                      │    export       │
                      ├─────────────────┤
                      │ id              │◄─── paid_batch_id từ các bảng trên
                      │ batch_code      │
                      │ status          │
                      │ total_amount    │
                      └─────────────────┘
```

### 3.2. Các bảng chính

#### `cashback` - Tiền hoàn từ đơn hàng

```sql
CREATE TABLE cashback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,              -- User sở hữu cashback
    order_id BIGINT NOT NULL,             -- Đơn hàng nguồn
    platform_id BIGINT,                   -- Sàn TMĐT
    cashback_amount DECIMAL(19,2),        -- Số tiền hoàn
    commission DECIMAL(19,2),             -- Hoa hồng gốc từ sàn
    cashback_rate DECIMAL(5,2),           -- Tỷ lệ hoàn (80%)
    status VARCHAR(20),                   -- PENDING/CONFIRMED/PAID/CANCELLED
    paid_batch_id BIGINT,                 -- Batch đã thanh toán (NULL = chưa)
    created_at DATETIME,
    updated_at DATETIME,

    INDEX idx_cashback_user_status (user_id, status),
    INDEX idx_cashback_paid_batch (paid_batch_id)
);
```

#### `referral_reward` - Thưởng mốc

```sql
CREATE TABLE referral_reward (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,              -- User nhận thưởng
    referrer_id BIGINT,                   -- Người giới thiệu (nếu có)
    reward_type VARCHAR(30),              -- MILESTONE_BONUS / REFERRER_BONUS
    milestone INT,                        -- Mốc đạt được (1, 5, 20, 50, 150)
    amount DECIMAL(19,2),                 -- Số tiền thưởng
    status VARCHAR(20),                   -- PENDING/GRANTED/EXPIRED
    paid_batch_id BIGINT,                 -- Batch đã thanh toán
    paid_at DATETIME,                     -- Thời điểm thanh toán
    created_at DATETIME,

    UNIQUE KEY uk_user_milestone (user_id, milestone),
    INDEX idx_reward_paid_batch (paid_batch_id)
);
```

#### `referrer_commission` - Hoa hồng giới thiệu

```sql
CREATE TABLE referrer_commission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    referrer_id BIGINT NOT NULL,          -- Người giới thiệu (nhận tiền)
    referee_id BIGINT NOT NULL,           -- Người được giới thiệu
    source_order_id BIGINT NOT NULL,      -- Đơn hàng nguồn
    original_commission DECIMAL(19,2),    -- Hoa hồng gốc từ sàn
    commission_rate DECIMAL(5,2),         -- Tỷ lệ (5%)
    commission_amount DECIMAL(19,2),      -- Số tiền = original × 5%
    status VARCHAR(20),                   -- PENDING/CONFIRMED/PAID
    expires_at DATETIME,                  -- Hết hạn (12 tháng)
    paid_batch_id BIGINT,                 -- Batch đã thanh toán
    paid_at DATETIME,                     -- Thời điểm thanh toán
    created_at DATETIME,

    UNIQUE KEY uk_source_order (source_order_id),
    INDEX idx_commission_referrer (referrer_id),
    INDEX idx_commission_paid_batch (paid_batch_id)
);
```

#### `payment_invoice` - Hoá đơn thanh toán

```sql
CREATE TABLE payment_invoice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,             -- Liên kết với batch_transfer_export
    invoice_number VARCHAR(50) UNIQUE,    -- VD: INV-20260130-00001

    -- Breakdown theo loại tiền
    amount DECIMAL(19,2),                 -- Tổng cashback
    bonus_amount DECIMAL(19,2),           -- Tổng thưởng mốc
    bonus_orders INT,                     -- Số mốc thưởng
    referrer_commission_amount DECIMAL(19,2), -- Tổng hoa hồng giới thiệu
    referrer_commission_orders INT,       -- Số đơn hoa hồng

    -- Breakdown theo sàn
    shopee_amount DECIMAL(19,2),
    lazada_amount DECIMAL(19,2),
    tiki_amount DECIMAL(19,2),
    tiktok_amount DECIMAL(19,2),
    other_amount DECIMAL(19,2),

    -- Bank info
    bank_name VARCHAR(50),
    bank_account_number VARCHAR(50),
    account_name VARCHAR(100),

    transfer_status VARCHAR(20),          -- COMPLETED/PENDING/FAILED
    transfer_time DATETIME,
    created_at DATETIME,

    INDEX idx_invoice_user (user_id),
    INDEX idx_invoice_batch (batch_id)
);
```

### 3.3. Cột quan trọng: `paid_batch_id`

Cột `paid_batch_id` xuất hiện ở 3 bảng và có vai trò **quan trọng nhất**:

| Bảng | Ý nghĩa khi `paid_batch_id = NULL` | Ý nghĩa khi `paid_batch_id = 18` |
|------|-----------------------------------|----------------------------------|
| `cashback` | Chưa thanh toán (trong ví) | Đã thanh toán ở batch 18 |
| `referral_reward` | Bonus chưa được chuyển | Bonus đã chuyển ở batch 18 |
| `referrer_commission` | Commission chưa chuyển | Commission đã chuyển ở batch 18 |

**Lợi ích:**
1. **Truy vết:** Biết chính xác tiền nào thanh toán ở batch nào
2. **Chống trùng:** Không thể thanh toán 2 lần
3. **Query nhanh:** Lấy tất cả tiền trong 1 hoá đơn bằng `WHERE paid_batch_id = ?`

---

## 4. Flow Chi Tiết

### 4.1. Tổng quan 3 Phase

```
╔══════════════════╗    ╔══════════════════╗    ╔══════════════════╗
║   PHASE 1        ║    ║   PHASE 2        ║    ║   PHASE 3        ║
║   Thu thập       ║ → ║   Thanh toán     ║ → ║   Xem hoá đơn    ║
║                  ║    ║                  ║    ║                  ║
║ • Import đơn     ║    ║ • Admin tạo     ║    ║ • API lấy        ║
║ • Tạo cashback   ║    ║   batch          ║    ║   invoice        ║
║ • Tạo bonus      ║    ║ • Complete      ║    ║ • Hiển thị       ║
║ • Tạo commission ║    ║   batch          ║    ║   breakdown      ║
╚══════════════════╝    ╚══════════════════╝    ╚══════════════════╝
```

### 4.2. Phase 1: Thu thập tiền (Import đơn hàng)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         IMPORT ĐƠN HÀNG                             │
│                    (ImportShopeeOrdersUseCase)                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│  1. Parse CSV/Excel file                                            │
│  2. Validate data                                                   │
│  3. Tạo/Update AffiliateOrder                                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            ▼                   ▼                   ▼
    ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
    │  💰 CASHBACK  │   │  🎁 BONUS     │   │  👥 COMMISSION│
    │               │   │               │   │               │
    │ Tạo record    │   │ Kiểm tra mốc  │   │ Nếu user có   │
    │ trong bảng    │   │ Nếu đạt →     │   │ referrer →    │
    │ cashback      │   │ tạo reward    │   │ tạo commission│
    │               │   │               │   │ cho referrer  │
    │ status =      │   │ status =      │   │ status =      │
    │ CONFIRMED     │   │ GRANTED       │   │ CONFIRMED     │
    │               │   │               │   │               │
    │ paid_batch_id │   │ paid_batch_id │   │ paid_batch_id │
    │ = NULL        │   │ = NULL        │   │ = NULL        │
    └───────────────┘   └───────────────┘   └───────────────┘
```

**Code tham khảo:**

```java
// ImportShopeeOrdersUseCase.java
// 1. Tạo Cashback
Cashback cashback = Cashback.builder()
    .userId(userId)
    .orderId(order.getId())
    .cashbackAmount(cashbackAmount)
    .status(CashbackStatus.CONFIRMED)
    // paid_batch_id = null (mặc định)
    .build();
cashbackRepository.save(cashback);

// 2. Kiểm tra và tạo Bonus (ProcessReferralMilestoneUseCase)
processReferralMilestoneUseCase.execute(userId);

// 3. Tạo Commission (CalculateReferrerCommissionUseCase)
if (user.getReferredBy() != null && isReferralActive(user)) {
    calculateReferrerCommissionUseCase.execute(order.getId(), userId);
}
```

### 4.3. Phase 2: Thanh toán Batch

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ADMIN TẠO BATCH TRANSFER                         │
│                   (CreateBatchTransferUseCase)                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│  1. Lấy tất cả user có tiền CONFIRMED                              │
│  2. Snapshot cashback IDs cho mỗi user                             │
│  3. Tạo batch_transfer_export + batch_transfer_item                │
│  4. Export file Excel cho ngân hàng                                │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  ADMIN CHUYỂN TIỀN & COMPLETE                       │
│                 (CompleteBatchTransferUseCase)                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            ▼                   ▼                   ▼
    ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
    │  💰 CASHBACK  │   │  🎁 BONUS     │   │  👥 COMMISSION│
    │               │   │               │   │               │
    │ UPDATE        │   │ UPDATE        │   │ UPDATE        │
    │ cashback      │   │ referral_     │   │ referrer_     │
    │ SET           │   │ reward SET    │   │ commission SET│
    │ status=PAID   │   │ paid_batch_id │   │ paid_batch_id │
    │ paid_batch_id │   │ = 18          │   │ = 18          │
    │ = 18          │   │ paid_at=NOW() │   │ paid_at=NOW() │
    └───────────────┘   └───────────────┘   └───────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     TẠO HOÁ ĐƠN                                     │
│               (GeneratePaymentInvoiceUseCase)                       │
│                                                                     │
│  payment_invoice:                                                   │
│    - amount = SUM(cashback)                                        │
│    - bonus_amount = SUM(referral_reward.amount)                    │
│    - referrer_commission_amount = SUM(referrer_commission.amount)  │
│    - batch_id = 18                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

**Code quan trọng trong CompleteBatchTransferUseCase:**

```java
// ========== 1. LOAD DỮ LIỆU UPFRONT ==========
// Load tất cả unpaid cho mỗi user trong batch
for (Long userId : userIds) {
    // Cashback đã được snapshot khi tạo batch

    // Load unpaid bonus
    List<ReferralReward> unpaidRewards =
        referralRewardRepository.findUnpaidByUserId(userId);

    // Load unpaid commission (user là referrer)
    List<ReferrerCommission> unpaidCommissions =
        referrerCommissionRepository.findUnpaidByReferrerId(userId);
}

// ========== 2. ĐÁNH DẤU ĐÃ THANH TOÁN ==========
// Đánh dấu Cashback
cashbackRepository.updateStatusByCashbackIdsWithBatchId(
    cashbackIds, CashbackStatus.PAID, batchId);

// Đánh dấu Bonus
referralRewardRepository.markAsPaidByBatch(rewardIds, batchId);
// SQL: UPDATE referral_reward
//      SET paid_batch_id=18, paid_at=NOW()
//      WHERE id IN (...)

// Đánh dấu Commission
referrerCommissionRepository.markAsPaidByBatch(commissionIds, batchId);
// SQL: UPDATE referrer_commission
//      SET paid_batch_id=18, paid_at=NOW()
//      WHERE id IN (...)

// ========== 3. TẠO HOÁ ĐƠN ==========
GenerateInvoiceCommand command = GenerateInvoiceCommand.builder()
    .userId(userId)
    .batchId(batchId)
    .amount(cashbackAmount)
    .bonusAmount(bonusAmount)
    .referrerCommissionAmount(commissionAmount)
    .build();
generatePaymentInvoiceUseCase.execute(command);
```

### 4.4. Phase 3: Xem hoá đơn

```
┌─────────────────────────────────────────────────────────────────────┐
│              USER GỌI API: GET /api/invoices/58/details             │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│              GetInvoiceWithOrderDetailsUseCase                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            ▼                   ▼                   ▼
    ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
    │  💰 CASHBACK  │   │  🎁 BONUS     │   │  👥 COMMISSION│
    │               │   │               │   │               │
    │ SELECT *      │   │ SELECT *      │   │ SELECT *      │
    │ FROM cashback │   │ FROM referral_│   │ FROM referrer_│
    │ WHERE         │   │ reward WHERE  │   │ commission    │
    │ paid_batch_id │   │ paid_batch_id │   │ WHERE         │
    │ = 18          │   │ = 18          │   │ paid_batch_id │
    │ AND user_id   │   │ AND user_id   │   │ = 18          │
    │ = 31          │   │ = 31          │   │ AND referrer_ │
    │               │   │               │   │ id = 31       │
    │ → orders[]    │   │ → milestone   │   │ → referrer    │
    │   items[]     │   │   Bonus       │   │   Commission  │
    └───────────────┘   └───────────────┘   └───────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API RESPONSE                                │
│                                                                     │
│  {                                                                  │
│    "summary": {                                                     │
│      "totalCashback": 54000,                                       │
│      "totalOrders": 5                                              │
│    },                                                               │
│    "orders": [...],           // Chi tiết từng đơn hàng            │
│    "bonusBreakdown": {                                             │
│      "totalBonusAmount": 25795,                                    │
│      "milestoneBonus": {                                           │
│        "count": 2,                                                 │
│        "amount": 25000,                                            │
│        "description": "Đạt 2 mốc: 1 đơn, 5 đơn"                   │
│      },                                                            │
│      "referrerCommission": {                                       │
│        "count": 1,                                                 │
│        "amount": 795,                                              │
│        "description": "Hoa hồng từ 1 đơn của người được GT"       │
│      }                                                             │
│    }                                                               │
│  }                                                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. Code Implementation

### 5.1. Cấu trúc thư mục

```
cashbee-backend/
├── cashbee-domain/
│   ├── model/
│   │   ├── Cashback.java              # Domain model
│   │   ├── ReferralReward.java        # Domain model
│   │   └── ReferrerCommission.java    # Domain model
│   └── repository/
│       ├── CashbackRepository.java           # Interface
│       ├── ReferralRewardRepository.java     # Interface
│       └── ReferrerCommissionRepository.java # Interface
│
├── cashbee-infrastructure/
│   └── persistence/
│       ├── entity/
│       │   ├── CashbackJpaEntity.java
│       │   ├── ReferralRewardJpaEntity.java
│       │   └── ReferrerCommissionJpaEntity.java
│       ├── repository/
│       │   ├── CashbackJpaRepository.java
│       │   ├── ReferralRewardJpaRepository.java
│       │   └── ReferrerCommissionJpaRepository.java
│       └── adapter/
│           ├── CashbackRepositoryAdapter.java
│           ├── ReferralRewardRepositoryAdapter.java
│           └── ReferrerCommissionRepositoryAdapter.java
│
├── cashbee-application/
│   └── usecase/
│       ├── order/
│       │   └── ImportShopeeOrdersUseCase.java      # Phase 1
│       ├── referral/
│       │   ├── ProcessReferralMilestoneUseCase.java    # Tạo bonus
│       │   └── CalculateReferrerCommissionUseCase.java # Tạo commission
│       ├── transfer/
│       │   ├── CreateBatchTransferUseCase.java     # Tạo batch
│       │   └── CompleteBatchTransferUseCase.java   # Phase 2 ⭐
│       └── invoice/
│           ├── GeneratePaymentInvoiceUseCase.java  # Tạo hoá đơn
│           └── GetInvoiceWithOrderDetailsUseCase.java # Phase 3
│
└── cashbee-presentation/
    └── controller/
        └── InvoiceController.java    # API endpoints
```

### 5.2. Repository Methods quan trọng

#### CashbackRepository

```java
// Lấy cashback chưa thanh toán
List<Cashback> findByUserIdAndStatus(Long userId, CashbackStatus status);

// Đánh dấu đã thanh toán
int updateStatusByCashbackIdsWithBatchId(
    List<Long> cashbackIds,
    CashbackStatus fromStatus,
    CashbackStatus toStatus,
    Long batchId
);

// Lấy cashback theo batch (cho hoá đơn)
List<Cashback> findByPaidBatchId(Long batchId);
```

#### ReferralRewardRepository

```java
// Lấy bonus chưa thanh toán
List<ReferralReward> findUnpaidByUserId(Long userId);
// SQL: WHERE user_id=? AND status='GRANTED' AND paid_batch_id IS NULL

// Tổng bonus chưa thanh toán
BigDecimal sumUnpaidAmountByUserId(Long userId);

// Đánh dấu đã thanh toán
int markAsPaidByBatch(List<Long> rewardIds, Long batchId);
// SQL: UPDATE SET paid_batch_id=?, paid_at=NOW() WHERE id IN (...)

// Lấy theo batch (cho hoá đơn)
List<ReferralReward> findByPaidBatchIdAndUserId(Long batchId, Long userId);
```

#### ReferrerCommissionRepository

```java
// Lấy commission chưa thanh toán (user là referrer)
List<ReferrerCommission> findUnpaidByReferrerId(Long referrerId);
// SQL: WHERE referrer_id=? AND status IN ('CONFIRMED','PAID')
//      AND paid_batch_id IS NULL

// Tổng commission chưa thanh toán
BigDecimal sumUnpaidCommissionByReferrerId(Long referrerId);

// Đánh dấu đã thanh toán
int markAsPaidByBatch(List<Long> commissionIds, Long batchId);

// Lấy theo batch (cho hoá đơn)
List<ReferrerCommission> findByPaidBatchIdAndReferrerId(Long batchId, Long referrerId);
```

---

## 6. Ví Dụ Thực Tế

### Scenario: User A có 2 referee (B và C)

```
┌─────────────────────────────────────────────────────────────────────┐
│                          SETUP                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   User A (ID: 31)                                                   │
│   ├── Mã giới thiệu: CBMC84TW                                      │
│   ├── Có 5 đơn hàng của chính mình (cashback)                      │
│   └── Có 2 referee:                                                 │
│       ├── User B (ID: 92) - đã mua 1 đơn                           │
│       └── User C (ID: 108) - chưa mua đơn nào                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    KHI COMPLETE BATCH 18                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   💰 CASHBACK của A (từ 5 đơn của chính A):                        │
│   ┌───────────────────────────────────────┐                        │
│   │ Đơn 1: 10,000đ  → paid_batch_id = 18  │                        │
│   │ Đơn 2: 12,000đ  → paid_batch_id = 18  │                        │
│   │ Đơn 3:  8,000đ  → paid_batch_id = 18  │                        │
│   │ Đơn 4: 15,000đ  → paid_batch_id = 18  │                        │
│   │ Đơn 5:  9,000đ  → paid_batch_id = 18  │                        │
│   └───────────────────────────────────────┘                        │
│   TỔNG CASHBACK: 54,000đ                                           │
│                                                                     │
│   🎁 BONUS của A:                                                   │
│   ┌───────────────────────────────────────┐                        │
│   │ Mốc 1 đơn: 10,000đ → paid_batch_id=18 │                        │
│   │ Mốc 5 đơn: 15,000đ → paid_batch_id=18 │                        │
│   └───────────────────────────────────────┘                        │
│   TỔNG BONUS: 25,000đ                                              │
│                                                                     │
│   👥 COMMISSION của A (từ đơn của B):                              │
│   ┌───────────────────────────────────────┐                        │
│   │ Từ đơn 443 của B:                     │                        │
│   │   Hoa hồng sàn: 15,910đ               │                        │
│   │   A nhận 5%: 795đ                     │                        │
│   │   → paid_batch_id = 18                │                        │
│   └───────────────────────────────────────┘                        │
│   TỔNG COMMISSION: 795đ                                            │
│                                                                     │
│   (User C chưa mua → không có commission từ C)                     │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   📄 HOÁ ĐƠN #58 cho User A:                                       │
│   ══════════════════════════════════════════                        │
│   │ Cashback:        54,000đ                                       │
│   │ Bonus:           25,000đ                                       │
│   │ Commission:         795đ                                       │
│   │ ─────────────────────────                                      │
│   │ TỔNG:            79,795đ                                       │
│   ══════════════════════════════════════════                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Query kiểm tra trong database

```sql
-- Xem cashback đã thanh toán ở batch 18 cho user 31
SELECT id, cashback_amount, status, paid_batch_id
FROM cashback
WHERE user_id = 31 AND paid_batch_id = 18;

-- Xem bonus đã thanh toán
SELECT id, reward_type, milestone, amount, paid_batch_id, paid_at
FROM referral_reward
WHERE user_id = 31 AND paid_batch_id = 18;

-- Xem commission đã thanh toán (user 31 là referrer)
SELECT id, referee_id, commission_amount, paid_batch_id, paid_at
FROM referrer_commission
WHERE referrer_id = 31 AND paid_batch_id = 18;

-- Tổng hợp hoá đơn
SELECT
    invoice_number,
    amount as cashback,
    bonus_amount,
    referrer_commission_amount,
    (amount + bonus_amount + referrer_commission_amount) as total
FROM payment_invoice
WHERE id = 58;
```

---

## 7. API Endpoints

### 7.1. Danh sách hoá đơn

```http
GET /api/invoices/me?page=0&size=10
Authorization: Bearer <token>
```

**Response:**
```json
{
    "content": [
        {
            "id": 58,
            "invoiceNumber": "INV-20260130-00012",
            "amount": 79795.00,              // Tổng (backward compatible)
            "cashbackAmount": 54000.00,
            "bonusAmount": 25000.00,
            "referrerCommissionAmount": 795.00,
            "totalAmount": 79795.00,
            "currency": "VND",
            "transferStatus": "COMPLETED",
            "totalOrders": 5,
            "transferTime": "2026-01-30T14:30:00"
        }
    ],
    "totalElements": 1
}
```

### 7.2. Chi tiết hoá đơn

```http
GET /api/invoices/{id}/details
Authorization: Bearer <token>
```

**Response:** Xem [Invoice Detail API Documentation](../api/invoice-detail-api.md)

---

## 8. Troubleshooting

### 8.1. Bonus không hiển thị trong hoá đơn

**Nguyên nhân có thể:**
1. `paid_batch_id` chưa được set trong `referral_reward`
2. `reward_type` không phải `MILESTONE_BONUS`

**Query kiểm tra:**
```sql
SELECT * FROM referral_reward
WHERE user_id = ? AND paid_batch_id = ?;
```

### 8.2. Commission không hiển thị

**Nguyên nhân có thể:**
1. `paid_batch_id` chưa được set trong `referrer_commission`
2. Referee chưa mua đơn hàng nào
3. Referral chưa được kích hoạt

**Query kiểm tra:**
```sql
-- Xem referee có đơn hàng không
SELECT u.id, u.username, u.referral_activated_at,
       COUNT(c.id) as order_count
FROM user u
LEFT JOIN cashback c ON c.user_id = u.id AND c.status IN ('CONFIRMED','PAID')
WHERE u.referred_by = ?
GROUP BY u.id;

-- Xem commission đã tạo chưa
SELECT * FROM referrer_commission WHERE referrer_id = ?;
```

### 8.3. Tổng tiền hoá đơn sai

**Kiểm tra:**
```sql
-- So sánh tổng trong invoice vs tổng thực tế
SELECT
    pi.id,
    pi.amount as invoice_cashback,
    pi.bonus_amount as invoice_bonus,
    pi.referrer_commission_amount as invoice_commission,
    (SELECT SUM(cashback_amount) FROM cashback
     WHERE paid_batch_id = pi.batch_id AND user_id = pi.user_id) as actual_cashback,
    (SELECT SUM(amount) FROM referral_reward
     WHERE paid_batch_id = pi.batch_id AND user_id = pi.user_id) as actual_bonus,
    (SELECT SUM(commission_amount) FROM referrer_commission
     WHERE paid_batch_id = pi.batch_id AND referrer_id = pi.user_id) as actual_commission
FROM payment_invoice pi
WHERE pi.id = ?;
```

---

## 9. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| **Cashback** | Tiền hoàn từ đơn hàng của chính user |
| **Bonus** | Tiền thưởng khi đạt mốc đơn hàng |
| **Commission** | Hoa hồng 5% từ đơn của người được giới thiệu |
| **Referrer** | Người giới thiệu (người có mã giới thiệu) |
| **Referee** | Người được giới thiệu (người nhập mã giới thiệu) |
| **Batch** | Đợt thanh toán, gồm nhiều user |
| **Batch Transfer** | Quá trình chuyển tiền hàng loạt |
| **paid_batch_id** | ID của batch đã thanh toán record này |
| **Invoice** | Hoá đơn thanh toán cho user |
| **Milestone** | Mốc số đơn hàng (1, 5, 20, 50, 150) |

---

## Changelog

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-01-30 | Backend Team | Initial documentation |

---

## Related Documents

- [Invoice Detail API](../api/invoice-detail-api.md)
- [Referral System](./referral-system.md) *(TODO)*
- [Batch Transfer Flow](./batch-transfer-flow.md) *(TODO)*
