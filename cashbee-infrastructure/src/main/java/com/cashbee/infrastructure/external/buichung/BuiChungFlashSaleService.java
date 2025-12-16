package com.cashbee.infrastructure.external.buichung;

import com.cashbee.infrastructure.external.buichung.dto.BuiChungProduct;
import com.cashbee.infrastructure.external.buichung.dto.BuiChungSystemStatusResponse;
import com.cashbee.infrastructure.external.buichung.dto.BuiChungTimeSlotData;
import com.cashbee.infrastructure.external.buichung.dto.BuiChungTimeSlotsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service to fetch Flash Sale data from buichung.vn API.
 *
 * buichung.vn is a third-party service that aggregates Shopee Flash Sale products.
 * We use their public API to get flash sale data and display to our users.
 *
 * API Endpoints:
 * - GET /api/data?t={timestamp} - Get all flash sale products
 * - GET /api/time-slots?t={timestamp} - Get available time slots
 * - GET /api/system-status?t={timestamp} - Check if system is active
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuiChungFlashSaleService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * buichung.vn API base URL.
     */
    @Value("${cashbee.external.buichung.base-url:https://buichung.vn}")
    private String baseUrl;

    /**
     * Whether buichung.vn integration is enabled.
     */
    @Value("${cashbee.external.buichung.enabled:true}")
    private boolean enabled;

    /**
     * User-Agent header for API requests.
     */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    /**
     * Get all flash sale data from buichung.vn.
     *
     * API: GET /api/data?t={timestamp}
     *
     * Response structure:
     * {
     *   "16-12 Khung: 00:00": {
     *     "linkMapping": {...},
     *     "productCache": {...}
     *   }
     * }
     *
     * @return Map of time slot name to time slot data
     */
    public Map<String, BuiChungTimeSlotData> getFlashSaleData() {
        if (!enabled) {
            log.warn("BuiChung integration is disabled");
            return Collections.emptyMap();
        }

        String url = buildUrl("/api/data");
        log.info("BuiChungService: Fetching flash sale data from: {}", url);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse JSON response to Map<String, BuiChungTimeSlotData>
                Map<String, BuiChungTimeSlotData> result = parseFlashSaleData(response.getBody());

                int totalProducts = result.values().stream()
                    .mapToInt(BuiChungTimeSlotData::getProductCount)
                    .sum();

                log.info("BuiChungService: Successfully fetched {} time slots with {} total products",
                    result.size(), totalProducts);

                return result;
            } else {
                log.warn("BuiChungService: API returned non-OK status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }

        } catch (RestClientException e) {
            log.error("BuiChungService: Failed to fetch flash sale data", e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("BuiChungService: Unexpected error while fetching flash sale data", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get list of available time slots.
     *
     * API: GET /api/time-slots?t={timestamp}
     *
     * @return List of time slot names
     */
    public List<String> getTimeSlots() {
        if (!enabled) {
            log.warn("BuiChung integration is disabled");
            return Collections.emptyList();
        }

        String url = buildUrl("/api/time-slots");
        log.info("BuiChungService: Fetching time slots from: {}", url);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BuiChungTimeSlotsResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                BuiChungTimeSlotsResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                BuiChungTimeSlotsResponse body = response.getBody();
                if (body.isSuccess() && body.getData() != null) {
                    log.info("BuiChungService: Successfully fetched {} time slots", body.getData().size());
                    return body.getData();
                }
            }

            log.warn("BuiChungService: Failed to fetch time slots");
            return Collections.emptyList();

        } catch (RestClientException e) {
            log.error("BuiChungService: Failed to fetch time slots", e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if buichung.vn system is active.
     *
     * API: GET /api/system-status?t={timestamp}
     *
     * @return true if system is active
     */
    public boolean isSystemActive() {
        if (!enabled) {
            return false;
        }

        String url = buildUrl("/api/system-status");
        log.debug("BuiChungService: Checking system status from: {}", url);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BuiChungSystemStatusResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                BuiChungSystemStatusResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                BuiChungSystemStatusResponse body = response.getBody();
                boolean isActive = body.isSuccess() && body.isActive();
                log.debug("BuiChungService: System status - active: {}", isActive);
                return isActive;
            }

            return false;

        } catch (Exception e) {
            log.error("BuiChungService: Failed to check system status", e);
            return false;
        }
    }

    /**
     * Build URL with timestamp parameter for cache-busting.
     *
     * @param path API path
     * @return Full URL with timestamp
     */
    private String buildUrl(String path) {
        long timestamp = System.currentTimeMillis();
        return baseUrl + path + "?t=" + timestamp;
    }

    /**
     * Create HTTP headers for API requests.
     *
     * @return HttpHeaders with required headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", "*/*");
        headers.set("Accept-Language", "en,vi;q=0.9");
        headers.set("Referer", baseUrl + "/");
        return headers;
    }

    /**
     * Parse flash sale data JSON response.
     *
     * The response is a Map where:
     * - Key: Time slot name (e.g., "16-12 Khung: 00:00")
     * - Value: Time slot data containing linkMapping and productCache
     *
     * @param json JSON response string
     * @return Parsed map of time slot data
     */
    private Map<String, BuiChungTimeSlotData> parseFlashSaleData(String json) {
        try {
            // First, parse as generic Map to handle the nested structure
            Map<String, Object> rawData = objectMapper.readValue(json, new TypeReference<>() {});

            Map<String, BuiChungTimeSlotData> result = new HashMap<>();

            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                String timeSlot = entry.getKey();

                // Convert the value to BuiChungTimeSlotData
                String slotJson = objectMapper.writeValueAsString(entry.getValue());
                BuiChungTimeSlotData slotData = objectMapper.readValue(slotJson, BuiChungTimeSlotData.class);

                result.put(timeSlot, slotData);
            }

            return result;

        } catch (Exception e) {
            log.error("BuiChungService: Failed to parse flash sale data JSON", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get all products from all time slots as a flat list.
     *
     * @return List of all products
     */
    public List<BuiChungProduct> getAllProducts() {
        Map<String, BuiChungTimeSlotData> data = getFlashSaleData();

        List<BuiChungProduct> allProducts = new ArrayList<>();

        data.forEach((timeSlot, slotData) -> {
            if (slotData.getProductCache() != null) {
                allProducts.addAll(slotData.getProductCache().values());
            }
        });

        log.info("BuiChungService: Retrieved {} total products from all time slots", allProducts.size());
        return allProducts;
    }
}
