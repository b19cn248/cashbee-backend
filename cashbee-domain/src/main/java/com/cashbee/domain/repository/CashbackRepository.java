package com.cashbee.domain.repository;

import com.cashbee.domain.enums.CashbackStatus;
import com.cashbee.domain.model.Cashback;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Cashback.
 * Infrastructure layer will implement this interface.
 *
 * @author CashBee Team
 */
public interface CashbackRepository {

    /**
     * Save cashback.
     *
     * @param cashback Cashback to save
     * @return Saved cashback with generated ID
     */
    Cashback save(Cashback cashback);

    /**
     * Find cashback by ID.
     *
     * @param id Cashback ID
     * @return Optional cashback
     */
    Optional<Cashback> findById(Long id);

    /**
     * Find cashback by order ID.
     *
     * @param orderId Order ID
     * @return Optional cashback
     */
    Optional<Cashback> findByOrderId(Long orderId);

    /**
     * Find all cashbacks for a user.
     *
     * @param userId User ID
     * @return List of cashbacks
     */
    List<Cashback> findByUserId(Long userId);

    /**
     * Find all cashbacks for a user with specific status.
     *
     * @param userId User ID
     * @param status Cashback status
     * @return List of cashbacks
     */
    List<Cashback> findByUserIdAndStatus(Long userId, CashbackStatus status);

    /**
     * Check if cashback exists for an order.
     *
     * @param orderId Order ID
     * @return true if cashback exists
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Delete cashback by ID.
     *
     * @param id Cashback ID
     */
    void deleteById(Long id);

    /**
     * Find cashback by order item ID.
     *
     * @param orderItemId Order item ID
     * @return Optional cashback
     */
    Optional<Cashback> findByOrderItemId(Long orderItemId);

    /**
     * Check if cashback exists for an order item.
     *
     * @param orderItemId Order item ID
     * @return true if cashback exists
     */
    boolean existsByOrderItemId(Long orderItemId);

    /**
     * Sum cashback amount by user ID and status.
     *
     * @param userId User ID
     * @param status Cashback status
     * @return Sum of cashback amounts
     */
    java.math.BigDecimal sumCashbackAmountByUserIdAndStatus(Long userId, CashbackStatus status);

    /**
     * Sum cashback amount by user ID and statuses.
     *
     * @param userId User ID
     * @param statuses List of statuses
     * @return Sum of cashback amounts
     */
    java.math.BigDecimal sumCashbackAmountByUserIdAndStatusIn(Long userId, List<CashbackStatus> statuses);
}
