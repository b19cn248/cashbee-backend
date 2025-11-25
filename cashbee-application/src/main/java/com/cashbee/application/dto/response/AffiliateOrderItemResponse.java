package com.cashbee.application.dto.response;

import com.cashbee.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for AffiliateOrderItem.
 * Used in API responses.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateOrderItemResponse {

    private Long id;

    private Long orderId;

    private String itemId;

    private String itemName;

    private Integer quantity;

    private BigDecimal actualAmount;

    private BigDecimal itemCommission;

    private String shopId;

    private String shopName;

    private String categoryLv1;

    private String categoryLv2;

    private String categoryLv3;

    private String imgUrl;

    private BigDecimal brandCommissionRate;

    private BigDecimal platformCommissionRate;

    /**
     * Item status (PENDING, APPROVED, PAID, CANCELLED).
     * Each item can have different status within the same order.
     */
    private OrderStatus status;

    private LocalDateTime createdAt;
}
