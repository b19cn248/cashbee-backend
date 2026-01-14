package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.ReferrerTierConfigResponse;
import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all referrer tier configurations.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAllTiersUseCase {

    private final ReferrerTierConfigRepository repository;

    /**
     * Get all referrer tier configurations.
     *
     * @return list of tier config responses
     */
    @Transactional(readOnly = true)
    public List<ReferrerTierConfigResponse> execute() {
        log.info("UseCase: Getting all referrer tiers");

        List<ReferrerTierConfig> configs = repository.findAll();

        log.info("UseCase: Found {} tiers", configs.size());

        return configs.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    private ReferrerTierConfigResponse buildResponse(ReferrerTierConfig config) {
        return ReferrerTierConfigResponse.builder()
                .id(config.getId())
                .tierName(config.getTierName())
                .minReferrals(config.getMinReferrals())
                .commissionRate(config.getCommissionRate())
                .bonusPerActivation(config.getBonusPerActivation())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
