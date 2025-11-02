package com.cashbee.application.dto.transaction;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Query DTO for getting transaction history.
 *
 * Supports pagination to handle large transaction lists.
 *
 * Business Flow:
 * 1. User requests to view transaction history
 * 2. Frontend sends GET request with userId, page, size
 * 3. Backend executes query and returns paginated results
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class GetTransactionHistoryQuery {

    /**
     * User ID whose transactions to fetch.
     */
    private final Long userId;

    /**
     * Page number (0-indexed).
     * Default: 0 (first page)
     */
    @Builder.Default
    private final int page = 0;

    /**
     * Page size (number of transactions per page).
     * Default: 20
     */
    @Builder.Default
    private final int size = 20;
}
