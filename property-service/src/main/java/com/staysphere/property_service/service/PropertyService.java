package com.staysphere.property_service.service;

import com.staysphere.property_service.client.SearchServiceClient;
import com.staysphere.property_service.dto.*;
import com.staysphere.property_service.entity.Property;
import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.exception.PropertyNotFoundException;
import com.staysphere.property_service.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final SearchServiceClient searchServiceClient;
    public PropertyResponse createProperty(CreatePropertyRequest request) {

        Property property = Property.builder()
                .hostId(request.getHostId())
                .title(request.getTitle())
                .description(request.getDescription())
                .city(request.getCity())
                .country(request.getCountry())
                .address(request.getAddress())
                .currency(request.getCurrency())
                .pricePerNight(request.getPricePerNight())
                .maxGuests(request.getMaxGuests())
                .propertyType(request.getPropertyType())
                .amenities(request.getAmenities())
                .build();
        property.setStatus(PropertyStatus.ACTIVE);
        Property savedProperty = propertyRepository.save(property);
        syncToSearch(savedProperty);
        return mapToResponse(savedProperty);
    }

    public PropertyResponse getPropertyById(Long id) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));

        return mapToResponse(property);
    }

    public List<PropertySummaryResponse> getAllProperties() {

        return propertyRepository.findAll()
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    public List<PropertySummaryResponse> getPropertiesByHostId(Long hostId) {

        return propertyRepository.findByHostId(hostId)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    public List<PropertySummaryResponse> getPropertiesByCity(String city) {

        return propertyRepository.findByCity(city)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    public PropertyResponse updateProperty(Long id,
                                           UpdatePropertyRequest request) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));

        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setCity(request.getCity());
        property.setCountry(request.getCountry());
        property.setAddress(request.getAddress());
        property.setCurrency(request.getCurrency());
        property.setPricePerNight(request.getPricePerNight());
        property.setMaxGuests(request.getMaxGuests());
        property.setPropertyType(request.getPropertyType());
        property.setAmenities(request.getAmenities());
        property.setStatus(request.getStatus());

        Property updatedProperty = propertyRepository.save(property);
        syncToSearch(updatedProperty);
        return mapToResponse(updatedProperty);
    }

    public void deleteProperty(Long id) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));

        property.setStatus(PropertyStatus.INACTIVE);

        Property updatedProperty = propertyRepository.save(property);

        syncToSearch(updatedProperty);
    }

    private PropertyResponse mapToResponse(Property property) {

        return PropertyResponse.builder()
                .hostId(property.getHostId())
                .title(property.getTitle())
                .description(property.getDescription())
                .city(property.getCity())
                .country(property.getCountry())
                .address(property.getAddress())
                .currency(property.getCurrency())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .amenities(property.getAmenities())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    private PropertySummaryResponse mapToSummary(Property property) {

        return PropertySummaryResponse.builder()
                .hostId(property.getHostId())
                .title(property.getTitle())
                .city(property.getCity())
                .country(property.getCountry())
                .currency(property.getCurrency())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .build();
    }
    private void syncToSearch(Property property) {
        SearchPropertySyncRequest request = SearchPropertySyncRequest.builder()
                .propertyId(property.getId())
                .title(property.getTitle())
                .city(property.getCity())
                .country(property.getCountry())
                .pricePerNight(property.getPricePerNight())
                .currency(property.getCurrency())
                .maxGuests(property.getMaxGuests())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .amenities(property.getAmenities())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();

        searchServiceClient.syncProperty(request);
    }
}