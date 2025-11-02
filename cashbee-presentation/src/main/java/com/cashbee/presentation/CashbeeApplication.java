package com.cashbee.presentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for CashBee Backend.
 *
 * This is the entry point for the application.
 *
 * Configuration:
 * - Base package scanning: com.cashbee
 * - JPA entities: com.cashbee.infrastructure.entity (new) + com.cashbee.infrastructure.persistence.entity (legacy)
 * - JPA repositories: com.cashbee.infrastructure.repository (new) + com.cashbee.infrastructure.persistence.repository (legacy)
 *
 * @author CashBee Team
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.cashbee")
@EntityScan(basePackages = {
    "com.cashbee.infrastructure.entity",
    "com.cashbee.infrastructure.persistence.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.cashbee.infrastructure.repository",
    "com.cashbee.infrastructure.persistence.repository"
})
@EnableTransactionManagement
public class CashbeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbeeApplication.class, args);
    }
}
