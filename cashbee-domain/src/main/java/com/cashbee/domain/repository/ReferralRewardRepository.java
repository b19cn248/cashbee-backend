package com.cashbee.domain.repository;

import com.cashbee.domain.enums.ReferralRewardStatus;
import com.cashbee.domain.enums.ReferralRewardType;
import com.cashbee.domain.model.ReferralReward;

import java.util.List;
import java.util.Optional;

/**
 * ReferralReward Repository Interface (Port).
 *
 * <p>This is a domain interface that defines the contract for referral reward persistence.
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
public interface ReferralRewardRepository {

    /**
     * Save a referral reward (insert or update).
     *
     * @param reward ReferralReward to save
     * @return Saved reward with generated ID if new
     */
    ReferralReward save(ReferralReward reward);

    /**
     * Find referral reward by ID.
     *
     * @param id Reward ID
     * @return Optional containing reward if found
     */
    Optional<ReferralReward> findById(Long id);

    /**
     * Find all rewards for a user.
     *
     * @param userId User ID
     * @return List of rewards for the user
     */
    List<ReferralReward> findByUserId(Long userId);

    /**
     * Find all rewards for a user by type.
     *
     * @param userId     User ID
     * @param rewardType Reward type
     * @return List of rewards matching criteria
     */
    List<ReferralReward> findByUserIdAndRewardType(Long userId, ReferralRewardType rewardType);

    /**
     * Find a specific reward by user and milestone.
     *
     * @param userId    User ID
     * @param milestone Milestone value (3, 10, 40, 150)
     * @return Optional containing reward if found
     */
    Optional<ReferralReward> findByUserIdAndMilestone(Long userId, Integer milestone);

    /**
     * Check if a reward exists for user and milestone.
     * Used to prevent duplicate rewards.
     *
     * @param userId    User ID
     * @param milestone Milestone value
     * @return true if reward already exists
     */
    boolean existsByUserIdAndMilestone(Long userId, Integer milestone);

    /**
     * Find all rewards by status.
     *
     * @param status Reward status
     * @return List of rewards with given status
     */
    List<ReferralReward> findByStatus(ReferralRewardStatus status);

    /**
     * Find all rewards granted by a specific referrer.
     *
     * @param referrerId Referrer user ID
     * @return List of rewards associated with the referrer
     */
    List<ReferralReward> findByReferrerId(Long referrerId);

    /**
     * Count rewards by user ID.
     *
     * @param userId User ID
     * @return Number of rewards for the user
     */
    long countByUserId(Long userId);

    /**
     * Count rewards by referrer ID.
     *
     * @param referrerId Referrer user ID
     * @return Number of rewards associated with the referrer
     */
    long countByReferrerId(Long referrerId);
}
