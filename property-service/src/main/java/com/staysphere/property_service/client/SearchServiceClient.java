package com.staysphere.property_service.client;

import com.staysphere.property_service.dto.SearchPropertySyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchServiceClient {

    private final WebClient searchServiceWebClient;

    public void syncProperty(SearchPropertySyncRequest request) {
        try {
            searchServiceWebClient.post()
                    .uri("/api/search/properties/sync")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Property synced to search-service. propertyId={}", request.getPropertyId());

        } catch (Exception e) {
            log.error("Failed to sync property to search-service. propertyId={}", request.getPropertyId(), e);
        }
    }
}