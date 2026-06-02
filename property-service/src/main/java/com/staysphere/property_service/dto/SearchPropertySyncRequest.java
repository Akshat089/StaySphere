package com.staysphere.property_service.dto;

import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.enums.PropertyType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchPropertySyncRequest {

    private Long propertyId;
    private String title;
    private String city;
    private String country;
    private Double pricePerNight;
    private String currency;
    private Integer maxGuests;
    private PropertyType propertyType;
    private PropertyStatus status;
    private String amenities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}