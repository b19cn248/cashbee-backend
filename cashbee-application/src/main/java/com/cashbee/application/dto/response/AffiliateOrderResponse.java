package com.cashbee.application.dto.response;

import com.cashbee.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for AffiliateOrder.
 * Used in API responses.
 * Includes order items as nested objects.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateOrderResponse {

    private Long id;

    private Long platformId;

    private String platformName; // Denormalized for convenience

    private Long userId;

    private String clickId;

    private String orderId;

    private OrderStatus orderStatus;

    private String productName;

    private BigDecimal productPrice;

    private BigDecimal commissionAmount;

    private String currency;

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private LocalDateTime paidTime;

    private String source;

    private Long importBatchId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * List of order items (nested).
     * This makes it convenient for frontend to display order details.
     */
    private List<AffiliateOrderItemResponse> items;

    /**
     * Calculated field: total number of items in this order.
     */
    private Integer totalItems;

    /**
     * Calculated field: can this order receive cashback?
     */
    private Boolean canReceiveCashback;
}
