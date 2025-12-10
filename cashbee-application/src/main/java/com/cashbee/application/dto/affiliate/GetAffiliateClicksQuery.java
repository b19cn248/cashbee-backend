package com.cashbee.application.dto.affiliate;

import com.cashbee.domain.enums.ClickStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query DTO for filtering and paginating affiliate clicks.
 * Used by Admin to search/filter tracking links.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAffiliateClicksQuery {

    /**
     * Filter by specific user ID (optional).
     */
    private Long userId;

    /**
     * Filter by platform ID (optional).
     */
    private Long platformId;

    /**
     * Filter by click status: CREATED, CLICKED, MATCHED, EXPIRED (optional).
     */
    private ClickStatus status;

    /**
     * Filter by order matched flag (optional).
     */
    private Boolean orderMatched;

    /**
     * Search keyword - searches in trackingCode, productName (optional).
     */
    private String search;

    /**
     * Page number (0-indexed).
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size (default 20, max 100).
     */
    @Builder.Default
    private int size = 20;
}
