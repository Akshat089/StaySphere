package com.staysphere.search_service.service;

import com.staysphere.search_service.dto.PropertySearchResponse;
import com.staysphere.search_service.dto.SearchPropertySyncRequest;
import com.staysphere.search_service.entity.SearchProperty;
import com.staysphere.search_service.enums.PropertyStatus;
import com.staysphere.search_service.enums.PropertyType;
import com.staysphere.search_service.repository.SearchPropertyRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchPropertyRepository searchPropertyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public PropertySearchResponse syncProperty(SearchPropertySyncRequest request) {
        SearchProperty property = searchPropertyRepository
                .findByPropertyId(request.getPropertyId())
                .orElse(new SearchProperty());

        property.setPropertyId(request.getPropertyId());
        property.setTitle(request.getTitle());
        property.setCity(request.getCity());
        property.setCountry(request.getCountry());
        property.setPricePerNight(request.getPricePerNight());
        property.setCurrency(request.getCurrency());
        property.setMaxGuests(request.getMaxGuests());
        property.setPropertyType(request.getPropertyType());
        property.setStatus(request.getStatus());
        property.setAmenities(request.getAmenities());

        SearchProperty savedProperty = searchPropertyRepository.save(property);

        clearSearchCache();

        return mapToResponse(savedProperty);
    }

    public List<PropertySearchResponse> searchProperties(
            String city,
            String country,
            PropertyType propertyType,
            Integer maxGuests,
            Double minPrice,
            Double maxPrice
    ) {
        String cacheKey = buildCacheKey(
                city,
                country,
                propertyType,
                maxGuests,
                minPrice,
                maxPrice
        );

        System.out.println("Checking Redis for key: " + cacheKey);

        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            System.out.println("CACHE HIT - returning from Redis");
            return (List<PropertySearchResponse>) cachedResult;
        }

        System.out.println("CACHE MISS - querying search_db");

        Specification<SearchProperty> specification = buildSpecification(
                city,
                country,
                propertyType,
                maxGuests,
                minPrice,
                maxPrice
        );

        List<SearchProperty> properties = searchPropertyRepository.findAll(specification);

        List<PropertySearchResponse> response = properties.stream()
                .map(this::mapToResponse)
                .toList();

        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    private Specification<SearchProperty> buildSpecification(
            String city,
            String country,
            PropertyType propertyType,
            Integer maxGuests,
            Double minPrice,
            Double maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("status"), PropertyStatus.ACTIVE));

            if (city != null && !city.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        city.toLowerCase()
                ));
            }

            if (country != null && !country.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("country")),
                        country.toLowerCase()
                ));
            }

            if (propertyType != null) {
                predicates.add(criteriaBuilder.equal(root.get("propertyType"), propertyType));
            }

            if (maxGuests != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("maxGuests"),
                        maxGuests
                ));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("pricePerNight"),
                        minPrice
                ));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("pricePerNight"),
                        maxPrice
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PropertySearchResponse mapToResponse(SearchProperty property) {
        return PropertySearchResponse.builder()
                .propertyId(property.getPropertyId())
                .title(property.getTitle())
                .city(property.getCity())
                .country(property.getCountry())
                .pricePerNight(property.getPricePerNight())
                .currency(property.getCurrency())
                .maxGuests(property.getMaxGuests())
                .propertyType(property.getPropertyType())
                .amenities(property.getAmenities())
                .build();
    }

    private String buildCacheKey(
            String city,
            String country,
            PropertyType propertyType,
            Integer maxGuests,
            Double minPrice,
            Double maxPrice
    ) {
        return "search:properties:"
                + "city=" + city
                + ":country=" + country
                + ":type=" + propertyType
                + ":maxGuests=" + maxGuests
                + ":minPrice=" + minPrice
                + ":maxPrice=" + maxPrice;
    }

    private void clearSearchCache() {
        var keys = redisTemplate.keys("search:properties:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}