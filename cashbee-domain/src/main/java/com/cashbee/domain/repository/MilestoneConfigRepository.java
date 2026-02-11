package com.cashbee.domain.repository;

import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.model.MilestoneConfig;

import java.util.List;
import java.util.Optional;

/**
 * MilestoneConfig Repository Interface (Port).
 *
 * <p>This is a domain interface that defines the contract for milestone config persistence.
 * The infrastructure layer will provide the actual implementation.
 *
 * <p>Following Hexagonal Architecture principles:
 * <ul>
 *   <li>Domain layer defines WHAT operations are needed</li>
 *   <li>Infrastructure layer implements HOW (JPA, etc.)</li>
 * </ul>
 *
 * @author CashBee Team
 */
public interface MilestoneConfigRepository {

    /**
     * Save a milestone config (insert or update).
     *
     * @param config MilestoneConfig to save
     * @return Saved config with generated ID if new
     */
    MilestoneConfig save(MilestoneConfig config);

    /**
     * Find milestone config by ID.
     *
     * @param id Config ID
     * @return Optional containing config if found
     */
    Optional<MilestoneConfig> findById(Long id);

    /**
     * Find all active milestone configs.
     *
     * @return List of active configs
     */
    List<MilestoneConfig> findAllActive();

    /**
     * Find all milestone configs by milestone type.
     *
     * @param milestoneType WITH_REFERRER or WITHOUT_REFERRER
     * @return List of configs for given type
     */
    List<MilestoneConfig> findByMilestoneType(MilestoneType milestoneType);

    /**
     * Find active milestone configs by milestone type.
     *
     * @param milestoneType WITH_REFERRER or WITHOUT_REFERRER
     * @return List of active configs for given type
     */
    List<MilestoneConfig> findActiveByMilestoneType(MilestoneType milestoneType);

    /**
     * Find milestone config by type and orders required.
     *
     * @param milestoneType   WITH_REFERRER or WITHOUT_REFERRER
     * @param ordersRequired Number of orders for milestone
     * @return Optional containing config if found
     */
    Optional<MilestoneConfig> findByMilestoneTypeAndOrdersRequired(
            MilestoneType milestoneType,
            Integer ordersRequired
    );

    /**
     * Find active milestone config by type and orders required.
     *
     * @param milestoneType   WITH_REFERRER or WITHOUT_REFERRER
     * @param ordersRequired Number of orders for milestone
     * @return Optional containing config if found
     */
    Optional<MilestoneConfig> findActiveByMilestoneTypeAndOrdersRequired(
            MilestoneType milestoneType,
            Integer ordersRequired
    );

    /**
     * Find all milestone configs where orders required is greater than given value.
     * Useful for finding next milestone for a user.
     *
     * @param milestoneType   WITH_REFERRER or WITHOUT_REFERRER
     * @param currentOrders Current number of completed orders
     * @return List of milestone configs with higher order requirements
     */
    List<MilestoneConfig> findNextMilestones(MilestoneType milestoneType, Integer currentOrders);

    /**
     * Check if a milestone config exists for type and orders.
     *
     * @param milestoneType   WITH_REFERRER or WITHOUT_REFERRER
     * @param ordersRequired Number of orders for milestone
     * @return true if config exists
     */
    boolean existsByMilestoneTypeAndOrdersRequired(MilestoneType milestoneType, Integer ordersRequired);

    /**
     * Delete a milestone config by ID.
     *
     * @param id Config ID
     */
    void deleteById(Long id);

    /**
     * Find all milestone configs (both active and inactive).
     *
     * @return List of all configs
     */
    List<MilestoneConfig> findAll();

    /**
     * Find all ACTIVE milestone configs where orders_required <= maxOrders.
     * Used for re-processing: find all milestones a user has already qualified for.
     *
     * <p>Example: User has 15 orders, this returns milestones at 1, 5, 10 orders
     * (all milestones the user has already passed).
     *
     * @param milestoneType WITH_REFERRER or WITHOUT_REFERRER
     * @param maxOrders     Maximum orders (user's current completed orders)
     * @return List of milestone configs sorted by orders_required ASC
     */
    List<MilestoneConfig> findActiveByMilestoneTypeAndOrdersRequiredLessThanOrEqual(
            MilestoneType milestoneType,
            Integer maxOrders
    );
}
