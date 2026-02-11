# Frontend Update: Admin Users API - Order Date Filter

> **Version:** 1.2.0
> **Date:** 2025-12-18
> **Backend Developer:** Claude Code Assistant
> **Priority:** Medium

---

## TL;DR - Tong Ket Nhanh

API `GET /api/admin/users` da duoc cap nhat voi **2 parameters moi**:

| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `orderFromDate` | String | `YYYY-MM-DD` | Loc users co don hang TU ngay nay |
| `orderToDate` | String | `YYYY-MM-DD` | Loc users co don hang DEN ngay nay |

---

## 1. Use Cases Moi

| Use Case | Request Example |
|----------|-----------------|
| Users co don hang hom nay | `?orderFromDate=2025-12-18&orderToDate=2025-12-18` |
| Users co don 3 ngay gan day | `?orderFromDate=2025-12-15&orderToDate=2025-12-18` |
| Users co don tu ngay X | `?orderFromDate=2025-12-01` |
| Users co don truoc ngay Y | `?orderToDate=2025-12-15` |
| Users co don trong thang 11 | `?orderFromDate=2025-11-01&orderToDate=2025-11-30` |

---

## 2. Luu Y Quan Trong

### Filter Priority

Khi co nhieu filter, he thong chi ap dung **1 filter** theo thu tu uu tien:

1. **Order Date Filter** (cao nhat)
2. **Search Filter**
3. **Status Filter**
4. **No Filter**

> Neu can ket hop filters (vd: ACTIVE + don hang hom nay), hay lien he BE team.

### Date Format

- **PHAI** su dung format: `YYYY-MM-DD`
- Vi du: `2025-12-18` (DUNG) | `18/12/2025` (SAI)

---

## 3. TypeScript Interface Updates

```typescript
// services/adminService.ts
export interface GetUsersParams {
  status?: string | null;
  search?: string | null;
  orderFromDate?: string | null;  // NEW - Format: YYYY-MM-DD
  orderToDate?: string | null;    // NEW - Format: YYYY-MM-DD
  page?: number;
  size?: number;
}
```

---

## 4. React Implementation

### 4.1 Service Function

```typescript
export async function getUsers({
  status = null,
  search = null,
  orderFromDate = null,  // NEW
  orderToDate = null,    // NEW
  page = 0,
  size = 20
}: GetUsersParams = {}) {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString()
  });

  if (status) params.append('status', status);
  if (search) params.append('search', search);
  if (orderFromDate) params.append('orderFromDate', orderFromDate);  // NEW
  if (orderToDate) params.append('orderToDate', orderToDate);        // NEW

  const response = await fetch(`${API_URL}/api/admin/users?${params}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });

  return response.json();
}
```

### 4.2 Helper Functions (Optional)

```typescript
// Lay ngay hom nay (YYYY-MM-DD)
const getToday = () => new Date().toISOString().split('T')[0];

// Lay ngay N ngay truoc
const getDaysAgo = (days: number) => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().split('T')[0];
};

// Su dung
// Users hom nay
getUsers({ orderFromDate: getToday(), orderToDate: getToday() });

// Users 7 ngay gan day
getUsers({ orderFromDate: getDaysAgo(6), orderToDate: getToday() });
```

---

## 5. UI Suggestions

### 5.1 Quick Filter Buttons

```
[Tat ca] [Hom nay] [3 ngay] [7 ngay] [30 ngay] [Tuy chon...]
```

### 5.2 Date Range Picker

```
Tu ngay: [____/____/________]  Den ngay: [____/____/________]  [Loc]
```

### 5.3 Active Filter Badge

Khi dang filter theo date, hien thi badge:
```
Dang loc: 2025-12-15 -> 2025-12-18 [x]
```

---

## 6. Testing

### Manual Test

```bash
# Users hom nay
curl "http://localhost:8080/api/admin/users?orderFromDate=2025-12-18&orderToDate=2025-12-18" \
  -H "Authorization: Bearer <token>"

# Users 7 ngay gan day
curl "http://localhost:8080/api/admin/users?orderFromDate=2025-12-11&orderToDate=2025-12-18" \
  -H "Authorization: Bearer <token>"
```

### Expected Response

Response format khong thay doi, van tra ve:

```json
{
  "success": true,
  "data": {
    "content": [...users],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  }
}
```

---

## 7. Questions?

Lien he Backend team qua:
- Slack: #cashbee-backend
- Email: backend@cashbee.vn
- Plane: CASHBEE project

---

## 8. Files Changed (Backend)

| File | Layer | Changes |
|------|-------|---------|
| `GetUsersQuery.java` | Application | +orderFromDate, +orderToDate, +hasOrderDateFilter() |
| `UserRepository.java` | Domain | +findUsersWithOrdersInDateRange() |
| `UserJpaRepository.java` | Infrastructure | +JPQL query |
| `UserRepositoryAdapter.java` | Infrastructure | +implementation |
| `GetUsersUseCase.java` | Application | +date conversion logic |
| `AdminController.java` | Presentation | +params |

---

**Happy Coding!**
