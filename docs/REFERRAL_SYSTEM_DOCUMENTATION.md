# Hệ Thống Mã Giới Thiệu (Referral System)

## Mục Lục
1. [Tổng Quan](#1-tổng-quan)
2. [Thuật Ngữ](#2-thuật-ngữ)
3. [Cấu Trúc Database](#3-cấu-trúc-database)
4. [Luồng Hoạt Động](#4-luồng-hoạt-động)
5. [Hệ Thống Milestone](#5-hệ-thống-milestone)
6. [Tính Toán Hoa Hồng](#6-tính-toán-hoa-hồng)
7. [API Endpoints](#7-api-endpoints)
8. [Cấu Trúc Code](#8-cấu-trúc-code)

---

## 1. Tổng Quan

### 1.1 Mô Tả Hệ Thống

Hệ thống mã giới thiệu cho phép người dùng giới thiệu bạn bè sử dụng CashBee. Khi người được giới thiệu hoàn thành đủ số đơn hàng, cả hai bên đều nhận được thưởng.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HỆ THỐNG REFERRAL CASHBEE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐         Chia sẻ mã          ┌──────────────┐            │
│   │   REFERRER   │  ───────────────────────▶   │   REFEREE    │            │
│   │  (Người GT)  │         "CBXYZ123"          │ (Người được  │            │
│   │              │                              │   giới thiệu) │            │
│   └──────────────┘                              └──────────────┘            │
│         │                                              │                    │
│         │                                              │                    │
│         ▼                                              ▼                    │
│   ┌──────────────┐                              ┌──────────────┐            │
│   │  Nhận 5%     │                              │ Hoàn thành   │            │
│   │  hoa hồng    │◀─────────────────────────────│  đơn hàng    │            │
│   │  từ đơn của  │     (Sau khi đạt 5 đơn)     │              │            │
│   │  Referee     │                              │              │            │
│   └──────────────┘                              └──────────────┘            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Lợi Ích

| Đối tượng | Lợi ích |
|-----------|---------|
| **Referee** (Người được giới thiệu) | Nhận thưởng milestone khi hoàn thành 5, 10, 80, 300 đơn |
| **Referrer** (Người giới thiệu) | Nhận 5% hoa hồng từ đơn hàng của Referee trong 5 tháng + thưởng milestone |

---

## 2. Thuật Ngữ

### 2.1 Các Khái Niệm Chính

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              THUẬT NGỮ                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  REFERRER (Người giới thiệu)                                               │
│  ├── Là người chia sẻ mã giới thiệu của mình                               │
│  ├── Nhận 5% hoa hồng từ đơn hàng của Referee                              │
│  └── Nhận thưởng khi Referee đạt milestone                                 │
│                                                                             │
│  REFEREE (Người được giới thiệu)                                           │
│  ├── Là người nhập mã giới thiệu của Referrer                              │
│  ├── Nhận thưởng milestone khi hoàn thành đủ đơn                           │
│  └── Được nâng cấp tier khi đạt milestone 80, 300                          │
│                                                                             │
│  REFERRAL CODE (Mã giới thiệu)                                             │
│  ├── Format: CB + 6 ký tự ngẫu nhiên (VD: CB4F7A9K)                        │
│  ├── Mỗi user có 1 mã duy nhất                                             │
│  └── Không phân biệt hoa thường (case-insensitive)                         │
│                                                                             │
│  MILESTONE (Cột mốc)                                                       │
│  ├── Các mốc đơn hàng: 5, 10, 80, 300                                      │
│  ├── Mỗi mốc có thưởng riêng                                               │
│  └── Có thể kích hoạt nâng tier                                            │
│                                                                             │
│  COMMISSION PERIOD (Thời gian hoa hồng)                                    │
│  ├── 5 tháng kể từ khi Referee đạt mốc 5 đơn                               │
│  └── Sau 5 tháng, Referrer không còn nhận hoa hồng                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Milestone Types

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MILESTONE TYPES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  WITH_REFERRER (Có người giới thiệu)                                       │
│  ├── Áp dụng cho Referee có nhập mã giới thiệu                             │
│  ├── Milestones: 5, 10, 80, 300 đơn                                        │
│  └── Cả Referee và Referrer đều nhận thưởng                                │
│                                                                             │
│  WITHOUT_REFERRER (Không có người giới thiệu)                              │
│  ├── Áp dụng cho user không nhập mã giới thiệu                             │
│  ├── Milestones: 80, 300 đơn (chỉ nâng tier)                               │
│  └── Chỉ Referee nhận thưởng nâng tier                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Cấu Trúc Database

### 3.1 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATABASE SCHEMA                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐         ┌─────────────────────┐                   │
│  │       users         │         │   milestone_config  │                   │
│  ├─────────────────────┤         ├─────────────────────┤                   │
│  │ id                  │         │ id                  │                   │
│  │ keycloak_id         │         │ milestone_type      │                   │
│  │ username            │         │ orders_required     │                   │
│  │ email               │         │ referee_bonus       │                   │
│  │ referral_code ──────┼────┐    │ referrer_bonus      │                   │
│  │ referred_by ────────┼──┐ │    │ new_tier            │                   │
│  │ user_level          │  │ │    │ commission_months   │                   │
│  │ total_completed_    │  │ │    │ activates_referral  │                   │
│  │   orders            │  │ │    │ is_active           │                   │
│  │ referral_activated_ │  │ │    └─────────────────────┘                   │
│  │   at                │  │ │                                              │
│  │ referral_expires_at │  │ │    ┌─────────────────────┐                   │
│  └─────────────────────┘  │ │    │  referral_reward    │                   │
│           │               │ │    ├─────────────────────┤                   │
│           │               │ └───▶│ user_id ────────────┼───┐               │
│           │               │      │ referrer_id ────────┼───┤               │
│           │               └─────▶│ reward_type         │   │               │
│           │                      │ milestone           │   │               │
│           │                      │ amount              │   │               │
│           │                      │ status              │   │               │
│           │                      └─────────────────────┘   │               │
│           │                                                │               │
│           │              ┌─────────────────────┐           │               │
│           │              │ referrer_commission │           │               │
│           │              ├─────────────────────┤           │               │
│           └─────────────▶│ referrer_id ────────┼───────────┤               │
│                          │ referee_id ─────────┼───────────┘               │
│                          │ source_order_id     │                           │
│                          │ original_commission │                           │
│                          │ commission_rate     │                           │
│                          │ commission_amount   │                           │
│                          │ expires_at          │                           │
│                          │ status              │                           │
│                          └─────────────────────┘                           │
│                                                                             │
│           ┌─────────────────────┐     ┌─────────────────────┐              │
│           │    transactions     │     │    user_wallet      │              │
│           ├─────────────────────┤     ├─────────────────────┤              │
│           │ id                  │     │ id                  │              │
│           │ user_id             │     │ user_id             │              │
│           │ wallet_id           │     │ balance             │              │
│           │ type                │     │ pending_balance     │              │
│           │ amount              │     │ locked_balance      │              │
│           │ source_type ◀───────┼──┐  │ total_earned        │              │
│           │ source_id           │  │  │ total_withdrawn     │              │
│           │ status              │  │  └─────────────────────┘              │
│           └─────────────────────┘  │                                       │
│                                    │                                       │
│    source_type values:             │                                       │
│    ├── ORDER                       │                                       │
│    ├── MILESTONE_BONUS ────────────┘                                       │
│    ├── REFERRER_BONUS                                                      │
│    ├── REFERRER_COMMISSION                                                 │
│    ├── PAYOUT                                                              │
│    └── ADJUSTMENT                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Bảng milestone_config

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `milestone_type` | VARCHAR(20) | WITH_REFERRER / WITHOUT_REFERRER |
| `orders_required` | INT | Số đơn cần hoàn thành |
| `referee_bonus` | DECIMAL | Thưởng cho Referee (VND) |
| `referrer_bonus` | DECIMAL | Thưởng cho Referrer (VND) |
| `new_tier` | VARCHAR(20) | Tier mới (NORMAL/VIP/SUPER) |
| `commission_months` | INT | Số tháng hoa hồng |
| `activates_referral` | BOOLEAN | Có kích hoạt hoa hồng không |
| `is_active` | BOOLEAN | Milestone còn hiệu lực |

**Dữ liệu mẫu:**

```sql
-- WITH_REFERRER milestones
INSERT INTO milestone_config VALUES
(1, 'WITH_REFERRER', 5, 10000, 20000, NULL, 5, TRUE, TRUE),   -- Mốc 5 đơn
(2, 'WITH_REFERRER', 10, 20000, NULL, NULL, NULL, FALSE, TRUE), -- Mốc 10 đơn
(3, 'WITH_REFERRER', 80, NULL, NULL, 'VIP', NULL, FALSE, TRUE), -- Mốc 80 đơn
(4, 'WITH_REFERRER', 300, NULL, NULL, 'SUPER', NULL, FALSE, TRUE); -- Mốc 300 đơn

-- WITHOUT_REFERRER milestones (chỉ nâng tier)
INSERT INTO milestone_config VALUES
(5, 'WITHOUT_REFERRER', 80, NULL, NULL, 'VIP', NULL, FALSE, TRUE),
(6, 'WITHOUT_REFERRER', 300, NULL, NULL, 'SUPER', NULL, FALSE, TRUE);
```

---

## 4. Luồng Hoạt Động

### 4.1 Luồng Nhập Mã Giới Thiệu

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LUỒNG NHẬP MÃ GIỚI THIỆU                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────┐                                                               │
│  │ Referee │                                                               │
│  │ (User)  │                                                               │
│  └────┬────┘                                                               │
│       │                                                                    │
│       │ 1. Nhập mã "CBXYZ123"                                              │
│       ▼                                                                    │
│  ┌─────────────────────────────────────────┐                               │
│  │       SetReferralCodeUseCase            │                               │
│  └────────────────┬────────────────────────┘                               │
│                   │                                                        │
│       ┌───────────┴───────────┐                                            │
│       ▼                       ▼                                            │
│  ┌─────────────┐        ┌─────────────┐                                    │
│  │ 2. Validate │        │ 3. Check    │                                    │
│  │ - Mã tồn tại│        │ - Chưa có   │                                    │
│  │ - Không tự  │        │   referrer  │                                    │
│  │   refer     │        │ - Referrer  │                                    │
│  │             │        │   active    │                                    │
│  └──────┬──────┘        └──────┬──────┘                                    │
│         │                      │                                           │
│         └──────────┬───────────┘                                           │
│                    ▼                                                       │
│         ┌─────────────────────┐                                            │
│         │ 4. Lưu referred_by  │                                            │
│         │    vào user table   │                                            │
│         └─────────────────────┘                                            │
│                    │                                                       │
│                    ▼                                                       │
│         ┌─────────────────────┐                                            │
│         │ 5. Trả về response  │                                            │
│         │  - Tên Referrer     │                                            │
│         │    (đã che)         │                                            │
│         └─────────────────────┘                                            │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║ LƯU Ý: Chỉ có thể nhập mã 1 lần duy nhất                              ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Luồng Xử Lý Milestone

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LUỒNG XỬ LÝ MILESTONE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐                                                       │
│  │ Order PAID      │  ◀── Webhook từ Shopee/Platform                       │
│  └────────┬────────┘                                                       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │              ProcessReferralMilestoneUseCase                    │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────┐                                                       │
│  │ 1. Tăng counter │                                                       │
│  │ total_completed │                                                       │
│  │ _orders + 1     │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────────────────────────────┐                               │
│  │ 2. Xác định Milestone Type              │                               │
│  │                                         │                               │
│  │  User có referrer? ──────┬───────────── │                               │
│  │                    YES   │       NO     │                               │
│  │                    ▼     │       ▼      │                               │
│  │          WITH_REFERRER   │  WITHOUT_    │                               │
│  │          (5,10,80,300)   │  REFERRER    │                               │
│  │                          │  (80,300)    │                               │
│  └───────────────┬──────────┴──────────────┘                               │
│                  │                                                         │
│                  ▼                                                         │
│  ┌───────────────────────────────────────────────────────────────┐         │
│  │ 3. Kiểm tra có đạt milestone không                            │         │
│  │                                                               │         │
│  │    SELECT * FROM milestone_config                             │         │
│  │    WHERE milestone_type = ? AND orders_required = ?           │         │
│  │    AND is_active = TRUE                                       │         │
│  └─────────────────────────────────────────────────────────────────┘       │
│                  │                                                         │
│        ┌─────────┴─────────┐                                               │
│        │ Đạt milestone?    │                                               │
│        └────────┬──────────┘                                               │
│           YES   │       NO                                                 │
│           ▼     │       ▼                                                  │
│  ┌──────────────┴───┐  ┌────────────────┐                                  │
│  │ Xử lý milestone  │  │ Chỉ save user  │                                  │
│  │ (xem chi tiết    │  │ (tăng counter) │                                  │
│  │  bên dưới)       │  └────────────────┘                                  │
│  └──────────────────┘                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Chi Tiết Xử Lý Milestone

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CHI TIẾT XỬ LÝ MILESTONE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                    processMilestone()                           │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│                             │                                              │
│       ┌─────────────────────┼─────────────────────┐                        │
│       ▼                     ▼                     ▼                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │ 1. Kích hoạt │    │ 2. Nâng Tier │    │ 3. Thưởng   │                  │
│  │    Referral  │    │              │    │    Bonus    │                  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                  │
│         │                   │                   │                          │
│         ▼                   ▼                   ▼                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐          │
│  │ Chỉ ở mốc 5  │    │ Mốc 80: VIP  │    │ Referee Bonus        │          │
│  │ đơn, set:    │    │ Mốc 300:     │    │ ├── Thêm vào wallet  │          │
│  │ - activated  │    │   SUPER      │    │ ├── Tạo transaction  │          │
│  │   At = now   │    │              │    │ └── Tạo reward record│          │
│  │ - expiresAt  │    │              │    │                      │          │
│  │   = now +    │    │              │    │ Referrer Bonus       │          │
│  │   5 months   │    │              │    │ ├── Thêm vào wallet  │          │
│  └──────────────┘    └──────────────┘    │ ├── Tạo transaction  │          │
│                                          │ └── Tạo reward record│          │
│                                          └──────────────────────┘          │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║                  MILESTONE 5 ĐƠN (WITH_REFERRER)                      ║ │
│  ║                                                                       ║ │
│  ║  ┌─────────────────────────────────────────────────────────────────┐  ║ │
│  ║  │ 1. Kích hoạt referral (activates_referral = TRUE)               │  ║ │
│  ║  │    - referral_activated_at = NOW()                              │  ║ │
│  ║  │    - referral_expires_at = NOW() + 5 months                     │  ║ │
│  ║  │                                                                 │  ║ │
│  ║  │ 2. Referee nhận 10,000 VND                                      │  ║ │
│  ║  │    - Thêm vào wallet.balance                                    │  ║ │
│  ║  │    - Tạo transaction (source_type = MILESTONE_BONUS)            │  ║ │
│  ║  │    - Tạo referral_reward record                                 │  ║ │
│  ║  │                                                                 │  ║ │
│  ║  │ 3. Referrer nhận 20,000 VND                                     │  ║ │
│  ║  │    - Thêm vào wallet.balance                                    │  ║ │
│  ║  │    - Tạo transaction (source_type = REFERRER_BONUS)             │  ║ │
│  ║  │    - Tạo referral_reward record                                 │  ║ │
│  ║  └─────────────────────────────────────────────────────────────────┘  ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4 Luồng Tính Hoa Hồng Referrer

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LUỒNG TÍNH HOA HỒNG REFERRER (5%)                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐                                                       │
│  │ Order PAID      │  ◀── Webhook từ Shopee                                │
│  │ (Cashback từ    │                                                       │
│  │  Shopee: 100k)  │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │           CalculateReferrerCommissionUseCase                    │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                     KIỂM TRA ĐIỀU KIỆN                          │       │
│  │                                                                 │       │
│  │  ┌─────────────────────────────────────────────────────────┐    │       │
│  │  │ 1. Referee có referrer?          → có referred_by      │    │       │
│  │  │ 2. Referral đã kích hoạt?        → referral_activated  │    │       │
│  │  │ 3. Còn trong thời hạn hoa hồng?  → NOW() < expires_at  │    │       │
│  │  │ 4. Referrer còn active?          → status = ACTIVE     │    │       │
│  │  │ 5. Chưa có commission cho order? → không duplicate     │    │       │
│  │  └─────────────────────────────────────────────────────────┘    │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│           │                                                                │
│           │ Tất cả điều kiện thỏa mãn                                      │
│           ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                    TÍNH HOA HỒNG                                │       │
│  │                                                                 │       │
│  │   Commission = Original Commission × 5%                        │       │
│  │                                                                 │       │
│  │   Ví dụ:                                                       │       │
│  │   ┌─────────────────────────────────────────────────────────┐  │       │
│  │   │ Cashback từ Shopee:     100,000 VND                     │  │       │
│  │   │ Hoa hồng Referrer:      100,000 × 5% = 5,000 VND        │  │       │
│  │   │                                                         │  │       │
│  │   │ LƯU Ý: App trả hoa hồng này, không trừ từ Referee      │  │       │
│  │   └─────────────────────────────────────────────────────────┘  │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                  TẠO COMMISSION RECORD                          │       │
│  │                                                                 │       │
│  │   referrer_commission:                                          │       │
│  │   ├── referrer_id = 123                                         │       │
│  │   ├── referee_id = 456                                          │       │
│  │   ├── source_order_id = 789                                     │       │
│  │   ├── original_commission = 100,000                             │       │
│  │   ├── commission_rate = 5.00                                    │       │
│  │   ├── commission_amount = 5,000                                 │       │
│  │   ├── expires_at = [referee's referral_expires_at]              │       │
│  │   └── status = CONFIRMED                                        │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║ TIMELINE:                                                             ║ │
│  ║                                                                       ║ │
│  ║  [Referee đạt 5 đơn]──────────[5 tháng sau]                          ║ │
│  ║         │                          │                                  ║ │
│  ║         ▼                          ▼                                  ║ │
│  ║  referral_activated_at      referral_expires_at                      ║ │
│  ║         │                          │                                  ║ │
│  ║         ├──────────────────────────┤                                  ║ │
│  ║         │    HOA HỒNG 5% ACTIVE    │                                  ║ │
│  ║         └──────────────────────────┘                                  ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Hệ Thống Milestone

### 5.1 Bảng Tổng Hợp Milestone

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BẢNG MILESTONE CHI TIẾT                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  WITH_REFERRER (Người dùng có nhập mã giới thiệu)                          │
│  ═══════════════════════════════════════════════════════════════════════   │
│                                                                             │
│  ┌────────┬──────────────────┬──────────────────┬──────────────────────┐   │
│  │ Mốc    │ Referee nhận     │ Referrer nhận    │ Đặc biệt             │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 5 đơn  │ 10,000 VND       │ 20,000 VND       │ Kích hoạt hoa hồng   │   │
│  │        │                  │                  │ 5 tháng              │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 10 đơn │ 20,000 VND       │ -                │ -                    │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 80 đơn │ -                │ -                │ Nâng cấp lên VIP     │   │
│  │        │                  │                  │ (Cashback 83%)       │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 300 đơn│ -                │ -                │ Nâng cấp lên SUPER   │   │
│  │        │                  │                  │ (Cashback 85%)       │   │
│  └────────┴──────────────────┴──────────────────┴──────────────────────┘   │
│                                                                             │
│                                                                             │
│  WITHOUT_REFERRER (Người dùng không nhập mã giới thiệu)                    │
│  ═══════════════════════════════════════════════════════════════════════   │
│                                                                             │
│  ┌────────┬──────────────────┬──────────────────┬──────────────────────┐   │
│  │ Mốc    │ Referee nhận     │ Referrer nhận    │ Đặc biệt             │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 80 đơn │ -                │ N/A              │ Nâng cấp lên VIP     │   │
│  ├────────┼──────────────────┼──────────────────┼──────────────────────┤   │
│  │ 300 đơn│ -                │ N/A              │ Nâng cấp lên SUPER   │   │
│  └────────┴──────────────────┴──────────────────┴──────────────────────┘   │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║ User Level và Cashback Rate:                                          ║ │
│  ║ ├── NORMAL: 80% cashback (mặc định)                                   ║ │
│  ║ ├── VIP:    83% cashback (đạt 80 đơn)                                 ║ │
│  ║ └── SUPER:  85% cashback (đạt 300 đơn)                                ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Ví Dụ Thực Tế

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VÍ DỤ THỰC TẾ                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User A (Referrer) giới thiệu User B (Referee)                             │
│  ════════════════════════════════════════════                              │
│                                                                             │
│  Ngày 1: B nhập mã của A                                                   │
│  ├── B.referred_by = "A's code"                                            │
│  └── Chưa có gì xảy ra                                                     │
│                                                                             │
│  Ngày 15: B hoàn thành đơn thứ 5 (cashback 50,000 VND)                     │
│  ├── B đạt milestone 5 đơn                                                 │
│  ├── B nhận +10,000 VND vào wallet                                         │
│  ├── A nhận +20,000 VND vào wallet                                         │
│  ├── Kích hoạt referral: B.referral_activated_at = NOW                     │
│  ├── B.referral_expires_at = NOW + 5 tháng                                 │
│  └── A bắt đầu nhận 5% từ cashback của B                                   │
│      └── A nhận +2,500 VND (5% × 50,000)                                   │
│                                                                             │
│  Ngày 30: B hoàn thành thêm đơn (cashback 100,000 VND)                     │
│  └── A nhận +5,000 VND (5% × 100,000)                                      │
│                                                                             │
│  Ngày 45: B hoàn thành đơn thứ 10                                          │
│  ├── B đạt milestone 10 đơn                                                │
│  └── B nhận +20,000 VND vào wallet                                         │
│                                                                             │
│  Tháng 6 (sau 5 tháng kể từ ngày kích hoạt):                               │
│  ├── B.referral_expires_at đã qua                                          │
│  ├── A không còn nhận 5% từ B nữa                                          │
│  └── B vẫn tiếp tục nhận cashback bình thường                              │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║ TỔNG KẾT SAU 5 THÁNG:                                                 ║ │
│  ║                                                                       ║ │
│  ║ User A (Referrer) nhận được:                                          ║ │
│  ║ ├── Thưởng milestone 5 đơn:     20,000 VND                            ║ │
│  ║ └── Hoa hồng 5% (nhiều đơn):    ~50,000 VND (giả sử B mua nhiều)      ║ │
│  ║ ───────────────────────────────────────────                           ║ │
│  ║ TỔNG:                           ~70,000 VND                            ║ │
│  ║                                                                       ║ │
│  ║ User B (Referee) nhận được:                                           ║ │
│  ║ ├── Thưởng milestone 5 đơn:     10,000 VND                            ║ │
│  ║ ├── Thưởng milestone 10 đơn:    20,000 VND                            ║ │
│  ║ └── Cashback từ các đơn:        (theo tỷ lệ 80%)                      ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Tính Toán Hoa Hồng

### 6.1 Công Thức

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CÔNG THỨC TÍNH HOA HỒNG                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║                                                                       ║ │
│  ║   Hoa hồng Referrer = Cashback của Referee × 5%                       ║ │
│  ║                                                                       ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
│  Ví dụ tính toán:                                                          │
│  ─────────────────                                                         │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │ Đơn hàng của Referee:                                              │    │
│  │ ├── Giá trị đơn:              1,000,000 VND                        │    │
│  │ ├── Tỷ lệ commission Shopee:  8%                                   │    │
│  │ ├── Commission từ Shopee:     80,000 VND                           │    │
│  │ ├── User Level:               NORMAL (80%)                         │    │
│  │ └── Cashback cho Referee:     80,000 × 80% = 64,000 VND            │    │
│  │                                                                    │    │
│  │ Hoa hồng cho Referrer:                                             │    │
│  │ └── 64,000 × 5% = 3,200 VND                                        │    │
│  │                                                                    │    │
│  │ ⚠️  LƯU Ý: App trả 3,200 VND này, không trừ từ Referee            │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│                                                                             │
│  Điều kiện để Referrer nhận hoa hồng:                                      │
│  ─────────────────────────────────────                                     │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  ✓ Referee đã nhập mã của Referrer                                 │    │
│  │  ✓ Referee đã đạt 5 đơn (kích hoạt referral)                       │    │
│  │  ✓ Còn trong thời hạn 5 tháng                                      │    │
│  │  ✓ Referrer có status = ACTIVE                                     │    │
│  │  ✓ Đơn hàng chưa được tính commission trước đó                     │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Trạng Thái Commission

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TRẠNG THÁI REFERRER COMMISSION                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────┐      ┌────────────────┐      ┌────────────────┐        │
│  │    PENDING     │ ───▶ │   CONFIRMED    │ ───▶ │     PAID       │        │
│  └────────────────┘      └────────────────┘      └────────────────┘        │
│         │                                               ▲                  │
│         │                                               │                  │
│         ▼                                               │                  │
│  ┌────────────────┐                           ┌────────────────┐           │
│  │   CANCELLED    │                           │    EXPIRED     │           │
│  └────────────────┘                           └────────────────┘           │
│                                                                             │
│  ╔═══════════════════════════════════════════════════════════════════════╗ │
│  ║ PENDING:    Đơn hàng đang chờ xác nhận từ Shopee                      ║ │
│  ║ CONFIRMED:  Đơn đã được xác nhận, commission sẵn sàng                 ║ │
│  ║ PAID:       Commission đã được cộng vào wallet Referrer              ║ │
│  ║ CANCELLED:  Đơn bị hủy, không có commission                          ║ │
│  ║ EXPIRED:    Quá thời hạn hoa hồng                                     ║ │
│  ╚═══════════════════════════════════════════════════════════════════════╝ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. API Endpoints

### 7.1 Referral APIs

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REFERRAL API ENDPOINTS                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  POST /api/referral/set-code                                               │
│  ─────────────────────────────                                             │
│  Nhập mã giới thiệu                                                        │
│                                                                             │
│  Request:                                                                  │
│  ┌────────────────────────────────────────┐                                │
│  │ {                                      │                                │
│  │   "referralCode": "CBXYZ123"           │                                │
│  │ }                                      │                                │
│  └────────────────────────────────────────┘                                │
│                                                                             │
│  Response (Success):                                                       │
│  ┌────────────────────────────────────────┐                                │
│  │ {                                      │                                │
│  │   "success": true,                     │                                │
│  │   "referralCode": "CBXYZ123",          │                                │
│  │   "referrerId": 123,                   │                                │
│  │   "referrerName": "Ng******"           │                                │
│  │ }                                      │                                │
│  └────────────────────────────────────────┘                                │
│                                                                             │
│  ─────────────────────────────────────────────────────────────────────     │
│                                                                             │
│  GET /api/referral/my-referrals                                            │
│  ──────────────────────────────                                            │
│  Lấy danh sách người mình đã giới thiệu                                    │
│                                                                             │
│  Response:                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ {                                                                   │   │
│  │   "myReferralCode": "CBXYZ123",                                     │   │
│  │   "totalReferrals": 5,                                              │   │
│  │   "activeReferrals": 3,                                             │   │
│  │   "referrals": [                                                    │   │
│  │     {                                                               │   │
│  │       "name": "Ng******",                                           │   │
│  │       "email": "ng***@gmail.com",                                   │   │
│  │       "completedOrders": 15,                                        │   │
│  │       "referralActivated": true,                                    │   │
│  │       "referralActivatedAt": "2024-01-15T10:30:00",                 │   │
│  │       "referralExpiresAt": "2024-06-15T10:30:00",                   │   │
│  │       "withinCommissionPeriod": true,                               │   │
│  │       "status": "ACTIVE"                                            │   │
│  │     }                                                               │   │
│  │   ]                                                                 │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ─────────────────────────────────────────────────────────────────────     │
│                                                                             │
│  GET /api/referral/my-referrer                                             │
│  ─────────────────────────────                                             │
│  Xem thông tin người đã giới thiệu mình                                    │
│                                                                             │
│  Response:                                                                 │
│  ┌────────────────────────────────────────┐                                │
│  │ {                                      │                                │
│  │   "hasReferrer": true,                 │                                │
│  │   "referrerCode": "CBXYZ123",          │                                │
│  │   "referrerName": "Ng******",          │                                │
│  │   "referralActivated": true,           │                                │
│  │   "referralActivatedAt": "...",        │                                │
│  │   "referralExpiresAt": "...",          │                                │
│  │   "daysRemaining": 120                 │                                │
│  │ }                                      │                                │
│  └────────────────────────────────────────┘                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Cấu Trúc Code

### 8.1 Package Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CẤU TRÚC PACKAGE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  cashbee-domain/                                                           │
│  ├── model/                                                                │
│  │   ├── User.java                    # Domain model với referral fields   │
│  │   ├── MilestoneConfig.java         # Cấu hình milestone                 │
│  │   ├── ReferralReward.java          # Thưởng referral                    │
│  │   └── ReferrerCommission.java      # Hoa hồng từ referee                │
│  ├── enums/                                                                │
│  │   ├── MilestoneType.java           # WITH_REFERRER, WITHOUT_REFERRER    │
│  │   ├── TransactionSourceType.java   # ORDER, MILESTONE_BONUS, etc.       │
│  │   └── UserLevel.java               # NORMAL, VIP, SUPER                 │
│  └── repository/                                                           │
│      ├── MilestoneConfigRepository.java                                    │
│      ├── ReferralRewardRepository.java                                     │
│      └── ReferrerCommissionRepository.java                                 │
│                                                                             │
│  cashbee-application/                                                      │
│  ├── usecase/referral/                                                     │
│  │   ├── SetReferralCodeUseCase.java          # Nhập mã giới thiệu         │
│  │   ├── ProcessReferralMilestoneUseCase.java # Xử lý milestone            │
│  │   ├── CalculateReferrerCommissionUseCase.java # Tính hoa hồng 5%        │
│  │   ├── GetMyReferralsUseCase.java           # Lấy DS người mình GT       │
│  │   └── GetMyReferrerUseCase.java            # Xem người GT mình          │
│  ├── dto/referral/                                                         │
│  │   ├── SetReferralCodeCommand.java                                       │
│  │   ├── SetReferralCodeResponse.java                                      │
│  │   ├── GetMyReferralsResponse.java                                       │
│  │   └── ReferralValidationResult.java                                     │
│  └── service/                                                              │
│      └── ReferralCodeValidator.java           # Validate mã giới thiệu     │
│                                                                             │
│  cashbee-infrastructure/                                                   │
│  ├── persistence/entity/                                                   │
│  │   ├── UserJpaEntity.java                                                │
│  │   ├── MilestoneConfigJpaEntity.java                                     │
│  │   ├── ReferralRewardJpaEntity.java                                      │
│  │   └── ReferrerCommissionJpaEntity.java                                  │
│  ├── persistence/repository/                                               │
│  │   └── *JpaRepository.java                                               │
│  ├── persistence/mapper/                                                   │
│  │   └── *Mapper.java                                                      │
│  └── persistence/adapter/                                                  │
│      └── *RepositoryAdapter.java                                           │
│                                                                             │
│  cashbee-presentation/                                                     │
│  ├── controller/                                                           │
│  │   └── ReferralController.java              # REST API endpoints         │
│  └── resources/db/changelog/                                               │
│      └── 044-update-milestone-referral-system.xml  # DB migration          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Luồng Xử Lý Code

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LUỒNG XỬ LÝ CODE CHI TIẾT                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    KHI ĐƠN HÀNG ĐƯỢC THANH TOÁN                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  1. OrderWebhookController nhận webhook từ Shopee                          │
│     │                                                                      │
│     ▼                                                                      │
│  2. UpdateAffiliateOrderUseCase cập nhật trạng thái đơn                    │
│     │                                                                      │
│     │ (Khi status = PAID)                                                  │
│     ▼                                                                      │
│  3. Gọi song song 2 use case:                                              │
│     │                                                                      │
│     ├──▶ ProcessReferralMilestoneUseCase                                   │
│     │    │                                                                 │
│     │    ├── Tăng total_completed_orders                                   │
│     │    ├── Kiểm tra milestone từ milestone_config                        │
│     │    ├── Nếu đạt milestone:                                            │
│     │    │   ├── Kích hoạt referral (nếu mốc 5)                            │
│     │    │   ├── Nâng tier (nếu mốc 80/300)                                │
│     │    │   └── Thêm bonus vào wallet                                     │
│     │    └── Tạo transaction với source_type = MILESTONE_BONUS             │
│     │                                                                      │
│     └──▶ CalculateReferrerCommissionUseCase                                │
│          │                                                                 │
│          ├── Kiểm tra điều kiện hoa hồng                                   │
│          ├── Tính commission = cashback × 5%                               │
│          └── Tạo referrer_commission record                                │
│                                                                             │
│  ─────────────────────────────────────────────────────────────────────     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     KHI USER NHẬP MÃ GIỚI THIỆU                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  1. ReferralController.setReferralCode() nhận request                      │
│     │                                                                      │
│     ▼                                                                      │
│  2. SetReferralCodeUseCase.execute()                                       │
│     │                                                                      │
│     ├── ReferralCodeValidator.checkNotAlreadyReferred()                    │
│     │   └── Kiểm tra user chưa có referrer                                 │
│     │                                                                      │
│     ├── ReferralCodeValidator.validateOrThrow()                            │
│     │   ├── Normalize code to UPPERCASE                                    │
│     │   ├── Kiểm tra code tồn tại                                          │
│     │   ├── Kiểm tra không tự refer                                        │
│     │   └── Kiểm tra referrer active                                       │
│     │                                                                      │
│     ├── user.setReferredBy(code)                                           │
│     └── userRepository.save(user)                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phụ Lục

### A. Các Trường Hợp Đặc Biệt

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CÁC TRƯỜNG HỢP ĐẶC BIỆT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. User đã có referrer cố nhập lại mã khác                                │
│     → Throw ReferralCodeAlreadySetException                                │
│     → Message: "Bạn đã nhập mã giới thiệu trước đó"                        │
│                                                                             │
│  2. User nhập mã của chính mình                                            │
│     → Throw SelfReferralException                                          │
│     → Message: "Không thể sử dụng mã giới thiệu của chính bạn"             │
│                                                                             │
│  3. Mã giới thiệu không tồn tại                                            │
│     → Throw InvalidReferralCodeException                                   │
│     → Message: "Mã giới thiệu không tồn tại"                               │
│                                                                             │
│  4. Referrer bị ban/inactive                                               │
│     → Throw InvalidReferralCodeException                                   │
│     → Message: "Mã giới thiệu không còn hoạt động"                         │
│                                                                             │
│  5. Referee đạt milestone nhưng Referrer đã bị ban                         │
│     → Referee vẫn nhận bonus                                               │
│     → Referrer KHÔNG nhận bonus                                            │
│                                                                             │
│  6. Hoa hồng khi referral đã hết hạn                                       │
│     → Không tạo commission                                                 │
│     → Log: "Referral period expired"                                       │
│                                                                             │
│  7. Duplicate commission cho cùng 1 order                                  │
│     → Kiểm tra existsBySourceOrderId trước khi tạo                         │
│     → Không tạo duplicate                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### B. Câu Hỏi Thường Gặp (FAQ)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FAQ                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Q: Tại sao thời hạn hoa hồng là 5 tháng?                                  │
│  A: Được cấu hình trong milestone_config (commission_months = 5).          │
│     Có thể thay đổi bằng cách update database.                             │
│                                                                             │
│  Q: Ai trả hoa hồng 5% cho Referrer?                                       │
│  A: CashBee trả từ quỹ marketing, không trừ từ Referee.                    │
│                                                                             │
│  Q: Nếu Referee không bao giờ đạt 5 đơn?                                   │
│  A: Referrer không bao giờ nhận được gì từ Referee đó.                     │
│                                                                             │
│  Q: Có thể thay đổi referrer sau khi đã nhập?                              │
│  A: Không. Mỗi user chỉ được nhập mã 1 lần duy nhất.                       │
│                                                                             │
│  Q: Thưởng milestone có thời hạn không?                                    │
│  A: Không. Thưởng được cộng ngay vào wallet khi đạt milestone.             │
│                                                                             │
│  Q: Nếu user không có referrer thì sao?                                    │
│  A: Họ vẫn được nâng tier ở mốc 80 và 300 đơn,                             │
│     nhưng không có thưởng bonus ở mốc 5 và 10.                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

**Tài liệu này được tạo tự động và cập nhật lần cuối: 2024-12-24**

**Version: 2.0 (Milestone System Update)**
