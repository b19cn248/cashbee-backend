package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection DTO for cashback data joined with order and item information.
 *
 * This DTO is used to receive data from a JOIN query across:
 * - cashback table
 * - affiliate_order table
 * - affiliate_order_item table
 * - affiliate_platform table
 *
 * Purpose: Carrier object to transport data from database to application layer.
 * The UseCase will then transform this into the final InvoiceDetailResponse.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashbackWithOrderInfo {

    // ============ Cashback Info ============

    /** Cashback record ID */
    private Long cashbackId;

    /** Commission received from platform */
    private BigDecimal commissionAmount;

    /** Cashback amount given to user */
    private BigDecimal cashbackAmount;

    /** Cashback rate (e.g., 80.00 for 80%) */
    private BigDecimal cashbackRate;

    // ============ Order Info ============

    /** Internal order ID */
    private Long orderId;

    /** Platform order code (e.g., 251228J18KS4X6) */
    private String orderCode;

    /** When order was placed */
    private LocalDateTime orderTime;

    // ============ Platform Info ============

    /** Platform ID */
    private Long platformId;

    /** Platform name (Shopee, Lazada, etc.) */
    private String platformName;

    // ============ Order Item Info ============

    /** Order item ID */
    private Long orderItemId;

    /** Product/item name */
    private String itemName;

    /** Quantity purchased */
    private Integer quantity;

    /** Actual product price paid */
    private BigDecimal actualAmount;

    /** Commission for this specific item */
    private BigDecimal itemCommission;

    /** Shop/seller name */
    private String shopName;

    /** Product image URL */
    private String imageUrl;

    /** Product category */
    private String category;
}
