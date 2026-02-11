package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed invoice response with order breakdown.
 *
 * This DTO provides a complete "supermarket receipt" style view of an invoice,
 * showing every order and item that contributed to the total cashback.
 *
 * Structure:
 * - Invoice metadata (number, status, timestamps)
 * - Summary totals (orders, items, amounts)
 * - Bank transfer info
 * - List of orders with item details
 * - Calculation explanation for transparency
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailResponse {

    // ============ Invoice Metadata ============

    private Long id;
    private String invoiceNumber;
    private LocalDateTime transferTime;
    private String transferStatus;
    private LocalDateTime createdAt;

    // ============ Summary Section ============

    private InvoiceSummaryDetail summary;

    // ============ Bank Info ============

    private BankInfo bankInfo;

    // ============ Orders List ============

    private List<OrderDetail> orders;

    // ============ Bonus Breakdown ============

    private BonusBreakdown bonusBreakdown;

    // ============ Calculation Explanation ============

    private CalculationInfo calculation;

    // ========== Nested Classes ==========

    /**
     * Summary totals for the invoice.
     * Provides overview numbers at the top of the receipt.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceSummaryDetail {

        /** Total number of unique orders */
        private Integer totalOrders;

        /** Total number of items across all orders */
        private Integer totalItems;

        /** Sum of all product prices */
        private BigDecimal totalProductAmount;

        /** Total commission received from platforms */
        private BigDecimal totalCommission;

        /** Cashback rate user receives (e.g., 80%) */
        private BigDecimal cashbackRate;

        /** Platform fee rate (e.g., 20%) */
        private BigDecimal platformFeeRate;

        /** Final cashback amount = commission * cashbackRate */
        private BigDecimal totalCashback;

        /** Currency code (default: VND) */
        @Builder.Default
        private String currency = "VND";
    }

    /**
     * Bank account information for the transfer.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInfo {
        private String bankName;
        private String bankFullName;
        private String accountNumber;
        private String accountName;
    }

    /**
     * Details of a single order in the invoice.
     * Groups items by order code.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {

        /** Platform order code (e.g., 251228J18KS4X6) */
        private String orderCode;

        /** When the order was placed */
        private LocalDateTime orderTime;

        /** Platform name (Shopee, Lazada, etc.) */
        private String platformName;

        /** List of items in this order */
        private List<OrderItemDetail> items;

        /** Order subtotals */
        private OrderTotals totals;
    }

    /**
     * Details of a single item within an order.
     * This is the most granular level - individual product info.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail {

        /** Product/item name */
        private String itemName;

        /** Shop/seller name */
        private String shopName;

        /** Quantity purchased */
        private Integer quantity;

        /** Product price (actual amount paid) */
        private BigDecimal productPrice;

        /** Commission from platform for this item */
        private BigDecimal commission;

        /** Cashback rate (e.g., 80.00) */
        private BigDecimal cashbackRate;

        /** Actual cashback amount = commission * rate */
        private BigDecimal cashbackAmount;

        /** Product image URL (optional) */
        private String imageUrl;

        /** Category (optional) */
        private String category;
    }

    /**
     * Totals for a single order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTotals {
        private BigDecimal productAmount;
        private BigDecimal commission;
        private BigDecimal cashbackAmount;
    }

    /**
     * Explanation of how cashback was calculated.
     * Helps users understand the math behind their payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationInfo {

        /** Human-readable description */
        private String description;

        /** Formula used (e.g., "Cashback = Commission × 80%") */
        private String formula;

        /** Additional notes */
        private String note;
    }

    /**
     * Breakdown of bonus payments (milestone bonus + referrer commission).
     * Shows bonus rewards that were paid in this batch transfer.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusBreakdown {

        /** Total bonus amount (milestone + referrer) */
        private BigDecimal totalBonusAmount;

        /** Milestone bonus details */
        private BonusDetail milestoneBonus;

        /** Referrer commission details */
        private BonusDetail referrerCommission;

        /**
         * Check if there are any bonuses in this invoice.
         */
        public boolean hasBonus() {
            return totalBonusAmount != null && totalBonusAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Detail for a specific bonus type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusDetail {

        /** Type label (e.g., "Thưởng mốc", "Hoa hồng giới thiệu") */
        private String label;

        /** Number of bonus rewards */
        private Integer count;

        /** Total amount for this bonus type */
        private BigDecimal amount;

        /** Description of the bonuses */
        private String description;
    }
}
