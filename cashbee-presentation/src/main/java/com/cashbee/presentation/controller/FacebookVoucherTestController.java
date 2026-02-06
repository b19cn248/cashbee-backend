package com.cashbee.presentation.controller;

import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test Controller for debugging Facebook Voucher link formats.
 *
 * This controller generates affiliate links in multiple formats for testing
 * which format works best with Shopee.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/facebook-voucher/test")
@RequiredArgsConstructor
@Tag(name = "Facebook Voucher Test", description = "Test different affiliate link formats")
public class FacebookVoucherTestController {

    private static final Logger log = LoggerFactory.getLogger(FacebookVoucherTestController.class);

    // Cashbee's affiliate ID from database
    private static final String AFFILIATE_ID = "17392100442";

    /**
     * Test multiple affiliate link formats.
     *
     * Generates the same link in different formats to test which one works.
     *
     * @param shopeeUrl Original Shopee URL to convert
     * @return Multiple link formats for testing
     */
    @GetMapping("/formats")
    @Operation(
        summary = "Test multiple link formats",
        description = "Generate affiliate links in different formats to find which works"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> testFormats(
        @RequestParam String shopeeUrl) {

        log.info("Testing link formats for URL: {}", shopeeUrl);

        String subId = generateSubId();
        Map<String, Object> result = new HashMap<>();

        // Store original URL
        result.put("originalUrl", shopeeUrl);
        result.put("subId", subId);
        result.put("affiliateId", AFFILIATE_ID);

        // Format 1: Direct URL with parameters (current implementation)
        String separator1 = shopeeUrl.contains("?") ? "&" : "?";
        String format1 = String.format("%s%saffiliate_id=%s&sub_id=%s",
            shopeeUrl, separator1, AFFILIATE_ID, subId);
        result.put("format1_direct", format1);
        result.put("format1_description", "Direct URL + affiliate_id + sub_id");

        // Format 2: an_redir format (old implementation)
        String encodedUrl = URLEncoder.encode(shopeeUrl, StandardCharsets.UTF_8);
        String format2 = String.format(
            "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s",
            encodedUrl, AFFILIATE_ID, subId);
        result.put("format2_an_redir", format2);
        result.put("format2_description", "an_redir redirect format");

        // Format 3: URL with utm parameters (alternative)
        String format3 = String.format("%s%sutm_source=an_%s&utm_medium=affiliates&utm_content=%s",
            shopeeUrl, separator1, AFFILIATE_ID, subId);
        result.put("format3_utm", format3);
        result.put("format3_description", "UTM parameters format");

        // Format 4: Combined an_redir + p=aff (like daoshopee)
        String format4 = String.format(
            "https://s.shopee.vn/an_redir?origin_link=%s&affiliate_id=%s&sub_id=%s&p=aff",
            encodedUrl, AFFILIATE_ID, subId);
        result.put("format4_an_redir_aff", format4);
        result.put("format4_description", "an_redir + p=aff parameter");

        // Instructions
        result.put("testInstructions",
            "Test each format:\n" +
            "1. Copy a format link\n" +
            "2. Paste it into a Facebook comment/post\n" +
            "3. Click the link FROM WITHIN Facebook\n" +
            "4. Check if Shopee shows the product AND Facebook vouchers\n" +
            "5. Report which format works!");

        return ResponseEntity.ok(ApiResponse.success(result, "Generated test links in multiple formats"));
    }

    /**
     * Quick test with a known working product URL.
     *
     * Uses a popular product that's unlikely to be removed.
     */
    @GetMapping("/quick")
    @Operation(
        summary = "Quick test with sample product",
        description = "Test with a known working Shopee product URL"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> quickTest() {
        // Use a popular, stable product URL for testing
        // This is Shopee's official shop - unlikely to be removed
        String sampleUrl = "https://shopee.vn/product/88201679/22159858498";

        return testFormats(sampleUrl);
    }

    private String generateSubId() {
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        long timestamp = Instant.now().getEpochSecond();
        return String.format("fb_test-%s-%d", shortUuid, timestamp);
    }
}
