package com.cashbee.application.util.affiliate;

import com.cashbee.domain.model.AffiliatePlatform;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to build affiliate tracking URLs from templates.
 *
 * Takes a template with placeholders and replaces them with actual values.
 *
 * Template format example:
 * "https://shopee.vn/universal-link/{product_id}?af_siteid=0&pid={affiliate_id}&af_sub1={tracking_code}"
 *
 * Supported placeholders:
 * - {product_id} or {item_id} - Product/Item ID
 * - {shop_id} - Shop ID
 * - {affiliate_id} - Platform's affiliate ID
 * - {tracking_code} - Generated tracking code for user
 *
 * @author CashBee Team
 */
@Component
public class AffiliateLinkBuilder {

    /**
     * Build affiliate tracking URL from template.
     *
     * @param platform AffiliatePlatform with linkTemplate and affiliateId
     * @param itemId Product/Item ID
     * @param shopId Shop ID (may be null for some URL formats)
     * @param trackingCode Unique tracking code
     * @return Built affiliate tracking URL
     * @throws IllegalArgumentException if template is invalid or required placeholders missing
     */
    public String build(AffiliatePlatform platform, String itemId, String shopId, String trackingCode) {
        if (platform == null) {
            throw new IllegalArgumentException("Platform cannot be null");
        }

        if (platform.getLinkTemplate() == null || platform.getLinkTemplate().isBlank()) {
            throw new IllegalArgumentException("Platform link template is not configured");
        }

        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID is required");
        }

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code is required");
        }

        // Prepare replacement values
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{product_id}", itemId);
        replacements.put("{item_id}", itemId);
        replacements.put("{tracking_code}", urlEncode(trackingCode));
        replacements.put("{affiliate_id}", platform.getAffiliateId() != null ? platform.getAffiliateId() : "");

        // Shop ID is optional
        if (shopId != null && !shopId.isBlank()) {
            replacements.put("{shop_id}", shopId);
        }

        // Replace all placeholders
        String url = platform.getLinkTemplate();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            url = url.replace(entry.getKey(), entry.getValue());
        }

        // Validate that no placeholders remain
        if (url.contains("{") && url.contains("}")) {
            throw new IllegalArgumentException(
                "Template contains unfilled placeholders. Template: " + platform.getLinkTemplate()
            );
        }

        return url;
    }

    /**
     * Build affiliate tracking URL with custom parameters.
     *
     * @param platform AffiliatePlatform with linkTemplate
     * @param itemId Product/Item ID
     * @param shopId Shop ID (may be null)
     * @param trackingCode Tracking code
     * @param customParams Additional custom parameters to replace
     * @return Built affiliate tracking URL
     */
    public String build(AffiliatePlatform platform, String itemId, String shopId,
                       String trackingCode, Map<String, String> customParams) {
        if (platform == null) {
            throw new IllegalArgumentException("Platform cannot be null");
        }

        if (platform.getLinkTemplate() == null || platform.getLinkTemplate().isBlank()) {
            throw new IllegalArgumentException("Platform link template is not configured");
        }

        // Start with base replacements
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{product_id}", itemId != null ? itemId : "");
        replacements.put("{item_id}", itemId != null ? itemId : "");
        replacements.put("{tracking_code}", trackingCode != null ? urlEncode(trackingCode) : "");
        replacements.put("{affiliate_id}", platform.getAffiliateId() != null ? platform.getAffiliateId() : "");

        if (shopId != null && !shopId.isBlank()) {
            replacements.put("{shop_id}", shopId);
        }

        // Add custom parameters
        if (customParams != null) {
            for (Map.Entry<String, String> entry : customParams.entrySet()) {
                String key = entry.getKey().startsWith("{") ? entry.getKey() : "{" + entry.getKey() + "}";
                replacements.put(key, urlEncode(entry.getValue()));
            }
        }

        // Replace all placeholders
        String url = platform.getLinkTemplate();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            url = url.replace(entry.getKey(), entry.getValue());
        }

        return url;
    }

    /**
     * Validate that a template can be built with given parameters.
     *
     * @param template Link template
     * @param itemId Item ID
     * @param shopId Shop ID (may be null)
     * @param affiliateId Affiliate ID
     * @param trackingCode Tracking code
     * @return true if template is valid and can be built
     */
    public boolean canBuild(String template, String itemId, String shopId,
                           String affiliateId, String trackingCode) {
        try {
            AffiliatePlatform tempPlatform = AffiliatePlatform.builder()
                .linkTemplate(template)
                .affiliateId(affiliateId)
                .build();

            build(tempPlatform, itemId, shopId, trackingCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract placeholders from template.
     *
     * @param template Link template
     * @return List of placeholder names (without braces)
     */
    public java.util.List<String> extractPlaceholders(String template) {
        if (template == null || template.isBlank()) {
            return java.util.List.of();
        }

        java.util.List<String> placeholders = new java.util.ArrayList<>();
        int start = 0;

        while (true) {
            start = template.indexOf("{", start);
            if (start == -1) break;

            int end = template.indexOf("}", start);
            if (end == -1) break;

            String placeholder = template.substring(start, end + 1);
            if (!placeholders.contains(placeholder)) {
                placeholders.add(placeholder);
            }

            start = end + 1;
        }

        return placeholders;
    }

    /**
     * URL encode a value for safe inclusion in URL.
     *
     * @param value Value to encode
     * @return URL encoded value
     */
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }

        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported, this should never happen
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }
}
