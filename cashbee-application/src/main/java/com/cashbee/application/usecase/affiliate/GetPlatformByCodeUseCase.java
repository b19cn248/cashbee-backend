package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving affiliate platform by code.
 *
 * Returns platform details for a specific platform code (e.g., "shopee").
 * Throws NotFoundException if platform does not exist.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPlatformByCodeUseCase {

    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute use case to get platform by code.
     *
     * @param code platform code (e.g., "shopee", "lazada")
     * @return platform response DTO
     * @throws NotFoundException if platform not found
     */
    @Transactional(readOnly = true)
    public AffiliatePlatformResponse execute(String code) {
        log.info("UseCase: Getting affiliate platform by code: {}", code);

        var platform = platformRepository.findByCode(code)
            .orElseThrow(() -> {
                log.error("Platform not found with code: {}", code);
                return new NotFoundException("Affiliate platform not found: " + code);
            });

        var response = AffiliatePlatformResponse.builder()
            .id(platform.getId())
            .name(platform.getName())
            .code(platform.getCode())
            .defaultCommissionRate(platform.getDefaultCommissionRate())
            .status(platform.getStatus().name())
            .hasApiCredentials(platform.hasApiCredentials())
            .baseUrl(platform.getBaseUrl())
            .build();

        log.info("UseCase: Found platform: {}", platform.getName());
        return response;
    }
}
