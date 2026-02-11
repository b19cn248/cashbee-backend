package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.GrantMissingRewardsResponse;
import com.cashbee.application.dto.referraladmin.GrantMissingRewardsResponse.GrantedRewardDetail;
import com.cashbee.application.dto.referraladmin.MissingMilestoneInfo;
import com.cashbee.application.dto.transaction.CreateTransactionCommand;
import com.cashbee.application.usecase.referral.UpdateReferrerTierUseCase;
import com.cashbee.application.usecase.transaction.CreateTransactionUseCase;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.*;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Use Case: Grant missing milestone rewards to users.
 *
 * This use case handles retroactive granting of milestone rewards that were missed
 * due to milestones being added after users already passed them.
 *
 * Logic:
 * 1. Calculate user's actual completed orders from cashback table
 * 2. Find all milestones they should have received based on order count
 * 3. Compare with milestones already granted (referral_reward table)
 * 4. Grant any missing rewards
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrantMissingMilestoneRewardsUseCase {

    private final UserRepository userRepository;
    private final MilestoneConfigRepository milestoneConfigRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final CashbackRepository cashbackRepository;
    private final UserWalletRepository walletRepository;
    private final CreateTransactionUseCase createTransactionUseCase;
    private final UpdateReferrerTierUseCase updateReferrerTierUseCase;

    /**
     * Find all missing milestone rewards for a user.
     *
     * @param userId User ID to check
     * @return List of missing milestone info
     */
    public List<MissingMilestoneInfo> findMissingRewards(Long userId) {
        log.info("Finding missing milestone rewards for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return findMissingRewardsForUser(user);
    }

    /**
     * Find all missing milestone rewards for ALL users.
     *
     * @return List of all missing milestone info across all users
     */
    public List<MissingMilestoneInfo> findAllMissingRewards() {
        log.info("Finding all missing milestone rewards for all users");

        List<MissingMilestoneInfo> allMissing = new ArrayList<>();
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            List<MissingMilestoneInfo> userMissing = findMissingRewardsForUser(user);
            allMissing.addAll(userMissing);
        }

        log.info("Found {} total missing rewards across {} users", allMissing.size(), allUsers.size());
        return allMissing;
    }

    /**
     * Grant all missing milestone rewards for a specific user.
     *
     * @param userId User ID to grant rewards for
     * @return Response with details of granted rewards
     */
    @Transactional
    public GrantMissingRewardsResponse grantMissingRewardsForUser(Long userId) {
        log.info("Granting missing milestone rewards for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return processGrantMissingRewards(user);
    }

    /**
     * Grant all missing milestone rewards for ALL users.
     *
     * @return List of responses for each user processed
     */
    @Transactional
    public List<GrantMissingRewardsResponse> grantAllMissingRewards() {
        log.info("Granting all missing milestone rewards for all users");

        List<GrantMissingRewardsResponse> responses = new ArrayList<>();
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                GrantMissingRewardsResponse response = processGrantMissingRewards(user);
                if (response.getRewardsGranted() > 0) {
                    responses.add(response);
                }
            } catch (Exception e) {
                log.error("Failed to grant missing rewards for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("Processed {} users with missing rewards", responses.size());
        return responses;
    }

    /**
     * Find missing rewards for a specific user.
     */
    private List<MissingMilestoneInfo> findMissingRewardsForUser(User user) {
        Long userId = user.getId();
        List<MissingMilestoneInfo> missing = new ArrayList<>();

        // Get actual completed orders count from cashback table
        int actualOrders = cashbackRepository.countConfirmedOrdersByUserId(userId);
        if (actualOrders == 0) {
            return missing;
        }

        // Find referrer if user has one
        User referrer = null;
        if (user.hasReferrer()) {
            referrer = userRepository.findByReferralCode(user.getReferredBy()).orElse(null);
        }

        // Determine milestone type
        MilestoneType milestoneType = referrer != null
                ? MilestoneType.WITH_REFERRER
                : MilestoneType.WITHOUT_REFERRER;

        // Get all active milestone configs
        List<MilestoneConfig> configs = milestoneConfigRepository.findActiveByMilestoneType(milestoneType);

        for (MilestoneConfig config : configs) {
            // Skip if user hasn't reached this milestone yet
            if (actualOrders < config.getOrdersRequired()) {
                continue;
            }

            // Check if reward already granted
            boolean alreadyGranted = referralRewardRepository.existsByUserIdAndMilestone(
                    userId, config.getOrdersRequired());

            if (!alreadyGranted) {
                MissingMilestoneInfo info = MissingMilestoneInfo.builder()
                        .userId(userId)
                        .username(user.getUsername())
                        .referrerId(referrer != null ? referrer.getId() : null)
                        .referrerUsername(referrer != null ? referrer.getUsername() : null)
                        .actualOrders(actualOrders)
                        .milestone(config.getOrdersRequired())
                        .description(config.getDescription())
                        .milestoneType(milestoneType)
                        .refereeBonus(config.getRefereeBonus())
                        .referrerBonus(config.getReferrerBonus())
                        .activatesReferral(config.getActivatesReferral())
                        .build();
                missing.add(info);
            }
        }

        return missing;
    }

    /**
     * Process and grant missing rewards for a user.
     */
    private GrantMissingRewardsResponse processGrantMissingRewards(User user) {
        Long userId = user.getId();
        List<MissingMilestoneInfo> missingRewards = findMissingRewardsForUser(user);

        List<GrantedRewardDetail> grantedDetails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal totalRefereeBonusGranted = BigDecimal.ZERO;
        BigDecimal totalReferrerBonusGranted = BigDecimal.ZERO;
        boolean referralActivated = false;

        // Find referrer
        User referrer = null;
        if (user.hasReferrer()) {
            referrer = userRepository.findByReferralCode(user.getReferredBy()).orElse(null);
        }

        // Sort by milestone order to process in sequence
        missingRewards.sort((a, b) -> a.getMilestone().compareTo(b.getMilestone()));

        for (MissingMilestoneInfo missing : missingRewards) {
            try {
                GrantedRewardDetail detail = grantSingleMilestoneReward(user, referrer, missing);
                grantedDetails.add(detail);

                if (detail.getRefereeBonusGranted() != null) {
                    totalRefereeBonusGranted = totalRefereeBonusGranted.add(detail.getRefereeBonusGranted());
                }
                if (detail.getReferrerBonusGranted() != null) {
                    totalReferrerBonusGranted = totalReferrerBonusGranted.add(detail.getReferrerBonusGranted());
                }
                if (Boolean.TRUE.equals(detail.getActivatedReferral())) {
                    referralActivated = true;
                }

            } catch (Exception e) {
                warnings.add("Failed to grant milestone " + missing.getMilestone() + ": " + e.getMessage());
                log.error("Failed to grant milestone {} for user {}: {}",
                        missing.getMilestone(), userId, e.getMessage(), e);
            }
        }

        // Save user if any changes were made
        if (!grantedDetails.isEmpty()) {
            userRepository.save(user);
        }

        return GrantMissingRewardsResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .missingMilestonesCount(missingRewards.size())
                .rewardsGranted(grantedDetails.size())
                .totalRefereeBonusGranted(totalRefereeBonusGranted)
                .totalReferrerBonusGranted(totalReferrerBonusGranted)
                .referralActivated(referralActivated)
                .grantedRewards(grantedDetails)
                .warnings(warnings)
                .build();
    }

    /**
     * Grant a single milestone reward.
     */
    private GrantedRewardDetail grantSingleMilestoneReward(User user, User referrer, MissingMilestoneInfo missing) {
        Long userId = user.getId();
        Long referrerId = referrer != null ? referrer.getId() : null;
        Integer milestone = missing.getMilestone();

        log.info("Granting missing milestone {} for user {}", milestone, userId);

        GrantedRewardDetail.GrantedRewardDetailBuilder detailBuilder = GrantedRewardDetail.builder()
                .milestone(milestone)
                .description(missing.getDescription())
                .referrerId(referrerId)
                .referrerUsername(referrer != null ? referrer.getUsername() : null)
                .activatedReferral(false);

        // Find milestone config for additional info (tier upgrade, etc.)
        Optional<MilestoneConfig> configOpt = milestoneConfigRepository
                .findActiveByMilestoneTypeAndOrdersRequired(missing.getMilestoneType(), milestone);

        MilestoneConfig config = configOpt.orElse(null);

        // 1. Handle referral activation
        if (Boolean.TRUE.equals(missing.getActivatesReferral()) && referrer != null && config != null) {
            if (user.getReferralActivatedAt() == null) {
                user.activateReferral(config.getCommissionMonths());
                log.info("Activated referral for user: {}, referrer: {}, duration: {} months",
                        userId, referrer.getId(), config.getCommissionMonths());

                // Increment referrer's activated referrals count
                updateReferrerTierUseCase.incrementAndUpdateTier(referrer.getId());
                detailBuilder.activatedReferral(true);
            }
        }

        // 2. Handle tier upgrade
        if (config != null && config.hasTierUpgrade()) {
            UserLevel newTier = config.getNewTier();
            if (shouldUpgrade(user.getUserLevel(), newTier)) {
                user.upgradeTo(newTier);
                log.info("User {} upgraded to tier: {}", userId, newTier);

                // Create tier upgrade reward record
                ReferralReward tierReward = ReferralReward.createTierUpgrade(
                        userId, referrerId, milestone, newTier);
                tierReward.grant();
                referralRewardRepository.save(tierReward);
            }
        }

        // 3. Handle referee bonus
        BigDecimal refereeBonus = missing.getRefereeBonus();
        if (refereeBonus != null && refereeBonus.compareTo(BigDecimal.ZERO) > 0) {
            ReferralReward reward = ReferralReward.createMilestoneBonus(
                    userId, referrerId, milestone, refereeBonus);
            reward.grant();
            ReferralReward savedReward = referralRewardRepository.save(reward);

            addBonusToWallet(user, refereeBonus, savedReward.getId(),
                    TransactionSourceType.MILESTONE_BONUS,
                    "[Retroactive] Milestone " + milestone + " bonus");

            detailBuilder.refereeBonusGranted(refereeBonus);
            log.info("Referee bonus granted: userId={}, milestone={}, amount={}",
                    userId, milestone, refereeBonus);
        }

        // 4. Handle referrer bonus
        BigDecimal referrerBonus = missing.getReferrerBonus();
        if (referrerBonus != null && referrerBonus.compareTo(BigDecimal.ZERO) > 0 && referrer != null) {
            // Create reward record for referrer
            ReferralReward referrerReward = ReferralReward.builder()
                    .userId(referrer.getId())
                    .referrerId(null)
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(milestone)
                    .amount(referrerBonus)
                    .status(ReferralRewardStatus.GRANTED)
                    .build();
            referrerReward.grant();
            ReferralReward savedReferrerReward = referralRewardRepository.save(referrerReward);

            addBonusToWallet(referrer, referrerBonus, savedReferrerReward.getId(),
                    TransactionSourceType.REFERRER_BONUS,
                    "[Retroactive] Referrer bonus for milestone " + milestone);

            detailBuilder.referrerBonusGranted(referrerBonus);
            log.info("Referrer bonus granted: referrerId={}, milestone={}, amount={}",
                    referrer.getId(), milestone, referrerBonus);
        }

        return detailBuilder.build();
    }

    /**
     * Add bonus to user's wallet and create transaction.
     */
    private void addBonusToWallet(User user, BigDecimal amount, Long rewardId,
                                   TransactionSourceType sourceType, String description) {
        Long userId = user.getId();

        // Get or create wallet
        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Wallet not found for user {}, creating new wallet", userId);
                    UserWallet newWallet = UserWallet.builder()
                            .userId(userId)
                            .build();
                    return walletRepository.save(newWallet);
                });

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.addBonus(amount);
        walletRepository.save(wallet);
        BigDecimal balanceAfter = wallet.getBalance();

        // Create transaction
        CreateTransactionCommand transactionCommand = CreateTransactionCommand.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .type(TransactionType.BONUS)
                .amount(amount)
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .sourceType(sourceType)
                .sourceId(rewardId)
                .build();

        createTransactionUseCase.execute(transactionCommand);

        log.debug("Bonus added to wallet: userId={}, amount={}, balance: {} -> {}",
                userId, amount, balanceBefore, balanceAfter);
    }

    /**
     * Check if user should be upgraded from current level to new level.
     */
    private boolean shouldUpgrade(UserLevel current, UserLevel target) {
        if (current == null || target == null) {
            return false;
        }
        return target.ordinal() > current.ordinal();
    }
}
