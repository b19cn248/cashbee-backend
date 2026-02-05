# Hướng dẫn tích hợp: Hệ thống Mốc Thưởng (Milestone Rewards)

> **Phiên bản:** 1.1
> **Cập nhật:** 2026-02-05
> **Dành cho:** Mobile App Team

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Cấu trúc Milestones](#2-cấu-trúc-milestones)
3. [API Endpoints](#3-api-endpoints)
4. [Thiết kế UI/UX](#4-thiết-kế-uiux)
5. [Nội dung hiển thị](#5-nội-dung-hiển-thị)
6. [Notifications](#6-notifications)
7. [Edge Cases](#7-edge-cases)
8. [FAQ](#8-faq)
9. [Hệ thống Hoa hồng Referrer (Commission)](#9-hệ-thống-hoa-hồng-referrer-commission)

---

## 1. Tổng quan

### 1.1. Mô tả chiến dịch

Hệ thống Mốc Thưởng khuyến khích người dùng:
- **Sử dụng mã giới thiệu** khi đăng ký (được thưởng nhiều hơn)
- **Tiếp tục mua sắm** để đạt các mốc thưởng
- **Giới thiệu bạn bè** để nhận hoa hồng

### 1.2. Hai loại người dùng

| Loại | Điều kiện | Tổng thưởng tiềm năng |
|------|-----------|----------------------|
| **CÓ MÃ GIỚI THIỆU** | Nhập mã khi đăng ký | **75,000₫** + VIP/SUPER |
| **KHÔNG CÓ MÃ** | Không nhập mã | 35,000₫ + VIP/SUPER |

### 1.3. Điều kiện đơn hàng hợp lệ

```
✅ Đơn hàng được tính khi:
   - Giá trị sản phẩm > 100,000₫
   - Trạng thái: CONFIRMED hoặc PAID

❌ Đơn hàng KHÔNG được tính:
   - Giá trị sản phẩm ≤ 100,000₫
   - Trạng thái: PENDING, CANCELLED, REJECTED
```

---

## 2. Cấu trúc Milestones

### 2.1. Người dùng CÓ MÃ GIỚI THIỆU (WITH_REFERRER)

| Mốc | Đơn cần | Thưởng Bạn (B) | Thưởng Người GT (A) | Tier | Ghi chú |
|-----|---------|----------------|---------------------|------|---------|
| 1 | 1 | **+10,000₫** | +20,000₫ | - | Kích hoạt hoa hồng 12 tháng |
| 2 | 5 | **+15,000₫** | +10,000₫ | - | - |
| 3 | 20 | **+20,000₫** | - | - | Pre-VIP |
| 4 | 50 | - | - | **VIP** | Cashback 83% |
| 5 | 150 | **+30,000₫** | - | **SUPER** | Cashback 85% |

**Tổng:** 75,000₫ tiền thưởng + Upgrade VIP → SUPER

### 2.2. Người dùng KHÔNG CÓ MÃ (WITHOUT_REFERRER)

| Mốc | Đơn cần | Thưởng | Tier | Ghi chú |
|-----|---------|--------|------|---------|
| 1 | 1 | **+5,000₫** | - | Welcome bonus |
| 2 | 20 | **+10,000₫** | - | Milestone bonus |
| 3 | 50 | - | **VIP** | Cashback 83% |
| 4 | 150 | **+20,000₫** | **SUPER** | Cashback 85% |

**Tổng:** 35,000₫ tiền thưởng + Upgrade VIP → SUPER

### 2.3. Bảng so sánh lợi ích

```
┌─────────────────────────────────────────────────────────────┐
│                    SO SÁNH LỢI ÍCH                          │
├─────────────────────────────────────────────────────────────┤
│                    │ Có mã GT  │ Không có mã │   Chênh lệch │
├────────────────────┼───────────┼─────────────┼──────────────┤
│ Thưởng đơn đầu     │  10,000₫  │    5,000₫   │    +100%     │
│ Tổng thưởng        │  75,000₫  │   35,000₫   │    +114%     │
│ Số mốc thưởng      │     5     │      4      │      +1      │
│ Hoa hồng 12 tháng  │     ✅    │      ❌     │      -       │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. API Endpoints

### 3.1. Lấy thông tin referral & milestones

```http
GET /api/referral/stats
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "referralCode": "ABC123",
    "referredBy": "XYZ789",
    "totalCompletedOrders": 12,
    "referralActivatedAt": "2026-01-15T10:30:00",
    "referralExpiresAt": "2027-01-15T10:30:00",
    "userLevel": "NORMAL",
    "totalReferrals": 5,
    "activatedReferrals": 3,
    "milestones": [
      {
        "ordersRequired": 1,
        "refereeBonus": 10000,
        "referrerBonus": 20000,
        "newTier": null,
        "activatesReferral": true,
        "commissionMonths": 12,
        "isCompleted": true,
        "completedAt": "2026-01-10T14:20:00"
      },
      {
        "ordersRequired": 5,
        "refereeBonus": 15000,
        "referrerBonus": 10000,
        "newTier": null,
        "activatesReferral": false,
        "commissionMonths": 0,
        "isCompleted": true,
        "completedAt": "2026-01-20T09:15:00"
      },
      {
        "ordersRequired": 20,
        "refereeBonus": 20000,
        "referrerBonus": 0,
        "newTier": null,
        "activatesReferral": false,
        "commissionMonths": 0,
        "isCompleted": false,
        "completedAt": null
      },
      {
        "ordersRequired": 50,
        "refereeBonus": 0,
        "referrerBonus": 0,
        "newTier": "VIP",
        "activatesReferral": false,
        "commissionMonths": 0,
        "isCompleted": false,
        "completedAt": null
      },
      {
        "ordersRequired": 150,
        "refereeBonus": 30000,
        "referrerBonus": 0,
        "newTier": "SUPER",
        "activatesReferral": false,
        "commissionMonths": 0,
        "isCompleted": false,
        "completedAt": null
      }
    ],
    "rewards": [
      {
        "id": 101,
        "rewardType": "MILESTONE_BONUS",
        "milestone": 1,
        "amount": 10000,
        "newTier": null,
        "status": "GRANTED",
        "grantedAt": "2026-01-10T14:20:00"
      },
      {
        "id": 102,
        "rewardType": "MILESTONE_BONUS",
        "milestone": 5,
        "amount": 15000,
        "newTier": null,
        "status": "GRANTED",
        "grantedAt": "2026-01-20T09:15:00"
      }
    ],
    "nextMilestone": {
      "ordersRequired": 20,
      "ordersRemaining": 8,
      "refereeBonus": 20000,
      "newTier": null
    }
  }
}
```

### 3.2. Lấy lịch sử thưởng

```http
GET /api/referral/rewards
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "rewardType": "MILESTONE_BONUS",
      "milestone": 1,
      "amount": 10000,
      "newTier": null,
      "status": "GRANTED",
      "grantedAt": "2026-01-10T14:20:00",
      "paidAt": "2026-01-25T16:00:00"
    }
  ]
}
```

---

## 4. Thiết kế UI/UX

### 4.1. Màn hình Milestone Progress

```
┌─────────────────────────────────────────────────────────────┐
│                    MỐC THƯỞNG CỦA BẠN                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Đơn hoàn thành: 12/20                                     │
│   ████████████████████░░░░░░░░░░  60%                       │
│                                                             │
│   Còn 8 đơn nữa để nhận +20,000₫                           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ✅ Mốc 1 (1 đơn)     +10,000₫        ĐÃ NHẬN             │
│   ✅ Mốc 2 (5 đơn)     +15,000₫        ĐÃ NHẬN             │
│   🎯 Mốc 3 (20 đơn)    +20,000₫        8 đơn nữa           │
│   ⭐ Mốc 4 (50 đơn)    VIP (83%)       38 đơn nữa          │
│   👑 Mốc 5 (150 đơn)   +30k + SUPER    138 đơn nữa         │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│   💰 Tổng đã nhận: 25,000₫                                  │
│   🎁 Còn lại: 50,000₫ + VIP + SUPER                        │
└─────────────────────────────────────────────────────────────┘
```

### 4.2. Component cho từng mốc

```
┌─────────────────────────────────────────────────────────────┐
│  MỐC ĐÃ HOÀN THÀNH                                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────┐                                                    │
│  │ ✅  │  Mốc 1: Đơn hàng đầu tiên                          │
│  └─────┘  +10,000₫ đã được cộng vào ví                      │
│           Hoàn thành: 10/01/2026                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  MỐC ĐANG HƯỚNG TỚI                                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────┐                                                    │
│  │ 🎯  │  Mốc 3: 20 đơn hàng                                │
│  └─────┘  Phần thưởng: +20,000₫                             │
│           Tiến độ: 12/20 (còn 8 đơn)                        │
│           ████████████░░░░░░░░  60%                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  MỐC CHƯA MỞ KHÓA                                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────┐                                                    │
│  │ 🔒  │  Mốc 4: 50 đơn hàng                                │
│  └─────┘  Phần thưởng: Lên hạng VIP (Cashback 83%)         │
│           Còn 38 đơn nữa để mở khóa                         │
└─────────────────────────────────────────────────────────────┘
```

### 4.3. Màn hình so sánh (cho user chưa có mã)

```
┌─────────────────────────────────────────────────────────────┐
│              🎁 NHẬP MÃ GIỚI THIỆU                          │
│                 NHẬN THÊM 40,000₫!                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────┐    ┌─────────────────┐               │
│   │   KHÔNG CÓ MÃ   │    │    CÓ MÃ GT     │               │
│   ├─────────────────┤    ├─────────────────┤               │
│   │ Đơn 1:   +5k    │    │ Đơn 1:  +10k ⭐ │               │
│   │ Đơn 20: +10k    │    │ Đơn 5:  +15k ⭐ │               │
│   │ Đơn 50:  VIP    │    │ Đơn 20: +20k ⭐ │               │
│   │ Đơn 150:+20k    │    │ Đơn 50:  VIP    │               │
│   │                 │    │ Đơn 150:+30k ⭐ │               │
│   ├─────────────────┤    ├─────────────────┤               │
│   │ Tổng: 35,000₫   │    │ Tổng: 75,000₫  │               │
│   └─────────────────┘    └─────────────────┘               │
│                                                             │
│   [     NHẬP MÃ NGAY ĐỂ NHẬN +40K     ]                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Nội dung hiển thị

### 5.1. Mô tả từng mốc - CÓ MÃ GIỚI THIỆU

| Mốc | Tiêu đề | Mô tả ngắn | Mô tả chi tiết |
|-----|---------|------------|----------------|
| 1 | Đơn hàng đầu tiên | +10,000₫ | Chúc mừng đơn hàng đầu tiên! Bạn nhận 10,000₫ và người giới thiệu bạn nhận 20,000₫. Hoa hồng giới thiệu được kích hoạt trong 12 tháng. |
| 5 | 5 đơn hàng | +15,000₫ | Tuyệt vời! Bạn đã hoàn thành 5 đơn hàng và nhận thêm 15,000₫. Người giới thiệu bạn cũng nhận 10,000₫. |
| 20 | 20 đơn hàng | +20,000₫ | Bạn đang trên đường trở thành VIP! Nhận ngay 20,000₫ cho nỗ lực mua sắm. |
| 50 | Lên hạng VIP | VIP (83%) | Chúc mừng bạn đã lên hạng VIP! Từ giờ bạn nhận cashback 83% cho mọi đơn hàng. |
| 150 | Lên hạng SUPER | +30,000₫ + SUPER | Wow! Bạn là thành viên SUPER với cashback 85%! Nhận thêm 30,000₫ tri ân. |

### 5.2. Mô tả từng mốc - KHÔNG CÓ MÃ

| Mốc | Tiêu đề | Mô tả ngắn | Mô tả chi tiết |
|-----|---------|------------|----------------|
| 1 | Chào mừng | +5,000₫ | Chào mừng bạn đến với CashBee! Nhận ngay 5,000₫ cho đơn hàng đầu tiên. |
| 20 | 20 đơn hàng | +10,000₫ | Cảm ơn bạn đã đồng hành! Nhận 10,000₫ thưởng cho 20 đơn hàng. |
| 50 | Lên hạng VIP | VIP (83%) | Chúc mừng bạn đã lên hạng VIP! Cashback tăng lên 83%. |
| 150 | Lên hạng SUPER | +20,000₫ + SUPER | Bạn là thành viên SUPER! Cashback 85% + thưởng 20,000₫. |

### 5.3. Thông điệp khuyến khích

```javascript
// Khi user gần đạt mốc
const encourageMessages = {
  1: "Còn {remaining} đơn nữa thôi! Mua sắm ngay để nhận thưởng!",
  2: "Sắp đạt mốc {milestone} đơn rồi! Chỉ còn {remaining} đơn!",
  5: "Wow, gần tới rồi! {remaining} đơn nữa để nhận {reward}!",
  10: "Chỉ còn {remaining} đơn để lên {tier}! Cố lên nào!"
};

// Khi user đạt mốc
const celebrateMessages = {
  MILESTONE_BONUS: "🎉 Chúc mừng! Bạn nhận được {amount}₫!",
  TIER_UPGRADE: "⭐ Tuyệt vời! Bạn đã lên hạng {tier}!",
  ACTIVATION: "✅ Hoa hồng giới thiệu đã được kích hoạt {months} tháng!"
};
```

---

## 6. Notifications

### 6.1. Push Notifications

#### Khi đạt mốc thưởng tiền

```json
{
  "type": "MILESTONE_BONUS",
  "title": "🎉 Bạn vừa nhận thưởng!",
  "body": "Chúc mừng đạt mốc {milestone} đơn! +{amount}₫ đã được cộng vào ví.",
  "data": {
    "screen": "WALLET",
    "milestone": 5,
    "amount": 15000
  }
}
```

**Ví dụ cụ thể:**
- Mốc 1: "🎉 Bạn vừa nhận thưởng! Chúc mừng đơn hàng đầu tiên! +10,000₫ đã được cộng vào ví."
- Mốc 5: "🎉 Bạn vừa nhận thưởng! Chúc mừng đạt mốc 5 đơn! +15,000₫ đã được cộng vào ví."
- Mốc 20: "🎉 Bạn vừa nhận thưởng! Chúc mừng đạt mốc 20 đơn! +20,000₫ đã được cộng vào ví."

#### Khi lên hạng

```json
{
  "type": "TIER_UPGRADE",
  "title": "⭐ Chúc mừng lên hạng {tier}!",
  "body": "Bạn đã lên hạng {tier}! Cashback tăng lên {cashbackRate}% cho mọi đơn hàng.",
  "data": {
    "screen": "PROFILE",
    "newTier": "VIP",
    "cashbackRate": 83
  }
}
```

**Ví dụ cụ thể:**
- VIP: "⭐ Chúc mừng lên hạng VIP! Cashback tăng lên 83% cho mọi đơn hàng."
- SUPER: "👑 Chúc mừng lên hạng SUPER! Cashback tăng lên 85% cho mọi đơn hàng."

#### Khi gần đạt mốc (Optional - scheduled notification)

```json
{
  "type": "MILESTONE_REMINDER",
  "title": "🎯 Sắp đạt mốc thưởng!",
  "body": "Còn {remaining} đơn nữa để nhận {reward}. Mua sắm ngay!",
  "data": {
    "screen": "MILESTONE_PROGRESS",
    "nextMilestone": 20,
    "remaining": 3,
    "reward": "20,000₫"
  }
}
```

#### Cho người giới thiệu (Referrer)

```json
{
  "type": "REFERRER_BONUS",
  "title": "💰 Bạn nhận hoa hồng!",
  "body": "{refereeName} vừa đạt mốc {milestone} đơn. Bạn nhận +{amount}₫!",
  "data": {
    "screen": "WALLET",
    "refereeId": 12345,
    "milestone": 1,
    "amount": 20000
  }
}
```

### 6.2. In-App Notifications

```javascript
// Notification model
{
  id: "notif_123",
  type: "MILESTONE_ACHIEVED",
  title: "Chúc mừng đạt mốc 5 đơn!",
  message: "Bạn đã nhận 15,000₫ vào ví. Còn 15 đơn nữa để đạt mốc tiếp theo!",
  imageUrl: "/assets/milestone-5.png",
  actionUrl: "/wallet",
  actionText: "Xem ví",
  createdAt: "2026-01-20T09:15:00Z",
  isRead: false
}
```

### 6.3. Toast/Snackbar Messages

```javascript
// Hiển thị ngay sau khi đơn được confirm
const toastMessages = {
  orderConfirmed: "✅ Đơn hàng được xác nhận! ({current}/{next} đơn)",
  milestoneAchieved: "🎉 +{amount}₫ đã cộng vào ví!",
  tierUpgraded: "⭐ Lên hạng {tier}! Cashback {rate}%",
  almostThere: "🔥 Còn {remaining} đơn để nhận {reward}!"
};
```

---

## 7. Edge Cases

### 7.1. Đơn không đủ điều kiện

**Tình huống:** User đặt đơn 80,000₫ (< 100,000₫)

**Xử lý UI:**
```
┌─────────────────────────────────────────────────────────────┐
│  ⚠️ Đơn hàng này không được tính vào mốc thưởng            │
│                                                             │
│  Lý do: Giá trị đơn hàng dưới 100,000₫                     │
│                                                             │
│  💡 Mẹo: Đơn hàng từ 100,000₫ trở lên sẽ được tính         │
│     vào tiến độ mốc thưởng của bạn.                        │
└─────────────────────────────────────────────────────────────┘
```

### 7.2. User chưa nhập mã nhưng muốn nhập sau

**Tình huống:** User đăng ký không có mã, sau đó muốn nhập

**Quy tắc:**
- ❌ Không cho phép nhập mã sau khi đã đăng ký
- ✅ Hiển thị thông báo giải thích

**UI:**
```
┌─────────────────────────────────────────────────────────────┐
│  ℹ️ Không thể thêm mã giới thiệu                            │
│                                                             │
│  Mã giới thiệu chỉ có thể nhập khi đăng ký tài khoản.      │
│                                                             │
│  Nhưng đừng lo! Bạn vẫn có thể:                            │
│  • Nhận thưởng từ các mốc đơn hàng                         │
│  • Lên hạng VIP/SUPER                                       │
│  • Giới thiệu bạn bè để nhận hoa hồng                      │
└─────────────────────────────────────────────────────────────┘
```

### 7.3. Referrer không tồn tại

**Tình huống:** User nhập mã không hợp lệ

**Xử lý:**
- Validate mã trước khi cho đăng ký
- Hiển thị lỗi rõ ràng

```
┌─────────────────────────────────────────────────────────────┐
│  ❌ Mã giới thiệu không hợp lệ                              │
│                                                             │
│  Vui lòng kiểm tra lại mã hoặc liên hệ người giới thiệu.   │
│                                                             │
│  [Nhập lại]  [Bỏ qua, tiếp tục đăng ký]                    │
└─────────────────────────────────────────────────────────────┘
```

### 7.4. Commission đã hết hạn

**Tình huống:** Sau 12 tháng, referrer không còn nhận commission

**UI cho Referrer:**
```
┌─────────────────────────────────────────────────────────────┐
│  ℹ️ Hoa hồng từ {refereeName} đã kết thúc                   │
│                                                             │
│  Thời gian nhận hoa hồng 12 tháng đã hết.                  │
│  Tổng hoa hồng đã nhận: 150,000₫                           │
│                                                             │
│  💡 Giới thiệu thêm bạn bè để tiếp tục nhận hoa hồng!      │
└─────────────────────────────────────────────────────────────┘
```

### 7.5. Đã đạt tất cả các mốc

**Tình huống:** User SUPER với 150+ đơn

**UI:**
```
┌─────────────────────────────────────────────────────────────┐
│  👑 BẠN ĐÃ ĐẠT TẤT CẢ CÁC MỐC!                             │
│                                                             │
│  Cảm ơn bạn đã đồng hành cùng CashBee!                     │
│                                                             │
│  ✅ Tổng thưởng đã nhận: 75,000₫                           │
│  ✅ Hạng hiện tại: SUPER (Cashback 85%)                    │
│                                                             │
│  💡 Tiếp tục mua sắm để nhận cashback 85%                  │
│  💡 Giới thiệu bạn bè để nhận hoa hồng không giới hạn!    │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. FAQ

### Q1: Tại sao đơn của tôi không được tính?

**A:** Đơn hàng chỉ được tính khi:
- Giá trị sản phẩm > 100,000₫
- Trạng thái đã xác nhận (CONFIRMED) hoặc đã thanh toán (PAID)

Đơn hàng bị huỷ, từ chối hoặc giá trị dưới 100,000₫ sẽ không được tính.

### Q2: Khi nào tôi nhận được tiền thưởng?

**A:** Tiền thưởng được cộng **ngay lập tức** vào ví khi đơn hàng được xác nhận và bạn đạt mốc. Tiền trong ví có thể rút về tài khoản ngân hàng khi đạt mức tối thiểu.

### Q3: Người giới thiệu tôi có nhận được gì không?

**A:** Có! Khi bạn đạt mốc 1 và 5 đơn, người giới thiệu sẽ nhận:
- Mốc 1: +20,000₫
- Mốc 5: +10,000₫
- Ngoài ra còn nhận % hoa hồng từ mỗi đơn hàng của bạn trong 12 tháng

### Q4: Tôi có thể nhập mã giới thiệu sau khi đăng ký không?

**A:** Không. Mã giới thiệu chỉ có thể nhập trong quá trình đăng ký tài khoản. Tuy nhiên, bạn vẫn có thể giới thiệu người khác và nhận hoa hồng.

### Q5: Hoa hồng 12 tháng hoạt động như thế nào?

**A:** Sau khi người được giới thiệu hoàn thành đơn đầu tiên, hoa hồng sẽ được kích hoạt. Trong 12 tháng tiếp theo, bạn sẽ nhận một phần % từ mỗi đơn hàng của họ.

### Q6: VIP và SUPER khác gì nhau?

**A:**
| Hạng | Cashback | Điều kiện |
|------|----------|-----------|
| NORMAL | 80% | Mặc định |
| VIP | 83% | 50 đơn hàng |
| SUPER | 85% | 150 đơn hàng |

### Q7: Commission hoạt động như thế nào?

**A:** Khi bạn giới thiệu người mới và họ hoàn thành đơn đầu tiên (>100k), hệ thống commission sẽ được kích hoạt. Trong 12 tháng tiếp theo, bạn sẽ nhận một % hoa hồng từ mỗi đơn hàng của họ.

**Công thức:** `Commission = Hoa hồng Shopee × Tỷ lệ tier%`

Ví dụ: Nếu bạn ở tier BRONZE (5%) và referee mua đơn có hoa hồng Shopee 10,000₫ → Bạn nhận 500₫.

### Q8: Làm sao để tăng % commission?

**A:** Giới thiệu nhiều người hơn! % commission tăng theo số người đã kích hoạt:

| Tier | Điều kiện | Commission |
|------|-----------|------------|
| BRONZE | 0+ người | 5% |
| SILVER | 5+ người | 7% (+40%) |
| GOLD | 20+ người | 10% (+100%) |

### Q9: Commission có bị trừ từ cashback của người được giới thiệu không?

**A:** **KHÔNG!** Commission do CashBee chi trả hoàn toàn. Người được giới thiệu vẫn nhận đủ 100% cashback của họ. Đây là cách CashBee cảm ơn bạn đã giới thiệu người dùng mới.

### Q10: Sau 12 tháng thì sao?

**A:** Sau 12 tháng, bạn sẽ không còn nhận commission từ referee đó nữa. Tuy nhiên:
- Bạn vẫn giữ nguyên tier đã đạt được
- Tiếp tục giới thiệu người mới để có thêm nguồn thu nhập
- Không có giới hạn số người bạn có thể giới thiệu

### Q11: BRONZE, SILVER, GOLD khác gì với NORMAL, VIP, SUPER?

**A:** Đây là hai hệ thống tier riêng biệt:

| Hệ thống | Tier | Áp dụng cho | Dựa trên |
|----------|------|-------------|----------|
| **User Tier** | NORMAL/VIP/SUPER | Cashback khi mua hàng | Số đơn của bản thân |
| **Referrer Tier** | BRONZE/SILVER/GOLD | Commission từ referee | Số người giới thiệu |

Một user có thể là VIP (83% cashback) đồng thời là GOLD referrer (10% commission).

---

## 9. Hệ thống Hoa hồng Referrer (Commission)

Ngoài thưởng mốc, người giới thiệu (Referrer) còn nhận **hoa hồng từ mỗi đơn hàng** của người được giới thiệu (Referee) trong thời gian 12 tháng.

### 9.1. Điều kiện kích hoạt Commission

```
┌─────────────────────────────────────────────────────────────┐
│              ĐIỀU KIỆN KÍCH HOẠT COMMISSION                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Referee (người nhập mã) phải:                              │
│  ✅ Hoàn thành 1 ĐƠN HÀNG ĐẦU TIÊN (giá trị >100,000₫)     │
│                                                             │
│  → Khi đạt mốc 1 đơn:                                       │
│    • Referee nhận +10,000₫                                  │
│    • Referrer nhận +20,000₫                                 │
│    • Commission được KÍCH HOẠT trong 12 THÁNG               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Lưu ý quan trọng:**
- Commission chỉ được kích hoạt sau khi referee hoàn thành đơn đầu tiên
- Thời hạn 12 tháng tính từ ngày kích hoạt, **KHÔNG phải** từ ngày đăng ký
- Sau 12 tháng, referrer không còn nhận commission từ referee đó nữa

### 9.2. Công thức tính Commission

```
Commission = Hoa_hồng_từ_Shopee × Tỷ_lệ_Commission%

Ví dụ:
┌─────────────────────────────────────────────────────────────┐
│  Đơn hàng Shopee: 1,000,000₫                                │
│  Hoa hồng từ Shopee: 10,000₫                                │
│                                                             │
│  Referee nhận cashback (80%): 8,000₫                        │
│  Referrer nhận commission (5%): 10,000 × 5% = 500₫         │
│                                                             │
│  ⚠️ Commission do CashBee trả, KHÔNG trừ từ referee        │
└─────────────────────────────────────────────────────────────┘
```

### 9.3. Hệ thống Tier - Commission Rate tăng theo số người giới thiệu

Referrer có thể **tăng % commission** bằng cách giới thiệu nhiều người hơn:

| Tier | Điều kiện | Commission Rate | Bonus/Activation | Màu đề xuất |
|------|-----------|-----------------|------------------|-------------|
| **BRONZE** | 0+ activated referrals | **5%** | 0₫ | `#CD7F32` |
| **SILVER** | 5+ activated referrals | **7%** | +5,000₫ | `#C0C0C0` |
| **GOLD** | 20+ activated referrals | **10%** | +10,000₫ | `#FFD700` |

```
┌─────────────────────────────────────────────────────────────┐
│                    REFERRER TIER PROGRESSION                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  BRONZE ───(5 người)───► SILVER ───(20 người)───► GOLD     │
│    5%                      7%                      10%      │
│                                                             │
│  "Activated referral" = Referee đã hoàn thành 1 đơn >100k  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Ví dụ so sánh thu nhập:**

| Scenario | BRONZE (5%) | SILVER (7%) | GOLD (10%) |
|----------|-------------|-------------|------------|
| Commission gốc 10,000₫ | 500₫ | 700₫ | 1,000₫ |
| Commission gốc 50,000₫ | 2,500₫ | 3,500₫ | 5,000₫ |
| **Tăng so với BRONZE** | - | **+40%** | **+100%** |

### 9.4. Bonus khi có người kích hoạt (Tier SILVER & GOLD)

Khi referrer đạt tier SILVER hoặc GOLD, họ sẽ nhận **bonus thêm** mỗi khi có referee mới kích hoạt:

```
┌─────────────────────────────────────────────────────────────┐
│  TIER SILVER: Mỗi referee kích hoạt = +5,000₫ bonus        │
│  TIER GOLD:   Mỗi referee kích hoạt = +10,000₫ bonus       │
│                                                             │
│  (Bonus này cộng thêm vào thưởng mốc 20,000₫ thông thường) │
└─────────────────────────────────────────────────────────────┘
```

### 9.5. Các điều kiện để nhận Commission

Mỗi khi referee mua hàng, hệ thống sẽ kiểm tra:

| # | Điều kiện | Mô tả |
|---|-----------|-------|
| 1 | Referee có referrer | Referee phải đã nhập mã giới thiệu khi đăng ký |
| 2 | Referral đã kích hoạt | Referee đã hoàn thành ít nhất 1 đơn >100k |
| 3 | Còn trong thời hạn 12 tháng | Chưa quá 12 tháng kể từ ngày kích hoạt |
| 4 | Referrer còn hoạt động | Tài khoản referrer không bị khóa/xóa |
| 5 | Hoa hồng gốc > 0 | Đơn hàng phải có hoa hồng từ Shopee |
| 6 | Chưa tính commission | Tránh tính trùng cho cùng 1 đơn |

### 9.6. API Response mở rộng cho Commission

**GET /api/referral/stats** - Response bổ sung thông tin commission:

```json
{
  "success": true,
  "data": {
    "referralCode": "ABC123",
    "referredBy": "XYZ789",

    // Thông tin Referee (nếu có referrer)
    "referralActivatedAt": "2026-01-15T10:30:00",
    "referralExpiresAt": "2027-01-15T10:30:00",
    "isWithinCommissionPeriod": true,
    "daysUntilExpiry": 245,

    // Thông tin Referrer Tier
    "referrerTier": "SILVER",
    "referrerCommissionRate": 7.00,
    "totalActivatedReferrals": 8,
    "nextTier": {
      "name": "GOLD",
      "requiredReferrals": 20,
      "remainingReferrals": 12,
      "commissionRate": 10.00,
      "bonusPerActivation": 10000
    },

    // Thống kê Commission (cho Referrer)
    "commissionStats": {
      "totalEarned": 125000,
      "totalPending": 15000,
      "totalPaid": 110000,
      "thisMonthEarned": 25000,
      "activeReferees": 5
    },

    // Danh sách referees
    "referees": [
      {
        "id": 101,
        "username": "user_a",
        "activatedAt": "2026-01-10T14:20:00",
        "expiresAt": "2027-01-10T14:20:00",
        "isActive": true,
        "totalCommissionEarned": 50000,
        "totalOrders": 25
      }
    ]
  }
}
```

### 9.7. Thiết kế UI cho Referrer Dashboard

#### Màn hình chính - Referrer Stats

```
┌─────────────────────────────────────────────────────────────┐
│                  💰 HOA HỒNG GIỚI THIỆU                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Tier hiện tại: 🥈 SILVER (7%)                            │
│   ████████████████░░░░░░░░░░  8/20 người                   │
│   Còn 12 người nữa để lên GOLD (10%)                       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   📊 THỐNG KÊ HOA HỒNG                                      │
│   ┌─────────────┬─────────────┬─────────────┐              │
│   │  Tháng này  │   Đã nhận   │  Chờ duyệt  │              │
│   │   25,000₫   │  110,000₫   │   15,000₫   │              │
│   └─────────────┴─────────────┴─────────────┘              │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   👥 NGƯỜI ĐÃ GIỚI THIỆU (8 người active)                  │
│                                                             │
│   ┌─────────────────────────────────────────────────┐      │
│   │ 👤 user_a        Còn 245 ngày    +50,000₫      │      │
│   │ 👤 user_b        Còn 180 ngày    +35,000₫      │      │
│   │ 👤 user_c        Còn 120 ngày    +25,000₫      │      │
│   │ 👤 user_d        ⚠️ Hết hạn      +15,000₫      │      │
│   └─────────────────────────────────────────────────┘      │
│                                                             │
│   [  CHIA SẺ MÃ GIỚI THIỆU: ABC123  ]                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### Tier Progress Card

```
┌─────────────────────────────────────────────────────────────┐
│  🥈 SILVER TIER                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Commission Rate: 7%                                       │
│   Bonus mỗi kích hoạt: +5,000₫                             │
│                                                             │
│   Tiến độ lên GOLD:                                         │
│   ████████████████░░░░░░░░░░  8/20 người                   │
│                                                             │
│   🎯 Lên GOLD để nhận:                                      │
│   • Commission tăng lên 10% (+43%)                         │
│   • Bonus +10,000₫ mỗi người kích hoạt                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### Commission History Item

```
┌─────────────────────────────────────────────────────────────┐
│  💸 Hoa hồng từ user_a                                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Đơn hàng: #SH123456789                                    │
│   Hoa hồng gốc: 10,000₫                                     │
│   Commission (7%): +700₫                                    │
│   Trạng thái: ✅ Đã thanh toán                              │
│   Ngày: 05/02/2026                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 9.8. Notifications cho Commission

#### Khi nhận commission từ đơn hàng

```json
{
  "type": "REFERRER_COMMISSION",
  "title": "💸 Bạn nhận hoa hồng!",
  "body": "Đơn hàng của {refereeName} mang về cho bạn +{amount}₫",
  "data": {
    "screen": "COMMISSION_HISTORY",
    "refereeId": 101,
    "orderId": 12345,
    "amount": 700
  }
}
```

#### Khi lên tier

```json
{
  "type": "REFERRER_TIER_UPGRADE",
  "title": "🎉 Chúc mừng lên hạng {tierName}!",
  "body": "Commission của bạn tăng lên {rate}%! Bonus +{bonus}₫ mỗi người kích hoạt.",
  "data": {
    "screen": "REFERRER_DASHBOARD",
    "newTier": "SILVER",
    "commissionRate": 7.00,
    "bonusPerActivation": 5000
  }
}
```

**Ví dụ cụ thể:**
- SILVER: "🎉 Chúc mừng lên hạng SILVER! Commission của bạn tăng lên 7%! Bonus +5,000₫ mỗi người kích hoạt."
- GOLD: "🏆 Chúc mừng lên hạng GOLD! Commission của bạn tăng lên 10%! Bonus +10,000₫ mỗi người kích hoạt."

#### Khi referee kích hoạt (Milestone 1)

```json
{
  "type": "REFEREE_ACTIVATED",
  "title": "🎊 {refereeName} đã kích hoạt!",
  "body": "Bạn nhận +{milestoneBonus}₫ thưởng mốc và bắt đầu nhận {rate}% hoa hồng trong 12 tháng.",
  "data": {
    "screen": "REFERRER_DASHBOARD",
    "refereeId": 101,
    "milestoneBonus": 20000,
    "commissionRate": 7.00,
    "activationBonus": 5000
  }
}
```

#### Khi sắp hết hạn commission

```json
{
  "type": "COMMISSION_EXPIRING_SOON",
  "title": "⏰ Sắp hết hạn hoa hồng!",
  "body": "Còn {days} ngày nhận hoa hồng từ {refereeName}. Nhắc họ mua sắm nhé!",
  "data": {
    "screen": "REFEREE_DETAIL",
    "refereeId": 101,
    "daysRemaining": 7
  }
}
```

### 9.9. Ví dụ thu nhập thực tế

**Scenario: Referrer GOLD với 25 referees active**

```
┌─────────────────────────────────────────────────────────────┐
│               TÍNH TOÁN THU NHẬP PASSIVE                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Giả sử:                                                    │
│  • 25 referees active                                       │
│  • Mỗi referee mua trung bình 10 đơn/tháng                 │
│  • Hoa hồng trung bình mỗi đơn: 5,000₫                     │
│  • Tier: GOLD (10%)                                         │
│                                                             │
│  Tính toán:                                                 │
│  Commission/đơn = 5,000₫ × 10% = 500₫                      │
│  Tổng đơn/tháng = 25 người × 10 đơn = 250 đơn              │
│  Thu nhập/tháng = 250 × 500₫ = 125,000₫                    │
│                                                             │
│  ────────────────────────────────────────────               │
│  THU NHẬP PASSIVE: ~125,000₫/tháng                         │
│  ────────────────────────────────────────────               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 9.10. Tóm tắt hệ thống Commission

| Câu hỏi | Trả lời |
|---------|---------|
| **Bao nhiêu đơn để kích hoạt commission?** | **1 đơn** (>100,000₫) |
| **Thời hạn nhận commission?** | **12 tháng** từ ngày kích hoạt |
| **% commission mặc định?** | **5%** (BRONZE) |
| **Commission có tăng không?** | **CÓ** - 5% → 7% → 10% |
| **Điều kiện tăng %?** | 5+ activated referrals → 7%, 20+ → 10% |
| **Bonus khi lên tier?** | SILVER: +5k/activation, GOLD: +10k/activation |
| **Ai trả commission?** | **CashBee** (không trừ từ referee) |
| **Có giới hạn số referee?** | **Không** - càng nhiều càng tốt |

---

## Phụ lục

### A. Bảng màu đề xuất

#### Milestone Colors

| Element | Màu | Hex Code |
|---------|-----|----------|
| Mốc hoàn thành | Xanh lá | `#22C55E` |
| Mốc đang tiến | Vàng | `#F59E0B` |
| Mốc chưa mở | Xám | `#9CA3AF` |
| Bonus amount | Đỏ/Cam | `#EF4444` |

#### User Tier Colors (Cashback)

| Tier | Màu | Hex Code | Gradient |
|------|-----|----------|----------|
| NORMAL | Xám | `#6B7280` | - |
| VIP | Tím | `#8B5CF6` | `#8B5CF6` → `#A78BFA` |
| SUPER | Vàng kim | `#F59E0B` | `#F59E0B` → `#FBBF24` |

#### Referrer Tier Colors (Commission)

| Tier | Màu | Hex Code | Gradient |
|------|-----|----------|----------|
| BRONZE | Đồng | `#CD7F32` | `#CD7F32` → `#D4A574` |
| SILVER | Bạc | `#C0C0C0` | `#A8A8A8` → `#D4D4D4` |
| GOLD | Vàng | `#FFD700` | `#FFD700` → `#FFC107` |

### B. Icons đề xuất

#### Milestone Icons

| Element | Icon | Alternative |
|---------|------|-------------|
| Mốc hoàn thành | ✅ | checkmark-circle-filled |
| Mốc tiếp theo | 🎯 | target |
| Mốc chưa mở | 🔒 | lock |
| Tiền thưởng | 💰 | coin-stack |

#### User Tier Icons (Cashback)

| Tier | Icon | Alternative |
|------|------|-------------|
| NORMAL | 👤 | user |
| VIP | ⭐ | star-filled |
| SUPER | 👑 | crown |

#### Referrer Tier Icons (Commission)

| Tier | Icon | Alternative |
|------|------|-------------|
| BRONZE | 🥉 | medal-bronze |
| SILVER | 🥈 | medal-silver |
| GOLD | 🥇 | medal-gold |

#### Commission Icons

| Element | Icon | Alternative |
|---------|------|-------------|
| Hoa hồng | 💸 | money-wings |
| Commission history | 📜 | receipt |
| Referrer dashboard | 👥 | users-group |
| Active referee | 🟢 | dot-green |
| Expired referee | 🔴 | dot-red |
| Pending commission | ⏳ | hourglass |

### C. Animation đề xuất

1. **Khi đạt mốc:** Confetti animation + bounce effect
2. **Khi lên hạng:** Glow effect + scale up animation
3. **Progress bar:** Smooth fill animation khi update
4. **Notification:** Slide in from top + vibration

---

**Liên hệ hỗ trợ:** Backend Team - [backend@cashbee.vn]
