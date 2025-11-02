# Testing Guide - CashBee Backend

This guide provides step-by-step instructions for testing the CashBee Backend API.

## 📋 Prerequisites

Before testing, ensure:
- ✅ Database is set up and running
- ✅ Application is running on `http://localhost:8080`
- ✅ You have a REST client (curl, Postman, or Swagger UI)

## 🚀 Quick Test Flow

### 1. Verify Application is Running

```bash
# Health check
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### 2. Access Swagger UI

Open in browser:
```
http://localhost:8080/swagger-ui.html
```

This provides interactive API documentation with "Try it out" functionality.

## 🧪 Test Scenarios

### Scenario 1: New User Registration (from Keycloak)

**Step 1: Sync a new user**

```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User synced successfully",
  "data": {
    "id": 1,
    "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "0901234567",
    "referralCode": "CBXYZ123",
    "referredBy": null,
    "status": "ACTIVE",
    "createdAt": "2025-10-29T...",
    "updatedAt": "2025-10-29T..."
  },
  "timestamp": "2025-10-29T..."
}
```

**Verify:**
- ✅ User created with ID
- ✅ Unique referral code generated (CBxxxxxx format)
- ✅ Status is ACTIVE

**Step 2: Verify wallet was auto-created**

```bash
# Use the userId from previous response
curl http://localhost:8080/api/wallets/user/1
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 0.00,
    "pendingBalance": 0.00,
    "lockedBalance": 0.00,
    "totalEarned": 0.00,
    "totalWithdrawn": 0.00,
    "createdAt": "2025-10-29T...",
    "updatedAt": "2025-10-29T..."
  },
  "timestamp": "2025-10-29T..."
}
```

**Verify:**
- ✅ Wallet created automatically
- ✅ All balances initialized to 0.00

### Scenario 2: User with Referral Code

**Step 1: Create first user (referrer)**

```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "referrer-uuid-123",
    "username": "alice",
    "email": "alice@example.com",
    "fullName": "Alice Smith"
  }'
```

**Note the referralCode from response (e.g., "CBXYZ789")**

**Step 2: Create second user using referral code**

```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "referred-uuid-456",
    "username": "bob",
    "email": "bob@example.com",
    "fullName": "Bob Johnson",
    "referredBy": "CBXYZ789"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    ...
    "referredBy": "CBXYZ789",
    ...
  }
}
```

**Verify:**
- ✅ User created with valid referredBy code
- ✅ Referral relationship established

### Scenario 3: Cashback Flow

**Step 1: Add pending balance (order placed)**

```bash
curl -X POST http://localhost:8080/api/wallets/pending \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 50000.00,
    "description": "Cashback from Shopee order #12345"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Pending balance added successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 0.00,
    "pendingBalance": 50000.00,
    "lockedBalance": 0.00,
    "totalEarned": 0.00,
    ...
  }
}
```

**Verify:**
- ✅ Pending balance increased to 50,000
- ✅ Available balance still 0
- ✅ totalEarned still 0 (not confirmed yet)

**Step 2: Confirm pending balance (order confirmed)**

```bash
curl -X POST http://localhost:8080/api/wallets/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 50000.00,
    "description": "Order confirmed by Shopee"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Pending balance confirmed successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "balance": 50000.00,
    "pendingBalance": 0.00,
    "lockedBalance": 0.00,
    "totalEarned": 50000.00,
    ...
  }
}
```

**Verify:**
- ✅ Balance increased to 50,000 (now available)
- ✅ Pending balance decreased to 0
- ✅ totalEarned increased to 50,000

**Step 3: Add more pending balance**

```bash
curl -X POST http://localhost:8080/api/wallets/pending \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 25000.00,
    "description": "Cashback from Shopee order #12346"
  }'
```

**Step 4: Verify final state**

```bash
curl http://localhost:8080/api/wallets/user/1
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "balance": 50000.00,
    "pendingBalance": 25000.00,
    "lockedBalance": 0.00,
    "totalEarned": 50000.00,
    "totalWithdrawn": 0.00
  }
}
```

### Scenario 4: Error Handling Tests

**Test 1: User not found**

```bash
curl http://localhost:8080/api/wallets/user/999999
```

**Expected Response:**
```json
{
  "success": false,
  "errorCode": "WALLET_NOT_FOUND",
  "message": "Wallet not found for user: 999999",
  "timestamp": "2025-10-29T..."
}
```

**Test 2: Invalid amount (negative)**

```bash
curl -X POST http://localhost:8080/api/wallets/pending \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": -1000.00,
    "description": "Invalid amount"
  }'
```

**Expected Response:**
```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than 0"
  }
}
```

**Test 3: Confirm more than pending balance**

```bash
curl -X POST http://localhost:8080/api/wallets/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 999999.00,
    "description": "Try to confirm too much"
  }'
```

**Expected Response:**
```json
{
  "success": false,
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance. Required: 999999.00, Available: 25000.00"
}
```

## 📊 Database Verification

Connect to MySQL and verify data:

```sql
-- View users
SELECT * FROM user;

-- View wallets
SELECT * FROM user_wallet;

-- View user with wallet
SELECT
    u.username,
    u.email,
    u.referral_code,
    w.balance,
    w.pending_balance,
    w.total_earned
FROM user u
LEFT JOIN user_wallet w ON u.id = w.user_id;
```

## 🎯 Test Checklist

### Basic Functionality
- [ ] Application starts successfully
- [ ] Health check returns UP
- [ ] Swagger UI accessible
- [ ] Database connection working

### User Management
- [ ] Create new user
- [ ] User gets unique referral code
- [ ] Wallet auto-created for new user
- [ ] User with referral code works
- [ ] Duplicate user sync updates existing user

### Wallet Management
- [ ] Get wallet returns correct data
- [ ] Add pending balance works
- [ ] Confirm pending balance works
- [ ] Balance calculations correct
- [ ] Multiple operations in sequence work

### Error Handling
- [ ] Not found returns 404
- [ ] Validation errors return 400
- [ ] Insufficient balance handled correctly
- [ ] Negative amounts rejected

### Data Integrity
- [ ] Foreign keys enforced
- [ ] Unique constraints work
- [ ] Decimal precision correct (2 decimals)
- [ ] Timestamps auto-generated

## 🔧 Troubleshooting

### Issue: Connection refused

**Check:**
```bash
# Is application running?
curl http://localhost:8080/actuator/health

# Check logs
tail -f logs/cashbee-backend.log
```

### Issue: Database connection failed

**Check:**
```bash
# Can you connect to MySQL?
mysql -u cashbee_user -p cashbee

# Check application.yml credentials
cat cashbee-presentation/src/main/resources/application.yml | grep datasource -A 5
```

### Issue: Liquibase fails

**Solution:**
```sql
-- Reset database (WARNING: deletes all data)
DROP DATABASE cashbee;
CREATE DATABASE cashbee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Restart application
./mvnw spring-boot:run -pl cashbee-presentation
```

## 📝 Test Data Setup Script

For automated testing, create this SQL script:

```sql
-- scripts/test-data.sql

-- Insert test users
INSERT INTO user (keycloak_id, username, email, full_name, phone, referral_code, status)
VALUES
('test-uuid-1', 'testuser1', 'test1@example.com', 'Test User 1', '0901111111', 'CBTEST01', 'ACTIVE'),
('test-uuid-2', 'testuser2', 'test2@example.com', 'Test User 2', '0902222222', 'CBTEST02', 'ACTIVE'),
('test-uuid-3', 'testuser3', 'test3@example.com', 'Test User 3', '0903333333', 'CBTEST03', 'ACTIVE');

-- Insert test wallets
INSERT INTO user_wallet (user_id, balance, pending_balance, locked_balance, total_earned, total_withdrawn)
VALUES
(1, 100000.00, 50000.00, 0.00, 150000.00, 0.00),
(2, 75000.00, 25000.00, 10000.00, 100000.00, 25000.00),
(3, 0.00, 0.00, 0.00, 0.00, 0.00);
```

Load test data:
```bash
mysql -u cashbee_user -p cashbee < scripts/test-data.sql
```

## ✅ Success Criteria

All tests passing means:
- ✅ **Application**: Starts and runs without errors
- ✅ **Database**: Schema created correctly via Liquibase
- ✅ **API**: All endpoints return expected responses
- ✅ **Business Logic**: Wallet calculations correct
- ✅ **Error Handling**: Proper error messages returned
- ✅ **Data Integrity**: Database constraints working

---

**Happy Testing! 🧪**

For issues or questions, refer to:
- README.md for general documentation
- CONTRIBUTING.md for development guidelines
- Swagger UI for interactive API testing
