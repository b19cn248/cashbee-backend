package com.cashbee.presentation.config;

import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.JsonSerialization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration with Keycloak Authorization Services.
 * <p>
 * This configuration uses Keycloak Policy Enforcer (PEP) for authorization:
 * - Resources, Scopes, Policies, Permissions are defined in Keycloak
 * - No @PreAuthorize annotations needed in controllers
 * - Authorization decisions are made by Keycloak Authorization Server
 * - Policy Enforcer intercepts requests and enforces permissions
 * <p>
 * Architecture:
 * 1. Client sends JWT token in Authorization header
 * 2. Spring Security validates JWT signature
 * 3. Policy Enforcer checks permissions with Keycloak Authorization Server
 * 4. Request is allowed or denied based on Keycloak policies
 * <p>
 * Configuration in Keycloak:
 * - Resources: /api/users, /api/wallets, /api/payouts, etc.
 * - Scopes: view, create, update, delete, approve, etc.
 * - Policies: Role-based, User-based, Time-based, etc.
 * - Permissions: Combine Resources + Scopes + Policies
 *
 * @author CashBee Team
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    String jwkSetUri;

    /**
     * Configure HTTP security filter chain with Policy Enforcer.
     * <p>
     * Flow:
     * 1. JWT validation (OAuth2 Resource Server)
     * 2. Extract roles from JWT
     * 3. Policy Enforcer checks permissions
     * 4. Allow/deny request
     *
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                "/api/v1/articles/rss-feed", "/api/v1/build/**",
                                "/api/v1/articles/check-article-exists",
                                "/api/v1/n8n/**", "/api/v1/notifications",
                                "/api/v1/gemini/generate",
                                "/api/payouts/**", "/api/users/**", "/api/admin/platforms/**",
                                "/api/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .addFilterAfter(createPolicyEnforcerFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }


    /**
     * Create Policy Enforcer Filter.
     * <p>
     * This filter intercepts requests and enforces authorization
     * based on Keycloak policies and permissions.
     *
     * @return ServletPolicyEnforcerFilter instance
     */
    private ServletPolicyEnforcerFilter createPolicyEnforcerFilter() {
        return new ServletPolicyEnforcerFilter(new ConfigurationResolver() {
            @Override
            public PolicyEnforcerConfig resolve(HttpRequest request) {
                try {
                    return JsonSerialization.readValue(
                            getClass().getResourceAsStream("/policy-enforcer-dev.json"),
                            PolicyEnforcerConfig.class
                    );
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load policy-enforcer.json", e);
                }
            }
        });
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3007",
                "https://cashbee.nguocchieuvangle.io.vn/", "https://video.management.v1.openlearnhub.io.vn/",
                "https://auth.nguocchieuvangle.io.vn/"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Cache-Control",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "db",                           // Custom header cho database selection
                "X-Forwarded-For",
                "X-Forwarded-Proto",
                "X-Forwarded-Host"
        ));
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Custom Bearer Token Resolver that skips JWT validation for public endpoints.
     * <p>
     * For public endpoints (permitAll), we don't require JWT token.
     * For protected endpoints, JWT token is required.
     *
     * @return BearerTokenResolver instance
     */
    private BearerTokenResolver customBearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

        return (HttpServletRequest request) -> {
            String requestUri = request.getRequestURI();

            // Skip JWT validation for public endpoints
            if (requestUri.startsWith("/actuator/") ||
                    requestUri.startsWith("/v3/api-docs") ||
                    requestUri.startsWith("/swagger-ui") ||
                    requestUri.startsWith("/api/payouts")) {
                return null;  // No token required
            }

            // For other endpoints, use default resolver (requires Bearer token)
            return defaultResolver.resolve(request);
        };
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
    }

}
