package com.cashbee.domain.repository;

import com.cashbee.domain.model.UserNotificationPreference;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserNotificationPreference domain model.
 *
 * This is a domain-level interface (port in hexagonal architecture).
 * Implementation will be in infrastructure layer.
 *
 * One-to-one relationship with User (each user has at most one preference record).
 *
 * @author CashBee Team
 */
public interface UserNotificationPreferenceRepository {

    /**
     * Save notification preference.
     *
     * @param preference Preference to save
     * @return Saved preference
     */
    UserNotificationPreference save(UserNotificationPreference preference);

    /**
     * Find preference by ID.
     *
     * @param id Preference ID
     * @return Preference if found
     */
    Optional<UserNotificationPreference> findById(Long id);

    /**
     * Find preference by user ID.
     *
     * @param userId User ID
     * @return Preference if found
     */
    Optional<UserNotificationPreference> findByUserId(Long userId);

    /**
     * Find or create default preference for user.
     * If user doesn't have preferences, creates with default values.
     *
     * @param userId User ID
     * @return Existing or newly created preference
     */
    default UserNotificationPreference findOrCreateDefault(Long userId) {
        return findByUserId(userId)
                .orElseGet(() -> save(UserNotificationPreference.createDefault(userId)));
    }

    /**
     * Check if user has notification preferences.
     *
     * @param userId User ID
     * @return true if preference exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete preference by user ID.
     * Used when user account is deleted.
     *
     * @param userId User ID
     */
    void deleteByUserId(Long userId);

    /**
     * Find all users who opted in for payment invoice email.
     * Used for batch email sending.
     *
     * @param userIds List of user IDs to check
     * @return List of user IDs who want payment invoice emails
     */
    List<Long> findUserIdsWithEmailPaymentInvoiceEnabled(List<Long> userIds);

    /**
     * Find all users who opted in for cashback confirmed email.
     *
     * @param userIds List of user IDs to check
     * @return List of user IDs who want cashback confirmed emails
     */
    List<Long> findUserIdsWithEmailCashbackConfirmedEnabled(List<Long> userIds);

    /**
     * Find all users who opted in for promotional emails.
     *
     * @param userIds List of user IDs to check
     * @return List of user IDs who want promotional emails
     */
    List<Long> findUserIdsWithEmailPromotionalEnabled(List<Long> userIds);
}
