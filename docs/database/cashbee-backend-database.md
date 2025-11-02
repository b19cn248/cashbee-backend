//////////////////////////////////////////////////////////
// DATABASE DESIGN - CASHBACK APP (KEYCLOAK INTEGRATED)
// Version: 2.0 - Updated for Keycloak Authentication
// Date: 2025-10-29
//////////////////////////////////////////////////////////

// Authentication & Authorization: Managed by Keycloak
// This DB only stores business data and syncs user info from Keycloak

Table user {
    id bigint [pk, increment]
    keycloak_id varchar(255) [unique, not null] // Keycloak User UUID
    username varchar(50) [unique, not null]
    email varchar(100) [unique, not null]
    full_name varchar(100)
    phone varchar(20)
    referral_code varchar(20) [unique]
    referred_by varchar(20)
    status varchar(20) [default: 'ACTIVE'] // ACTIVE, BANNED, SUSPENDED
    last_login_at datetime
    last_sync_at datetime // Last sync from Keycloak
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]
    deleted_at datetime // Soft delete

    Note: 'Roles are managed in Keycloak. No local role tables.'
    Indexes {
        keycloak_id [unique]
        email [unique]
        username [unique]
        referral_code
    }
}

// User Profile: Extended user information for business purposes
Table user_profile {
    id bigint [pk, increment]
    user_id bigint [unique, not null, ref: > user.id]
    avatar_url varchar(500)
    date_of_birth date
    gender varchar(10) // MALE, FEMALE, OTHER
    address text
    city varchar(100)
    province varchar(100)
    country varchar(50) [default: 'VN']
    postal_code varchar(20)

    // Payment Info
    momo_number varchar(20)
    momo_name varchar(100)
    zalo_pay_number varchar(20)
    zalo_pay_name varchar(100)
    bank_account_number varchar(50)
    bank_account_name varchar(100)
    bank_name varchar(100)
    bank_branch varchar(100)

    // Preferences
    preferred_payout_method varchar(20) // MOMO, ZALOPAY, BANK
    notification_enabled boolean [default: true]
    email_notification_enabled boolean [default: true]

    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]

    Indexes {
        user_id [unique]
    }
}

Table user_wallet {
    id bigint [pk, increment]
    user_id bigint [unique, not null, ref: > user.id]
    balance decimal(12,2) [default: 0] // Available balance
    pending_balance decimal(12,2) [default: 0] // Pending cashback
    locked_balance decimal(12,2) [default: 0] // Locked for withdrawal
    total_earned decimal(12,2) [default: 0] // Total cashback earned
    total_withdrawn decimal(12,2) [default: 0] // Total withdrawn
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]

    Indexes {
        user_id [unique]
    }
}

Table affiliate_platform {
    id bigint [pk, increment]
    name varchar(50) [unique, not null] // SHOPEE, LAZADA, TIKTOK
    code varchar(20) [unique, not null] // shopee, lazada, tiktok
    api_key varchar(255)
    api_secret varchar(255)
    base_url varchar(255)
    default_commission_rate decimal(5,2) // Platform default commission
    status varchar(20) [default: 'ACTIVE'] // ACTIVE, INACTIVE
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]

    Indexes {
        code [unique]
    }
}

// Note: affiliate_click table removed - not needed for MVP (file import only)

Table affiliate_import_batch {
    id bigint [pk, increment]
    batch_code varchar(50) [unique] // Auto-generated: BATCH_YYYYMMDD_HHMMSS
    file_name varchar(255) [not null]
    file_type varchar(20) // EXCEL, JSON
    file_size bigint // File size in bytes
    platform_id bigint [not null, ref: > affiliate_platform.id]
    total_orders int [default: 0]
    success_orders int [default: 0]
    failed_orders int [default: 0]
    status varchar(20) [default: 'PROCESSING'] // PROCESSING, COMPLETED, FAILED
    error_message text
    imported_by bigint [not null, ref: > user.id] // Keycloak user_id from our DB
    imported_at datetime [default: `current_timestamp`]
    completed_at datetime

    Indexes {
        batch_code [unique]
        platform_id
        imported_by
        imported_at
    }
}

Table affiliate_order {
    id bigint [pk, increment]
    platform_id bigint [not null, ref: > affiliate_platform.id]
    user_id bigint [not null, ref: > user.id]
    click_id varchar(100) // Affiliate click ID from platform
    order_id varchar(100) [unique, not null] // Platform order ID (order_sn from Shopee)
    order_status varchar(20) [default: 'PENDING'] // PENDING, APPROVED, CANCELLED, REJECTED, PAID
    product_name varchar(255)
    product_price decimal(12,2)
    commission_amount decimal(12,2) [not null] // Total commission from platform
    currency varchar(10) [default: 'VND']
    order_time datetime
    confirm_time datetime
    paid_time datetime
    source varchar(20) [default: 'IMPORT'] // IMPORT, API
    import_batch_id bigint [ref: > affiliate_import_batch.id]
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]
    deleted_at datetime // Soft delete

    Indexes {
        order_id [unique]
        platform_id
        user_id
        order_status
        import_batch_id
        order_time
    }
}

Table affiliate_order_item {
    id bigint [pk, increment]
    order_id bigint [not null, ref: > affiliate_order.id]
    item_id varchar(50) [not null] // Platform item ID
    item_name varchar(255) [not null]
    quantity int [default: 1]
    actual_amount decimal(12,2) // Item actual price
    item_commission decimal(12,2) // Commission for this item
    shop_id varchar(50)
    shop_name varchar(255)
    category_lv1 varchar(100)
    category_lv2 varchar(100)
    category_lv3 varchar(100)
    img_url varchar(500) // Product image URL
    brand_commission_rate decimal(8,4)
    platform_commission_rate decimal(8,4)
    created_at datetime [default: `current_timestamp`]

    Indexes {
        order_id
        item_id
        shop_id
    }
}

Table cashback {
    id bigint [pk, increment]
    user_id bigint [not null, ref: > user.id]
    order_id bigint [not null, ref: > affiliate_order.id]
    platform_id bigint [not null, ref: > affiliate_platform.id]
    commission_amount decimal(12,2) [not null] // Original commission from platform
    cashback_amount decimal(12,2) [not null] // Amount to return to user
    cashback_rate decimal(5,2) [not null] // Rate applied (from policy)
    policy_id bigint [ref: > cashback_policy.id] // Policy used for calculation
    status varchar(20) [default: 'PENDING'] // PENDING, CONFIRMED, PAID, CANCELLED, EXPIRED
    note text // Admin note or reason for cancellation
    created_at datetime [default: `current_timestamp`]
    confirmed_at datetime // When order status changed to APPROVED
    paid_at datetime // When cashback added to wallet
    cancelled_at datetime
    updated_at datetime [default: `current_timestamp`]

    Indexes {
        user_id
        order_id
        status
        created_at
    }
}

Table wallet_transaction {
    id bigint [pk, increment]
    wallet_id bigint [not null, ref: > user_wallet.id]
    transaction_code varchar(50) [unique] // Auto-generated: TXN_YYYYMMDD_HHMMSS_XXX
    type varchar(20) [not null] // CASHBACK, WITHDRAW, BONUS, REFERRAL, REFUND, ADJUSTMENT
    amount decimal(12,2) [not null] // Positive for credit, negative for debit
    balance_before decimal(12,2) // Balance before transaction
    balance_after decimal(12,2) // Balance after transaction
    status varchar(20) [default: 'PENDING'] // SUCCESS, PENDING, FAILED, CANCELLED
    description text
    reference_type varchar(50) // CASHBACK_ID, PAYOUT_ID, ORDER_ID, etc.
    reference_id varchar(100)
    created_by bigint [ref: > user.id] // User who initiated (for admin adjustments)
    created_at datetime [default: `current_timestamp`]
    processed_at datetime

    Indexes {
        transaction_code [unique]
        wallet_id
        type
        status
        created_at
        reference_type
        reference_id
    }
}

Table payout_request {
    id bigint [pk, increment]
    request_code varchar(50) [unique] // Auto-generated: PAYOUT_YYYYMMDD_HHMMSS
    user_id bigint [not null, ref: > user.id]
    amount decimal(12,2) [not null]
    fee decimal(12,2) [default: 0] // Transaction fee
    net_amount decimal(12,2) [not null] // Amount - Fee
    payout_method varchar(20) [not null] // MOMO, ZALOPAY, BANK

    // Payment Account Info (snapshot at request time)
    account_number varchar(50) [not null]
    account_name varchar(100) [not null]
    bank_name varchar(100) // For BANK method
    bank_branch varchar(100) // For BANK method

    status varchar(20) [default: 'REQUESTED'] // REQUESTED, PROCESSING, PAID, REJECTED, CANCELLED
    user_note text // User's note when requesting
    admin_note text // Admin's note when processing
    rejection_reason text

    processed_by bigint [ref: > user.id] // Admin who processed this request
    transaction_id bigint [ref: > wallet_transaction.id] // Link to wallet transaction

    requested_at datetime [default: `current_timestamp`]
    processed_at datetime
    cancelled_at datetime

    Indexes {
        request_code [unique]
        user_id
        status
        requested_at
        processed_by
    }
}

Table cashback_policy {
    id bigint [pk, increment]
    policy_name varchar(100) [not null]
    policy_code varchar(50) [unique]
    platform_id bigint [ref: > affiliate_platform.id] // Null = apply to all platforms
    user_level varchar(20) [default: 'NORMAL'] // NORMAL, VIP, SUPER
    cashback_rate decimal(5,2) [not null] // Percentage of commission to return
    min_order_value decimal(12,2) [default: 0]
    max_cashback_per_order decimal(12,2) // Cap per order
    is_active boolean [default: true]
    priority int [default: 0] // Higher priority applied first
    effective_from datetime [not null]
    effective_to datetime
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]

    Note: 'Example: rate=70 means user gets 70% of commission as cashback'
    Indexes {
        policy_code [unique]
        platform_id
        is_active
        effective_from
    }
}

// System Configuration
Table system_config {
    id bigint [pk, increment]
    config_key varchar(100) [unique, not null]
    config_value text [not null]
    data_type varchar(20) [default: 'STRING'] // STRING, INTEGER, DECIMAL, BOOLEAN, JSON
    category varchar(50) // PAYOUT, CASHBACK, SYSTEM, NOTIFICATION
    description text
    is_editable boolean [default: true]
    updated_by bigint [ref: > user.id]
    created_at datetime [default: `current_timestamp`]
    updated_at datetime [default: `current_timestamp`]

    Note: 'Examples: min_payout_amount=20000, max_payout_amount=50000000, default_cashback_rate=70'
    Indexes {
        config_key [unique]
        category
    }
}

// Admin Audit Log
Table admin_audit_log {
    id bigint [pk, increment]
    user_id bigint [not null, ref: > user.id] // Admin user
    action varchar(50) [not null] // IMPORT_FILE, APPROVE_PAYOUT, UPDATE_CONFIG, etc.
    entity_type varchar(50) // USER, ORDER, PAYOUT, CONFIG, etc.
    entity_id varchar(100)
    changes json // Before/after data
    ip_address varchar(50)
    user_agent text
    request_id varchar(100) // For request tracing
    created_at datetime [default: `current_timestamp`]

    Indexes {
        user_id
        action
        entity_type
        entity_id
        created_at
    }
}
