package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.transaction.CreateTransactionCommand;
import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.application.usecase.transaction.CreateTransactionUseCase;
import com.cashbee.domain.enums.*;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.model.ReferralReward;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserWallet;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import com.cashbee.domain.repository.ReferralRewardRepository;
import com.cashbee.domain.repository.UserRepository;
import com.cashbee.domain.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessReferralMilestoneUseCase.
 * Tests milestone achievements and reward granting using milestone_config table.
 *
 * New Milestone Structure:
 * - WITH_REFERRER: 5 orders (B+10k, A+20k, activate 5 months), 10 orders (B+20k), 80 (VIP), 300 (SUPER)
 * - WITHOUT_REFERRER: 80 orders (VIP), 300 orders (SUPER)
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
class ProcessReferralMilestoneUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReferralRewardRepository referralRewardRepository;

    @Mock
    private MilestoneConfigRepository milestoneConfigRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private CreateTransactionUseCase createTransactionUseCase;

    @InjectMocks
    private ProcessReferralMilestoneUseCase useCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<ReferralReward> rewardCaptor;

    @Captor
    private ArgumentCaptor<UserWallet> walletCaptor;

    @Captor
    private ArgumentCaptor<CreateTransactionCommand> transactionCaptor;

    private User referee;
    private User referrer;
    private UserWallet refereeWallet;
    private UserWallet referrerWallet;

    @BeforeEach
    void setUp() {
        referee = User.builder()
                .id(1L)
                .keycloakId("referee-keycloak-id")
                .username("referee_user")
                .email("referee@example.com")
                .referralCode("REFEREE1")
                .referredBy("REFCODE1")
                .userLevel(UserLevel.NORMAL)
                .totalCompletedOrders(4) // Will become 5 after increment
                .status(UserStatus.ACTIVE)
                .build();

        referrer = User.builder()
                .id(2L)
                .keycloakId("referrer-keycloak-id")
                .username("referrer_user")
                .email("referrer@example.com")
                .referralCode("REFCODE1")
                .userLevel(UserLevel.VIP)
                .totalCompletedOrders(50)
                .status(UserStatus.ACTIVE)
                .build();

        refereeWallet = UserWallet.builder()
                .id(10L)
                .userId(1L)
                .balance(BigDecimal.valueOf(100000))
                .build();

        referrerWallet = UserWallet.builder()
                .id(20L)
                .userId(2L)
                .balance(BigDecimal.valueOf(500000))
                .build();
    }

    @Nested
    @DisplayName("Milestone 5 - Activation milestone (WITH_REFERRER)")
    class Milestone5 {

        private MilestoneConfig activationConfig;

        @BeforeEach
        void setUp() {
            activationConfig = MilestoneConfig.builder()
                    .id(1L)
                    .milestoneType(MilestoneType.WITH_REFERRER)
                    .ordersRequired(5)
                    .refereeBonus(BigDecimal.valueOf(10000))
                    .referrerBonus(BigDecimal.valueOf(20000))
                    .activatesReferral(true)
                    .commissionMonths(5)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("Should grant 10,000 VND bonus to referee and 20,000 VND to referrer at 5 orders")
        void shouldGrantBonusesToBothPartiesAt5Orders() {
            // Given
            referee.setTotalCompletedOrders(4); // Will be 5 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 5)).thenReturn(Optional.of(activationConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 5)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> {
                ReferralReward r = inv.getArgument(0);
                return ReferralReward.builder()
                        .id(100L)
                        .userId(r.getUserId())
                        .referrerId(r.getReferrerId())
                        .rewardType(r.getRewardType())
                        .milestone(r.getMilestone())
                        .amount(r.getAmount())
                        .status(r.getStatus())
                        .build();
            });
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(refereeWallet));
            when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(referrerWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(createTransactionUseCase.execute(any())).thenReturn(mock(TransactionResponse.class));

            // When
            useCase.execute(1L);

            // Then - verify 2 rewards saved (referee + referrer)
            verify(referralRewardRepository, times(2)).save(rewardCaptor.capture());
            var rewards = rewardCaptor.getAllValues();

            // Referee reward
            ReferralReward refereeReward = rewards.get(0);
            assertThat(refereeReward.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(refereeReward.getUserId()).isEqualTo(1L);
            assertThat(refereeReward.getMilestone()).isEqualTo(5);

            // Referrer reward
            ReferralReward referrerReward = rewards.get(1);
            assertThat(referrerReward.getAmount()).isEqualTo(BigDecimal.valueOf(20000));
            assertThat(referrerReward.getUserId()).isEqualTo(2L);
            assertThat(referrerReward.getMilestone()).isEqualTo(5);

            // Verify transactions created for both
            verify(createTransactionUseCase, times(2)).execute(transactionCaptor.capture());
            var transactions = transactionCaptor.getAllValues();

            // Referee transaction
            assertThat(transactions.get(0).getAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(transactions.get(0).getSourceType()).isEqualTo(TransactionSourceType.MILESTONE_BONUS);

            // Referrer transaction
            assertThat(transactions.get(1).getAmount()).isEqualTo(BigDecimal.valueOf(20000));
            assertThat(transactions.get(1).getSourceType()).isEqualTo(TransactionSourceType.REFERRER_BONUS);
        }

        @Test
        @DisplayName("Should activate referral with 5 months commission at 5 orders")
        void shouldActivateReferralAt5Orders() {
            // Given
            referee.setTotalCompletedOrders(4);
            assertThat(referee.isReferralActivated()).isFalse();

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 5)).thenReturn(Optional.of(activationConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 5)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> {
                ReferralReward r = inv.getArgument(0);
                return ReferralReward.builder().id(100L).userId(r.getUserId()).build();
            });
            when(walletRepository.findByUserId(anyLong())).thenReturn(Optional.of(refereeWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(createTransactionUseCase.execute(any())).thenReturn(mock(TransactionResponse.class));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(5);
            assertThat(savedUser.isReferralActivated()).isTrue();
            assertThat(savedUser.getReferralExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not grant duplicate reward for milestone 5")
        void shouldNotGrantDuplicateRewardForMilestone5() {
            // Given
            referee.setTotalCompletedOrders(4);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 5)).thenReturn(Optional.of(activationConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 5)).thenReturn(true);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository, never()).save(any());
            verify(createTransactionUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Milestone 10 - Second bonus (WITH_REFERRER)")
    class Milestone10 {

        private MilestoneConfig milestone10Config;

        @BeforeEach
        void setUp() {
            milestone10Config = MilestoneConfig.builder()
                    .id(2L)
                    .milestoneType(MilestoneType.WITH_REFERRER)
                    .ordersRequired(10)
                    .refereeBonus(BigDecimal.valueOf(20000))
                    .referrerBonus(BigDecimal.ZERO)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("Should grant 20,000 VND bonus to referee at 10 orders (no referrer bonus)")
        void shouldGrantBonusAt10Orders() {
            // Given
            referee.setTotalCompletedOrders(9); // Will be 10 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 10)).thenReturn(Optional.of(milestone10Config));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 10)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> {
                ReferralReward r = inv.getArgument(0);
                return ReferralReward.builder().id(100L).userId(r.getUserId()).amount(r.getAmount()).build();
            });
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(refereeWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(createTransactionUseCase.execute(any())).thenReturn(mock(TransactionResponse.class));

            // When
            useCase.execute(1L);

            // Then - only 1 reward (referee only, no referrer bonus)
            verify(referralRewardRepository, times(1)).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getAmount()).isEqualTo(BigDecimal.valueOf(20000));
            assertThat(reward.getUserId()).isEqualTo(1L);
            assertThat(reward.getMilestone()).isEqualTo(10);

            // Only 1 transaction (for referee)
            verify(createTransactionUseCase, times(1)).execute(any());
        }
    }

    @Nested
    @DisplayName("Milestone 80 - VIP tier upgrade")
    class Milestone80 {

        private MilestoneConfig vipConfig;

        @BeforeEach
        void setUp() {
            vipConfig = MilestoneConfig.builder()
                    .id(3L)
                    .milestoneType(MilestoneType.WITH_REFERRER)
                    .ordersRequired(80)
                    .refereeBonus(BigDecimal.ZERO)
                    .referrerBonus(BigDecimal.ZERO)
                    .newTier(UserLevel.VIP)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("Should upgrade to VIP tier at 80 orders")
        void shouldUpgradeToVipAt80Orders() {
            // Given
            referee.setTotalCompletedOrders(79); // Will be 80 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 80)).thenReturn(Optional.of(vipConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 80)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.TIER_UPGRADE);
            assertThat(reward.getMilestone()).isEqualTo(80);
            assertThat(reward.getNewTier()).isEqualTo(UserLevel.VIP);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.VIP);
        }
    }

    @Nested
    @DisplayName("Milestone 300 - SUPER tier upgrade")
    class Milestone300 {

        private MilestoneConfig superConfig;

        @BeforeEach
        void setUp() {
            superConfig = MilestoneConfig.builder()
                    .id(4L)
                    .milestoneType(MilestoneType.WITH_REFERRER)
                    .ordersRequired(300)
                    .refereeBonus(BigDecimal.ZERO)
                    .referrerBonus(BigDecimal.ZERO)
                    .newTier(UserLevel.SUPER)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("Should upgrade to SUPER tier at 300 orders")
        void shouldUpgradeToSuperAt300Orders() {
            // Given
            referee.setUserLevel(UserLevel.VIP);
            referee.setTotalCompletedOrders(299); // Will be 300 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 300)).thenReturn(Optional.of(superConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 300)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();

            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.TIER_UPGRADE);
            assertThat(reward.getMilestone()).isEqualTo(300);
            assertThat(reward.getNewTier()).isEqualTo(UserLevel.SUPER);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.SUPER);
        }
    }

    @Nested
    @DisplayName("WITHOUT_REFERRER scenarios")
    class WithoutReferrer {

        private MilestoneConfig vipConfigNoReferrer;

        @BeforeEach
        void setUp() {
            vipConfigNoReferrer = MilestoneConfig.builder()
                    .id(5L)
                    .milestoneType(MilestoneType.WITHOUT_REFERRER)
                    .ordersRequired(80)
                    .refereeBonus(BigDecimal.ZERO)
                    .referrerBonus(BigDecimal.ZERO)
                    .newTier(UserLevel.VIP)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("Should increment orders for non-referred user (no milestone match at 6 orders)")
        void shouldIncrementOrdersForNonReferredUser() {
            // Given
            referee.setReferredBy(null); // No referrer
            referee.setTotalCompletedOrders(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITHOUT_REFERRER, 6)).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(6);
            verify(referralRewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should grant tier upgrade for non-referred user at milestone 80")
        void shouldGrantTierUpgradeForNonReferredUser() {
            // Given
            referee.setReferredBy(null); // No referrer
            referee.setTotalCompletedOrders(79); // Will be 80 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITHOUT_REFERRER, 80)).thenReturn(Optional.of(vipConfigNoReferrer));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 80)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUserLevel()).isEqualTo(UserLevel.VIP);

            // Reward should be created without referrerId
            verify(referralRewardRepository).save(rewardCaptor.capture());
            ReferralReward reward = rewardCaptor.getValue();
            assertThat(reward.getReferrerId()).isNull();
            assertThat(reward.getRewardType()).isEqualTo(ReferralRewardType.TIER_UPGRADE);
        }

        @Test
        @DisplayName("Should NOT grant bonus at 5 orders for user without referrer")
        void shouldNotGrantBonusAt5OrdersForNonReferredUser() {
            // Given
            referee.setReferredBy(null); // No referrer
            referee.setTotalCompletedOrders(4); // Will be 5 after increment

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            // No milestone config for WITHOUT_REFERRER at 5 orders
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITHOUT_REFERRER, 5)).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then - no rewards granted
            verify(referralRewardRepository, never()).save(any());
            verify(createTransactionUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should do nothing if user not found")
        void shouldDoNothingIfUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            useCase.execute(999L);

            // Then
            verify(userRepository, never()).save(any());
            verify(referralRewardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null totalCompletedOrders")
        void shouldHandleNullTotalCompletedOrders() {
            // Given
            referee.setTotalCompletedOrders(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 1)).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            useCase.execute(1L);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getTotalCompletedOrders()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create wallet if not exists")
        void shouldCreateWalletIfNotExists() {
            // Given
            MilestoneConfig bonusConfig = MilestoneConfig.builder()
                    .id(1L)
                    .milestoneType(MilestoneType.WITH_REFERRER)
                    .ordersRequired(5)
                    .refereeBonus(BigDecimal.valueOf(10000))
                    .referrerBonus(BigDecimal.ZERO)
                    .isActive(true)
                    .build();

            referee.setTotalCompletedOrders(4);

            when(userRepository.findById(1L)).thenReturn(Optional.of(referee));
            when(userRepository.findByReferralCode("REFCODE1")).thenReturn(Optional.of(referrer));
            when(milestoneConfigRepository.findActiveByMilestoneTypeAndOrdersRequired(
                    MilestoneType.WITH_REFERRER, 5)).thenReturn(Optional.of(bonusConfig));
            when(referralRewardRepository.existsByUserIdAndMilestone(1L, 5)).thenReturn(false);
            when(referralRewardRepository.save(any())).thenAnswer(inv -> {
                ReferralReward r = inv.getArgument(0);
                return ReferralReward.builder().id(100L).userId(r.getUserId()).build();
            });
            when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty()); // No wallet
            when(walletRepository.save(any())).thenAnswer(inv -> {
                UserWallet w = inv.getArgument(0);
                w.setId(99L);
                return w;
            });
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(createTransactionUseCase.execute(any())).thenReturn(mock(TransactionResponse.class));

            // When
            useCase.execute(1L);

            // Then - wallet should be created
            verify(walletRepository, times(2)).save(walletCaptor.capture());
            UserWallet createdWallet = walletCaptor.getAllValues().get(0);
            assertThat(createdWallet.getUserId()).isEqualTo(1L);
        }
    }
}
