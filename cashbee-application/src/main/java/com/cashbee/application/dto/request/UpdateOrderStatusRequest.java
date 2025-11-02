package com.cashbee.application.dto.request;

import com.cashbee.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating order status.
 * Used by admin to approve/reject/cancel orders.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus newStatus;

    /**
     * Optional reason for status change (e.g., rejection reason).
     */
    private String reason;
}
