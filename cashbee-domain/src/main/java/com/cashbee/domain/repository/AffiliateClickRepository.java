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
}
