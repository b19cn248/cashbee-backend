package com.cashbee.application.dto.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO containing estimated cashback information.
 *
 * Returns product details and estimated cashback amount that user
 * will receive when purchasing through CashBee.
 *
 * Cashback Calculation (based on user level):
 * - Formula: cashback = commission / 60 * userPercentage
 * - NORMAL: 80% of full commission
 * - VIP: 83% of full commission
 * - SUPER: 85% of full commission
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateCashbackResponse {

    /**
     * Product name from Shopee.
     */
    private String productName;

    /**
     * Shop name on Shopee.
     */
    private String shopName;

    /**
     * Product price in VND.
     */
    private BigDecimal price;

    /**
     * Product image URL.
     */
    private String imageUrl;

    /**
     * Original Shopee product link.
     */
    private String productLink;

    /**
     * Number of sales (số lượt bán).
     */
    private Integer sales;

    /**
     * Commission from ChietKhau.Pro (for reference).
     * This is what ChietKhau.Pro pays their users (~52% of Shopee commission).
     */
    private BigDecimal chietKhauCommission;

    /**
     * Estimated cashback amount user will receive from CashBee (in VND).
     * Formula: commission / 60 * userPercentage
     */
    private BigDecimal estimatedCashback;

    /**
     * Cashback rate as percentage of product price.
     * Example: 6.99 means 6.99% cashback.
     */
    private BigDecimal cashbackRate;

    /**
     * User level of the current user.
     * Values: NORMAL, VIP, SUPER
     */
    private String userLevel;

    /**
     * Cashback rate percentage applied based on user level.
     * NORMAL=80, VIP=83, SUPER=85
     */
    private Integer appliedCashbackRate;

    /**
     * Whether the commission is capped by Shopee.
     */
    private Boolean isCapped;

    /**
     * Maximum commission cap from Shopee (if applicable).
     */
    private BigDecimal maxCap;

    /**
     * Friendly message to display to user.
     */
    private String message;
}
