package com.cashbee.application.dto.request;

import com.cashbee.domain.enums.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating an affiliate order.
 * Used for admin/import operations.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAffiliateOrderRequest {

    @NotNull(message = "Platform ID is required")
    private Long platformId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String clickId;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Order status is required")
    private OrderStatus orderStatus;

    private String productName;

    private BigDecimal productPrice;

    @NotNull(message = "Commission amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Commission must be greater than 0")
    private BigDecimal commissionAmount;

    @Builder.Default
    private String currency = "VND";

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private LocalDateTime paidTime;

    @Builder.Default
    private String source = "IMPORT";

    private Long importBatchId;

    /**
     * List of items in this order.
     */
    private List<CreateOrderItemRequest> items;

    /**
     * Nested DTO for order items.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderItemRequest {

        @NotBlank(message = "Item ID is required")
        private String itemId;

        @NotBlank(message = "Item name is required")
        private String itemName;

        @Builder.Default
        private Integer quantity = 1;

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
    }
}
