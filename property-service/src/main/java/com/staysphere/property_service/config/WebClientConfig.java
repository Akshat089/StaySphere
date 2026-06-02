package com.staysphere.property_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient searchServiceWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }
}