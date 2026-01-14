package com.cashbee.application.usecase.referral;

import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use Case: Sync total_completed_orders for all users.
 * <p>
 * This is a one-time or periodic job to fix historical data where
 * total_completed_orders may have been incorrectly counted due to:
 * - Re-import causing double counting
 * - Orders imported before milestone tracking was implemented
 * <p>
 * The correct count is based on distinct orders that have cashback
 * with CONFIRMED or PAID status.
 * <p>có ph
 * Usage:
 * - Run once to fix existing data after deploying the new recalculate logic
 * - Can be exposed as an admin API endpoint for manual sync
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncAllUsersCompletedOrdersUseCase {

    private final UserRepository userRepository;
    private final CashbackRepository cashbackRepository;

    /**
     * Sync completed orders count for all users.
     *
     * @return number of users updated
     */
    @Transactional
    public int execute() {
        log.info("Starting sync of total_completed_orders for all users");

        List<User> allUsers = userRepository.findAll();
        int updatedCount = 0;
        int totalUsers = allUsers.size();

        for (User user : allUsers) {
            try {
                boolean updated = syncUserCompletedOrders(user);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to sync user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Sync completed: {}/{} users updated", updatedCount, totalUsers);
        return updatedCount;
    }

    /**
     * Sync completed orders count for a specific user.
     *
     * @param userId the user ID to sync
     * @return true if user was updated, false if not found or no change
     */
    @Transactional
    public boolean executeForUser(Long userId) {
        log.debug("Syncing total_completed_orders for user: {}", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return false;
        }

        return syncUserCompletedOrders(user);
    }

    /**
     * Internal method to sync a single user's completed orders.
     *
     * @param user the user to sync
     * @return true if updated, false if no change needed
     */
    private boolean syncUserCompletedOrders(User user) {
        Long userId = user.getId();
        int currentCount = user.getTotalCompletedOrders() != null ? user.getTotalCompletedOrders() : 0;

        // Count distinct orders with CONFIRMED or PAID cashback
        int actualCount = cashbackRepository.countConfirmedOrdersByUserId(userId);

        // Only update if there's a difference
        if (currentCount != actualCount) {
            user.setTotalCompletedOrders(actualCount);
            userRepository.save(user);

            log.info("User {} total_completed_orders synced: {} -> {}",
                    userId, currentCount, actualCount);
            return true;
        }

        return false;
    }
}
