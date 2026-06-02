package com.staysphere.property_service.dto;

import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.enums.PropertyType;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePropertyRequest {

    private String title;
    private String description;
    private String city;
    private String country;
    private String address;

    @Positive(message = "Price per night must be positive")
    private Double pricePerNight;

    private String currency;

    @Positive(message = "Max guests must be positive")
    private Integer maxGuests;

    private PropertyType propertyType;
    private String amenities;
    private PropertyStatus status;
}