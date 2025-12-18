package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for UserJpaEntity.
 * Provides database operations for User table.
 *
 * This is infrastructure-specific and should not be used directly
 * by application or domain layers. Use UserRepository (domain interface) instead.
 *
 * @author CashBee Team
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * Find user by Keycloak UUID.
     *
     * @param keycloakId Keycloak user UUID
     * @return Optional containing user entity if found
     */
    Optional<UserJpaEntity> findByKeycloakId(String keycloakId);

    /**
     * Find user by username.
     *
     * @param username Username
     * @return Optional containing user entity if found
     */
    Optional<UserJpaEntity> findByUsername(String username);

    /**
     * Find user by email.
     *
     * @param email Email address
     * @return Optional containing user entity if found
     */
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * Find user by referral code.
     *
     * @param referralCode Referral code
     * @return Optional containing user entity if found
     */
    Optional<UserJpaEntity> findByReferralCode(String referralCode);

    /**
     * Check if user exists by Keycloak ID.
     *
     * @param keycloakId Keycloak user UUID
     * @return true if user exists
     */
    boolean existsByKeycloakId(String keycloakId);

    /**
     * Check if user exists by email.
     *
     * @param email Email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by username.
     *
     * @param username Username
     * @return true if user exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if user exists by referral code.
     *
     * @param referralCode Referral code
     * @return true if referral code exists
     */
    boolean existsByReferralCode(String referralCode);

    /**
     * Find all users referred by a specific referral code.
     *
     * @param referredBy Referral code
     * @return List of referred users
     */
    List<UserJpaEntity> findByReferredBy(String referredBy);

    /**
     * Find all active users (not deleted).
     *
     * @return List of active users
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL")
    List<UserJpaEntity> findAllActive();

    /**
     * Count users by status (excluding deleted).
     *
     * @param status User status
     * @return Count of users with given status
     */
    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.status = :status AND u.deletedAt IS NULL")
    long countByStatus(@Param("status") String status);

    /**
     * Count total active users (not deleted).
     *
     * @return Total number of active users
     */
    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.deletedAt IS NULL")
    long countActive();

    /**
     * Find all active users with PAGINATION.
     *
     * @param pageable Pagination parameters
     * @return Paginated active users
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL")
    Page<UserJpaEntity> findAllActive(Pageable pageable);

    /**
     * Find users by status with PAGINATION (excluding deleted).
     *
     * @param status User status
     * @param pageable Pagination parameters
     * @return Paginated users with given status
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.status = :status AND u.deletedAt IS NULL")
    Page<UserJpaEntity> findByStatusActive(@Param("status") String status, Pageable pageable);

    /**
     * Search users by email or username with PAGINATION (excluding deleted).
     * Uses LIKE for partial matching.
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Paginated users matching the search
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL " +
           "AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UserJpaEntity> searchByEmailOrUsername(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find users who have orders within a date range.
     *
     * This query:
     * - Joins User with AffiliateOrder via userId
     * - Filters orders by orderTime within the date range
     * - Returns DISTINCT users (each user appears once even with multiple orders)
     * - Excludes deleted users
     *
     * The query uses dynamic conditions:
     * - If fromDate is null, no lower bound check
     * - If toDate is null, no upper bound check
     *
     * @param fromDate Start of date range (inclusive), null for no lower bound
     * @param toDate End of date range (inclusive), null for no upper bound
     * @param pageable Pagination parameters
     * @return Paginated users with orders in the date range
     */
    @Query("""
        SELECT DISTINCT u FROM UserJpaEntity u
        INNER JOIN com.cashbee.infrastructure.entity.AffiliateOrderJpaEntity o ON o.userId = u.id
        WHERE u.deletedAt IS NULL
          AND o.deletedAt IS NULL
          AND (:fromDate IS NULL OR o.orderTime >= :fromDate)
          AND (:toDate IS NULL OR o.orderTime <= :toDate)
        """)
    Page<UserJpaEntity> findUsersWithOrdersInDateRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
