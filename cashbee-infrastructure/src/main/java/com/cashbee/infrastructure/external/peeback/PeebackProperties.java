package com.cashbee.infrastructure.external.peeback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for Peeback (PB) product-info API.
 *
 * Public API (no auth): {@code POST/GET https://peeback.vn/api/shopee/product-info}
 */
@Data
@Component
@ConfigurationProperties(prefix = "cashbee.external.peeback")
public class PeebackProperties {

    /**
     * Peeback API base URL.
     */
    private String baseUrl = "https://peeback.vn";

    /**
     * Enable Peeback as a commission source.
     */
    private boolean enabled = true;
}
