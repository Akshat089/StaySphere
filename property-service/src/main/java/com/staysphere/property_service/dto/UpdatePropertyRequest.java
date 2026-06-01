package com.staysphere.property_service.dto;
import com.staysphere.property_service.enums.PropertyStatus;
import com.staysphere.property_service.enums.PropertyType;
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
    private double pricePerNight;
    private String currency;
    private int maxGuests;
    private PropertyType propertyType;
    private String amenities;
    private PropertyStatus status;
}
