package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.UpdatePlatformConfigRequest;
import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating platform affiliate configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdatePlatformConfigUseCase {

    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute use case to update platform configuration.
     *
     * @param platformCode Platform code (e.g., "shopee")
     * @param request Update request
     * @return Updated platform response
     * @throws NotFoundException if platform not found
     */
    @Transactional
    public AffiliatePlatformResponse execute(String platformCode, UpdatePlatformConfigRequest request) {
        log.info("UseCase: Updating platform config for: {}", platformCode);

        // Get platform
        AffiliatePlatform platform = platformRepository.findByCode(platformCode)
            .orElseThrow(() -> {
                log.error("UseCase: Platform not found: {}", platformCode);
                return new NotFoundException("Platform not found: " + platformCode);
            });

        // Update config
        platform.setAffiliateId(request.getAffiliateId());
        platform.setLinkTemplate(request.getLinkTemplate());

        if (request.getTrackingEnabled() != null) {
            platform.setTrackingEnabled(request.getTrackingEnabled());
        }

        // Save
        platform = platformRepository.save(platform);

        log.info("UseCase: Updated platform config for: {}", platformCode);

        // Build response
        return AffiliatePlatformResponse.builder()
            .id(platform.getId())
            .name(platform.getName())
            .code(platform.getCode())
            .defaultCommissionRate(platform.getDefaultCommissionRate())
            .status(platform.getStatus().name())
            .hasApiCredentials(platform.hasApiCredentials())
            .baseUrl(platform.getBaseUrl())
            .build();
    }
}
