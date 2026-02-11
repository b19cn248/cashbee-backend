package com.cashbee.application.service;

import com.cashbee.application.util.affiliate.ShopeeAffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeUrlExpanderService;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to convert Shopee links to affiliate links for Flash Sale products.
 *
 * This service handles the conversion of Shopee product links to affiliate links
 * that include CashBee's affiliate ID. When users click these links and make
 * purchases, CashBee earns commission.
 *
 * Features:
 * - Converts shortened Shopee URLs (s.shopee.vn) to affiliate links
 * - Uses Shopee's redirect service format
 * - Batch conversion for performance
 * - Graceful fallback when conversion fails
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleAffiliateLinkService {

    private final AffiliatePlatformRepository platformRepository;
    private final ShopeeUrlExpanderService urlExpanderService;
    private final ShopeeAffiliateLinkBuilder affiliateLinkBuilder;

    /**
     * Sub ID for Flash Sale tracking.
     * Used to identify traffic source as flash sale in Shopee reports.
     */
    private static final String FLASH_SALE_SUB_ID = "flashsale";

    /**
     * Shopee redirect service base URL.
     */
    private static final String SHOPEE_REDIRECT_BASE = "https://s.shopee.vn/an_redir";

    /**
     * Cache for expanded URLs to avoid repeated HTTP calls.
     * Key: shortened URL, Value: expanded URL
     */
    private final Map<String, String> expandedUrlCache = new ConcurrentHashMap<>();

    /**
     * Convert a Shopee link to an affiliate link.
     *
     * Flow:
     * 1. Get affiliate_id from Shopee platform config
     * 2. If shortened URL, try to expand it first
     * 3. Build affiliate link using Shopee's redirect format
     *
     * @param shopeeUrl Shopee product link (can be shortened or full)
     * @return Affiliate link, or original link if conversion fails
     */
    public String convertToAffiliateLink(String shopeeUrl) {
        if (shopeeUrl == null || shopeeUrl.isBlank()) {
            return shopeeUrl;
        }

        log.debug("Converting to affiliate link: {}", shopeeUrl);

        // Step 1: Get Shopee platform config
        Optional<AffiliatePlatform> platformOpt = platformRepository.findByCode("shopee");
        if (platformOpt.isEmpty()) {
            log.warn("Shopee platform not found, returning original link");
            return shopeeUrl;
        }

        AffiliatePlatform platform = platformOpt.get();
        String affiliateId = platform.getAffiliateId();

        if (affiliateId == null || affiliateId.isBlank()) {
            log.warn("Shopee affiliate_id not configured, returning original link");
            return shopeeUrl;
        }

        // Step 2: Handle shortened URLs
        if (urlExpanderService.isShortenedUrl(shopeeUrl)) {
            return convertShortenedUrl(shopeeUrl, affiliateId);
        }

        // Step 3: Handle full Shopee URLs
        return buildAffiliateLink(shopeeUrl, affiliateId);
    }

    /**
     * Convert a shortened URL to affiliate link.
     *
     * First tries to expand the URL, then builds affiliate link.
     * If expansion fails, builds affiliate link directly with shortened URL.
     *
     * @param shortenedUrl Shortened Shopee URL (e.g., https://s.shopee.vn/xxx)
     * @param affiliateId Affiliate ID
     * @return Affiliate link
     */
    private String convertShortenedUrl(String shortenedUrl, String affiliateId) {
        // Check cache first
        String cachedExpanded = expandedUrlCache.get(shortenedUrl);
        if (cachedExpanded != null) {
            log.debug("Using cached expanded URL for: {}", shortenedUrl);
            return buildAffiliateLink(cachedExpanded, affiliateId);
        }

        // Try to expand URL
        try {
            String expandedUrl = urlExpanderService.expandUrl(shortenedUrl);
            expandedUrlCache.put(shortenedUrl, expandedUrl);
            log.debug("Expanded URL: {} -> {}", shortenedUrl, expandedUrl);
            return buildAffiliateLink(expandedUrl, affiliateId);

        } catch (Exception e) {
            log.warn("Failed to expand URL: {}, using direct method", shortenedUrl);
            // Fallback: build affiliate link directly with shortened URL
            return buildAffiliateLinkDirect(shortenedUrl, affiliateId);
        }
    }

    /**
     * Build affiliate link using ShopeeAffiliateLinkBuilder.
     *
     * @param fullUrl Full Shopee URL (must start with https://shopee.vn/)
     * @param affiliateId Affiliate ID
     * @return Affiliate link
     */
    private String buildAffiliateLink(String fullUrl, String affiliateId) {
        try {
            return affiliateLinkBuilder.build(fullUrl, affiliateId, FLASH_SALE_SUB_ID);
        } catch (Exception e) {
            log.warn("Failed to build affiliate link for: {}, using direct method", fullUrl);
            return buildAffiliateLinkDirect(fullUrl, affiliateId);
        }
    }

    /**
     * Build affiliate link directly without validation.
     *
     * Format: https://s.shopee.vn/an_redir?origin_link={ENCODED_URL}&affiliate_id={ID}&sub_id={SUB}
     *
     * This method is used as a fallback when normal methods fail,
     * or when the URL format is non-standard.
     *
     * @param url Shopee URL (any format)
     * @param affiliateId Affiliate ID
     * @return Affiliate link
     */
    private String buildAffiliateLinkDirect(String url, String affiliateId) {
        try {
            String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
            return String.format(
                "%s?origin_link=%s&affiliate_id=%s&sub_id=%s",
                SHOPEE_REDIRECT_BASE,
                encodedUrl,
                affiliateId,
                FLASH_SALE_SUB_ID
            );
        } catch (Exception e) {
            log.error("Failed to build direct affiliate link for: {}", url, e);
            return url; // Return original as last resort
        }
    }

    /**
     * Convert multiple links to affiliate links in batch.
     *
     * Uses parallel stream for better performance with large lists.
     *
     * @param shopeeUrls List of Shopee URLs to convert
     * @return Map of original URL to affiliate URL
     */
    public Map<String, String> convertBatch(List<String> shopeeUrls) {
        if (shopeeUrls == null || shopeeUrls.isEmpty()) {
            return new HashMap<>();
        }

        log.info("Converting {} URLs to affiliate links", shopeeUrls.size());

        Map<String, String> result = shopeeUrls.parallelStream()
            .distinct()
            .collect(Collectors.toMap(
                url -> url,
                this::convertToAffiliateLink,
                (v1, v2) -> v1 // Keep first on duplicate
            ));

        log.info("Successfully converted {} URLs", result.size());
        return result;
    }

    /**
     * Clear the expanded URL cache.
     *
     * Useful for testing or when cache needs to be refreshed.
     */
    public void clearCache() {
        expandedUrlCache.clear();
        log.info("Cleared expanded URL cache");
    }

    /**
     * Get cache statistics.
     *
     * @return Number of cached URLs
     */
    public int getCacheSize() {
        return expandedUrlCache.size();
    }
}
