package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.AffiliatePlatformResponse;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all affiliate platforms.
 *
 * Returns list of all platforms (both active and inactive).
 * Typically used by admin to view all platforms.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAffiliatePlatformsUseCase {

    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute use case to get all affiliate platforms.
     *
     * @return list of platform response DTOs
     */
    @Transactional(readOnly = true)
    public List<AffiliatePlatformResponse> execute() {
        log.info("UseCase: Getting all affiliate platforms");

        var platforms = platformRepository.findAll();

        var response = platforms.stream()
            .map(platform -> AffiliatePlatformResponse.builder()
                .id(platform.getId())
                .name(platform.getName())
                .code(platform.getCode())
                .defaultCommissionRate(platform.getDefaultCommissionRate())
                .status(platform.getStatus().name())
                .hasApiCredentials(platform.hasApiCredentials())
                .baseUrl(platform.getBaseUrl())
                .build())
            .collect(Collectors.toList());

        log.info("UseCase: Found {} affiliate platforms", response.size());
        return response;
    }
}
