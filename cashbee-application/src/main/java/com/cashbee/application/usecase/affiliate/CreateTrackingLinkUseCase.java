package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.CreateTrackingLinkRequest;
import com.cashbee.application.dto.affiliate.TrackingLinkResponse;
import com.cashbee.application.util.affiliate.AffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeAffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeFoodAffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeUrlParser;
import com.cashbee.application.util.affiliate.TrackingCodeGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.repository.AffiliateClickRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for creating an affiliate tracking link.
 *
 * Supports both Shopee Mall and ShopeeFood URLs.
 *
 * Flow:
 * 1. Parse URL to extract product/restaurant info and detect platform
 * 2. Get platform configuration (shopee or shopeefood)
 * 3. Create AffiliateClick record (save to get ID)
 * 4. Generate unique tracking code
 * 5. Build affiliate tracking URL using appropriate builder
 * 6. Update AffiliateClick with tracking info
 * 7. Return tracking link to user
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTrackingLinkUseCase {

    private final AffiliateClickRepository clickRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final ShopeeUrlParser urlParser;
    private final TrackingCodeGenerator trackingCodeGenerator;
    private final AffiliateLinkBuilder linkBuilder;
    private final ShopeeAffiliateLinkBuilder shopeeAffiliateLinkBuilder;
    private final ShopeeFoodAffiliateLinkBuilder shopeeFoodAffiliateLinkBuilder;

    /**
     * Execute use case to create tracking link.
     *
     * SECURITY UPDATE:
     * - userId is now passed as a separate parameter (extracted from JWT by controller)
     * - This ensures userId comes from authenticated source, not client request
     * - Prevents users from creating tracking links for other users
     *
     * @param request Request containing Shopee URL (NO userId)
     * @param userId User ID extracted from JWT token by controller
     * @return Response with generated tracking link
     * @throws NotFoundException if platform not found
     * @throws BusinessException if URL parsing fails or platform not configured
     */
    @Transactional
    public TrackingLinkResponse execute(CreateTrackingLinkRequest request, Long userId) {
        log.info("UseCase: Creating tracking link for authenticated user {} with URL: {}",
            userId, request.getShopeeUrl());

        // Step 1: Parse URL (supports both Shopee Mall and ShopeeFood)
        ShopeeUrlParser.ParsedShopeeUrl parsedUrl;
        try {
            parsedUrl = urlParser.parse(request.getShopeeUrl());
            log.info("UseCase: Parsed URL - Platform: {}, Item ID: {}, Shop ID: {}, Restaurant ID: {}",
                parsedUrl.getPlatform(), parsedUrl.getItemId(),
                parsedUrl.getShopId(), parsedUrl.getRestaurantId());
        } catch (IllegalArgumentException e) {
            log.error("UseCase: Invalid URL: {}", request.getShopeeUrl(), e);
            throw new BusinessException("Invalid URL format: " + e.getMessage());
        }

        // Step 2: Determine platform code from parsed URL or request
        // Priority: 1) Parsed URL platform, 2) Request platformCode, 3) Default "shopee"
        String platformCode;
        if (parsedUrl.getPlatform() != null) {
            platformCode = parsedUrl.getPlatform();  // Auto-detected from URL
        } else if (request.getPlatformCode() != null) {
            platformCode = request.getPlatformCode();
        } else {
            platformCode = "shopee";
        }
        log.info("UseCase: Using platform: {}", platformCode);

        AffiliatePlatform platform = platformRepository.findByCode(platformCode)
            .orElseThrow(() -> {
                log.error("UseCase: Platform not found: {}", platformCode);
                return new NotFoundException("Affiliate platform not found: " + platformCode);
            });

        // Validate platform is configured for tracking
        if (!platform.isActive()) {
            log.error("UseCase: Platform is not active: {}", platformCode);
            throw new BusinessException("Platform is not active: " + platform.getName());
        }

        if (platform.getTrackingEnabled() == null || !platform.getTrackingEnabled()) {
            log.error("UseCase: Tracking is not enabled for platform: {}", platformCode);
            throw new BusinessException("Tracking is not enabled for platform: " + platform.getName());
        }

        if (platform.getAffiliateId() == null || platform.getAffiliateId().isBlank()) {
            log.error("UseCase: Platform affiliate ID is not configured: {}", platformCode);
            throw new BusinessException("Platform affiliate ID is not configured. Please contact admin.");
        }

        // Note: Shopee and ShopeeFood use redirect service, no link_template needed
        // For other platforms, link_template is still required
        if (!"shopee".equalsIgnoreCase(platformCode) && !"shopeefood".equalsIgnoreCase(platformCode)) {
            if (platform.getLinkTemplate() == null || platform.getLinkTemplate().isBlank()) {
                log.error("UseCase: Platform link template is not configured: {}", platformCode);
                throw new BusinessException("Platform link template is not configured. Please contact admin.");
            }
        }

        // Step 3: Generate temporary tracking code (before save to avoid NULL constraint)
        String tempTrackingCode = trackingCodeGenerator.generateTemporary(userId);
        log.info("UseCase: Generated temporary tracking code: {}", tempTrackingCode);

        // Step 4: Build temporary tracking URL
        String tempTrackingUrl;
        try {
            tempTrackingUrl = buildTrackingUrl(parsedUrl, platform, platformCode, tempTrackingCode);
            log.info("UseCase: Built temporary tracking URL: {}", tempTrackingUrl);
        } catch (IllegalArgumentException e) {
            log.error("UseCase: Failed to build tracking URL", e);
            throw new BusinessException("Failed to build tracking URL: " + e.getMessage());
        }

        // Step 5: Create AffiliateClick with temporary tracking code
        LocalDateTime now = LocalDateTime.now();
        AffiliateClick click = AffiliateClick.builder()
            .userId(userId)
            .platformId(platform.getId())
            .shopId(parsedUrl.getShopId())
            .itemId(parsedUrl.getItemId())
            .productName(null)  // Can be fetched later via API if available
            .originalUrl(request.getShopeeUrl())
            .trackingCode(tempTrackingCode)  // ✅ Set temporary tracking code
            .trackingUrl(tempTrackingUrl)    // ✅ Set temporary tracking URL
            .status(ClickStatus.CREATED)
            .orderMatched(false)
            .createdAt(now)
            .build();

        // Step 6: Save to get auto-generated ID
        AffiliateClick savedClick = clickRepository.save(click);
        log.info("UseCase: Created AffiliateClick with ID: {}", savedClick.getId());

        // Step 7: Generate real tracking code using click ID
        String trackingCode = trackingCodeGenerator.generate(userId, savedClick.getId());
        log.info("UseCase: Generated real tracking code: {}", trackingCode);

        // Step 8: Build real affiliate tracking URL
        String trackingUrl;
        try {
            trackingUrl = buildTrackingUrl(parsedUrl, platform, platformCode, trackingCode);
            log.info("UseCase: Built real tracking URL: {}", trackingUrl);
        } catch (IllegalArgumentException e) {
            log.error("UseCase: Failed to build tracking URL", e);
            throw new BusinessException("Failed to build tracking URL: " + e.getMessage());
        }

        // Step 9: Update AffiliateClick with real tracking info
        savedClick.setTrackingCode(trackingCode);
        savedClick.setTrackingUrl(trackingUrl);
        savedClick = clickRepository.save(savedClick);

        log.info("UseCase: Successfully created tracking link for user {}, click ID {}",
            userId, savedClick.getId());

        // Step 10: Build and return response
        return TrackingLinkResponse.builder()
            .clickId(savedClick.getId())
            .trackingUrl(trackingUrl)
            .trackingCode(trackingCode)
            .originalUrl(request.getShopeeUrl())
            .productName(savedClick.getProductName())
            .shopId(parsedUrl.getShopId())
            .itemId(parsedUrl.getItemId())
            .platformName(platform.getName())
            .platformCode(platform.getCode())
            .estimatedCashbackRate(platform.getDefaultCommissionRate())
            .createdAt(savedClick.getCreatedAt())
            .message("Click this link to shop on " + platform.getName() + " and earn cashback!")
            .build();
    }

    /**
     * Build tracking URL using the appropriate builder based on platform.
     *
     * @param parsedUrl Parsed URL with product/restaurant info
     * @param platform Platform configuration
     * @param platformCode Platform code (shopee, shopeefood, etc.)
     * @param trackingCode Unique tracking code
     * @return Affiliate tracking URL
     */
    private String buildTrackingUrl(
        ShopeeUrlParser.ParsedShopeeUrl parsedUrl,
        AffiliatePlatform platform,
        String platformCode,
        String trackingCode
    ) {
        String urlForAffiliate = parsedUrl.getUrlForAffiliateLink();
        log.debug("UseCase: Building affiliate link with URL: {}", urlForAffiliate);

        if ("shopee".equalsIgnoreCase(platformCode)) {
            // Use Shopee Mall builder
            return shopeeAffiliateLinkBuilder.build(
                urlForAffiliate,
                platform.getAffiliateId(),
                trackingCode
            );
        } else if ("shopeefood".equalsIgnoreCase(platformCode)) {
            // Use ShopeeFood builder
            return shopeeFoodAffiliateLinkBuilder.build(
                urlForAffiliate,
                platform.getAffiliateId(),
                trackingCode
            );
        } else {
            // Use generic builder for other platforms
            return linkBuilder.build(
                platform,
                parsedUrl.getItemId(),
                parsedUrl.getShopId(),
                trackingCode
            );
        }
    }
}
