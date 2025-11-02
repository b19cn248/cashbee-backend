package com.cashbee.domain.repository;

import com.cashbee.domain.model.User;

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
}
