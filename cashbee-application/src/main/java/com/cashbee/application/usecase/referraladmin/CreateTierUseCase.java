package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.CreateTierRequest;
import com.cashbee.application.dto.referraladmin.ReferrerTierConfigResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use case for creating a new referrer tier configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTierUseCase {

    private final ReferrerTierConfigRepository repository;

    /**
     * Create a new referrer tier configuration.
     *
     * @param request create request
     * @return created tier response
     * @throws BusinessException if validation fails or duplicate exists
     */
    @Transactional
    public ReferrerTierConfigResponse execute(CreateTierRequest request) {
        log.info("UseCase: Creating tier - name: {}, minReferrals: {}",
                request.getTierName(), request.getMinReferrals());

        // Check for duplicate tier name
        if (repository.findByTierName(request.getTierName()).isPresent()) {
            throw new BusinessException(
                    "DUPLICATE_TIER",
                    "A tier already exists with name: " + request.getTierName()
            );
        }

        // Build domain model
        ReferrerTierConfig config = ReferrerTierConfig.builder()
                .tierName(request.getTierName())
                .minReferrals(request.getMinReferrals())
                .commissionRate(request.getCommissionRate())
                .bonusPerActivation(request.getBonusPerActivation() != null
                        ? request.getBonusPerActivation() : BigDecimal.ZERO)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save
        ReferrerTierConfig saved = repository.save(config);

        log.info("UseCase: Created tier with ID: {}", saved.getId());

        return buildResponse(saved);
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
