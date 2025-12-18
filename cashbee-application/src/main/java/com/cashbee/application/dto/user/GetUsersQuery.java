package com.cashbee.application.dto.user;

import com.cashbee.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Query DTO for getting users list with filtering and pagination.
 *
 * Used by GetUsersUseCase to fetch paginated users for admin.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersQuery {

    /**
     * Filter by user status (ACTIVE, SUSPENDED, BANNED).
     * If null, returns all statuses.
     */
    private UserStatus status;

    /**
     * Search keyword for email or username.
     * If null or empty, no search filter applied.
     */
    private String search;

    /**
     * Page number (0-indexed).
     * Default: 0
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size (number of users per page).
     * Default: 20, Max: 100
     */
    @Builder.Default
    private int size = 20;

    /**
     * Filter users who have orders FROM this date.
     * Format: YYYY-MM-DD (e.g., 2025-12-18)
     * If null, no lower bound on order date.
     */
    private LocalDate orderFromDate;

    /**
     * Filter users who have orders TO this date (inclusive).
     * Format: YYYY-MM-DD (e.g., 2025-12-18)
     * If null, no upper bound on order date.
     */
    private LocalDate orderToDate;

    /**
     * Validate and normalize query parameters.
     * Called before executing the query.
     */
    public void validate() {
        // Normalize page
        if (page < 0) {
            page = 0;
        }

        // Normalize size
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        // Trim search keyword
        if (search != null) {
            search = search.trim();
            if (search.isEmpty()) {
                search = null;
            }
        }
    }

    /**
     * Check if search filter is active.
     *
     * @return true if search keyword is provided
     */
    public boolean hasSearchFilter() {
        return search != null && !search.isBlank();
    }

    /**
     * Check if status filter is active.
     *
     * @return true if status is provided
     */
    public boolean hasStatusFilter() {
        return status != null;
    }

    /**
     * Check if order date filter is active.
     * Returns true if either fromDate or toDate is provided.
     *
     * @return true if any order date filter is provided
     */
    public boolean hasOrderDateFilter() {
        return orderFromDate != null || orderToDate != null;
    }
}
