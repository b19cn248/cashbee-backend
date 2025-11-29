package com.cashbee.domain.repository;

import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.model.AffiliateClick;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AffiliateClick entity.
 * This is a PORT in hexagonal architecture.
 *
 * @author CashBee Team
 */
public interface AffiliateClickRepository {

    /**
     * Save a click (create or update).
     */
    AffiliateClick save(AffiliateClick click);

    /**
     * Find click by ID.
     */
    Optional<AffiliateClick> findById(Long id);

    /**
     * Find click by tracking code.
     * This is the KEY method for matching CSV orders!
     */
    Optional<AffiliateClick> findByTrackingCode(String trackingCode);

    /**
     * Find all clicks for a user.
     */
    List<AffiliateClick> findByUserId(Long userId);

    /**
     * Find clicks by user and platform.
     */
    List<AffiliateClick> findByUserIdAndPlatformId(Long userId, Long platformId);

    /**
     * Find clicks by status.
     */
    List<AffiliateClick> findByStatus(ClickStatus status);

    /**
     * Find clicks created between dates.
     */
    List<AffiliateClick> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find expired clicks (older than X days and not matched).
     */
    List<AffiliateClick> findExpiredClicks(LocalDateTime cutoffDate);

    /**
     * Count clicks by user.
     */
    long countByUserId(Long userId);

    /**
     * Count matched clicks (resulted in orders).
     */
    long countByOrderMatchedTrue();

    /**
     * Calculate conversion rate (clicks → orders).
     */
    double getConversionRate();

    /**
     * Delete click by ID.
     */
    void deleteById(Long id);

    /**
     * Find clicks that match the given item/shop within a time window.
     * Used for fallback matching when Sub_id1 is missing from CSV import.
     *
     * Matching criteria:
     * - Same platform (e.g., Shopee)
     * - Same item ID (product that was clicked)
     * - Same shop ID (store that was clicked)
     * - Click created within windowMinutes before orderTime
     * - Click not already matched with another order (orderMatched = false)
     *
     * @param platformId Platform ID (e.g., Shopee = 1)
     * @param itemId Product item ID from order
     * @param shopId Shop ID from order
     * @param orderTime When the order was placed
     * @param windowMinutes Time window to search backward (e.g., 7 days = 10080 minutes)
     * @return List of possible matching clicks, sorted by createdAt DESC (most recent first)
     */
    List<AffiliateClick> findPossibleMatchesByContext(
        Long platformId,
        String itemId,
        String shopId,
        LocalDateTime orderTime,
        int windowMinutes
    );

    /**
     * Find clicks that match the given item/shop (NO time restriction).
     * Simplified fallback matching when Sub_id1 is missing.
     *
     * Matching criteria:
     * - Same platform (e.g., Shopee)
     * - Same item ID (product that was clicked)
     * - Same shop ID (store that was clicked)
     * - Click not already matched with another order (orderMatched = false)
     *
     * Logic:
     * - If exactly 1 match → auto-assign to that user
     * - If multiple matches from SAME user → use most recent click
     * - If multiple matches from DIFFERENT users → flag for admin review
     *
     * @param platformId Platform ID (e.g., Shopee = 1)
     * @param itemId Product item ID from order
     * @param shopId Shop ID from order
     * @return List of possible matching clicks, sorted by createdAt DESC (most recent first)
     */
    List<AffiliateClick> findPossibleMatchesByItemAndShop(
        Long platformId,
        String itemId,
        String shopId
    );
}
