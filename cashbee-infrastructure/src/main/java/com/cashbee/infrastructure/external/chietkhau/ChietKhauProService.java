package com.cashbee.infrastructure.external.chietkhau;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service to call ChietKhau.Pro external API.
 *
 * ChietKhau.Pro is a cashback platform that provides commission data for Shopee products.
 * We use their API to get commission information, then calculate our own cashback rate.
 *
 * API Endpoint: POST https://api.chietkhau.pro/api/v1/shopee/product-commission
 * Request: { "link": "https://shopee.vn/product/shopId/itemId" }
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChietKhauProService {

    private final RestTemplate restTemplate;

    /**
     * ChietKhau.Pro API base URL.
     * Default: https://api.chietkhau.pro
     */
    @Value("${cashbee.external.chietkhau.base-url:https://api.chietkhau.pro}")
    private String baseUrl;

    /**
     * API endpoint path for product commission.
     */
    private static final String PRODUCT_COMMISSION_PATH = "/api/v1/shopee/product-commission";

    /**
     * Get product commission information from ChietKhau.Pro API.
     *
     * @param shopeeProductUrl Shopee product URL (e.g., https://shopee.vn/product/123/456)
     * @return Optional containing product info with commission, or empty if failed
     */
    public Optional<ChietKhauProductInfo> getProductCommission(String shopeeProductUrl) {
        log.info("ChietKhauProService: Fetching commission for URL: {}", shopeeProductUrl);

        try {
            // Build request URL
            String apiUrl = baseUrl + PRODUCT_COMMISSION_PATH;

            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("link", shopeeProductUrl);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Call API
            log.debug("ChietKhauProService: Calling API: {}", apiUrl);
            ResponseEntity<ChietKhauApiResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                ChietKhauApiResponse.class
            );

            // Check response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ChietKhauApiResponse apiResponse = response.getBody();

                if (apiResponse.isSuccess()) {
                    ChietKhauProductInfo productInfo = apiResponse.getProductInfo();
                    log.info("ChietKhauProService: Successfully fetched commission - Product: {}, Price: {}, Commission: {}",
                        productInfo.getProductName(),
                        productInfo.getPrice(),
                        productInfo.getCommission());
                    return Optional.of(productInfo);
                } else {
                    log.warn("ChietKhauProService: API returned non-success status: {}", apiResponse.getStatus());
                    return Optional.empty();
                }
            } else {
                log.warn("ChietKhauProService: API returned non-OK status: {}", response.getStatusCode());
                return Optional.empty();
            }

        } catch (RestClientException e) {
            log.error("ChietKhauProService: Failed to call ChietKhau.Pro API for URL: {}", shopeeProductUrl, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("ChietKhauProService: Unexpected error while fetching commission", e);
            return Optional.empty();
        }
    }
}
