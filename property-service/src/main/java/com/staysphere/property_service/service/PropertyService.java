package com.staysphere.property_service.service;

import com.staysphere.property_service.client.SearchServiceClient;
import com.staysphere.property_service.dto.*;
import com.staysphere.property_service.entity.Property;
import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.exception.PropertyNotFoundException;
import com.staysphere.property_service.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final SearchServiceClient searchServiceClient;
    private final RestTemplate restTemplate;
    public PropertyResponse createProperty(CreatePropertyRequest request) {
        Long currentUserId = getCurrentUserId();
        validateHostExists(currentUserId);
        Property property = Property.builder()
                .hostId(currentUserId)
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
        if (!isAdmin() && !property.getHostId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to modify this property");
        }
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
        if (!isAdmin() && !property.getHostId().equals(getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to modify this property");
        }
        validateNoActiveBookings(id);
        property.setStatus(PropertyStatus.INACTIVE);

        Property updatedProperty = propertyRepository.save(property);

        syncToSearch(updatedProperty);
    }
    private void validateNoActiveBookings(Long propertyId) {
        try {
            BookingClientResponse[] bookings = restTemplate.getForObject(
                    "http://localhost:8084/api/bookings/property/" + propertyId,
                    BookingClientResponse[].class
            );

            if (bookings == null) {
                return;
            }

            List<BookingClientResponse> bookingList = Arrays.asList(bookings);

            boolean hasActiveBookings = bookingList.stream()
                    .anyMatch(booking ->
                            "CONFIRMED".equals(booking.getStatus()) ||
                                    "PENDING".equals(booking.getStatus())
                    );

            if (hasActiveBookings) {
                throw new RuntimeException("Cannot delete property because active bookings exist");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate property bookings before delete: " + ex.getMessage());
        }
    }
    private PropertyResponse mapToResponse(Property property) {

        return PropertyResponse.builder().id(property.getId())
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
                .id(property.getId())
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
    private void validateHostExists(Long hostId) {
        try {
            UserClientResponse user = restTemplate.getForObject(
                    "http://localhost:8081/api/users/" + hostId,
                    UserClientResponse.class
            );

            if (user == null) {
                throw new RuntimeException("Host user not found with id: " + hostId);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unable to validate host user: " + ex.getMessage());
        }
    }
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getDetails() == null) {
            throw new RuntimeException("Unauthorized: user details missing");
        }

        Object details = authentication.getDetails();

        if (details instanceof Long) {
            return (Long) details;
        }

        if (details instanceof Integer) {
            return ((Integer) details).longValue();
        }

        return Long.parseLong(details.toString());
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}