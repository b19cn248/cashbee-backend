# Keycloak Authorization Services Setup Guide

This guide explains how to configure Keycloak Authorization Services (PEP - Policy Enforcement Point) for CashBee Backend.

## Architecture Overview

```
Client → API Request + JWT Token
    ↓
Spring Security validates JWT
    ↓
Policy Enforcer checks permissions with Keycloak
    ↓
Keycloak evaluates: Resource + Scope + Policy
    ↓
Allow/Deny decision
    ↓
API Controller
```

**Key Components:**
- **Resources:** API endpoints (e.g., `/api/wallets`, `/api/payouts`)
- **Scopes:** Actions on resources (e.g., `view`, `create`, `admin`)
- **Policies:** Authorization rules (e.g., role-based, user-based)
- **Permissions:** Resource + Scope + Policy combination

---

## Step 1: Configure Keycloak Client

### 1.1. Create/Update Client

In Keycloak Admin Console:

1. Go to **Clients** → Select `cashbee-backend` (or create new)
2. **Settings Tab:**
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Service Accounts Enabled: `ON`
   - Authorization Enabled: `ON` ⚠️ **IMPORTANT**
   - Valid Redirect URIs: `http://localhost:8080/*`
3. **Save**

### 1.2. Get Client Secret

1. Go to **Credentials** tab
2. Copy the **Secret** value
3. Update in `application.yml`:
   ```yaml
   keycloak:
     credentials:
       secret: <your-client-secret>
   ```

---

## Step 2: Define Authorization Resources

Go to **Clients** → `cashbee-backend` → **Authorization** tab

### 2.1. Create Resources

Click **Resources** → **Create** for each endpoint:

#### **User Resources**
```
Name: User Sync
URI: /api/users/sync
Type: user-resource
Scopes: user:sync
```

```
Name: User Info
URI: /api/users/keycloak/*
Type: user-resource
Scopes: user:view
```

#### **Wallet Resources**
```
Name: Wallet View
URI: /api/wallets/user/*
Type: wallet-resource
Scopes: wallet:view
```

```
Name: Wallet Admin Operations
URI: /api/wallets/*
Type: wallet-resource
Scopes: wallet:admin, wallet:lock
```

#### **Transaction Resources**
```
Name: Transaction History
URI: /api/transactions/user/*
Type: transaction-resource
Scopes: transaction:view
```

#### **Payout Resources**
```
Name: Payout User Operations
URI: /api/payouts/*
Type: payout-resource
Scopes: payout:create, payout:view, payout:cancel
```

```
Name: Payout Admin Operations
URI: /api/payouts/admin/*
Type: payout-resource
Scopes: payout:admin:view, payout:admin:approve, payout:admin:reject, payout:admin:complete
```

#### **Admin Resources**
```
Name: Admin Dashboard
URI: /api/admin/*
Type: admin-resource
Scopes: admin:statistics
```

---

## Step 3: Define Authorization Scopes

Click **Authorization Scopes** → **Create** for each action:

### User Scopes
- `user:sync` - Sync user data
- `user:view` - View user information

### Wallet Scopes
- `wallet:view` - View wallet balance
- `wallet:lock` - Lock balance for payout
- `wallet:admin` - Admin wallet operations (add/confirm/unlock/deduct)

### Transaction Scopes
- `transaction:view` - View transaction history

### Payout Scopes
- `payout:create` - Create payout request
- `payout:view` - View payout requests
- `payout:cancel` - Cancel payout request
- `payout:admin:view` - View all payouts (admin)
- `payout:admin:approve` - Approve payout request (admin)
- `payout:admin:reject` - Reject payout request (admin)
- `payout:admin:complete` - Complete payout request (admin)

### Admin Scopes
- `admin:statistics` - View system statistics

---

## Step 4: Define Policies

Click **Policies** → **Create Policy**

### 4.1. Role-Based Policies

#### **User Role Policy**
```
Name: User Role Policy
Type: Role
Realm Roles: USER
Logic: Positive
Description: Allows users with USER role
```

#### **Admin Role Policy**
```
Name: Admin Role Policy
Type: Role
Realm Roles: ADMIN
Logic: Positive
Description: Allows users with ADMIN role
```

### 4.2. User-Based Policies (Optional)

#### **Owner Policy**
```
Name: Owner Policy
Type: JavaScript
Code:
var context = $evaluation.getContext();
var identity = context.getIdentity();
var resource = context.getPermission().getResource();

// Extract userId from resource URI
var resourceUri = resource.getUri();
var matches = resourceUri.match(/\/user\/(\d+)/);

if (matches && matches[1]) {
    var resourceUserId = matches[1];
    var currentUserId = identity.getAttributes().getValue('user_id');

    // Allow if accessing own resource or is admin
    $evaluation.grant = (currentUserId === resourceUserId) ||
                       identity.hasRealmRole('ADMIN');
}
```

### 4.3. Time-Based Policies (Optional)

#### **Business Hours Policy**
```
Name: Business Hours Policy
Type: Time
Start: 09:00:00
End: 18:00:00
Timezone: Asia/Ho_Chi_Minh
Logic: Positive
Description: Only allow during business hours
```

---

## Step 5: Define Permissions

Click **Permissions** → **Create Permission** → **Resource-Based Permission**

### 5.1. User Permissions

#### **User Sync Permission**
```
Name: User Sync Permission
Resources: User Sync
Scopes: user:sync
Policies: User Role Policy
Decision Strategy: Affirmative
```

#### **User View Permission**
```
Name: User View Permission
Resources: User Info
Scopes: user:view
Policies: User Role Policy, Admin Role Policy
Decision Strategy: Affirmative
```

### 5.2. Wallet Permissions

#### **Wallet View Permission**
```
Name: Wallet View Permission
Resources: Wallet View
Scopes: wallet:view
Policies: User Role Policy, Admin Role Policy
Decision Strategy: Affirmative
```

#### **Wallet Lock Permission**
```
Name: Wallet Lock Permission
Resources: Wallet Admin Operations
Scopes: wallet:lock
Policies: User Role Policy
Decision Strategy: Unanimous
```

#### **Wallet Admin Permission**
```
Name: Wallet Admin Permission
Resources: Wallet Admin Operations
Scopes: wallet:admin
Policies: Admin Role Policy
Decision Strategy: Unanimous
```

### 5.3. Transaction Permissions

#### **Transaction View Permission**
```
Name: Transaction View Permission
Resources: Transaction History
Scopes: transaction:view
Policies: User Role Policy, Admin Role Policy
Decision Strategy: Affirmative
```

### 5.4. Payout Permissions

#### **Payout Create Permission**
```
Name: Payout Create Permission
Resources: Payout User Operations
Scopes: payout:create
Policies: User Role Policy
Decision Strategy: Unanimous
```

#### **Payout View Permission**
```
Name: Payout View Permission
Resources: Payout User Operations
Scopes: payout:view
Policies: User Role Policy, Admin Role Policy
Decision Strategy: Affirmative
```

#### **Payout Cancel Permission**
```
Name: Payout Cancel Permission
Resources: Payout User Operations
Scopes: payout:cancel
Policies: User Role Policy, Admin Role Policy
Decision Strategy: Affirmative
```

#### **Payout Admin Permission**
```
Name: Payout Admin Permission
Resources: Payout Admin Operations
Scopes: payout:admin:view, payout:admin:approve, payout:admin:reject, payout:admin:complete
Policies: Admin Role Policy
Decision Strategy: Unanimous
```

### 5.5. Admin Permissions

#### **Admin Statistics Permission**
```
Name: Admin Statistics Permission
Resources: Admin Dashboard
Scopes: admin:statistics
Policies: Admin Role Policy
Decision Strategy: Unanimous
```

---

## Step 6: Test Authorization

### 6.1. Get Token from Keycloak

```bash
# Get token for user with USER role
curl -X POST http://localhost:8080/realms/cashbee/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=cashbee-backend" \
  -d "client_secret=<your-secret>" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password"
```

### 6.2. Test API with Token

```bash
# Extract access_token from response
TOKEN="<access_token>"

# Test allowed endpoint (USER role)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/wallets/user/1

# Test forbidden endpoint (requires ADMIN role)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/dashboard/statistics
# Expected: 403 Forbidden
```

---

## Step 7: Evaluation & Debugging

### 7.1. Evaluate Permissions

In Keycloak Admin Console:
1. Go to **Clients** → `cashbee-backend` → **Authorization** → **Evaluate**
2. Select **User**: Test user
3. Select **Resource**: e.g., "Wallet View"
4. Select **Scope**: e.g., "wallet:view"
5. Click **Evaluate**

Result shows:
- ✅ **PERMIT** - User has permission
- ❌ **DENY** - User lacks permission
- Applied policies and reasons

### 7.2. Common Issues

**Issue: Always getting 403 Forbidden**
- Check client secret in application.yml
- Verify Authorization Enabled in Keycloak client
- Check policy-enforcer.json paths match your resources
- Verify token contains required roles in `realm_access.roles`

**Issue: Policy Enforcer not loading**
- Check policy-enforcer.json is in `src/main/resources/`
- Verify JSON syntax is valid
- Check application logs for errors

**Issue: Specific endpoint not protected**
- Verify path pattern in policy-enforcer.json matches endpoint
- Use `*` for wildcards: `/api/wallets/user/*`
- Check HTTP method matches (GET, POST, PUT, DELETE)

---

## Architecture Highlights

### **Why Policy Enforcer?**

**Traditional @PreAuthorize approach:**
```java
@PreAuthorize("hasRole('ADMIN')")  // ❌ Hard-coded in code
public void adminOperation() { }
```

**Policy Enforcer approach:**
```java
public void adminOperation() { }  // ✅ Authorization in Keycloak
```

**Benefits:**
- ✅ Centralized authorization management
- ✅ No code changes for permission updates
- ✅ Fine-grained access control (Resource + Scope + Policy)
- ✅ Complex policies (time-based, user-based, custom JavaScript)
- ✅ Audit trail in Keycloak
- ✅ Policy evaluation and testing in Keycloak UI

---

## Policy Decision Strategies

When multiple policies apply to a permission:

**Affirmative (Default):**
- ✅ PERMIT if **at least one** policy permits
- ❌ DENY if **all** policies deny

**Unanimous:**
- ✅ PERMIT if **all** policies permit
- ❌ DENY if **any** policy denies

**Consensus:**
- ✅ PERMIT if **more** policies permit than deny
- ❌ DENY if **more** policies deny than permit

---

## Security Best Practices

1. **Always use HTTPS** in production
2. **Rotate client secrets** regularly
3. **Use service accounts** for backend-to-Keycloak communication
4. **Limit token lifetime** (default: 5 minutes for access tokens)
5. **Enable token introspection** for additional security
6. **Monitor authorization decisions** in Keycloak logs
7. **Test policies** thoroughly using Evaluation tab
8. **Use specific scopes** instead of broad permissions

---

## References

- [Keycloak Authorization Services](https://www.keycloak.org/docs/latest/authorization_services/)
- [Keycloak Policy Enforcer](https://www.keycloak.org/docs/latest/authorization_services/#_enforcer_overview)
- [Keycloak REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
