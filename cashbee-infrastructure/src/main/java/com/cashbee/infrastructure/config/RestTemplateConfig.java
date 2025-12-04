package com.cashbee.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used to call external APIs.
 *
 * @author CashBee Team
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create RestTemplate bean with timeout configuration.
     *
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 seconds
        factory.setReadTimeout(15000);     // 15 seconds
        return new RestTemplate(factory);
    }
}
