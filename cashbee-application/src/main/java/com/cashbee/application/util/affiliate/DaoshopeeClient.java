package com.cashbee.application.util.affiliate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Temporary client to call daoshopee.com API for generating Shopee affiliate short links.
 *
 * daoshopee generates native s.shopee.vn short links via Shopee's Affiliate Platform API,
 * which preserves Facebook traffic signals (referer + fbclid) for voucher eligibility.
 *
 * TODO: Replace with direct Shopee Affiliate Platform API integration.
 */
@Component
@Slf4j
public class DaoshopeeClient {

    private static final String API_URL = "https://daoshopee.com/api/sub";
    private static final String CODE = "888";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DaoshopeeClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a Shopee affiliate short link via daoshopee.com.
     *
     * @param shopeeUrl Original Shopee URL (short link or full URL)
     * @param sub1      Tracking code to put in sub1 (maps to utm_content)
     * @return Native s.shopee.vn short link with affiliate attribution
     * @throws RuntimeException if API call fails
     */
    public String generateAffiliateLink(String shopeeUrl, String sub1) {
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("content", shopeeUrl);
            body.put("code", CODE);
            body.put("sub1", sub1 != null ? sub1 : "");
            body.put("sub2", "");
            body.put("sub3", "");
            body.put("sub4", "");
            body.put("sub5", "");

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Calling daoshopee API - url: {}, sub1: {}", shopeeUrl, sub1);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("daoshopee API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("daoshopee API returned status " + response.statusCode());
            }

            String affiliateLink = response.body().trim();
            log.info("daoshopee API returned affiliate link: {}", affiliateLink);

            if (affiliateLink.isEmpty() || !affiliateLink.startsWith("https://")) {
                log.error("daoshopee API returned invalid response: {}", affiliateLink);
                throw new RuntimeException("daoshopee API returned invalid link: " + affiliateLink);
            }

            return affiliateLink;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call daoshopee API for URL: {}", shopeeUrl, e);
            throw new RuntimeException("Failed to generate affiliate link via daoshopee: " + e.getMessage(), e);
        }
    }
}
