# 🔒 API Migration Guide: JWT Authentication for Tracking Links

**Date:** 2025-11-03
**Breaking Change:** YES ⚠️
**Affects:** Frontend/Mobile - Affiliate Tracking API
**Version:** Backend v2.0 (JWT Authentication)

---

## 📋 OVERVIEW

The **Create Tracking Link API** has been updated with **enhanced security** to prevent unauthorized access and user impersonation attacks.

### What Changed?

| Aspect | Before (v1.0) | After (v2.0) |
|--------|---------------|--------------|
| **Authentication** | ❌ No authentication required | ✅ **JWT token required** |
| **User ID Source** | ❌ Client sends `userId` in request body | ✅ **Extracted from JWT token** |
| **Security Level** | ⚠️ LOW - Anyone can create links for any user | ✅ **HIGH - Users can only create links for themselves** |

---

## 🚨 BREAKING CHANGES

### ❌ OLD API (Deprecated - Will stop working)

```http
POST /api/affiliate/tracking/create-link
Content-Type: application/json

{
  "shopeeUrl": "https://shopee.vn/product/123456789/987654321",
  "platformCode": "shopee",
  "userId": 100  ← THIS FIELD IS REMOVED
}
```

**Problems with old API:**
- ❌ No authentication - anyone can call the API
- ❌ `userId` sent by client - can be forged
- ❌ Security vulnerability - attacker can create links for other users

---

### ✅ NEW API (Required)

```http
POST /api/affiliate/tracking/create-link
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...  ← JWT TOKEN REQUIRED

{
  "shopeeUrl": "https://shopee.vn/product/123456789/987654321",
  "platformCode": "shopee"
}
```

**Improvements:**
- ✅ JWT token required - only authenticated users
- ✅ `userId` extracted from token - cannot be forged
- ✅ Users can only create links for themselves

---

## 🔧 FRONTEND MIGRATION STEPS

### Step 1: Remove `userId` from Request Body

**Before:**
```javascript
const createTrackingLink = async (shopeeUrl) => {
  const response = await fetch('/api/affiliate/tracking/create-link', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      shopeeUrl: shopeeUrl,
      userId: currentUser.id  // ❌ REMOVE THIS
    })
  });

  return response.json();
};
```

**After:**
```javascript
const createTrackingLink = async (shopeeUrl) => {
  // Get JWT token from storage
  const token = localStorage.getItem('access_token');

  // ⚠️ Check if user is logged in
  if (!token) {
    throw new Error('User must be logged in to create tracking links');
  }

  const response = await fetch('/api/affiliate/tracking/create-link', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`  // ✅ ADD JWT TOKEN
    },
    body: JSON.stringify({
      shopeeUrl: shopeeUrl
      // ❌ NO userId field - backend extracts from token
    })
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Unauthorized - Please login again');
    }
    throw new Error('Failed to create tracking link');
  }

  return response.json();
};
```

---

### Step 2: Handle Authentication Errors

**401 Unauthorized Response:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**Error Handling:**
```javascript
try {
  const result = await createTrackingLink(shopeeUrl);
  console.log('Tracking link created:', result);
} catch (error) {
  if (error.message.includes('Unauthorized')) {
    // User not logged in or token expired
    // Redirect to login page
    router.push('/login');
  } else if (error.message.includes('User not found')) {
    // User has token but not registered in CashBee
    // Redirect to registration
    router.push('/complete-registration');
  } else {
    // Other errors
    console.error('Failed to create tracking link:', error);
    toast.error('Cannot create tracking link. Please try again.');
  }
}
```

---

### Step 3: Ensure JWT Token is Available

**React Example:**
```javascript
import { useAuth } from './hooks/useAuth';

const TrackingLinkCreator = () => {
  const { user, token, isAuthenticated } = useAuth();

  const handleCreateLink = async (shopeeUrl) => {
    // Check authentication before calling API
    if (!isAuthenticated) {
      toast.error('Please login to create tracking links');
      router.push('/login');
      return;
    }

    try {
      const response = await fetch('/api/affiliate/tracking/create-link', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`  // ✅ Use token from auth context
        },
        body: JSON.stringify({ shopeeUrl })
      });

      if (!response.ok) throw new Error('Failed to create link');

      const data = await response.json();
      console.log('Success:', data);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <div>
      {/* UI components */}
    </div>
  );
};
```

**Vue Example:**
```javascript
<template>
  <div>
    <button @click="createLink" v-if="isLoggedIn">
      Create Tracking Link
    </button>
    <button @click="goToLogin" v-else>
      Login to Create Links
    </button>
  </div>
</template>

<script>
export default {
  computed: {
    isLoggedIn() {
      return this.$store.getters['auth/isAuthenticated'];
    },
    token() {
      return this.$store.getters['auth/accessToken'];
    }
  },
  methods: {
    async createLink() {
      try {
        const response = await this.$axios.post(
          '/api/affiliate/tracking/create-link',
          { shopeeUrl: this.shopeeUrl },
          {
            headers: {
              'Authorization': `Bearer ${this.token}`  // ✅ Token from Vuex
            }
          }
        );
        console.log('Success:', response.data);
      } catch (error) {
        if (error.response?.status === 401) {
          this.$router.push('/login');
        }
      }
    },
    goToLogin() {
      this.$router.push('/login');
    }
  }
};
</script>
```

---

## 📡 API SPECIFICATION

### Endpoint
```
POST /api/affiliate/tracking/create-link
```

### Authentication
- **Required:** YES ✅
- **Type:** Bearer Token (JWT)
- **Header:** `Authorization: Bearer <JWT_TOKEN>`

### Request Headers
```http
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Body
```json
{
  "shopeeUrl": "https://shopee.vn/product/123456789/987654321",
  "platformCode": "shopee"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `shopeeUrl` | String | ✅ Yes | Original Shopee product URL |
| `platformCode` | String | ⭕ Optional | Platform code (default: "shopee") |
| ~~`userId`~~ | ~~Long~~ | ❌ **REMOVED** | **Extracted from JWT token** |

### Success Response (201 Created)
```json
{
  "success": true,
  "message": "Tracking link created successfully",
  "data": {
    "clickId": 999,
    "trackingUrl": "https://s.shopee.vn/an_redir?origin_link=https%3A%2F%2Fshopee.vn%2Fproduct%2F123456789%2F987654321&affiliate_id=YOUR_AFFILIATE_ID&sub_id=CB100_999_20251103120530",
    "trackingCode": "CB100_999_20251103120530",
    "originalUrl": "https://shopee.vn/product/123456789/987654321",
    "shopId": "123456789",
    "itemId": "987654321",
    "platformName": "Shopee",
    "platformCode": "shopee",
    "estimatedCashbackRate": 5.0,
    "createdAt": "2025-11-03T12:05:30",
    "message": "Click this link to shop on Shopee and earn cashback!"
  }
}
```

### Error Responses

#### 401 Unauthorized (No Token or Invalid Token)
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "timestamp": "2025-11-03T12:05:30"
}
```

**Causes:**
- No `Authorization` header
- JWT token expired
- JWT token invalid (wrong signature)
- JWT token from wrong issuer

**Frontend Action:** Redirect to login page

---

#### 403 Forbidden (User Account Not Active)
```json
{
  "success": false,
  "error": "FORBIDDEN",
  "message": "User account is not active. Status: SUSPENDED",
  "timestamp": "2025-11-03T12:05:30"
}
```

**Causes:**
- User account is banned
- User account is suspended

**Frontend Action:** Show message to contact support

---

#### 404 Not Found (User Not in System)
```json
{
  "success": false,
  "error": "NOT_FOUND",
  "message": "User not found in system. Please complete registration first.",
  "timestamp": "2025-11-03T12:05:30"
}
```

**Causes:**
- User has valid Keycloak token but not registered in CashBee
- User account deleted

**Frontend Action:** Redirect to registration/onboarding flow

---

#### 400 Bad Request (Invalid URL)
```json
{
  "success": false,
  "error": "BUSINESS_ERROR",
  "message": "Invalid Shopee URL format",
  "timestamp": "2025-11-03T12:05:30"
}
```

**Causes:**
- Invalid Shopee URL format
- URL is not from Shopee

**Frontend Action:** Show error message to user

---

## 🧪 TESTING GUIDE

### Test Case 1: Valid Request with Token

**Request:**
```bash
curl -X POST https://api.cashbee.com/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/123456789/987654321"
  }'
```

**Expected:** 201 Created with tracking link

---

### Test Case 2: Missing Authorization Header

**Request:**
```bash
curl -X POST https://api.cashbee.com/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/123456789/987654321"
  }'
```

**Expected:** 401 Unauthorized

---

### Test Case 3: Expired Token

**Request:**
```bash
curl -X POST https://api.cashbee.com/api/affiliate/tracking/create-link \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer EXPIRED_TOKEN" \
  -d '{
    "shopeeUrl": "https://shopee.vn/product/123456789/987654321"
  }'
```

**Expected:** 401 Unauthorized

---

## 🔑 JWT TOKEN FORMAT

**JWT Token Structure:**
```
Header.Payload.Signature
```

**Decoded Payload Example:**
```json
{
  "sub": "keycloak-uuid-abc123",  ← Keycloak User ID (used to find userId)
  "email": "user@example.com",
  "preferred_username": "john_doe",
  "iat": 1699000000,  ← Issued at timestamp
  "exp": 1699003600,  ← Expiration timestamp
  "iss": "https://auth.cashbee.com/realms/cashbee"  ← Issuer (Keycloak)
}
```

**Backend Extraction Flow:**
```
JWT Token
   ↓
Extract "sub" → "keycloak-uuid-abc123"
   ↓
Query Database: SELECT * FROM users WHERE keycloak_id = 'keycloak-uuid-abc123'
   ↓
Found User: { id: 100, keycloak_id: "keycloak-uuid-abc123", username: "john_doe" }
   ↓
Use userId = 100 to create tracking link
```

---

## 🚀 DEPLOYMENT CHECKLIST

### Backend (Already Deployed)
- ✅ SecurityConfig updated - authentication enabled
- ✅ SecurityUtils created - JWT extraction logic
- ✅ CreateTrackingLinkRequest updated - userId field removed
- ✅ CreateTrackingLinkUseCase updated - userId as parameter
- ✅ AffiliateTrackingController updated - JWT token injection

### Frontend (TODO)
- [ ] Update API call to remove `userId` from request body
- [ ] Add `Authorization` header with JWT token
- [ ] Handle 401 Unauthorized errors (redirect to login)
- [ ] Handle 404 Not Found errors (redirect to registration)
- [ ] Test with valid JWT token
- [ ] Test with expired token
- [ ] Test without token
- [ ] Update UI to show login requirement

---

## 📞 SUPPORT

### Questions?
- **Backend Team:** [Your Backend Email]
- **Documentation:** This file
- **API Testing:** Use Postman collection (provided separately)

### Common Issues

**Issue:** "401 Unauthorized" even with token
- **Solution:** Check token expiration, get new token from Keycloak

**Issue:** "404 User not found" with valid token
- **Solution:** User needs to complete registration in CashBee system

**Issue:** Old API still working
- **Solution:** Old API will be disabled in next deployment

---

## 📅 TIMELINE

| Date | Milestone |
|------|-----------|
| 2025-11-03 | ✅ Backend changes deployed |
| 2025-11-04 | 🔄 Frontend migration in progress |
| 2025-11-05 | ✅ Old API deprecated (warning added) |
| 2025-11-10 | ❌ Old API disabled (breaking change) |

**⚠️ Frontend must migrate before 2025-11-10 to avoid service disruption!**

---

## 🎓 LEARNING RESOURCES

### What is JWT?
- [JWT.io - Introduction to JSON Web Tokens](https://jwt.io/introduction)
- [OAuth 2.0 and OpenID Connect](https://openid.net/connect/)

### Spring Security with JWT
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### Keycloak Integration
- [Keycloak Documentation](https://www.keycloak.org/documentation)

---

## 📝 CHANGELOG

### Version 2.0 (2025-11-03)
- ✅ Added JWT authentication requirement
- ✅ Removed `userId` from request body
- ✅ Added SecurityUtils for token extraction
- ✅ Updated all affected components
- ⚠️ **BREAKING CHANGE:** Old API without token will not work

### Version 1.0 (Previous)
- ❌ No authentication (deprecated)
- ❌ `userId` in request body (security risk)

---

**END OF MIGRATION GUIDE**
