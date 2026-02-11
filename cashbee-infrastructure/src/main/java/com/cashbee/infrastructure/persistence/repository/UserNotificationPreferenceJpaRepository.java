package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.UserNotificationPreferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for UserNotificationPreferenceJpaEntity.
 *
 * @author CashBee Team
 */
@Repository
public interface UserNotificationPreferenceJpaRepository extends JpaRepository<UserNotificationPreferenceJpaEntity, Long> {

    /**
     * Find preference by user ID.
     */
    Optional<UserNotificationPreferenceJpaEntity> findByUserId(Long userId);

    /**
     * Check if user has notification preferences.
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete preference by user ID.
     */
    void deleteByUserId(Long userId);

    /**
     * Find all users who want payment invoice emails.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.emailPaymentInvoice = true")
    List<Long> findUserIdsWithEmailPaymentInvoiceEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want cashback confirmed emails.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.emailCashbackConfirmed = true")
    List<Long> findUserIdsWithEmailCashbackConfirmedEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want order matched emails.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.emailOrderMatched = true")
    List<Long> findUserIdsWithEmailOrderMatchedEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want promotional emails.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.emailPromotional = true")
    List<Long> findUserIdsWithEmailPromotionalEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want payment invoice push notifications.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.pushPaymentInvoice = true")
    List<Long> findUserIdsWithPushPaymentInvoiceEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want cashback confirmed push notifications.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.pushCashbackConfirmed = true")
    List<Long> findUserIdsWithPushCashbackConfirmedEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find all users who want order matched push notifications.
     */
    @Query("SELECT p.userId FROM UserNotificationPreferenceJpaEntity p " +
           "WHERE p.userId IN :userIds AND p.pushOrderMatched = true")
    List<Long> findUserIdsWithPushOrderMatchedEnabled(@Param("userIds") List<Long> userIds);

    /**
     * Find preferences for multiple users.
     */
    List<UserNotificationPreferenceJpaEntity> findByUserIdIn(List<Long> userIds);
}
