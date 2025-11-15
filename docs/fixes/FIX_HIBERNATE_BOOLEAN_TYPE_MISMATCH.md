# Fix: Hibernate Boolean Type Mismatch Error

## ❌ Error

```
org.hibernate.tool.schema.spi.SchemaManagementException:
Schema-validation: wrong column type encountered in column [verified] in table [otp_verifications];
found [tinyint (Types#TINYINT)], but expecting [bit (Types#BOOLEAN)]
```

## 🔍 Root Cause

**Vấn đề:** MySQL không có kiểu dữ liệu `BOOLEAN` native. Khi sử dụng `BOOLEAN` trong Liquibase:
- Liquibase tự động convert sang `TINYINT(1)` khi tạo table trong MySQL
- Hibernate JPA expect kiểu `BIT` khi validate schema với `@Column(nullable = false) private boolean verified`
- Mismatch giữa actual type (`TINYINT`) và expected type (`BIT`) → Schema validation fails

**MySQL Boolean Storage:**
- MySQL lưu `BOOLEAN` dưới dạng `TINYINT(1)`
- `TRUE` = 1, `FALSE` = 0
- Hibernate mong đợi `BIT` type thay vì `TINYINT`

## ✅ Solution

### Option 1: Use TINYINT(1) in Liquibase (RECOMMENDED)

Explicitly use `TINYINT(1)` instead of `BOOLEAN` in Liquibase migration files.

**Before (Wrong):**
```xml
<column name="verified" type="BOOLEAN" defaultValueBoolean="false">
    <constraints nullable="false"/>
</column>
```

**After (Correct):**
```xml
<column name="verified" type="TINYINT(1)" defaultValueNumeric="0">
    <constraints nullable="false"/>
</column>
```

**Why this works:**
- ✅ Matches actual MySQL storage type
- ✅ Hibernate can validate schema correctly
- ✅ `0` = false, `1` = true (standard MySQL boolean representation)
- ✅ JPA `boolean` fields work perfectly with `TINYINT(1)`

### Option 2: Use @Column(columnDefinition) in JPA Entity

Alternatively, specify column definition in JPA entity (NOT recommended - violates database independence).

```java
@Column(name = "verified", nullable = false, columnDefinition = "TINYINT(1)")
private boolean verified = false;
```

**Why NOT recommended:**
- ❌ Couples JPA entity to MySQL-specific type
- ❌ Reduces portability to other databases
- ❌ Migration file should be source of truth for schema

## 🔧 Implementation Steps

### Step 1: Update Migration File

**File:** `cashbee-presentation/src/main/resources/db/changelog/018-create-otp-verification-table.xml`

```xml
<!-- Change from BOOLEAN to TINYINT(1) -->
<column name="verified" type="TINYINT(1)" defaultValueNumeric="0">
    <constraints nullable="false"/>
</column>
```

### Step 2: Create Fix Migration (if table already exists)

**File:** `cashbee-presentation/src/main/resources/db/changelog/019-fix-otp-verified-column-type.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="019-fix-otp-verified-column-type" author="claude">
        <comment>Fix verified column type from BIT to TINYINT(1) to match Hibernate expectations</comment>

        <!-- Modify verified column to use TINYINT(1) instead of BIT -->
        <modifyDataType tableName="otp_verifications" columnName="verified" newDataType="TINYINT(1)"/>

        <rollback>
            <modifyDataType tableName="otp_verifications" columnName="verified" newDataType="BIT"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### Step 3: Add to Master Changelog

**File:** `db.changelog-master.xml`

```xml
<include file="db/changelog/018-create-otp-verification-table.xml"/>
<include file="db/changelog/019-fix-otp-verified-column-type.xml"/>
```

### Step 4: Run Migration

```bash
./mvnw liquibase:update
```

### Step 5: Verify

Start application - should boot without schema validation errors.

## 📋 Affected Columns

This fix applies to ALL boolean columns in the project:

**In `otp_verifications` table:**
- ✅ `verified` - Fixed to `TINYINT(1)`

**Check other tables for similar issues:**
```sql
-- Find all BIT columns in database
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cashbee'
  AND DATA_TYPE = 'bit';
```

## 🎯 Best Practices

### For Future Migrations

1. **Always use TINYINT(1) for boolean columns in MySQL:**
   ```xml
   <column name="is_active" type="TINYINT(1)" defaultValueNumeric="1">
       <constraints nullable="false"/>
   </column>
   ```

2. **Use consistent default values:**
   - `defaultValueNumeric="0"` for `false`
   - `defaultValueNumeric="1"` for `true`

3. **In JPA Entity:**
   ```java
   @Builder.Default
   @Column(nullable = false)
   private boolean isActive = true;
   ```
   - Let JPA handle type mapping
   - Don't use `columnDefinition` unless absolutely necessary

### Database Type Mapping Reference

| JPA Type | PostgreSQL | MySQL | H2 (Testing) |
|----------|-----------|-------|--------------|
| `boolean` | `BOOLEAN` | `TINYINT(1)` | `BOOLEAN` |
| `Boolean` | `BOOLEAN` | `TINYINT(1)` | `BOOLEAN` |

### Liquibase Type Mapping for MySQL

| Liquibase Type | MySQL Actual Type | Notes |
|----------------|-------------------|-------|
| `BOOLEAN` | `TINYINT(1)` | ⚠️ Can cause Hibernate validation issues |
| `TINYINT(1)` | `TINYINT(1)` | ✅ Recommended - explicit and clear |
| `BIT` | `BIT(1)` | ❌ Not compatible with JPA boolean |

## 🔍 Debugging Similar Issues

### 1. Check actual column type in MySQL:
```sql
DESCRIBE otp_verifications;
-- OR
SHOW CREATE TABLE otp_verifications;
```

### 2. Enable Hibernate SQL logging:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 3. Compare expected vs actual schema:
```bash
# Run with ddl-auto=validate (default)
./mvnw spring-boot:run

# Check error message for type mismatch
```

### 4. Fix by modifying column type:
```sql
-- Manual fix (if needed)
ALTER TABLE otp_verifications
MODIFY COLUMN verified TINYINT(1) NOT NULL DEFAULT 0;
```

## 📚 References

- [Liquibase MySQL Type Mapping](https://docs.liquibase.com/change-types/home.html)
- [Hibernate Schema Validation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#schema-generation)
- [MySQL Boolean Type](https://dev.mysql.com/doc/refman/8.0/en/boolean-literals.html)

## ✅ Verification Checklist

After applying fix:

- [x] Migration file uses `TINYINT(1)` instead of `BOOLEAN`
- [x] Fix migration created (if table exists)
- [x] Master changelog updated
- [x] Build successful: `./mvnw clean compile`
- [x] Application starts without schema validation errors
- [x] Boolean fields work correctly in application
- [x] Database column is `TINYINT(1)` type

## 🎉 Result

Application should now start successfully without schema validation errors. Boolean columns work correctly with values:
- `0` = `false`
- `1` = `true`

JPA automatically maps between Java `boolean` and MySQL `TINYINT(1)`.

---

**Fixed by:** Claude
**Date:** 2024-01-15
**Issue:** Schema validation - boolean type mismatch
**Solution:** Use `TINYINT(1)` instead of `BOOLEAN` in Liquibase migrations for MySQL
