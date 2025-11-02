package com.cashbee.application.usecase.cashback;

import com.cashbee.application.dto.cashback.CashbackPolicyResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.UserLevel;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for retrieving active cashback policy.
 *
 * This is the KEY use case for cashback calculation!
 * It finds the best matching policy based on platform, user level, and current date.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetActiveCashbackPolicyUseCase {

    private final CashbackPolicyRepository policyRepository;

    /**
     * Get active cashback policy for given criteria.
     *
     * @param platformId platform ID
     * @param userLevel user level
     * @return active policy
     * @throws NotFoundException if no active policy found
     */
    @Transactional(readOnly = true)
    public CashbackPolicyResponse execute(Long platformId, UserLevel userLevel) {
        log.info("UseCase: Getting active cashback policy for platform: {}, userLevel: {}",
            platformId, userLevel);

        var now = LocalDateTime.now();

        var policy = policyRepository.findActivePolicyFor(platformId, userLevel, now)
            .orElseThrow(() -> {
                log.error("No active cashback policy found for platform: {}, userLevel: {}",
                    platformId, userLevel);
                return new NotFoundException(
                    String.format("No active cashback policy found for platform %d and user level %s",
                        platformId, userLevel)
                );
            });

        var response = CashbackPolicyResponse.builder()
            .id(policy.getId())
            .policyName(policy.getPolicyName())
            .policyCode(policy.getPolicyCode())
            .platformId(policy.getPlatformId())
            .userLevel(policy.getUserLevel().name())
            .cashbackRate(policy.getCashbackRate())
            .minOrderValue(policy.getMinOrderValue())
            .maxCashbackPerOrder(policy.getMaxCashbackPerOrder())
            .isActive(policy.getIsActive())
            .priority(policy.getPriority())
            .effectiveFrom(policy.getEffectiveFrom())
            .effectiveTo(policy.getEffectiveTo())
            .build();

        log.info("UseCase: Found active policy: {} with rate: {}%",
            policy.getPolicyCode(), policy.getCashbackRate());

        return response;
    }
}
