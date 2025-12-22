package com.cashbee.domain.repository;

import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository Interface (Port).
 *
 * This is a domain interface that defines the contract for user persistence.
 * The infrastructure layer will provide the actual implementation.
 *
 * Following Hexagonal Architecture principles:
 * - Domain layer defines WHAT operations are needed
 * - Infrastructure layer implements HOW (JPA, MyBatis, JDBC, etc.)
 *
 * @author CashBee Team
 */
public interface UserRepository {

    /**
     * Save a user (insert or update).
     *
     * @param user User to save
     * @return Saved user with generated ID if new
     */
    User save(User user);

    /**
     * Find user by internal ID.
     *
     * @param id User ID
     * @return Optional containing user if found
     */
    Optional<User> findById(Long id);

    /**
     * Find user by Keycloak UUID.
     *
     * @param keycloakId Keycloak user UUID
     * @return Optional containing user if found
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Find user by username.
     *
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     *
     * @param email Email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by referral code.
     *
     * @param referralCode Referral code
     * @return Optional containing user if found
     */
    Optional<User> findByReferralCode(String referralCode);

    /**
     * Check if user exists by ID.
     *
     * @param id User ID
     * @return true if user exists
     */
    boolean existsById(Long id);

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
     * Check if user exists by phone number.
     *
     * @param phone Phone number
     * @return true if phone number exists
     */
    boolean existsByPhone(String phone);

    /**
     * Check if user exists by referredBy code.
     * This checks if someone has already used this referral code when registering.
     *
     * @param referredBy Referral code used during registration
     * @return true if someone has already used this referral code
     */
    boolean existsByReferredBy(String referredBy);

    /**
     * Find all users referred by a specific referral code.
     *
     * @param referralCode Referral code
     * @return List of referred users
     */
    List<User> findByReferredBy(String referralCode);

    /**
     * Find all users (excluding deleted).
     *
     * @return List of all active users
     */
    List<User> findAll();

    /**
     * Find all users with PAGINATION (excluding deleted).
     * This is the RECOMMENDED method for listing users!
     * Prevents memory issues when there are many users.
     *
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated users
     */
    Page<User> findAll(Pageable pageable);

    /**
     * Find users by status with PAGINATION.
     *
     * @param status User status to filter
     * @param pageable Pagination parameters
     * @return Paginated users
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Search users by email or username with PAGINATION.
     *
     * @param keyword Search keyword (matches email or username)
     * @param pageable Pagination parameters
     * @return Paginated users matching the search
     */
    Page<User> searchByEmailOrUsername(String keyword, Pageable pageable);

    /**
     * Delete user (soft delete).
     *
     * @param id User ID
     */
    void deleteById(Long id);

    /**
     * Count total users (excluding deleted).
     *
     * @return Total number of users
     */
    long count();

    /**
     * Count users by status.
     *
     * @param status User status
     * @return Number of users with given status
     */
    long countByStatus(String status);

    /**
     * Find users who have orders within a date range.
     *
     * This method queries users that have at least one order
     * with orderTime between fromDate and toDate.
     *
     * Use cases:
     * - Find users with orders today
     * - Find users with orders in the last 7 days
     * - Find users with orders in a specific date range
     *
     * @param fromDate Start of date range (inclusive), null for no lower bound
     * @param toDate End of date range (inclusive), null for no upper bound
     * @param pageable Pagination parameters
     * @return Paginated users with orders in the date range
     */
    Page<User> findUsersWithOrdersInDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );
}
