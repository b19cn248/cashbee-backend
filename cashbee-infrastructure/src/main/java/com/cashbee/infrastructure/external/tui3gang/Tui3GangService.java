package com.cashbee.infrastructure.external.tui3gang;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to call Tui3Gang external API for product commission.
 *
 * Tui3Gang is a cashback platform that provides commission data for Shopee products.
 * Their API returns 60% of full commission to users.
 * CashBee will recalculate to give 80% instead.
 *
 * API Endpoint: POST https://api.tui3gang.com/api/v1/shopee/get-info-product
 * Request: { "url": "https://shopee.vn/product/shopId/itemId", "idUser": "notlogin" }
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Tui3GangService {

    private final RestTemplate restTemplate;

    /**
     * Tui3Gang API base URL.
     * Default: https://api.tui3gang.com
     */
    @Value("${cashbee.external.tui3gang.base-url:https://api.tui3gang.com}")
    private String baseUrl;

    /**
     * API endpoint path for getting product info.
     */
    private static final String GET_INFO_PRODUCT_PATH = "/api/v1/shopee/get-info-product";

    /**
     * Default user ID when not logged in.
     */
    private static final String DEFAULT_USER_ID = "notlogin";

    /**
     * Get product commission information from Tui3Gang API.
     *
     * @param shopeeProductUrl Shopee product URL (e.g., https://shopee.vn/product/123/456)
     * @return Optional containing product info with commission, or empty if failed
     */
    public Optional<Tui3GangProductInfo> getProductInfo(String shopeeProductUrl) {
        log.info("Tui3GangService: Fetching product info for URL: {}", shopeeProductUrl);

        try {
            // Build request URL
            String apiUrl = baseUrl + GET_INFO_PRODUCT_PATH;

            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", shopeeProductUrl);
            requestBody.put("idUser", DEFAULT_USER_ID);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Origin", "https://tui3gang.com");
            headers.set("Referer", "https://tui3gang.com/");

            // Create HTTP entity
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Call API - Response is an array
            log.debug("Tui3GangService: Calling API: {}", apiUrl);
            ResponseEntity<List<Tui3GangProductInfo>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<Tui3GangProductInfo>>() {}
            );

            // Check response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty()) {
                Tui3GangProductInfo productInfo = response.getBody().get(0);
                log.info("Tui3GangService: Successfully fetched product info - Product: {}, Price: {}, Commission: {}",
                    productInfo.getProductName(),
                    productInfo.getPriceMin(),
                    productInfo.getCommission());
                return Optional.of(productInfo);
            } else {
                log.warn("Tui3GangService: API returned empty or non-OK status: {}", response.getStatusCode());
                return Optional.empty();
            }

        } catch (RestClientException e) {
            log.error("Tui3GangService: Failed to call Tui3Gang API for URL: {}", shopeeProductUrl, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Tui3GangService: Unexpected error while fetching product info", e);
            return Optional.empty();
        }
    }
}
