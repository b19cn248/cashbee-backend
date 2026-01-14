package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.transaction.CreateTransactionCommand;
import com.cashbee.application.usecase.transaction.CreateTransactionUseCase;
import com.cashbee.domain.enums.*;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Use Case: Process referral milestone when an order is completed.
 *
 * This use case is called when an order status changes to PAID.
 * It handles:
 * - Incrementing the user's completed order count
 * - Checking and granting milestone rewards from milestone_config table
 * - Adding bonus directly to wallet (both referee and referrer)
 * - Creating transactions for tracking
 * - Activating referral at activation milestone
 * - Upgrading user tier at milestones
 *
 * Milestone Types:
 * - WITH_REFERRER: 5, 10, 80, 300 orders
 * - WITHOUT_REFERRER: 80, 300 orders only
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessReferralMilestoneUseCase {

    private final UserRepository userRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final MilestoneConfigRepository milestoneConfigRepository;
    private final UserWalletRepository walletRepository;
    private final CreateTransactionUseCase createTransactionUseCase;
    private final CashbackRepository cashbackRepository;
    private final UpdateReferrerTierUseCase updateReferrerTierUseCase;

    /**
     * Execute use case when an order is completed.
     *
     * @param userId ID of the user whose order was completed
     */
    @Transactional
    public void execute(Long userId) {
        log.info("Processing referral milestone for user: {}", userId);

        // 1. Find the user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for milestone processing: {}", userId);
            return;
        }

        // 2. Recalculate completed orders from cashback table
        // Count distinct orders that have CONFIRMED or PAID cashback status
        // This approach prevents double-counting during re-import scenarios
        int completedOrders = cashbackRepository.countConfirmedOrdersByUserId(userId);
        int previousCount = user.getTotalCompletedOrders() != null ? user.getTotalCompletedOrders() : 0;
        user.setTotalCompletedOrders(completedOrders);

        log.debug("User {} completed orders updated: {} -> {}", userId, previousCount, completedOrders);

        // 3. Determine milestone type based on referrer status
        MilestoneType milestoneType = user.hasReferrer()
                ? MilestoneType.WITH_REFERRER
                : MilestoneType.WITHOUT_REFERRER;

        // 4. Find referrer if user was referred
        User referrer = null;
        if (user.hasReferrer()) {
            referrer = userRepository.findByReferralCode(user.getReferredBy()).orElse(null);
            if (referrer != null) {
                log.debug("User {} was referred by: {} (id={})",
                        userId, user.getReferredBy(), referrer.getId());
            }
        }

        // 5. Check if current order count matches any milestone
        Optional<MilestoneConfig> milestoneConfigOpt = milestoneConfigRepository
                .findActiveByMilestoneTypeAndOrdersRequired(milestoneType, completedOrders);

        if (milestoneConfigOpt.isPresent()) {
            processMilestone(user, referrer, milestoneConfigOpt.get());
        }

        // 6. Save user
        userRepository.save(user);

        log.info("Milestone processing completed for user: {}, orders: {}, type: {}",
                userId, completedOrders, milestoneType);
    }

    /**
     * Process a milestone when user reaches the required order count.
     */
    private void processMilestone(User user, User referrer, MilestoneConfig config) {
        Long userId = user.getId();
        Integer milestone = config.getOrdersRequired();
        Long referrerId = referrer != null ? referrer.getId() : null;

        log.info("Processing milestone {} for user {}", milestone, userId);

        // Check if reward already granted (prevent duplicates)
        if (referralRewardRepository.existsByUserIdAndMilestone(userId, milestone)) {
            log.debug("Milestone {} reward already granted for user: {}", milestone, userId);
            return;
        }

        // 1. Handle referral activation (for WITH_REFERRER milestones only)
        if (config.activatesReferralCommission() && referrer != null) {
            user.activateReferral(config.getCommissionMonths());
            log.info("Referral activated for user: {}, referrer: {}, duration: {} months",
                    userId, referrer.getId(), config.getCommissionMonths());

            // Increment referrer's activated referrals count and update tier
            updateReferrerTierUseCase.incrementAndUpdateTier(referrer.getId());
        }

        // 2. Handle tier upgrade
        if (config.hasTierUpgrade()) {
            UserLevel newTier = config.getNewTier();
            // Only upgrade if current level is lower
            if (shouldUpgrade(user.getUserLevel(), newTier)) {
                user.upgradeTo(newTier);
                log.info("User {} upgraded to tier: {}", userId, newTier);

                // Create tier upgrade reward record
                ReferralReward tierReward = ReferralReward.createTierUpgrade(
                        userId,
                        referrerId,
                        milestone,
                        newTier
                );
                tierReward.grant();
                referralRewardRepository.save(tierReward);
            }
        }

        // 3. Handle referee bonus (for the user completing orders)
        if (config.hasRefereeBonus()) {
            BigDecimal refereeBonus = config.getRefereeBonus();
            ReferralReward reward = ReferralReward.createMilestoneBonus(
                    userId,
                    referrerId,
                    milestone,
                    refereeBonus
            );
            reward.grant();
            ReferralReward savedReward = referralRewardRepository.save(reward);

            // Add bonus to referee's wallet
            addBonusToWallet(user, refereeBonus, savedReward.getId(),
                    TransactionSourceType.MILESTONE_BONUS,
                    "Milestone " + milestone + " bonus");

            log.info("Referee bonus granted: userId={}, milestone={}, amount={}",
                    userId, milestone, refereeBonus);
        }

        // 4. Handle referrer bonus (for the user who referred)
        if (config.hasReferrerBonus() && referrer != null) {
            BigDecimal referrerBonus = config.getReferrerBonus();

            // Create reward record for referrer
            ReferralReward referrerReward = ReferralReward.builder()
                    .userId(referrer.getId())
                    .referrerId(null) // Referrer's own reward
                    .rewardType(ReferralRewardType.MILESTONE_BONUS)
                    .milestone(milestone)
                    .amount(referrerBonus)
                    .status(ReferralRewardStatus.GRANTED)
                    .build();
            referrerReward.grant();
            ReferralReward savedReferrerReward = referralRewardRepository.save(referrerReward);

            // Add bonus to referrer's wallet
            addBonusToWallet(referrer, referrerBonus, savedReferrerReward.getId(),
                    TransactionSourceType.REFERRER_BONUS,
                    "Referrer bonus for milestone " + milestone);

            log.info("Referrer bonus granted: referrerId={}, milestone={}, amount={}",
                    referrer.getId(), milestone, referrerBonus);
        }
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

        // Record balance before
        BigDecimal balanceBefore = wallet.getBalance();

        // Add bonus to wallet
        wallet.addBonus(amount);
        walletRepository.save(wallet);

        // Record balance after
        BigDecimal balanceAfter = wallet.getBalance();

        // Create transaction for tracking
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

        // Order: NORMAL < VIP < SUPER
        int currentOrdinal = current.ordinal();
        int targetOrdinal = target.ordinal();

        return targetOrdinal > currentOrdinal;
    }
}
