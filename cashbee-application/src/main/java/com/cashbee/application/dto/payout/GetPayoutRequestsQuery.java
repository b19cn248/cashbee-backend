package com.cashbee.application.dto.payout;

import com.cashbee.domain.enums.PayoutStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Query DTO for getting payout requests with pagination and filtering.
 *
 * Business Scenarios:
 * - User views their own payout requests: provide userId
 * - Admin views all payout requests: leave userId as null
 * - Filter by status: provide status (REQUESTED, PROCESSING, PAID, REJECTED, CANCELLED)
 * - Combine userId + status for specific filtering
 *
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class GetPayoutRequestsQuery {

    /**
     * User ID filter (optional).
     * If null, returns all payout requests (admin view).
     * If provided, returns only payout requests for this user.
     */
    private final Long userId;

    /**
     * Status filter (optional).
     * If null, returns all statuses.
     * If provided, filters by this specific status.
     */
    private final PayoutStatus status;

    /**
     * Page number (0-based).
     * Default: 0
     */
    @Builder.Default
    private final int page = 0;

    /**
     * Page size.
     * Default: 20
     */
    @Builder.Default
    private final int size = 20;
}
