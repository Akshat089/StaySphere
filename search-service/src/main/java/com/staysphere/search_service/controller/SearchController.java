package com.staysphere.search_service.controller;

import com.staysphere.search_service.dto.PropertySearchResponse;
import com.staysphere.search_service.dto.SearchPropertySyncRequest;
import com.staysphere.search_service.enums.PropertyType;
import com.staysphere.search_service.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/properties")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/sync")
    public PropertySearchResponse syncProperty(
            @Valid @RequestBody SearchPropertySyncRequest request
    ) {
        return searchService.syncProperty(request);
    }

    @GetMapping
    public List<PropertySearchResponse> searchProperties(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) Integer maxGuests,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        return searchService.searchProperties(
                city,
                country,
                propertyType,
                maxGuests,
                minPrice,
                maxPrice
        );
    }
}