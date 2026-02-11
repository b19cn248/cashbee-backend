# REFERRAL SYSTEM OPTIMIZATION - MIGRATION PLAN

## 1. TONG QUAN

### 1.1. Muc tieu
- Tang ty le chuyen doi nguoi dung moi
- Tang viral coefficient (nguoi dung chia se nhieu hon)
- Giu chan nguoi dung lau hon voi nhieu moc thuong

### 1.2. Thay doi chinh
| # | Thay doi | Muc do uu tien |
|---|----------|----------------|
| 1 | Them First Order Bonus (1 don) | CAO |
| 2 | Giam moc kich hoat 5 -> 3 don | CAO |
| 3 | Them cac moc trung gian (20, 40, 150 don) | TRUNG BINH |
| 4 | Tang thoi gian commission 5 -> 12 thang | TRUNG BINH |
| 5 | Them Referrer Tier System | THAP |

---

## 2. PHASE 1: DATABASE MIGRATION (Uu tien CAO)

### 2.1. Milestone Config - Truoc va Sau

#### TRUOC (Hien tai):
```
WITH_REFERRER:
  5 don  -> B +10k, A +20k, Kich hoat 5% (5 thang)
  10 don -> B +20k
  80 don -> VIP
  300 don -> SUPER

WITHOUT_REFERRER:
  80 don -> VIP
  300 don -> SUPER
```

#### SAU (De xuat):
```
WITH_REFERRER:
  1 don  -> B +5k, A +5k (MOI - First Order Bonus)
  3 don  -> B +10k, A +20k, Kich hoat 5% (12 thang) (GIAM tu 5 don)
  10 don -> B +20k
  20 don -> B +15k (MOI)
  40 don -> B +20k (MOI)
  80 don -> VIP
  150 don -> B +50k (MOI)
  300 don -> SUPER

WITHOUT_REFERRER:
  1 don  -> B +3k (MOI - Welcome Bonus)
  20 don -> B +10k (MOI)
  40 don -> B +15k (MOI)
  80 don -> VIP
  150 don -> B +30k (MOI)
  300 don -> SUPER
```

### 2.2. SQL Migration Script

```sql
-- =====================================================
-- MIGRATION: Referral System Optimization v2.0
-- Date: 2026-01-14
-- Author: CashBee Team
-- =====================================================

-- STEP 1: Backup existing data
CREATE TABLE milestone_config_backup_20260114 AS
SELECT * FROM milestone_config;

-- STEP 2: Deactivate old milestone at 5 orders (will be replaced by 3 orders)
UPDATE milestone_config
SET is_active = 0,
    updated_at = NOW(),
    description = CONCAT('[DEPRECATED] ', description)
WHERE milestone_type = 'WITH_REFERRER'
  AND orders_required = 5;

-- STEP 3: Insert new milestones for WITH_REFERRER

-- 3.1. First Order Bonus (1 don)
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITH_REFERRER', 1, 5000.00, 5000.00,
    '', 0, 0, 'First Order Bonus: B +5k, A +5k', 1
);

-- 3.2. Activation Milestone (3 don - giam tu 5)
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITH_REFERRER', 3, 10000.00, 20000.00,
    '', 12, 1, 'Referral activation: B +10k, A +20k, A receives 5% commission for 12 months', 1
);

-- 3.3. Milestone 20 don
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITH_REFERRER', 20, 15000.00, 0.00,
    '', 0, 0, 'Milestone bonus: B +15k', 1
);

-- 3.4. Milestone 40 don (truoc VIP)
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITH_REFERRER', 40, 20000.00, 0.00,
    '', 0, 0, 'Pre-VIP milestone: B +20k', 1
);

-- 3.5. Milestone 150 don (truoc SUPER)
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITH_REFERRER', 150, 50000.00, 0.00,
    '', 0, 0, 'Pre-SUPER milestone: B +50k', 1
);

-- STEP 4: Insert new milestones for WITHOUT_REFERRER

-- 4.1. Welcome Bonus (1 don)
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITHOUT_REFERRER', 1, 3000.00, 0.00,
    '', 0, 0, 'Welcome Bonus: +3k for first order', 1
);

-- 4.2. Milestone 20 don
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITHOUT_REFERRER', 20, 10000.00, 0.00,
    '', 0, 0, 'Milestone bonus: +10k', 1
);

-- 4.3. Milestone 40 don
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITHOUT_REFERRER', 40, 15000.00, 0.00,
    '', 0, 0, 'Pre-VIP milestone: +15k', 1
);

-- 4.4. Milestone 150 don
INSERT INTO milestone_config (
    milestone_type, orders_required, referee_bonus, referrer_bonus,
    new_tier, commission_months, activates_referral, description, is_active
) VALUES (
    'WITHOUT_REFERRER', 150, 30000.00, 0.00,
    '', 0, 0, 'Pre-SUPER milestone: +30k', 1
);

-- STEP 5: Verify migration
SELECT
    milestone_type,
    orders_required,
    referee_bonus,
    referrer_bonus,
    new_tier,
    commission_months,
    activates_referral,
    is_active,
    description
FROM milestone_config
WHERE is_active = 1
ORDER BY milestone_type, orders_required;
```

---

## 3. PHASE 2: CODE CHANGES

### 3.1. Khong can thay doi code cho Phase 1
- `ProcessReferralMilestoneUseCase` da duoc thiet ke flexible
- Chi can INSERT/UPDATE data trong `milestone_config`
- Code tu dong doc tu database va xu ly

### 3.2. Verify code logic hien tai

```java
// ProcessReferralMilestoneUseCase.java - Line 94-99
// Da co logic tim milestone theo orders_required
Optional<MilestoneConfig> milestoneConfigOpt = milestoneConfigRepository
    .findActiveByMilestoneTypeAndOrdersRequired(milestoneType, completedOrders);

if (milestoneConfigOpt.isPresent()) {
    processMilestone(user, referrer, milestoneConfigOpt.get());
}
```

### 3.3. Luu y quan trong
- Code hien tai chi xu ly **DUNG** so don = orders_required
- Neu user nhay tu 0 -> 5 don (skip moc 1, 3), chi nhan thuong moc 5
- Can xem xet co muon "retroactive rewards" khong

---

## 4. PHASE 3: REFERRER TIER SYSTEM (Uu tien THAP)

### 4.1. Thiet ke bang moi: `referrer_tier_config`

```sql
CREATE TABLE referrer_tier_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier_name VARCHAR(30) NOT NULL,           -- 'BRONZE', 'SILVER', 'GOLD'
    min_referrals INT NOT NULL,               -- So nguoi gioi thieu toi thieu
    commission_rate DECIMAL(5,2) NOT NULL,    -- Ty le commission (5.00, 7.00, 10.00)
    bonus_per_activation DECIMAL(19,2) DEFAULT 0, -- Thuong them moi lan kich hoat
    description VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_tier_name (tier_name),
    KEY idx_min_referrals (min_referrals)
);

-- Insert default tiers
INSERT INTO referrer_tier_config (tier_name, min_referrals, commission_rate, bonus_per_activation, description) VALUES
('BRONZE', 0, 5.00, 0, 'Default tier: 5% commission'),
('SILVER', 5, 7.00, 5000, 'Silver tier: 7% commission + 5k bonus per activation'),
('GOLD', 20, 10.00, 10000, 'Gold tier: 10% commission + 10k bonus per activation');
```

### 4.2. Them cot vao bang `user`

```sql
ALTER TABLE user
ADD COLUMN referrer_tier VARCHAR(30) DEFAULT 'BRONZE' AFTER user_level,
ADD COLUMN total_activated_referrals INT DEFAULT 0 AFTER referrer_tier;
```

### 4.3. Code changes can thiet (Phase 3)

```java
// File moi: ReferrerTierConfig.java (Domain Model)
// File moi: ReferrerTierConfigRepository.java (Port)
// File moi: ReferrerTierConfigJpaEntity.java (Infrastructure)
// File moi: UpdateReferrerTierUseCase.java (Application)

// Sua: CalculateReferrerCommissionUseCase.java
// - Doc commission rate tu referrer_tier_config thay vi hardcode 5%
```

---

## 5. ROLLBACK PLAN

### 5.1. Rollback Phase 1 (Database only)

```sql
-- Rollback: Restore from backup
-- STEP 1: Deactivate new milestones
UPDATE milestone_config
SET is_active = 0
WHERE orders_required IN (1, 3, 20, 40, 150)
  AND created_at >= '2026-01-14';

-- STEP 2: Reactivate old milestone at 5 orders
UPDATE milestone_config
SET is_active = 1,
    description = REPLACE(description, '[DEPRECATED] ', '')
WHERE milestone_type = 'WITH_REFERRER'
  AND orders_required = 5;

-- STEP 3: (Optional) Delete new records
DELETE FROM milestone_config
WHERE orders_required IN (1, 3, 20, 40, 150)
  AND created_at >= '2026-01-14';
```

### 5.2. Rollback Phase 3

```sql
-- Drop new table
DROP TABLE IF EXISTS referrer_tier_config;

-- Remove new columns
ALTER TABLE user
DROP COLUMN referrer_tier,
DROP COLUMN total_activated_referrals;
```

---

## 6. TESTING CHECKLIST

### 6.1. Unit Tests
- [ ] Test milestone tai moc 1 don (First Order Bonus)
- [ ] Test milestone tai moc 3 don (Activation)
- [ ] Test milestone tai moc 20, 40, 150 don
- [ ] Test user WITHOUT_REFERRER nhan dung thuong
- [ ] Test khong duplicate reward

### 6.2. Integration Tests
- [ ] Import CSV voi user moi -> kiem tra milestone 1 don
- [ ] User dat 3 don -> kiem tra referral activation
- [ ] User dat 10 don -> kiem tra bonus B +20k
- [ ] Kiem tra wallet balance sau moi milestone

### 6.3. Manual Tests
- [ ] Tao user moi voi referral code
- [ ] Mua 1 don -> kiem tra ca A va B nhan 5k
- [ ] Mua them 2 don (tong 3) -> kiem tra activation
- [ ] Kiem tra A nhan 5% tu don hang cua B

---

## 7. DEPLOYMENT STEPS

### 7.1. Pre-deployment
1. [ ] Backup database production
2. [ ] Review migration script
3. [ ] Test tren staging environment
4. [ ] Thong bao team ve thoi gian deploy

### 7.2. Deployment
1. [ ] Chay migration script tren production
2. [ ] Verify data bang query SELECT
3. [ ] Monitor logs trong 30 phut
4. [ ] Test thu 1-2 don hang

### 7.3. Post-deployment
1. [ ] Monitor menh thuong (so tien bonus phat ra)
2. [ ] Theo doi ty le chuyen doi
3. [ ] Thu thap feedback tu user

---

## 8. TIMELINE DE XUAT

| Phase | Noi dung | Thoi gian |
|-------|----------|-----------|
| Phase 1 | Database migration (moc moi) | 1 ngay |
| Testing | Test tren staging | 2 ngay |
| Deploy | Deploy production | 1 ngay |
| Phase 2 | Monitor & adjust | 1 tuan |
| Phase 3 | Referrer Tier System | 1 tuan (neu can) |

---

## 9. MENH GIA CHI PHI

### 9.1. Chi phi them cho moi user B (WITH_REFERRER, hoan thanh 150 don)

| Moc | Thuong B | Thuong A | Tong |
|-----|----------|----------|------|
| 1 don | 5,000 | 5,000 | 10,000 |
| 3 don | 10,000 | 20,000 | 30,000 |
| 10 don | 20,000 | 0 | 20,000 |
| 20 don | 15,000 | 0 | 15,000 |
| 40 don | 20,000 | 0 | 20,000 |
| 80 don | 0 (VIP) | 0 | 0 |
| 150 don | 50,000 | 0 | 50,000 |
| **TONG** | **120,000** | **25,000** | **145,000** |

### 9.2. So sanh voi hien tai

| | Hien tai | De xuat | Tang |
|--|----------|---------|------|
| Thuong B | 30,000 | 120,000 | +90,000 |
| Thuong A | 20,000 | 25,000 | +5,000 |
| **Tong** | **50,000** | **145,000** | **+95,000** |

### 9.3. ROI du kien
- Chi phi tang: +95,000 VND/user
- Nhung neu viral coefficient tang 2x -> CAC (Customer Acquisition Cost) giam 50%
- Chi phi quang cao trung binh: 50,000-100,000 VND/user moi
- => Van re hon chay ads neu referral hieu qua

---

## 10. APPENDIX

### A. Liquibase Changelog File

Tao file: `cashbee-presentation/src/main/resources/db/changelog/047-optimize-referral-milestones.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="047-1-deactivate-old-milestone-5" author="cashbee">
        <comment>Deactivate old activation milestone at 5 orders</comment>
        <update tableName="milestone_config">
            <column name="is_active" value="0"/>
            <column name="updated_at" valueComputed="NOW()"/>
            <where>milestone_type = 'WITH_REFERRER' AND orders_required = 5</where>
        </update>
    </changeSet>

    <changeSet id="047-2-add-first-order-bonus" author="cashbee">
        <comment>Add First Order Bonus milestone</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITH_REFERRER"/>
            <column name="orders_required" value="1"/>
            <column name="referee_bonus" value="5000.00"/>
            <column name="referrer_bonus" value="5000.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="First Order Bonus: B +5k, A +5k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-3-add-activation-3-orders" author="cashbee">
        <comment>Add activation milestone at 3 orders (reduced from 5)</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITH_REFERRER"/>
            <column name="orders_required" value="3"/>
            <column name="referee_bonus" value="10000.00"/>
            <column name="referrer_bonus" value="20000.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="12"/>
            <column name="activates_referral" value="1"/>
            <column name="description" value="Referral activation: B +10k, A +20k, A receives 5% commission for 12 months"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-4-add-milestone-20" author="cashbee">
        <comment>Add milestone at 20 orders</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITH_REFERRER"/>
            <column name="orders_required" value="20"/>
            <column name="referee_bonus" value="15000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Milestone bonus: B +15k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-5-add-milestone-40" author="cashbee">
        <comment>Add milestone at 40 orders (pre-VIP)</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITH_REFERRER"/>
            <column name="orders_required" value="40"/>
            <column name="referee_bonus" value="20000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Pre-VIP milestone: B +20k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-6-add-milestone-150" author="cashbee">
        <comment>Add milestone at 150 orders (pre-SUPER)</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITH_REFERRER"/>
            <column name="orders_required" value="150"/>
            <column name="referee_bonus" value="50000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Pre-SUPER milestone: B +50k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <!-- WITHOUT_REFERRER milestones -->
    <changeSet id="047-7-add-welcome-bonus-no-referrer" author="cashbee">
        <comment>Add Welcome Bonus for users without referrer</comment>
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITHOUT_REFERRER"/>
            <column name="orders_required" value="1"/>
            <column name="referee_bonus" value="3000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Welcome Bonus: +3k for first order"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-8-add-milestone-20-no-referrer" author="cashbee">
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITHOUT_REFERRER"/>
            <column name="orders_required" value="20"/>
            <column name="referee_bonus" value="10000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Milestone bonus: +10k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-9-add-milestone-40-no-referrer" author="cashbee">
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITHOUT_REFERRER"/>
            <column name="orders_required" value="40"/>
            <column name="referee_bonus" value="15000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Pre-VIP milestone: +15k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

    <changeSet id="047-10-add-milestone-150-no-referrer" author="cashbee">
        <insert tableName="milestone_config">
            <column name="milestone_type" value="WITHOUT_REFERRER"/>
            <column name="orders_required" value="150"/>
            <column name="referee_bonus" value="30000.00"/>
            <column name="referrer_bonus" value="0.00"/>
            <column name="new_tier" value=""/>
            <column name="commission_months" value="0"/>
            <column name="activates_referral" value="0"/>
            <column name="description" value="Pre-SUPER milestone: +30k"/>
            <column name="is_active" value="1"/>
        </insert>
    </changeSet>

</databaseChangeLog>
```

### B. Update db.changelog-master.xml

Them dong sau vao `db.changelog-master.xml`:
```xml
<include file="db/changelog/047-optimize-referral-milestones.xml"/>
```
