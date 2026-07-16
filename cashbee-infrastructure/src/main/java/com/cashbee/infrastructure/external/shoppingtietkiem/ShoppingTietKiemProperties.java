package com.cashbee.infrastructure.external.shoppingtietkiem;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for ShoppingTietKiem (STK) external product-info API.
 *
 * Used as the commission data source for estimate-cashback.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cashbee.external.shopping-tiet-kiem")
public class ShoppingTietKiemProperties {

    /**
     * STK API base URL.
     */
    private String baseUrl = "https://api.shoppingtietkiem.com";

    /**
     * Required API key header (x-api-key).
     */
    private String apiKey;

    /**
     * App platform header (x-app-platform).
     */
    private String appPlatform = "android";

    /**
     * App version header (x-app-version).
     */
    private String appVersion = "2.0.12";

    /**
     * Device id header (x-device-id).
     */
    private String deviceId = "cashbee-backend-01";

    /**
     * Seconds before JWT expiry when token is considered stale and refreshed.
     */
    private long tokenRefreshSkewSeconds = 60;

    /**
     * STK accounts used for login. Tried in order with fallback.
     */
    private List<Account> accounts = new ArrayList<>();

    @Data
    public static class Account {
        private String email;
        private String password;
    }
}
