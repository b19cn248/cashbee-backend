package com.cashbee.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak configuration properties.
 *
 * Maps configuration from application.yml under 'keycloak.admin' prefix.
 * Used to configure Keycloak Admin Client for user/role management operations.
 *
 * Example configuration in application.yml:
 * <pre>
 * keycloak:
 *   admin:
 *     server-url: http://localhost:8080
 *     realm: cashbee
 *     client-id: cashbee-backend
 *     client-secret: your-client-secret
 *     username: admin
 *     password: admin
 * </pre>
 *
 * @author CashBee Team
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
@Getter
@Setter
public class KeycloakProperties {

    /**
     * Keycloak server URL.
     * Example: http://localhost:8080 or https://keycloak.yourdomain.com
     */
    private String serverUrl;

    /**
     * Keycloak realm name.
     * Example: cashbee, master, etc.
     */
    private String realm;

    /**
     * Client ID for backend service.
     * This client must have 'realm-management' roles assigned.
     * Example: cashbee-backend
     */
    private String clientId;

    /**
     * Client secret for confidential client.
     * Used for service-to-service authentication.
     */
    private String clientSecret;

    /**
     * Admin username for Keycloak operations.
     * Alternative to client credentials.
     * Example: admin
     */
    private String username;

    /**
     * Admin password.
     * Alternative to client credentials.
     */
    private String password;
}
