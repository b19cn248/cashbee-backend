package com.cashbee.infrastructure.external.peeback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client for Peeback (PB) product commission API.
 *
 * API: {@code POST /api/shopee/product-info}
 * Body: {@code { "url": "https://s.shopee.vn/..." }}
 *
 * No authentication required. Peeback aggregates STK (and cache).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PeebackService {

    private static final String PRODUCT_INFO_PATH = "/api/shopee/product-info";

    private final RestTemplate restTemplate;
    private final PeebackProperties properties;

    /**
     * Fetch product commission info from Peeback.
     *
     * @param shopeeProductUrl Shopee product URL (short or full)
     * @return product info if successful
     */
    public Optional<PeebackProductInfo> getProductInfo(String shopeeProductUrl) {
        if (!properties.isEnabled()) {
            log.debug("PeebackService: disabled by config");
            return Optional.empty();
        }

        log.info("PeebackService: Fetching product info for URL: {}", shopeeProductUrl);

        try {
            String apiUrl = properties.getBaseUrl() + PRODUCT_INFO_PATH;

            Map<String, String> body = new HashMap<>();
            body.put("url", shopeeProductUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.ACCEPT, "application/json");

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<PeebackProductInfo> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                PeebackProductInfo.class
            );

            PeebackProductInfo productInfo = response.getBody();
            if (response.getStatusCode().is2xxSuccessful()
                && productInfo != null
                && productInfo.getProductName() != null
                && !productInfo.getProductName().isBlank()) {

                log.info("PeebackService: OK - product={}, price={}, sellerRate={}, shopeeRate={}, commission={}",
                    productInfo.getProductName(),
                    productInfo.getPriceMin(),
                    productInfo.getSellerCommissionRate(),
                    productInfo.getShopeeCommissionRate(),
                    productInfo.getCommission());
                return Optional.of(productInfo);
            }

            log.warn("PeebackService: Empty/invalid product-info response, status={}",
                response.getStatusCode());
            return Optional.empty();

        } catch (HttpStatusCodeException e) {
            log.error("PeebackService: HTTP {} for URL {}: {}",
                e.getStatusCode().value(), shopeeProductUrl, e.getResponseBodyAsString());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("PeebackService: Failed to call product-info for URL: {}",
                shopeeProductUrl, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("PeebackService: Unexpected error for URL: {}", shopeeProductUrl, e);
            return Optional.empty();
        }
    }
}
