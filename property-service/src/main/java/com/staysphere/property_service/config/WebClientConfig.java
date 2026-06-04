package com.staysphere.property_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${SEARCH_SERVICE_URL:http://localhost:8083}")
    private String searchServiceUrl;

    @Bean
    public WebClient searchServiceWebClient() {
        return WebClient.builder()
                .baseUrl(searchServiceUrl)
                .build();
    }
}
