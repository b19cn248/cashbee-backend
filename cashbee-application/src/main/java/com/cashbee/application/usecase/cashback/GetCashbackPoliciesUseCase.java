package com.cashbee.application.usecase.cashback;

import com.cashbee.application.dto.cashback.CashbackPolicyResponse;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all cashback policies.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetCashbackPoliciesUseCase {

    private final CashbackPolicyRepository policyRepository;

    /**
     * Get all cashback policies.
     *
     * @return list of all policies
     */
    @Transactional(readOnly = true)
    public List<CashbackPolicyResponse> execute() {
        log.info("UseCase: Getting all cashback policies");

        var policies = policyRepository.findAll();

        var response = policies.stream()
            .map(policy -> CashbackPolicyResponse.builder()
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
                .build())
            .collect(Collectors.toList());

        log.info("UseCase: Found {} cashback policies", response.size());
        return response;
    }
}
