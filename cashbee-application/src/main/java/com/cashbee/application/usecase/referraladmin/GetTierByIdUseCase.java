package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.ReferrerTierConfigResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a referrer tier configuration by ID.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetTierByIdUseCase {

    private final ReferrerTierConfigRepository repository;

    /**
     * Get a referrer tier configuration by ID.
     *
     * @param id tier config ID
     * @return tier config response
     * @throws BusinessException if not found
     */
    @Transactional(readOnly = true)
    public ReferrerTierConfigResponse execute(Long id) {
        log.info("UseCase: Getting tier by ID: {}", id);

        ReferrerTierConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "TIER_NOT_FOUND",
                        "Referrer tier configuration not found with ID: " + id
                ));

        log.info("UseCase: Found tier - name: {}, minReferrals: {}",
                config.getTierName(), config.getMinReferrals());

        return buildResponse(config);
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
