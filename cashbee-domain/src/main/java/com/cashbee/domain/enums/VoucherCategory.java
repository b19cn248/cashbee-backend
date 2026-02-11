package com.cashbee.domain.enums;

/**
 * Category of promotion vouchers from Telegram channels.
 */
public enum VoucherCategory {
    /** General vouchers - applies to most products */
    GENERAL,

    /** New shop vouchers - for newly listed shops */
    NEW_SHOP,

    /** Shopee-handled vouchers */
    SHOPEE_XULY,

    /** HH Xtra vouchers */
    XTRA,

    /** Social media exclusive vouchers */
    MXH,

    /** VIP/Loyalty vouchers */
    VIP,

    /** Other uncategorized vouchers */
    OTHER
}
