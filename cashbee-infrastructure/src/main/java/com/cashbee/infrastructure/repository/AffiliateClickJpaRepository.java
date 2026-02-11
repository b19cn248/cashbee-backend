package com.cashbee.infrastructure.repository;

import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.infrastructure.entity.AffiliateClickJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AffiliateClickJpaEntity.
 *
 * @author CashBee Team
 */
@Repository
public interface AffiliateClickJpaRepository extends JpaRepository<AffiliateClickJpaEntity, Long> {

    Optional<AffiliateClickJpaEntity> findByTrackingCode(String trackingCode);

    List<AffiliateClickJpaEntity> findByUserId(Long userId);

    List<AffiliateClickJpaEntity> findByUserIdAndPlatformId(Long userId, Long platformId);

    List<AffiliateClickJpaEntity> findByStatus(ClickStatus status);

    List<AffiliateClickJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM AffiliateClickJpaEntity c WHERE c.createdAt < :cutoffDate AND c.orderMatched = false")
    List<AffiliateClickJpaEntity> findExpiredClicks(@Param("cutoffDate") LocalDateTime cutoffDate);

    long countByUserId(Long userId);

    long countByOrderMatchedTrue();

    @Query("SELECT (COUNT(c) * 1.0 / (SELECT COUNT(c2) FROM AffiliateClickJpaEntity c2 WHERE c2.status = 'CLICKED')) FROM AffiliateClickJpaEntity c WHERE c.orderMatched = true")
    Double calculateConversionRate();

    /**
     * Find clicks matching context for fallback matching (with time window).
     * Used when Sub_id1 is missing from CSV import.
     *
     * Conditions:
     * - Same platform
     * - Same item ID (exact match)
     * - Same shop ID (exact match)
     * - Click created within window before order time
     * - Not already matched with another order
     *
     * @param platformId Platform ID (e.g., Shopee)
     * @param itemId Item ID from order
     * @param shopId Shop ID from order
     * @param orderTime When the order was placed
     * @param windowStart Start of time window (orderTime - windowMinutes)
     * @return List of possible matching clicks, sorted by createdAt DESC
     */
    @Query("""
        SELECT c FROM AffiliateClickJpaEntity c
        WHERE c.platformId = :platformId
        AND c.itemId = :itemId
        AND c.shopId = :shopId
        AND c.createdAt >= :windowStart
        AND c.createdAt <= :orderTime
        AND c.orderMatched = false
        ORDER BY c.createdAt DESC
        """)
    List<AffiliateClickJpaEntity> findPossibleMatchesByContext(
        @Param("platformId") Long platformId,
        @Param("itemId") String itemId,
        @Param("shopId") String shopId,
        @Param("orderTime") LocalDateTime orderTime,
        @Param("windowStart") LocalDateTime windowStart
    );

    /**
     * Find clicks matching context for fallback matching (NO time restriction).
     * Used when Sub_id1 is missing from CSV import.
     *
     * This is a simplified version that only matches by itemId + shopId.
     * If exactly 1 match found → auto-assign to that user.
     * If multiple matches → flag for admin review.
     *
     * Conditions:
     * - Same platform
     * - Same item ID (exact match)
     * - Same shop ID (exact match)
     * - Not already matched with another order
     *
     * @param platformId Platform ID (e.g., Shopee)
     * @param itemId Item ID from order
     * @param shopId Shop ID from order
     * @return List of possible matching clicks, sorted by createdAt DESC
     */
    @Query("""
        SELECT c FROM AffiliateClickJpaEntity c
        WHERE c.platformId = :platformId
        AND c.itemId = :itemId
        AND c.shopId = :shopId
        AND c.orderMatched = false
        ORDER BY c.createdAt DESC
        """)
    List<AffiliateClickJpaEntity> findPossibleMatchesByItemAndShop(
        @Param("platformId") Long platformId,
        @Param("itemId") String itemId,
        @Param("shopId") String shopId
    );

    /**
     * Find all clicks with optional filters and pagination.
     * Supports filtering by userId, platformId, status, orderMatched, and search keyword.
     *
     * @param userId Filter by user ID (null = all users)
     * @param platformId Filter by platform ID (null = all platforms)
     * @param status Filter by status (null = all statuses)
     * @param orderMatched Filter by orderMatched flag (null = both)
     * @param search Search keyword for trackingCode or productName (null = no search)
     * @param pageable Spring Pageable for pagination
     * @return Page of matching clicks
     */
    @Query("""
        SELECT c FROM AffiliateClickJpaEntity c
        WHERE (:userId IS NULL OR c.userId = :userId)
        AND (:platformId IS NULL OR c.platformId = :platformId)
        AND (:status IS NULL OR c.status = :status)
        AND (:orderMatched IS NULL OR c.orderMatched = :orderMatched)
        AND (:search IS NULL OR :search = ''
             OR LOWER(c.trackingCode) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(c.productName) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.createdAt DESC
        """)
    List<AffiliateClickJpaEntity> findAllWithFilters(
        @Param("userId") Long userId,
        @Param("platformId") Long platformId,
        @Param("status") ClickStatus status,
        @Param("orderMatched") Boolean orderMatched,
        @Param("search") String search,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Count total clicks matching the filters.
     */
    @Query("""
        SELECT COUNT(c) FROM AffiliateClickJpaEntity c
        WHERE (:userId IS NULL OR c.userId = :userId)
        AND (:platformId IS NULL OR c.platformId = :platformId)
        AND (:status IS NULL OR c.status = :status)
        AND (:orderMatched IS NULL OR c.orderMatched = :orderMatched)
        AND (:search IS NULL OR :search = ''
             OR LOWER(c.trackingCode) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(c.productName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    long countWithFilters(
        @Param("userId") Long userId,
        @Param("platformId") Long platformId,
        @Param("status") ClickStatus status,
        @Param("orderMatched") Boolean orderMatched,
        @Param("search") String search
    );
}
