package com.cashbee.application.dto.order;

import com.cashbee.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query DTO for getting user's orders with filtering and pagination.
 *
 * This follows CQRS pattern - separating query from command.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserOrdersQuery {

    /**
     * User ID to get orders for.
     * Required field.
     */
    private Long userId;

    /**
     * Filter by order status (optional).
     * If null, returns all statuses.
     */
    private OrderStatus status;

    /**
     * Page number (0-indexed).
     * Default: 0
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size (number of orders per page).
     * Default: 10, Max: 50
     */
    @Builder.Default
    private int size = 10;

    /**
     * Validate and normalize query parameters.
     */
    public void validate() {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 50) {
            size = 50; // Max page size to prevent memory issues
        }
    }
}
