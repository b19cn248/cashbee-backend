package com.cashbee.domain.repository;

import com.cashbee.domain.model.ReferrerTierConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReferrerTierConfig entity.
 * This is a port in Hexagonal Architecture.
 *
 * @author CashBee Team
 */
public interface ReferrerTierConfigRepository {

    /**
     * Find tier config by ID.
     *
     * @param id tier config ID
     * @return optional tier config
     */
    Optional<ReferrerTierConfig> findById(Long id);

    /**
     * Find tier config by tier name.
     *
     * @param tierName tier name (BRONZE, SILVER, GOLD)
     * @return optional tier config
     */
    Optional<ReferrerTierConfig> findByTierName(String tierName);

    /**
     * Find all active tier configs ordered by min_referrals ascending.
     *
     * @return list of active tier configs
     */
    List<ReferrerTierConfig> findAllActiveOrderByMinReferrals();

    /**
     * Find the tier config that matches the given referral count.
     * Returns the highest tier that the referral count qualifies for.
     *
     * @param activatedReferrals number of activated referrals
     * @return optional tier config (highest qualifying tier)
     */
    Optional<ReferrerTierConfig> findTierByReferralCount(int activatedReferrals);

    /**
     * Save a tier config.
     *
     * @param tierConfig tier config to save
     * @return saved tier config
     */
    ReferrerTierConfig save(ReferrerTierConfig tierConfig);

    /**
     * Find all tier configs (both active and inactive).
     *
     * @return list of all tier configs
     */
    List<ReferrerTierConfig> findAll();

    /**
     * Delete a tier config by ID.
     *
     * @param id tier config ID
     */
    void deleteById(Long id);
}
