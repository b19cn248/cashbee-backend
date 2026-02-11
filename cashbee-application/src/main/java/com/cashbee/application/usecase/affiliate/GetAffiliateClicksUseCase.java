package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.AffiliateClickResponse;
import com.cashbee.application.dto.affiliate.GetAffiliateClicksQuery;
import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case for getting affiliate clicks (tracking links) with filters and pagination.
 * Used by Admin to view all tracking links created by users.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetAffiliateClicksUseCase {

    private final AffiliateClickRepository clickRepository;
    private final UserRepository userRepository;
    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute the use case: Get all affiliate clicks with filters and pagination.
     *
     * @param query Query parameters (userId, platformId, status, search, page, size)
     * @return Paginated list of affiliate clicks
     */
    public PageResponse<AffiliateClickResponse> execute(GetAffiliateClicksQuery query) {
        log.info("[USE CASE] GetAffiliateClicks - userId={}, platformId={}, status={}, orderMatched={}, search='{}', page={}, size={}",
            query.getUserId(), query.getPlatformId(), query.getStatus(),
            query.getOrderMatched(), query.getSearch(), query.getPage(), query.getSize());

        // Validate page size
        int size = Math.min(query.getSize(), 100);
        int page = Math.max(query.getPage(), 0);

        // Get clicks with filters
        List<AffiliateClick> clicks = clickRepository.findAllWithFilters(
            query.getUserId(),
            query.getPlatformId(),
            query.getStatus(),
            query.getOrderMatched(),
            query.getSearch(),
            page,
            size
        );

        // Get total count for pagination
        long totalElements = clickRepository.countWithFilters(
            query.getUserId(),
            query.getPlatformId(),
            query.getStatus(),
            query.getOrderMatched(),
            query.getSearch()
        );

        // Get unique user IDs and platform IDs for enrichment
        List<Long> userIds = clicks.stream()
            .map(AffiliateClick::getUserId)
            .distinct()
            .toList();

        List<Long> platformIds = clicks.stream()
            .map(AffiliateClick::getPlatformId)
            .distinct()
            .toList();

        // Fetch users and platforms for enrichment
        Map<Long, String> userIdToUsername = userIds.stream()
            .map(userRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toMap(User::getId, User::getUsername));

        Map<Long, String> platformIdToName = platformIds.stream()
            .map(platformRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toMap(AffiliatePlatform::getId, AffiliatePlatform::getName));

        // Convert to response DTOs
        List<AffiliateClickResponse> content = clicks.stream()
            .map(click -> toResponse(click, userIdToUsername, platformIdToName))
            .toList();

        log.info("[USE CASE] GetAffiliateClicks - returning {} clicks (total: {})",
            content.size(), totalElements);

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * Convert domain model to response DTO with enriched data.
     */
    private AffiliateClickResponse toResponse(
            AffiliateClick click,
            Map<Long, String> userIdToUsername,
            Map<Long, String> platformIdToName) {

        return AffiliateClickResponse.builder()
            .id(click.getId())
            .userId(click.getUserId())
            .username(userIdToUsername.getOrDefault(click.getUserId(), "Unknown"))
            .platformId(click.getPlatformId())
            .platformName(platformIdToName.getOrDefault(click.getPlatformId(), "Unknown"))
            .shopId(click.getShopId())
            .itemId(click.getItemId())
            .productName(click.getProductName())
            .trackingCode(click.getTrackingCode())
            .trackingUrl(click.getTrackingUrl())
            .originalUrl(click.getOriginalUrl())
            .createdAt(click.getCreatedAt())
            .clickedAt(click.getClickedAt())
            .orderMatched(click.getOrderMatched())
            .matchedOrderId(click.getMatchedOrderId())
            .status(click.getStatus() != null ? click.getStatus().name() : null)
            .build();
    }
}
