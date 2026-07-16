package com.cashbee.infrastructure.external.shoppingtietkiem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client for ShoppingTietKiem product commission API.
 *
 * API: {@code POST /api/affiliate/product-info}
 * Body: {@code { "shopeeUrl": "..." }}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingTietKiemService {

    private static final String PRODUCT_INFO_PATH = "/api/affiliate/product-info";
    private static final int MAX_AUTH_RETRIES = 2;

    private final RestTemplate restTemplate;
    private final ShoppingTietKiemAuthService authService;
    private final ShoppingTietKiemProperties properties;

    /**
     * Fetch product commission info from STK.
     *
     * On HTTP 401, invalidates token, rotates account, and retries a limited number of times.
     *
     * @param shopeeProductUrl Shopee product URL (short or full)
     * @return product info if successful
     */
    public Optional<ShoppingTietKiemProductInfo> getProductInfo(String shopeeProductUrl) {
        log.info("ShoppingTietKiemService: Fetching product info for URL: {}", shopeeProductUrl);

        for (int attempt = 1; attempt <= MAX_AUTH_RETRIES; attempt++) {
            Optional<String> tokenOpt = authService.getAccessToken();
            if (tokenOpt.isEmpty()) {
                log.error("ShoppingTietKiemService: Unable to obtain STK access token");
                return Optional.empty();
            }

            try {
                String apiUrl = properties.getBaseUrl() + PRODUCT_INFO_PATH;

                Map<String, String> body = new HashMap<>();
                body.put("shopeeUrl", shopeeProductUrl);

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                    body,
                    authService.buildHeaders(tokenOpt.get())
                );

                ResponseEntity<ShoppingTietKiemProductInfo> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    ShoppingTietKiemProductInfo.class
                );

                ShoppingTietKiemProductInfo productInfo = response.getBody();
                if (response.getStatusCode().is2xxSuccessful()
                    && productInfo != null
                    && productInfo.getProductName() != null) {

                    log.info("ShoppingTietKiemService: OK - product={}, price={}, commissionRate={}, cashback={}",
                        productInfo.getProductName(),
                        productInfo.getPriceMin(),
                        productInfo.getCommissionRate(),
                        productInfo.getEstimatedCashback());
                    return Optional.of(productInfo);
                }

                log.warn("ShoppingTietKiemService: Empty/invalid product-info response, status={}",
                    response.getStatusCode());
                return Optional.empty();

            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                    || e.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                    log.warn("ShoppingTietKiemService: Auth failed ({}), attempt {}/{} - rotating account",
                        e.getStatusCode().value(), attempt, MAX_AUTH_RETRIES);
                    authService.invalidateToken(true);
                    continue;
                }
                log.error("ShoppingTietKiemService: HTTP {} for URL {}: {}",
                    e.getStatusCode().value(), shopeeProductUrl, e.getResponseBodyAsString());
                return Optional.empty();
            } catch (RestClientException e) {
                log.error("ShoppingTietKiemService: Failed to call product-info for URL: {}",
                    shopeeProductUrl, e);
                return Optional.empty();
            } catch (Exception e) {
                log.error("ShoppingTietKiemService: Unexpected error for URL: {}", shopeeProductUrl, e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
