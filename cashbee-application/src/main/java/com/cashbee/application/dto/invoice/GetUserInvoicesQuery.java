package com.cashbee.application.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Query DTO for getting user invoices with filters.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserInvoicesQuery {

    /**
     * User ID (required).
     */
    private Long userId;

    /**
     * Filter by start date (optional).
     */
    private LocalDateTime startDate;

    /**
     * Filter by end date (optional).
     */
    private LocalDateTime endDate;

    /**
     * Page number (0-indexed).
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size.
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * Create query for user with default pagination.
     */
    public static GetUserInvoicesQuery forUser(Long userId) {
        return GetUserInvoicesQuery.builder()
                .userId(userId)
                .build();
    }

    /**
     * Create query with date range filter.
     */
    public static GetUserInvoicesQuery forUserWithDateRange(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return GetUserInvoicesQuery.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
