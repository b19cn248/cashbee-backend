package com.cashbee.presentation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration.
 *
 * Configures API documentation accessible at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - API Docs: http://localhost:8080/v3/api-docs
 *
 * @author CashBee Team
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:CashBee Backend}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CashBee Backend API")
                .version("1.0.0")
                .description("CashBee Cashback Platform - REST API Documentation\n\n" +
                    "This API provides endpoints for managing users, wallets, " +
                    "cashback transactions, and payout operations.\n\n" +
                    "**Architecture:** Multi-module Hexagonal Architecture\n" +
                    "**Authentication:** OAuth2/Keycloak\n" +
                    "**Database:** MySQL")
                .contact(new Contact()
                    .name("CashBee Development Team")
                    .email("dev@cashbee.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://cashbee.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.cashbee.com")
                    .description("Production Server")
            ));
    }
}
